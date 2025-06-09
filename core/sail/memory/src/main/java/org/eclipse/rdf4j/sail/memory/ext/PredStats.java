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

import org.apache.datasketches.hll.HllSketch;
import org.eclipse.rdf4j.model.Value;

/**
 * Statistics for a single predicate. Keeps two HyperLogLog sketches for distinct subjects and objects and counts total
 * triples.
 */
public final class PredStats {

	private final HllSketch subjHll;
	private final HllSketch objHll;
	private volatile long triples;
	private volatile boolean dirty;

	/**
	 * Create a new {@link PredStats} using sketches of size {@code 2^p} registers.
	 *
	 * @param p log₂ of the number of registers
	 */
	public PredStats(int p) {
		this.subjHll = new HllSketch(p);
		this.objHll = new HllSketch(p);
	}

	/**
	 * Update the statistics for a newly inserted triple.
	 *
	 * @param subj subject value of the triple
	 * @param obj  object value of the triple
	 */
	public void update(Value subj, Value obj) {
		subjHll.update(subj.stringValue());
		objHll.update(obj.stringValue());
		triples++;
	}

	/** Mark this stats instance as dirty due to a delete. */
	public void markDirty() {
		dirty = true;
	}

	/** @return HyperLogLog sketch counting distinct subjects. */
	public HllSketch getSubjHll() {
		return subjHll;
	}

	/** @return HyperLogLog sketch counting distinct objects. */
	public HllSketch getObjHll() {
		return objHll;
	}

	/** @return total number of triples seen. */
	public long getTriples() {
		return triples;
	}

	/** @return {@code true} when deletes have been observed. */
	public boolean isDirty() {
		return dirty;
	}
}
