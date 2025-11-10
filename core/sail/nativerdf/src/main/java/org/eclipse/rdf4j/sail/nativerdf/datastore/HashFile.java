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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.rdf4j.common.io.NioFile;

/**
 * Class supplying access to a hash file.
 *
 * @author Arjohn Kampman
 */
public class HashFile implements Closeable {

	/*-----------*
	 * Constants *
	 *-----------*/

	// The size of an item (32-bit hash + 32-bit ID), in bytes
	private static final int ITEM_SIZE = 8;

	/**
	 * Magic number "Native Hash File" to detect whether the file is actually a hash file. The first three bytes of the
	 * file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'n', 'h', 'f' };

	/**
	 * File format version, stored as the fourth byte in hash files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	/**
	 * The size of the file header in bytes. The file header contains the following data: magic number (3 bytes) file
	 * format version (1 byte), number of buckets (4 bytes), bucket size (4 bytes) and number of stored items (4 bytes).
	 */
	private static final long HEADER_LENGTH = 16;

	private static final int INIT_BUCKET_SIZE = 8;

	private static final int FORCE_CHUNK_BUCKETS = 1024;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final NioFile nioFile;

	private final boolean forceSync;

	// The number of (non-overflow) buckets in the hash file
	private volatile int bucketCount;

	// The number of items that can be stored in a bucket
	private final int bucketSize;

	// The number of items in the hash file
	private volatile int itemCount;

	// Load factor (fixed, for now)
	private final float loadFactor;

	// recordSize = ITEM_SIZE * bucketSize + 4
	private final int recordSize;

	private final byte[] emptyBucketTemplate;

	// first prime > 5MB
	private final BitSet poorMansBloomFilter;

	boolean loadedHashFileFromDisk = false;

	private volatile boolean tableDirty;

	/**
	 * A read/write lock that is used to prevent structural changes to the hash file while readers are active in order
	 * to prevent concurrency issues.
	 */
	private final ReentrantReadWriteLock structureLock = new ReentrantReadWriteLock();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public HashFile(File file) throws IOException {
		this(file, false);
	}

	public HashFile(File file, boolean forceSync) throws IOException {
		this(file, forceSync, 512); // 512 is default initial size
	}

