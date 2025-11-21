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
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
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
	private final boolean useCompact;
	private final QueryEvaluationStep fallback;
	private final List<String> varOrder;
	private final WcojStrategy strategy;
	private final RingPlan ringPlan;
	private final boolean ringEnabled;
	private final boolean trackPartial;
	private static final AtomicReference<Metrics> LAST_METRICS = new AtomicReference<>();

	public enum WcojStrategy {
		LTJ,
		RING,
		AUTO
	}

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

	private static final class CompactJoinCursor implements JoinCursor {
		private final TrieNavigator delegate;

		CompactJoinCursor(TrieNavigator delegate) {
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
		this.useCompact = dataset.useCompactTrie();
		this.varOrder = computeVarOrder(patterns);
		this.strategy = resolveStrategy();
		this.ringPlan = buildRingPlan(patterns, varOrder);
		this.ringEnabled = strategy == WcojStrategy.RING || (strategy == WcojStrategy.AUTO && ringPlan.cyclic);
		this.trackPartial = Boolean.getBoolean("rdf4j.lmdb.wcoj.trackPartial");
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		if (trieIndexManager == null || txnManager == null || LmdbEvaluationStrategy.hasActiveConnectionChanges()) {
			return fallback != null ? fallback.evaluate(bindings) : new EmptyIteration<>();
		}
		if (dataset.hasTransactionChanges()) {
			return fallback != null ? fallback.evaluate(bindings) : new EmptyIteration<>();
		}
		if (requiresDatasetContextFilter()) {
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

		if (!prefixDependenciesSatisfied(bound)) {
			return fallback != null ? fallback.evaluate(bindings) : new EmptyIteration<>();
		}

		Metrics metrics = trackPartial ? new Metrics() : null;

		try {
			Txn txn = txnManager.createReadTxn();
			if (metrics != null) {
				metrics.strategy = ringEnabled ? WcojStrategy.RING : WcojStrategy.LTJ;
				metrics.patternCount = patterns.size();
				metrics.varCount = ringEnabled ? ringPlan.ring.size() : varOrder.size();
			}
			if (ringEnabled) {
				RingIteration iteration = new RingIteration(bound, bindings, txn, metrics);
				if (metrics != null) {
					LAST_METRICS.set(metrics);
				}
				return iteration;
			}
			WcojIteration iteration = new WcojIteration(bound, bindings, txn, metrics);
			if (metrics != null) {
				LAST_METRICS.set(metrics);
			}
			return iteration;
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private WcojStrategy resolveStrategy() {
		String raw = System.getProperty("rdf4j.lmdb.wcoj.strategy", "auto").toLowerCase(Locale.ROOT);
		switch (raw) {
		case "ring":
			return WcojStrategy.RING;
		case "auto":
		case "detect":
			return WcojStrategy.AUTO;
		default:
			return WcojStrategy.LTJ;
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

			if (useCompact) {
				try {
					JoinCursor compact = openCompactCursor(bestIndex, prefix, wantExplicit, wantInferred);
					if (compact != null && !compact.atEnd()) {
						return compact;
					}
					if (compact != null) {
						compact.close();
					}
				} catch (IOException | RuntimeException e) {
					// fall back to legacy backend silently if compact tries are unavailable or corrupt
				}
			}

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

	private JoinCursor openCompactCursor(String perm, long[] prefix, boolean wantExplicit, boolean wantInferred)
			throws IOException {
		TrieNavigator explicitNav = null;
		TrieNavigator inferredNav = null;

		if (wantExplicit) {
			CompactTrieReader.LoadedTrie trie = dataset.getCompactTrie(perm, true);
			if (trie != null) {
				explicitNav = new CompactTrieNavigator(trie);
				if (!explicitNav.openPrefix(prefix)) {
					explicitNav.close();
					explicitNav = null;
				}
			}
		}

		if (wantInferred) {
			CompactTrieReader.LoadedTrie trie = dataset.getCompactTrie(perm, false);
			if (trie != null) {
				inferredNav = new CompactTrieNavigator(trie);
				if (!inferredNav.openPrefix(prefix)) {
					inferredNav.close();
					inferredNav = null;
				}
			}
		}

		if (explicitNav == null && inferredNav == null) {
			return null;
		}
		if (explicitNav != null && inferredNav != null) {
			return new UnionJoinCursor(new CompactJoinCursor(explicitNav), new CompactJoinCursor(inferredNav));
		}
		if (explicitNav != null) {
			return new CompactJoinCursor(explicitNav);
		}
		return new CompactJoinCursor(inferredNav);
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
		Map<String, Double> bestCost = new HashMap<>();
		Map<String, Integer> degree = new HashMap<>();
		int seq = 0;
		Map<String, Integer> firstSeen = new HashMap<>();
		for (StatementPattern p : patterns) {
			double cost = p.getCostEstimate();
			for (Var v : new Var[] { p.getSubjectVar(), p.getPredicateVar(), p.getObjectVar(), p.getContextVar() }) {
				if (v != null && !v.hasValue()) {
					String name = v.getName();
					bestCost.merge(name, cost, Math::min);
					degree.merge(name, 1, Integer::sum);
					firstSeen.putIfAbsent(name, seq++);
				}
			}
		}
		List<String> indexes = getIndexNames();
		Map<String, Set<String>> dependencies = new HashMap<>();
		for (StatementPattern pattern : patterns) {
			for (Var v : new Var[] { pattern.getSubjectVar(), pattern.getPredicateVar(), pattern.getObjectVar(),
					pattern.getContextVar() }) {
				if (v != null && !v.hasValue()) {
					String name = v.getName();
					Set<String> deps = minimalDependencies(pattern, name, indexes);
					if (!deps.isEmpty()) {
						dependencies.computeIfAbsent(name, k -> new LinkedHashSet<>()).addAll(deps);
					}
				}
			}
		}

		Comparator<String> heuristic = (a, b) -> {
			double ca = bestCost.getOrDefault(a, Double.MAX_VALUE);
			double cb = bestCost.getOrDefault(b, Double.MAX_VALUE);
			int cmp = Double.compare(ca, cb);
			if (cmp != 0) {
				return cmp;
			}
			cmp = Integer.compare(degree.getOrDefault(b, 0), degree.getOrDefault(a, 0));
			if (cmp != 0) {
				return cmp;
			}
			return Integer.compare(firstSeen.getOrDefault(a, 0), firstSeen.getOrDefault(b, 0));
		};

		List<String> order = new ArrayList<>();
		Set<String> remaining = new LinkedHashSet<>(bestCost.keySet());
		Set<String> placed = new HashSet<>();
		while (!remaining.isEmpty()) {
			List<String> ready = new ArrayList<>();
			for (String candidate : remaining) {
				Set<String> deps = dependencies.getOrDefault(candidate, Collections.emptySet());
				if (placed.containsAll(deps)) {
					ready.add(candidate);
				}
			}
			if (ready.isEmpty()) {
				String pick = remaining.stream().min(heuristic).orElseThrow();
				order.add(pick);
				placed.add(pick);
				remaining.remove(pick);
			} else {
				ready.sort(heuristic);
				String pick = ready.get(0);
				order.add(pick);
				placed.add(pick);
				remaining.remove(pick);
			}
		}
		return order;
	}

	private RingPlan buildRingPlan(List<StatementPattern> patterns, List<String> baseOrder) {
		List<String> ring = new ArrayList<>(baseOrder);
		if (ring.isEmpty()) {
			Set<String> vars = new LinkedHashSet<>();
			for (StatementPattern p : patterns) {
				for (Var v : new Var[] { p.getSubjectVar(), p.getPredicateVar(), p.getObjectVar(),
						p.getContextVar() }) {
					if (v != null && !v.hasValue()) {
						vars.add(v.getName());
					}
				}
			}
			ring.addAll(vars);
		}

		Map<String, Set<String>> adjacency = buildAdjacency(patterns);
		boolean cyclic = hasCycle(adjacency);

		Map<String, Integer> indexOf = new HashMap<>();
		for (int i = 0; i < ring.size(); i++) {
			indexOf.put(ring.get(i), i);
		}
		List<List<StatementPattern>> incident = new ArrayList<>(ring.size());
		for (int i = 0; i < ring.size(); i++) {
			incident.add(new ArrayList<>());
		}
		for (StatementPattern pattern : patterns) {
			for (Var v : new Var[] { pattern.getSubjectVar(), pattern.getPredicateVar(), pattern.getObjectVar(),
					pattern.getContextVar() }) {
				if (v != null && !v.hasValue()) {
					Integer idx = indexOf.get(v.getName());
					if (idx != null) {
						incident.get(idx).add(pattern);
					}
				}
			}
		}
		return new RingPlan(ring, indexOf, incident, cyclic);
	}

	private Map<String, Set<String>> buildAdjacency(List<StatementPattern> patterns) {
		Map<String, Set<String>> adj = new HashMap<>();
		for (StatementPattern pattern : patterns) {
			List<String> vars = new ArrayList<>(4);
			addVarIfNeeded(vars, pattern.getSubjectVar());
			addVarIfNeeded(vars, pattern.getPredicateVar());
			addVarIfNeeded(vars, pattern.getObjectVar());
			addVarIfNeeded(vars, pattern.getContextVar());
			for (int i = 0; i < vars.size(); i++) {
				for (int j = i + 1; j < vars.size(); j++) {
					String a = vars.get(i);
					String b = vars.get(j);
					adj.computeIfAbsent(a, k -> new LinkedHashSet<>()).add(b);
					adj.computeIfAbsent(b, k -> new LinkedHashSet<>()).add(a);
				}
			}
		}
		return adj;
	}

	private boolean hasCycle(Map<String, Set<String>> adj) {
		Set<String> visited = new HashSet<>();
		for (String node : adj.keySet()) {
			if (visited.contains(node)) {
				continue;
			}
			if (dfsHasCycle(node, null, adj, visited)) {
				return true;
			}
		}
		return false;
	}

	private boolean dfsHasCycle(String node, String parent, Map<String, Set<String>> adj, Set<String> visited) {
		visited.add(node);
		for (String nei : adj.getOrDefault(node, Set.of())) {
			if (nei.equals(parent)) {
				continue;
			}
			if (visited.contains(nei)) {
				return true;
			}
			if (dfsHasCycle(nei, node, adj, visited)) {
				return true;
			}
		}
		return false;
	}

	private static final class RingPlan {
		final List<String> ring;
		final Map<String, Integer> indexOf;
		final List<List<StatementPattern>> incident;
		final boolean cyclic;

		RingPlan(List<String> ring, Map<String, Integer> indexOf, List<List<StatementPattern>> incident,
				boolean cyclic) {
			this.ring = ring;
			this.indexOf = indexOf;
			this.incident = incident;
			this.cyclic = cyclic;
		}
	}

	private void addVarIfNeeded(List<String> vars, Var v) {
		if (v != null && !v.hasValue()) {
			String name = v.getName();
			if (!vars.contains(name)) {
				vars.add(name);
			}
		}
	}

	private void closeAll(List<JoinCursor> cursors) {
		for (JoinCursor c : cursors) {
			try {
				c.close();
			} catch (Exception ignore) {
			}
		}
	}

	private List<String> getIndexNames() {
		List<String> indexes = trieIndexManager != null ? trieIndexManager.getIndexNames() : null;
		if (indexes == null || indexes.isEmpty()) {
			return java.util.List.of("spoc");
		}
		return indexes;
	}

	private Set<String> minimalDependencies(StatementPattern pattern, String varName, List<String> indexes) {
		Set<String> best = null;
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
			Set<String> deps = new LinkedHashSet<>();
			for (int i = 0; i < position && i < 3; i++) {
				Var v = componentVar(pattern, order[i]);
				if (v == null) {
					continue;
				}
				long c = constantId(v);
				if (c != LmdbValue.UNKNOWN_ID) {
					continue;
				}
				if (!v.hasValue()) {
					deps.add(v.getName());
				}
			}
			if (best == null || deps.size() < best.size()) {
				best = deps;
			}
		}
		return best == null ? Collections.emptySet() : best;
	}

	private boolean requiresDatasetContextFilter() {
		if (context == null) {
			return false;
		}
		Dataset ds = context.getDataset();
		if (ds == null) {
			return false;
		}
		return (ds.getDefaultGraphs() != null && !ds.getDefaultGraphs().isEmpty())
				|| (ds.getNamedGraphs() != null && !ds.getNamedGraphs().isEmpty());
	}

	private boolean prefixDependenciesSatisfied(Map<String, Long> bound) {
		Set<String> available = new HashSet<>(bound.keySet());
		for (String var : varOrder) {
			if (!varDependenciesSatisfied(var, available)) {
				return false;
			}
			available.add(var);
		}
		return true;
	}

	private boolean varDependenciesSatisfied(String var, Set<String> available) {
		for (StatementPattern pattern : patterns) {
			if (usesVar(pattern, var) && !hasUsableIndex(pattern, var, available)) {
				return false;
			}
		}
		return true;
	}

	private boolean hasUsableIndex(StatementPattern pattern, String varName, Set<String> available) {
		for (String idx : getIndexNames()) {
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
			boolean ok = true;
			for (int i = 0; i < position && i < 3; i++) {
				Var v = componentVar(pattern, order[i]);
				long c = constantId(v);
				if (c != LmdbValue.UNKNOWN_ID) {
					continue;
				}
				if (v != null && !v.hasValue()) {
					if (!available.contains(v.getName())) {
						ok = false;
						break;
					}
				}
			}
			if (ok) {
				return true;
			}
		}
		return false;
	}

	static Metrics pollLastMetrics() {
		return LAST_METRICS.getAndSet(null);
	}

	static final class Metrics {
		private long partialBindings;
		private WcojStrategy strategy;
		private int varCount;
		private int patternCount;
		private long framesPushed;

		void recordBinding() {
			partialBindings++;
		}

		void recordPush() {
			framesPushed++;
		}

		long getPartialBindings() {
			return partialBindings;
		}

		WcojStrategy getStrategy() {
			return strategy;
		}

		long getFramesPushed() {
			return framesPushed;
		}
	}

	private final class RingIteration extends AbstractCloseableIteration<BindingSet> {

		private final BindingSet incoming;
		private final Txn txn;
		private final Map<String, Long> bound;
		private final Pool pool;
		private final Deque<RingFrame> stack = new ArrayDeque<>();
		private BindingSet next;
		private boolean exhausted = false;
		private final Metrics metrics;

		RingIteration(Map<String, Long> seed, BindingSet incoming, Txn txn, Metrics metrics) {
			this.bound = new HashMap<>(seed);
			this.incoming = incoming;
			this.txn = txn;
			this.pool = Pool.get();
			this.metrics = metrics;
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
					if (stack.size() == ringPlan.ring.size()) {
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
			if (idx >= ringPlan.ring.size()) {
				return false;
			}
			String var = ringPlan.ring.get(idx);
			RingDomain domain = openDomain(var);
			if (domain == null || !domain.hasCurrent()) {
				if (domain != null) {
					domain.close();
				}
				return false;
			}
			if (domain.fixed) {
				if (!hasForwardSupport(idx)) {
					domain.close();
					return false;
				}
				if (metrics != null) {
					metrics.recordBinding();
				}
				stack.push(new RingFrame(var, domain, idx));
				if (metrics != null) {
					metrics.recordPush();
				}
				return true;
			}
			while (true) {
				bound.put(var, domain.current());
				if (metrics != null) {
					metrics.recordBinding();
				}
				if (hasForwardSupport(idx)) {
					stack.push(new RingFrame(var, domain, idx));
					if (metrics != null) {
						metrics.recordPush();
					}
					return true;
				}
				bound.remove(var);
				if (!domain.advance()) {
					domain.close();
					return false;
				}
			}
		}

		private boolean advance() throws IOException {
			while (!stack.isEmpty()) {
				RingFrame frame = stack.peek();
				if (frame.domain.fixed) {
					frame.close();
					stack.pop();
					continue;
				}
				if (frame.domain.advance()) {
					bound.put(frame.var, frame.domain.current());
					if (metrics != null) {
						metrics.recordBinding();
					}
					if (hasForwardSupport(frame.index)) {
						return true;
					}
					bound.remove(frame.var);
					continue;
				}
				bound.remove(frame.var);
				frame.close();
				stack.pop();
			}
			return false;
		}

		private RingDomain openDomain(String var) throws IOException {
			boolean isPreBound = bound.containsKey(var);
			long preBoundValue = isPreBound ? bound.get(var) : -1L;
			List<JoinCursor> cursors = new ArrayList<>();
			List<StatementPattern> related = ringPlan.incident.get(ringPlan.indexOf.get(var));
			for (StatementPattern pattern : related) {
				JoinCursor cursor = openCursorForVar(pattern, var, bound, txn.get(), pool);
				if (cursor == null || cursor.atEnd()) {
					closeAll(cursors);
					return null;
				}
				if (isPreBound) {
					if (cursor.key() != preBoundValue && !cursor.seek(preBoundValue)) {
						closeAll(cursors);
						return null;
					}
					if (cursor.atEnd() || cursor.key() != preBoundValue) {
						closeAll(cursors);
						return null;
					}
				}
				cursors.add(cursor);
			}
			if (isPreBound) {
				return new RingDomain(var, cursors, null, preBoundValue, true);
			}
			LeapfrogIterator iterator = new LeapfrogIterator(cursors);
			if (!iterator.hasCurrent()) {
				closeAll(cursors);
				return null;
			}
			return new RingDomain(var, cursors, iterator, iterator.current(), false);
		}

		private boolean hasForwardSupport(int currentIdx) throws IOException {
			if (ringPlan.ring.isEmpty()) {
				return false;
			}
			if (stack.size() == ringPlan.ring.size()) {
				return true;
			}
			int nextIdx = (currentIdx + 1) % ringPlan.ring.size();
			if (nextIdx < stack.size()) {
				return true; // already bound
			}
			String nextVar = ringPlan.ring.get(nextIdx);
			RingDomain probe = openDomain(nextVar);
			if (probe == null || !probe.hasCurrent()) {
				if (probe != null) {
					probe.close();
				}
				return false;
			}
			probe.close();
			return true;
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
				RingFrame frame = stack.pop();
				frame.close();
			}
		}
	}

	private final class WcojIteration extends AbstractCloseableIteration<BindingSet> {

		private final BindingSet incoming;
		private final Txn txn;
		private final Map<String, Long> bound;
		private final Pool pool;
		private final Metrics metrics;
		private final Deque<Frame> stack = new ArrayDeque<>();
		private BindingSet next;
		private boolean exhausted = false;

		WcojIteration(Map<String, Long> seed, BindingSet incoming, Txn txn, Metrics metrics) {
			this.bound = new HashMap<>(seed);
			this.incoming = incoming;
			this.txn = txn;
			this.pool = Pool.get();
			this.metrics = metrics;
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
				if (metrics != null) {
					metrics.recordBinding();
				}
				stack.push(new Frame(var, cursors, null, true));
				if (metrics != null) {
					metrics.recordPush();
				}
				return true;
			}
			LeapfrogIterator iterator = new LeapfrogIterator(cursors);
			if (!iterator.hasCurrent()) {
				closeAll(cursors);
				return false;
			}
			bound.put(var, iterator.current());
			if (metrics != null) {
				metrics.recordBinding();
			}
			stack.push(new Frame(var, cursors, iterator, false));
			if (metrics != null) {
				metrics.recordPush();
			}
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
					if (metrics != null) {
						metrics.recordBinding();
					}
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

	private static final class RingFrame {
		final String var;
		final RingDomain domain;
		final int index;

		RingFrame(String var, RingDomain domain, int index) {
			this.var = var;
			this.domain = domain;
			this.index = index;
		}

		void close() {
			domain.close();
		}
	}

	private static final class RingDomain {
		final String var;
		final List<JoinCursor> cursors;
		final LeapfrogIterator iterator;
		final boolean fixed;
		private long current;

		RingDomain(String var, List<JoinCursor> cursors, LeapfrogIterator iterator, long current, boolean fixed) {
			this.var = var;
			this.cursors = cursors;
			this.iterator = iterator;
			this.current = current;
			this.fixed = fixed;
		}

		boolean hasCurrent() {
			if (fixed) {
				return true;
			}
			return iterator != null && iterator.hasCurrent();
		}

		long current() {
			return current;
		}

		boolean advance() throws IOException {
			if (fixed || iterator == null) {
				return false;
			}
			Long nextVal = iterator.advance();
			if (nextVal == null) {
				return false;
			}
			current = nextVal;
			return true;
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
