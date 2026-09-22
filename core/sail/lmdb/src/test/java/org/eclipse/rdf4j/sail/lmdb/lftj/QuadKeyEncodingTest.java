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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class QuadKeyEncodingTest {

	@Test
	void encodeDecodeRoundTripAcrossOrders() {
		QuadKey key = new QuadKey(1L, 2L, 3L, 4L);
		QuadKeyOrder spoc = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);
		QuadKeyOrder psoc = QuadKeyOrder.of(Slot.P, Slot.S, Slot.O, Slot.C);

		byte[] spocBytes = QuadKeyEncoding.encode(key, spoc);
		QuadKey decodedSpoc = QuadKeyEncoding.decode(spocBytes, spoc);
		assertThat(decodedSpoc).isEqualTo(key);

		byte[] psocBytes = QuadKeyEncoding.encode(key, psoc);
		QuadKey decodedPsoc = QuadKeyEncoding.decode(psocBytes, psoc);
		assertThat(decodedPsoc).isEqualTo(key);
	}

	@Test
	void positionsReturnsEachSlotInOrder() {
		QuadKeyOrder order = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);

		List<Slot> positions = order.positions();

		assertThat(positions).containsExactly(Slot.S, Slot.P, Slot.O, Slot.C);
	}

	@Test
	void positionsReturnsDefensiveSnapshot() throws Exception {
		QuadKeyOrder order = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);

		List<Slot> snapshot = order.positions();

		Field field = QuadKeyOrder.class.getDeclaredField("positions");
		field.setAccessible(true);
		Slot[] internal = (Slot[]) field.get(order);
		internal[0] = Slot.C;

		assertThat(snapshot).containsExactly(Slot.S, Slot.P, Slot.O, Slot.C);
	}

	@Test
	void encodedOrderMatchesIndexLayout() {
		QuadKey key = new QuadKey(10L, 20L, 30L, 40L);
		QuadKeyOrder psoc = QuadKeyOrder.of(Slot.P, Slot.S, Slot.O, Slot.C);

		byte[] bytes = QuadKeyEncoding.encode(key, psoc);
		ByteBuffer buffer = ByteBuffer.wrap(bytes);

		assertThat(buffer.getLong()).isEqualTo(key.p());
		assertThat(buffer.getLong()).isEqualTo(key.s());
		assertThat(buffer.getLong()).isEqualTo(key.o());
		assertThat(buffer.getLong()).isEqualTo(key.c());
	}

	@Test
	void matchesPrefixRespectsBoundSlots() {
		Prefix prefix = Prefix.builder().subject(5L).object(7L).build();

		QuadKey matching = new QuadKey(5L, 9L, 7L, 0L);
		QuadKey wrongSubject = new QuadKey(6L, 9L, 7L, 0L);
		QuadKey wrongObject = new QuadKey(5L, 9L, 8L, 0L);

		assertThat(QuadKeyEncoding.matchesPrefix(matching, prefix)).isTrue();
		assertThat(QuadKeyEncoding.matchesPrefix(wrongSubject, prefix)).isFalse();
		assertThat(QuadKeyEncoding.matchesPrefix(wrongObject, prefix)).isFalse();
	}

	@Test
	void minimalKeyUsesPrefixAndMinimums() {
		Prefix prefix = Prefix.builder().subject(3L).object(11L).build();
		QuadKey minimal = QuadKeyEncoding.minimalKeyForPrefix(prefix);

		assertThat(minimal.s()).isEqualTo(3L);
		assertThat(minimal.p()).isEqualTo(QuadKeyEncoding.MIN_TERM_ID);
		assertThat(minimal.o()).isEqualTo(11L);
		assertThat(minimal.c()).isEqualTo(QuadKeyEncoding.MIN_TERM_ID);
	}

	@Test
	void lexicographicEncodingRespectsOrder() {
		QuadKeyOrder spoc = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);
		QuadKey smaller = new QuadKey(1L, 2L, 3L, 4L);
		QuadKey larger = new QuadKey(2L, 1L, 3L, 4L);

		byte[] smallerBytes = QuadKeyEncoding.encode(smaller, spoc);
		byte[] largerBytes = QuadKeyEncoding.encode(larger, spoc);

		assertThat(Arrays.compareUnsigned(smallerBytes, largerBytes)).isLessThan(0);
	}
}
