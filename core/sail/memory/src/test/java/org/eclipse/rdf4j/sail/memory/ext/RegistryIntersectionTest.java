/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.memory.ext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import org.apache.datasketches.hll.HllSketch;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

/** Tests for {@link SketchRegistry#estimateIntersection(HllSketch, HllSketch)}. */
public class RegistryIntersectionTest {

	@Test
	public void estimateOverlap() {
		HllSketch a = new HllSketch(14);
		HllSketch b = new HllSketch(14);
		for (int i = 0; i < 1000; i++) {
			a.update("v" + i);
		}
		for (int i = 500; i < 1500; i++) {
			b.update("v" + i);
		}
		SketchRegistry reg = new SketchRegistry(14);
		long est = reg.estimateIntersection(a, b);
		assertThat((double) est).isCloseTo(500.0, withinPercentage(3.0));
	}
}
