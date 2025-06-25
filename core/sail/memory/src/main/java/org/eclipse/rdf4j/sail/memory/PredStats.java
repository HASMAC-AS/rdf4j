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

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.Value;

import net.agkn.hll.HLL;

final class PredStats {
	private static final int LOG2M = 14;
	private static final int REGWIDTH = 5;

	final HLL subj = new HLL(LOG2M, REGWIDTH);
	final HLL obj = new HLL(LOG2M, REGWIDTH);

	long triples = 0L;

	void update(Value s, Value o) {
		subj.addRaw(hash64(s.stringValue()));
		obj.addRaw(hash64(o.stringValue()));
		++triples;
	}

	private static long hash64(String str) {
		final long FNV_64_PRIME = 0x100000001b3L;
		long hash = 0xcbf29ce484222325L;
		byte[] data = str.getBytes(StandardCharsets.UTF_8);
		for (byte b : data) {
			hash ^= (b & 0xff);
			hash *= FNV_64_PRIME;
		}
		return hash;
	}

	double dupSubj() {
		return triples / Math.max(1.0, subj.cardinality());
	}

	double dupObj() {
		return triples / Math.max(1.0, obj.cardinality());
	}
}
