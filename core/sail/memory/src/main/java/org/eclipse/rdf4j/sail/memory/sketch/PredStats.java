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

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.digest.MurmurHash3;
import org.eclipse.rdf4j.model.Value;

import net.agkn.hll.HLL;

/**
 * Maintains HyperLogLog sketches for subjects and objects of a predicate and a count of total triples.
 */
public final class PredStats {

	private static final int P = 14; // log2 #registers
	private static final int W = 5; // bits/register

	private final HLL subj = new HLL(P, W);
	private final HLL obj = new HLL(P, W);
	private long triples = 0L;

	/**
	 * Update the statistics with a new subject/object pair.
	 *
	 * @param s the subject value
	 * @param o the object value
	 */
	public void update(Value s, Value o) {
		subj.addRaw(hash64(s));
		obj.addRaw(hash64(o));
		++triples;
	}

	/**
	 * @return estimated number of distinct subjects encountered
	 */
	public long ndvSubj() {
		return subj.cardinality();
	}

	/**
	 * @return estimated number of distinct objects encountered
	 */
	public long ndvObj() {
		return obj.cardinality();
	}

	/**
	 * @return duplication factor for the subject position
	 */
	public double dupSubj() {
		return triples / Math.max(1.0, (double) ndvSubj());
	}

	/**
	 * @return duplication factor for the object position
	 */
	public double dupObj() {
		return triples / Math.max(1.0, (double) ndvObj());
	}

	/**
	 * @return the number of triples seen
	 */
	public long getTriples() {
		return triples;
	}

	private static long hash64(Value v) {
		byte[] bytes = v.stringValue().getBytes(StandardCharsets.UTF_8);
		return MurmurHash3.hash64(bytes, 0, bytes.length);
	}
}
