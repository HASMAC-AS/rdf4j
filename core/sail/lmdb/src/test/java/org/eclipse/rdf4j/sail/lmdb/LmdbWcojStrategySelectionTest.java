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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LmdbWcojStrategySelectionTest {

	private final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void autoModeFallsBackOnAcyclicChain() throws Exception {
		LmdbWcojBGPQueryEvaluationStep step = buildStep(chainPatterns(), null);

		Method shouldUse = LmdbWcojBGPQueryEvaluationStep.class.getDeclaredMethod("shouldUseWcoj", Map.class);
		shouldUse.setAccessible(true);
		boolean useWcoj = (boolean) shouldUse.invoke(step, Map.of());

		Field ringEnabled = LmdbWcojBGPQueryEvaluationStep.class.getDeclaredField("ringEnabled");
		ringEnabled.setAccessible(true);
		boolean ring = ringEnabled.getBoolean(step);

		assertThat(useWcoj).isFalse();
		assertThat(ring).isFalse();
	}

	@Test
	void autoModeUsesWcojOnCycle() throws Exception {
		LmdbWcojBGPQueryEvaluationStep step = buildStep(cyclePatterns(), null);

		Method shouldUse = LmdbWcojBGPQueryEvaluationStep.class.getDeclaredMethod("shouldUseWcoj", Map.class);
		shouldUse.setAccessible(true);
		boolean useWcoj = (boolean) shouldUse.invoke(step, Map.of());

		Field ringEnabled = LmdbWcojBGPQueryEvaluationStep.class.getDeclaredField("ringEnabled");
		ringEnabled.setAccessible(true);
		boolean ring = ringEnabled.getBoolean(step);

		assertThat(useWcoj).isTrue();
		assertThat(ring).isTrue();
	}

	private List<StatementPattern> chainPatterns() {
		return List.of(new StatementPattern(var("a"), var("b"), var("c")),
				new StatementPattern(var("c"), var("d"), var("e")),
				new StatementPattern(var("e"), var("f"), var("g")),
				new StatementPattern(var("g"), var("h"), var("i")),
				new StatementPattern(var("i"), var("j"), var("k")));
	}

	private List<StatementPattern> cyclePatterns() {
		return List.of(new StatementPattern(var("a"), var("p1"), var("b")),
				new StatementPattern(var("b"), var("p2"), var("a")));
	}

	private Var var(String name) {
		return new Var(name);
	}

	private LmdbWcojBGPQueryEvaluationStep buildStep(List<StatementPattern> patterns, QueryEvaluationStep fallback)
			throws Exception {
		System.setProperty("net.bytebuddy.experimental", "true");

		ValueStore valueStore = Mockito.mock(ValueStore.class);
		Mockito.when(valueStore.getId(Mockito.any(Value.class))).thenReturn(1L);

		TrieIndexManager trieIndexManager = Mockito.mock(TrieIndexManager.class);
		Mockito.when(trieIndexManager.getIndexNames()).thenReturn(List.of("spoc"));

		Txn txn = Mockito.mock(TxnManager.Txn.class);
		Mockito.when(txn.get()).thenReturn(0L);

		TxnManager txnManager = Mockito.mock(TxnManager.class);
		Mockito.when(txnManager.createReadTxn()).thenReturn(txn);

		LmdbEvaluationDataset dataset = new LmdbEvaluationDataset() {
			@Override
			public RecordIterator getRecordIterator(StatementPattern pattern,
					org.eclipse.rdf4j.query.BindingSet bindings) {
				throw new UnsupportedOperationException();
			}

			@Override
			public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
					int ctxIndex, long[] patternIds) {
				throw new UnsupportedOperationException();
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

		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(null, vf, null);
		return new LmdbWcojBGPQueryEvaluationStep(patterns, context, dataset, trieIndexManager, txnManager,
				fallback);
	}
}
