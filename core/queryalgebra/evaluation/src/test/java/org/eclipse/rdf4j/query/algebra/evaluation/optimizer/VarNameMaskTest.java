/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Var;
import org.junit.jupiter.api.Test;

class VarNameMaskTest {

	@Test
	void containsAllSupportsSmallMasks() {
		VarNameMap varNames = new VarNameMap();

		VarNameMask all = varNames.maskOf(List.of("a", "b", "c"));
		VarNameMask subset = varNames.maskOf(List.of("a", "c"));
		VarNameMask other = varNames.maskOf(List.of("d"));

		assertTrue(all.containsAll(subset));
		assertFalse(all.containsAll(other));
		assertEquals(4, varNames.size());
	}

	@Test
	void supportsMoreThanSixtyFourVariables() {
		VarNameMap varNames = new VarNameMap();
		for (int i = 0; i < 70; i++) {
			varNames.idOf("v" + i);
		}

		VarNameMask mask = varNames.maskOf(List.of("v0", "v63", "v64", "v69"));
		VarNameMask highSubset = varNames.maskOf(List.of("v64", "v69"));
		VarNameMask lowSubset = varNames.maskOf(List.of("v0", "v63"));

		assertEquals(4, mask.cardinality());
		assertTrue(mask.contains(varNames.idOf("v64")));
		assertTrue(mask.containsAll(highSubset));
		assertTrue(mask.containsAll(lowSubset));
		assertFalse(lowSubset.containsAll(mask));
	}

	@Test
	void unionsSmallAndLargeMasks() {
		VarNameMap varNames = new VarNameMap();
		for (int i = 0; i < 70; i++) {
			varNames.idOf("v" + i);
		}

		VarNameMask first = varNames.maskOf(List.of("v0", "v64"));
		VarNameMask second = varNames.maskOf(List.of("v69"));
		VarNameMask union = first.union(second);

		assertEquals(3, union.cardinality());
		assertTrue(union.contains(varNames.idOf("v0")));
		assertTrue(union.contains(varNames.idOf("v64")));
		assertTrue(union.contains(varNames.idOf("v69")));
	}

	@Test
	void collectorUsesTheSameNameSlotsAsTheMap() {
		VarNameMap varNames = new VarNameMap();
		Compare expression = new Compare(new Var("a"), new Var("b"));

		VarNameMask mask = VarNameMaskCollector.process(expression, varNames);

		assertEquals(2, mask.cardinality());
		assertTrue(mask.contains(varNames.idOf("a")));
		assertTrue(mask.contains(varNames.idOf("b")));
	}

	@Test
	void singletonMaskCanReturnItsOnlyId() {
		VarNameMap varNames = new VarNameMap();

		VarNameMask mask = varNames.maskOf(List.of("only"));

		assertEquals(varNames.idOf("only"), mask.singleId());
	}
}
