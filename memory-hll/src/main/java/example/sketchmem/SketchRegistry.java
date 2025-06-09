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
package example.sketchmem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.agkn.hll.HLL;

/**
 * Thread-safe registry of {@link PredStats} keyed by predicate IRI.
 */
final class SketchRegistry {
	private final ConcurrentMap<String, PredStats> map = new ConcurrentHashMap<>();

	/**
	 * Retrieve stats for the given predicate, creating on demand.
	 *
	 * @param iri predicate IRI
	 * @return statistics instance
	 */
	PredStats forPred(String iri) {
		return map.computeIfAbsent(iri, k -> new PredStats());
	}

	/**
	 * Estimate the intersection of two sketches using the inclusion-exclusion principle.
	 */
	long intersection(HLL a, HLL b) {
		try {
			HLL u = (HLL) a.clone();
			u.union(b);
			return a.cardinality() + b.cardinality() - u.cardinality();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
