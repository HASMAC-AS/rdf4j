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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
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

		AtomicBoolean cancelled = new AtomicBoolean(false);

		Iterator<BindingSet> iterator = new BindingIterator(cancelled);
		return new SingleThreadWCOJIteration(iterator, cancelled);
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

	private final class BindingIterator implements Iterator<BindingSet> {

		private final AtomicBoolean cancelled;

		private int snapshotIndex;
		private Iterator<BindingSet> current = Collections.emptyIterator();
		private RuntimeException error;

		BindingIterator(AtomicBoolean cancelled) {
			this.cancelled = cancelled;
		}

		@Override
		public boolean hasNext() {
			if (cancelled.get()) {
				return false;
			}
			if (error != null) {
				throw error;
			}
			while ((current == null || !current.hasNext()) && snapshotIndex < snapshots.size()) {
				if (cancelled.get()) {
					return false;
				}
				prepareNextIterator();
			}
			if (error != null) {
				throw error;
			}
			return current != null && current.hasNext();
		}

		@Override
		public BindingSet next() {
			if (!hasNext()) {
				throw new NoSuchElementException("No more results");
			}
			return current.next();
		}

		private void prepareNextIterator() {
			LmdbDatasetSnapshot snapshot = snapshots.get(snapshotIndex++);
			Map<?, ?> indexHandles = snapshot.indexHandles();
			if (indexHandles == null || indexHandles.isEmpty()) {
				current = Collections.emptyIterator();
				return;
			}

			ValueStoreFacade valueStore = new ValueStoreFacade(snapshot);
			List<QuadPattern> quadPatterns;
			try {
				quadPatterns = toQuadPatterns(wcoj.getPatterns(), valueStore);
			} catch (QueryEvaluationException e) {
				error = new RuntimeException(e);
				current = Collections.emptyIterator();
				return;
			}

			List<BindingSet> results = new ArrayList<>();
			try {
				LFTJExecutor executor = new LFTJExecutor(valueStore.txnId(), snapshot.indexHandles());
				executor.evaluate(quadPatterns, row -> {
					if (cancelled.get()) {
						throw new CancellationException("cancelled");
					}
					try {
						results.add(toBindingSet(row, valueStore));
					} catch (QueryEvaluationException e) {
						throw new RuntimeException(e);
					}
				});
			} catch (CancellationException e) {
				// stop processing this snapshot early
			} catch (IOException e) {
				error = new RuntimeException(e);
			} catch (RuntimeException e) {
				if (!(e instanceof CancellationException)) {
					error = e;
				}
			}
			current = results.iterator();
		}
	}

	private final class SingleThreadWCOJIteration extends CloseableIteratorIteration<BindingSet> {

		private final AtomicBoolean cancelled;

		SingleThreadWCOJIteration(Iterator<BindingSet> iterator, AtomicBoolean cancelled) {
			super(iterator);
			this.cancelled = cancelled;
		}

		@Override
		protected void handleClose() {
			cancelled.set(true);
		}
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
