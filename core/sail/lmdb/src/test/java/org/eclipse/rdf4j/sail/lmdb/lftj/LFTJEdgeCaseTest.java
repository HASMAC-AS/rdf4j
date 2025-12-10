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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class LFTJEdgeCaseTest {

	private static final QuadKeyOrder SPOC = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);

	@Test
	void emptyJoinYieldsNoBindings() throws Exception {
		LFTJExecutor executor = new LFTJExecutor(1L, Map.of(SPOC, 1), (txn, dbi, ord, slot) -> new EmptyIterator(slot));

		QuadPattern pattern = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(99L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(0L));

		List<Map<String, Long>> results = executor.evaluate(List.of(pattern));

		assertThat(results).isEmpty();
	}

	@Test
	void redundantPatternsIntersectToSameValues() throws Exception {
		List<QuadKey> table = List.of(
				new QuadKey(1, 1, 1, 0),
				new QuadKey(2, 1, 2, 0));

		LFTJExecutor executor = new LFTJExecutor(1L, Map.of(SPOC, 1),
				(txn, dbi, ord, slot) -> new TableIterator(slot, table));

		QuadPattern p = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(1L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(0L));

		List<Map<String, Long>> results = executor.evaluate(List.of(p, p));

		assertThat(results).hasSize(2);
		assertThat(results).allMatch(binding -> binding.get("s").equals(binding.get("o")));
		assertThat(results.stream().map(m -> m.get("s"))).containsExactlyInAnyOrder(1L, 2L);
	}

	private static final class EmptyIterator implements CloseableTrieIterator {
		private final Slot slot;
		private final int dbi = 1;
		private final QuadKeyOrder order = SPOC;

		EmptyIterator(Slot slot) {
			this.slot = slot;
		}

		@Override
		public void open(Prefix prefix) {
		}

		@Override
		public boolean atEnd() {
			return true;
		}

		@Override
		public long key() {
			throw new IllegalStateException("at end");
		}

		@Override
		public void next() {
		}

		@Override
		public void seek(long value) {
		}

		@Override
		public void close() {
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
	}

	private static final class TableIterator implements CloseableTrieIterator {
		private final Slot slot;
		private final List<QuadKey> table;
		private final int dbi = 1;
		private final QuadKeyOrder order = SPOC;
		private int idx = -1;
		private List<QuadKey> filtered = List.of();

		TableIterator(Slot slot, List<QuadKey> table) {
			this.slot = slot;
			this.table = table;
		}

		@Override
		public void open(Prefix prefix) {
			filtered = table.stream().filter(q -> matches(prefix, q)).collect(Collectors.toList());
			idx = 0;
		}

		private boolean matches(Prefix prefix, QuadKey q) {
			if (prefix.hasSubject() && prefix.subject() != q.s()) {
				return false;
			}
			if (prefix.hasPredicate() && prefix.predicate() != q.p()) {
				return false;
			}
			if (prefix.hasObject() && prefix.object() != q.o()) {
				return false;
			}
			if (prefix.hasContext() && prefix.context() != q.c()) {
				return false;
			}
			return true;
		}

		@Override
		public boolean atEnd() {
			return idx < 0 || idx >= filtered.size();
		}

		@Override
		public long key() {
			if (atEnd()) {
				throw new IllegalStateException("at end");
			}
			QuadKey q = filtered.get(idx);
			switch (slot) {
			case S:
				return q.s();
			case P:
				return q.p();
			case O:
				return q.o();
			case C:
				return q.c();
			default:
				throw new IllegalStateException("Unexpected slot " + slot);
			}
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
			while (idx < filtered.size() && key() < value) {
				idx++;
			}
		}

		@Override
		public void close() {
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
	}
}
