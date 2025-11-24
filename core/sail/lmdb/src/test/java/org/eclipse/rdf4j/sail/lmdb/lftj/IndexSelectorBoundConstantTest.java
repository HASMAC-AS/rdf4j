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

import org.junit.jupiter.api.Test;

class IndexSelectorBoundConstantTest {

	@Test
	void prefersBoundConstantsBeforeUnboundVariables() {
		QuadPattern pattern = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.unbound());

		List<QuadKeyOrder> candidates = List.of(
				QuadKeyOrder.SPOC,
				QuadKeyOrder.SOPC);

		List<String> variableOrder = List.of("s", "o");

		QuadKeyOrder chosen = IndexSelector.chooseBestOrder(pattern, variableOrder, candidates);

		assertThat(chosen).isEqualTo(QuadKeyOrder.SPOC);
	}
}
