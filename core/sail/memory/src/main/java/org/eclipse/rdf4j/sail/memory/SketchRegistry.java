/*****************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *****************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.agkn.hll.HLL;

final class SketchRegistry {
	private final ConcurrentMap<String, PredStats> map = new ConcurrentHashMap<>();

	PredStats forPred(String iri) {
		return map.computeIfAbsent(iri, k -> new PredStats());
	}

	long intersection(HLL a, HLL b) {
		try {
			HLL union = a.clone();
			union.union(b);
			return a.cardinality() + b.cardinality() - union.cardinality();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
