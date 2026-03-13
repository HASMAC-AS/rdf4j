/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
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
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cmp;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.KeyBuilder;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.TripleIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A record iterator that wraps a native LMDB iterator.
 */
class LmdbRecordIterator implements RecordIterator {
	private static final Logger log = LoggerFactory.getLogger(LmdbRecordIterator.class);
	private final Pool pool;

	private final TripleIndex index;

	private long subj;
	private long pred;
	private long obj;
	private long context;

	private final long cursor;

	private MDBVal maxKey;

	private boolean matchValues;
	private GroupMatcher groupMatcher;

	/**
	 * True when late-bound variables exist beyond the contiguous prefix of the chosen index order, requiring
	 * value-level filtering. When false, range bounds already guarantee that every visited key matches and the
	 * GroupMatcher is redundant.
	 */
	private boolean needMatcher;

	private final Txn txnRef;

	private long txnRefVersion;

	private long txn;

	private final int dbi;

	private volatile boolean closed = false;
	private boolean exhausted = false;
	private boolean transferred = false;

	private final MDBVal keyData;

	private final MDBVal valueData;

	private ByteBuffer minKeyBuf;

	private ByteBuffer maxKeyBuf;

	private final long[] quad;

	private boolean fetchNext = false;

	private final StampedLongAdderLockManager txnLockManager;

	private final Thread ownerThread = Thread.currentThread();

	private long sourceRowsScannedActual;
	private long sourceRowsMatchedActual;
	private long sourceRowsFilteredActual;
	private final boolean reusableOnExhaustion;

	LmdbRecordIterator(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef) throws IOException {
		this(index, null, rangeSearch, subj, pred, obj, context, explicit, txnRef, null, false);
	}

	LmdbRecordIterator(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef, long[] quadReuse) throws IOException {
		this(index, null, rangeSearch, subj, pred, obj, context, explicit, txnRef, quadReuse, false);
	}

	LmdbRecordIterator(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef, long[] quadReuse, boolean reusableOnExhaustion)
			throws IOException {
		this(index, null, rangeSearch, subj, pred, obj, context, explicit, txnRef, quadReuse,
				reusableOnExhaustion);
	}

	LmdbRecordIterator(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj,
			long pred, long obj, long context, boolean explicit, Txn txnRef) throws IOException {
		this(index, keyBuilder, rangeSearch, subj, pred, obj, context, explicit, txnRef, null, false);
	}

	LmdbRecordIterator(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj,
			long pred, long obj, long context, boolean explicit, Txn txnRef, long[] quadReuse) throws IOException {
		this(index, keyBuilder, rangeSearch, subj, pred, obj, context, explicit, txnRef, quadReuse, false);
	}

	LmdbRecordIterator(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj,
			long pred, long obj, long context, boolean explicit, Txn txnRef, long[] quadReuse,
			boolean reusableOnExhaustion) throws IOException {
		this.reusableOnExhaustion = reusableOnExhaustion;
		if (quadReuse != null && quadReuse.length >= 4) {
			this.quad = quadReuse;
		} else {
			this.quad = new long[4];
		}
		this.pool = Pool.get();
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();
		this.index = index;
		this.dbi = index.getDB(explicit);
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

			// Try to reuse a pooled cursor only for read-only transactions; otherwise open a new one
			if (txnRef.isReadOnly()) {
				long pooled = pool.getCursor(dbi, index);
				if (pooled != 0L) {
					long c = pooled;
					try {
						E(mdb_cursor_renew(txn, c));
					} catch (IOException renewEx) {
						// Renewal failed (e.g., incompatible txn). Close pooled cursor and open a fresh one.
						mdb_cursor_close(c);
						try (MemoryStack stack = MemoryStack.stackPush()) {
							PointerBuffer pp = stack.mallocPointer(1);
							E(mdb_cursor_open(txn, dbi, pp));
							c = pp.get(0);
						}
					}
					cursor = c;
				} else {
					try (MemoryStack stack = MemoryStack.stackPush()) {
						PointerBuffer pp = stack.mallocPointer(1);
						E(mdb_cursor_open(txn, dbi, pp));
						cursor = pp.get(0);
					}
				}
			} else {
				try (MemoryStack stack = MemoryStack.stackPush()) {
					PointerBuffer pp = stack.mallocPointer(1);
					E(mdb_cursor_open(txn, dbi, pp));
					cursor = pp.get(0);
				}
			}
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
		retarget(keyBuilder, rangeSearch, subj, pred, obj, context);
	}

