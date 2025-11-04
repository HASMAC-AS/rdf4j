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

package org.eclipse.rdf4j.sail.lmdb.util;

import static org.eclipse.rdf4j.sail.lmdb.Varint.firstToLength;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A matcher for partial equality tests of varint lists supporting both heap and direct buffers.
 */
public final class GroupMatcher {

	private static final int FIELD_COUNT = 4;

	private final byte[] expected;
	private final Field[] fields;
	private final boolean requiresMatch;

	public GroupMatcher(byte[] valueArray, boolean[] shouldMatch) {
		Objects.requireNonNull(valueArray, "valueArray");
		Objects.requireNonNull(shouldMatch, "shouldMatch");
		if (shouldMatch.length != FIELD_COUNT) {
			throw new IllegalArgumentException("GroupMatcher expects exactly " + FIELD_COUNT + " match flags");
		}

		this.expected = valueArray;
		this.fields = new Field[FIELD_COUNT];

		boolean any = false;
		int offset = 0;
		for (int i = 0; i < FIELD_COUNT; i++) {
			if (offset >= valueArray.length) {
				throw new IllegalArgumentException("valueArray shorter than expected for field " + i);
			}
			byte first = valueArray[offset];
			int length = firstToLength(first);
			if (offset + length > valueArray.length) {
				throw new IllegalArgumentException("valueArray truncated while reading field " + i);
			}
			fields[i] = new Field(offset, length, first, shouldMatch[i]);
			any |= shouldMatch[i];
			offset += length;
		}
		this.requiresMatch = any;
	}

	public boolean matches(ByteBuffer other) {
		Objects.requireNonNull(other, "other");
		if (!requiresMatch) {
			return true;
		}
		ByteBuffer slice = other.slice();
		return matches(slice.remaining(), slice::get);
	}

	public boolean matches(DirectSlice slice) {
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
			int actualLength = firstToLength(first);
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
