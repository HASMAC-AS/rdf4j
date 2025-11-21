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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Reader for the compact trie binary format written by {@link CompactTrieWriter}.
 */
final class CompactTrieReader {

	private static final byte[] MAGIC = new byte[] { 'C', 'L', 'T', 'J' };
	private static final int HEADER_BYTES = 70;

	private CompactTrieReader() {
	}

	static LoadedTrie load(Path file) throws IOException {
		if (!Files.isReadable(file)) {
			throw new IOException("File not readable: " + file);
		}
		try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)) {
			ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
			readFully(fc, header);
			header.flip();
			byte[] magic = new byte[4];
			header.get(magic);
			for (int i = 0; i < 4; i++) {
				if (magic[i] != MAGIC[i]) {
					throw new IOException("Invalid magic in compact trie file: " + file);
				}
			}
			int version = header.getInt();
			if (version != 1) {
				throw new IOException("Unsupported compact trie version " + version);
			}
			byte arity = header.get();
			byte levels = header.get();
			if (arity != 4 || levels != 4) {
				throw new IOException("Unexpected arity/levels in compact trie: " + arity + "/" + levels);
			}
			char[] perm = new char[4];
			for (int i = 0; i < 4; i++) {
				perm[i] = (char) header.get();
			}
			long c0 = header.getLong();
			long c1 = header.getLong();
			long c2 = header.getLong();
			long c3 = header.getLong();
			long o0 = header.getLong();
			long o1 = header.getLong();
			long o2 = header.getLong();

			long[] v0 = readLongs(fc, c0);
			long[] off0 = readLongs(fc, o0);
			long[] v1 = readLongs(fc, c1);
			long[] off1 = readLongs(fc, o1);
			long[] v2 = readLongs(fc, c2);
			long[] off2 = readLongs(fc, o2);
			long[] v3 = readLongs(fc, c3);

			return new LoadedTrie(new TrieIndexManager.IndexOrder(new String(perm)), v0, v1, v2, v3, off0, off1,
					off2);
		}
	}

	private static long[] readLongs(FileChannel fc, long count) throws IOException {
		long[] arr = new long[(int) count];
		if (count == 0) {
			return arr;
		}
		ByteBuffer buf = ByteBuffer.allocate((int) (count * Long.BYTES)).order(ByteOrder.LITTLE_ENDIAN);
		readFully(fc, buf);
		buf.flip();
		for (int i = 0; i < count; i++) {
			arr[i] = buf.getLong();
		}
		return arr;
	}

	private static void readFully(FileChannel fc, ByteBuffer buf) throws IOException {
		while (buf.hasRemaining()) {
			if (fc.read(buf) == -1) {
				throw new IOException("Unexpected EOF while reading compact trie file");
			}
		}
	}

	/** Lightweight holder for loaded trie arrays. */
	static final class LoadedTrie {
		final TrieIndexManager.IndexOrder order;
		final long[] v0;
		final long[] v1;
		final long[] v2;
		final long[] v3;
		final long[] off0;
		final long[] off1;
		final long[] off2;

		LoadedTrie(TrieIndexManager.IndexOrder order, long[] v0, long[] v1, long[] v2, long[] v3, long[] off0,
				long[] off1, long[] off2) {
			this.order = order;
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
