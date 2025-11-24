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

class LFTJIteratorPoolUsageTest {

	@Test
	void iteratorsAreReusedAcrossBindings() throws Exception {
		QuadKeyOrder spoc = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);
		ReusingProvider provider = new ReusingProvider(spoc, 1);

		LFTJExecutor executor = new LFTJExecutor(1L, Map.of(spoc, 1), provider);

		QuadPattern first = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(1L),
				QuadPatternTerm.variable("o1"),
				QuadPatternTerm.constant(0L));

		QuadPattern second = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(1L),
				QuadPatternTerm.variable("o2"),
				QuadPatternTerm.constant(0L));

		List<Map<String, Long>> results = executor.evaluate(List.of(first, second));

		assertThat(results).isNotEmpty();
		assertThat(provider.creationsFor(Slot.S)).isEqualTo(2); // one per pattern
		assertThat(provider.totalCloses()).isEqualTo(provider.totalCreated());
	}

	private static final class ReusingProvider implements LFTJExecutor.TrieIteratorProvider {
		private final QuadKeyOrder order;
		private final int dbi;
		private final Map<Slot, List<ReusingIterator>> created = new EnumMap<>(Slot.class);

		ReusingProvider(QuadKeyOrder order, int dbi) {
			this.order = order;
			this.dbi = dbi;
		}

		@Override
		public CloseableTrieIterator create(long txn, int dbi, QuadKeyOrder order, Slot slot) throws IOException {
			ReusingIterator it = new ReusingIterator(slot, this.dbi, this.order, new long[] { 1L, 2L });
			created.computeIfAbsent(slot, s -> new ArrayList<>()).add(it);
			return it;
		}

		long creationsFor(Slot slot) {
			return created.getOrDefault(slot, List.of()).size();
		}

		long opensFor(Slot slot) {
			return created.getOrDefault(slot, List.of()).stream().mapToLong(ReusingIterator::opens).sum();
		}

		long totalCreated() {
			return created.values().stream().mapToLong(List::size).sum();
		}

		long totalCloses() {
			return created.values().stream().flatMap(List::stream).filter(ReusingIterator::isClosed).count();
		}
	}

	private static final class ReusingIterator implements CloseableTrieIterator {
		private final Slot slot;
		private final int dbi;
		private final QuadKeyOrder order;
		private final long[] values;
		private int idx = -1;
		private long opens;
		private boolean closed;

		ReusingIterator(Slot slot, int dbi, QuadKeyOrder order, long[] values) {
			this.slot = slot;
			this.dbi = dbi;
			this.order = order;
			this.values = values;
		}

		@Override
		public void open(Prefix prefix) {
			opens++;
			idx = 0;
		}

		@Override
		public boolean atEnd() {
			return idx < 0 || idx >= values.length;
		}

		@Override
		public long key() {
			if (atEnd()) {
				throw new IllegalStateException("at end");
			}
			return values[idx];
		}

		@Override
		public void next() {
			if (!atEnd()) {
				idx++;
			}
		}

		@Override
		public void seek(long value) {
			if (atEnd()) {
				return;
			}
			while (idx < values.length && values[idx] < value) {
				idx++;
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
