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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.CloneNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.agkn.hll.HLL;

public class UnionPropertyTest {
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
	public void unionMatchesSetSize() {
		HLL a = new HLL(LOG2M, REGWIDTH);
		HLL b = new HLL(LOG2M, REGWIDTH);
		Set<String> set = new HashSet<>();
		for (int i = 0; i < 5000; i++) {
			String v = "a" + i;
			set.add(v);
			long h = hash64(v);
			a.addRaw(h);
		}
		for (int i = 2500; i < 7500; i++) {
			String v = "b" + i;
			set.add(v);
			long h = hash64(v);
			b.addRaw(h);
		}
		try {
			HLL u = a.clone();
			u.union(b);
			assertEquals(set.size(), u.cardinality(), set.size() * 0.05);
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