	public HashFile(File file, boolean forceSync, int initialSize) throws IOException {
		this.nioFile = new NioFile(file);
		this.forceSync = forceSync;
		loadFactor = 0.75f;

		try {
			if (nioFile.size() == 0L) {
				// Empty file, insert bucket count, bucket size
				// and item count at the start of the file

				// the bucket count handles sizes not divisible by INIT_BUCKET_SIZE
				bucketCount = (int) Math.ceil(initialSize * 1.0 / INIT_BUCKET_SIZE);
				bucketSize = INIT_BUCKET_SIZE;
				itemCount = 0;
				recordSize = ITEM_SIZE * bucketSize + 4;
				emptyBucketTemplate = new byte[recordSize];

				// Initialize the file by writing <_bucketCount> empty buckets
				writeEmptyBuckets(HEADER_LENGTH, bucketCount);

				sync();
			} else {
				// Read bucket count, bucket size and item count from the file
				ByteBuffer buf = ByteBuffer.allocate((int) HEADER_LENGTH);
				nioFile.read(buf, 0L);
				buf.rewind();

				if (buf.remaining() < HEADER_LENGTH) {
					throw new IOException("File too short to be a compatible hash file");
				}

				byte[] magicNumber = new byte[MAGIC_NUMBER.length];
				buf.get(magicNumber);
				byte version = buf.get();
				bucketCount = buf.getInt();
				bucketSize = buf.getInt();
				itemCount = buf.getInt();

				if (!Arrays.equals(MAGIC_NUMBER, magicNumber)) {
					throw new IOException("File doesn't contain compatible hash file data");
				}

				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read hash file; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read hash file; invalid file format version: " + version);
				}

				recordSize = ITEM_SIZE * bucketSize + 4;
				emptyBucketTemplate = new byte[recordSize];
				loadedHashFileFromDisk = itemCount > 0;
			}

			if (!loadedHashFileFromDisk) {
				// 41943049 is ~5MB, and a prime
				if (initialSize > 41943049) {
					// initialSize < Integer.MAX_VALUE and Integer.MAX_VALUE = ~250 MB
					poorMansBloomFilter = new BitSet(initialSize);
				} else {
					poorMansBloomFilter = new BitSet(41943049);
				}
			} else {
				poorMansBloomFilter = null;
			}
		} catch (IOException e) {
			this.nioFile.close();
			throw e;
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	public File getFile() {
		return nioFile.getFile();
	}

	public int getItemCount() {
		return itemCount;
	}

	/**
	 * Gets an iterator that iterates over the IDs with hash codes that match the specified hash code.
	 */
	public IDIterator getIDIterator(int hash) throws IOException {
		if (!loadedHashFileFromDisk && !poorMansBloomFilter.get(getBloomFilterIndex(hash))) {
			return emptyIDIterator;
		} else {
			return new IDIterator(hash);
		}
	}

	private int getBloomFilterIndex(int hash) {
		return Math.abs(hash) % poorMansBloomFilter.size();
	}

	/**
	 * Stores ID under the specified hash code in this hash file.
	 */
	public void storeID(int hash, int id) throws IOException {
		structureLock.readLock().lock();
		if (!loadedHashFileFromDisk) {
			poorMansBloomFilter.set(getBloomFilterIndex(hash), true);
		}
		try {
			// Calculate bucket offset for initial bucket
			long bucketOffset = getBucketOffset(hash);
			storeID(bucketOffset, hash, id);
		} finally {
			structureLock.readLock().unlock();
		}

		if (++itemCount >= loadFactor * bucketCount * bucketSize) {
			structureLock.writeLock().lock();
			try {
				increaseHashTable();
			} finally {
				structureLock.writeLock().unlock();
			}
		}
	}

	private void storeID(long bucketOffset, int hash, int id) throws IOException {
		MappedByteBuffer bucket = mapBucket(bucketOffset);

		while (true) {
			int slotID = findEmptySlotInBucket(bucket);

			if (slotID >= 0) {
				bucket.putInt(ITEM_SIZE * slotID, hash);
				bucket.putInt(ITEM_SIZE * slotID + 4, id);
				tableDirty = true;
				return;
			}

			int overflowID = bucket.getInt(ITEM_SIZE * bucketSize);

			if (overflowID == 0) {
				overflowID = createOverflowBucket();
				bucket.putInt(ITEM_SIZE * bucketSize, overflowID);
				tableDirty = true;
			}

			bucketOffset = getOverflowBucketOffset(overflowID);
			bucket = mapBucket(bucketOffset);
		}
	}

	public void clear() throws IOException {
		structureLock.writeLock().lock();
		if (poorMansBloomFilter != null) {
			poorMansBloomFilter.clear();
		}
		try {
			// Truncate the file to remove any overflow buffers
			nioFile.truncate(HEADER_LENGTH + (long) bucketCount * recordSize);

			// Overwrite normal buckets with empty ones
			writeEmptyBuckets(HEADER_LENGTH, bucketCount);

			itemCount = 0;
			tableDirty = true;
		} finally {
			structureLock.writeLock().unlock();
		}
	}

	/**
	 * Syncs any unstored data to the hash file.
	 */
	public void sync() throws IOException {
		structureLock.readLock().lock();
		try {
			// Update the file header
			writeFileHeader();
		} finally {
			structureLock.readLock().unlock();
		}

		if (forceSync) {
			forceMappedBuckets();
			nioFile.force(false);
		}
	}

	public void sync(boolean force) throws IOException {
		sync();
		if (force) {
			forceMappedBuckets();
		}
		// Always honor explicit requests to force metadata to disk while preserving data durability for sync(false)
		nioFile.force(force);
	}

	@Override
	public void close() throws IOException {
		// Persist current header (bucketCount, bucketSize, itemCount) before closing
		// to ensure readers after reopen see the correct table layout even if no
		// explicit sync() was called after structural changes (e.g., rehash).
		try {
			sync(true);
		} finally {
			nioFile.close();
		}
	}

	/*-----------------*
	 * Utility methods *
	 *-----------------*/

	private RandomAccessFile createEmptyFile(File file) throws IOException {
		// Make sure the file exists
		if (!file.exists()) {
			boolean created = file.createNewFile();
			if (!created) {
				throw new IOException("Failed to create file " + file);
			}
		}

		// Open the file in read-write mode and make sure the file is empty
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.setLength(0L);

		return raf;
	}

	/**
	 * Writes the bucket count, bucket size and item count to the file header.
	 */
	private void writeFileHeader() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate((int) HEADER_LENGTH);
		buf.put(MAGIC_NUMBER);
		buf.put(FILE_FORMAT_VERSION);
		buf.putInt(bucketCount);
		buf.putInt(bucketSize);
		buf.putInt(itemCount);
		buf.rewind();

		nioFile.write(buf, 0L);
	}

