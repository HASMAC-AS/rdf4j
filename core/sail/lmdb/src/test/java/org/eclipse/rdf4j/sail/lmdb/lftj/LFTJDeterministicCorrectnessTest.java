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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

class LFTJDeterministicCorrectnessTest {

	private static final QuadKeyOrder SPOC = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);
	private static final QuadKeyOrder POSC = QuadKeyOrder.of(Slot.P, Slot.O, Slot.S, Slot.C);
	private static final QuadKeyOrder OSPC = QuadKeyOrder.of(Slot.O, Slot.S, Slot.P, Slot.C);

	@TempDir
	Path tempDir;

	private long env;
	private int dbiSpoc;
	private int dbiPosc;
	private int dbiOspc;
	private List<QuadKey> dataset;

	@BeforeEach
	void setUp() throws Exception {
		env = createEnvironment(tempDir);
		dbiSpoc = openDatabase(env, "spoc");
		dbiPosc = openDatabase(env, "posc");
		dbiOspc = openDatabase(env, "ospc");
		dataset = seedDataset();
		for (QuadKey quad : dataset) {
			insertQuad(quad, SPOC, dbiSpoc);
			insertQuad(quad, POSC, dbiPosc);
			insertQuad(quad, OSPC, dbiOspc);
		}
	}

	@AfterEach
	void tearDown() {
		if (env != 0) {
			mdb_env_close(env);
		}
	}

	@Test
	void chainJoinMatchesNaive() throws Exception {
		List<QuadPattern> patterns = List.of(
				QuadPattern.of(
						QuadPatternTerm.variable("s"),
						QuadPatternTerm.constant(1L),
						QuadPatternTerm.variable("o"),
						QuadPatternTerm.constant(0L)),
				QuadPattern.of(
						QuadPatternTerm.variable("o"),
						QuadPatternTerm.constant(1L),
						QuadPatternTerm.variable("x"),
						QuadPatternTerm.constant(0L)));

		List<Map<String, Long>> expected = List.of(
				Map.of("s", 1L, "o", 2L, "x", 3L),
				Map.of("s", 2L, "o", 3L, "x", 1L),
				Map.of("s", 3L, "o", 1L, "x", 2L));

		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			try {
				LFTJExecutor executor = new LFTJExecutor(txn,
						Map.of(SPOC, dbiSpoc, POSC, dbiPosc, OSPC, dbiOspc));
				List<Map<String, Long>> actual = executor.evaluate(patterns);
				Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	@Test
	void triangleJoinMatchesNaive() throws Exception {
		List<QuadPattern> patterns = List.of(
				QuadPattern.of(
						QuadPatternTerm.variable("a"),
						QuadPatternTerm.constant(1L),
						QuadPatternTerm.variable("b"),
						QuadPatternTerm.constant(0L)),
				QuadPattern.of(
						QuadPatternTerm.variable("b"),
						QuadPatternTerm.constant(1L),
						QuadPatternTerm.variable("c"),
						QuadPatternTerm.constant(0L)),
				QuadPattern.of(
						QuadPatternTerm.variable("c"),
						QuadPatternTerm.constant(1L),
						QuadPatternTerm.variable("a"),
						QuadPatternTerm.constant(0L)));

		List<Map<String, Long>> expected = List.of(Map.of("a", 1L, "b", 2L, "c", 3L));

		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			try {
				LFTJExecutor executor = new LFTJExecutor(txn,
						Map.of(SPOC, dbiSpoc, POSC, dbiPosc, OSPC, dbiOspc));
				List<Map<String, Long>> actual = executor.evaluate(patterns);
				Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	@Test
	void starJoinMatchesNaive() throws Exception {
		List<QuadPattern> patterns = List.of(
				QuadPattern.of(
						QuadPatternTerm.variable("s"),
						QuadPatternTerm.constant(1L),
						QuadPatternTerm.variable("o1"),
						QuadPatternTerm.constant(0L)),
				QuadPattern.of(
						QuadPatternTerm.variable("s"),
						QuadPatternTerm.constant(2L),
						QuadPatternTerm.variable("o2"),
						QuadPatternTerm.constant(0L)));

		List<Map<String, Long>> expected = List.of(Map.of("s", 1L, "o1", 2L, "o2", 5L));

		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			try {
				LFTJExecutor executor = new LFTJExecutor(txn,
						Map.of(SPOC, dbiSpoc, POSC, dbiPosc, OSPC, dbiOspc));
				List<Map<String, Long>> actual = executor.evaluate(patterns);
				Assertions.assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	private List<Map<String, Long>> evaluateNaively(List<QuadPattern> patterns) {
		List<Map<String, Long>> bindings = new ArrayList<>();
		bindings.add(Map.of());

		for (QuadPattern pattern : patterns) {
			List<Map<String, Long>> matches = matchesForPattern(pattern);
			List<Map<String, Long>> next = new ArrayList<>();
			for (Map<String, Long> current : bindings) {
				for (Map<String, Long> match : matches) {
					Map<String, Long> merged = mergeIfCompatible(current, match);
					if (merged != null) {
						next.add(merged);
					}
				}
			}
			bindings = next;
		}
		return bindings;
	}

	private List<Map<String, Long>> matchesForPattern(QuadPattern pattern) {
		List<Map<String, Long>> matches = new ArrayList<>();
		for (QuadKey quad : dataset) {
			Map<String, Long> binding = new HashMap<>();
			if (!bind(pattern.term(Slot.S), quad.s(), binding)) {
				continue;
			}
			if (!bind(pattern.term(Slot.P), quad.p(), binding)) {
				continue;
			}
			if (!bind(pattern.term(Slot.O), quad.o(), binding)) {
				continue;
			}
			if (!bind(pattern.term(Slot.C), quad.c(), binding)) {
				continue;
			}
			matches.add(binding);
		}
		return matches;
	}

	private boolean bind(QuadPatternTerm term, long value, Map<String, Long> binding) {
		if (term.isUnbound()) {
			return true;
		}
		if (term.isConstant()) {
			return term.constant() == value;
		}
		String variable = term.variable();
		Long existing = binding.get(variable);
		if (existing != null && existing != value) {
			return false;
		}
		binding.put(variable, value);
		return true;
	}

	private Map<String, Long> mergeIfCompatible(Map<String, Long> left, Map<String, Long> right) {
		Map<String, Long> merged = new HashMap<>(left);
		for (Map.Entry<String, Long> entry : right.entrySet()) {
			Long existing = merged.get(entry.getKey());
			if (existing != null && !existing.equals(entry.getValue())) {
				return null;
			}
			merged.put(entry.getKey(), entry.getValue());
		}
		return merged;
	}

	private List<QuadKey> seedDataset() {
		return List.of(
				new QuadKey(1, 1, 2, 0),
				new QuadKey(2, 1, 3, 0),
				new QuadKey(3, 1, 1, 0),
				new QuadKey(1, 2, 5, 0),
				new QuadKey(2, 2, 6, 0));
	}

	private void insertQuad(QuadKey quadKey, QuadKeyOrder order, int dbi) throws IOException {
		byte[] encoded = QuadKeyEncoding.encode(quadKey, order);
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
			dataVal.mv_data((ByteBuffer) null);
			dataVal.mv_size(0);
			try {
				assertSuccess(mdb_put(txn, dbi, keyVal, dataVal, 0));
				assertSuccess(mdb_txn_commit(txn));
			} catch (Exception e) {
				mdb_txn_abort(txn);
				throw e;
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
