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
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.sail.nativerdf.testutil.CountingFileChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataFileMemoryMappingTest {

	@TempDir
	File tempDir;

	@Test
	void getDataUsesMemoryMapping() throws Exception {
		File dataPath = new File(tempDir, "values.dat");

		try (DataFile dataFile = new DataFile(dataPath)) {
			long offset = dataFile.storeData("hello".getBytes(StandardCharsets.UTF_8));
			dataFile.sync();

			CountingFileChannel counting = wrapWithCountingChannel(dataFile);

			byte[] data = dataFile.getData(offset);

			assertThat(new String(data, StandardCharsets.UTF_8)).isEqualTo("hello");
			assertThat(counting.getMapCount())
					.as("DataFile#getData should rely on FileChannel#map for reads")
					.isGreaterThan(0);
		}
	}

	private static CountingFileChannel wrapWithCountingChannel(DataFile dataFile)
			throws NoSuchFieldException, IllegalAccessException {
		Field nioFileField = DataFile.class.getDeclaredField("nioFile");
		nioFileField.setAccessible(true);
		NioFile nioFile = (NioFile) nioFileField.get(dataFile);

		Field fcField = NioFile.class.getDeclaredField("fc");
		fcField.setAccessible(true);
		FileChannel delegate = (FileChannel) fcField.get(nioFile);
		CountingFileChannel counting = new CountingFileChannel(delegate);
		fcField.set(nioFile, counting);
		return counting;
	}
}
