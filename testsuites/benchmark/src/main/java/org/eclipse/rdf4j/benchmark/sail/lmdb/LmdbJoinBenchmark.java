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

import org.eclipse.rdf4j.sail.lmdb.Varint;
import org.eclipse.rdf4j.sail.lmdb.util.DirectSlice;
import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
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

	private GroupMatcher branchlessMatcher;
	private Varint.GroupMatcher legacyMatcher;
	private ByteBuffer[] heapCandidates;
	private DirectSlice[] directCandidates;

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

		branchlessMatcher = new GroupMatcher(encodedReference, MATCH_ALL);
		legacyMatcher = new Varint.GroupMatcher(ByteBuffer.wrap(encodedReference), MATCH_ALL);

		heapCandidates = new ByteBuffer[SAMPLE_SIZE];
		directCandidates = new DirectSlice[SAMPLE_SIZE];

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
			directCandidates[i] = DirectSlice.wrap(direct);
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
				.allocate(Varint.calcListLengthUnsigned(values[0], values[1], values[2], values[3]));
		buffer.order(ByteOrder.nativeOrder());
		for (long value : values) {
			Varint.writeUnsigned(buffer, value);
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
}
