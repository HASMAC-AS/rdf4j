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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.sail.lmdb.lftj.LFTJExecutor;
import org.eclipse.rdf4j.sail.lmdb.lftj.LmdbWCOJ;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyOrder;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadPattern;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadPatternTerm;
import org.eclipse.rdf4j.sail.lmdb.lftj.Slot;

/**
 * Query evaluation step that executes an {@link LmdbWCOJ} using {@link LFTJExecutor}.
 */
public class LmdbWCOJStep implements QueryEvaluationStep {

	private final LmdbWCOJ wcoj;
	private final Object snapshot;
	private final QueryEvaluationContext context;
	private final Function<LmdbWCOJ, TupleExpr> rebuildJoin;
	private final org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy strategy;

	public LmdbWCOJStep(LmdbWCOJ wcoj, Object snapshot, QueryEvaluationContext context,
			Function<LmdbWCOJ, TupleExpr> rebuildJoin,
			org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy strategy) {
		this.wcoj = wcoj;
		this.snapshot = snapshot;
		this.context = context;
		this.rebuildJoin = rebuildJoin;
		this.strategy = strategy;
	}

	@Override
	public CloseableIteratorIteration<BindingSet> evaluate(BindingSet bindings) throws QueryEvaluationException {
		if (bindings != null && !bindings.isEmpty()) {
			// fall back to the standard join pipeline for bound input
			TupleExpr delegate = rebuildJoin.apply(wcoj);
			QueryEvaluationStep fallback = strategy.precompile(delegate, context);
			return new CloseableIteratorIteration<>(fallback.evaluate(bindings));
		}

		ValueStoreFacade valueStore = new ValueStoreFacade(snapshot);
		List<QuadPattern> quadPatterns = toQuadPatterns(wcoj.getPatterns(), valueStore);
		LFTJExecutor executor = new LFTJExecutor(valueStore.txnId(), valueStore.indexHandles());

		List<Map<String, Long>> raw;
		try {
			raw = executor.evaluate(quadPatterns);
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}

		List<BindingSet> converted = new ArrayList<>(raw.size());
		for (Map<String, Long> row : raw) {
			MapBindingSet bs = new MapBindingSet(row.size());
			for (Map.Entry<String, Long> entry : row.entrySet()) {
				try {
					bs.addBinding(entry.getKey(), valueStore.getValue(entry.getValue()));
				} catch (IOException e) {
					throw new QueryEvaluationException(e);
				}
			}
			converted.add(bs);
		}

		return new CloseableIteratorIteration<>(converted.iterator()) {
			@Override
			public BindingSet next() {
				return super.next();
			}
		};
	}

	private List<QuadPattern> toQuadPatterns(List<StatementPattern> patterns, ValueStoreFacade valueStore)
			throws QueryEvaluationException {
		List<QuadPattern> quadPatterns = new ArrayList<>(patterns.size());
		for (StatementPattern pattern : patterns) {
			quadPatterns.add(QuadPattern.of(
					toTerm(pattern.getSubjectVar(), valueStore),
					toTerm(pattern.getPredicateVar(), valueStore),
					toTerm(pattern.getObjectVar(), valueStore),
					toTerm(pattern.getContextVar(), valueStore)));
		}
		return quadPatterns;
	}

	private QuadPatternTerm toTerm(org.eclipse.rdf4j.query.algebra.Var var, ValueStoreFacade valueStore)
			throws QueryEvaluationException {
		if (var == null) {
			return QuadPatternTerm.unbound();
		}
		if (var.hasValue()) {
			return QuadPatternTerm.constant(valueStore.getId(var.getValue()));
		}
		if (var.getName() != null) {
			return QuadPatternTerm.variable(var.getName());
		}
		return QuadPatternTerm.unbound();
	}

	private static final class ValueStoreFacade {
		private final Object snapshot;

		ValueStoreFacade(Object snapshot) {
			this.snapshot = snapshot;
		}

		long txnId() throws QueryEvaluationException {
			Object txn = invoke(snapshot, "getTxn");
			Object id = invoke(txn, "get");
			return ((Number) id).longValue();
		}

		@SuppressWarnings("unchecked")
		Map<QuadKeyOrder, Integer> indexHandles() throws QueryEvaluationException {
			return (Map<QuadKeyOrder, Integer>) invoke(snapshot, "indexHandles");
		}

		long getId(Value value) throws QueryEvaluationException {
			Object valueStore = valueStore();
			Object id = invoke(valueStore, "getId", Value.class, value);
			return ((Number) id).longValue();
		}

		org.eclipse.rdf4j.model.Value getValue(long id) throws IOException {
			try {
				Object valueStore = valueStore();
				return (org.eclipse.rdf4j.model.Value) invoke(valueStore, "getValue", long.class, id);
			} catch (QueryEvaluationException e) {
				throw new IOException(e);
			}
		}

		private Object valueStore() throws QueryEvaluationException {
			return invoke(snapshot, "valueStore");
		}

		private Object invoke(Object target, String method, Class<?> paramType, Object arg)
				throws QueryEvaluationException {
			return invoke(target, method, new Class<?>[] { paramType }, new Object[] { arg });
		}

		private Object invoke(Object target, String method) throws QueryEvaluationException {
			return invoke(target, method, new Class<?>[0], new Object[0]);
		}

		private Object invoke(Object target, String method, Class<?>[] paramTypes, Object[] args)
				throws QueryEvaluationException {
			if (target == null) {
				throw new QueryEvaluationException("LMDB snapshot unavailable for WCOJ evaluation");
			}
			try {
				var m = target.getClass().getMethod(method, paramTypes);
				m.setAccessible(true);
				return m.invoke(target, args);
			} catch (Exception e) {
				throw new QueryEvaluationException(e);
			}
		}
	}
}
