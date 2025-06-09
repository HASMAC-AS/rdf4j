/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.memory.ext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.hll.Union;

/**
 * Central registry that stores {@link PredStats} for all predicates.
 */
public class SketchRegistry {

	private final ConcurrentMap<String, PredStats> data = new ConcurrentHashMap<>();
	private final int defaultP;

	/** Create a registry with default sketch precision of {@code p=14}. */
	public SketchRegistry() {
		this(14);
	}

	/**
	 * @param defaultP log₂ of the register count for new sketches
	 */
	public SketchRegistry(int defaultP) {
		this.defaultP = defaultP;
	}

	/**
	 * Return the {@link PredStats} instance for the given predicate IRI, creating it if necessary.
	 *
	 * @param predIri the predicate IRI string
	 * @return stats container
	 */
	public PredStats forPred(String predIri) {
		return data.computeIfAbsent(predIri, k -> new PredStats(defaultP));
	}

	/**
	 * Estimate the size of the intersection between two sketches using the inclusion–exclusion principle.
	 */
	public long estimateIntersection(HllSketch a, HllSketch b) {
		Union u = new Union(a.getLgConfigK());
		u.update(a);
		u.update(b);
		double est = a.getEstimate() + b.getEstimate() - u.getResult().getEstimate();
		return Math.round(est);
	}

	/**
	 * Placeholder for future rebuild logic of dirty sketches.
	 */
	public void refreshDirtyIfNeeded() {
// TODO implement
	}
}
