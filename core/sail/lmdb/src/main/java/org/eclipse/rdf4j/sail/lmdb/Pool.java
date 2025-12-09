/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
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
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A simple pool for {@link MDBVal}, {@link ByteBuffer}, {@link Statistics} and LMDB cursors.
 */
class Pool {
	// thread-local pool instance
	private static final ThreadLocal<Pool> threadlocal = ThreadLocal.withInitial(Pool::new);

	private final MDBVal[] valPool = new MDBVal[1024];
	private final ByteBuffer[] keyPool = new ByteBuffer[1024];
	private final Statistics[] statisticsPool = new Statistics[512];
	private final long[] cursorPool = new long[256];
	private final int[] cursorDbiPool = new int[256];
	private int valPoolIndex = -1;
	private int keyPoolIndex = -1;
	private int statisticsPoolIndex = -1;
	private int cursorPoolIndex = -1;

	final MDBVal getVal() {
		if (valPoolIndex >= 0) {
			return valPool[valPoolIndex--];
		}
		return MDBVal.malloc();
	}

	final ByteBuffer getKeyBuffer() {
		if (keyPoolIndex >= 0) {
			ByteBuffer bb = keyPool[keyPoolIndex--];
			bb.clear();
			return bb;
		}
		return MemoryUtil.memAlloc(TripleStore.MAX_KEY_LENGTH);
	}

	final Statistics getStatistics() {
		if (statisticsPoolIndex >= 0) {
			return statisticsPool[statisticsPoolIndex--];
		}
		return new Statistics();
	}

	final void free(MDBVal val) {
		if (valPoolIndex < valPool.length - 1) {
			valPool[++valPoolIndex] = val;
		} else {
			val.close();
		}
	}

	final void free(ByteBuffer bb) {
		if (keyPoolIndex < keyPool.length - 1) {
			keyPool[++keyPoolIndex] = bb;
		} else {
			MemoryUtil.memFree(bb);
		}
	}

	final void free(Statistics statistics) {
		if (statisticsPoolIndex < statisticsPool.length - 1) {
			statisticsPool[++statisticsPoolIndex] = statistics;
		}
	}

	final long getCursor(MemoryStack stack, long txn, int dbi) throws IOException {
		for (int i = cursorPoolIndex; i >= 0; i--) {
			if (cursorDbiPool[i] == dbi) {
				long cursor = cursorPool[i];
				cursorPool[i] = cursorPool[cursorPoolIndex];
				cursorDbiPool[i] = cursorDbiPool[cursorPoolIndex];
				cursorPoolIndex--;

				int rc = mdb_cursor_renew(txn, cursor);
				if (rc == MDB_SUCCESS) {
					return cursor;
				}

				mdb_cursor_close(cursor);
			}
		}

		PointerBuffer pp = stack.mallocPointer(1);
		E(mdb_cursor_open(txn, dbi, pp));
		return pp.get(0);
	}

	final void freeCursor(long cursor, int dbi) {
		if (cursor == 0) {
			return;
		}
		if (cursorPoolIndex < cursorPool.length - 1) {
			cursorPool[++cursorPoolIndex] = cursor;
			cursorDbiPool[cursorPoolIndex] = dbi;
		} else {
			mdb_cursor_close(cursor);
		}
	}

	final void close() {
		while (valPoolIndex >= 0) {
			valPool[valPoolIndex--].close();
		}
		while (keyPoolIndex >= 0) {
			MemoryUtil.memFree(keyPool[keyPoolIndex--]);
		}
		while (cursorPoolIndex >= 0) {
			mdb_cursor_close(cursorPool[cursorPoolIndex--]);
		}
	}

	/**
	 * Get a pool instance for the current thread.
	 *
	 * @return a Pool instance
	 */
	public static Pool get() {
		return threadlocal.get();
	}

	/**
	 * Release the pool instance for the current thread.
	 */
	public static void release() {
		Pool pool = threadlocal.get();
		pool.close();
		threadlocal.remove();
	}
}
