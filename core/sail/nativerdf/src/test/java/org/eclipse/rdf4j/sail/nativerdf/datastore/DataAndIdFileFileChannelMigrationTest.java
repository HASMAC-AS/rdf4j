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
import java.util.Arrays;

import org.eclipse.rdf4j.common.io.NioFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guard-rail tests that enforce the ongoing migration from {@link NioFile} to direct {@link FileChannel} usage in the
 * NativeStore data files.
 */
public class DataAndIdFileFileChannelMigrationTest {

	@TempDir
	File tempDir;

	@Test
	public void dataFileUsesDirectFileChannel() throws Exception {
		File path = new File(tempDir, "values.dat");

		try (DataFile dataFile = new DataFile(path)) {
			assertNoNioFileField(DataFile.class);
			assertHasOpenFileChannelField(dataFile);
		}
	}

	@Test
	public void dataFileRecoversFromUnexpectedChannelClose() throws Exception {
		File path = new File(tempDir, "values.dat");

		try (DataFile dataFile = new DataFile(path)) {
			long firstOffset = dataFile.storeData("alpha".getBytes(StandardCharsets.UTF_8));
			Field channelField = DataFile.class.getDeclaredField("fileChannel");
			channelField.setAccessible(true);
			FileChannel original = (FileChannel) channelField.get(dataFile);
			original.close();

			long secondOffset = dataFile.storeData("beta".getBytes(StandardCharsets.UTF_8));
			assertThat(secondOffset).isGreaterThan(firstOffset);

			byte[] recovered = dataFile.tryRecoverBetweenOffsets(firstOffset, secondOffset);
			assertThat(recovered).isEqualTo("alpha".getBytes(StandardCharsets.UTF_8));

			FileChannel reopened = (FileChannel) channelField.get(dataFile);
			assertThat(reopened).isNotSameAs(original);
			assertThat(reopened.isOpen()).isTrue();
		}
	}

	@Test
	public void idFileUsesDirectFileChannel() throws Exception {
		File path = new File(tempDir, "values.id");

		try (IDFile idFile = new IDFile(path)) {
			assertNoNioFileField(IDFile.class);
			assertHasOpenFileChannelField(idFile);
		}
	}

	@Test
	public void idFileRecoversFromUnexpectedChannelClose() throws Exception {
		File path = new File(tempDir, "values.id");

		try (IDFile idFile = new IDFile(path)) {
			int id = idFile.storeOffset(42L);
			Field channelField = IDFile.class.getDeclaredField("fileChannel");
			channelField.setAccessible(true);
			FileChannel original = (FileChannel) channelField.get(idFile);
			original.close();

			idFile.setOffset(id, 84L);
			assertThat(idFile.getOffset(id)).isEqualTo(84L);

			FileChannel reopened = (FileChannel) channelField.get(idFile);
			assertThat(reopened).isNotSameAs(original);
			assertThat(reopened.isOpen()).isTrue();
		}
	}

	private static void assertNoNioFileField(Class<?> type) {
		boolean hasNioFile = Arrays.stream(type.getDeclaredFields())
				.anyMatch(field -> field.getType().equals(NioFile.class));

		assertThat(hasNioFile)
				.as("%s should not retain NioFile fields during the FileChannel migration", type.getSimpleName())
				.isFalse();
	}

	private static void assertHasOpenFileChannelField(Object instance) throws Exception {
		Field channelField = Arrays.stream(instance.getClass().getDeclaredFields())
				.filter(field -> FileChannel.class.isAssignableFrom(field.getType()))
				.peek(field -> field.setAccessible(true))
				.findFirst()
				.orElseThrow(() -> new AssertionError(
						instance.getClass().getSimpleName() + " should expose a FileChannel-backed field"));

		Object value = channelField.get(instance);

		assertThat(value)
				.as("%s should expose an initialized FileChannel instance", instance.getClass().getSimpleName())
				.isInstanceOf(FileChannel.class);

		if (value instanceof FileChannel) {
			FileChannel fileChannel = (FileChannel) value;
			assertThat(fileChannel.isOpen())
					.as("FileChannel field %s should be open", channelField.getName())
					.isTrue();
		}
	}
}
