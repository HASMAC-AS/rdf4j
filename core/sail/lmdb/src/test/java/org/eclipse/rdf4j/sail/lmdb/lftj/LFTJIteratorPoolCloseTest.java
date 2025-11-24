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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LFTJIteratorPoolCloseTest {

	@Test
	void pooledIteratorsAreClosedAfterEvaluate() throws Exception {
		QuadKeyOrder spoc = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);
		TrackingProvider provider = new TrackingProvider(spoc, 1);

		LFTJExecutor executor = new LFTJExecutor(1L, Map.of(spoc, 1), provider);

		QuadPattern pattern = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(1L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(0L));

		executor.evaluate(List.of(pattern));

		assertThat(provider.created).hasSize(1);
		assertThat(provider.created.get(0).closed).isTrue();
	}

	private static final class TrackingProvider implements LFTJExecutor.TrieIteratorProvider {
		private final QuadKeyOrder order;
		private final int dbi;
		private final List<TrackingIterator> created = new ArrayList<>();

		TrackingProvider(QuadKeyOrder order, int dbi) {
			this.order = order;
			this.dbi = dbi;
		}

		@Override
		public CloseableTrieIterator create(long txn, int dbi, QuadKeyOrder order, Slot slot) throws IOException {
			TrackingIterator it = new TrackingIterator(slot, this.dbi, this.order);
			created.add(it);
			return it;
		}
	}

	private static final class TrackingIterator implements CloseableTrieIterator {
		private final Slot slot;
		private final int dbi;
		private final QuadKeyOrder order;
		private boolean end = false;
		private boolean closed;

		TrackingIterator(Slot slot, int dbi, QuadKeyOrder order) {
			this.slot = slot;
			this.dbi = dbi;
			this.order = order;
		}

		@Override
		public void open(Prefix prefix) {
			end = true; // immediately at end to trigger pool release path
		}

		@Override
		public boolean atEnd() {
			return end;
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
