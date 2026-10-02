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

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guard-rail test ensuring HashFile#createEmptyFile(File) returns a {@link FileChannel}. This enforces the migration
 * away from {@link java.io.RandomAccessFile} when preparing overflow bucket files.
 */
public class HashFileFileChannelMigrationTest {

	@TempDir
	File tempDir;

	@Test
	public void createEmptyFileReturnsFileChannel() throws Exception {
		File hashPath = new File(tempDir, "values.hash");
		File tmpFile = new File(tempDir, "overflow.tmp");

		try (HashFile hashFile = new HashFile(hashPath)) {
			Method method = HashFile.class.getDeclaredMethod("createEmptyFile", File.class);
			method.setAccessible(true);

			Object resource = null;
			try {
				resource = method.invoke(hashFile, tmpFile);

				assertThat(resource)
						.as("createEmptyFile should expose FileChannel usage instead of RandomAccessFile")
						.isInstanceOf(FileChannel.class);
			} finally {
				if (resource instanceof Closeable) {
					((Closeable) resource).close();
				} else if (resource instanceof AutoCloseable) {
					((AutoCloseable) resource).close();
				}
			}
		}
	}
}
