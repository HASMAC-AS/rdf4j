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

import org.eclipse.rdf4j.model.Value;

import net.agkn.hll.HLL;

/**
 * Collects HyperLogLog sketches for a single predicate.
 */
final class PredStats {
	private static final int LOG2M = 14; // 16 384 registers (≈12 KiB)
	private static final int REGW = 5; // std-err ≈1.6 %

	final HLL subj = new HLL(LOG2M, REGW);
	final HLL obj = new HLL(LOG2M, REGW);
	long triples = 0L;

	/** Update sketches with the given statement's subject and object. */
	void update(Value s, Value o) {
		subj.addRaw(hash64(s));
		obj.addRaw(hash64(o));
		triples++;
	}

	private static long hash64(Value v) {
		return v.stringValue().hashCode() & 0xffffffffL;
	}

	/**
	 * Number of recorded triples for this predicate.
	 */
	long tripleCount() {
		return triples;
	}

	/**
	 * Estimated number of distinct subjects.
	 */
	long subjCount() {
		return subj.cardinality();
	}

	/**
	 * Estimated number of distinct objects.
	 */
	long objCount() {
		return obj.cardinality();
	}

	/**
	 * Estimated duplicate factor for subjects.
	 *
	 * @return average number of triples per distinct subject
	 */
	double dupSubj() {
		return triples / Math.max(1.0, subj.cardinality());
	}

	/**
	 * Estimated duplicate factor for objects.
	 *
	 * @return average number of triples per distinct object
	 */
	double dupObj() {
		return triples / Math.max(1.0, obj.cardinality());
	}
}
