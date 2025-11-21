/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;

class LmdbWcojForwardCheckCacheTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void reusesCachedForwardDomainInsteadOfReopening() throws Exception {
		System.setProperty("net.bytebuddy.experimental", "true");

		ValueStore valueStore = mock(ValueStore.class);
		when(valueStore.getId(any(Value.class))).thenAnswer(inv -> {
			Value v = inv.getArgument(0);
			String str = v.stringValue();
			if (str.startsWith("urn:id/")) {
				return Long.parseLong(str.substring(7));
			}
			return 1L;
		});
		LmdbValue stubValue = mock(LmdbValue.class);
		when(valueStore.getLazyValue(anyLong())).thenReturn(stubValue);

		TrieIndexManager trieIndexManager = mock(TrieIndexManager.class);
		when(trieIndexManager.getIndexNames()).thenReturn(List.of("spoc"));

		List<TrieLevelCursor> plannedCursors = List.of(
				cursorWithKeys(1L), cursorWithKeys(1L),
				cursorWithKeys(2L), cursorWithKeys(2L),
				cursorWithKeys(3L), cursorWithKeys(3L));
		AtomicInteger openCalls = new AtomicInteger();
		doAnswer(inv -> {
			int idx = openCalls.getAndIncrement();
			if (idx >= plannedCursors.size()) {
				throw new IllegalStateException("unexpected cursor open " + idx);
			}
			return plannedCursors.get(idx);
		}).when(trieIndexManager).openCursor(anyString(), anyInt(), anyBoolean(), anyLong(), any(), any());
		doAnswer(inv -> {
			int idx = openCalls.getAndIncrement();
			if (idx >= plannedCursors.size()) {
				throw new IllegalStateException("unexpected cursor open " + idx);
			}
			return plannedCursors.get(idx);
		}).when(trieIndexManager).openCursor(anyString(), anyInt(), anyBoolean(), anyLong());

		TxnManager txnManager = mock(TxnManager.class);
		Txn txn = mock(Txn.class);
		when(txn.get()).thenReturn(0L);
		when(txnManager.createReadTxn()).thenReturn(txn);

		LmdbEvaluationDataset dataset = new LmdbEvaluationDataset() {
			@Override
			public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings) {
				throw new UnsupportedOperationException("iterator path not used");
			}

			@Override
			public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
					int ctxIndex, long[] patternIds) {
				throw new UnsupportedOperationException("iterator path not used");
			}

			@Override
			public ValueStore getValueStore() {
				return valueStore;
			}

			@Override
			public TrieIndexManager getTrieIndexManager() {
				return trieIndexManager;
			}

			@Override
			public TxnManager getTxnManager() {
				return txnManager;
			}
		};

		StatementPattern p1 = new StatementPattern(new Var("a"), new Var("_p1", vf.createIRI("urn:p1")),
				new Var("b"));
		StatementPattern p2 = new StatementPattern(new Var("b"), new Var("_p2", vf.createIRI("urn:p2")),
				new Var("c"));
		StatementPattern p3 = new StatementPattern(new Var("c"), new Var("_p3", vf.createIRI("urn:p3")),
				new Var("a"));
		List<StatementPattern> patterns = List.of(p1, p2, p3);

		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(null, vf, null);
		QueryEvaluationStep fallback = bs -> {
			throw new AssertionError("fallback should not be used");
		};

		LmdbWcojBGPQueryEvaluationStep step = new LmdbWcojBGPQueryEvaluationStep(patterns, context, dataset,
				trieIndexManager, txnManager, fallback);

		MapBindingSet incoming = new MapBindingSet();
		incoming.addBinding("c", vf.createIRI("urn:id/3"));

		List<BindingSet> results = Iterations.asList(step.evaluate(incoming));

		int openings = openCalls.get();
		assertThat(openings).isEqualTo(plannedCursors.size());
		assertThat(results).isNotNull();
	}

	private TrieLevelCursor cursorWithKeys(long... keys) throws IOException {
		TrieLevelCursor cursor = mock(TrieLevelCursor.class);
		AtomicInteger pos = new AtomicInteger(keys.length == 0 ? 1 : 0);

		doAnswer(inv -> {
			pos.set(keys.length == 0 ? 1 : 0);
			return null;
		}).when(cursor).openPrefix(anyBoolean(), any(long[].class));
		doAnswer(inv -> {
			pos.set(keys.length == 0 ? 1 : 0);
			return null;
		}).when(cursor).openPrefix(any(long[].class));

		when(cursor.atEnd()).thenAnswer(inv -> pos.get() >= keys.length);
		when(cursor.key()).thenAnswer(inv -> keys[pos.get()]);
		when(cursor.seek(anyLong())).thenAnswer(inv -> {
			long target = inv.getArgument(0);
			int idx = pos.get();
			while (idx < keys.length && keys[idx] < target) {
				idx++;
			}
			pos.set(idx);
			return idx < keys.length;
		});
		when(cursor.next()).thenAnswer(inv -> {
			int idx = pos.incrementAndGet();
			return idx < keys.length;
		});
		return cursor;
	}
}
