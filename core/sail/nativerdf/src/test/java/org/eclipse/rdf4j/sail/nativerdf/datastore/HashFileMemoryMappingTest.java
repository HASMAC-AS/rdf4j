/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HashFileMemoryMappingTest {

	@TempDir
	File tempDir;

	@Test
	void idIteratorUsesMappedByteBuffer() throws Exception {
		File hashPath = new File(tempDir, "values.hash");

		try (HashFile hashFile = new HashFile(hashPath)) {
			hashFile.storeID(42, 1);

			HashFile.IDIterator iterator = hashFile.getIDIterator(42);
			try {
				Field field = HashFile.IDIterator.class.getDeclaredField("bucketBuffer");
				field.setAccessible(true);

				ByteBuffer bucketBuffer = (ByteBuffer) field.get(iterator);

				assertThat(bucketBuffer)
						.as("IDIterator should rely on a memory-mapped bucket for lookups")
						.isInstanceOf(MappedByteBuffer.class);
			} finally {
				iterator.close();
			}
		}
	}
}
