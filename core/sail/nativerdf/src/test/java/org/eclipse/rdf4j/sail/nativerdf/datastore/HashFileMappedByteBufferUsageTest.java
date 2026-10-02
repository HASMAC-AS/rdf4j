/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.eclipse.rdf4j.common.io.NioFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Failing test capturing the desired migration behaviour: bucket operations should go through
 * {@link java.nio.MappedByteBuffer} instead of manual positional IO.
 */
class HashFileMappedByteBufferUsageTest {

	@TempDir
	File tempDir;

	private File hashFilePath;

	@BeforeEach
	void setup() {
		hashFilePath = new File(tempDir, "values.hash");
	}

	@AfterEach
	void tearDown() {
		// nothing to close; tests close files explicitly
	}

	@Test
	void storeAndLookupUsesMappedByteBuffer() throws Exception {
		try (HashFile hashFile = new HashFile(hashFilePath, /* forceSync= */ false, /* initialSize= */ 16)) {
			TrackingFileChannel tracker = injectTrackingChannel(hashFile);

			hashFile.storeID(42, 1001);

			HashFile.IDIterator iterator = hashFile.getIDIterator(42);
			try {
				assertThat(iterator.next()).isEqualTo(1001);
				assertThat(iterator.next()).isEqualTo(-1);
			} finally {
				iterator.close();
			}

			assertThat(tracker.mapCount)
					.as("HashFile should rely on memory-mapped access for bucket operations")
					.isGreaterThan(0);
		}
	}

	private static TrackingFileChannel injectTrackingChannel(HashFile hashFile) throws Exception {
		Field nioFileField = HashFile.class.getDeclaredField("nioFile");
		nioFileField.setAccessible(true);
		NioFile nio = (NioFile) nioFileField.get(hashFile);

		Field fcField = NioFile.class.getDeclaredField("fc");
		fcField.setAccessible(true);
		FileChannel delegate = (FileChannel) fcField.get(nio);

		TrackingFileChannel tracking = new TrackingFileChannel(delegate);
		fcField.set(nio, tracking);
		return tracking;
	}

	private static final class TrackingFileChannel extends FileChannel {
		private final FileChannel delegate;
		private volatile int mapCount = 0;

		private TrackingFileChannel(FileChannel delegate) {
			this.delegate = delegate;
		}

		@Override
		public MappedByteBuffer map(MapMode mode, long position, long size) throws java.io.IOException {
			mapCount++;
			return delegate.map(mode, position, size);
		}

		@Override
		public void force(boolean metaData) throws java.io.IOException {
			delegate.force(metaData);
		}

		@Override
		public int read(java.nio.ByteBuffer dst) throws java.io.IOException {
			return delegate.read(dst);
		}

		@Override
		public long read(java.nio.ByteBuffer[] dsts, int offset, int length) throws java.io.IOException {
			return delegate.read(dsts, offset, length);
		}

		@Override
		public int write(java.nio.ByteBuffer src) throws java.io.IOException {
			return delegate.write(src);
		}

		@Override
		public long write(java.nio.ByteBuffer[] srcs, int offset, int length) throws java.io.IOException {
			return delegate.write(srcs, offset, length);
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
		public long size() throws java.io.IOException {
			return delegate.size();
		}

		@Override
		public FileChannel truncate(long size) throws java.io.IOException {
			delegate.truncate(size);
			return this;
		}

		@Override
		public int read(java.nio.ByteBuffer dst, long position) throws java.io.IOException {
			return delegate.read(dst, position);
		}

		@Override
		public int write(java.nio.ByteBuffer src, long position) throws java.io.IOException {
			return delegate.write(src, position);
		}

		@Override
		public long transferTo(long position, long count, java.nio.channels.WritableByteChannel target)
				throws java.io.IOException {
			return delegate.transferTo(position, count, target);
		}

		@Override
		public long transferFrom(java.nio.channels.ReadableByteChannel src, long position, long count)
				throws java.io.IOException {
			return delegate.transferFrom(src, position, count);
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
