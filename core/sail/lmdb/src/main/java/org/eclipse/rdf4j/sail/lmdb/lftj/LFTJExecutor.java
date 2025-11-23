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

	public LFTJExecutor(long txn, Map<QuadKeyOrder, Integer> indexMapping) {
		this.txn = txn;
		this.indexMapping = Map.copyOf(Objects.requireNonNull(indexMapping, "indexMapping"));
		if (this.indexMapping.isEmpty()) {
			throw new IllegalArgumentException("At least one index must be provided");
		}
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
		recurse(variableOrder, variableOrderIndex, participating, 0, chosenOrders, new HashMap<>(), consumer);
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
			Map<QuadPattern, QuadKeyOrder> chosenOrders, Map<String, Long> bindings,
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
		List<LMDBTrieIterator> iterators = new ArrayList<>(participating.size());
		try {
			for (QuadPattern pattern : participating) {
				QuadKeyOrder order = chosenOrders.get(pattern);
				Integer dbi = indexMapping.get(order);
				if (dbi == null) {
					throw new IllegalStateException("No DBI registered for order " + order);
				}
				Slot slot = pattern.variableSlots().get(variable);
				Prefix prefix = PrefixBuilder.buildPrefix(pattern, variableOrderIndex, currentIndex, bindings);
				LMDBTrieIterator iterator = new LMDBTrieIterator(txn, dbi.intValue(), order, slot);
				iterator.open(prefix);
				if (iterator.atEnd()) {
					iterator.close();
					return;
				}
				iterators.add(iterator);
			}

			try {
				leapfrog(iterators, value -> {
					bindings.put(variable, value);
					try {
						recurse(variableOrder, variableOrderIndex, participatingByVariable, depth + 1, chosenOrders,
								bindings, consumer);
					} catch (IOException e) {
						throw new EvaluationException(e);
					}
				});
			} catch (EvaluationException e) {
				throw e.ioException;
			}
			bindings.remove(variable);
		} finally {
			for (LMDBTrieIterator iterator : iterators) {
				iterator.close();
			}
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

	private void leapfrog(List<LMDBTrieIterator> iterators, LongConsumer consumer) {
		LeapfrogIteratorCursor cursor = new LeapfrogIteratorCursor(iterators);
		while (cursor.hasValue()) {
			consumer.accept(cursor.current());
			cursor.advance();
		}
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
