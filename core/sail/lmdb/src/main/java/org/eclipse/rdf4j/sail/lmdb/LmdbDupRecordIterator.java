/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_MULTIPLE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT_MULTIPLE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_KEY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.DupIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A dupsort/dupfixed-optimized record iterator using MDB_GET_MULTIPLE/NEXT_MULTIPLE to reduce JNI calls.
 */
class LmdbDupRecordIterator implements RecordIterator {

	@FunctionalInterface
	interface FallbackSupplier {
		RecordIterator get(long[] quadReuse) throws IOException;
	}

	/** Toggle copying of duplicate blocks for extra safety (defaults to copying). */
	private static final boolean COPY_DUP_BLOCKS = Boolean.parseBoolean(
			System.getProperty("rdf4j.lmdb.copyDupBlocks", "true"));

	/** Size in bytes of one (v3,v4) tuple. */
	private static final int DUP_PAIR_BYTES = Long.BYTES * 2;

	private final Pool pool;
	private final DupIndex index;
	private final int dupDbi;
	private final long cursor;

	private final Txn txnRef;
	private long txn; // refreshed on txn version changes
	private long txnRefVersion;
	private final StampedLongAdderLockManager txnLockManager;
	private final Thread ownerThread = Thread.currentThread();

	private final MDBVal keyData;
	private final MDBVal valueData;

	/** Reused output buffer required by the RecordIterator API. */
	private final long[] quad;

	/** Scalars defining the prefix to scan (subject, predicate). */
	private long prefixSubj;
	private long prefixPred;

	private ByteBuffer prefixKeyBuf;

	/** Current duplicate block view and read indices. */
	private ByteBuffer dupBuf;
	private int dupPos;
	private int dupLimit;

	private int lastResult;
	private boolean closed = false;
	private boolean exhausted = false;
	private boolean transferred = false;
	private final boolean reusableOnExhaustion;

	private RecordIterator fallback;
	private FallbackSupplier fallbackSupplier;

	LmdbDupRecordIterator(DupIndex index, long subj, long pred,
			boolean explicit, Txn txnRef, FallbackSupplier fallbackSupplier) throws IOException {
		this(index, subj, pred, explicit, txnRef, fallbackSupplier, false);
	}

	LmdbDupRecordIterator(DupIndex index, long subj, long pred,
			boolean explicit, Txn txnRef, FallbackSupplier fallbackSupplier, boolean reusableOnExhaustion)
			throws IOException {
		this.reusableOnExhaustion = reusableOnExhaustion;
		this.index = index;

		// Output buffer (s,p are constant for the life of this iterator)
		this.quad = new long[4];
		this.quad[0] = subj;
		this.quad[1] = pred;
		this.quad[2] = -1L;
		this.quad[3] = -1L;

		this.prefixSubj = subj;
		this.prefixPred = pred;

		this.fallbackSupplier = fallbackSupplier;

		this.pool = Pool.get();
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();
		this.dupDbi = index.getDupDB(explicit);
		this.txnRef = txnRef;
		this.txnLockManager = txnRef.lockManager();

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			this.txnRefVersion = txnRef.version();
			this.txn = txnRef.get();

			cursor = openCursor(txn, dupDbi, txnRef.isReadOnly());
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
		retarget(subj, pred, fallbackSupplier);
	}

	private LmdbDupRecordIterator(DupIndex index, int dupDbi, long subj, long pred, Txn txnRef,
			StampedLongAdderLockManager txnLockManager, long cursor, Pool pool, MDBVal keyData, MDBVal valueData,
			ByteBuffer prefixKeyBuf, FallbackSupplier fallbackSupplier, boolean reusableOnExhaustion)
			throws IOException {
		this.reusableOnExhaustion = reusableOnExhaustion;
		this.index = index;
		this.quad = new long[4];
		this.quad[2] = -1L;
		this.quad[3] = -1L;
		this.pool = pool;
		this.keyData = keyData;
		this.valueData = valueData;
		this.dupDbi = dupDbi;
		this.txnRef = txnRef;
		this.txnLockManager = txnLockManager;
		this.cursor = cursor;
		this.prefixKeyBuf = prefixKeyBuf;
		retarget(subj, pred, fallbackSupplier);
	}

