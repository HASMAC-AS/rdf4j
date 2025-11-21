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
import static org.lwjgl.util.lmdb.LMDB.MDB_FIRST;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT_NODUP;
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

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight cursor over one trie level duplicates list. Keys are the fixed prefix; values are the children IDs.
 */
final class TrieLevelCursor implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(TrieLevelCursor.class);
	private final TrieIndexManager.IndexOrder order;
	private final int level;
	private final int dbi;
	private final long txn;
	private final Pool pool;
	private final Object cursorOwner;
	private final ByteBuffer keyBuffer;

	private long cursor;
	private final MDBVal keyVal;
	private final MDBVal dataVal;
	private boolean atEnd = true;
	private long currentChild = -1;
	private boolean iterateKeys = false;
	private boolean closed;

	TrieLevelCursor(TrieIndexManager.IndexOrder order, int level, int dbi, long txn) throws IOException {
		this(order, level, dbi, txn, null, null);
	}

	TrieLevelCursor(TrieIndexManager.IndexOrder order, int level, int dbi, long txn, Pool pool, Object cursorOwner)
			throws IOException {
		this.order = order;
		this.level = level;
		this.dbi = dbi;
		this.txn = txn;
		this.pool = pool;
		this.cursorOwner = cursorOwner;
		this.keyBuffer = pool != null ? pool.getKeyBuffer() : null;
		this.keyVal = pool != null ? pool.getVal() : MDBVal.malloc();
		this.dataVal = pool != null ? pool.getVal() : MDBVal.malloc();
		this.cursor = openCursor();
	}

	void openPrefix(boolean iterateKeys, long... prefix) throws IOException {
		if (cursor == 0) {
			throw new IOException("Cursor closed");
		}
		this.iterateKeys = iterateKeys;
		if (prefix.length == 0) {
			int rc = mdb_cursor_get(cursor, keyVal, dataVal, MDB_FIRST);
			if (rc == MDB_SUCCESS) {
				atEnd = false;
				currentChild = iterateKeys ? Varint.readUnsigned(keyVal.mv_data())
						: Varint.readUnsigned(dataVal.mv_data());
			} else if (rc == MDB_NOTFOUND) {
				atEnd = true;
				currentChild = -1;
			} else {
				E(rc);
			}
		} else {
			int required = prefix.length * 9;
			ByteBuffer key;
			if (keyBuffer != null && keyBuffer.capacity() >= required) {
				key = keyBuffer;
				key.clear();
			} else {
				key = ByteBuffer.allocateDirect(required);
			}
			for (long v : prefix) {
				Varint.writeUnsigned(key, v);
			}
			key.flip();
			keyVal.mv_data(key);

			int rc = mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET_KEY);
			if (rc == MDB_SUCCESS) {
				atEnd = false;
				currentChild = Varint.readUnsigned(dataVal.mv_data());
			} else if (rc == MDB_NOTFOUND) {
				atEnd = true;
				currentChild = -1;
			} else {
				E(rc);
			}
		}
	}

	void openPrefix(long... prefix) throws IOException {
		openPrefix(false, prefix);
	}

	boolean next() throws IOException {
		if (atEnd) {
			return false;
		}
		int op = iterateKeys ? MDB_NEXT_NODUP : MDB_NEXT_DUP;
		int rc = mdb_cursor_get(cursor, keyVal, dataVal, op);
		if (rc == MDB_SUCCESS) {
			currentChild = iterateKeys ? Varint.readUnsigned(keyVal.mv_data()) : Varint.readUnsigned(dataVal.mv_data());
			return true;
		}
		if (rc == MDB_NOTFOUND) {
			atEnd = true;
			return false;
		}
		E(rc);
		return false;
	}

	boolean seek(long target) throws IOException {
		if (atEnd) {
			return false;
		}
		if (currentChild >= target) {
			return true;
		}
		if (iterateKeys) {
			ByteBuffer key = keyBuffer != null ? keyBuffer : ByteBuffer.allocateDirect(9);
			key.clear();
			Varint.writeUnsigned(key, target);
			key.flip();
			keyVal.mv_data(key);
			int rc = mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET_RANGE);
			if (rc == MDB_SUCCESS) {
				currentChild = Varint.readUnsigned(keyVal.mv_data());
				atEnd = false;
				return true;
			}
			if (rc == MDB_NOTFOUND) {
				atEnd = true;
				return false;
			}
			E(rc);
			return false;
		} else {
			while (next()) {
				if (currentChild >= target) {
					return true;
				}
			}
			return false;
		}
	}

	long key() {
		return currentChild;
	}

	boolean atEnd() {
		return atEnd;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		if (cursor != 0) {
			if (pool != null) {
				pool.freeCursor(dbi, cursorOwner != null ? cursorOwner : order, cursor);
			} else {
				mdb_cursor_close(cursor);
			}
			cursor = 0;
		}
		if (pool != null) {
			pool.free(keyVal);
			pool.free(dataVal);
			if (keyBuffer != null) {
				pool.free(keyBuffer);
			}
		} else {
			keyVal.close();
			dataVal.close();
		}
	}

	@Override
	public String toString() {
		return "TrieLevelCursor[" + order.name + " L" + level + "]";
	}

	private long openCursor() throws IOException {
		if (pool != null) {
			long pooled = pool.getCursor(dbi, cursorOwner != null ? cursorOwner : order);
			if (pooled != 0L) {
				try {
					E(mdb_cursor_renew(txn, pooled));
					return pooled;
				} catch (IOException renewEx) {
					mdb_cursor_close(pooled);
				}
			}
		} else {
			log.debug("We really want to use a cursor pool here! So this message shouldn't be printed often.");
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_cursor_open(txn, dbi, pp));
			return pp.get(0);
		}
	}
}
