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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

	private static final int RESULT_QUEUE_CAPACITY = 64;
	private static final Object END_OF_RESULTS = new Object();

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

		BlockingQueue<Object> queue = new ArrayBlockingQueue<>(RESULT_QUEUE_CAPACITY);
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicBoolean cancelled = new AtomicBoolean(false);

		Thread worker = new Thread(() -> {
			try {
				for (LmdbDatasetSnapshot snapshot : snapshots) {
					if (cancelled.get()) {
						break;
					}

					Map<?, ?> indexHandles = snapshot.indexHandles();
					if (indexHandles == null || indexHandles.isEmpty()) {
						// No usable indexes: nothing to evaluate for this snapshot
						continue;
					}

					ValueStoreFacade valueStore = new ValueStoreFacade(snapshot);
					List<QuadPattern> quadPatterns = toQuadPatterns(wcoj.getPatterns(), valueStore);
					LFTJExecutor executor = new LFTJExecutor(valueStore.txnId(), snapshot.indexHandles());

					executor.evaluate(quadPatterns, row -> {
						if (cancelled.get()) {
							return;
						}
						MapBindingSet bs = toBindingSet(row, valueStore);
						put(queue, bs, cancelled);
					});
				}
			} catch (Throwable t) {
				error.compareAndSet(null, t);
			} finally {
				put(queue, END_OF_RESULTS, cancelled);
			}
		}, "rdf4j-lmdb-wcoj");
		worker.setDaemon(true);
		worker.start();

		return new StreamingWCOJIteration(queue, error, cancelled, worker);
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

	private static void put(BlockingQueue<Object> queue, Object value, AtomicBoolean cancelled) {
		if (cancelled.get()) {
			return;
		}
		boolean interrupted = false;
		try {
			while (!cancelled.get()) {
				try {
					queue.put(value);
					return;
				} catch (InterruptedException e) {
					interrupted = true;
					if (cancelled.get()) {
						return;
					}
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static final class BindingQueueIterator implements Iterator<BindingSet> {

		private final BlockingQueue<Object> queue;
		private final AtomicReference<Throwable> error;
		private final AtomicBoolean cancelled;

		private BindingSet next;
		private boolean done;

		BindingQueueIterator(BlockingQueue<Object> queue, AtomicReference<Throwable> error,
				AtomicBoolean cancelled) {
			this.queue = queue;
			this.error = error;
			this.cancelled = cancelled;
		}

		@Override
		public boolean hasNext() {
			if (done) {
				return false;
			}
			if (next != null) {
				return true;
			}
			while (true) {
				if (cancelled.get()) {
					done = true;
					queue.clear();
					return false;
				}
				Object item;
				try {
					item = queue.take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					done = true;
					throw new RuntimeException("Interrupted while waiting for WCOJ results", e);
				}
				if (item == END_OF_RESULTS) {
					done = true;
					Throwable t = error.get();
					if (t != null) {
						if (t instanceof RuntimeException) {
							throw (RuntimeException) t;
						}
						throw new RuntimeException("Error during WCOJ evaluation", t);
					}
					return false;
				}
				next = (BindingSet) item;
				return true;
			}
		}

		@Override
		public BindingSet next() {
			if (!hasNext()) {
				throw new NoSuchElementException("No more results");
			}
			BindingSet result = next;
			next = null;
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}

	private static final class StreamingWCOJIteration extends CloseableIteratorIteration<BindingSet> {

		private final BlockingQueue<Object> queue;
		private final AtomicBoolean cancelled;
		private final Thread worker;

		StreamingWCOJIteration(BlockingQueue<Object> queue, AtomicReference<Throwable> error,
				AtomicBoolean cancelled, Thread worker) {
			super(new BindingQueueIterator(queue, error, cancelled));
			this.queue = queue;
			this.cancelled = cancelled;
			this.worker = worker;
		}

		@Override
		protected void handleClose() {
			cancelled.set(true);
			queue.clear();
			worker.interrupt();
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
