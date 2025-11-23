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
		switch (order.fieldSequence()) {
		case "spoc":
			return encodeSpoc(key);
		case "spco":
			return encodeSpco(key);
		case "sopc":
			return encodeSopc(key);
		case "socp":
			return encodeSocp(key);
		case "scpo":
			return encodeScpo(key);
		case "scop":
			return encodeScop(key);
		case "psoc":
			return encodePsoc(key);
		case "psco":
			return encodePsco(key);
		case "posc":
			return encodePosc(key);
		case "pocs":
			return encodePocs(key);
		case "pcso":
			return encodePcso(key);
		case "pcos":
			return encodePcos(key);
		case "ospc":
			return encodeOspc(key);
		case "oscp":
			return encodeOscp(key);
		case "opsc":
			return encodeOpsc(key);
		case "opcs":
			return encodeOpcs(key);
		case "ocsp":
			return encodeOcsp(key);
		case "ocps":
			return encodeOcps(key);
		case "cspo":
			return encodeCspo(key);
		case "csop":
			return encodeCsop(key);
		case "cpso":
			return encodeCpso(key);
		case "cpos":
			return encodeCpos(key);
		case "cosp":
			return encodeCosp(key);
		case "cops":
			return encodeCops(key);
		default:
			throw new IllegalArgumentException("Unsupported quad key order: " + order.fieldSequence());
		}
	}

	public static QuadKey decode(byte[] bytes, QuadKeyOrder order) {
		Objects.requireNonNull(bytes, "bytes");
		Objects.requireNonNull(order, "order");
		switch (order.fieldSequence()) {
		case "spoc":
			return decodeSpoc(bytes);
		case "spco":
			return decodeSpco(bytes);
		case "sopc":
			return decodeSopc(bytes);
		case "socp":
			return decodeSocp(bytes);
		case "scpo":
			return decodeScpo(bytes);
		case "scop":
			return decodeScop(bytes);
		case "psoc":
			return decodePsoc(bytes);
		case "psco":
			return decodePsco(bytes);
		case "posc":
			return decodePosc(bytes);
		case "pocs":
			return decodePocs(bytes);
		case "pcso":
			return decodePcso(bytes);
		case "pcos":
			return decodePcos(bytes);
		case "ospc":
			return decodeOspc(bytes);
		case "oscp":
			return decodeOscp(bytes);
		case "opsc":
			return decodeOpsc(bytes);
		case "opcs":
			return decodeOpcs(bytes);
		case "ocsp":
			return decodeOcsp(bytes);
		case "ocps":
			return decodeOcps(bytes);
		case "cspo":
			return decodeCspo(bytes);
		case "csop":
			return decodeCsop(bytes);
		case "cpso":
			return decodeCpso(bytes);
		case "cpos":
			return decodeCpos(bytes);
		case "cosp":
			return decodeCosp(bytes);
		case "cops":
			return decodeCops(bytes);
		default:
			throw new IllegalArgumentException("Unsupported quad key order: " + order.fieldSequence());
		}
	}

	public static byte[] encodeSpoc(QuadKey key) {
		return encodeSequence(key.s(), key.p(), key.o(), key.c());
	}

	public static byte[] encodeSpco(QuadKey key) {
		return encodeSequence(key.s(), key.p(), key.c(), key.o());
	}

	public static byte[] encodeSopc(QuadKey key) {
		return encodeSequence(key.s(), key.o(), key.p(), key.c());
	}

	public static byte[] encodeSocp(QuadKey key) {
		return encodeSequence(key.s(), key.o(), key.c(), key.p());
	}

	public static byte[] encodeScpo(QuadKey key) {
		return encodeSequence(key.s(), key.c(), key.p(), key.o());
	}

	public static byte[] encodeScop(QuadKey key) {
		return encodeSequence(key.s(), key.c(), key.o(), key.p());
	}

	public static byte[] encodePsoc(QuadKey key) {
		return encodeSequence(key.p(), key.s(), key.o(), key.c());
	}

	public static byte[] encodePsco(QuadKey key) {
		return encodeSequence(key.p(), key.s(), key.c(), key.o());
	}

	public static byte[] encodePosc(QuadKey key) {
		return encodeSequence(key.p(), key.o(), key.s(), key.c());
	}

	public static byte[] encodePocs(QuadKey key) {
		return encodeSequence(key.p(), key.o(), key.c(), key.s());
	}

	public static byte[] encodePcso(QuadKey key) {
		return encodeSequence(key.p(), key.c(), key.s(), key.o());
	}

	public static byte[] encodePcos(QuadKey key) {
		return encodeSequence(key.p(), key.c(), key.o(), key.s());
	}

	public static byte[] encodeOspc(QuadKey key) {
		return encodeSequence(key.o(), key.s(), key.p(), key.c());
	}

	public static byte[] encodeOscp(QuadKey key) {
		return encodeSequence(key.o(), key.s(), key.c(), key.p());
	}

	public static byte[] encodeOpsc(QuadKey key) {
		return encodeSequence(key.o(), key.p(), key.s(), key.c());
	}

	public static byte[] encodeOpcs(QuadKey key) {
		return encodeSequence(key.o(), key.p(), key.c(), key.s());
	}

	public static byte[] encodeOcsp(QuadKey key) {
		return encodeSequence(key.o(), key.c(), key.s(), key.p());
	}

	public static byte[] encodeOcps(QuadKey key) {
		return encodeSequence(key.o(), key.c(), key.p(), key.s());
	}

	public static byte[] encodeCspo(QuadKey key) {
		return encodeSequence(key.c(), key.s(), key.p(), key.o());
	}

	public static byte[] encodeCsop(QuadKey key) {
		return encodeSequence(key.c(), key.s(), key.o(), key.p());
	}

	public static byte[] encodeCpso(QuadKey key) {
		return encodeSequence(key.c(), key.p(), key.s(), key.o());
	}

	public static byte[] encodeCpos(QuadKey key) {
		return encodeSequence(key.c(), key.p(), key.o(), key.s());
	}

	public static byte[] encodeCosp(QuadKey key) {
		return encodeSequence(key.c(), key.o(), key.s(), key.p());
	}

	public static byte[] encodeCops(QuadKey key) {
		return encodeSequence(key.c(), key.o(), key.p(), key.s());
	}

	public static QuadKey decodeSpoc(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeSpco(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeSopc(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeSocp(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeScpo(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeScop(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePsoc(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePsco(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePosc(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePocs(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePcso(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePcos(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOspc(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOscp(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOpsc(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOpcs(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOcsp(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOcps(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCspo(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCsop(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCpso(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCpos(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCosp(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCops(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	private static byte[] encodeSequence(long first, long second, long third, long fourth) {
		ByteBuffer buffer = ByteBuffer
				.allocate(Varint.calcListLengthUnsigned(first, second, third, fourth));
		Varint.writeUnsigned(buffer, first);
		Varint.writeUnsigned(buffer, second);
		Varint.writeUnsigned(buffer, third);
		Varint.writeUnsigned(buffer, fourth);
		return buffer.array();
	}

	private static void ensureFullyConsumed(ByteBuffer buffer) {
		if (buffer.hasRemaining()) {
			throw new IllegalArgumentException("QuadKey encoding contains trailing bytes");
		}
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