	private LmdbRecordIterator(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj,
			long pred, long obj, long context, boolean explicit, Txn txnRef,
			StampedLongAdderLockManager txnLockManager, int dbi, long cursor, Pool pool, MDBVal keyData,
			MDBVal valueData, MDBVal maxKey, ByteBuffer minKeyBuf, ByteBuffer maxKeyBuf, long[] quadReuse,
			boolean reusableOnExhaustion) {
		this.reusableOnExhaustion = reusableOnExhaustion;
		if (quadReuse != null && quadReuse.length >= 4) {
			this.quad = quadReuse;
		} else {
			this.quad = new long[4];
		}
		this.pool = pool;
		this.keyData = keyData;
		this.valueData = valueData;
		this.index = index;
		this.dbi = dbi;
		this.txnRef = txnRef;
		this.txnLockManager = txnLockManager;
		this.cursor = cursor;
		this.maxKey = maxKey;
		this.minKeyBuf = minKeyBuf;
		this.maxKeyBuf = maxKeyBuf;
		retarget(keyBuilder, rangeSearch, subj, pred, obj, context);
	}

	@Override
	public long[] next() {
		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			if (closed || exhausted || transferred) {
				log.debug("Calling next() on an LmdbRecordIterator that is already closed, returning null");
				return null;
			}

			int lastResult;
			if (txnRefVersion != txnRef.version()) {
				// TODO: None of the tests in the LMDB Store cover this case!
				// cursor must be renewed
				this.txn = txnRef.get();
				mdb_cursor_renew(txn, cursor);
				if (fetchNext) {
					// cursor must be positioned on last item, reuse minKeyBuf if available
					if (minKeyBuf == null) {
						minKeyBuf = pool.getKeyBuffer();
					}
					minKeyBuf.clear();
					index.toKey(minKeyBuf, quad[0], quad[1], quad[2], quad[3]);
					minKeyBuf.flip();
					keyData.mv_data(minKeyBuf);
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET);
					if (lastResult != MDB_SUCCESS) {
						// use MDB_SET_RANGE if key was deleted
						lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
					}
					if (lastResult != MDB_SUCCESS) {
						markExhausted();
						return null;
					}
				}
				// update version of txn ref
				this.txnRefVersion = txnRef.version();
			}

			if (fetchNext) {
				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				fetchNext = false;
			} else {
				if (minKeyBuf != null) {
					// set cursor to min key
					keyData.mv_data(minKeyBuf);
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
				} else {
					// set cursor to first item
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				}
			}

			while (lastResult == MDB_SUCCESS) {
				sourceRowsScannedActual++;
				// if (maxKey != null && TripleStore.COMPARATOR.compare(keyData.mv_data(), maxKey.mv_data()) > 0) {
				if (maxKey != null && mdb_cmp(txn, dbi, keyData, maxKey) > 0) {
					sourceRowsFilteredActual++;
					lastResult = MDB_NOTFOUND;
				} else if (matches()) {
					sourceRowsFilteredActual++;
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				} else {
					// Matching value found
					sourceRowsMatchedActual++;
					index.keyToQuad(keyData.mv_data(), subj, pred, obj, context, quad);
					sourceRowsMatchedActual++;
					// fetch next value
					fetchNext = true;
					return quad;
				}
			}
			markExhausted();
			return null;
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	LmdbRecordIterator tryTransfer(TripleIndex index, KeyBuilder keyBuilder, boolean rangeSearch, long subj,
			long pred, long obj, long context, boolean explicit, Txn txnRef, long[] quadReuse) {
		if (!reusableOnExhaustion || transferred || closed || !exhausted || this.index != index || this.txnRef != txnRef
				|| this.dbi != index.getDB(explicit)) {
			return null;
		}
		transferred = true;
		return new LmdbRecordIterator(index, keyBuilder, rangeSearch, subj, pred, obj, context, explicit, txnRef,
				txnLockManager, dbi, cursor, pool, keyData, valueData, maxKey, minKeyBuf, maxKeyBuf, quadReuse,
				reusableOnExhaustion);
	}

