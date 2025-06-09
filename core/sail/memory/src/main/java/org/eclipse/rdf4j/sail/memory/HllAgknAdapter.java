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

import net.agkn.hll.HLL;

final class HllAgknAdapter implements Sketch {

	private final HLL hll;

	HllAgknAdapter() {
		this(new HLL(11, 5));
	}

	HllAgknAdapter(HLL hll) {
		this.hll = hll;
	}

	@Override
	public void add(long hash) {
		hll.addRaw(hash);
	}

	@Override
	public long cardinality() {
		return hll.cardinality();
	}

	@Override
	public void unionInto(Sketch other) {
		if (other instanceof HllAgknAdapter) {
			((HllAgknAdapter) other).hll.union(hll);
		} else {
			throw new IllegalArgumentException("Incompatible sketch type");
		}
	}

	@Override
	public Sketch copy() {
		try {
			return new HllAgknAdapter(hll.clone());
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
