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

import org.junit.jupiter.api.Test;

class LFTJExecutorCursorLeakTest {

	@Test
	void closesIteratorWhenOpenFindsNoMatches() throws Exception {
		QuadKeyOrder order = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);
		FakeIterator iterator = new FakeIterator(Slot.S);

		LFTJExecutor.TrieIteratorProvider provider = (txn, dbi, ord, slot) -> iterator;
		LFTJExecutor executor = new LFTJExecutor(1L, Map.of(order, 1), provider);

		QuadPattern pattern = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.variable("c"));

		executor.evaluate(List.of(pattern));

		assertThat(iterator.openCalled).isTrue();
		assertThat(iterator.closed).isTrue();
		assertThat(iterator.prefix.predicate()).isEqualTo(2L);
	}

	private static final class FakeIterator implements CloseableTrieIterator {
		private final Slot slot;
		boolean openCalled;
		boolean closed;
		Prefix prefix;
		int dbi = 1;
		QuadKeyOrder order = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);

		FakeIterator(Slot slot) {
			this.slot = slot;
		}

		@Override
		public void open(Prefix prefix) {
			this.prefix = prefix;
			openCalled = true;
		}

		@Override
		public boolean atEnd() {
			return true;
		}

		@Override
		public long key() {
			return 0;
		}

		@Override
		public void next() {
		}

		@Override
		public void seek(long value) {
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
	}
}