	/**
	 * Returns the offset of the bucket for the specified hash code.
	 */
	private long getBucketOffset(int hash) {
		int bucketNo = hash % bucketCount;
		if (bucketNo < 0) {
			bucketNo += bucketCount;
		}
		return HEADER_LENGTH + (long) bucketNo * recordSize;
	}

	/**
	 * Returns the offset of the overflow bucket with the specified ID.
	 */
	private long getOverflowBucketOffset(int bucketID) {
		return HEADER_LENGTH + ((long) bucketCount + (long) bucketID - 1L) * recordSize;
	}

	/**
	 * Creates a new overflow bucket and returns its ID.
	 */
	private int createOverflowBucket() throws IOException {
		long offset = nioFile.size();
		writeEmptyBuckets(offset, 1);
		return (int) ((offset - HEADER_LENGTH) / recordSize) - bucketCount + 1;
	}

	private void writeEmptyBuckets(long fileOffset, int bucketCount) throws IOException {
		if (bucketCount <= 0) {
			return;
		}

		long requiredSize = fileOffset + (long) bucketCount * recordSize;
		if (nioFile.size() < requiredSize) {
			nioFile.truncate(requiredSize);
		}

		for (int i = 0; i < bucketCount; i++) {
			long bucketOffset = fileOffset + (long) i * recordSize;
			MappedByteBuffer bucket = nioFile.map(MapMode.READ_WRITE, bucketOffset, recordSize);
			bucket.position(0);
			bucket.put(emptyBucketTemplate);
		}

		tableDirty = true;
	}

	private MappedByteBuffer mapBucket(long bucketOffset) throws IOException {
		return nioFile.map(MapMode.READ_WRITE, bucketOffset, recordSize);
	}

	private void forceMappedBuckets() throws IOException {
		if (!tableDirty) {
			return;
		}

		long tableSize = HEADER_LENGTH + (long) bucketCount * recordSize;
		long currentSize = Math.min(tableSize, nioFile.size());
		if (currentSize <= HEADER_LENGTH) {
			tableDirty = false;
			return;
		}

		long offset = HEADER_LENGTH;
		long maxChunkBytes = (long) recordSize * FORCE_CHUNK_BUCKETS;
		if (maxChunkBytes <= 0) {
			maxChunkBytes = Integer.MAX_VALUE;
		}

		while (offset < currentSize) {
			long remaining = currentSize - offset;
			long chunkBytes = Math.min(remaining, maxChunkBytes);
			chunkBytes = Math.min(chunkBytes, Integer.MAX_VALUE);
			if (chunkBytes <= 0) {
				break;
			}

			MappedByteBuffer mappedRegion = nioFile.map(MapMode.READ_WRITE, offset, chunkBytes);
			mappedRegion.force();
			offset += chunkBytes;
		}

		tableDirty = false;
	}

	private int findEmptySlotInBucket(ByteBuffer bucket) {
		for (int slotNo = 0; slotNo < bucketSize; slotNo++) {
			// Check for offsets that are equal to 0
			if (bucket.getInt(ITEM_SIZE * slotNo + 4) == 0) {
				return slotNo;
			}
		}

		return -1;
	}

