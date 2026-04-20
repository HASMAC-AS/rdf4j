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

		if (value <= 240) {
			bb.put((byte) value);
		} else if (value <= 2287) {
			// header: 241..248, then 1 payload byte
			// Using bit ops instead of div/mod and putShort to batch the two bytes.
			long v = value - 240; // 1..2047
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				int hi = (int) (v >>> 8) + 241; // 241..248
				int lo = (int) (v & 0xFF); // 0..255
				bb.putShort((short) ((hi << 8) | lo));
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
		} else if (value <= 67823) {
			// header 249, then 2 payload bytes (value - 2288), big-endian
			long v = value - 2288; // 0..65535
			bb.put((byte) 249);
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				bb.putShort((short) v);
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
		} else {
			int bytes = descriptor(value) + 1; // 3..8
			bb.put((byte) (250 + (bytes - 3))); // header 250..255
			writeSignificantBits(bb, value, bytes); // payload (batched)
		}
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
		if (value <= 240) {
			return 1;
		} else if (value <= 2287) {
			return 2;
		} else if (value <= 67823) {
			return 3;
		} else {
			int bytes = descriptor(value) + 1;
			return 1 + bytes;
		}
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
		return decodeUnsigned(bb, bb.position(), true);
	}

	public static void skipUnsigned(ByteBuffer bb) throws IllegalArgumentException {
		final int a0 = bb.get() & 0xFF; // lead byte, unsigned

		if (a0 <= 240) {
			return;
		}

		final int len = FIRST_TO_LENGTH[a0 & 0xFF];
		if (len == 0) {
			throw new IllegalArgumentException("Invalid varint header: " + a0);
		}

		bb.position(bb.position() + (len - 1));

	}

	public static long readUnsignedHeap(ByteBuffer bb) throws IllegalArgumentException {
		return decodeUnsigned(bb, bb.position(), true);
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
		return decodeUnsigned(bb, pos, false);
	}

	private static final long[] HEADER_BASE = buildHeaderBase();

	private static long[] buildHeaderBase() {
		long[] base = new long[256];

		for (int header = 0; header <= 240; header++) {
			base[header] = header;
		}

		for (int header = 241; header <= 248; header++) {
			base[header] = 240L + ((long) (header - 241) << 8);
		}

		base[249] = 2288L;

		return base;
	}

	private static long decodeUnsigned(ByteBuffer bb, int pos, boolean advance) {
		final int a0 = bb.get(pos) & 0xFF;
		final int len = FIRST_TO_LENGTH[a0];

		if (len == 0) {
			throw new IllegalArgumentException("Invalid varint header: " + a0);
		}

		final int payloadLen = len - 1;
		final long base = HEADER_BASE[a0];
		final long payload;

		if (payloadLen == 0) {
			payload = 0L;
		} else if (bb.hasArray()) {
			payload = readPayload(bb.array(), bb.arrayOffset() + pos + 1, payloadLen);
		} else {
			payload = readPayload(bb, pos + 1, payloadLen);
		}

		if (advance) {
			bb.position(pos + len);
		}

		return base + payload;
	}

	private static long readPayload(ByteBuffer bb, int pos, int len) {
		switch (len) {
		case 0:
			return 0L;
		case 1:
			return bb.get(pos) & 0xFFL;
		case 2:
			return ((bb.get(pos) & 0xFFL) << 8) | (bb.get(pos + 1) & 0xFFL);
		default:
			ByteBuffer dup = bb.duplicate().order(ByteOrder.BIG_ENDIAN);
			dup.position(pos);
			return readSignificantBitsDirect(dup, len);
		}
	}

	static long readUnsigned(byte[] array, int offset, int len) {
		if (len <= 0 || len > 9) {
			throw new IllegalArgumentException("Invalid varint length: " + len);
		}

		final int header = array[offset] & 0xFF;
		final long base = HEADER_BASE[header];
		final long payload = readPayload(array, offset + 1, len - 1);

		return base + payload;
	}

	public static long readUnsigned(byte[] array, int offset) {
		int len = FIRST_TO_LENGTH[array[offset] & 0xFF];
		if (len == 0) {
			throw new IllegalArgumentException("Invalid varint header: " + (array[offset] & 0xFF));
		}
		return readUnsigned(array, offset, len);
	}

	private static long readPayload(byte[] array, int offset, int len) {
		switch (len) {
		case 0:
			return 0L;
		case 1:
			return array[offset] & 0xFFL;
		case 2:
			return ((array[offset] & 0xFFL) << 8) | (array[offset + 1] & 0xFFL);
		case 3:
			return ((array[offset] & 0xFFL) << 16)
					| ((array[offset + 1] & 0xFFL) << 8)
					| (array[offset + 2] & 0xFFL);
		case 4:
			return ((array[offset] & 0xFFL) << 24)
					| ((array[offset + 1] & 0xFFL) << 16)
					| ((array[offset + 2] & 0xFFL) << 8)
					| (array[offset + 3] & 0xFFL);
		case 5:
			return ((array[offset] & 0xFFL) << 32)
					| ((array[offset + 1] & 0xFFL) << 24)
					| ((array[offset + 2] & 0xFFL) << 16)
					| ((array[offset + 3] & 0xFFL) << 8)
					| (array[offset + 4] & 0xFFL);
		case 6:
			return ((array[offset] & 0xFFL) << 40)
					| ((array[offset + 1] & 0xFFL) << 32)
					| ((array[offset + 2] & 0xFFL) << 24)
					| ((array[offset + 3] & 0xFFL) << 16)
					| ((array[offset + 4] & 0xFFL) << 8)
					| (array[offset + 5] & 0xFFL);
		case 7:
			return ((array[offset] & 0xFFL) << 48)
					| ((array[offset + 1] & 0xFFL) << 40)
					| ((array[offset + 2] & 0xFFL) << 32)
					| ((array[offset + 3] & 0xFFL) << 24)
					| ((array[offset + 4] & 0xFFL) << 16)
					| ((array[offset + 5] & 0xFFL) << 8)
					| (array[offset + 6] & 0xFFL);
		case 8:
			return ((array[offset] & 0xFFL) << 56)
					| ((array[offset + 1] & 0xFFL) << 48)
					| ((array[offset + 2] & 0xFFL) << 40)
					| ((array[offset + 3] & 0xFFL) << 32)
					| ((array[offset + 4] & 0xFFL) << 24)
					| ((array[offset + 5] & 0xFFL) << 16)
					| ((array[offset + 6] & 0xFFL) << 8)
					| (array[offset + 7] & 0xFFL);
		default:
			throw new IllegalArgumentException("Unsupported payload length: " + len);
		}
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
		// TODO: Optimise for quads and also call writeUnsigned
		for (int i = 0; i < values.length; i++) {
			final long value = values[i];
			if (value <= 240) {
				bb.put((byte) value);
			} else if (value <= 2287) {
				bb.put((byte) ((value - 240) / 256 + 241));
				bb.put((byte) ((value - 240) % 256));
			} else if (value <= 67823) {
				bb.put((byte) 249);
				bb.put((byte) ((value - 2288) / 256));
				bb.put((byte) ((value - 2288) % 256));
			} else {
				int bytes = descriptor(value) + 1;
				bb.put((byte) (250 + (bytes - 3)));
				writeSignificantBits(bb, value, bytes);
			}
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

	private static long readSignificantBitsDirect(ByteBuffer bb, int n) {
		return SignificantBytesBE.readDirect(bb, n);
	}

	private static long readSignificantBitsHeap(ByteBuffer bb, int n) {
		return SignificantBytesBE.read(bb, n);
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
