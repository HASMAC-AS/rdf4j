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

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Describes a quad pattern with per-slot variables or constants.
 */
public final class QuadPattern {
	private final Map<Slot, QuadPatternTerm> terms;
	private final Map<String, Slot> variableSlots;

	private QuadPattern(QuadPatternTerm subject, QuadPatternTerm predicate, QuadPatternTerm object,
			QuadPatternTerm context) {
		terms = new EnumMap<>(Slot.class);
		terms.put(Slot.S, Objects.requireNonNull(subject, "subject"));
		terms.put(Slot.P, Objects.requireNonNull(predicate, "predicate"));
		terms.put(Slot.O, Objects.requireNonNull(object, "object"));
		terms.put(Slot.C, Objects.requireNonNull(context, "context"));
		variableSlots = computeVariableSlots();
	}

	public static QuadPattern of(QuadPatternTerm subject, QuadPatternTerm predicate, QuadPatternTerm object,
			QuadPatternTerm context) {
		return new QuadPattern(subject, predicate, object, context);
	}

	public QuadPatternTerm term(Slot slot) {
		return terms.get(slot);
	}

	public Optional<Slot> slotOfVariable(String variable) {
		return Optional.ofNullable(variableSlots.get(variable));
	}

	public Set<String> variables() {
		return Collections.unmodifiableSet(variableSlots.keySet());
	}

	public Map<String, Slot> variableSlots() {
		return Collections.unmodifiableMap(variableSlots);
	}

	private Map<String, Slot> computeVariableSlots() {
		Map<String, Slot> slots = new HashMap<>();
		for (Map.Entry<Slot, QuadPatternTerm> entry : terms.entrySet()) {
			QuadPatternTerm term = entry.getValue();
			if (term.isVariable()) {
				slots.put(term.variable(), entry.getKey());
			}
		}
		return slots;
	}
}
