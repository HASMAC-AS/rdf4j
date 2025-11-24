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
package org.eclipse.rdf4j.benchmark.lmdb.lftj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

import org.eclipse.rdf4j.sail.lmdb.lftj.LeapfrogIteratorCursor;
import org.eclipse.rdf4j.sail.lmdb.lftj.TrieIterator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Microbenchmark comparing LeapfrogIteratorCursor intersection throughput over in-memory sorted arrays.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class LeapfrogIteratorCursorBenchmark {

	@State(Scope.Thread)
	public static class BenchmarkState {
		@Param({ "2", "4", "6" })
		public int iterators;

		@Param({ "1000", "100000" })
		public int length;

		@Param({ "0.0", "0.1", "0.5" })
		public double overlapRatio;

		public List<TrieIterator> trieIterators;

		@Setup(Level.Trial)
		public void setUp() {
			trieIterators = new ArrayList<>(iterators);
			ThreadLocalRandom random = ThreadLocalRandom.current();
			for (int i = 0; i < iterators; i++) {
				long[] data = new long[length];
				for (int j = 0; j < length; j++) {
					// base distribution plus optional shared prefix to control overlap
					long base = j + (i * length);
					long shared = (j < length * overlapRatio) ? j : 0;
					data[j] = base + shared;
				}
				Arrays.sort(data);
				trieIterators.add(new ArrayTrieIterator(data));
			}
		}
	}

	@Benchmark
	public long intersect(BenchmarkState state) {
		LeapfrogIteratorCursor cursor = new LeapfrogIteratorCursor(state.trieIterators);
		long count = 0;
		while (cursor.hasValue()) {
			count += cursor.current();
			cursor.advance();
		}
		return count;
	}

	/**
	 * Simple TrieIterator backed by a sorted long array.
	 */
	static final class ArrayTrieIterator implements TrieIterator {
		private final long[] data;
		private int pos = 0;

		ArrayTrieIterator(long[] data) {
			this.data = data;
			if (data.length == 0) {
				pos = -1;
			}
		}

		@Override
		public void open(org.eclipse.rdf4j.sail.lmdb.lftj.Prefix prefix) {
			// unused; data fixed at construction
		}

		@Override
		public boolean atEnd() {
			return pos < 0 || pos >= data.length;
		}

		@Override
		public long key() {
			if (atEnd()) {
				throw new IllegalStateException("Iterator at end");
			}
			return data[pos];
		}

		@Override
		public void next() {
			if (!atEnd()) {
				pos++;
			}
		}

		@Override
		public void seek(long value) {
			if (atEnd()) {
				return;
			}
			while (pos < data.length && data[pos] < value) {
				pos++;
			}
		}
	}
}
