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
package org.eclipse.rdf4j.sail.lmdb.lftj;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * Executes Leapfrog Triejoin over LMDB-backed quad indexes.
 */
public final class LFTJExecutor {

	private final long txn;

	private final Map<QuadKeyOrder, Integer> indexMapping;

	private final TrieIteratorProvider iteratorProvider;

	public LFTJExecutor(long txn, Map<QuadKeyOrder, Integer> indexMapping) {
		this(txn, indexMapping, defaultIteratorProvider());
	}

	LFTJExecutor(long txn, Map<QuadKeyOrder, Integer> indexMapping, TrieIteratorProvider iteratorProvider) {
		this.txn = txn;
		this.indexMapping = Map.copyOf(Objects.requireNonNull(indexMapping, "indexMapping"));
		if (this.indexMapping.isEmpty()) {
			throw new IllegalArgumentException("At least one index must be provided");
		}
		this.iteratorProvider = Objects.requireNonNull(iteratorProvider, "iteratorProvider");
	}

	public List<Map<String, Long>> evaluate(List<QuadPattern> patterns) throws IOException {
		List<Map<String, Long>> results = new ArrayList<>();
		evaluate(patterns, results::add);
		return results;
	}

	public void evaluate(List<QuadPattern> patterns, Consumer<Map<String, Long>> consumer) throws IOException {
		Objects.requireNonNull(patterns, "patterns");
		Objects.requireNonNull(consumer, "consumer");
		List<String> variableOrder = chooseVariableOrder(patterns, indexMapping.keySet());
		Map<QuadPattern, QuadKeyOrder> chosenOrders = chooseOrders(patterns, variableOrder);
		Map<String, Integer> variableOrderIndex = indexVariableOrder(variableOrder);
		Map<String, List<QuadPattern>> participating = groupByVariable(patterns);
		IteratorPool iteratorPool = new IteratorPool();
		try {
			recurse(variableOrder, variableOrderIndex, participating, 0, chosenOrders, iteratorPool, new HashMap<>(),
					consumer);
		} finally {
			iteratorPool.closeAll();
		}
	}

	public static List<String> chooseVariableOrder(List<QuadPattern> patterns,
			Collection<QuadKeyOrder> availableOrders) {
		Objects.requireNonNull(patterns, "patterns");
		Collection<QuadKeyOrder> orders = availableOrders == null ? List.of() : availableOrders;

		Map<String, VariableScore> scores = new HashMap<>();
		for (QuadPattern pattern : patterns) {
			Map<String, Slot> variableSlots = pattern.variableSlots();
			for (Map.Entry<String, Slot> entry : variableSlots.entrySet()) {
				String variable = entry.getKey();
				Slot slot = entry.getValue();
				VariableScore score = scores.computeIfAbsent(variable, v -> new VariableScore());
				int bestPosition = bestPosition(slot, orders);
				score.record(bestPosition);
			}
		}

		List<String> order = new ArrayList<>(scores.keySet());
		order.sort((left, right) -> {
			VariableScore a = scores.get(left);
			VariableScore b = scores.get(right);
			int cmp = Integer.compare(b.leadingMatches, a.leadingMatches);
			if (cmp != 0) {
				return cmp;
			}
			cmp = Double.compare(a.averagePosition(), b.averagePosition());
			if (cmp != 0) {
				return cmp;
			}
			cmp = Integer.compare(b.occurrences, a.occurrences);
			if (cmp != 0) {
				return cmp;
			}
			return left.compareTo(right);
		});
		return order;
	}

	private static int bestPosition(Slot slot, Collection<QuadKeyOrder> orders) {
		int best = Integer.MAX_VALUE;
		for (QuadKeyOrder order : orders) {
			int position = order.indexOf(slot);
			if (position >= 0 && position < best) {
				best = position;
			}
		}
		return best;
	}

	private Map<QuadPattern, QuadKeyOrder> chooseOrders(List<QuadPattern> patterns, List<String> variableOrder) {
		List<QuadKeyOrder> candidates = new ArrayList<>(indexMapping.keySet());
		Map<QuadPattern, QuadKeyOrder> chosen = new HashMap<>();
		for (QuadPattern pattern : patterns) {
			QuadKeyOrder order = IndexSelector.chooseBestOrder(pattern, variableOrder, candidates);
			chosen.put(pattern, order);
		}
		return chosen;
	}

