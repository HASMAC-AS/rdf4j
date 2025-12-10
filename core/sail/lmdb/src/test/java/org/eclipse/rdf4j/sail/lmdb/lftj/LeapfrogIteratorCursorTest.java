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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class LeapfrogIteratorCursorTest {

	@Test
	void terminatesWhenSeekDoesNotAdvance() {
		TrieIterator stuck = new NonAdvancingIterator(1L);
		TrieIterator ahead = new NonAdvancingIterator(2L);

		assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
			LeapfrogIteratorCursor cursor = new LeapfrogIteratorCursor(List.of(stuck, ahead));
			assertThat(cursor.hasValue()).isFalse();
			assertThat(cursor.sawStalledSeek()).isTrue();
		});
	}

	private static final class NonAdvancingIterator implements TrieIterator {
		private long key;

		NonAdvancingIterator(long key) {
			this.key = key;
		}

		@Override
		public void open(Prefix prefix) {
		}

		@Override
		public boolean atEnd() {
			return false;
		}

		@Override
		public long key() {
			return key;
		}

		@Override
		public void next() {
		}

		@Override
		public void seek(long value) {
			// Intentionally refuse to advance to simulate LMDB cursor that cannot reach the target.
		}
	}
}
