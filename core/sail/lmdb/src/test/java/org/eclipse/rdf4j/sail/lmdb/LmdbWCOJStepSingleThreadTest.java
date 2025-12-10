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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.lftj.LmdbWCOJ;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKey;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyEncoding;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyOrder;
import org.eclipse.rdf4j.sail.lmdb.lftj.Slot;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;
import org.mockito.Mockito;

class LmdbWCOJStepSingleThreadTest {

	private static final QuadKeyOrder SPOC = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);

	private static final String WORKER_THREAD_NAME = "rdf4j-lmdb-wcoj";

	@TempDir
	Path tempDir;

	private long env;
	private int dbi;

	@BeforeEach
	void setUp() throws Exception {
		env = createEnvironment(tempDir);
		dbi = openDatabase(env, "spoc");
		for (int i = 0; i < 100; i++) {
			insertQuad(new QuadKey(i, 1L, i + 100L, 0L));
		}
	}

	@AfterEach
	void tearDown() {
		if (env != 0) {
			mdb_env_close(env);
		}
	}

	@Test
	void evaluateDoesNotSpawnBackgroundThread() throws Exception {
		try (MemoryStack stack = stackPush()) {
			long txn = beginReadTransaction(stack);
			TxnManager txnManager = new TxnManager(env, TxnManager.Mode.NONE);
			TxnManager.Txn txnRef = txnManager.createTxn(txn);

			ValueStore valueStore = Mockito.mock(ValueStore.class);
			LmdbValue lmdbValue = Mockito.mock(LmdbValue.class);
			Mockito.when(valueStore.getValue(Mockito.anyLong())).thenReturn(lmdbValue);

			LmdbDatasetSnapshot snapshot = new SimpleSnapshot(txnRef, Map.of(SPOC, dbi), valueStore);

			LmdbWCOJ wcoj = new LmdbWCOJ(
					List.of(new StatementPattern(new Var("s"), new Var("p"), new Var("o"))));

			LmdbWCOJStep step = new LmdbWCOJStep(wcoj,
					List.of(snapshot),
					new QueryEvaluationContext.Minimal((Literal) null, (Dataset) null, (Comparator<Value>) null),
					ignored -> wcoj,
					null);

			CloseableIteration<BindingSet> iteration = step.evaluate(null);
			try {
				boolean workerStarted = waitForThread(WORKER_THREAD_NAME, 500);
				assertFalse(workerStarted, "WCOJ evaluation should run on the calling thread without spawning workers");
			} finally {
				iteration.close();
				mdb_txn_abort(txn);
			}
		}
	}

	private boolean waitForThread(String name, long timeoutMillis) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
		while (System.nanoTime() < deadline) {
			for (Thread thread : Thread.getAllStackTraces().keySet()) {
				if (name.equals(thread.getName())) {
					return true;
				}
			}
			Thread.sleep(10);
		}
		return false;
	}

	private long beginReadTransaction(MemoryStack stack) throws IOException {
		PointerBuffer txnPtr = stack.mallocPointer(1);
		assertSuccess(mdb_txn_begin(env, MemoryUtil.NULL, MDB_RDONLY, txnPtr));
		return txnPtr.get(0);
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

	private void assertSuccess(int rc) throws IOException {
		if (rc != MDB_SUCCESS && rc != MDB_NOTFOUND) {
			throw new IOException(mdb_strerror(rc));
		}
	}

	private static final class SimpleSnapshot implements LmdbDatasetSnapshot {

		private final TxnManager.Txn txn;
		private final Map<QuadKeyOrder, Integer> indexHandles;
		private final ValueStore valueStore;

		SimpleSnapshot(TxnManager.Txn txn, Map<QuadKeyOrder, Integer> indexHandles, ValueStore valueStore) {
			this.txn = txn;
			this.indexHandles = indexHandles;
			this.valueStore = valueStore;
		}

		@Override
		public TxnManager.Txn getTxn() {
			return txn;
		}

		@Override
		public Map<QuadKeyOrder, Integer> indexHandles() {
			return indexHandles;
		}

		@Override
		public ValueStore valueStore() {
			return valueStore;
		}

		@Override
		public boolean isExplicit() {
			return true;
		}
	}
}