	/**
	 * Double the number of buckets in the hash file and rehashes the stored items.
	 */
	private void increaseHashTable() throws IOException {
		long oldTableSize = HEADER_LENGTH + (long) bucketCount * recordSize;
		long newTableSize = HEADER_LENGTH + (long) bucketCount * recordSize * 2;
		long oldFileSize = nioFile.size(); // includes overflow buckets

		File tmpFile = new File(getFile().getParentFile(), "rehash_" + getFile().getName());
		try (RandomAccessFile tmpRaf = createEmptyFile(tmpFile)) {
			FileChannel tmpChannel = tmpRaf.getChannel();
			nioFile.transferTo(oldTableSize, oldFileSize - oldTableSize, tmpChannel);

			writeEmptyBuckets(oldTableSize, bucketCount);
			bucketCount *= 2;
			nioFile.truncate(newTableSize);

			for (long bucketOffset = HEADER_LENGTH; bucketOffset < oldTableSize; bucketOffset += recordSize) {
				MappedByteBuffer bucket = mapBucket(bucketOffset);
				boolean bucketChanged = false;

				for (int slotNo = 0; slotNo < bucketSize; slotNo++) {
					int id = bucket.getInt(ITEM_SIZE * slotNo + 4);

					if (id != 0) {
						int hash = bucket.getInt(ITEM_SIZE * slotNo);
						long newOffset = getBucketOffset(hash);

						if (newOffset != bucketOffset) {
							bucket.putInt(ITEM_SIZE * slotNo, 0);
							bucket.putInt(ITEM_SIZE * slotNo + 4, 0);
							bucketChanged = true;

							storeID(newOffset, hash, id);
						}
					}
				}

				if (bucket.getInt(ITEM_SIZE * bucketSize) != 0) {
					bucket.putInt(ITEM_SIZE * bucketSize, 0);
					bucketChanged = true;
				}

				if (bucketChanged) {
					tableDirty = true;
				}
			}

			ByteBuffer overflowBucket = ByteBuffer.allocate(recordSize);
			long tmpFileSize = tmpChannel.size();
			for (long bucketOffset = 0L; bucketOffset < tmpFileSize; bucketOffset += recordSize) {
				overflowBucket.clear();
				int read = tmpChannel.read(overflowBucket, bucketOffset);
				if (read <= 0) {
					continue;
				}
				overflowBucket.position(0);

				for (int slotNo = 0; slotNo < bucketSize; slotNo++) {
					int id = overflowBucket.getInt(ITEM_SIZE * slotNo + 4);

					if (id != 0) {
						int hash = overflowBucket.getInt(ITEM_SIZE * slotNo);
						long newBucketOffset = getBucketOffset(hash);

						storeID(newBucketOffset, hash, id);
					}
				}
			}
		}
		tmpFile.delete();
		sync(true);
	}

	/*------------------------*
	 * Inner class IDIterator *
	 *------------------------*/

	private final IDIterator emptyIDIterator = new IDIterator() {
		@Override
		public void close() {

		}

		@Override
		public int next() {
			return -1;
		}
	};

	public class IDIterator {

		private final int queryHash;

		private MappedByteBuffer bucketBuffer;

		private int slotNo;

		private IDIterator(int hash) throws IOException {
			queryHash = hash;
			structureLock.readLock().lock();
			try {
				// Read initial bucket
				long bucketOffset = getBucketOffset(hash);
				bucketBuffer = mapBucket(bucketOffset);

				slotNo = -1;
			} catch (IOException | RuntimeException e) {
				structureLock.readLock().unlock();
				throw e;
			}
		}

		IDIterator() {
			queryHash = 0;
		}

		public void close() {
			bucketBuffer = null;
			structureLock.readLock().unlock();
		}

		/**
		 * Returns the next ID that has been mapped to the specified hash code, or <var>-1</var> if no more IDs were
		 * found.
		 */
		public int next() throws IOException {
			while (bucketBuffer != null) {
				// Search in current bucket
				while (++slotNo < bucketSize) {
					if (bucketBuffer.getInt(ITEM_SIZE * slotNo) == queryHash) {
						return bucketBuffer.getInt(ITEM_SIZE * slotNo + 4);
					}
				}

				// No matching hash code in current bucket, check overflow
				// bucket
				int overflowID = bucketBuffer.getInt(ITEM_SIZE * bucketSize);
				if (overflowID == 0) {
					// No overflow bucket, end the search
					bucketBuffer = null;
					break;
				} else {
					// Continue with overflow bucket
					long bucketOffset = getOverflowBucketOffset(overflowID);
					bucketBuffer = mapBucket(bucketOffset);
					slotNo = -1;
				}
			}

			return -1;
		}

	} // End inner class IDIterator
} // End class HashFile
