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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompactTrieNavigatorTest {

	@TempDir
	Path tmp;

	private CompactTrieNavigator nav;

	@BeforeEach
	void setup() throws Exception {
		List<IdQuad> quads = Arrays.asList(
				new IdQuad(1, 10, 100, 5),
				new IdQuad(1, 10, 101, 6),
				new IdQuad(1, 11, 100, 7),
				new IdQuad(2, 10, 100, 8));
		TrieIndexManager.IndexOrder order = new TrieIndexManager.IndexOrder("spoc");
		Path out = tmp.resolve("trie.bin");
		CompactTrieWriter.write(quads, order, out);
		CompactTrieReader.LoadedTrie trie = CompactTrieReader.load(out);
		nav = new CompactTrieNavigator(trie);
	}

	@Test
	void iterateRootAndChildren() throws Exception {
		nav.openRoot();
		assertThat(nav.key()).isEqualTo(1L);
		nav.descend(); // into b values for s=1
		assertThat(nav.key()).isEqualTo(10L);
		nav.descend(); // into o values for (1,10)
		assertThat(nav.key()).isEqualTo(100L);
		nav.next();
		assertThat(nav.key()).isEqualTo(101L);
	}

	@Test
	void seekWithinLevel() throws Exception {
		nav.openRoot();
		nav.seek(2L); // seek subject 2
		assertThat(nav.key()).isEqualTo(2L);
		nav.descend();
		assertThat(nav.key()).isEqualTo(10L);
	}

	@Test
	void openPrefixExact() throws Exception {
		boolean ok = nav.openPrefix(1L, 11L);
		assertThat(ok).isTrue();
		assertThat(nav.level()).isEqualTo(2);
		assertThat(nav.key()).isEqualTo(100L); // c value at level 2
	}

	@Test
	void openPrefixMissing() throws Exception {
		boolean ok = nav.openPrefix(3L);
		assertThat(ok).isFalse();
		assertThat(nav.atEnd()).isTrue();
	}
}