	@Override
	public long[] next() {
		if (transferred) {
			return null;
		}
		if (fallback != null) {
			return fallback.next();
		}

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			if (closed || exhausted) {
				return null;
			}

			// Txn renewal if the TxnManager rotated its underlying LMDB txn
			if (txnRefVersion != txnRef.version()) {
				this.txn = txnRef.get();
				E(mdb_cursor_renew(txn, cursor));
				txnRefVersion = txnRef.version();
				if (!positionOnPrefix() || !primeDuplicateBlock()) {
					markExhausted();
					return null;
				}
			}

			while (true) {
				// Fast-path: emit from current duplicate block if at least one pair remains
				if (dupBuf != null && dupLimit - dupPos >= DUP_PAIR_BYTES) {
					long v3 = dupBuf.getLong(dupPos);
					long v4 = dupBuf.getLong(dupPos + Long.BYTES);
					dupPos += DUP_PAIR_BYTES;

					// s,p are constant; update only the tail
					quad[2] = v3;
					quad[3] = v4;
					return quad;
				}

				// Ask LMDB for the next duplicate block under the same key
				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT_MULTIPLE);
				if (lastResult == MDB_SUCCESS) {
					resetDuplicateBuffer(valueData.mv_data());
					continue;
				}
				if (lastResult != MDB_NOTFOUND) {
					E(lastResult);
				}

				// No more duplicate blocks for this key; advance to next key in range
				do {
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
					if (lastResult != MDB_SUCCESS) {
						markExhausted();
						return null;
					}
					// Ensure we're still within the requested (subj,pred) prefix
					if (!currentKeyHasPrefix() && !adjustCursorToPrefix()) {
						markExhausted();
						return null;
					}
				} while (!primeDuplicateBlock()); // skip any keys without a duplicate block (defensive)
			}
		} catch (IOException e) {
			throw new SailException(e);
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	LmdbDupRecordIterator tryTransfer(long subj, long pred, boolean explicit, Txn txnRef,
			FallbackSupplier fallbackSupplier) {
		if (!reusableOnExhaustion || transferred || closed || !exhausted || fallback != null || this.txnRef != txnRef
				|| this.dupDbi != index.getDupDB(explicit)) {
			return null;
		}
		transferred = true;
		try {
			return new LmdbDupRecordIterator(index, dupDbi, subj, pred, txnRef, txnLockManager, cursor, pool, keyData,
					valueData, prefixKeyBuf, fallbackSupplier, reusableOnExhaustion);
		} catch (IOException e) {
			transferred = false;
			throw new SailException(e);
		}
	}

	private void retarget(long subj, long pred, FallbackSupplier fallbackSupplier) throws IOException {
		this.prefixSubj = subj;
		this.prefixPred = pred;
		this.fallbackSupplier = fallbackSupplier;
		this.fallback = null;
		this.quad[0] = subj;
		this.quad[1] = pred;
		this.quad[2] = -1L;
		this.quad[3] = -1L;
		this.dupBuf = null;
		this.dupPos = 0;
		this.dupLimit = 0;
		this.lastResult = MDB_NOTFOUND;
		this.exhausted = false;
		this.txn = txnRef.get();
		this.txnRefVersion = txnRef.version();
		resetPrefixKey();

		boolean positioned = positionOnPrefix();
		if (positioned) {
			positioned = primeDuplicateBlock();
		}
		if (!positioned) {
			closeInternal(false);
			fallback = createFallback();
		}
	}

	/* ---------- Positioning & Prefix handling ---------- */

	private boolean positionOnPrefix() throws IOException {
		keyData.mv_data(prefixKeyBuf);

		lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_KEY);
		if (lastResult == MDB_NOTFOUND) {
			lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
		}
		if (lastResult != MDB_SUCCESS) {
			return false;
		}
		if (currentKeyHasPrefix()) {
			return true;
		}
		return adjustCursorToPrefix();
	}

	private boolean adjustCursorToPrefix() throws IOException {
		int cmp = comparePrefix();
		while (cmp < 0) {
			lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
			if (lastResult != MDB_SUCCESS) {
				return false;
			}
			cmp = comparePrefix();
		}
		return cmp == 0;
	}

	/**
	 * Compare current cursor key with (prefixSubj, prefixPred) without allocating a duplicate buffer.
	 */
	private int comparePrefix() {
		ByteBuffer key = keyData.mv_data();
		final int pos = key.position();
		try {
			long a0 = Varint.readUnsigned(key);
			int c0 = Long.compare(a0, prefixSubj);
			if (c0 != 0) {
				return c0;
			}
			long a1 = Varint.readUnsigned(key);
			return Long.compare(a1, prefixPred);
		} finally {
			key.position(pos); // restore buffer position
		}
	}

	private boolean currentKeyHasPrefix() {
		return comparePrefix() == 0;
	}

	private void resetPrefixKey() {
		if (prefixKeyBuf == null) {
			prefixKeyBuf = pool.getKeyBuffer();
		}
		prefixKeyBuf.clear();
		Varint.writeUnsigned(prefixKeyBuf, prefixSubj);
		Varint.writeUnsigned(prefixKeyBuf, prefixPred);
		prefixKeyBuf.flip();
	}

	/* ---------- Duplicate block priming ---------- */

	/**
	 * Prime the duplicate buffer for the current key using MDB_GET_MULTIPLE.
	 */
	private boolean primeDuplicateBlock() throws IOException {
		lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_GET_MULTIPLE);
		if (lastResult == MDB_SUCCESS) {
			resetDuplicateBuffer(valueData.mv_data());
			return dupBuf != null && dupLimit - dupPos >= DUP_PAIR_BYTES;
		}
		if (lastResult == MDB_NOTFOUND) {
			resetDuplicateBuffer(null);
			return false;
		}
		E(lastResult);
		return false; // unreachable
	}

	/**
	 * Prepare a readable view over the LMDB-provided value buffer. Default: zero-copy {@code slice()}. If
	 * COPY_DUP_BLOCKS is true, heap-copy for extra safety.
	 */
	private void resetDuplicateBuffer(ByteBuffer buffer) {
		if (buffer == null) {
			dupBuf = null;
			dupPos = dupLimit = 0;
			return;
		}

		if (!COPY_DUP_BLOCKS) {
			// Zero-copy: view over [position, limit) of the native buffer
			ByteBuffer view = buffer.slice();
			view.order(ByteOrder.BIG_ENDIAN);
			dupBuf = view;
			dupPos = view.position(); // 0
			dupLimit = view.limit();
		} else {
			// Conservative path: copy to Java heap to decouple lifetime from cursor operations
			ByteBuffer src = buffer.duplicate();
			src.position(buffer.position());
			src.limit(buffer.limit());
			ByteBuffer copy = ByteBuffer.allocate(src.remaining());
			copy.put(src);
			copy.flip();
			copy.order(ByteOrder.BIG_ENDIAN);
			dupBuf = copy;
			dupPos = dupBuf.position();
			dupLimit = dupBuf.limit();
		}
	}

	/* ---------- Lifecycle ---------- */

	private void closeInternal(boolean maybeCalledAsync) {
		if (!closed) {
			long writeStamp = 0L;
			boolean writeLocked = false;
			if (maybeCalledAsync && ownerThread != Thread.currentThread()) {
				try {
					writeStamp = txnLockManager.writeLock();
					writeLocked = true;
				} catch (InterruptedException e) {
					throw new SailException(e);
				}
			}
			try {
				if (!closed) {
					if (txnRef.isReadOnly()) {
						pool.freeCursor(dupDbi, index, cursor);
					} else {
						mdb_cursor_close(cursor);
					}
					pool.free(keyData);
					pool.free(valueData);
					if (prefixKeyBuf != null) {
						pool.free(prefixKeyBuf);
					}
				}
			} finally {
				closed = true;
				if (writeLocked) {
					txnLockManager.unlockWrite(writeStamp);
				}
			}
		}
	}

	private void markExhausted() {
		exhausted = true;
		if (!reusableOnExhaustion) {
			closeInternal(false);
		}
	}

	@Override
	public void close() {
		if (transferred) {
			return;
		}
		if (fallback != null) {
			fallback.close();
		} else {
			closeInternal(true);
		}
	}

	private long openCursor(long txn, int dbi, boolean tryReuse) throws IOException {
		if (tryReuse) {
			long pooled = pool.getCursor(dbi, index);
			if (pooled != 0L) {
				try {
					E(mdb_cursor_renew(txn, pooled));
					return pooled;
				} catch (IOException renewEx) {
					mdb_cursor_close(pooled);
				}
			}
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_cursor_open(txn, dbi, pp));
			return pp.get(0);
		}
	}

	private RecordIterator createFallback() throws IOException {
		if (fallbackSupplier == null) {
			return null;
		}
		return fallbackSupplier.get(quad);
	}
}
