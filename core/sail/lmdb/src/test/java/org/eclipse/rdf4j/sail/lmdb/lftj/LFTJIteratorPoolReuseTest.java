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

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LFTJIteratorPoolReuseTest {

	@Test
	void reusesIteratorsAcrossVariableBindings() throws Exception {
		QuadKeyOrder spoc = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);
		CountingProvider provider = new CountingProvider(spoc, 1);

		LFTJExecutor executor = new LFTJExecutor(0L, Map.of(spoc, 1), provider);

		QuadPattern first = QuadPattern.of(
				QuadPatternTerm.variable("x"),
				QuadPatternTerm.constant(1L),
				QuadPatternTerm.variable("y"),
				QuadPatternTerm.constant(0L));

		QuadPattern second = QuadPattern.of(
				QuadPatternTerm.variable("x"),
				QuadPatternTerm.constant(1L),
				QuadPatternTerm.variable("y"),
				QuadPatternTerm.constant(0L));

		List<Map<String, Long>> results = executor.evaluate(List.of(first, second));

		assertThat(results).hasSize(4);
		assertThat(results.stream().map(m -> m.get("x"))).containsExactlyInAnyOrder(1L, 1L, 2L, 2L);
		assertThat(results.stream().map(m -> m.get("y"))).containsExactlyInAnyOrder(10L, 10L, 20L, 20L);

		assertThat(provider.creationsFor(Slot.S)).isEqualTo(2);
		assertThat(provider.opensFor(Slot.S)).isEqualTo(2);

		assertThat(provider.creationsFor(Slot.O)).isEqualTo(2);
		assertThat(provider.opensFor(Slot.O)).isEqualTo(4);
		assertThat(provider.closedIterators(Slot.O)).isEqualTo(2);
	}

	private static final class CountingProvider implements LFTJExecutor.TrieIteratorProvider {
		private final QuadKeyOrder order;
		private final int dbi;
		private final Map<Slot, List<CountingIterator>> created = new EnumMap<>(Slot.class);

		CountingProvider(QuadKeyOrder order, int dbi) {
			this.order = order;
			this.dbi = dbi;
		}

		@Override
		public CloseableTrieIterator create(long txn, int dbi, QuadKeyOrder order, Slot slot) throws IOException {
			CountingIterator iterator = new CountingIterator(slot, this.dbi, this.order, valuesFor(slot));
			created.computeIfAbsent(slot, s -> new ArrayList<>()).add(iterator);
			return iterator;
		}

		long creationsFor(Slot slot) {
			return created.getOrDefault(slot, List.of()).size();
		}

		long opensFor(Slot slot) {
			return created.getOrDefault(slot, List.of()).stream().mapToLong(CountingIterator::opens).sum();
		}

		long closedIterators(Slot slot) {
			return created.getOrDefault(slot, List.of()).stream().filter(CountingIterator::isClosed).count();
		}

		private long[] valuesFor(Slot slot) {
			if (slot == Slot.S) {
				return new long[] { 1L, 2L };
			}
			return new long[] { 10L, 20L };
		}
	}

	private static final class CountingIterator implements CloseableTrieIterator {
		private final Slot slot;
		private final int dbi;
		private final QuadKeyOrder order;
		private final long[] values;

		private int index = -1;
		private long opens;
		private boolean closed;

		CountingIterator(Slot slot, int dbi, QuadKeyOrder order, long[] values) {
			this.slot = slot;
			this.dbi = dbi;
			this.order = order;
			this.values = values;
		}

		@Override
		public void open(Prefix prefix) {
			index = 0;
			opens++;
		}

		@Override
		public boolean atEnd() {
			return index < 0 || index >= values.length;
		}

		@Override
		public long key() {
			if (atEnd()) {
				throw new IllegalStateException("Iterator is at end");
			}
			return values[index];
		}

		@Override
		public void next() {
			if (!atEnd()) {
				index++;
			}
		}

		@Override
		public void seek(long value) {
			if (atEnd()) {
				return;
			}
			while (index < values.length && values[index] < value) {
				index++;
			}
		}

		@Override
		public void close() {
			closed = true;
		}

		@Override
		public Slot slot() {
			return slot;
		}

		@Override
		public int slotDbi() {
			return dbi;
		}

		@Override
		public QuadKeyOrder slotOrder() {
			return order;
		}

		long opens() {
			return opens;
		}

		boolean isClosed() {
			return closed;
		}
	}
}
