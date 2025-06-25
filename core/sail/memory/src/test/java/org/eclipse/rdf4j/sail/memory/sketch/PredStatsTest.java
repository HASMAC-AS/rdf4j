/****************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************/
package org.eclipse.rdf4j.sail.memory.sketch;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PredStats}.
 */
public class PredStatsTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void ndvWithinFivePercent() {
		PredStats stats = new PredStats();
		for (int i = 0; i < 10_000; i++) {
			stats.update(vf.createIRI("urn:s:" + i), vf.createLiteral("o" + i));
		}
		long ndv = stats.ndvSubj();
		assertTrue(ndv > 9500 && ndv < 10500,
				"NDV should be within 5% of actual value but was " + ndv);
		assertTrue(Math.abs(stats.dupSubj() - 1.0) < 0.05,
				"duplication factor should be ~1 but was " + stats.dupSubj());
	}
}
