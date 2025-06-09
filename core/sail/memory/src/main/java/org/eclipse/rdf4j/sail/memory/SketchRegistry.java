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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class SketchRegistry {

	private final Map<String, PredStats> map = new ConcurrentHashMap<>();

	PredStats forPred(String iri) {
		return map.computeIfAbsent(iri, k -> new PredStats());
	}

	PredStats get(String iri) {
		return map.get(iri);
	}

	long intersection(Sketch a, Sketch b) {
		Sketch union = a.copy();
		b.unionInto(union);
		return a.cardinality() + b.cardinality() - union.cardinality();
	}
}
