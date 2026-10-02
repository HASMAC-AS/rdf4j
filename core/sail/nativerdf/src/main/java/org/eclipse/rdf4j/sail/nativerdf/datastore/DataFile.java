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
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
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

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final Set<StandardOpenOption> FILE_OPEN_OPTIONS = EnumSet
			.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);

	private final Path path;
	private final Set<StandardOpenOption> openOptions;
	private final Object channelLock = new Object();
	private volatile FileChannel fileChannel;
	private volatile boolean explicitlyClosed;

	private final boolean forceSync;

	// cached file size, also reflects buffer usage
	private volatile long fileSize;

	// 4KB write buffer that is flushed on sync, close and any read operations
	private final ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DataFile(File file) throws IOException {
		this(file, false);
	}

	public DataFile(File file, boolean forceSync) throws IOException {
		this.path = file.toPath();
		this.openOptions = EnumSet.copyOf(FILE_OPEN_OPTIONS);
		this.forceSync = forceSync;
		this.explicitlyClosed = false;

		try {
			synchronized (channelLock) {
				this.fileChannel = FileChannel.open(path, openOptions);
			}

			long initialSize = size();
			if (initialSize == 0L) {
				writeBytes(MAGIC_NUMBER, 0L);
				writeByte(FILE_FORMAT_VERSION, MAGIC_NUMBER.length);
				sync();
			} else if (initialSize < HEADER_LENGTH) {
				throw new IOException("File too small to be a compatible data file");
			} else {
				byte[] magic = readBytes(0L, MAGIC_NUMBER.length);
				if (!Arrays.equals(MAGIC_NUMBER, magic)) {
					throw new IOException("File doesn't contain compatible data records");
				}

				byte version = readByte(MAGIC_NUMBER.length);
				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read data file; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read data file; invalid file format version: " + version);
				}
			}

			this.fileSize = size();
		} catch (IOException e) {
			closeQuietly();
			throw e;
		}

	}

	/*---------*
	 * Methods *
	 *---------*/

	public File getFile() {
		return path.toFile();
	}

	/**
	 * Returns the current file size (after flushing any pending writes).
	 */
	public long getFileSize() throws IOException {
		flush();
		return fileSize;
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
		return readBytes(startOffset + 4L, toRead);
	}

	/**
	 * Stores the specified data and returns the byte-offset at which it has been stored.
	 *
	 * @param data The data to store, must not be <var>null</var>.
	 * @return The byte-offset in the file at which the data was stored.
	 */
	synchronized public long storeData(byte[] data) throws IOException {
		assert data != null : "data must not be null";

		long offset = fileSize;

		if (data.length + 4 > buffer.capacity()) {
			// direct write because we are writing more data than the buffer can hold

			flush();

			// TODO: two writes could be more efficient since it prevent array copies
			ByteBuffer buf = ByteBuffer.allocate(data.length + 4);
			buf.putInt(data.length);
			buf.put(data);
			buf.flip();
			int total = buf.remaining();

			writeFully(buf, offset);

			fileSize += total;

		} else {
			if (data.length + 4 > remainingBufferCapacity()) {
				flush();
			}

			buffer.putInt(data.length);
			buffer.put(data);
			fileSize += data.length + 4;
		}

		return offset;

	}

	synchronized private void flush() throws IOException {
		int position = buffer.position();
		if (position == 0) {
			return;
		}
		buffer.flip();
		writeFully(buffer, fileSize - position);
		buffer.clear();

	}

	synchronized private int remainingBufferCapacity() {
		return buffer.capacity() - buffer.position();
	}

	// This variable is used for predicting the number of bytes to read in getData(long offset). This helps us to only
	// need to execute a single IO read instead of first one read to find the length and then one read to read the data.
	int dataLengthApproximateAverage = 25;

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

		// Read in twice the average length because multiple small read operations take more time than one single larger
		// operation even if that larger operation is unnecessarily large (within sensible limits).
		byte[] data = new byte[(dataLengthApproximateAverage * 2) + 4];
		ByteBuffer buf = ByteBuffer.wrap(data);
		readFully(buf, offset);

		int dataLength = (data[0] << 24) & 0xff000000 |
				(data[1] << 16) & 0x00ff0000 |
				(data[2] << 8) & 0x0000ff00 |
				(data[3]) & 0x000000ff;

		// Validate and possibly reduce the length before allocating a large array
		dataLength = guardedDataLength(dataLength);

		try {

			// We have either managed to read enough data and can return the required subset of the data, or we have
			// read
			// too little so we need to execute another read to get the correct data.
			if (dataLength <= data.length - 4) {

				// adjust the approximate average with 1 part actual length and 99 parts previous average up to a
				// sensible
				// max of 200
				dataLengthApproximateAverage = (int) Math.max(0, Math.min(200,
						((dataLengthApproximateAverage / 100.0) * 99) + (dataLength / 100.0)));

				int i = dataLength + 4;
				if (i < 0 || i > data.length) {
					throw new IOException("Corrupt data record at offset " + offset + ". Data length: " + dataLength);
				}

				return Arrays.copyOfRange(data, 4, i);

			} else {

				// adjust the approximate average, but favour the actual dataLength since dataLength predictions misses
				// are costly
				dataLengthApproximateAverage = Math.max(0,
						Math.min(200, (dataLengthApproximateAverage + dataLength) / 2));

				// we didn't read enough data so we need to execute a new read
				data = new byte[dataLength];
				buf = ByteBuffer.wrap(data);
				readFully(buf, offset + 4L);

				return data;
			}
		} catch (OutOfMemoryError e) {
			if (dataLength > LARGE_READ_THRESHOLD) {
				logger.error(
						"Trying to read large amounts of data may be a sign of data corruption. Consider setting the system property org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes to true");
			}
			throw e;
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

	private FileChannel ensureChannel() throws IOException {
		FileChannel channel = fileChannel;
		if (channel != null && channel.isOpen()) {
			return channel;
		}

		synchronized (channelLock) {
			if (explicitlyClosed) {
				throw new ClosedChannelException();
			}
			channel = fileChannel;
			if (channel == null || !channel.isOpen()) {
				fileChannel = FileChannel.open(path, openOptions);
				channel = fileChannel;
			}
			return channel;
		}
	}

	private void reopen(ClosedChannelException e) throws IOException {
		if (explicitlyClosed) {
			throw e;
		}

		synchronized (channelLock) {
			FileChannel channel = fileChannel;
			if (channel != null && channel.isOpen()) {
				return;
			}
			fileChannel = FileChannel.open(path, openOptions);
		}
	}

	private long size() throws IOException {
		while (true) {
			try {
				return ensureChannel().size();
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	private void truncate(long newSize) throws IOException {
		while (true) {
			try {
				ensureChannel().truncate(newSize);
				return;
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	private void force(boolean metaData) throws IOException {
		while (true) {
			try {
				ensureChannel().force(metaData);
				return;
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	private int writeFully(ByteBuffer buffer, long offset) throws IOException {
		final int startPosition = buffer.position();
		while (buffer.hasRemaining()) {
			try {
				FileChannel channel = ensureChannel();
				long position = offset + (buffer.position() - startPosition);
				int written = channel.write(buffer, position);
				if (written == 0) {
					continue;
				}
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
		return buffer.position() - startPosition;
	}

	private int readFully(ByteBuffer buffer, long offset) throws IOException {
		final int startPosition = buffer.position();
		while (buffer.hasRemaining()) {
			try {
				FileChannel channel = ensureChannel();
				long position = offset + (buffer.position() - startPosition);
				int read = channel.read(buffer, position);
				if (read < 0) {
					if (buffer.position() == startPosition) {
						return -1;
					}
					break;
				}
				if (read == 0) {
					continue;
				}
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
		return buffer.position() - startPosition;
	}

	private void writeBytes(byte[] value, long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(value);
		writeFully(buf, offset);
	}

	private byte[] readBytes(long offset, int length) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(length);
		int read = readFully(buf, offset);
		if (read < length) {
			throw new IOException("Unexpected EOF in DataFile.readBytes: expected " + length + ", read " + read);
		}
		return buf.array();
	}

	private void writeByte(byte value, long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1);
		buf.put(0, value);
		buf.position(0);
		writeFully(buf, offset);
	}

	private byte readByte(long offset) throws IOException {
		byte[] bytes = readBytes(offset, 1);
		return bytes[0];
	}

	private void closeChannel() throws IOException {
		FileChannel channel;
		synchronized (channelLock) {
			if (explicitlyClosed) {
				channel = fileChannel;
				fileChannel = null;
			} else {
				explicitlyClosed = true;
				channel = fileChannel;
				fileChannel = null;
			}
		}
		if (channel != null) {
			channel.close();
		}
	}

	private void closeQuietly() {
		try {
			closeChannel();
		} catch (IOException e) {
			// ignore
		}
	}

	/**
	 * Discards all stored data.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	synchronized public void clear() throws IOException {
		truncate(HEADER_LENGTH);
		fileSize = HEADER_LENGTH;
		buffer.clear();
	}

	/**
	 * Syncs any unstored data to the hash file.
	 */
	synchronized public void sync() throws IOException {
		flush();

		if (forceSync) {
			force(false);
		}
	}

	synchronized public void sync(boolean force) throws IOException {
		flush();

		force(force);
	}

	/**
	 * Closes the data file, releasing any file locks that it might have.
	 *
	 * @throws IOException
	 */
	@Override
	synchronized public void close() throws IOException {
		flush();
		try {
			force(true);
		} finally {
			closeChannel();
		}
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
			return position < fileSize;
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
