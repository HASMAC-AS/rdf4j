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
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;
import java.util.Arrays;

/**
 * In-memory navigator over a {@link CompactTrieReader.LoadedTrie}.
 */
final class CompactTrieNavigator implements TrieNavigator {

	private final long[][] values = new long[4][];
	private final long[][] offsets = new long[3][];

	private final int[] lo = new int[4];
	private final int[] hi = new int[4];
	private final int[] idx = new int[4];
	private int level;
	private boolean atEnd;

	CompactTrieNavigator(CompactTrieReader.LoadedTrie trie) {
		values[0] = trie.v0;
		values[1] = trie.v1;
		values[2] = trie.v2;
		values[3] = trie.v3;
		offsets[0] = trie.off0;
		offsets[1] = trie.off1;
		offsets[2] = trie.off2;
	}

	@Override
	public void openRoot() {
		level = 0;
		lo[0] = 0;
		hi[0] = values[0].length;
		idx[0] = lo[0];
		atEnd = hi[0] == 0;
	}

	@Override
	public boolean openPrefix(long... prefix) {
		openRoot();
		for (int i = 0; i < prefix.length; i++) {
			if (atEnd) {
				return false;
			}
			int pos = seekInRange(level, prefix[i], true);
			if (atEnd || pos >= hi[level] || values[level][pos] != prefix[i]) {
				atEnd = true;
				return false;
			}
			idx[level] = pos;
			if (level < 3) {
				descend();
			}
		}
		return !atEnd;
	}

	@Override
	public boolean next() {
		if (atEnd) {
			return false;
		}
		idx[level]++;
		if (idx[level] >= hi[level]) {
			atEnd = true;
			return false;
		}
		return true;
	}

	@Override
	public boolean seek(long target) {
		if (atEnd) {
			return false;
		}
		int pos = seekInRange(level, target, false);
		if (pos >= hi[level]) {
			atEnd = true;
			return false;
		}
		idx[level] = pos;
		return true;
	}

	private int seekInRange(int lvl, long target, boolean exactOnly) {
		int l = lo[lvl];
		int r = hi[lvl];
		int pos = Arrays.binarySearch(values[lvl], l, r, target);
		if (pos < 0) {
			pos = -pos - 1;
		}
		if (exactOnly && pos < r && values[lvl][pos] != target) {
			pos = r; // signal not found
		}
		if (pos >= r) {
			atEnd = true;
		}
		return pos;
	}

	@Override
	public long key() {
		return values[level][idx[level]];
	}

	@Override
	public boolean atEnd() {
		return atEnd;
	}

	@Override
	public void descend() {
		if (level >= 3) {
			return;
		}
		int pos = idx[level];
		int childLo = (int) offsets[level][pos];
		int childHi = (int) offsets[level][pos + 1];
		level++;
		lo[level] = childLo;
		hi[level] = childHi;
		idx[level] = childLo;
		atEnd = childLo == childHi;
	}

	@Override
	public void ascend() {
		if (level == 0) {
			return;
		}
		level--;
		atEnd = idx[level] >= hi[level];
	}

	@Override
	public int level() {
		return level;
	}

	@Override
	public void close() {
		// nothing to release; arrays are in-heap
	}
}
