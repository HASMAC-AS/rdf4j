/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_FIRST;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOMETASYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSYNC;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

class PoolCursorTest {

	@Test
	void reusesCursors(@TempDir File dataDir) throws Exception {
		long env;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer envPointer = stack.mallocPointer(1);
			LmdbUtil.E(mdb_env_create(envPointer));
			env = envPointer.get(0);
		}

		try {
			LmdbUtil.E(mdb_env_set_maxdbs(env, 1));
			LmdbUtil.E(mdb_env_open(env, dataDir.getAbsolutePath(), MDB_NOSYNC | MDB_NOMETASYNC | MDB_NOTLS, 0664));

			int dbi = LmdbUtil.openDatabase(env, null, MDB_CREATE, null);

			LmdbUtil.transaction(env, (stack, txn) -> {
				MDBVal key = MDBVal.calloc(stack);
				key.mv_data(stack.bytes((byte) 1));
				MDBVal value = MDBVal.calloc(stack);
				value.mv_data(stack.bytes((byte) 1));
				mdb_put(txn, dbi, key, value, 0);
				return null;
			});

			Pool pool = Pool.get();
			long firstCursor;

			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer txnPointer = stack.mallocPointer(1);
				LmdbUtil.E(mdb_txn_begin(env, NULL, MDB_RDONLY, txnPointer));
				long txn = txnPointer.get(0);

				firstCursor = pool.getCursor(stack, txn, dbi);
				MDBVal key = MDBVal.calloc(stack);
				MDBVal value = MDBVal.calloc(stack);
				assertEquals(MDB_SUCCESS, mdb_cursor_get(firstCursor, key, value, MDB_FIRST));
				pool.freeCursor(firstCursor, dbi);
				mdb_txn_abort(txn);
			}

			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer txnPointer = stack.mallocPointer(1);
				LmdbUtil.E(mdb_txn_begin(env, NULL, MDB_RDONLY, txnPointer));
				long txn = txnPointer.get(0);

				long secondCursor = pool.getCursor(stack, txn, dbi);
				MDBVal key = MDBVal.calloc(stack);
				MDBVal value = MDBVal.calloc(stack);
				assertEquals(firstCursor, secondCursor);
				assertEquals(MDB_SUCCESS, mdb_cursor_get(secondCursor, key, value, MDB_FIRST));
				pool.freeCursor(secondCursor, dbi);
				mdb_txn_abort(txn);
			}
		} finally {
			mdb_env_close(env);
			Pool.release();
		}
	}

	@Test
	void opensNewCursorForWriteTxn(@TempDir File dataDir) throws Exception {
		long env;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer envPointer = stack.mallocPointer(1);
			LmdbUtil.E(mdb_env_create(envPointer));
			env = envPointer.get(0);
		}

		try {
			LmdbUtil.E(mdb_env_set_maxdbs(env, 1));
			LmdbUtil.E(mdb_env_open(env, dataDir.getAbsolutePath(), MDB_NOSYNC | MDB_NOMETASYNC | MDB_NOTLS, 0664));

			int dbi = LmdbUtil.openDatabase(env, null, MDB_CREATE, null);

			LmdbUtil.transaction(env, (stack, txn) -> {
				MDBVal key = MDBVal.calloc(stack);
				key.mv_data(stack.bytes((byte) 1));
				MDBVal value = MDBVal.calloc(stack);
				value.mv_data(stack.bytes((byte) 1));
				mdb_put(txn, dbi, key, value, 0);
				return null;
			});

			Pool pool = Pool.get();

			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer txnPointer = stack.mallocPointer(1);
				LmdbUtil.E(mdb_txn_begin(env, NULL, MDB_RDONLY, txnPointer));
				long txn = txnPointer.get(0);

				long readCursor = pool.getCursor(stack, txn, dbi);
				pool.freeCursor(readCursor, dbi);
				mdb_txn_abort(txn);
			}

			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer txnPointer = stack.mallocPointer(1);
				LmdbUtil.E(mdb_txn_begin(env, NULL, 0, txnPointer));
				long txn = txnPointer.get(0);

				long writeCursor = assertDoesNotThrow(() -> pool.getCursor(stack, txn, dbi));

				MDBVal key = MDBVal.calloc(stack);
				MDBVal value = MDBVal.calloc(stack);
				assertEquals(MDB_SUCCESS, mdb_cursor_get(writeCursor, key, value, MDB_FIRST));
				pool.freeCursor(writeCursor, dbi);
				mdb_txn_abort(txn);
			}
		} finally {
			mdb_env_close(env);
			Pool.release();
		}
	}
}
