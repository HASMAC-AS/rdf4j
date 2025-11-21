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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Minimal leapfrog-triejoin evaluator for pure BGPs using the LMDB trie indexes.
 * <p>
 * Planning keeps things lightweight: it picks a trie index per pattern based on available constants and builds a
 * variable order that respects index dependencies so each leapfrog step can use the most selective prefix available.
 */
class LmdbWcojBGPQueryEvaluationStep implements QueryEvaluationStep {

	private final List<StatementPattern> patterns;
	private final QueryEvaluationContext context;
	private final LmdbEvaluationDataset dataset;
	private final TrieIndexManager trieIndexManager;
	private final TxnManager txnManager;
	private final ValueStore valueStore;
	private final QueryEvaluationStep fallback;
	private final List<String> varOrder;

	private interface JoinCursor extends AutoCloseable {
		boolean next() throws IOException;

		boolean seek(long target) throws IOException;

		long key();

		boolean atEnd();

		@Override
		void close();
	}

	private static final class SingleJoinCursor implements JoinCursor {
		private final TrieLevelCursor delegate;

		SingleJoinCursor(TrieLevelCursor delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean next() throws IOException {
			return delegate.next();
		}

		@Override
		public boolean seek(long target) throws IOException {
			return delegate.seek(target);
		}

		@Override
		public long key() {
			return delegate.key();
		}

		@Override
		public boolean atEnd() {
			return delegate.atEnd();
		}

		@Override
		public void close() {
			delegate.close();
		}
	}

	private static final class UnionJoinCursor implements JoinCursor {
		private final JoinCursor explicit;
		private final JoinCursor inferred;
		private long current;
		private boolean atEnd;

		UnionJoinCursor(JoinCursor explicit, JoinCursor inferred) throws IOException {
			this.explicit = explicit;
			this.inferred = inferred;
			advanceCurrent();
		}

		private void advanceCurrent() throws IOException {
			long expKey = explicit.atEnd() ? Long.MAX_VALUE : explicit.key();
			long infKey = inferred.atEnd() ? Long.MAX_VALUE : inferred.key();
			if (expKey == Long.MAX_VALUE && infKey == Long.MAX_VALUE) {
				atEnd = true;
				current = -1;
				return;
			}
			current = Math.min(expKey, infKey);
			atEnd = false;
		}

		@Override
		public boolean next() throws IOException {
			if (atEnd) {
				return false;
			}
			long prev = current;
			if (!explicit.atEnd() && explicit.key() == prev) {
				explicit.next();
			}
			if (!inferred.atEnd() && inferred.key() == prev) {
				inferred.next();
			}
			advanceCurrent();
			return !atEnd;
		}

		@Override
		public boolean seek(long target) throws IOException {
			if (atEnd) {
				return false;
			}
			if (current >= target) {
				return true;
			}
			if (!explicit.atEnd()) {
				explicit.seek(target);
			}
			if (!inferred.atEnd()) {
				inferred.seek(target);
			}
			advanceCurrent();
			return !atEnd;
		}

		@Override
		public long key() {
			return current;
		}

		@Override
		public boolean atEnd() {
			return atEnd;
		}

		@Override
		public void close() {
			try {
				explicit.close();
			} finally {
				inferred.close();
			}
		}
	}

