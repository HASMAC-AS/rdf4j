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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds {@link Prefix} instances for a pattern at a specific position in the global variable order.
 */
public final class PrefixBuilder {
	private PrefixBuilder() {
	}

	public static Prefix buildPrefix(QuadPattern pattern, List<String> variableOrder, String currentVariable,
			Map<String, Long> bindings) {
		Objects.requireNonNull(pattern, "pattern");
		Objects.requireNonNull(variableOrder, "variableOrder");
		Objects.requireNonNull(currentVariable, "currentVariable");
		Objects.requireNonNull(bindings, "bindings");

		int currentIndex = indexOf(variableOrder, currentVariable);
		Prefix.Builder prefix = Prefix.builder();

		assignSlot(prefix, Slot.S, pattern.term(Slot.S), variableOrder, bindings, currentIndex);
		assignSlot(prefix, Slot.P, pattern.term(Slot.P), variableOrder, bindings, currentIndex);
		assignSlot(prefix, Slot.O, pattern.term(Slot.O), variableOrder, bindings, currentIndex);
		assignSlot(prefix, Slot.C, pattern.term(Slot.C), variableOrder, bindings, currentIndex);

		return prefix.build();
	}

        private static void assignSlot(Prefix.Builder prefix, Slot slot, QuadPatternTerm term, List<String> variableOrder,
                        Map<String, Long> bindings, int currentIndex) {
                if (term.isUnbound()) {
                        return;
                }

                if (term.isConstant()) {
                        write(prefix, slot, term.constant());
                        return;
                }

                int termIndex = indexOf(variableOrder, term.variable());
                if (termIndex < currentIndex) {
                        Long boundValue = bindings.get(term.variable());
                        if (boundValue != null) {
                                write(prefix, slot, boundValue);
                        }
                }
        }

	private static void write(Prefix.Builder prefix, Slot slot, long value) {
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

	private static int indexOf(List<String> variableOrder, String variable) {
		int idx = variableOrder.indexOf(variable);
		if (idx < 0) {
			throw new IllegalArgumentException("Variable not present in order: " + variable);
		}
		return idx;
	}
}
