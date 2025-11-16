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
package org.eclipse.rdf4j.sail.nativerdf.testutil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FileChannel wrapper that counts the number of {@link #map(FileChannel.MapMode, long, long)} invocations.
 */
public final class CountingFileChannel extends FileChannel {

	private final FileChannel delegate;
	private final AtomicInteger mapCount = new AtomicInteger();
	private final List<MapRequest> mapRequests = new CopyOnWriteArrayList<>();

	public CountingFileChannel(FileChannel delegate) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	public int getMapCount() {
		return mapCount.get();
	}

	@Override
	public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
		mapCount.incrementAndGet();
		mapRequests.add(new MapRequest(mode, position, size));
		return delegate.map(mode, position, size);
	}

	public List<MapRequest> getMapRequests() {
		return Collections.unmodifiableList(mapRequests);
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return delegate.read(dst);
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		return delegate.read(dsts, offset, length);
	}

	@Override
	public int read(ByteBuffer dst, long position) throws IOException {
		return delegate.read(dst, position);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return delegate.write(src);
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		return delegate.write(srcs, offset, length);
	}

	@Override
	public int write(ByteBuffer src, long position) throws IOException {
		return delegate.write(src, position);
	}

	@Override
	public long position() throws IOException {
		return delegate.position();
	}

	@Override
	public FileChannel position(long newPosition) throws IOException {
		delegate.position(newPosition);
		return this;
	}

	@Override
	public long size() throws IOException {
		return delegate.size();
	}

	@Override
	public FileChannel truncate(long size) throws IOException {
		delegate.truncate(size);
		return this;
	}

	@Override
	public void force(boolean metaData) throws IOException {
		delegate.force(metaData);
	}

	@Override
	public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
		return delegate.transferTo(position, count, target);
	}

	@Override
	public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
		return delegate.transferFrom(src, position, count);
	}

	@Override
	public FileLock lock(long position, long size, boolean shared) throws IOException {
		return delegate.lock(position, size, shared);
	}

	@Override
	public FileLock tryLock(long position, long size, boolean shared) throws IOException {
		return delegate.tryLock(position, size, shared);
	}

	@Override
	protected void implCloseChannel() throws IOException {
		delegate.close();
	}

	public static final class MapRequest {
		private final MapMode mode;
		private final long position;
		private final long size;

		private MapRequest(MapMode mode, long position, long size) {
			this.mode = mode;
			this.position = position;
			this.size = size;
		}

		public MapMode getMode() {
			return mode;
		}

		public long getPosition() {
			return position;
		}

		public long getSize() {
			return size;
		}
	}
}
