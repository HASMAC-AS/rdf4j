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
package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.sail.nativerdf.btree.RecordComparator;
import org.junit.jupiter.api.Test;

class TripleComparatorTest {

	private static final int RECORD_LENGTH = 16;

	@Test
	void rejectsUnknownFieldSequence() {
		assertThatThrownBy(() -> instantiateComparator("zzzz"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("zzzz");
	}

	@Test
	void comparesLikeReferenceImplementation() {
		for (String order : allFieldOrders()) {
			RecordComparator comparator = instantiateComparator(order);
			Random random = new Random(order.hashCode());
			char[] sequence = order.toCharArray();
			for (int i = 0; i < 128; i++) {
				int offset = (i % 3) * 4;
				byte[] key = randomBytes(random, RECORD_LENGTH);
				byte[] data = randomBytes(random, offset + RECORD_LENGTH);
				if (i % 5 == 0) {
					System.arraycopy(key, 0, data, offset, RECORD_LENGTH);
				}
				int actual = comparator.compareBTreeValues(key, data, offset, RECORD_LENGTH);
				int expected = referenceCompare(key, data, offset, sequence);
				assertThat(Integer.signum(actual))
						.as("order=%s iteration=%s", order, i)
						.isEqualTo(Integer.signum(expected));
			}
		}
	}

	private static RecordComparator instantiateComparator(String fieldSequence) {
		try {
			Class<?> comparatorClass = Class.forName("org.eclipse.rdf4j.sail.nativerdf.TripleStore$TripleComparator");
			var ctor = comparatorClass.getDeclaredConstructor(String.class);
			ctor.setAccessible(true);
			return (RecordComparator) ctor.newInstance(fieldSequence);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static Collection<String> allFieldOrders() {
		try {
			Class<?> comparatorClass = Class
					.forName("org.eclipse.rdf4j.sail.nativerdf.TripleStore$TripleComparator");
			Class<?> recordComparatorClass = Class
					.forName("org.eclipse.rdf4j.sail.nativerdf.btree.RecordComparator");

			return Arrays.stream(comparatorClass.getDeclaredFields())
					.filter(f -> isStaticFinalRecordComparator(f, recordComparatorClass))
					.map(Field::getName)
					.map(TripleComparatorTest::orderFromFieldName)
					.sorted()
					.collect(Collectors.toList());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isStaticFinalRecordComparator(Field f, Class<?> recordComparatorClass) {
		int m = f.getModifiers();
		return Modifier.isStatic(m) && Modifier.isFinal(m) && recordComparatorClass.isAssignableFrom(f.getType());
	}

	private static String orderFromFieldName(String fieldName) {
		String base = fieldName.startsWith("compare") ? fieldName.substring("compare".length()) : fieldName;
		return base.toLowerCase(Locale.ROOT);
	}

	private static byte[] randomBytes(Random random, int length) {
		byte[] bytes = new byte[length];
		random.nextBytes(bytes);
		return bytes;
	}

	private static int referenceCompare(byte[] key, byte[] data, int offset, char[] sequence) {
		for (char field : sequence) {
			int base = fieldOffset(field);
			int keyValue = readInt(key, base);
			int dataValue = readInt(data, offset + base);
			int cmp = Integer.compareUnsigned(keyValue, dataValue);
			if (cmp != 0) {
				return cmp;
			}
		}
		return 0;
	}

	private static int fieldOffset(char field) {
		switch (field) {
		case 's':
			return 0;
		case 'p':
			return 4;
		case 'o':
			return 8;
		case 'c':
			return 12;
		default:
			throw new IllegalArgumentException("Unknown field: " + field);
		}
	}

	private static int readInt(byte[] source, int offset) {
		return ((source[offset] & 0xFF) << 24)
				| ((source[offset + 1] & 0xFF) << 16)
				| ((source[offset + 2] & 0xFF) << 8)
				| (source[offset + 3] & 0xFF);
	}
}
