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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

class LFTJExecutionTest {

	private static final QuadKeyOrder SPOC = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);

	@TempDir
	Path tempDir;

	private long env;
	private int dbi;

	@BeforeEach
	void setUp() throws Exception {
		env = createEnvironment(tempDir);
		dbi = openDatabase(env, "spoc");
		insertQuad(new QuadKey(1L, 2L, 3L, 0L));
		insertQuad(new QuadKey(1L, 2L, 4L, 0L));
		insertQuad(new QuadKey(2L, 2L, 4L, 0L));
		insertQuad(new QuadKey(3L, 4L, 5L, 0L));
		insertQuad(new QuadKey(4L, 4L, 5L, 0L));
		insertQuad(new QuadKey(10L, 1L, 11L, 0L));
		insertQuad(new QuadKey(11L, 2L, 12L, 0L));
		insertQuad(new QuadKey(12L, 3L, 10L, 0L));
	}

	@AfterEach
	void tearDown() {
		if (env != 0) {
			mdb_env_close(env);
		}
	}

	@Test
	void ordersVariablesUsingIndexAlignment() {
		QuadPattern first = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(0L));

		QuadPattern second = QuadPattern.of(
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(4L),
				QuadPatternTerm.variable("x"),
				QuadPatternTerm.constant(0L));

		List<String> order = LFTJExecutor.chooseVariableOrder(List.of(first, second), Set.of(SPOC));

		assertThat(order).containsExactly("s", "o", "x");
	}

	@Test
	void prioritizesVariablesThatMatchLeadingIndexSlots() {
		QuadKeyOrder posc = QuadKeyOrder.of(Slot.P, Slot.O, Slot.S, Slot.C);
		QuadPattern first = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.variable("p"),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(0L));

		QuadPattern second = QuadPattern.of(
				QuadPatternTerm.variable("x"),
				QuadPatternTerm.variable("p"),
				QuadPatternTerm.constant(9L),
				QuadPatternTerm.constant(0L));

		List<String> order = LFTJExecutor.chooseVariableOrder(List.of(first, second), Set.of(SPOC, posc));

		assertThat(order.get(0)).isEqualTo("p");
	}

	@Test
	void evaluatesChainJoinAgainstLMDBIndexes() throws Exception {
		QuadPattern first = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(0L));

		QuadPattern second = QuadPattern.of(
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(4L),
				QuadPatternTerm.variable("x"),
				QuadPatternTerm.constant(0L));

		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			try {
				LFTJExecutor executor = new LFTJExecutor(txn, Map.of(SPOC, dbi));
				List<Map<String, Long>> results = executor.evaluate(List.of(first, second));

				assertThat(results).containsExactlyInAnyOrder(
						Map.of("o", 3L, "s", 1L, "x", 5L),
						Map.of("o", 4L, "s", 1L, "x", 5L),
						Map.of("o", 4L, "s", 2L, "x", 5L));
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	@Test
	void evaluatesTriangleJoin() throws Exception {
		QuadPattern first = QuadPattern.of(
				QuadPatternTerm.variable("a"),
				QuadPatternTerm.constant(1L),
				QuadPatternTerm.variable("b"),
				QuadPatternTerm.constant(0L));

		QuadPattern second = QuadPattern.of(
				QuadPatternTerm.variable("b"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("c"),
				QuadPatternTerm.constant(0L));

		QuadPattern third = QuadPattern.of(
				QuadPatternTerm.variable("c"),
				QuadPatternTerm.constant(3L),
				QuadPatternTerm.variable("a"),
				QuadPatternTerm.constant(0L));

		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			try {
				LFTJExecutor executor = new LFTJExecutor(txn, Map.of(SPOC, dbi));
				List<Map<String, Long>> results = executor.evaluate(List.of(first, second, third));

				assertThat(results).containsExactly(Map.of(
						"a", 10L,
						"b", 11L,
						"c", 12L));
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
			Path envFile = path.resolve("env.lmdb");
			assertSuccess(mdb_env_open(createdEnv, envFile.toString(), MDB_NOSUBDIR, 0664));
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
		byte[] encoded = QuadKeyEncoding.encode(quadKey, SPOC);
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
}
