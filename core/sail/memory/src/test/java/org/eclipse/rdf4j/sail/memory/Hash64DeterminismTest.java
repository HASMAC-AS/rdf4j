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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class Hash64DeterminismTest {

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
	public void repeatability() {
		long h1 = hash64("test");
		long h2 = hash64("test");
		assertEquals(h1, h2);
	}

	@Test
	public void collisionRate() {
		Set<Long> hashes = new HashSet<>();
		int collisions = 0;
		for (int i = 0; i < 100000; i++) {
			long h = hash64(UUID.randomUUID().toString());
			if (!hashes.add(h)) {
				collisions++;
			}
		}
		assertTrue(collisions <= 1000);
	}
}
