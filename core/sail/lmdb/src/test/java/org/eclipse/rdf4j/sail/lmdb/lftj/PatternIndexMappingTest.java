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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PatternIndexMappingTest {

	@Test
	void buildsPrefixForFirstVariableWithOnlyConstants() {
		QuadPattern pattern = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(9L));

		Prefix prefix = PrefixBuilder.buildPrefix(pattern, List.of("s", "o"), "s", Map.of());

		assertThat(prefix.hasSubject()).isFalse();
		assertThat(prefix.hasPredicate()).isTrue();
		assertThat(prefix.predicate()).isEqualTo(2L);
		assertThat(prefix.hasObject()).isFalse();
		assertThat(prefix.hasContext()).isTrue();
		assertThat(prefix.context()).isEqualTo(9L);
	}

	@Test
	void buildsPrefixWithEarlierBindingsAndConstants() {
		QuadPattern pattern = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(9L));

		Prefix prefix = PrefixBuilder.buildPrefix(pattern, List.of("s", "o"), "o", Map.of("s", 42L));

		assertThat(prefix.hasSubject()).isTrue();
		assertThat(prefix.subject()).isEqualTo(42L);
		assertThat(prefix.hasPredicate()).isTrue();
		assertThat(prefix.predicate()).isEqualTo(2L);
		assertThat(prefix.hasObject()).isFalse();
		assertThat(prefix.hasContext()).isTrue();
		assertThat(prefix.context()).isEqualTo(9L);
	}

	@Test
	void choosesIndexOrderFavouringEarlierVariables() {
		QuadPattern pattern = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.variable("c"));

		List<QuadKeyOrder> orders = List.of(
				QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C),
				QuadKeyOrder.of(Slot.O, Slot.S, Slot.P, Slot.C),
				QuadKeyOrder.of(Slot.P, Slot.S, Slot.O, Slot.C));

		QuadKeyOrder chosen = IndexSelector.chooseBestOrder(pattern, List.of("s", "o", "c"), orders);

		assertThat(chosen.positionAt(0)).isEqualTo(Slot.S);
		assertThat(chosen.positionAt(1)).isEqualTo(Slot.P);
		assertThat(chosen.positionAt(2)).isEqualTo(Slot.O);
		assertThat(chosen.positionAt(3)).isEqualTo(Slot.C);
	}
}
