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

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PredStats}. */
public class PredStatsTest {

	@Test
	public void distinctSubjectEstimate() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		PredStats ps = new PredStats(14);
		for (int i = 0; i < 1000; i++) {
			ps.update(vf.createIRI("urn:s" + i), vf.createIRI("urn:o" + i));
		}
		double est = ps.getSubjHll().getEstimate();
		assertThat(est).isCloseTo(1000.0, withinPercentage(3.0));
	}
}
