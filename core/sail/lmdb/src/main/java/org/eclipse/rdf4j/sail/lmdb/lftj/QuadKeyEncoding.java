/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.lftj;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.eclipse.rdf4j.sail.lmdb.Varint;

/**
 * Utility methods for encoding and decoding quad keys according to a {@link QuadKeyOrder}.
 */
public final class QuadKeyEncoding {
	public static final long MIN_TERM_ID = 0L;

	private QuadKeyEncoding() {
	}

	public static byte[] encode(QuadKey key, QuadKeyOrder order) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(order, "order");
		long[] orderedComponents = orderedComponents(key, order);
		ByteBuffer buffer = ByteBuffer.allocate(
				Varint.calcListLengthUnsigned(orderedComponents[0], orderedComponents[1], orderedComponents[2],
						orderedComponents[3]));
		for (long component : orderedComponents) {
			Varint.writeUnsigned(buffer, component);
		}
		return buffer.array();
	}

	public static QuadKey decode(byte[] bytes, QuadKeyOrder order) {
		Objects.requireNonNull(bytes, "bytes");
		Objects.requireNonNull(order, "order");
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long s = 0;
		long p = 0;
		long o = 0;
		long c = 0;
		for (int i = 0; i < 4; i++) {
			long value = Varint.readUnsigned(buffer);
			Slot slot = order.positionAt(i);
			switch (slot) {
			case S:
				s = value;
				break;
			case P:
				p = value;
				break;
			case O:
				o = value;
				break;
			case C:
				c = value;
				break;
			default:
				throw new IllegalStateException("Unexpected slot: " + slot);
			}
		}
		if (buffer.hasRemaining()) {
			throw new IllegalArgumentException("QuadKey encoding contains trailing bytes");
		}
		return new QuadKey(s, p, o, c);
	}

	private static long[] orderedComponents(QuadKey key, QuadKeyOrder order) {
		long[] components = new long[4];
		for (int i = 0; i < 4; i++) {
			components[i] = componentForRole(key, order.positionAt(i));
		}
		return components;
	}

	public static boolean matchesPrefix(QuadKey key, Prefix prefix) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(prefix, "prefix");
		if (prefix.hasSubject() && key.s() != prefix.subject()) {
			return false;
		}
		if (prefix.hasPredicate() && key.p() != prefix.predicate()) {
			return false;
		}
		if (prefix.hasObject() && key.o() != prefix.object()) {
			return false;
		}
		if (prefix.hasContext() && key.c() != prefix.context()) {
			return false;
		}
		return true;
	}

	public static QuadKey minimalKeyForPrefix(Prefix prefix) {
		Objects.requireNonNull(prefix, "prefix");
		long s = prefix.hasSubject() ? prefix.subject() : MIN_TERM_ID;
		long p = prefix.hasPredicate() ? prefix.predicate() : MIN_TERM_ID;
		long o = prefix.hasObject() ? prefix.object() : MIN_TERM_ID;
		long c = prefix.hasContext() ? prefix.context() : MIN_TERM_ID;
		return new QuadKey(s, p, o, c);
	}

	public static long componentForRole(QuadKey key, Slot role) {
		switch (role) {
		case S:
			return key.s();
		case P:
			return key.p();
		case O:
			return key.o();
		case C:
			return key.c();
		default:
			throw new IllegalStateException("Unexpected slot: " + role);
		}
	}
}
