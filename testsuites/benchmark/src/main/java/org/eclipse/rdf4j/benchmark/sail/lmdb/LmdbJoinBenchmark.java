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

package org.eclipse.rdf4j.benchmark.sail.lmdb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class LmdbJoinBenchmark {

	private static final int FIELD_COUNT = 4;
	private static final boolean[] MATCH_ALL = { true, true, true, true };
	private static final int SAMPLE_SIZE = 512;

	@Param({ "8", "16", "24" })
	public int keyLength;

	@Param({ "0.05", "0.5", "0.95" })
	public double hitRate;

	@Param({ "1", "8", "32" })
	public int batchSize;

	private BranchlessGroupMatcher branchlessMatcher;
	private LegacyGroupMatcher legacyMatcher;
	private ByteBuffer[] heapCandidates;
	private DirectSliceView[] directCandidates;

	private int legacyCursor;
	private int branchlessCursor;
	private int directCursor;

	@Setup(Level.Trial)
	public void setup() {
		if (keyLength % FIELD_COUNT != 0) {
			throw new IllegalArgumentException("keyLength must be divisible by " + FIELD_COUNT);
		}

		int perFieldLength = keyLength / FIELD_COUNT;
		ValueVariants variants = variantsForLength(perFieldLength);

		long[] referenceValues = { variants.base, variants.base, variants.base, variants.base };
		byte[] encodedReference = encode(referenceValues);

		branchlessMatcher = new BranchlessGroupMatcher(encodedReference, MATCH_ALL);
		legacyMatcher = new LegacyGroupMatcher(ByteBuffer.wrap(encodedReference), MATCH_ALL);

		heapCandidates = new ByteBuffer[SAMPLE_SIZE];
		directCandidates = new DirectSliceView[SAMPLE_SIZE];

		Random random = new Random(42);
		for (int i = 0; i < SAMPLE_SIZE; i++) {
			boolean match = random.nextDouble() < hitRate;
			long[] candidate = buildCandidate(referenceValues, variants, match, random.nextInt(FIELD_COUNT));
			byte[] encoded = encode(candidate);

			heapCandidates[i] = ByteBuffer.wrap(encoded);

			ByteBuffer direct = ByteBuffer.allocateDirect(encoded.length);
			direct.order(ByteOrder.nativeOrder());
			direct.put(encoded);
			direct.flip();
			directCandidates[i] = DirectSliceView.wrap(direct);
		}
	}

	@Setup(Level.Iteration)
	public void resetCursors() {
		legacyCursor = 0;
		branchlessCursor = 0;
		directCursor = 0;
	}

	@Benchmark
	public int baseline() {
		return runLegacy(batchSize);
	}

	@Benchmark
	public int branchless() {
		return runBranchless(batchSize);
	}

	@Benchmark
	public int branchlessBatched() {
		return runDirect(batchSize);
	}

	private int runLegacy(int count) {
		int hits = 0;
		for (int i = 0; i < count; i++) {
			int idx = legacyCursor;
			legacyCursor = (legacyCursor + 1) % SAMPLE_SIZE;

			ByteBuffer buffer = heapCandidates[idx].duplicate();
			buffer.position(0);
			if (legacyMatcher.matches(buffer)) {
				hits++;
			}
		}
		return hits;
	}

	private int runBranchless(int count) {
		int hits = 0;
		for (int i = 0; i < count; i++) {
			int idx = branchlessCursor;
			branchlessCursor = (branchlessCursor + 1) % SAMPLE_SIZE;

			if (branchlessMatcher.matches(heapCandidates[idx])) {
				hits++;
			}
		}
		return hits;
	}

	private int runDirect(int count) {
		int hits = 0;
		for (int i = 0; i < count; i++) {
			int idx = directCursor;
			directCursor = (directCursor + 1) % SAMPLE_SIZE;

			if (branchlessMatcher.matches(directCandidates[idx])) {
				hits++;
			}
		}
		return hits;
	}

	private static long[] buildCandidate(long[] referenceValues, ValueVariants variants, boolean match,
			int mismatchIndex) {
		long[] candidate = referenceValues.clone();
		if (!match) {
			candidate[mismatchIndex] = variants.nonMatchingSameLength;
		}
		return candidate;
	}

	private static byte[] encode(long[] values) {
		Objects.requireNonNull(values, "values");
		ByteBuffer buffer = ByteBuffer
				.allocate(VarintSupport.calcListLengthUnsigned(values[0], values[1], values[2], values[3]));
		buffer.order(ByteOrder.nativeOrder());
		for (long value : values) {
			VarintSupport.writeUnsigned(buffer, value);
		}
		buffer.flip();
		byte[] encoded = new byte[buffer.remaining()];
		buffer.get(encoded);
		return encoded;
	}

	private static ValueVariants variantsForLength(int length) {
		switch (length) {
		case 2:
			return new ValueVariants(241L, 330L);
		case 4:
			return new ValueVariants(1_048_576L, 1_048_577L);
		case 6:
			return new ValueVariants(4_294_967_296L, 4_294_967_297L);
		default:
			throw new IllegalArgumentException("Unsupported varint length: " + length);
		}
	}

	private static final class ValueVariants {
		final long base;
		final long nonMatchingSameLength;

		ValueVariants(long base, long nonMatchingSameLength) {
			this.base = base;
			this.nonMatchingSameLength = nonMatchingSameLength;
		}
	}

	private static final class BranchlessGroupMatcher {

		private static final int FIELD_COUNT = 4;

		private final byte[] expected;
		private final Field[] fields;
		private final boolean requiresMatch;

		BranchlessGroupMatcher(byte[] valueArray, boolean[] shouldMatch) {
			Objects.requireNonNull(valueArray, "valueArray");
			Objects.requireNonNull(shouldMatch, "shouldMatch");
			if (shouldMatch.length != FIELD_COUNT) {
				throw new IllegalArgumentException(
						"GroupMatcher expects exactly " + FIELD_COUNT + " match flags");
			}

			this.expected = valueArray;
			this.fields = new Field[FIELD_COUNT];

			boolean any = false;
			int offset = 0;
			for (int i = 0; i < FIELD_COUNT; i++) {
				if (offset >= valueArray.length) {
					throw new IllegalArgumentException(
							"valueArray shorter than expected for field " + i);
				}
				byte first = valueArray[offset];
				int length = VarintSupport.firstToLength(first);
				if (offset + length > valueArray.length) {
					throw new IllegalArgumentException(
							"valueArray truncated while reading field " + i);
				}
				fields[i] = new Field(offset, length, first, shouldMatch[i]);
				any |= shouldMatch[i];
				offset += length;
			}
			this.requiresMatch = any;
		}

		boolean matches(ByteBuffer other) {
			Objects.requireNonNull(other, "other");
			if (!requiresMatch) {
				return true;
			}
			ByteBuffer slice = other.slice();
			return matches(slice.remaining(), slice::get);
		}

		boolean matches(DirectSliceView slice) {
			Objects.requireNonNull(slice, "slice");
			if (!requiresMatch) {
				return true;
			}
			return matches(slice.length(), slice::get);
		}

		private boolean matches(int totalLength, ByteAccessor accessor) {
			int offset = 0;
			int diff = 0;

			for (Field field : fields) {
				if (offset >= totalLength) {
					return false;
				}

				byte first = accessor.get(offset);
				int actualLength = VarintSupport.firstToLength(first);
				if (actualLength <= 0 || offset + actualLength > totalLength) {
					return false;
				}

				if (field.shouldMatch) {
					diff |= compareField(field, accessor, offset, first, actualLength);
				}

				offset += actualLength;
			}

			return diff == 0;
		}

		private int compareField(Field field, ByteAccessor accessor, int offset, byte firstByte, int actualLength) {
			int diff = (firstByte ^ field.firstByte) & 0xFF;
			diff |= field.length ^ actualLength;

			int limit = Math.min(field.length, actualLength);
			for (int i = 1; i < limit; i++) {
				int expectedByte = expected[field.offset + i] & 0xFF;
				int candidateByte = accessor.get(offset + i) & 0xFF;
				diff |= expectedByte ^ candidateByte;
			}

			return diff;
		}

		private interface ByteAccessor {
			byte get(int index);
		}

		private static final class Field {
			final int offset;
			final int length;
			final byte firstByte;
			final boolean shouldMatch;

			Field(int offset, int length, byte firstByte, boolean shouldMatch) {
				this.offset = offset;
				this.length = length;
				this.firstByte = firstByte;
				this.shouldMatch = shouldMatch;
			}
		}
	}

	private static final class DirectSliceView {
		private final ByteBuffer buffer;

		private DirectSliceView(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		static DirectSliceView wrap(ByteBuffer buffer) {
			Objects.requireNonNull(buffer, "buffer");
			if (!buffer.isDirect()) {
				throw new IllegalArgumentException("DirectSlice requires a direct ByteBuffer");
			}
			return new DirectSliceView(buffer.slice());
		}

		int length() {
			return buffer.remaining();
		}

		byte get(int index) {
			return buffer.get(index);
		}

		ByteBuffer asByteBuffer() {
			return buffer.duplicate();
		}
	}

	private static final class LegacyGroupMatcher {
		final ByteBuffer value;
		final boolean[] shouldMatch;
		final int[] lengths;

		LegacyGroupMatcher(ByteBuffer value, boolean[] shouldMatch) {
			this.value = value;
			this.shouldMatch = shouldMatch;
			this.lengths = new int[shouldMatch.length];
			int pos = 0;
			for (int i = 0; i < lengths.length; i++) {
				int length = VarintSupport.firstToLength(value.get(pos));
				lengths[i] = length;
				pos += length;
			}
		}

		boolean matches(ByteBuffer other) {
			int thisPos = 0;
			int otherPos = 0;
			for (int i = 0; i < shouldMatch.length; i++) {
				int length = lengths[i];
				int otherLength = VarintSupport.firstToLength(other.get(otherPos));
				if (shouldMatch[i]) {
					if (length != otherLength
							|| compareRegion(value, thisPos, other, otherPos, length) != 0) {
						return false;
					}
				}
				thisPos += length;
				otherPos += otherLength;
			}
			return true;
		}

		private static int compareRegion(ByteBuffer bb1, int startIdx1, ByteBuffer bb2, int startIdx2, int length) {
			int result = 0;
			for (int i = 0; result == 0 && i < length; i++) {
				result = (bb1.get(startIdx1 + i) & 0xff) - (bb2.get(startIdx2 + i) & 0xff);
			}
			return result;
		}
	}

	private static final class VarintSupport {
		private static final int[] FIRST_TO_LENGTH = buildFirstToLength();

		private VarintSupport() {
		}

		static int calcListLengthUnsigned(long a, long b, long c, long d) {
			return calcLengthUnsigned(a) + calcLengthUnsigned(b) + calcLengthUnsigned(c) + calcLengthUnsigned(d);
		}

		static void writeUnsigned(final ByteBuffer bb, final long value) {
			if (value <= 240) {
				bb.put((byte) value);
			} else if (value <= 2287) {
				long v = value - 240;
				final ByteOrder prev = bb.order();
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(ByteOrder.BIG_ENDIAN);
				}
				try {
					int hi = (int) (v >>> 8) + 241;
					int lo = (int) (v & 0xFF);
					bb.putShort((short) ((hi << 8) | lo));
				} finally {
					if (prev != ByteOrder.BIG_ENDIAN) {
						bb.order(prev);
					}
				}
			} else if (value <= 67823) {
				long v = value - 2288;
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
				int bytes = descriptor(value) + 1;
				bb.put((byte) (250 + (bytes - 3)));
				writeSignificantBits(bb, value, bytes);
			}
		}

		static int firstToLength(byte a0) {
			return FIRST_TO_LENGTH[a0 & 0xFF];
		}

		private static int calcLengthUnsigned(long value) {
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

		private static byte descriptor(long value) {
			return value == 0 ? 0 : (byte) (7 - Long.numberOfLeadingZeros(value) / 8);
		}

		private static void writeSignificantBits(ByteBuffer bb, long value, int bytes) {
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				int i = bytes;
				if ((i & 1) != 0) {
					bb.put((byte) (value >>> ((i - 1) * 8)));
					i--;
				}
				if (i == 8) {
					bb.putLong(value);
					return;
				}
				if (i >= 4) {
					int shift = (i - 4) * 8;
					bb.putInt((int) (value >>> shift));
					i -= 4;
				}
				while (i >= 2) {
					int shift = (i - 2) * 8;
					bb.putShort((short) (value >>> shift));
					i -= 2;
				}
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
		}

		private static int[] buildFirstToLength() {
			int[] t = new int[256];
			for (int i = 0; i <= 240; i++) {
				t[i] = 1;
			}
			for (int i = 241; i <= 248; i++) {
				t[i] = 2;
			}
			t[249] = 3;
			for (int i = 250; i <= 255; i++) {
				t[i] = i - 246;
			}
			return t;
		}
	}
}
