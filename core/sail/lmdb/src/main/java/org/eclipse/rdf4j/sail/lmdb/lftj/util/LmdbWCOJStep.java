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
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.sail.lmdb.lftj.LFTJExecutor;
import org.eclipse.rdf4j.sail.lmdb.lftj.LmdbWCOJ;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadPattern;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadPatternTerm;
import org.eclipse.rdf4j.sail.lmdb.lftj.Slot;

/**
 * Query evaluation step that executes an {@link LmdbWCOJ} using {@link LFTJExecutor}.
 */
public class LmdbWCOJStep implements QueryEvaluationStep {

	private final LmdbWCOJ wcoj;
	private final List<LmdbDatasetSnapshot> snapshots;
	private final QueryEvaluationContext context;
	private final Function<LmdbWCOJ, TupleExpr> rebuildJoin;
	private final EvaluationStrategy strategy;

	public LmdbWCOJStep(LmdbWCOJ wcoj, List<LmdbDatasetSnapshot> snapshots, QueryEvaluationContext context,
			Function<LmdbWCOJ, TupleExpr> rebuildJoin,
			EvaluationStrategy strategy) {
		this.wcoj = wcoj;
		this.snapshots = List.copyOf(Objects.requireNonNull(snapshots, "snapshots"));
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

		List<BindingSet> converted = new ArrayList<>();
		for (LmdbDatasetSnapshot snapshot : snapshots) {
			ValueStoreFacade valueStore = new ValueStoreFacade(snapshot);
			List<QuadPattern> quadPatterns = toQuadPatterns(wcoj.getPatterns(), valueStore);
			LFTJExecutor executor = new LFTJExecutor(valueStore.txnId(), snapshot.indexHandles());

			List<Map<String, Long>> raw;
			try {
				raw = executor.evaluate(quadPatterns);
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}

			for (Map<String, Long> row : raw) {
				converted.add(toBindingSet(row, valueStore));
			}
		}

		return new CloseableIteratorIteration<>(converted.iterator()) {
			@Override
			public BindingSet next() {
				return super.next();
			}
		};
	}

	private MapBindingSet toBindingSet(Map<String, Long> row, ValueStoreFacade valueStore)
			throws QueryEvaluationException {
		MapBindingSet bs = new MapBindingSet(row.size());
		for (Map.Entry<String, Long> entry : row.entrySet()) {
			try {
				bs.addBinding(entry.getKey(), valueStore.getValue(entry.getValue()));
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
		}
		return bs;
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

	private QuadPatternTerm toTerm(Var var, ValueStoreFacade valueStore)
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
		private final LmdbDatasetSnapshot snapshot;

		ValueStoreFacade(LmdbDatasetSnapshot snapshot) {
			this.snapshot = snapshot;
		}

		long txnId() throws QueryEvaluationException {
			try {
				return snapshot.getTxn().get();
			} catch (Exception e) {
				throw new QueryEvaluationException(e);
			}
		}

		long getId(Value value) throws QueryEvaluationException {
			try {
				return snapshot.valueStore().getId(value);
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
		}

		Value getValue(long id) throws IOException {
			return snapshot.valueStore().getValue(id);
		}

	}
}
