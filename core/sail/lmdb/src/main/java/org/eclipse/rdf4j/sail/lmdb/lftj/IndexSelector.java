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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Chooses an index order for a pattern using the provided variable order as guidance.
 */
public final class IndexSelector {
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

		for (QuadKeyOrder candidate : candidates) {
			Compatibility compatibility = compatibilityScore(candidate, variableSlots, orderIndex);
			if (bestScore == null || isBetter(compatibility, bestScore)) {
				bestOrder = candidate;
				bestScore = compatibility;
			} else if (compatibility.equals(bestScore)
					&& candidates.indexOf(candidate) < candidates.indexOf(bestOrder)) {
				bestOrder = candidate;
				bestScore = compatibility;
			}
		}

		return bestOrder;
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
	}
}