	LmdbWcojBGPQueryEvaluationStep(List<StatementPattern> patterns, QueryEvaluationContext context,
			LmdbEvaluationDataset dataset, TrieIndexManager trieIndexManager, TxnManager txnManager,
			QueryEvaluationStep fallback) {
		this.patterns = patterns;
		this.context = context;
		this.dataset = dataset;
		this.trieIndexManager = trieIndexManager;
		this.txnManager = txnManager;
		this.valueStore = dataset.getValueStore();
		this.fallback = fallback;
		this.varOrder = computeVarOrder(patterns);
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		if (trieIndexManager == null || txnManager == null || LmdbEvaluationStrategy.hasActiveConnectionChanges()) {
			return fallback != null ? fallback.evaluate(bindings) : new EmptyIteration<>();
		}
		if (dataset.hasTransactionChanges()) {
			return fallback != null ? fallback.evaluate(bindings) : new EmptyIteration<>();
		}

		Map<String, Long> bound = new HashMap<>();
		for (String name : bindings.getBindingNames()) {
			Value v = bindings.getValue(name);
			try {
				long id = valueStore.getId(v);
				if (id == LmdbValue.UNKNOWN_ID) {
					return new EmptyIteration<>();
				}
				bound.put(name, id);
			} catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
		}

		try {
			Txn txn = txnManager.createReadTxn();
			return new WcojIteration(bound, bindings, txn);
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private JoinCursor openCursorForVar(StatementPattern pattern, String varName, Map<String, Long> bound,
			long txn, Pool pool) throws QueryEvaluationException {
		List<String> indexes = trieIndexManager.getIndexNames();
		if (indexes == null || indexes.isEmpty()) {
			indexes = java.util.List.of("spoc");
		}
		String bestIndex = null;
		char[] bestOrder = null;
		int bestPrefix = -1;
		for (String idx : indexes) {
			char[] order = idx.toCharArray();
			int position = -1;
			for (int i = 0; i < order.length; i++) {
				Var v = componentVar(pattern, order[i]);
				if (v != null && !v.hasValue() && varName.equals(v.getName())) {
					position = i;
					break;
				}
			}
			if (position < 0) {
				continue;
			}
			int usablePrefix = 0;
			boolean ok = true;
			for (int i = 0; i < position && i < 3; i++) {
				Var v = componentVar(pattern, order[i]);
				long c = constantId(v);
				if (c != LmdbValue.UNKNOWN_ID) {
					usablePrefix++;
					continue;
				}
				if (v != null && !v.hasValue()) {
					Long b = bound.get(v.getName());
					if (b == null) {
						ok = false;
						break;
					}
					usablePrefix++;
				}
			}
			if (ok && usablePrefix > bestPrefix) {
				bestPrefix = usablePrefix;
				bestIndex = idx;
				bestOrder = order;
			}
		}
		if (bestIndex == null) {
			return null;
		}
		int pos = -1;
		for (int i = 0; i < bestOrder.length; i++) {
			Var v = componentVar(pattern, bestOrder[i]);
			if (v != null && !v.hasValue() && varName.equals(v.getName())) {
				pos = i;
				break;
			}
		}
		long[] prefix = new long[Math.min(pos, 3)];
		for (int i = 0; i < prefix.length; i++) {
			Var v = componentVar(pattern, bestOrder[i]);
			long val = constantId(v);
			if (val == LmdbValue.UNKNOWN_ID) {
				Long b = bound.get(v.getName());
				if (b == null) {
					return null;
				}
				val = b;
			}
			prefix[i] = val;
		}
		int level = pos == 0 ? 1 : Math.min(pos, 3);
		try {
			LmdbEvaluationDataset.DatasetMode mode = dataset.getDatasetMode();
			boolean wantExplicit = mode != LmdbEvaluationDataset.DatasetMode.INFERRED;
			boolean wantInferred = mode != LmdbEvaluationDataset.DatasetMode.EXPLICIT;
			boolean iterateKeys = prefix.length == 0 && level == 1;

			TrieLevelCursor explicitCursor = null;
			TrieLevelCursor inferredCursor = null;
			try {
				if (wantExplicit) {
					explicitCursor = trieIndexManager.openCursor(bestIndex, level, true, txn, pool, null);
					explicitCursor.openPrefix(iterateKeys, prefix);
				}
				if (wantInferred) {
					inferredCursor = trieIndexManager.openCursor(bestIndex, level, false, txn, pool, null);
					inferredCursor.openPrefix(iterateKeys, prefix);
				}

				if (explicitCursor != null && inferredCursor != null) {
					return new UnionJoinCursor(new SingleJoinCursor(explicitCursor),
							new SingleJoinCursor(inferredCursor));
				}
				if (explicitCursor != null) {
					return new SingleJoinCursor(explicitCursor);
				}
				if (inferredCursor != null) {
					return new SingleJoinCursor(inferredCursor);
				}
				return null;
			} catch (IOException | RuntimeException e) {
				if (explicitCursor != null) {
					explicitCursor.close();
				}
				if (inferredCursor != null) {
					inferredCursor.close();
				}
				throw e;
			}
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private long constantId(Var v) {
		if (v == null || !v.hasValue()) {
			return LmdbValue.UNKNOWN_ID;
		}
		try {
			return valueStore.getId(v.getValue());
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private Var componentVar(StatementPattern pattern, char component) {
		switch (component) {
		case 's':
			return pattern.getSubjectVar();
		case 'p':
			return pattern.getPredicateVar();
		case 'o':
			return pattern.getObjectVar();
		case 'c':
			return pattern.getContextVar();
		default:
			return null;
		}
	}

	private boolean usesVar(StatementPattern p, String name) {
		return varEquals(p.getSubjectVar(), name) || varEquals(p.getPredicateVar(), name)
				|| varEquals(p.getObjectVar(), name) || varEquals(p.getContextVar(), name);
	}

	private boolean varEquals(Var v, String name) {
		return v != null && !v.hasValue() && name.equals(v.getName());
	}

	private BindingSet materialize(Map<String, Long> bound, BindingSet incoming) {
		MapBindingSet bset = new MapBindingSet();
		for (String name : incoming.getBindingNames()) {
			bset.addBinding(name, incoming.getValue(name));
		}
		for (Map.Entry<String, Long> e : bound.entrySet()) {
			if (bset.hasBinding(e.getKey())) {
				continue;
			}
			try {
				Value v = valueStore.getLazyValue(e.getValue());
				bset.addBinding(e.getKey(), v);
			} catch (IOException io) {
				throw new QueryEvaluationException(io);
			}
		}
		return bset;
	}

	private List<String> computeVarOrder(List<StatementPattern> patterns) {
		Map<String, Integer> firstSeen = new LinkedHashMap<>();
		Map<String, Integer> degree = new HashMap<>();
		int seq = 0;
		for (StatementPattern p : patterns) {
			for (Var v : new Var[] { p.getSubjectVar(), p.getPredicateVar(), p.getObjectVar(), p.getContextVar() }) {
				if (v != null && !v.hasValue()) {
					String name = v.getName();
					firstSeen.putIfAbsent(name, seq++);
					degree.merge(name, 1, Integer::sum);
				}
			}
		}
		List<String> vars = new ArrayList<>(firstSeen.keySet());
		vars.sort((a, b) -> {
			int da = degree.getOrDefault(a, 0);
			int db = degree.getOrDefault(b, 0);
			if (da != db) {
				return Integer.compare(db, da);
			}
			return Integer.compare(firstSeen.get(a), firstSeen.get(b));
		});
		return vars;
	}

	private void closeAll(List<JoinCursor> cursors) {
		for (JoinCursor c : cursors) {
			try {
				c.close();
			} catch (Exception ignore) {
			}
		}
	}

	private final class WcojIteration extends AbstractCloseableIteration<BindingSet> {

		private final BindingSet incoming;
		private final Txn txn;
		private final Map<String, Long> bound;
		private final Pool pool;
		private final Deque<Frame> stack = new ArrayDeque<>();
		private BindingSet next;
		private boolean exhausted = false;

		WcojIteration(Map<String, Long> seed, BindingSet incoming, Txn txn) {
			this.bound = new HashMap<>(seed);
			this.incoming = incoming;
			this.txn = txn;
			this.pool = Pool.get();
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			if (exhausted || isClosed()) {
				return false;
			}
			fetchNext();
			return next != null;
		}

		@Override
		public BindingSet next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			BindingSet result = next;
			next = null;
			return result;
		}

		private void fetchNext() throws QueryEvaluationException {
			try {
				while (true) {
					if (stack.size() == varOrder.size()) {
						next = materialize(bound, incoming);
						if (!advance()) {
							exhausted = true;
							close();
						}
						return;
					}
					if (!descend()) {
						if (!advance()) {
							close();
							exhausted = true;
							return;
						}
					}
				}
			} catch (IOException e) {
				close();
				throw new QueryEvaluationException(e);
			}
		}

		private boolean descend() throws IOException {
			int idx = stack.size();
			if (idx >= varOrder.size()) {
				return false;
			}
			String var = varOrder.get(idx);
			boolean isPreBound = bound.containsKey(var);
			long preBoundValue = isPreBound ? bound.get(var) : -1L;
			List<JoinCursor> cursors = new ArrayList<>();
			for (StatementPattern pattern : patterns) {
				if (usesVar(pattern, var)) {
					JoinCursor cursor = openCursorForVar(pattern, var, bound, txn.get(), pool);
					if (cursor == null || cursor.atEnd()) {
						closeAll(cursors);
						return false;
					}
					if (isPreBound) {
						if (cursor.key() != preBoundValue && !cursor.seek(preBoundValue)) {
							closeAll(cursors);
							return false;
						}
						if (cursor.atEnd() || cursor.key() != preBoundValue) {
							closeAll(cursors);
							return false;
						}
					}
					cursors.add(cursor);
				}
			}
			if (isPreBound) {
				stack.push(new Frame(var, cursors, null, true));
				return true;
			}
			LeapfrogIterator iterator = new LeapfrogIterator(cursors);
			if (!iterator.hasCurrent()) {
				closeAll(cursors);
				return false;
			}
			bound.put(var, iterator.current());
			stack.push(new Frame(var, cursors, iterator, false));
			return true;
		}

		private boolean advance() throws IOException {
			while (!stack.isEmpty()) {
				Frame frame = stack.peek();
				if (frame.fixed) {
					frame.close();
					stack.pop();
					continue;
				}
				Long nextVal = frame.iterator.advance();
				if (nextVal != null) {
					bound.put(frame.var, nextVal);
					return true;
				}
				bound.remove(frame.var);
				frame.close();
				stack.pop();
			}
			return false;
		}

		@Override
		protected void handleClose() {
			closeAllFrames();
			if (txn != null) {
				txn.close();
			}
		}

		private void closeAllFrames() {
			while (!stack.isEmpty()) {
				Frame frame = stack.pop();
				frame.close();
			}
		}
	}

	private static final class Frame {
		final String var;
		final List<JoinCursor> cursors;
		final LeapfrogIterator iterator;
		final boolean fixed;

		Frame(String var, List<JoinCursor> cursors, LeapfrogIterator iterator, boolean fixed) {
			this.var = var;
			this.cursors = cursors;
			this.iterator = iterator;
			this.fixed = fixed;
		}

		void close() {
			for (JoinCursor cursor : cursors) {
				try {
					cursor.close();
				} catch (Exception ignore) {
				}
			}
		}
	}

	private static final class LeapfrogIterator {
		private final List<JoinCursor> cursors;
		private boolean hasCurrent;
		private long current;

		LeapfrogIterator(List<JoinCursor> cursors) throws IOException {
			this.cursors = cursors;
			this.hasCurrent = alignToIntersection();
		}

		boolean hasCurrent() {
			return hasCurrent;
		}

		long current() {
			return current;
		}

		Long advance() throws IOException {
			if (!hasCurrent) {
				return null;
			}
			if (!cursors.get(0).next()) {
				hasCurrent = false;
				return null;
			}
			if (!alignFrom(cursors.get(0).key())) {
				hasCurrent = false;
				return null;
			}
			return current;
		}

		private boolean alignToIntersection() throws IOException {
			if (cursors.isEmpty()) {
				return false;
			}
			long max = Long.MIN_VALUE;
			for (JoinCursor cursor : cursors) {
				if (cursor.atEnd()) {
					return false;
				}
				max = Math.max(max, cursor.key());
			}
			return alignFrom(max);
		}

		private boolean alignFrom(long start) throws IOException {
			long max = start;
			while (true) {
				boolean allEq = true;
				for (JoinCursor cursor : cursors) {
					while (!cursor.atEnd() && cursor.key() < max) {
						if (!cursor.seek(max)) {
							return false;
						}
					}
					if (cursor.atEnd()) {
						return false;
					}
					if (cursor.key() > max) {
						max = cursor.key();
						allEq = false;
					}
				}
				if (allEq) {
					current = max;
					return true;
				}
			}
		}
	}
}
