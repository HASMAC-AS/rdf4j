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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
		Objects.requireNonNull(patterns, "patterns");
		List<String> variableOrder = chooseVariableOrder(patterns);
		Map<QuadPattern, QuadKeyOrder> chosenOrders = chooseOrders(patterns, variableOrder);
		List<Map<String, Long>> results = new ArrayList<>();
		recurse(variableOrder, 0, patterns, chosenOrders, new HashMap<>(), results);
		return results;
	}

	public static List<String> chooseVariableOrder(List<QuadPattern> patterns) {
		Objects.requireNonNull(patterns, "patterns");
		Map<String, Integer> frequency = new HashMap<>();
		for (QuadPattern pattern : patterns) {
			for (String variable : pattern.variables()) {
				frequency.merge(variable, 1, Integer::sum);
			}
		}
		List<String> order = new ArrayList<>(frequency.keySet());
		order.sort(Comparator.<String>comparingInt(var -> frequency.getOrDefault(var, 0))
				.reversed()
				.thenComparing(Comparator.naturalOrder()));
		return order;
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

	private void recurse(List<String> variableOrder, int depth, List<QuadPattern> patterns,
			Map<QuadPattern, QuadKeyOrder> chosenOrders, Map<String, Long> bindings,
			List<Map<String, Long>> results) throws IOException {
		if (depth >= variableOrder.size()) {
			results.add(Map.copyOf(bindings));
			return;
		}

		String variable = variableOrder.get(depth);
		List<QuadPattern> participating = patternsWithVariable(patterns, variable);
		List<LMDBTrieIterator> iterators = new ArrayList<>(participating.size());
		try {
			for (QuadPattern pattern : participating) {
				QuadKeyOrder order = chosenOrders.get(pattern);
				Integer dbi = indexMapping.get(order);
				if (dbi == null) {
					throw new IllegalStateException("No DBI registered for order " + order);
				}
				Slot slot = pattern.variableSlots().get(variable);
				Prefix prefix = PrefixBuilder.buildPrefix(pattern, variableOrder, variable, bindings);
				LMDBTrieIterator iterator = new LMDBTrieIterator(txn, dbi.intValue(), order, slot);
				iterator.open(prefix);
				if (iterator.atEnd()) {
					return;
				}
				iterators.add(iterator);
			}

			try {
				leapfrog(iterators, value -> {
					bindings.put(variable, value);
					try {
						recurse(variableOrder, depth + 1, patterns, chosenOrders, bindings, results);
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

	private void leapfrog(List<LMDBTrieIterator> iterators, LongConsumer consumer) {
		if (iterators.isEmpty()) {
			return;
		}
		for (TrieIterator iterator : iterators) {
			if (iterator.atEnd()) {
				return;
			}
		}
		iterators.sort(Comparator.comparingLong(TrieIterator::key));
		while (true) {
			long maxKey = iterators.get(iterators.size() - 1).key();
			boolean advanced = false;
			for (int i = 0; i < iterators.size() - 1; i++) {
				TrieIterator iterator = iterators.get(i);
				if (iterator.key() < maxKey) {
					iterator.seek(maxKey);
					advanced = true;
					if (iterator.atEnd()) {
						return;
					}
				}
			}

			if (!advanced) {
				consumer.accept(maxKey);
				for (TrieIterator iterator : iterators) {
					iterator.next();
					if (iterator.atEnd()) {
						return;
					}
				}
			}
			iterators.sort(Comparator.comparingLong(TrieIterator::key));
		}
	}

	private List<QuadPattern> patternsWithVariable(List<QuadPattern> patterns, String variable) {
		List<QuadPattern> participating = new ArrayList<>();
		for (QuadPattern pattern : patterns) {
			if (pattern.variables().contains(variable)) {
				participating.add(pattern);
			}
		}
		if (participating.isEmpty()) {
			throw new IllegalArgumentException("Variable not found in any pattern: " + variable);
		}
		return participating;
	}
}
