/*****************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *****************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

public class SketchStatsTest {

	@Test
	public void testHashDeterminism() {
		IRI iri = SimpleValueFactory.getInstance().createIRI("http://example.com/a");
		long h1 = HashFunctions.hash64(iri.stringValue());
		long h2 = HashFunctions.hash64(iri.stringValue());
		assertEquals(h1, h2);
	}

	@Test
	public void testSketchAccuracy() {
		HllAgknAdapter sketch = new HllAgknAdapter();
		for (int i = 0; i < 100_000; i++) {
			sketch.add(HashFunctions.hash64("x" + i));
		}
		long card = sketch.cardinality();
		double err = Math.abs(card - 100_000) / 100_000.0;
		assertTrue(err < 0.2, "error=" + err);
	}

	@Test
	public void testIntersectionAccuracy() {
		int size = 10000;
		int[] percents = { 0, 1, 10, 50, 90 };
		for (int p : percents) {
			HllAgknAdapter a = new HllAgknAdapter();
			HllAgknAdapter b = new HllAgknAdapter();
			int overlap = size * p / 100;
			for (int i = 0; i < size; i++) {
				a.add(HashFunctions.hash64("a" + i));
			}
			for (int i = 0; i < overlap; i++) {
				b.add(HashFunctions.hash64("a" + i));
			}
			for (int i = overlap; i < size; i++) {
				b.add(HashFunctions.hash64("b" + i));
			}
			SketchRegistry reg = new SketchRegistry();
			long inter = reg.intersection(a, b);
			double error = Math.abs(inter - overlap) / (double) size;
			assertTrue(error < 0.2, "percent " + p + " error=" + error);
		}
	}
}
