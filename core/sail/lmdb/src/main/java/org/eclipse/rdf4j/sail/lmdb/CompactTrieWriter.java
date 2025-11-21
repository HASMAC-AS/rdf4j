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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a compact, array-based trie for a single permutation and writes it to disk.
 * <p>
 * v1 keeps the layout intentionally simple: sorted value arrays per level with first-child offset tables.
 */
final class CompactTrieWriter {

	private static final byte[] MAGIC = new byte[] { 'C', 'L', 'T', 'J' };
	private static final int VERSION = 1;
	private static final int HEADER_BYTES = 70; // exact bytes written in header block

	private CompactTrieWriter() {
	}

	static void write(List<IdQuad> quads, TrieIndexManager.IndexOrder order, Path output) throws IOException {
		List<long[]> tuples = reorder(quads, order);
		TrieArrays arrays = buildArrays(tuples);
		writeFile(order, arrays, output);
	}

	private static List<long[]> reorder(List<IdQuad> quads, TrieIndexManager.IndexOrder order) {
		List<long[]> out = new ArrayList<>(quads.size());
		for (IdQuad q : quads) {
			long[] r = order.reorder(q);
			out.add(r);
		}
		Collections.sort(out, lexComparator());
		return out;
	}

	private static Comparator<long[]> lexComparator() {
		return (a, b) -> {
			for (int i = 0; i < 4; i++) {
				int cmp = Long.compare(a[i], b[i]);
				if (cmp != 0) {
					return cmp;
				}
			}
			return 0;
		};
	}

	private static TrieArrays buildArrays(List<long[]> tuples) {
		List<Long> v0 = new ArrayList<>();
		List<Long> v1 = new ArrayList<>();
		List<Long> v2 = new ArrayList<>();
		List<Long> v3 = new ArrayList<>();
		List<Long> off0 = new ArrayList<>();
		List<Long> off1 = new ArrayList<>();
		List<Long> off2 = new ArrayList<>();

		// ensure offset lists start at 0 for non-empty cases; empty tries handled after loop
		if (!tuples.isEmpty()) {
			off0.add(0L);
			off1.add(0L);
			off2.add(0L);
		}

		long prevA = Long.MIN_VALUE;
		long prevB = Long.MIN_VALUE;
		long prevC = Long.MIN_VALUE;
		long prevD = Long.MIN_VALUE;
		boolean first = true;

		for (long[] t : tuples) {
			long a = t[0];
			long b = t[1];
			long c = t[2];
			long d = t[3];

			if (!first && a == prevA && b == prevB && c == prevC && d == prevD) {
				continue; // exact duplicate tuple; skip
			}

			if (first || a != prevA) {
				v0.add(a);
				if (!first) {
					off0.add((long) v1.size());
				}
				prevA = a;
				prevB = Long.MIN_VALUE;
			}

			if (first || b != prevB) {
				v1.add(b);
				if (!first && b != prevB) {
					off1.add((long) v2.size());
				}
				prevB = b;
				prevC = Long.MIN_VALUE;
			}

			if (first || c != prevC) {
				v2.add(c);
				if (!first && c != prevC) {
					off2.add((long) v3.size());
				}
				prevC = c;
			}

			v3.add(d);
			prevD = d;
			first = false;
		}

		if (!tuples.isEmpty()) {
			off2.add((long) v3.size());
			off1.add((long) v2.size());
			off0.add((long) v1.size());
		} else {
			// empty trie: single zero offset per level
			off0.add(0L);
			off1.add(0L);
			off2.add(0L);
		}

		return new TrieArrays(toLongArray(v0), toLongArray(v1), toLongArray(v2), toLongArray(v3),
				toLongArray(off0), toLongArray(off1), toLongArray(off2));
	}

	private static long[] toLongArray(List<Long> list) {
		long[] arr = new long[list.size()];
		for (int i = 0; i < list.size(); i++) {
			arr[i] = list.get(i);
		}
		return arr;
	}

	private static void writeFile(TrieIndexManager.IndexOrder order, TrieArrays arrays, Path output)
			throws IOException {
		Files.createDirectories(output.getParent());
		OpenOption[] opts = new OpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE };

		try (FileChannel fc = FileChannel.open(output, opts)) {
			ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
			header.put(MAGIC);
			header.putInt(VERSION);
			header.put((byte) 4); // arity
			header.put((byte) 4); // levels
			for (char c : order.order) {
				header.put((byte) c);
			}
			// counts
			header.putLong(arrays.v0.length);
			header.putLong(arrays.v1.length);
			header.putLong(arrays.v2.length);
			header.putLong(arrays.v3.length);
			// offset lengths
			header.putLong(arrays.off0.length);
			header.putLong(arrays.off1.length);
			header.putLong(arrays.off2.length);

			header.flip();
			fc.write(header);

			writeLongArray(fc, arrays.v0);
			writeLongArray(fc, arrays.off0);
			writeLongArray(fc, arrays.v1);
			writeLongArray(fc, arrays.off1);
			writeLongArray(fc, arrays.v2);
			writeLongArray(fc, arrays.off2);
			writeLongArray(fc, arrays.v3);
		}
	}

	private static void writeLongArray(FileChannel fc, long[] data) throws IOException {
		if (data.length == 0) {
			return;
		}
		// write in chunks to avoid large buffers
		ByteBuffer buf = ByteBuffer.allocateDirect(8 * Math.min(data.length, 1024)).order(ByteOrder.LITTLE_ENDIAN);
		int idx = 0;
		while (idx < data.length) {
			buf.clear();
			int batch = Math.min(buf.capacity() / 8, data.length - idx);
			for (int i = 0; i < batch; i++) {
				buf.putLong(data[idx + i]);
			}
			buf.flip();
			fc.write(buf);
			idx += batch;
		}
	}

	/** Small struct to shuttle arrays around. */
	static final class TrieArrays {
		final long[] v0;
		final long[] v1;
		final long[] v2;
		final long[] v3;
		final long[] off0;
		final long[] off1;
		final long[] off2;

		TrieArrays(long[] v0, long[] v1, long[] v2, long[] v3, long[] off0, long[] off1, long[] off2) {
			this.v0 = v0;
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
			this.off0 = off0;
			this.off1 = off1;
			this.off2 = off2;
		}
	}
}
