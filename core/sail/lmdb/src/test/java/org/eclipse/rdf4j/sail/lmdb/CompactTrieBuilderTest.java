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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link CompactTrieWriter} that validate array construction and file layout.
 */
class CompactTrieBuilderTest {

	@TempDir
	Path tmp;

	@Test
	void writesExpectedArrays() throws Exception {
		List<IdQuad> quads = Arrays.asList(
				new IdQuad(1, 10, 100, 5),
				new IdQuad(1, 10, 101, 5),
				new IdQuad(1, 11, 100, 5),
				new IdQuad(2, 10, 100, 5));

		TrieIndexManager.IndexOrder order = new TrieIndexManager.IndexOrder("spoc");
		Path out = tmp.resolve("trie.bin");

		CompactTrieWriter.write(quads, order, out);

		TrieFile tf = read(out);

		assertThat(tf.permutation).containsExactly('s', 'p', 'o', 'c');
		assertThat(tf.v0).containsExactly(1L, 2L);
		assertThat(tf.off0).containsExactly(0L, 2L, 3L);
		assertThat(tf.v1).containsExactly(10L, 11L, 10L);
		assertThat(tf.off1).containsExactly(0L, 2L, 3L, 4L);
		assertThat(tf.v2).containsExactly(100L, 101L, 100L, 100L);
		assertThat(tf.off2).containsExactly(0L, 1L, 2L, 3L, 4L);
		assertThat(tf.v3).containsExactly(5L, 5L, 5L, 5L);
	}

	@Test
	void handlesEmptyInput() throws Exception {
		TrieIndexManager.IndexOrder order = new TrieIndexManager.IndexOrder("spoc");
		Path out = tmp.resolve("empty.bin");

		CompactTrieWriter.write(List.of(), order, out);

		TrieFile tf = read(out);
		assertThat(tf.v0).isEmpty();
		assertThat(tf.v1).isEmpty();
		assertThat(tf.v2).isEmpty();
		assertThat(tf.v3).isEmpty();
		assertThat(tf.off0).containsExactly(0L);
		assertThat(tf.off1).containsExactly(0L);
		assertThat(tf.off2).containsExactly(0L);
	}

	private TrieFile read(Path file) throws IOException {
		CompactTrieReader.LoadedTrie trie = CompactTrieReader.load(file);
		return new TrieFile(trie.order.order, trie.v0, trie.v1, trie.v2, trie.v3, trie.off0, trie.off1, trie.off2);
	}

	private static final class TrieFile {
		final char[] permutation;
		final long[] v0;
		final long[] v1;
		final long[] v2;
		final long[] v3;
		final long[] off0;
		final long[] off1;
		final long[] off2;

		TrieFile(char[] permutation, long[] v0, long[] v1, long[] v2, long[] v3, long[] off0, long[] off1,
				long[] off2) {
			this.permutation = permutation;
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
