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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.sail.memory.SketchRegistry;
import org.junit.jupiter.api.Test;

import net.agkn.hll.HLL;

public class IntersectionAccuracyTest {
	private static final int LOG2M = 14;
	private static final int REGWIDTH = 5;

	private long hash64(String s) {
		final long FNV_64_PRIME = 0x100000001b3L;
		long hash = 0xcbf29ce484222325L;
		for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
			hash ^= (b & 0xff);
			hash *= FNV_64_PRIME;
		}
		return hash;
	}

	@Test
	public void intersectionWithinError() {
		for (int overlap = 0; overlap <= 10000; overlap += 2000) {
			Set<String> leftSet = new HashSet<>();
			Set<String> rightSet = new HashSet<>();
			HLL left = new HLL(LOG2M, REGWIDTH);
			HLL right = new HLL(LOG2M, REGWIDTH);
			for (int i = 0; i < 10000; i++) {
				String val = "x" + i;
				if (i < overlap) {
					leftSet.add(val);
					rightSet.add(val);
					long h = hash64(val);
					left.addRaw(h);
					right.addRaw(h);
				} else {
					leftSet.add(val);
					left.addRaw(hash64(val));
					String val2 = "y" + i;
					rightSet.add(val2);
					right.addRaw(hash64(val2));
				}
			}
			SketchRegistry reg = new SketchRegistry();
			long inter = reg.intersection(left, right);
			if (overlap == 0) {
				assertTrue(inter <= 800);
			} else {
				double err = Math.abs(inter - overlap) / (double) overlap;
				assertTrue(err <= 0.08);
			}
		}
	}
}
