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
package org.eclipse.rdf4j.sail.lmdb.lftj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOSUBDIR;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;
import static org.lwjgl.util.lmdb.LMDB.mdb_strerror;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_abort;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_begin;
import static org.lwjgl.util.lmdb.LMDB.mdb_txn_commit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

class LMDBTrieIteratorTest {

	private static final QuadKeyOrder ORDER = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);

	@TempDir
	Path tempDir;

	private long env;
	private int dbi;

	@BeforeEach
	void setUp() throws Exception {
		env = createEnvironment(tempDir);
		dbi = openDatabase(env, "spoc");
		insertQuad(new QuadKey(1L, 2L, 3L, 0L));
		insertQuad(new QuadKey(1L, 2L, 3L, 1L));
		insertQuad(new QuadKey(1L, 2L, 4L, 0L));
		insertQuad(new QuadKey(2L, 2L, 3L, 0L));
		insertQuad(new QuadKey(2L, 2L, 3L, 2L));
		insertQuad(new QuadKey(3L, 2L, 5L, 0L));
		insertQuad(new QuadKey(4L, 3L, 6L, 0L));
	}

	@AfterEach
	void tearDown() {
		if (env != 0) {
			mdb_env_close(env);
		}
	}

	@Test
	void deduplicatesValuesWithinPrefix() throws Exception {
		Prefix prefix = Prefix.builder().predicate(2L).build();

		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			try (LMDBTrieIterator iterator = new LMDBTrieIterator(txn, dbi, ORDER, Slot.S)) {
				iterator.open(prefix);
				assertThat(collect(iterator)).containsExactly(1L, 2L, 3L);
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	@Test
	void seekAdvancesWithinPrefix() throws Exception {
		Prefix prefix = Prefix.builder().subject(1L).predicate(2L).build();

		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			try (LMDBTrieIterator iterator = new LMDBTrieIterator(txn, dbi, ORDER, Slot.O)) {
				iterator.open(prefix);
				assertThat(iterator.atEnd()).isFalse();
				assertThat(iterator.key()).isEqualTo(3L);

				iterator.seek(4L);
				assertThat(iterator.atEnd()).isFalse();
				assertThat(iterator.key()).isEqualTo(4L);

				iterator.seek(5L);
				assertThat(iterator.atEnd()).isTrue();
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	@Test
	void openToMissingPrefixMarksEnd() throws Exception {
		Prefix prefix = Prefix.builder().predicate(99L).build();

		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			try (LMDBTrieIterator iterator = new LMDBTrieIterator(txn, dbi, ORDER, Slot.S)) {
				iterator.open(prefix);
				assertThat(iterator.atEnd()).isTrue();
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	private long createEnvironment(Path path) throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer envPtr = stack.mallocPointer(1);
			assertSuccess(mdb_env_create(envPtr));
			long createdEnv = envPtr.get(0);
			mdb_env_set_maxdbs(createdEnv, 4);
			mdb_env_set_mapsize(createdEnv, 16 * 1024 * 1024);
			assertSuccess(mdb_env_open(createdEnv, path.toString(), 0, 0664));
			return createdEnv;
		}
	}

	private int openDatabase(long environment, String name) throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer txnPtr = stack.mallocPointer(1);
			assertSuccess(mdb_txn_begin(environment, MemoryUtil.NULL, 0, txnPtr));
			long txn = txnPtr.get(0);
			IntBuffer dbiBuf = stack.mallocInt(1);
			assertSuccess(mdb_dbi_open(txn, name, MDB_CREATE, dbiBuf));
			assertSuccess(mdb_txn_commit(txn));
			return dbiBuf.get(0);
		}
	}

	private void insertQuad(QuadKey quadKey) throws IOException {
		byte[] encoded = QuadKeyEncoding.encode(quadKey, ORDER);
		try (MemoryStack stack = stackPush()) {
			PointerBuffer txnPtr = stack.mallocPointer(1);
			assertSuccess(mdb_txn_begin(env, MemoryUtil.NULL, 0, txnPtr));
			long txn = txnPtr.get(0);
			MDBVal keyVal = MDBVal.callocStack(stack);
			MDBVal dataVal = MDBVal.callocStack(stack);
			ByteBuffer keyBuffer = stack.malloc(encoded.length);
			keyBuffer.put(encoded);
			keyBuffer.flip();
			keyVal.mv_data(keyBuffer);
			keyVal.mv_size(keyBuffer.remaining());
			dataVal.mv_size(0);
			dataVal.mv_data((ByteBuffer) null);
			assertSuccess(mdb_put(txn, dbi, keyVal, dataVal, 0));
			assertSuccess(mdb_txn_commit(txn));
		}
	}

	private long beginReadTransaction(MemoryStack stack) throws IOException {
		PointerBuffer txnPtr = stack.mallocPointer(1);
		assertSuccess(mdb_txn_begin(env, MemoryUtil.NULL, MDB_RDONLY, txnPtr));
		return txnPtr.get(0);
	}

	private void assertSuccess(int rc) throws IOException {
		if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND) {
			throw new IOException(mdb_strerror(rc));
		}
	}

	private List<Long> collect(LMDBTrieIterator iterator) {
		List<Long> values = new ArrayList<>();
		while (!iterator.atEnd()) {
			values.add(iterator.key());
			iterator.next();
		}
		return values;
	}
}
