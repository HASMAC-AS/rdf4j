/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.rdf4j.sail.lmdb.util.SignificantBytesBE;

/**
 * Encodes and decodes unsigned values using variable-length encoding.
 */
public final class Varint {

	static final byte[] ENCODED_LONG_MAX = new byte[] {
			(byte) 0xFF, // header: 8 payload bytes
			0x7F, // MSB of Long.MAX_VALUE
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};

	static final byte[] ENCODED_LONG_MAX_QUAD = new byte[] {
			(byte) 0xFF, // header: 8 payload bytes
			0x7F, // MSB of Long.MAX_VALUE
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, // header: 8 payload bytes
			0x7F, // MSB of Long.MAX_VALUE
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, // header: 8 payload bytes
			0x7F, // MSB of Long.MAX_VALUE
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, // header: 8 payload bytes
			0x7F, // MSB of Long.MAX_VALUE
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};

	static final byte[] ALL_ZERO_QUAD = new byte[] { 0, 0, 0, 0 };

	private static final int[] BASE_LENGTH_BY_TIER = { 1, 2, 3, 1 };
	private static final int[] BASE_EXTRA_BY_TIER = { 0, 1, 2, 0 };

	private Varint() {
	}

	/**
	 * Encodes a value using the <a href="https://sqlite.org/src4/doc/trunk/www/varint.wiki">variable-length encoding of
	 * SQLite</a>.
	 *
	 * <p>
	 * The encoding has the following properties:
	 * <ol>
	 * <li>Smaller (and more common) values use fewer bytes and take up less space than larger (and less common)
	 * values.</li>
	 * <li>The length of any varint can be determined by looking at just the first byte of the encoding.</li>
	 * <li>Lexicographical and numeric ordering for varints are the same. Hence if a group of varints are ordered
	 * lexicographically (that is to say, if they are ordered by {@link java.util.Arrays#compare(byte[], byte[])} with
	 * shorter varints coming first) then those varints will also be in numeric order. This property means that varints
	 * can be used as keys in the key/value backend storage and the records will occur in numerical order of the
	 * keys.</li>
	 * </ol>
	 * </p>
	 * <p>
	 * <table>
	 * <thead>
	 * <tr>
	 * <th>A0</th>
	 * <th>Value</th>
	 * </tr>
	 * </thead> <tbody>
	 * <tr>
	 * <td>0–240</td>
	 * <td>A0</td>
	 * </tr>
	 * <tr>
	 * <td>241–248</td>
	 * <td>240 + 256 × (A0 – 241) + A1</td>
	 * </tr>
	 * <tr>
	 * <td>249</td>
	 * <td>2288 + 256 × A1 + A2</td>
	 * </tr>
	 * <tr>
	 * <td>250</td>
	 * <td>A1…A3 as a 3-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>251</td>
	 * <td>A1…A4 as a 4-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>252</td>
	 * <td>A1…A5 as a 5-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>253</td>
	 * <td>A1…A6 as a 6-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>254</td>
	 * <td>A1…A7 as a 7-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>255</td>
	 * <td>A1…A8 as a 8-byte big-endian integer</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * </p>
	 *
	 * @param bb    buffer for writing bytes
	 * @param value value to encode
	 */
	public static void writeUnsigned(final ByteBuffer bb, final long value) {

		// Fast path for Long.MAX_VALUE (0xFF header + 8 data bytes)
		if (value == Long.MAX_VALUE) {
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				bb.put(ENCODED_LONG_MAX);
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
			return;
		}

		final int tier = classifyUnsigned(value);
		final int descriptorBytes = descriptor(value) + 1; // 1..8
		final int isTier3 = 1 ^ ((tier - 3) >>> 31);
		final long diffTier1 = value - 240;
		final long diffTier2 = value - 2288;

		int header = 0;
		header |= maskEq(tier, 0) & (int) value;
		header |= maskEq(tier, 1) & (241 + (int) (diffTier1 >>> 8));
		header |= maskEq(tier, 2) & 249;
		header |= maskEq(tier, 3) & (250 + (descriptorBytes - 3));
		bb.put((byte) header);

		final int extra = BASE_EXTRA_BY_TIER[tier] + isTier3 * descriptorBytes;
		if (extra == 0) {
			return;
		}

		if (extra == 1) {
			bb.put((byte) diffTier1);
			return;
		}

		if (extra == 2) {
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				bb.putShort((short) diffTier2);
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
			return;
		}

		writeSignificantBits(bb, value, descriptorBytes); // payload (batched)
	}

