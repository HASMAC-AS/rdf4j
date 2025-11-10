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
package org.eclipse.rdf4j.common.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NioFileSizeCachingTest {

	@TempDir
	Path tempDir;

	@Test
	void sizeIsCachedUntilMutationUpdatesIt() throws Exception {
		Path file = tempDir.resolve("size-cache.dat");

		Files.write(file, "abc".getBytes(StandardCharsets.UTF_8));

		try (NioFile nioFile = new NioFile(file.toFile())) {

			SizeCountingFileChannel counting = injectCountingChannel(nioFile);

			assertEquals(3, nioFile.size());
			assertEquals(1, counting.getSizeCallCount());

			assertEquals(3, nioFile.size());
			assertEquals(1, counting.getSizeCallCount(), "subsequent size calls should hit the cache");

			nioFile.writeBytes("def".getBytes(StandardCharsets.UTF_8), 3);
			assertEquals(6, nioFile.size());
			assertEquals(1, counting.getSizeCallCount(), "writes should update the cached size");

			nioFile.truncate(2);
			int countAfterTruncate = counting.getSizeCallCount();
			assertEquals(2, nioFile.size());
			assertEquals(countAfterTruncate, counting.getSizeCallCount(),
					"truncate should update the cached size");
		}
	}

	private static SizeCountingFileChannel injectCountingChannel(NioFile nioFile)
			throws NoSuchFieldException, IllegalAccessException {
		Field fcField = NioFile.class.getDeclaredField("fc");
		fcField.setAccessible(true);
		FileChannel delegate = (FileChannel) fcField.get(nioFile);
		SizeCountingFileChannel counting = new SizeCountingFileChannel(delegate);
		fcField.set(nioFile, counting);
		return counting;
	}

	private static final class SizeCountingFileChannel extends FileChannel {

		private final FileChannel delegate;
		private int sizeCallCount;

		private SizeCountingFileChannel(FileChannel delegate) {
			this.delegate = delegate;
		}

		int getSizeCallCount() {
			return sizeCallCount;
		}

		@Override
		public long size() throws java.io.IOException {
			sizeCallCount++;
			return delegate.size();
		}

		@Override
		public int read(ByteBuffer dst) throws java.io.IOException {
			return delegate.read(dst);
		}

		@Override
		public long read(ByteBuffer[] dsts, int offset, int length) throws java.io.IOException {
			return delegate.read(dsts, offset, length);
		}

		@Override
		public int read(ByteBuffer dst, long position) throws java.io.IOException {
			return delegate.read(dst, position);
		}

		@Override
		public int write(ByteBuffer src) throws java.io.IOException {
			return delegate.write(src);
		}

		@Override
		public long write(ByteBuffer[] srcs, int offset, int length) throws java.io.IOException {
			return delegate.write(srcs, offset, length);
		}

		@Override
		public int write(ByteBuffer src, long position) throws java.io.IOException {
			return delegate.write(src, position);
		}

		@Override
		public long position() throws java.io.IOException {
			return delegate.position();
		}

		@Override
		public FileChannel position(long newPosition) throws java.io.IOException {
			delegate.position(newPosition);
			return this;
		}

		@Override
		public FileChannel truncate(long size) throws java.io.IOException {
			delegate.truncate(size);
			return this;
		}

		@Override
		public void force(boolean metaData) throws java.io.IOException {
			delegate.force(metaData);
		}

		@Override
		public long transferTo(long position, long count, WritableByteChannel target) throws java.io.IOException {
			return delegate.transferTo(position, count, target);
		}

		@Override
		public long transferFrom(java.nio.channels.ReadableByteChannel src, long position, long count)
				throws java.io.IOException {
			return delegate.transferFrom(src, position, count);
		}

		@Override
		public MappedByteBuffer map(MapMode mode, long position, long size) throws java.io.IOException {
			return delegate.map(mode, position, size);
		}

		@Override
		public FileLock lock(long position, long size, boolean shared) throws java.io.IOException {
			return delegate.lock(position, size, shared);
		}

		@Override
		public FileLock tryLock(long position, long size, boolean shared) throws java.io.IOException {
			return delegate.tryLock(position, size, shared);
		}

		@Override
		protected void implCloseChannel() throws java.io.IOException {
			delegate.close();
		}
	}
}
