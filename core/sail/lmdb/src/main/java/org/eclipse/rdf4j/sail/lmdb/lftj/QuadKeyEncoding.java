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

	public interface QuadKeySink {
		void set(long s, long p, long o, long c);
	}

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
		return decode(bytes, 0, bytes.length, order);
	}

	public static QuadKey decode(byte[] bytes, int offset, int length, QuadKeyOrder order) {
		Objects.requireNonNull(bytes, "bytes");
		Objects.requireNonNull(order, "order");
		if (offset < 0 || length < 0 || offset + length > bytes.length) {
			throw new IllegalArgumentException("Invalid offset/length for key decode");
		}
		ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
		return decode(buffer, order);
	}

	public static void decodeInto(byte[] bytes, int offset, int length, QuadKeyOrder order, QuadKeySink sink) {
		Objects.requireNonNull(bytes, "bytes");
		Objects.requireNonNull(order, "order");
		Objects.requireNonNull(sink, "sink");
		if (offset < 0 || length < 0 || offset + length > bytes.length) {
			throw new IllegalArgumentException("Invalid offset/length for key decode");
		}
		ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
		decodeInto(buffer, order, sink);
	}

	public static QuadKey decode(ByteBuffer buffer, QuadKeyOrder order) {
		Objects.requireNonNull(buffer, "buffer");
		Objects.requireNonNull(order, "order");
		ByteBuffer slice = buffer.slice();
		switch (order.fieldSequence()) {
		case "spoc":
			return decodeSpoc(slice);
		case "spco":
			return decodeSpco(slice);
		case "sopc":
			return decodeSopc(slice);
		case "socp":
			return decodeSocp(slice);
		case "scpo":
			return decodeScpo(slice);
		case "scop":
			return decodeScop(slice);
		case "psoc":
			return decodePsoc(slice);
		case "psco":
			return decodePsco(slice);
		case "posc":
			return decodePosc(slice);
		case "pocs":
			return decodePocs(slice);
		case "pcso":
			return decodePcso(slice);
		case "pcos":
			return decodePcos(slice);
		case "ospc":
			return decodeOspc(slice);
		case "oscp":
			return decodeOscp(slice);
		case "opsc":
			return decodeOpsc(slice);
		case "opcs":
			return decodeOpcs(slice);
		case "ocsp":
			return decodeOcsp(slice);
		case "ocps":
			return decodeOcps(slice);
		case "cspo":
			return decodeCspo(slice);
		case "csop":
			return decodeCsop(slice);
		case "cpso":
			return decodeCpso(slice);
		case "cpos":
			return decodeCpos(slice);
		case "cosp":
			return decodeCosp(slice);
		case "cops":
			return decodeCops(slice);
		default:
			throw new IllegalArgumentException("Unsupported quad key order: " + order.fieldSequence());
		}
	}

	public static void decodeInto(ByteBuffer buffer, QuadKeyOrder order, QuadKeySink sink) {
		int startPos = buffer.position();
		String sequence = order.fieldSequence();

		long v1 = Varint.readUnsigned(buffer);
		long v2 = Varint.readUnsigned(buffer);
		long v3 = Varint.readUnsigned(buffer);
		long v4 = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);

		long s = 0;
		long p = 0;
		long o = 0;
		long c = 0;

		for (int i = 0; i < 4; i++) {
			long value;
			switch (i) {
			case 0:
				value = v1;
				break;
			case 1:
				value = v2;
				break;
			case 2:
				value = v3;
				break;
			default:
				value = v4;
				break;
			}

			switch (sequence.charAt(i)) {
			case 's':
				s = value;
				break;
			case 'p':
				p = value;
				break;
			case 'o':
				o = value;
				break;
			case 'c':
				c = value;
				break;
			default:
				throw new IllegalArgumentException("Unsupported quad key order: " + sequence);
			}
		}

		sink.set(s, p, o, c);
		buffer.position(startPos);
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

	public static int encodeInto(QuadKey key, QuadKeyOrder order, ByteBuffer buffer) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(order, "order");
		Objects.requireNonNull(buffer, "buffer");
		switch (order.fieldSequence()) {
		case "spoc":
			return encodeSequenceInto(buffer, key.s(), key.p(), key.o(), key.c());
		case "spco":
			return encodeSequenceInto(buffer, key.s(), key.p(), key.c(), key.o());
		case "sopc":
			return encodeSequenceInto(buffer, key.s(), key.o(), key.p(), key.c());
		case "socp":
			return encodeSequenceInto(buffer, key.s(), key.o(), key.c(), key.p());
		case "scpo":
			return encodeSequenceInto(buffer, key.s(), key.c(), key.p(), key.o());
		case "scop":
			return encodeSequenceInto(buffer, key.s(), key.c(), key.o(), key.p());
		case "psoc":
			return encodeSequenceInto(buffer, key.p(), key.s(), key.o(), key.c());
		case "psco":
			return encodeSequenceInto(buffer, key.p(), key.s(), key.c(), key.o());
		case "posc":
			return encodeSequenceInto(buffer, key.p(), key.o(), key.s(), key.c());
		case "pocs":
			return encodeSequenceInto(buffer, key.p(), key.o(), key.c(), key.s());
		case "pcso":
			return encodeSequenceInto(buffer, key.p(), key.c(), key.s(), key.o());
		case "pcos":
			return encodeSequenceInto(buffer, key.p(), key.c(), key.o(), key.s());
		case "ospc":
			return encodeSequenceInto(buffer, key.o(), key.s(), key.p(), key.c());
		case "oscp":
			return encodeSequenceInto(buffer, key.o(), key.s(), key.c(), key.p());
		case "opsc":
			return encodeSequenceInto(buffer, key.o(), key.p(), key.s(), key.c());
		case "opcs":
			return encodeSequenceInto(buffer, key.o(), key.p(), key.c(), key.s());
		case "ocsp":
			return encodeSequenceInto(buffer, key.o(), key.c(), key.s(), key.p());
		case "ocps":
			return encodeSequenceInto(buffer, key.o(), key.c(), key.p(), key.s());
		case "cspo":
			return encodeSequenceInto(buffer, key.c(), key.s(), key.p(), key.o());
		case "csop":
			return encodeSequenceInto(buffer, key.c(), key.s(), key.o(), key.p());
		case "cpso":
			return encodeSequenceInto(buffer, key.c(), key.p(), key.s(), key.o());
		case "cpos":
			return encodeSequenceInto(buffer, key.c(), key.p(), key.o(), key.s());
		case "cosp":
			return encodeSequenceInto(buffer, key.c(), key.o(), key.s(), key.p());
		case "cops":
			return encodeSequenceInto(buffer, key.c(), key.o(), key.p(), key.s());
		default:
			throw new IllegalArgumentException("Unsupported quad key order: " + order.fieldSequence());
		}
	}

	public static int encodeFieldsInto(long s, long p, long o, long c, QuadKeyOrder order, ByteBuffer buffer) {
		Objects.requireNonNull(order, "order");
		Objects.requireNonNull(buffer, "buffer");
		switch (order.fieldSequence()) {
		case "spoc":
			return encodeSequenceInto(buffer, s, p, o, c);
		case "spco":
			return encodeSequenceInto(buffer, s, p, c, o);
		case "sopc":
			return encodeSequenceInto(buffer, s, o, p, c);
		case "socp":
			return encodeSequenceInto(buffer, s, o, c, p);
		case "scpo":
			return encodeSequenceInto(buffer, s, c, p, o);
		case "scop":
			return encodeSequenceInto(buffer, s, c, o, p);
		case "psoc":
			return encodeSequenceInto(buffer, p, s, o, c);
		case "psco":
			return encodeSequenceInto(buffer, p, s, c, o);
		case "posc":
			return encodeSequenceInto(buffer, p, o, s, c);
		case "pocs":
			return encodeSequenceInto(buffer, p, o, c, s);
		case "pcso":
			return encodeSequenceInto(buffer, p, c, s, o);
		case "pcos":
			return encodeSequenceInto(buffer, p, c, o, s);
		case "ospc":
			return encodeSequenceInto(buffer, o, s, p, c);
		case "oscp":
			return encodeSequenceInto(buffer, o, s, c, p);
		case "opsc":
			return encodeSequenceInto(buffer, o, p, s, c);
		case "opcs":
			return encodeSequenceInto(buffer, o, p, c, s);
		case "ocsp":
			return encodeSequenceInto(buffer, o, c, s, p);
		case "ocps":
			return encodeSequenceInto(buffer, o, c, p, s);
		case "cspo":
			return encodeSequenceInto(buffer, c, s, p, o);
		case "csop":
			return encodeSequenceInto(buffer, c, s, o, p);
		case "cpso":
			return encodeSequenceInto(buffer, c, p, s, o);
		case "cpos":
			return encodeSequenceInto(buffer, c, p, o, s);
		case "cosp":
			return encodeSequenceInto(buffer, c, o, s, p);
		case "cops":
			return encodeSequenceInto(buffer, c, o, p, s);
		default:
			throw new IllegalArgumentException("Unsupported quad key order: " + order.fieldSequence());
		}
	}

	public static QuadKey decodeSpoc(byte[] bytes) {
		return decodeSpoc(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeSpoc(ByteBuffer buffer) {
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeSpco(byte[] bytes) {
		return decodeSpco(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeSpco(ByteBuffer buffer) {
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeSopc(byte[] bytes) {
		return decodeSopc(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeSopc(ByteBuffer buffer) {
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeSocp(byte[] bytes) {
		return decodeSocp(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeSocp(ByteBuffer buffer) {
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeScpo(byte[] bytes) {
		return decodeScpo(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeScpo(ByteBuffer buffer) {
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeScop(byte[] bytes) {
		return decodeScop(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeScop(ByteBuffer buffer) {
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePsoc(byte[] bytes) {
		return decodePsoc(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodePsoc(ByteBuffer buffer) {
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePsco(byte[] bytes) {
		return decodePsco(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodePsco(ByteBuffer buffer) {
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePosc(byte[] bytes) {
		return decodePosc(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodePosc(ByteBuffer buffer) {
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePocs(byte[] bytes) {
		return decodePocs(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodePocs(ByteBuffer buffer) {
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePcso(byte[] bytes) {
		return decodePcso(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodePcso(ByteBuffer buffer) {
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodePcos(byte[] bytes) {
		return decodePcos(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodePcos(ByteBuffer buffer) {
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOspc(byte[] bytes) {
		return decodeOspc(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeOspc(ByteBuffer buffer) {
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOscp(byte[] bytes) {
		return decodeOscp(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeOscp(ByteBuffer buffer) {
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOpsc(byte[] bytes) {
		return decodeOpsc(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeOpsc(ByteBuffer buffer) {
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOpcs(byte[] bytes) {
		return decodeOpcs(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeOpcs(ByteBuffer buffer) {
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOcsp(byte[] bytes) {
		return decodeOcsp(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeOcsp(ByteBuffer buffer) {
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeOcps(byte[] bytes) {
		return decodeOcps(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeOcps(ByteBuffer buffer) {
		long o = Varint.readUnsigned(buffer);
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCspo(byte[] bytes) {
		return decodeCspo(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeCspo(ByteBuffer buffer) {
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCsop(byte[] bytes) {
		return decodeCsop(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeCsop(ByteBuffer buffer) {
		long c = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCpso(byte[] bytes) {
		return decodeCpso(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeCpso(ByteBuffer buffer) {
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCpos(byte[] bytes) {
		return decodeCpos(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeCpos(ByteBuffer buffer) {
		long c = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCosp(byte[] bytes) {
		return decodeCosp(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeCosp(ByteBuffer buffer) {
		long c = Varint.readUnsigned(buffer);
		long o = Varint.readUnsigned(buffer);
		long s = Varint.readUnsigned(buffer);
		long p = Varint.readUnsigned(buffer);
		ensureFullyConsumed(buffer);
		return new QuadKey(s, p, o, c);
	}

	public static QuadKey decodeCops(byte[] bytes) {
		return decodeCops(ByteBuffer.wrap(bytes));
	}

	private static QuadKey decodeCops(ByteBuffer buffer) {
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

	private static int encodeSequenceInto(ByteBuffer buffer, long first, long second, long third, long fourth) {
		int startPosition = buffer.position();
		Varint.writeUnsigned(buffer, first);
		Varint.writeUnsigned(buffer, second);
		Varint.writeUnsigned(buffer, third);
		Varint.writeUnsigned(buffer, fourth);
		return buffer.position() - startPosition;
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
