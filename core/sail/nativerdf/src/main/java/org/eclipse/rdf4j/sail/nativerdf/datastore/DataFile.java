/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import static org.eclipse.rdf4j.sail.nativerdf.NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.io.NioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class supplying access to a data file. A data file stores data sequentially. Each entry starts with the entry's
 * length (4 bytes), followed by the data itself. File offsets are used to identify entries.
 *
 * @author Arjohn Kampman
 */
public class DataFile implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(DataFile.class);

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Magic number "Native Data File" to detect whether the file is actually a data file. The first three bytes of the
	 * file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'n', 'd', 'f' };

	/**
	 * File format version, stored as the fourth byte in data files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	private static final long HEADER_LENGTH = MAGIC_NUMBER.length + 1;

	// Guard parameters
	private static final int FALLBACK_LARGE_READ_THRESHOLD = 128 * 1024 * 1024; // 128MB fallback
	public static final String LARGE_READ_THRESHOLD_PROPERTY = "org.eclipse.rdf4j.sail.nativerdf.datastore.DataFile.largeReadThresholdBytes";
	public static final int LARGE_READ_THRESHOLD = getConfiguredLargeReadThreshold();
	private static final int SOFT_FAIL_CAP_BYTES = 32 * 1024 * 1024; // 32MB
	private static final int READ_ONLY_MAP_SEGMENT_SIZE = 128 * 1024 * 1024; // 128MB per mapping segment

	/*-----------*
	 * Variables *
	 *-----------*/

	private final NioFile nioFile;

	private final boolean forceSync;

	// cached file size, also reflects buffer usage
	private volatile long nioFileSize;

	// 4KB write buffer that is flushed on sync, close and any read operations
	private final ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);

	private final Object readMapLock = new Object();
	private volatile ReadOnlyMapping readOnlyMapping = ReadOnlyMapping.EMPTY;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DataFile(File file) throws IOException {
		this(file, false);
	}

	public DataFile(File file, boolean forceSync) throws IOException {
		this.nioFile = new NioFile(file);
		this.forceSync = forceSync;

		try {
			// Open a read/write channel to the file

			if (nioFile.size() == 0) {
				// Empty file, write header
				nioFile.writeBytes(MAGIC_NUMBER, 0);
				nioFile.writeByte(FILE_FORMAT_VERSION, MAGIC_NUMBER.length);

				sync();
			} else if (nioFile.size() < HEADER_LENGTH) {
				throw new IOException("File too small to be a compatible data file");
			} else {
				// Verify file header
				if (!Arrays.equals(MAGIC_NUMBER, nioFile.readBytes(0, MAGIC_NUMBER.length))) {
					throw new IOException("File doesn't contain compatible data records");
				}

				byte version = nioFile.readByte(MAGIC_NUMBER.length);
				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read data file; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read data file; invalid file format version: " + version);
				}
			}
		} catch (IOException e) {
			this.nioFile.close();
			throw e;
		}

		this.nioFileSize = nioFile.size();

	}

	/*---------*
	 * Methods *
	 *---------*/

	public File getFile() {
		return nioFile.getFile();
	}

	/**
	 * Returns the current file size (after flushing any pending writes).
	 */
	public long getFileSize() throws IOException {
		flush();
		return nioFileSize;
	}

	/**
	 * Attempts to recover data bytes between two known entry offsets when the length field at {@code startOffset} is
	 * corrupt (e.g., zero). This returns up to {@code endOffset - startOffset - 4} bytes starting after the length
	 * field, capped to a reasonable maximum to avoid large allocations.
	 */
	public byte[] tryRecoverBetweenOffsets(long startOffset, long endOffset) throws IOException {
		flush();
		if (endOffset <= startOffset + 4) {
			return new byte[0];
		}
		long available = endOffset - (startOffset + 4);
		int cap = 32 * 1024 * 1024; // 32MB cap for recovery
		int toRead = (int) Math.min(Math.max(available, 0), cap);
		return nioFile.readBytes(startOffset + 4L, toRead);
	}

	/**
	 * Stores the specified data and returns the byte-offset at which it has been stored.
	 *
	 * @param data The data to store, must not be <var>null</var>.
	 * @return The byte-offset in the file at which the data was stored.
	 */
	synchronized public long storeData(byte[] data) throws IOException {
		assert data != null : "data must not be null";

		long offset = nioFileSize;

		if (data.length + 4 > buffer.capacity()) {
			// direct write because we are writing more data than the buffer can hold

			flush();

			// TODO: two writes could be more efficient since it prevent array copies
			ByteBuffer buf = ByteBuffer.allocate(data.length + 4);
			buf.putInt(data.length);
			buf.put(data);
			buf.rewind();

			nioFile.write(buf, offset);

			nioFileSize += buf.array().length;

		} else {
			if (data.length + 4 > remainingBufferCapacity()) {
				flush();
			}

			buffer.putInt(data.length);
			buffer.put(data);
			nioFileSize += data.length + 4;
		}

		return offset;

	}

	synchronized private void flush() throws IOException {
		int position = buffer.position();
		if (position == 0) {
			return;
		}
		buffer.position(0);

		byte[] byteToWrite = new byte[position];
		buffer.get(byteToWrite, 0, position);

		nioFile.write(ByteBuffer.wrap(byteToWrite), nioFileSize - byteToWrite.length);

		buffer.position(0);

	}

	synchronized private int remainingBufferCapacity() {
		return buffer.capacity() - buffer.position();
	}

	/**
	 * Gets the data that is stored at the specified offset.
	 *
	 * @param offset An offset in the data file, must be larger than 0.
	 * @return The data that was found on the specified offset.
	 * @throws IOException If an I/O error occurred.
	 */
	public byte[] getData(long offset) throws IOException {
		assert offset > 0 : "offset must be larger than 0, is: " + offset;
		flush();

		long available = Math.max(0, nioFileSize - offset);
		if (available < 4) {
			return new byte[0];
		}

		long headerEnd = offset + Integer.BYTES;
		if (headerEnd < offset) {
			throw new IOException("Corrupt data record at offset " + offset + ". Data length overflow.");
		}

		ensureMappingIncludes(headerEnd);
		ReadOnlyMapping mapping = readOnlyMapping;
		int declaredLength = readInt(mapping, offset);

		// Validate and possibly reduce the length before allocating a large array
		int dataLength = guardedDataLength(declaredLength);

		if (dataLength < 0) {
			throw new IOException("Corrupt data record at offset " + offset + ". Data length: " + dataLength);
		}

		try {
			byte[] result = new byte[dataLength];
			if (dataLength == 0) {
				return result;
			}

			long payloadOffset = offset + Integer.BYTES;
			if (payloadOffset < offset) {
				throw new IOException("Corrupt data record at offset " + offset + ". Data length overflow.");
			}

			long payloadAvailableLong = Math.max(0, nioFileSize - payloadOffset);
			int payloadAvailable = (int) Math.min(Integer.MAX_VALUE, payloadAvailableLong);
			int toRead = Math.min(dataLength, payloadAvailable);

			if (toRead > 0) {
				long payloadEnd = payloadOffset + toRead;
				if (payloadEnd < payloadOffset) {
					throw new IOException("Corrupt data record at offset " + offset + ". Data length overflow.");
				}
				ensureMappingIncludes(payloadEnd);
				mapping = readOnlyMapping;
				readFully(mapping, payloadOffset, result, 0, toRead);
			}

			return result;
		} catch (OutOfMemoryError e) {
			if (dataLength > LARGE_READ_THRESHOLD) {
				logger.error(
						"Trying to read large amounts of data may be a sign of data corruption. Consider setting the system property org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes to true");
			}
			throw e;
		}

	}

	private void ensureMappingIncludes(long endExclusive) throws IOException {
		if (endExclusive <= 0) {
			return;
		}

		ReadOnlyMapping current = readOnlyMapping;
		if (current.contains(endExclusive)) {
			return;
		}

		synchronized (readMapLock) {
			current = readOnlyMapping;
			if (current.contains(endExclusive)) {
				return;
			}

			long targetSize = Math.min(nioFileSize, Long.MAX_VALUE);
			if (targetSize <= current.mappedSize) {
				return;
			}

			List<MappingSegment> additions = new ArrayList<>();
			long position = current.mappedSize;
			while (position < targetSize) {
				long remaining = targetSize - position;
				int chunkSize = (int) Math.min(remaining, READ_ONLY_MAP_SEGMENT_SIZE);
				if (chunkSize <= 0) {
					break;
				}
				MappedByteBuffer buffer = nioFile.mapReadOnly(position, chunkSize);
				buffer.order(ByteOrder.BIG_ENDIAN);
				additions.add(new MappingSegment(position, position + chunkSize, buffer));
				position += chunkSize;
			}

			if (!additions.isEmpty()) {
				readOnlyMapping = current.append(additions, position);
			}
		}
	}

	private int readInt(ReadOnlyMapping mapping, long offset) throws IOException {
		MappingSegment segment = mapping.segmentFor(offset);
		ByteBuffer duplicate = segment.buffer.duplicate();
		int withinSegment = (int) (offset - segment.start);
		if (withinSegment <= duplicate.limit() - Integer.BYTES) {
			duplicate.position(withinSegment);
			return duplicate.getInt();
		}
		byte[] header = new byte[Integer.BYTES];
		readFully(mapping, offset, header, 0, Integer.BYTES);
		return ByteBuffer.wrap(header).getInt();
	}

	private void readFully(ReadOnlyMapping mapping, long offset, byte[] target, int targetOffset, int length)
			throws IOException {
		if (length <= 0) {
			return;
		}

		int remaining = length;
		long position = offset;
		int destPos = targetOffset;

		while (remaining > 0) {
			MappingSegment segment = mapping.segmentFor(position);
			ByteBuffer duplicate = segment.buffer.duplicate();
			int withinSegment = (int) (position - segment.start);
			duplicate.position(withinSegment);

			int chunk = Math.min(remaining, duplicate.remaining());
			if (chunk <= 0) {
				throw new IOException("Unable to read mapped bytes at offset " + position);
			}

			duplicate.get(target, destPos, chunk);
			remaining -= chunk;
			destPos += chunk;
			position += chunk;
		}
	}

	/**
	 * For very large reads, ensure there appears to be sufficient free heap to allocate the requested record. If soft
	 * fail mode is enabled and insufficient memory is observed, returns a reduced cap to allow recovery; otherwise
	 * throws an IOException with guidance for remediation.
	 */
	private int guardedDataLength(int requested) throws IOException {
		if (requested <= 0) {
			return requested;
		}

		// Soft-fail corruption cap remains in effect for oversized claims
		if (requested > LARGE_READ_THRESHOLD && SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
			logger.error(
					"Data length is {}MB which is larger than {}MB. This is likely data corruption. Truncating length to {} MB.",
					requested / (1024 * 1024), LARGE_READ_THRESHOLD / (1024 * 1024),
					SOFT_FAIL_CAP_BYTES / (1024 * 1024));
			return SOFT_FAIL_CAP_BYTES;
		}

		if (requested <= LARGE_READ_THRESHOLD) {
			return requested;
		}

		Runtime rt = Runtime.getRuntime();
		for (int i = 0; i < 6; i++) { // initial check + up to 5 GC attempts
			long free = getFreeMemory(rt);
			if (free >= requested) {
				return requested;
			}
			if (i < 5) {
				System.gc();
				try {
					TimeUnit.MILLISECONDS.sleep(1);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}

		long free = getFreeMemory(rt);
		if (SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
			logger.error(
					"Attempt to read {} MB but only {} MB free heap available. Truncating to {} MB due to soft-fail mode.",
					requested / (1024 * 1024), free / (1024 * 1024), SOFT_FAIL_CAP_BYTES / (1024 * 1024));
			return SOFT_FAIL_CAP_BYTES;
		}
		throw new IOException("Attempt to read " + (requested / (1024 * 1024)) + " MB but only "
				+ (free / (1024 * 1024))
				+ " MB free heap available. This may indicate corrupted data length. Consider enabling soft-fail mode via system property 'org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes'=true to attempt recovery.");
	}

	@InternalUseOnly
	public long getFreeMemory(Runtime rt) {
		// this method is overridden in tests to simulate low-heap conditions
		long allocated = rt.totalMemory() - rt.freeMemory();
		return (rt.maxMemory() - allocated);
	}

	private static int getConfiguredLargeReadThreshold() {
		int defaultThreshold = defaultLargeReadThreshold();
		String configured = System.getProperty(LARGE_READ_THRESHOLD_PROPERTY);
		if (configured == null || configured.isBlank()) {
			logger.debug(
					"Using default large read threshold of {} MB. To configure, set system property {} to a positive integer value in bytes.",
					defaultThreshold / (1024 * 1024), LARGE_READ_THRESHOLD_PROPERTY);
			return defaultThreshold;
		}
		try {
			int parsed = Integer.parseInt(configured.trim());
			if (parsed <= 0) {
				logger.warn(
						"Ignoring non-positive value {} for system property {}. Falling back to {} MB.",
						configured, LARGE_READ_THRESHOLD_PROPERTY, defaultThreshold / (1024 * 1024));
				return defaultThreshold;
			}
			return parsed;
		} catch (NumberFormatException e) {
			logger.warn(
					"Ignoring non-numeric value {} for system property {}. Falling back to {} MB.",
					configured, LARGE_READ_THRESHOLD_PROPERTY, defaultThreshold / (1024 * 1024));
			return defaultThreshold;
		}
	}

	private static int defaultLargeReadThreshold() {
		long maxMemory = Runtime.getRuntime().maxMemory();
		if (maxMemory <= 0) {
			return FALLBACK_LARGE_READ_THRESHOLD;
		}
		long threshold = maxMemory / 16L;
		if (threshold <= 0) {
			return FALLBACK_LARGE_READ_THRESHOLD;
		}
		return Math.toIntExact(Math.min(threshold, Integer.MAX_VALUE));
	}

	private static final class ReadOnlyMapping {

		private static final ReadOnlyMapping EMPTY = new ReadOnlyMapping(new MappingSegment[0], 0);

		private final MappingSegment[] segments;
		private final long mappedSize;

		private ReadOnlyMapping(MappingSegment[] segments, long mappedSize) {
			this.segments = segments;
			this.mappedSize = mappedSize;
		}

		boolean contains(long endExclusive) {
			return endExclusive <= mappedSize;
		}

		ReadOnlyMapping append(List<MappingSegment> additions, long newMappedSize) {
			if (additions.isEmpty()) {
				return this;
			}
			MappingSegment[] merged = Arrays.copyOf(segments, segments.length + additions.size());
			for (int i = 0; i < additions.size(); i++) {
				merged[segments.length + i] = additions.get(i);
			}
			return new ReadOnlyMapping(merged, newMappedSize);
		}

		MappingSegment segmentFor(long offset) {
			if (segments.length == 0) {
				throw new IllegalStateException("No mapped segments available for offset " + offset);
			}
			int low = 0;
			int high = segments.length - 1;
			while (low <= high) {
				int mid = (low + high) >>> 1;
				MappingSegment segment = segments[mid];
				if (offset < segment.start) {
					high = mid - 1;
				} else if (offset >= segment.end) {
					low = mid + 1;
				} else {
					return segment;
				}
			}
			throw new IllegalArgumentException("Offset " + offset + " outside mapped region [0, " + mappedSize + ")");
		}
	}

	private static final class MappingSegment {

		private final long start;
		private final long end;
		private final MappedByteBuffer buffer;

		private MappingSegment(long start, long end, MappedByteBuffer buffer) {
			this.start = start;
			this.end = end;
			this.buffer = buffer;
		}
	}

	/**
	 * Discards all stored data.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	synchronized public void clear() throws IOException {
		nioFile.truncate(HEADER_LENGTH);
		nioFileSize = HEADER_LENGTH;
		buffer.clear();
		readOnlyMapping = ReadOnlyMapping.EMPTY;
	}

	/**
	 * Syncs any unstored data to the hash file.
	 */
	synchronized public void sync() throws IOException {
		flush();

		if (forceSync) {
			nioFile.force(false);
		}
	}

	synchronized public void sync(boolean force) throws IOException {
		flush();

		nioFile.force(force);
	}

	/**
	 * Closes the data file, releasing any file locks that it might have.
	 *
	 * @throws IOException
	 */
	@Override
	synchronized public void close() throws IOException {
		flush();
		nioFile.force(true);
		readOnlyMapping = ReadOnlyMapping.EMPTY;
		nioFile.close();
	}

	/**
	 * Gets an iterator that can be used to iterate over all stored data.
	 *
	 * @return a DataIterator.
	 */
	public DataIterator iterator() {
		try {
			flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new DataIterator();
	}

	/**
	 * An iterator that iterates over the data that is stored in a data file.
	 */
	public class DataIterator {

		private long position = HEADER_LENGTH;

		public boolean hasNext() {
			return position < nioFileSize;
		}

		public byte[] next() throws IOException {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			byte[] data = getData(position);
			position += (4 + data.length);
			return data;
		}
	}
}