	// Writes the top `bytes` significant bytes of `value` in big-endian order.
	// Uses putLong/putInt/putShort to batch writes and a single leading byte if needed.
	private static void writeSignificantBits(ByteBuffer bb, long value, int bytes) {
		final ByteOrder prev = bb.order();
		if (prev != ByteOrder.BIG_ENDIAN) {
			bb.order(ByteOrder.BIG_ENDIAN);
		}
		try {
			int i = bytes;

			// If odd number of bytes, write the leading MSB first
			if ((i & 1) != 0) {
				bb.put((byte) (value >>> ((i - 1) * 8)));
				i--;
			}

			// Now i is even: prefer largest chunks first
			if (i == 8) { // exactly 8 bytes
				bb.putLong(value);
				return;
			}

			if (i >= 4) { // write next 4 bytes, if any
				int shift = (i - 4) * 8;
				bb.putInt((int) (value >>> shift));
				i -= 4;
			}

			while (i >= 2) { // write remaining pairs
				int shift = (i - 2) * 8;
				bb.putShort((short) (value >>> shift));
				i -= 2;
			}
			// i must be 0 here.
		} finally {
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(prev);
			}
		}
	}

	/**
	 * Calculates required length in bytes to encode the given long value using variable-length encoding.
	 *
	 * @param value the value value
	 * @return length in bytes
	 */
	public static int calcLengthUnsigned(long value) {
		int tier = classifyUnsigned(value);
		int descriptorBytes = descriptor(value) + 1;
		int isTier3 = 1 ^ ((tier - 3) >>> 31);
		return BASE_LENGTH_BY_TIER[tier] + isTier3 * descriptorBytes;
	}

	static int classifyUnsigned(long value) {
		int ge241 = (int) ((((value - 241L) >>> 63) ^ 1L) & 1L);
		int ge2288 = (int) ((((value - 2288L) >>> 63) ^ 1L) & 1L);
		int ge67824 = (int) ((((value - 67824L) >>> 63) ^ 1L) & 1L);
		return ge241 + ge2288 + ge67824;
	}

	private static int maskEq(int x, int y) {
		int diff = x ^ y;
		int same = ((diff | -diff) >>> 31) ^ 1;
		return -same;
	}

	/**
	 * Calculates required length in bytes to encode a list of four long values using variable-length encoding.
	 *
	 * @param a first value
	 * @param b second value
	 * @param c third value
	 * @param d fourth value
	 * @return length in bytes
	 */
	public static int calcListLengthUnsigned(long a, long b, long c, long d) {
		return calcLengthUnsigned(a) + calcLengthUnsigned(b) + calcLengthUnsigned(c) + calcLengthUnsigned(d);
	}

	/**
	 * The number of bytes required to represent the given number minus one. The descriptor can be encoded in 3 bits.
	 *
	 * <p>
	 * 000 = 1, 001 = 2, ..., 111 = 8
	 * </p>
	 *
	 * @param value the long value
	 * @return the descriptor encoded as byte
	 */
	private static byte descriptor(long value) {
		return value == 0 ? 0 : (byte) (7 - Long.numberOfLeadingZeros(value) / 8);
	}

	/**
	 * Decodes a value using SQLite's variable-length integer encoding.
	 *
	 * Lead-byte layout → number of additional bytes: 0..240 → 0 241..248→ 1 249 → 2 250..255→ 3..8 (i.e., 250→3, 251→4,
	 * …, 255→8)
	 *
	 * @param bb buffer for reading bytes
	 * @return decoded value
	 * @throws IllegalArgumentException if encoded varint is longer than 9 bytes
	 * @see #writeUnsigned(ByteBuffer, long)
	 */
	public static long readUnsigned(ByteBuffer bb) throws IllegalArgumentException {
		final int a0 = bb.get() & 0xFF;
		final int extra = VARINT_EXTRA_BYTES[a0];
		final long base = VARINT_BASE_VALUES[a0];

		if (extra == 0) {
			return base;
		}

		if (extra == 1) {
			return base + (bb.get() & 0xFFL);
		}

		if (extra == 2) {
			final int a1 = bb.get() & 0xFF;
			final int a2 = bb.get() & 0xFF;
			return base + ((long) a1 << 8) + a2;
		}

		if (extra >= 3 && extra <= 8) {
			return base + readSignificantBits(bb, extra);
		}

		throw new IllegalArgumentException("Bytes is higher than 8: " + extra);
	}

	public static void skipUnsigned(ByteBuffer bb) throws IllegalArgumentException {
		final int a0 = bb.get() & 0xFF; // lead byte, unsigned
		bb.position(bb.position() + VARINT_EXTRA_BYTES[a0]);

	}

	/** Lookup: lead byte (0..255) → additive base before reading payload. */
	private static final long[] VARINT_BASE_VALUES = buildVarintBaseValues();

	/** Lookup: lead byte (0..255) → number of additional bytes (0..8). */
	private static final byte[] VARINT_EXTRA_BYTES = buildVarintExtraBytes();

	private static long[] buildVarintBaseValues() {
		long[] base = new long[256];
		for (int i = 0; i <= 240; i++) {
			base[i] = i;
		}
		for (int i = 241; i <= 248; i++) {
			base[i] = 240L + ((long) (i - 241) << 8);
		}
		base[249] = 2288L;
		return base;
	}

	private static byte[] buildVarintExtraBytes() {
		final byte[] t = new byte[256];

		// 0..240 → 0 extra bytes
		for (int i = 0; i <= 240; i++) {
			t[i] = 0;
		}

		// 241..248 → 1 extra byte
		for (int i = 241; i <= 248; i++) {
			t[i] = 1;
		}

		// 249 → 2 extra bytes
		t[249] = 2;

		// 250..255 → 3..8 extra bytes
		for (int i = 250; i <= 255; i++) {
			t[i] = (byte) (i - 247); // 250→3, …, 255→8
		}

		return t;
	}

	public static long readUnsignedHeap(ByteBuffer bb) throws IllegalArgumentException {
		int a0 = bb.get() & 0xFF;
		int extra = VARINT_EXTRA_BYTES[a0];
		long base = VARINT_BASE_VALUES[a0];

		if (extra == 0) {
			return base;
		}

		if (extra == 1) {
			return base + (bb.get() & 0xFFL);
		}

		if (extra == 2) {
			int a1 = bb.get() & 0xFF;
			int a2 = bb.get() & 0xFF;
			return base + ((long) a1 << 8) + a2;
		}

		if (extra >= 3 && extra <= 8) {
			return base + readSignificantBitsHeap(bb, extra);
		}

		throw new IllegalArgumentException("Bytes is higher than 8: " + extra);
	}

	/**
	 * Decodes a value using the <a href="https://sqlite.org/src4/doc/trunk/www/varint.wiki">variable-length encoding of
	 * SQLite</a>.
	 *
	 * @param bb buffer for reading bytes
	 * @return decoded value
	 * @throws IllegalArgumentException if encoded varint is longer than 9 bytes
	 * @see #writeUnsigned(ByteBuffer, long)
	 */
	public static long readUnsigned(ByteBuffer bb, int pos) throws IllegalArgumentException {
		int a0 = bb.get(pos) & 0xFF;
		int extra = VARINT_EXTRA_BYTES[a0];
		long base = VARINT_BASE_VALUES[a0];

		if (extra == 0) {
			return base;
		}

		if (extra == 1) {
			int a1 = bb.get(pos + 1) & 0xFF;
			return base + a1;
		}

		if (extra == 2) {
			int a1 = bb.get(pos + 1) & 0xFF;
			int a2 = bb.get(pos + 2) & 0xFF;
			return base + ((long) a1 << 8) + a2;
		}

		if (extra >= 3 && extra <= 8) {
			return base + readSignificantBits(bb, pos + 1, extra);
		}

		throw new IllegalArgumentException("Bytes is higher than 8: " + extra);
	}

	private static final int[] FIRST_TO_LENGTH = buildFirstToLength();

	private static int[] buildFirstToLength() {
		int[] t = new int[256];
		// 0..240 → 1
		for (int i = 0; i <= 240; i++) {
			t[i] = 1;
		}
		// 241..248 → 2
		for (int i = 241; i <= 248; i++) {
			t[i] = 2;
		}
		// 249 → 3
		t[249] = 3;
		// 250..255 → 4..9
		for (int i = 250; i <= 255; i++) {
			t[i] = i - 246; // 250→4, 255→9
		}
		return t;
	}

	/**
	 * Determines length of an encoded varint value by inspecting the first byte.
	 *
	 * @param a0 first byte of varint value
	 * @return decoded value
	 */
	public static int firstToLength(byte a0) {
		return FIRST_TO_LENGTH[a0 & 0xFF];
	}

	/**
	 * Decodes a single element of a list of variable-length long values from a buffer.
	 *
	 * @param bb    buffer for reading bytes
	 * @param index the element's index
	 * @return the decoded value
	 */
	public static long readListElementUnsigned(ByteBuffer bb, int index) {
		int pos = 0;
		for (int i = 0; i < index; i++) {
			pos += firstToLength(bb.get(pos));
		}
		return readUnsigned(bb, pos);
	}

	/**
	 * Encodes multiple values using variable-length encoding into the given buffer.
	 *
	 * @param bb     buffer for writing bytes
	 * @param values array with values to write
	 */
	public static void writeListUnsigned(final ByteBuffer bb, final long[] values) {
		for (long value : values) {
			writeUnsigned(bb, value);
		}
	}

	/**
	 * Decodes multiple values using variable-length encoding from the given buffer.
	 *
	 * @param bb     buffer for writing bytes
	 * @param values array for the result values
	 */
	public static void readListUnsigned(ByteBuffer bb, long[] values) {
		for (int i = 0; i < values.length; i++) {
			values[i] = readUnsigned(bb);
		}
	}

	public static void readQuadUnsigned(ByteBuffer bb, long[] values) {
		values[0] = readUnsigned(bb);
		values[1] = readUnsigned(bb);
		values[2] = readUnsigned(bb);
		values[3] = readUnsigned(bb);
	}

	/**
	 * Decodes multiple values using variable-length encoding from the given buffer.
	 *
	 * @param bb       buffer for writing bytes
	 * @param indexMap map for indexes of values within values array
	 * @param values   array for the result values
	 */
	public static void readListUnsigned(ByteBuffer bb, int[] indexMap, long[] values) {
		for (int i = 0; i < values.length; i++) {
			values[indexMap[i]] = readUnsigned(bb);
		}
	}

	public static void readQuadUnsigned(ByteBuffer bb, int[] indexMap, long[] values) {
		values[indexMap[0]] = readUnsigned(bb);
		values[indexMap[1]] = readUnsigned(bb);
		values[indexMap[2]] = readUnsigned(bb);
		values[indexMap[3]] = readUnsigned(bb);
	}

	/**
	 * Reads only the significant bytes of the given value in big-endian order.
	 *
	 * @param bb buffer for reading bytes
	 * @param n  number of significant bytes
	 */
	private static long readSignificantBits(ByteBuffer bb, int n) {
		if (bb.isDirect()) {
			return readSignificantBitsDirect(bb, n);
		} else {
			return readSignificantBitsHeap(bb, n);
		}
	}

	private static long readSignificantBitsDirect(ByteBuffer bb, int n) {
		return SignificantBytesBE.readDirect(bb, n);
	}

	private static long readSignificantBitsHeap(ByteBuffer bb, int n) {
		return SignificantBytesBE.read(bb, n);
	}

	/**
	 * Reads only the significant bytes of the given value in big-endian order.
	 *
	 * @param bb    buffer for reading bytes
	 * @param pos   position within the buffer
	 * @param bytes number of significant bytes
	 */
	private static long readSignificantBits(ByteBuffer bb, int pos, int bytes) {
		bytes--;
		long value = (long) (bb.get(pos++) & 0xFF) << (bytes * 8);
		while (bytes-- > 0) {
			value |= (long) (bb.get(pos++) & 0xFF) << (bytes * 8);
		}
		return value;
	}

	private static int compareRegion(ByteBuffer bb1, int startIdx1, ByteBuffer bb2, int startIdx2, int length) {
		int result = 0;
		for (int i = 0; result == 0 && i < length; i++) {
			result = (bb1.get(startIdx1 + i) & 0xff) - (bb2.get(startIdx2 + i) & 0xff);
		}
		return result;
	}

	/**
	 * Use of this class is deprecated, use {@link org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher} instead.
	 */
	@Deprecated(forRemoval = true)
	public static class GroupMatcher {

		final ByteBuffer value;
		final boolean[] shouldMatch;
		final int[] lengths;

		public GroupMatcher(ByteBuffer value, boolean[] shouldMatch) {
			this.value = value;
			this.shouldMatch = shouldMatch;
			this.lengths = new int[shouldMatch.length];
			int pos = 0;
			for (int i = 0; i < lengths.length; i++) {
				int length = firstToLength(value.get(pos));
				lengths[i] = length;
				pos += length;
			}
		}

		public GroupMatcher(ByteBuffer value, boolean a, boolean b, boolean c, boolean d, boolean e) {
			this(value, new boolean[] { a, b, c, d, e });
		}

		public boolean matches(ByteBuffer other) {
			int thisPos = 0;
			int otherPos = 0;
			for (int i = 0; i < shouldMatch.length; i++) {
				int length = lengths[i];
				int otherLength = firstToLength(other.get(otherPos));
				if (shouldMatch[i]) {
					if (length != otherLength || compareRegion(value, thisPos, other, otherPos, length) != 0) {
						return false;
					}
				}
				thisPos += length;
				otherPos += otherLength;
			}
			return true;
		}
	}

}
