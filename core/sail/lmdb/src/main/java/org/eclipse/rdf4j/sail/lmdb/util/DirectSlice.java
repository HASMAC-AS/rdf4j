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

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Lightweight view over a direct {@link ByteBuffer} without copying.
 */
public final class DirectSlice {

	private final ByteBuffer buffer;

	private DirectSlice(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	/**
	 * Creates a new {@link DirectSlice} over the remaining bytes in {@code buffer}.
	 *
	 * @param buffer the direct buffer to expose
	 * @return a zero-copy slice over {@code buffer}
	 */
	public static DirectSlice wrap(ByteBuffer buffer) {
		Objects.requireNonNull(buffer, "buffer");
		if (!buffer.isDirect()) {
			throw new IllegalArgumentException("DirectSlice requires a direct ByteBuffer");
		}

		return new DirectSlice(buffer.slice());
	}

	/**
	 * Returns the number of bytes in this slice.
	 */
	public int length() {
		return buffer.remaining();
	}

	/**
	 * Reads a byte at the given absolute position without modifying buffer state.
	 */
	public byte get(int index) {
		return buffer.get(index);
	}

	/**
	 * Returns a duplicate {@link ByteBuffer} view over the same memory region.
	 */
	public ByteBuffer asByteBuffer() {
		return buffer.duplicate();
	}
}
