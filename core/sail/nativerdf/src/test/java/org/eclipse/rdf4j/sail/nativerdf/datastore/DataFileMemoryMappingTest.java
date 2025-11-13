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
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.sail.nativerdf.testutil.CountingFileChannel;
import org.eclipse.rdf4j.sail.nativerdf.testutil.CountingFileChannel.MapRequest;
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

	@Test
	void repeatedReadsDoNotTriggerAdditionalMappings() throws Exception {
		File dataPath = new File(tempDir, "values.dat");

		try (DataFile dataFile = new DataFile(dataPath)) {
			long offset = dataFile.storeData("world".getBytes(StandardCharsets.UTF_8));
			dataFile.sync();

			CountingFileChannel counting = wrapWithCountingChannel(dataFile);

			byte[] firstRead = dataFile.getData(offset);
			int mapsAfterFirstRead = counting.getMapCount();

			byte[] secondRead = dataFile.getData(offset);
			int mapsAfterSecondRead = counting.getMapCount();

			assertThat(new String(firstRead, StandardCharsets.UTF_8)).isEqualTo("world");
			assertThat(new String(secondRead, StandardCharsets.UTF_8)).isEqualTo("world");
			assertThat(mapsAfterSecondRead)
					.as("subsequent reads should reuse existing memory mapping")
					.isEqualTo(mapsAfterFirstRead);
		}
	}

	@Test
	void smallReadDoesNotMapEntireLargeFile() throws Exception {
		File dataPath = new File(tempDir, "values-large.dat");

		try (DataFile dataFile = new DataFile(dataPath)) {
			long offset = dataFile.storeData("tiny".getBytes(StandardCharsets.UTF_8));
			dataFile.sync();

			long segmentSize = readOnlySegmentSize();
			long inflatedSize = segmentSize * 4L;
			expandFileTo(dataPath, inflatedSize);
			setNioFileSize(dataFile, inflatedSize);

			CountingFileChannel counting = wrapWithCountingChannel(dataFile);

			byte[] result = dataFile.getData(offset);

			assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("tiny");
			assertThat(counting.getMapRequests()).isNotEmpty();
			assertThat(totalMappedBytes(counting))
					.as("should not map the entire %d-byte file", inflatedSize)
					.isLessThan(inflatedSize);
		}
	}

	@Test
	void highOffsetReadMapsSingleSegment() throws Exception {
		File dataPath = new File(tempDir, "values-high-offset.dat");

		try (DataFile dataFile = new DataFile(dataPath)) {
			long segmentSize = readOnlySegmentSize();
			long recordOffset = segmentSize * 3L;
			byte[] payload = "far-away".getBytes(StandardCharsets.UTF_8);

			writeRecordAt(dataFile, recordOffset, payload);
			setNioFileSize(dataFile, recordOffset + Integer.BYTES + payload.length);

			CountingFileChannel counting = wrapWithCountingChannel(dataFile);

			byte[] loaded = dataFile.getData(recordOffset);

			assertThat(new String(loaded, StandardCharsets.UTF_8)).isEqualTo("far-away");
			assertThat(counting.getMapRequests())
					.as("should map only the segment that covers the record")
					.hasSize(1);
			MapRequest request = counting.getMapRequests().get(0);
			assertThat(request.getPosition()).isEqualTo(alignDown(recordOffset, segmentSize));
			assertThat(request.getSize()).isLessThanOrEqualTo(segmentSize);
		}
	}

	private static CountingFileChannel wrapWithCountingChannel(DataFile dataFile)
			throws NoSuchFieldException, IllegalAccessException {
		NioFile nioFile = getNioFile(dataFile);

		Field fcField = NioFile.class.getDeclaredField("fc");
		fcField.setAccessible(true);
		FileChannel delegate = (FileChannel) fcField.get(nioFile);
		CountingFileChannel counting = new CountingFileChannel(delegate);
		fcField.set(nioFile, counting);
		return counting;
	}

	private static long totalMappedBytes(CountingFileChannel counting) {
		return counting.getMapRequests().stream().mapToLong(MapRequest::getSize).sum();
	}

	private static long readOnlySegmentSize() throws NoSuchFieldException, IllegalAccessException {
		Field field = DataFile.class.getDeclaredField("READ_ONLY_MAP_SEGMENT_SIZE");
		field.setAccessible(true);
		return field.getInt(null);
	}

	private static void setNioFileSize(DataFile dataFile, long size)
			throws NoSuchFieldException, IllegalAccessException {
		Field sizeField = DataFile.class.getDeclaredField("nioFileSize");
		sizeField.setAccessible(true);
		sizeField.setLong(dataFile, size);
	}

	private static void expandFileTo(File path, long size) throws Exception {
		try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
			raf.setLength(size);
		}
	}

	private static void writeRecordAt(DataFile dataFile, long offset, byte[] payload)
			throws Exception {
		NioFile nioFile = getNioFile(dataFile);
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + payload.length);
		buffer.putInt(payload.length);
		buffer.put(payload);
		buffer.flip();
		nioFile.write(buffer, offset);
	}

	private static NioFile getNioFile(DataFile dataFile) throws NoSuchFieldException, IllegalAccessException {
		Field nioFileField = DataFile.class.getDeclaredField("nioFile");
		nioFileField.setAccessible(true);
		return (NioFile) nioFileField.get(dataFile);
	}

	private static long alignDown(long value, long segmentSize) {
		return value - (value % segmentSize);
	}
}