	private void retarget(KeyBuilder keyBuilder, boolean rangeSearch, long subj, long pred, long obj, long context) {
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.context = context;
		this.txn = txnRef.get();
		this.txnRefVersion = txnRef.version();
		this.quad[0] = subj;
		this.quad[1] = pred;
		this.quad[2] = obj;
		this.quad[3] = context;
		this.groupMatcher = null;
		this.fetchNext = false;
		this.exhausted = false;
		this.sourceRowsScannedActual = 0;
		this.sourceRowsMatchedActual = 0;
		this.sourceRowsFilteredActual = 0;

		if (rangeSearch) {
			if (minKeyBuf == null) {
				minKeyBuf = pool.getKeyBuffer();
			}
			if (maxKey == null) {
				maxKey = pool.getVal();
			}
			if (maxKeyBuf == null) {
				maxKeyBuf = pool.getKeyBuffer();
			}
			minKeyBuf.clear();
			if (keyBuilder != null) {
				keyBuilder.writeMin(minKeyBuf);
			} else {
				index.getMinKey(minKeyBuf, subj, pred, obj, context);
			}
			minKeyBuf.flip();

			maxKeyBuf.clear();
			if (keyBuilder != null) {
				keyBuilder.writeMax(maxKeyBuf);
			} else {
				index.getMaxKey(maxKeyBuf, subj, pred, obj, context);
			}
			maxKeyBuf.flip();
			maxKey.mv_data(maxKeyBuf);
		} else {
			if (subj > 0 || pred > 0 || obj > 0 || context >= 0) {
				if (minKeyBuf == null) {
					minKeyBuf = pool.getKeyBuffer();
				}
				minKeyBuf.clear();
				index.getMinKey(minKeyBuf, subj, pred, obj, context);
				minKeyBuf.flip();
			} else if (minKeyBuf != null) {
				pool.free(minKeyBuf);
				minKeyBuf = null;
			}
			if (maxKey != null) {
				pool.free(maxKeyBuf);
				pool.free(maxKey);
				maxKeyBuf = null;
				maxKey = null;
			}
		}

		this.matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;
		int prefixLen = index.getPatternScore(subj, pred, obj, context);
		int boundCount = (subj > 0 ? 1 : 0) + (pred > 0 ? 1 : 0) + (obj > 0 ? 1 : 0) + (context >= 0 ? 1 : 0);
		this.needMatcher = boundCount > prefixLen;
	}

	private boolean matches() {
		// When there are no late-bound variables beyond the contiguous prefix, range bounds fully determine matches.
		if (!needMatcher) {
			return false;
		}

		if (groupMatcher != null) {
			return !this.groupMatcher.matches(keyData.mv_data());
		} else if (matchValues) {
			this.groupMatcher = index.createMatcher(subj, pred, obj, context);
			return !this.groupMatcher.matches(keyData.mv_data());
		} else {
			return false;
		}
	}

	private void markExhausted() {
		exhausted = true;
		if (!reusableOnExhaustion) {
			closeInternal(false);
		}
	}

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
						pool.freeCursor(dbi, index, cursor);
					} else {
						mdb_cursor_close(cursor);
					}
					pool.free(keyData);
					pool.free(valueData);
					if (minKeyBuf != null) {
						pool.free(minKeyBuf);
					}
					if (maxKey != null) {
						pool.free(maxKeyBuf);
						pool.free(maxKey);
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

	@Override
	public void close() {
		if (transferred) {
			return;
		}
		closeInternal(true);
	}

	@Override
	public String getIndexName() {
		return index.toString();
	}

	@Override
	public long getSourceRowsScannedActual() {
		return sourceRowsScannedActual;
	}

	@Override
	public long getSourceRowsMatchedActual() {
		return sourceRowsMatchedActual;
	}

	@Override
	public long getSourceRowsFilteredActual() {
		return sourceRowsFilteredActual;
	}
}
