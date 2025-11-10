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
package org.eclipse.rdf4j.sail.nativerdf.btree;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.sail.nativerdf.testutil.CountingFileChannel;
import org.eclipse.rdf4j.sail.nativerdf.testutil.CountingFileChannel.MapRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BTreeMemoryMappingTest {

	@TempDir
	File tempDir;

	@Test
	void nodeReadUsesMemoryMapping() throws Exception {
		byte[] value = ByteBuffer.allocate(8).putLong(42L).array();

		File dataDir = new File(tempDir, "btree");
		assertThat(dataDir.mkdirs() || dataDir.isDirectory()).isTrue();

		try (BTree tree = new BTree(dataDir, "values", 4096, value.length)) {
			tree.insert(value);
		}

		try (BTree tree = new BTree(dataDir, "values", 4096, value.length)) {
			CountingFileChannel counting = wrapWithCountingChannel(tree);

			byte[] result = tree.get(value);
			assertThat(result).isEqualTo(value);
			assertThat(counting.getMapCount())
					.as("BTree node reads should map the underlying file")
					.isGreaterThan(0);
		}
	}

	@Test
	void repeatedNodeReadsReuseExistingMapping() throws Exception {
		byte[] value = ByteBuffer.allocate(8).putLong(99L).array();

		File dataDir = new File(tempDir, "btree-reuse");
		assertThat(dataDir.mkdirs() || dataDir.isDirectory()).isTrue();

		try (BTree tree = new BTree(dataDir, "values", 4096, value.length)) {
			tree.insert(value);
		}

		try (BTree tree = new BTree(dataDir, "values", 4096, value.length)) {
			CountingFileChannel counting = wrapWithCountingChannel(tree);
			int rootNodeID = getRootNodeId(tree);
			Node node = new Node(rootNodeID, tree);

			node.read();
			int mapsAfterFirst = counting.getMapCount();

			node.read();
			int mapsAfterSecond = counting.getMapCount();

			assertThat(mapsAfterSecond)
					.as("subsequent node reads should reuse the existing memory mapping")
					.isEqualTo(mapsAfterFirst);
		}
	}

	@Test
	void nodeReadDoesNotMapEntireLargeFile() throws Exception {
		byte[] value = ByteBuffer.allocate(8).putLong(7L).array();

		File dataDir = new File(tempDir, "btree-large");
		assertThat(dataDir.mkdirs() || dataDir.isDirectory()).isTrue();

		File btreeFile = new File(dataDir, "values.dat");
		try (BTree tree = new BTree(dataDir, "values", 4096, value.length)) {
			tree.insert(value);
		}

		long segmentSize = readOnlySegmentSize();
		long inflatedSize = segmentSize * 4L;
		expandFileTo(btreeFile, inflatedSize);

		try (BTree tree = new BTree(dataDir, "values", 4096, value.length)) {
			CountingFileChannel counting = wrapWithCountingChannel(tree);

			byte[] result = tree.get(value);
			assertThat(result).isEqualTo(value);
			assertThat(counting.getMapRequests()).isNotEmpty();
			assertThat(totalMappedBytes(counting))
					.as("node read should not map the entire %d-byte file", inflatedSize)
					.isLessThan(inflatedSize);
		}
	}

	private static CountingFileChannel wrapWithCountingChannel(BTree tree)
			throws NoSuchFieldException, IllegalAccessException {
		Field nioFileField = BTree.class.getDeclaredField("nioFile");
		nioFileField.setAccessible(true);
		NioFile nioFile = (NioFile) nioFileField.get(tree);

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
		Field field = BTree.class.getDeclaredField("READ_ONLY_MAP_SEGMENT_SIZE");
		field.setAccessible(true);
		return field.getInt(null);
	}

	private static void expandFileTo(File file, long size) throws Exception {
		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			raf.setLength(size);
		}
	}

	private static int getRootNodeId(BTree tree) throws NoSuchFieldException, IllegalAccessException {
		Field rootField = BTree.class.getDeclaredField("rootNodeID");
		rootField.setAccessible(true);

		return rootField.getInt(tree);
	}
}
