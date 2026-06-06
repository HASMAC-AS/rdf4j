/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable bit mask over query-local variable-name ids.
 * <p>
 * The first 64 ids are stored directly in a {@code long}. Larger queries transparently spill into additional long words,
 * so callers get the fast path for common queries without imposing a hard 64-variable limit.
 */
final class VarNameMask {

	private static final long[] NO_HIGH_BITS = new long[0];
	private static final VarNameMask EMPTY = new VarNameMask(0L, NO_HIGH_BITS, 0);

	private final long lowBits;
	private final long[] highBits;
	private final int cardinality;

	private VarNameMask(long lowBits, long[] highBits, int cardinality) {
		this.lowBits = lowBits;
		this.highBits = highBits;
		this.cardinality = cardinality;
	}

	static VarNameMask empty() {
		return EMPTY;
	}

	static Builder builder() {
		return new Builder();
	}

	boolean isEmpty() {
		return cardinality == 0;
	}

	int cardinality() {
		return cardinality;
	}

	boolean contains(int varId) {
		if (varId < 0) {
			return false;
		}
		if (varId < Long.SIZE) {
			return (lowBits & bit(varId)) != 0L;
		}

		int highIndex = highIndex(varId);
		return highIndex < highBits.length && (highBits[highIndex] & highBit(varId)) != 0L;
	}

	boolean containsAll(VarNameMask other) {
		Objects.requireNonNull(other, "other must not be null");

		if ((other.lowBits & ~lowBits) != 0L) {
			return false;
		}

		for (int i = 0; i < other.highBits.length; i++) {
			long thisWord = i < highBits.length ? highBits[i] : 0L;
			if ((other.highBits[i] & ~thisWord) != 0L) {
				return false;
			}
		}

		return true;
	}

	boolean intersects(VarNameMask other) {
		Objects.requireNonNull(other, "other must not be null");

		if ((lowBits & other.lowBits) != 0L) {
			return true;
		}

		int highLength = Math.min(highBits.length, other.highBits.length);
		for (int i = 0; i < highLength; i++) {
			if ((highBits[i] & other.highBits[i]) != 0L) {
				return true;
			}
		}

		return false;
	}

	VarNameMask union(VarNameMask other) {
		Objects.requireNonNull(other, "other must not be null");

		if (other.isEmpty()) {
			return this;
		}
		if (isEmpty()) {
			return other;
		}

		long lowBits = this.lowBits | other.lowBits;
		int highLength = Math.max(this.highBits.length, other.highBits.length);
		long[] highBits = highLength == 0 ? NO_HIGH_BITS : new long[highLength];
		for (int i = 0; i < highLength; i++) {
			long left = i < this.highBits.length ? this.highBits[i] : 0L;
			long right = i < other.highBits.length ? other.highBits[i] : 0L;
			highBits[i] = left | right;
		}

		return fromBits(lowBits, highBits);
	}

	int singleId() {
		if (cardinality != 1) {
			throw new IllegalStateException("Mask contains " + cardinality + " variables");
		}
		if (lowBits != 0L) {
			return Long.numberOfTrailingZeros(lowBits);
		}

		for (int i = 0; i < highBits.length; i++) {
			if (highBits[i] != 0L) {
				return Long.SIZE + (i * Long.SIZE) + Long.numberOfTrailingZeros(highBits[i]);
			}
		}

		throw new IllegalStateException("Mask contains one variable, but no bit was set");
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof VarNameMask)) {
			return false;
		}
		VarNameMask that = (VarNameMask) other;
		return lowBits == that.lowBits && Arrays.equals(highBits, that.highBits);
	}

	@Override
	public int hashCode() {
		int result = Long.hashCode(lowBits);
		result = 31 * result + Arrays.hashCode(highBits);
		return result;
	}

	@Override
	public String toString() {
		return "VarNameMask{" + "lowBits=" + Long.toBinaryString(lowBits) + ", highBits="
				+ Arrays.toString(highBits) + '}';
	}

	private static VarNameMask fromBits(long lowBits, long[] highBits) {
		long[] trimmedHighBits = trim(highBits);
		int cardinality = Long.bitCount(lowBits);
		for (long word : trimmedHighBits) {
			cardinality += Long.bitCount(word);
		}

		if (cardinality == 0) {
			return EMPTY;
		}
		return new VarNameMask(lowBits, trimmedHighBits, cardinality);
	}

	private static long[] trim(long[] highBits) {
		if (highBits == null || highBits.length == 0) {
			return NO_HIGH_BITS;
		}

		int length = highBits.length;
		while (length > 0 && highBits[length - 1] == 0L) {
			length--;
		}
		if (length == 0) {
			return NO_HIGH_BITS;
		}
		return Arrays.copyOf(highBits, length);
	}

	private static long bit(int varId) {
		return 1L << varId;
	}

	private static long highBit(int varId) {
		return 1L << ((varId - Long.SIZE) & (Long.SIZE - 1));
	}

	private static int highIndex(int varId) {
		return (varId - Long.SIZE) >> 6;
	}

	static final class Builder {

		private long lowBits;
		private long[] highBits = NO_HIGH_BITS;

		void add(int varId) {
			if (varId < 0) {
				throw new IllegalArgumentException("Variable id must be non-negative: " + varId);
			}
			if (varId < Long.SIZE) {
				lowBits |= bit(varId);
			} else {
				int highIndex = highIndex(varId);
				ensureHighCapacity(highIndex);
				highBits[highIndex] |= highBit(varId);
			}
		}

		VarNameMask build() {
			return fromBits(lowBits, highBits);
		}

		private void ensureHighCapacity(int highIndex) {
			if (highBits.length <= highIndex) {
				highBits = Arrays.copyOf(highBits, highIndex + 1);
			}
		}
	}
}
