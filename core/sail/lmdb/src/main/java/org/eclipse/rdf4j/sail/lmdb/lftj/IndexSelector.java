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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chooses an index order for a pattern using the provided variable order as guidance.
 */
public final class IndexSelector {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexSelector.class);

	private static final List<QuadKeyOrder> ALL_ORDERS = allOrders();

	private IndexSelector() {
	}

	public static QuadKeyOrder chooseBestOrder(QuadPattern pattern, List<String> variableOrder,
			List<QuadKeyOrder> candidates) {
		Objects.requireNonNull(pattern, "pattern");
		Objects.requireNonNull(variableOrder, "variableOrder");
		Objects.requireNonNull(candidates, "candidates");
		if (candidates.isEmpty()) {
			throw new IllegalArgumentException("No candidate orders provided");
		}

		Map<String, Integer> orderIndex = indexMap(variableOrder);
		Map<String, Slot> variableSlots = pattern.variableSlots();
		if (variableSlots.isEmpty()) {
			return candidates.get(0);
		}

		QuadKeyOrder bestOrder = null;
		Compatibility bestScore = null;
		int bestIndex = Integer.MAX_VALUE;
		List<Evaluation> evaluations = new ArrayList<>(candidates.size());
		Evaluation chosenEvaluation = null;

		for (int i = 0; i < candidates.size(); i++) {
			QuadKeyOrder candidate = candidates.get(i);
			Compatibility compatibility = compatibilityScore(candidate, variableSlots, orderIndex);
			Evaluation evaluation = new Evaluation(candidate, compatibility);
			evaluations.add(evaluation);
			if (bestScore == null || isBetter(compatibility, bestScore)) {
				bestOrder = candidate;
				bestScore = compatibility;
				bestIndex = i;
				chosenEvaluation = evaluation;
			} else if (compatibility.equals(bestScore) && i < bestIndex) {
				bestOrder = candidate;
				bestScore = compatibility;
				bestIndex = i;
				chosenEvaluation = evaluation;
			}
		}

//		Evaluation bestPossible = bestPossibleOrder(variableSlots, orderIndex);
//		logSelection(variableOrder, variableSlots, evaluations, chosenEvaluation, bestPossible);

		return bestOrder;
	}

	private static Evaluation bestPossibleOrder(Map<String, Slot> variableSlots, Map<String, Integer> orderIndex) {
		Evaluation best = null;
		int bestIndex = Integer.MAX_VALUE;
		for (int i = 0; i < ALL_ORDERS.size(); i++) {
			QuadKeyOrder order = ALL_ORDERS.get(i);
			Compatibility compatibility = compatibilityScore(order, variableSlots, orderIndex);
			if (best == null || isBetter(compatibility, best.compatibility)) {
				best = new Evaluation(order, compatibility);
				bestIndex = i;
			} else if (compatibility.equals(best.compatibility) && i < bestIndex) {
				best = new Evaluation(order, compatibility);
				bestIndex = i;
			}
		}
		return best;
	}

	private static Compatibility compatibilityScore(QuadKeyOrder order, Map<String, Slot> variableSlots,
			Map<String, Integer> orderIndex) {
		int satisfied = 0;
		int positionSum = 0;
		Set<String> variables = variableSlots.keySet();

		for (String variable : variables) {
			Slot slot = variableSlots.get(variable);
			int slotPosition = order.indexOf(slot);
			positionSum += slotPosition;
			Integer variablePosition = orderIndex.get(variable);
			if (variablePosition == null) {
				continue;
			}
			if (allEarlierVariablesBeforeSlot(variablePosition, slotPosition, order, variableSlots, orderIndex)) {
				satisfied++;
			}
		}
		return new Compatibility(satisfied, positionSum);
	}

	private static boolean allEarlierVariablesBeforeSlot(int variablePosition, int slotPosition, QuadKeyOrder order,
			Map<String, Slot> variableSlots, Map<String, Integer> orderIndex) {
		for (Map.Entry<String, Slot> entry : variableSlots.entrySet()) {
			Integer otherPosition = orderIndex.get(entry.getKey());
			if (otherPosition == null || otherPosition >= variablePosition) {
				continue;
			}
			if (order.indexOf(entry.getValue()) > slotPosition) {
				return false;
			}
		}
		return true;
	}

	private static Map<String, Integer> indexMap(List<String> variableOrder) {
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < variableOrder.size(); i++) {
			map.put(variableOrder.get(i), i);
		}
		return map;
	}

	private static boolean isBetter(Compatibility candidate, Compatibility best) {
		if (candidate.score != best.score) {
			return candidate.score > best.score;
		}
		return candidate.positionSum < best.positionSum;
	}

	private static void logSelection(List<String> variableOrder, Map<String, Slot> variableSlots,
			List<Evaluation> evaluations, Evaluation chosen, Evaluation bestPossible) {
		if (!LOGGER.isInfoEnabled() || chosen == null) {
			return;
		}

		String available = evaluations.stream()
				.map(IndexSelector::formatEvaluation)
				.collect(Collectors.joining(", "));
		String picked = formatEvaluation(chosen);
		String reason = String.format(
				"highest compatibility score (%d) and lowest position sum (%d) among available options",
				chosen.compatibility.score(), chosen.compatibility.positionSum());
		String betterIndex = "none";
		if (bestPossible != null && isBetter(bestPossible.compatibility, chosen.compatibility)) {
			betterIndex = formatEvaluation(bestPossible);
		}

		LOGGER.info(
				"Available indexes: [{}]; picked: {}; reason: {}; Better index to enable: {} (variable order={}, variables={})",
				available, picked, reason, betterIndex, variableOrder, variableSlots.keySet());
	}

	private static String formatEvaluation(Evaluation evaluation) {
		return evaluation.order.fieldSequence() + "(score=" + evaluation.compatibility.score() + ", positionSum="
				+ evaluation.compatibility.positionSum() + ")";
	}

	private static List<QuadKeyOrder> allOrders() {
		List<QuadKeyOrder> orders = new ArrayList<>(24);
		Slot[] slots = Slot.values();
		permute(slots, 0, orders);
		orders.sort(Comparator.comparing(QuadKeyOrder::fieldSequence));
		return List.copyOf(orders);
	}

	private static void permute(Slot[] slots, int index, List<QuadKeyOrder> orders) {
		if (index == slots.length) {
			orders.add(QuadKeyOrder.of(slots));
			return;
		}
		for (int i = index; i < slots.length; i++) {
			swap(slots, index, i);
			permute(slots, index + 1, orders);
			swap(slots, index, i);
		}
	}

	private static void swap(Slot[] slots, int left, int right) {
		Slot tmp = slots[left];
		slots[left] = slots[right];
		slots[right] = tmp;
	}

	private static final class Evaluation {
		private final QuadKeyOrder order;
		private final Compatibility compatibility;

		private Evaluation(QuadKeyOrder order, Compatibility compatibility) {
			this.order = order;
			this.compatibility = compatibility;
		}
	}

	private static final class Compatibility {
		private final int score;
		private final int positionSum;

		private Compatibility(int score, int positionSum) {
			this.score = score;
			this.positionSum = positionSum;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			Compatibility other = (Compatibility) obj;
			return score == other.score && positionSum == other.positionSum;
		}

		@Override
		public int hashCode() {
			return Objects.hash(score, positionSum);
		}

		int score() {
			return score;
		}

		int positionSum() {
			return positionSum;
		}
	}
}
