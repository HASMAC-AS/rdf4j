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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongConsumer;

import org.junit.jupiter.api.Test;

class LFTJExecutorLeapfrogTest {

	@Test
	void leapfrogTerminatesWhenSeekDoesNotAdvance() throws Exception {
		LMDBTrieIterator stuck = mock(LMDBTrieIterator.class);
		LMDBTrieIterator ahead = mock(LMDBTrieIterator.class);

		when(stuck.atEnd()).thenReturn(false);
		when(ahead.atEnd()).thenReturn(false);
		when(stuck.key()).thenReturn(1L);
		when(ahead.key()).thenReturn(2L);

		AtomicInteger seeks = new AtomicInteger();
		doAnswer(invocation -> {
			if (seeks.incrementAndGet() > 5) {
				throw new IllegalStateException("seek did not advance");
			}
			return null;
		}).when(stuck).seek(anyLong());
		doAnswer(invocation -> null).when(ahead).seek(anyLong());

		QuadKeyOrder spoc = QuadKeyOrder.of(Slot.S, Slot.P, Slot.O, Slot.C);
		LFTJExecutor executor = new LFTJExecutor(0L, Map.of(spoc, 1));
		Method leapfrog = LFTJExecutor.class.getDeclaredMethod("leapfrog", List.class, LongConsumer.class);
		leapfrog.setAccessible(true);

		assertTimeoutPreemptively(Duration.ofMillis(200), () -> assertDoesNotThrow(() -> {
			leapfrog.invoke(executor, new java.util.ArrayList<>(List.of(stuck, ahead)), (LongConsumer) value -> {
				// no-op
			});
		}));
	}
}
