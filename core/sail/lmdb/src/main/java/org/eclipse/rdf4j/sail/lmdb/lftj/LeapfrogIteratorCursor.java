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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Leapfrog intersection cursor over a set of {@link TrieIterator}s. Extracted for testability and to guard against
 * non-advancing seek implementations that could otherwise loop forever.
 */
public final class LeapfrogIteratorCursor {

	private final List<TrieIterator> iterators;
	private final int size;
	private int p;
	private boolean atValue;
	private long current;
	private boolean exhausted;
	private int stalledSeeks;

	public LeapfrogIteratorCursor(List<? extends TrieIterator> iterators) {
		this.iterators = new ArrayList<>(iterators);
		this.size = this.iterators.size();
		this.iterators.sort(Comparator.comparingLong(TrieIterator::key));
		if (this.iterators.isEmpty()) {
			exhausted = true;
			return;
		}
		for (TrieIterator iterator : this.iterators) {
			if (iterator.atEnd()) {
				exhausted = true;
				return;
			}
		}
		p = 0;
		leapfrogSearch();
	}

	public boolean hasValue() {
		return atValue && !exhausted;
	}

	public long current() {
		if (!atValue) {
			throw new IllegalStateException("No current value");
		}
		return current;
	}

	public void advance() {
		if (exhausted) {
			return;
		}
		TrieIterator iterator = iterators.get(p);
		iterator.next();
		if (iterator.atEnd()) {
			exhausted = true;
			atValue = false;
			return;
		}
		leapfrogSearch();
	}

	private void leapfrogSearch() {
		atValue = false;
		while (true) {
			int next = (p + 1) % size;
			TrieIterator currentIterator = iterators.get(p);
			TrieIterator nextIterator = iterators.get(next);
			long key = currentIterator.key();
			long nextKey = nextIterator.key();

			if (key == nextKey) {
				p = next;
				if (p == 0) {
					current = key;
					atValue = true;
					return;
				}
			} else if (key < nextKey) {
				long before = key;
				currentIterator.seek(nextKey);
				if (currentIterator.atEnd()) {
					exhausted = true;
					return;
				}
				long after = currentIterator.key();
				if (after == before) {
					// The iterator could not advance; bail out to avoid spinning forever.
					exhausted = true;
					stalledSeeks++;
					return;
				}
			} else {
				p = next;
			}
		}
	}

	public boolean sawStalledSeek() {
		return stalledSeeks > 0;
	}
}