	private void recurse(List<String> variableOrder, Map<String, Integer> variableOrderIndex,
			Map<String, List<QuadPattern>> participatingByVariable, int depth,
			Map<QuadPattern, QuadKeyOrder> chosenOrders, IteratorPool iteratorPool, Map<String, Long> bindings,
			Consumer<Map<String, Long>> consumer) throws IOException {
		if (depth >= variableOrder.size()) {
			consumer.accept(Map.copyOf(bindings));
			return;
		}

		String variable = variableOrder.get(depth);
		List<QuadPattern> participating = participatingByVariable.get(variable);
		if (participating == null || participating.isEmpty()) {
			throw new IllegalArgumentException("Variable not found in any pattern: " + variable);
		}
		int currentIndex = variableOrderIndex.get(variable);
		List<CloseableTrieIterator> iterators = new ArrayList<>(participating.size());
		for (QuadPattern pattern : participating) {
			QuadKeyOrder order = chosenOrders.get(pattern);
			Integer dbi = indexMapping.get(order);
			if (dbi == null) {
				throw new IllegalStateException("No DBI registered for order " + order);
			}
			Slot slot = pattern.variableSlots().get(variable);
			Prefix prefix = PrefixBuilder.buildPrefix(pattern, variableOrderIndex, currentIndex, bindings);
			CloseableTrieIterator iterator = iteratorPool.acquire(dbi.intValue(), order, slot);
			iterator.open(prefix);
			if (iterator.atEnd()) {
				iteratorPool.release(iterator);
				return;
			}
			iterators.add(iterator);
		}

		try {
			leapfrog(iterators, value -> {
				bindings.put(variable, value);
				try {
					recurse(variableOrder, variableOrderIndex, participatingByVariable, depth + 1, chosenOrders,
							iteratorPool, bindings, consumer);
				} catch (IOException e) {
					throw new EvaluationException(e);
				}
			});
		} catch (EvaluationException e) {
			throw e.ioException;
		}
		for (CloseableTrieIterator iterator : iterators) {
			iteratorPool.release(iterator);
		}
		bindings.remove(variable);
	}

	private final class IteratorPool {
		private final Map<PoolKey, ArrayDeque<CloseableTrieIterator>> pool = new HashMap<>();

		CloseableTrieIterator acquire(int dbi, QuadKeyOrder order, Slot slot) throws IOException {
			PoolKey key = new PoolKey(dbi, order, slot);
			ArrayDeque<CloseableTrieIterator> deque = pool.get(key);
			if (deque != null) {
				CloseableTrieIterator iterator = deque.pollFirst();
				if (iterator != null) {
					return iterator;
				}
			}
			return iteratorProvider.create(txn, dbi, order, slot);
		}

		void release(CloseableTrieIterator iterator) {
			PoolKey key = new PoolKey(iterator.slotDbi(), iterator.slotOrder(), iterator.slot());
			pool.computeIfAbsent(key, k -> new ArrayDeque<>()).addFirst(iterator);
		}

		void closeAll() {
			for (ArrayDeque<CloseableTrieIterator> deque : pool.values()) {
				for (CloseableTrieIterator iterator : deque) {
					iterator.close();
				}
			}
			pool.clear();
		}
	}

	private static final class PoolKey {
		private final int dbi;
		private final QuadKeyOrder order;
		private final Slot slot;

		PoolKey(int dbi, QuadKeyOrder order, Slot slot) {
			this.dbi = dbi;
			this.order = order;
			this.slot = slot;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			PoolKey other = (PoolKey) obj;
			return dbi == other.dbi && order.equals(other.order) && slot == other.slot;
		}

		@Override
		public int hashCode() {
			return Objects.hash(dbi, order, slot);
		}
	}

	private static final class IteratorKey {
		private final QuadPattern pattern;
		private final Slot slot;

		IteratorKey(QuadPattern pattern, Slot slot) {
			this.pattern = Objects.requireNonNull(pattern, "pattern");
			this.slot = Objects.requireNonNull(slot, "slot");
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			IteratorKey other = (IteratorKey) obj;
			return pattern.equals(other.pattern) && slot == other.slot;
		}

		@Override
		public int hashCode() {
			return Objects.hash(pattern, slot);
		}
	}

	private static final class EvaluationException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private final IOException ioException;

		EvaluationException(IOException ioException) {
			super(ioException);
			this.ioException = ioException;
		}
	}

	private static final class VariableScore {
		private static final int DEFAULT_POSITION = 4;
		private int occurrences;
		private int leadingMatches;
		private int positionSum;

		void record(int bestPosition) {
			occurrences++;
			if (bestPosition == 0) {
				leadingMatches++;
			}
			positionSum += bestPosition == Integer.MAX_VALUE ? DEFAULT_POSITION : bestPosition;
		}

		double averagePosition() {
			if (occurrences == 0) {
				return DEFAULT_POSITION;
			}
			return (double) positionSum / occurrences;
		}
	}

	private void leapfrog(List<CloseableTrieIterator> iterators, LongConsumer consumer) {
		LeapfrogIteratorCursor cursor = new LeapfrogIteratorCursor(iterators);
		while (cursor.hasValue()) {
			consumer.accept(cursor.current());
			cursor.advance();
		}
	}

	@FunctionalInterface
	interface TrieIteratorProvider {
		CloseableTrieIterator create(long txn, int dbi, QuadKeyOrder order, Slot slot) throws IOException;
	}

	private static TrieIteratorProvider defaultIteratorProvider() {
		return (txn, dbi, order, slot) -> new LMDBTrieIterator(txn, dbi, order, slot);
	}

	private Map<String, Integer> indexVariableOrder(List<String> variableOrder) {
		Map<String, Integer> index = new HashMap<>(variableOrder.size());
		for (int i = 0; i < variableOrder.size(); i++) {
			index.put(variableOrder.get(i), i);
		}
		return index;
	}

	private Map<String, List<QuadPattern>> groupByVariable(List<QuadPattern> patterns) {
		Map<String, List<QuadPattern>> mapping = new HashMap<>();
		for (QuadPattern pattern : patterns) {
			for (String variable : pattern.variables()) {
				mapping.computeIfAbsent(variable, v -> new ArrayList<>()).add(pattern);
			}
		}
		return mapping;
	}
}
