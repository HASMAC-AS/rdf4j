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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazy loader/cache for compact trie files stored alongside the LMDB store.
 */
final class CompactTrieCache {

	private final Map<String, CompactTrieReader.LoadedTrie> cache = new ConcurrentHashMap<>();
	private final String[] permutations;
	private final Path baseDir;

	CompactTrieCache(String indexSpecCsv, java.io.File dir) {
		this.permutations = indexSpecCsv.split("[,\\s]+");
		this.baseDir = dir.toPath();
	}

	CompactTrieReader.LoadedTrie load(String perm, boolean explicit) throws IOException {
		String key = perm.toLowerCase(Locale.ROOT) + (explicit ? "_exp" : "_inf");
		CompactTrieReader.LoadedTrie cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		Path file = pathFor(perm, explicit);
		if (!java.nio.file.Files.exists(file)) {
			return null;
		}
		CompactTrieReader.LoadedTrie loaded = CompactTrieReader.load(file);
		cache.put(key, loaded);
		return loaded;
	}

	private Path pathFor(String perm, boolean explicit) {
		String file = "compact_" + perm.toLowerCase(Locale.ROOT) + (explicit ? "_exp" : "_inf") + ".bin";
		return baseDir.resolve(file);
	}
}
