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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.sail.lmdb.LmdbWCOJStep;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.lftj.LmdbWCOJ;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyOrder;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LmdbWCOJStepBindingSetTest {

	@Test
	void usesContextBindingSetAndSetters() throws Exception {
		TrackingContext context = new TrackingContext();
		LmdbWCOJStep step = new LmdbWCOJStep(null,
				List.of(), context, w -> null, Mockito.mock(EvaluationStrategy.class));

		List<String> variableOrder = List.of("s", "o");
		long[] values = new long[] { 1L, 2L };
		boolean[] present = new boolean[] { true, true };

		ValueStore valueStore = mock(ValueStore.class);
		LmdbValue value1 = mock(LmdbValue.class);
		LmdbValue value2 = mock(LmdbValue.class);
		when(valueStore.getValue(1L)).thenReturn(value1);
		when(valueStore.getValue(2L)).thenReturn(value2);

		LmdbDatasetSnapshot snapshot = new LmdbDatasetSnapshot() {
			@Override
			public Txn getTxn() {
				Txn txn = mock(Txn.class);
				when(txn.get()).thenReturn(0L);
				return txn;
			}

			@Override
			public Map<QuadKeyOrder, Integer> indexHandles() {
				return Map.of();
			}

			@Override
			public ValueStore valueStore() {
				return valueStore;
			}

			@Override
			public boolean isExplicit() {
				return true;
			}
		};

		Class<?> facadeClass = Class.forName("org.eclipse.rdf4j.sail.lmdb.LmdbWCOJStep$ValueStoreFacade");
		Object facade = facadeClass.getDeclaredConstructor(LmdbDatasetSnapshot.class).newInstance(snapshot);

		Method toBindingSet = LmdbWCOJStep.class.getDeclaredMethod("toBindingSet", List.class, long[].class,
				boolean[].class, facadeClass);
		toBindingSet.setAccessible(true);

		BindingSet result = (BindingSet) toBindingSet.invoke(step, variableOrder, values, present, facade);

		assertThat(result).isInstanceOf(TrackingBindingSet.class);
		assertThat(result.getValue("s")).isSameAs(value1);
		assertThat(result.getValue("o")).isSameAs(value2);
		assertThat(context.requestedSetters).containsExactlyInAnyOrder("s", "o");
		assertThat(context.appliedSetters).containsExactlyInAnyOrder("s", "o");
	}

	private static final class TrackingBindingSet extends MapBindingSet {
	}

	private static final class TrackingContext implements QueryEvaluationContext {
		private final List<String> requestedSetters = new ArrayList<>();
		private final List<String> appliedSetters = new ArrayList<>();

		@Override
		public MutableBindingSet createBindingSet() {
			return new TrackingBindingSet();
		}

		@Override
		public BiConsumer<Value, MutableBindingSet> setBinding(String variableName) {
			requestedSetters.add(variableName);
			return (value, bs) -> {
				appliedSetters.add(variableName);
				bs.setBinding(variableName, value);
			};
		}

		@Override
		public Dataset getDataset() {
			return null;
		}

		@Override
		public org.eclipse.rdf4j.model.Literal getNow() {
			return null;
		}
	}
}
