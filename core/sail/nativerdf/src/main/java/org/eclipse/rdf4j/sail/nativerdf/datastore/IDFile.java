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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.math.LongMath;

/**
 * Class supplying access to an ID file. An ID file maps IDs (integers &gt;= 1) to file pointers (long integers). There
 * is a direct correlation between IDs and the position at which the file pointers are stored; the file pointer for ID X
 * is stored at position 8*X.
 * <p>
 * This class supports parallel reads but not parallel writes.
 *
 * @author Arjohn Kampman
 */
public class IDFile implements Closeable {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Magic number "Native ID File" to detect whether the file is actually an ID file. The first three bytes of the
	 * file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'n', 'i', 'f' };

	/**
	 * File format version, stored as the fourth byte in ID files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	/**
	 * The size of the file header in bytes. The file header contains the following data: magic number (3 bytes) file
	 * format version (1 byte) and 4 dummy bytes to align data at 8-byte offsets.
	 */
	private static final long HEADER_LENGTH = 8;

	private static final long ITEM_SIZE = 8L;

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

	// A cache for lines read from the file index. This cache is unlimited size and uses soft values to allow GC of
	// unreferenced lines when under memory pressure.
	private final Cache<Integer, Long[]> cache = CacheBuilder.newBuilder().softValues().build();

	// We choose a cacheLineSize of 4KB since this is a typical file system block size.
	private final int blockSize = 4 * 1024; // 4KB
	private final int cacheLineSize = (int) (blockSize / ITEM_SIZE);
	private final int cacheLineShift = LongMath.log2(cacheLineSize, RoundingMode.UNNECESSARY);

	// keeping a reference of the last created cache line here should stop GC from removing it
	private int gcReducingCacheIndex;
	private Long[] gcReducingCache;

	// cached file size
	private volatile long fileSize;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public IDFile(File file) throws IOException {
		this(file, false);
	}

	public IDFile(File file, boolean forceSync) throws IOException {
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
				writeByte(FILE_FORMAT_VERSION, 3L);
				writeBytes(new byte[] { 0, 0, 0, 0 }, 4L);
				sync();
			} else if (initialSize < HEADER_LENGTH) {
				throw new IOException("File too small to be a compatible ID file");
			} else {
				if (!Arrays.equals(MAGIC_NUMBER, readBytes(0L, MAGIC_NUMBER.length))) {
					throw new IOException("File doesn't contain compatible ID records");
				}

				byte version = readByte(MAGIC_NUMBER.length);
				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read ID file; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read ID file; invalid file format version: " + version);
				}
			}

			fileSize = size();
		} catch (IOException e) {
			closeQuietly();
			throw e;
		}

	}

	/*---------*
	 * Methods *
	 *---------*/

	public final File getFile() {
		return path.toFile();
	}

	/**
	 * Gets the largest ID that is stored in this ID file.
	 *
	 * @return The largest ID, or <var>0</var> if the file does not contain any data.
	 */
	public int getMaxID() {
		return (int) (fileSize / ITEM_SIZE) - 1;
	}

	/**
	 * Stores the offset of a new data entry, returning the ID under which is stored.
	 */
	public int storeOffset(long offset) throws IOException {
		long currentSize = fileSize;
		writeLong(offset, currentSize);
		fileSize += ITEM_SIZE;
		return (int) (currentSize / ITEM_SIZE);
	}

	/**
	 * Sets or updates the stored offset for the specified ID.
	 *
	 * @param id     The ID to set the offset for, must be larger than 0.
	 * @param offset The (new) offset for the specified ID.
	 */
	public void setOffset(int id, long offset) throws IOException {
		assert id > 0 : "id must be larger than 0, is: " + id;

		writeLong(offset, ITEM_SIZE * id);

		// We need to update the cache after writing to file (not before) so that if anyone refreshes the cache it will
		// include the write above.
		// The scenario is as follows:
		// 1. there is nothing in the cache, everything is fine
		// 2. the relevant cache line is from before the writeLong operation above, in which case we update it
		// 3. the relevant cache line is from right after the write in which case updating it doesnt matter

		int cacheLookupIndex = id >> cacheLineShift;
		int cacheLineLookupIndex = id % cacheLineSize;

		Long[] cacheLine = getCacheLine(cacheLookupIndex);

		if (cacheLine != null) {
			cacheLine[cacheLineLookupIndex] = offset;
		}

	}

	/**
	 * Gets the offset of the data entry with the specified ID.
	 *
	 * @param id The ID to get the offset for, must be larger than 0.
	 * @return The offset for the ID.
	 */
	public long getOffset(int id) throws IOException {
		assert id > 0 : "id must be larger than 0, is: " + id;

		// the index used to lookup the cache line
		int cacheLookupIndex = id >> cacheLineShift;

		// the index used to lookup the actual value inside the cache line
		int cacheLineLookupIndex = id % cacheLineSize;

		// the cache line which is of size cacheLineSize
		Long[] cacheLine = getCacheLine(cacheLookupIndex);

		if (cacheLine != null) {
			return cacheLine[cacheLineLookupIndex];
		}

		// We only cache complete lines of size cacheLineSize. This means that the last line in the file will almost
		// never be cached. This simplifies the code since we don't have to deal with partial lines.
		if (getMaxID() > cacheLineSize && id < getMaxID() - cacheLineSize) {

			// doing one big read is considerably faster than doing a single read per id
			byte[] bytes = readBytes(ITEM_SIZE * (cacheLookupIndex << cacheLineShift),
					(int) (ITEM_SIZE * cacheLineSize));

			cacheLine = convertBytesToLongs(bytes);

			synchronized (this) {
				// we try not to overwrite an existing cache line
				if (cache.getIfPresent(cacheLineLookupIndex) == null) {
					cache.put(cacheLookupIndex, cacheLine);
				}
				gcReducingCache = cacheLine;
				gcReducingCacheIndex = cacheLookupIndex;
			}

			return cacheLine[cacheLineLookupIndex];

		}

		// we did not find a cached value and we did not create a new cache line
		return readLong(ITEM_SIZE * id);
	}

	/**
	 * Gets the offset directly from the underlying file, bypassing any caches. Useful for validating cached results
	 * when diagnosing corruption.
	 *
	 * @param id The ID to get the offset for, must be larger than 0.
	 * @return the raw offset stored for the id
	 * @throws IOException if an I/O error occurs
	 */
	public long getOffsetNoCache(int id) throws IOException {
		assert id > 0 : "id must be larger than 0, is: " + id;
		return readLong(ITEM_SIZE * id);
	}

	/**
	 * Discards all stored data.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void clear() throws IOException {
		truncate(HEADER_LENGTH);
		fileSize = HEADER_LENGTH;
		clearCache();
	}

	/**
	 * Syncs any unstored data to the hash file.
	 */
	public void sync() throws IOException {
		if (forceSync) {
			force(false);
		}
	}

	public void sync(boolean force) throws IOException {
		force(force);
	}

	/**
	 * Closes the ID file, releasing any file locks that it might have.
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		closeChannel();
	}

	synchronized private Long[] getCacheLine(int cacheLookupIndex) {
		if (cacheLookupIndex == gcReducingCacheIndex) {
			return gcReducingCache;
		} else {
			return cache.getIfPresent(cacheLookupIndex);
		}
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

	private void writeLong(long value, long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
		buf.putLong(0, value);
		buf.position(0);
		writeFully(buf, offset);
	}

	private long readLong(long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
		int read = readFully(buf, offset);
		if (read < Long.BYTES) {
			throw new IOException("Unexpected EOF in IDFile.readLong: expected " + Long.BYTES + ", read " + read);
		}
		return buf.getLong(0);
	}

	private void writeBytes(byte[] value, long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(value);
		writeFully(buf, offset);
	}

	private byte[] readBytes(long offset, int length) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(length);
		int read = readFully(buf, offset);
		if (read < length) {
			throw new IOException("Unexpected EOF in IDFile.readBytes: expected " + length + ", read " + read);
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

	private Long[] convertBytesToLongs(byte[] bytes) {
		Long[] cacheLine;
		cacheLine = new Long[cacheLineSize];
		for (int i = 0; i < bytes.length; i += ITEM_SIZE) {
			long l = ((long) (bytes[i + 0] & 0xff) << 56)
					| ((long) bytes[i + 1] & 0xff) << 48
					| ((long) bytes[i + 2] & 0xff) << 40
					| ((long) bytes[i + 3] & 0xff) << 32
					| ((long) bytes[i + 4] & 0xff) << 24
					| ((long) bytes[i + 5] & 0xff) << 16
					| ((long) bytes[i + 6] & 0xff) << 8
					| ((long) bytes[i + 7] & 0xff);

			cacheLine[(int) (i / ITEM_SIZE)] = l;
		}
		return cacheLine;
	}

	synchronized public void clearCache() {
		cache.invalidateAll();
		gcReducingCacheIndex = -1;
		gcReducingCache = null;
	}

}
