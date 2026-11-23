/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSetTest;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MapBindingSet}
 *
 * @author Jeen Broekstra
 */
public class MapBindingSetTest extends BindingSetTest<MapBindingSet> {

	@Test
	void compactValueTestVisitsLaterRootsAfterMismatchAndPreservesArrayAcrossReentry() {
		var row = new MapBindingSet.CompactLayout(new String[] { "a", "unbound", "b" })
				.snapshot(new Value[] { RDF.ALT, null, RDF.BAG }, 2);
		List<Value> visited = new ArrayList<>();
		assertFalse(row.testAllValues(value -> {
			visited.add(value);
			if (value == RDF.ALT) {
				row.removeBinding("b"); // Detach the mutable map. The outer visit already selected its stable array.
				assertTrue(row.testAllValues(nested -> nested == RDF.ALT));
				return false;
			}
			return true;
		}));
		assertEquals(List.of(RDF.ALT, RDF.BAG), visited);

		var original = new MapBindingSet.CompactLayout(new String[] { "a", "b" })
				.snapshot(new Value[] { RDF.ALT, RDF.BAG }, 2);
		var failure = new IllegalStateException("later getter failed");
		assertSame(failure, assertThrows(IllegalStateException.class, () -> original.testAllValues(value -> {
			if (value == RDF.BAG) {
				throw failure;
			}
			return false;
		})));
	}

	@Test
	void valueTestHonorsNoncompactSubclassTraversal() {
		var row = new MapBindingSet() {
			@Override
			public void forEachValue(Consumer<? super Value> visitor) {
				visitor.accept(RDF.BAG);
				visitor.accept(RDF.ALT);
			}
		};
		List<Value> visited = new ArrayList<>();
		assertFalse(row.testAllValues(value -> {
			visited.add(value);
			return value == RDF.ALT;
		}));
		assertEquals(List.of(RDF.BAG, RDF.ALT), visited);
	}

	@Override
	protected MapBindingSet[] createTwoEqualReorderedBindingSets() {

		MapBindingSet bindingSet1 = new MapBindingSet();
		MapBindingSet bindingSet2 = new MapBindingSet();

		bindingSet1.addBinding("x", RDF.ALT);
		bindingSet1.addBinding("y", RDF.BAG);
		bindingSet1.addBinding("z", RDF.FIRST);

		bindingSet2.addBinding("y", RDF.BAG);
		bindingSet2.addBinding("x", RDF.ALT);
		bindingSet2.addBinding("z", RDF.FIRST);

		return new MapBindingSet[] { bindingSet1, bindingSet2 };
	}

}
