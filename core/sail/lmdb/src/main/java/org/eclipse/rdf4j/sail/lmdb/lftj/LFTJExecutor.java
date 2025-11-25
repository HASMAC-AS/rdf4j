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
		PrecompiledPatterns precompiled = precompile(patterns, chosenOrders, variableOrder);
		long[] values = new long[variableOrder.size()];
		boolean[] bound = new boolean[variableOrder.size()];
		IteratorPool iteratorPool = new IteratorPool();
		try {
			recurse(variableOrder, precompiled.participatingByVariable(), 0, precompiled.patternMetadata(),
					iteratorPool,
					values, bound, consumer);
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

	private void recurse(List<String> variableOrder, List<VarParticipation>[] participatingByVariable, int depth,
			PatternMetadata[] patternMetadata, IteratorPool iteratorPool, long[] values, boolean[] bound,
			Consumer<Map<String, Long>> consumer) throws IOException {
		if (depth >= variableOrder.size()) {
			consumer.accept(toResult(variableOrder, values, bound));
			return;
		}

		List<VarParticipation> participating = participatingByVariable[depth];
		if (participating == null || participating.isEmpty()) {
			throw new IllegalArgumentException("Variable not found in any pattern: " + variableOrder.get(depth));
		}

		List<CloseableTrieIterator> iterators = new ArrayList<>(participating.size());
		for (VarParticipation participation : participating) {
			PatternMetadata metadata = patternMetadata[participation.patternIndex];
			QuadKeyOrder order = metadata.order;
			Integer dbi = indexMapping.get(order);
			if (dbi == null) {
				throw new IllegalStateException("No DBI registered for order " + order);
			}
			Prefix prefix = buildPrefix(metadata, depth, values, bound);
			CloseableTrieIterator iterator = iteratorPool.acquire(dbi.intValue(), order, participation.slot);
			iterator.open(prefix);
			if (iterator.atEnd()) {
				iteratorPool.release(iterator);
				return;
			}
			iterators.add(iterator);
		}

		try {
			leapfrog(iterators, value -> {
				values[depth] = value;
				bound[depth] = true;
				try {
					recurse(variableOrder, participatingByVariable, depth + 1, patternMetadata, iteratorPool, values,
							bound, consumer);
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
		bound[depth] = false;
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

	private PrecompiledPatterns precompile(List<QuadPattern> patterns, Map<QuadPattern, QuadKeyOrder> chosenOrders,
			List<String> variableOrder) {
		Map<String, Integer> variableIndex = new HashMap<>(variableOrder.size());
		for (int i = 0; i < variableOrder.size(); i++) {
			variableIndex.put(variableOrder.get(i), i);
		}

		PatternMetadata[] metadata = new PatternMetadata[patterns.size()];
		@SuppressWarnings("unchecked")
		List<VarParticipation>[] participating = new List[variableOrder.size()];

		for (int i = 0; i < patterns.size(); i++) {
			QuadPattern pattern = patterns.get(i);
			QuadKeyOrder order = chosenOrders.get(pattern);
			SlotDescriptor[] slots = new SlotDescriptor[Slot.values().length];
			for (Slot slot : Slot.values()) {
				QuadPatternTerm term = pattern.term(slot);
				if (term.isConstant()) {
					slots[slot.ordinal()] = SlotDescriptor.constant(slot, term.constant());
					continue;
				}
				if (term.isVariable()) {
					Integer varId = variableIndex.get(term.variable());
					if (varId == null) {
						throw new IllegalArgumentException("Variable not present in order: " + term.variable());
					}
					slots[slot.ordinal()] = SlotDescriptor.variable(slot, varId.intValue());
					participating[varId.intValue()] = addParticipation(participating[varId.intValue()], i, slot);
					continue;
				}
				slots[slot.ordinal()] = SlotDescriptor.unbound(slot);
			}
			metadata[i] = new PatternMetadata(order, slots);
		}

		for (int i = 0; i < participating.length; i++) {
			if (participating[i] == null) {
				participating[i] = List.of();
			}
		}

		return new PrecompiledPatterns(metadata, participating);
	}

	private List<VarParticipation> addParticipation(List<VarParticipation> existing, int patternIndex, Slot slot) {
		List<VarParticipation> list = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
		list.add(new VarParticipation(patternIndex, slot));
		return list;
	}

	private Prefix buildPrefix(PatternMetadata metadata, int currentVarIndex, long[] values, boolean[] bound) {
		Prefix.Builder prefix = Prefix.builder();
		for (SlotDescriptor descriptor : metadata.slots) {
			if (descriptor.isConstant()) {
				write(prefix, descriptor.slot(), descriptor.constant());
				continue;
			}
			if (descriptor.isVariable() && descriptor.variableId() < currentVarIndex
					&& bound[descriptor.variableId()]) {
				write(prefix, descriptor.slot(), values[descriptor.variableId()]);
			}
		}
		return prefix.build();
	}

	private Map<String, Long> toResult(List<String> variableOrder, long[] values, boolean[] bound) {
		Map<String, Long> result = new HashMap<>(variableOrder.size());
		for (int i = 0; i < variableOrder.size(); i++) {
			if (bound[i]) {
				result.put(variableOrder.get(i), values[i]);
			}
		}
		return result;
	}

	private void write(Prefix.Builder prefix, Slot slot, long value) {
		switch (slot) {
		case S:
			prefix.subject(value);
			break;
		case P:
			prefix.predicate(value);
			break;
		case O:
			prefix.object(value);
			break;
		case C:
			prefix.context(value);
			break;
		default:
			throw new IllegalArgumentException("Unknown slot: " + slot);
		}
	}

	private static final class PrecompiledPatterns {
		private final PatternMetadata[] patternMetadata;
		private final List<VarParticipation>[] participatingByVariable;

		PrecompiledPatterns(PatternMetadata[] patternMetadata, List<VarParticipation>[] participatingByVariable) {
			this.patternMetadata = patternMetadata;
			this.participatingByVariable = participatingByVariable;
		}

		PatternMetadata[] patternMetadata() {
			return patternMetadata;
		}

		List<VarParticipation>[] participatingByVariable() {
			return participatingByVariable;
		}
	}

	private static final class PatternMetadata {
		private final QuadKeyOrder order;
		private final SlotDescriptor[] slots;

		PatternMetadata(QuadKeyOrder order, SlotDescriptor[] slots) {
			this.order = order;
			this.slots = slots;
		}
	}

	private static final class VarParticipation {
		private final int patternIndex;
		private final Slot slot;

		VarParticipation(int patternIndex, Slot slot) {
			this.patternIndex = patternIndex;
			this.slot = slot;
		}
	}

	private static final class SlotDescriptor {
		private final Slot slot;
		private final boolean constant;
		private final boolean variable;
		private final long constantValue;
		private final int variableId;

		private SlotDescriptor(Slot slot, boolean constant, boolean variable, long constantValue, int variableId) {
			this.slot = slot;
			this.constant = constant;
			this.variable = variable;
			this.constantValue = constantValue;
			this.variableId = variableId;
		}

		static SlotDescriptor constant(Slot slot, long value) {
			return new SlotDescriptor(slot, true, false, value, -1);
		}

		static SlotDescriptor variable(Slot slot, int varId) {
			return new SlotDescriptor(slot, false, true, 0L, varId);
		}

		static SlotDescriptor unbound(Slot slot) {
			return new SlotDescriptor(slot, false, false, 0L, -1);
		}

		Slot slot() {
			return slot;
		}

		boolean isConstant() {
			return constant;
		}

		boolean isVariable() {
			return variable;
		}

		long constant() {
			return constantValue;
		}

		int variableId() {
			return variableId;
		}
	}
}
