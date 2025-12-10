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
package org.eclipse.rdf4j.benchmark.lmdb.lftj;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.sail.lmdb.lftj.LMDBTrieIterator;
import org.eclipse.rdf4j.sail.lmdb.lftj.Prefix;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKey;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyEncoding;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyOrder;
import org.eclipse.rdf4j.sail.lmdb.lftj.Slot;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Microbenchmark for {@link LMDBTrieIterator} iteration and seek performance on a fixed LMDB dataset.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LMDBTrieIteratorBenchmark {

	@State(Scope.Benchmark)
	public static class BenchmarkState {
		@Param({ "spoc" })
		public String orderName;

		@Param({ "1000", "100000" })
		public int entries;

		public QuadKeyOrder order;
		public long env;
		public int dbi;
		public Prefix predicatePrefix;

		private Path tempDir;

		@Setup(Level.Trial)
		public void setUp() throws Exception {
			order = QuadKeyOrder.fromFieldSequence(orderName);
			tempDir = Files.createTempDirectory("lmdb-trie-iterator-bench");
			env = createEnvironment(tempDir);
			dbi = openDatabase(env, orderName);
			predicatePrefix = Prefix.builder().predicate(1L).build();
			populateData();
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			if (env != 0) {
				mdb_env_close(env);
			}
			if (tempDir != null) {
				try {
					Files.walk(tempDir).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
						try {
							Files.deleteIfExists(path);
						} catch (Exception ignored) {
							// best-effort cleanup
						}
					});
				} catch (Exception ignored) {
					// best-effort cleanup
				}
			}
		}

		private void populateData() throws Exception {
			for (int i = 0; i < entries; i++) {
				long s = i;
				long p = 1L;
				long o = i % 10;
				long c = 0L;
				insertQuad(new QuadKey(s, p, o, c));
			}
		}

		private void insertQuad(QuadKey quadKey) throws Exception {
			byte[] encoded = QuadKeyEncoding.encode(quadKey, order);
			try (MemoryStack stack = stackPush()) {
				PointerBuffer txnPtr = stack.mallocPointer(1);
				assertSuccess(mdb_txn_begin(env, MemoryUtil.NULL, 0, txnPtr));
				long txn = txnPtr.get(0);

				MDBVal keyVal = MDBVal.malloc();
				MDBVal dataVal = MDBVal.malloc();
				ByteBuffer keyBuffer = ByteBuffer.allocateDirect(encoded.length);
				keyBuffer.put(encoded);
				keyBuffer.flip();
				keyVal.mv_data(keyBuffer);
				keyVal.mv_size(keyBuffer.remaining());
				dataVal.mv_size(0);
				dataVal.mv_data((ByteBuffer) null);

				try {
					assertSuccess(mdb_put(txn, dbi, keyVal, dataVal, 0));
					assertSuccess(mdb_txn_commit(txn));
				} catch (Exception e) {
					mdb_txn_abort(txn);
					throw e;
				}
			}
		}

		private long createEnvironment(Path path) throws Exception {
			try (MemoryStack stack = stackPush()) {
				PointerBuffer envPtr = stack.mallocPointer(1);
				assertSuccess(mdb_env_create(envPtr));
				long createdEnv = envPtr.get(0);
				mdb_env_set_maxdbs(createdEnv, 4);
				mdb_env_set_mapsize(createdEnv, 64L * 1024 * 1024);
				assertSuccess(mdb_env_open(createdEnv, path.toString(), 0, 0664));
				return createdEnv;
			}
		}

		private int openDatabase(long environment, String name) throws Exception {
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

		private void assertSuccess(int rc) throws Exception {
			if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND) {
				throw new Exception(mdb_strerror(rc));
			}
		}
	}

	@Benchmark
	public long iteratePrefix(BenchmarkState state) throws Exception {
		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(state.env, stack);
			try (LMDBTrieIterator iterator = new LMDBTrieIterator(txn, state.dbi, state.order, Slot.S)) {
				iterator.open(state.predicatePrefix);
				long sum = 0;
				while (!iterator.atEnd()) {
					sum += iterator.key();
					iterator.next();
				}
				return sum;
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	@Benchmark
	public long seekWithinPrefix(BenchmarkState state) throws Exception {
		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(state.env, stack);
			try (LMDBTrieIterator iterator = new LMDBTrieIterator(txn, state.dbi, state.order, Slot.S)) {
				iterator.open(state.predicatePrefix);
				iterator.seek(state.entries / 2);
				return iterator.atEnd() ? 0 : iterator.key();
			} finally {
				mdb_txn_abort(txn);
			}
		}
	}

	private long beginReadTransaction(long env, MemoryStack stack) throws Exception {
		PointerBuffer txnPtr = stack.mallocPointer(1);
		assertSuccess(mdb_txn_begin(env, MemoryUtil.NULL, MDB_RDONLY, txnPtr));
		return txnPtr.get(0);
	}

	private void assertSuccess(int rc) throws Exception {
		if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND) {
			throw new Exception(mdb_strerror(rc));
		}
	}
}
