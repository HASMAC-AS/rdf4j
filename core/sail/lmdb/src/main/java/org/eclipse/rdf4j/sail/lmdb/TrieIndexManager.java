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

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_DUPSORT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NODUPDATA;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_put;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * Maintains per-index trie databases (three levels) that expose ordered children for each prefix. Each configured
 * triple index (e.g. spoc) gets three DUPSORT LMDB databases per explicitness flag. The manager handles writes; reads
 * are provided by {@link TrieLevelCursor}.
 */
final class TrieIndexManager {

	static final class TrieDbs {
		final IndexOrder order;
		final int[] level1 = new int[2]; // [explicit, inferred]
		final int[] level2 = new int[2];
		final int[] level3 = new int[2];

		TrieDbs(IndexOrder order) {
			this.order = order;
		}
	}

	private final Map<String, TrieDbs> indexes = new HashMap<>();
	private final long env;

	TrieIndexManager(long env, String indexSpecCsv) throws IOException {
		this.env = env;
		for (IndexOrder order : parse(indexSpecCsv)) {
			TrieDbs dbs = new TrieDbs(order);
			indexes.put(order.name, dbs);
			openDbs(dbs, true);
			openDbs(dbs, false);
		}
	}

	boolean hasIndexes() {
		return !indexes.isEmpty();
	}

	List<String> getIndexNames() {
		return new ArrayList<>(indexes.keySet());
	}

	TrieLevelCursor openCursor(String indexName, int level, boolean explicit, long txn) throws IOException {
		return openCursor(indexName, level, explicit, txn, null, null);
	}

	TrieLevelCursor openCursor(String indexName, int level, boolean explicit, long txn, Pool pool, Object cursorOwner)
			throws IOException {
		TrieDbs dbs = indexes.get(indexName);
		if (dbs == null) {
			throw new IllegalArgumentException("Unknown index " + indexName);
		}
		int dbi = dbForLevel(dbs, level, explicit);
		Object owner = cursorOwner != null ? cursorOwner : dbs;
		return new TrieLevelCursor(dbs.order, level, dbi, txn, pool, owner);
	}

	void insert(IdQuad quad, boolean explicit, long txn) throws IOException {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			for (TrieDbs dbs : indexes.values()) {
				long[] reordered = dbs.order.reorder(quad);
				putLevel(stack, dbs.level1[explicit ? 0 : 1], txn, reordered[0], reordered[1]);
				putLevel(stack, dbs.level2[explicit ? 0 : 1], txn,
						reordered[0], reordered[1], reordered[2]);
				putLevel(stack, dbs.level3[explicit ? 0 : 1], txn,
						reordered[0], reordered[1], reordered[2], reordered[3]);
			}
		}
	}

	void delete(IdQuad quad, boolean explicit, long txn) throws IOException {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			for (TrieDbs dbs : indexes.values()) {
				long[] reordered = dbs.order.reorder(quad);
				delLevel(stack, dbs.level1[explicit ? 0 : 1], txn, reordered[0], reordered[1]);
				delLevel(stack, dbs.level2[explicit ? 0 : 1], txn,
						reordered[0], reordered[1], reordered[2]);
				delLevel(stack, dbs.level3[explicit ? 0 : 1], txn,
						reordered[0], reordered[1], reordered[2], reordered[3]);
			}
		}
	}

	private void openDbs(TrieDbs dbs, boolean explicit) throws IOException {
		int idx = explicit ? 0 : 1;
		int flags = MDB_CREATE | MDB_DUPSORT;
		dbs.level1[idx] = LmdbUtil.openDatabase(env, dbName(dbs.order.name, 1, explicit), flags, null);
		dbs.level2[idx] = LmdbUtil.openDatabase(env, dbName(dbs.order.name, 2, explicit), flags, null);
		dbs.level3[idx] = LmdbUtil.openDatabase(env, dbName(dbs.order.name, 3, explicit), flags, null);
	}

	private void putLevel(MemoryStack stack, int dbi, long txn, long... ids) throws IOException {
		MDBVal keyVal = MDBVal.malloc(stack);
		MDBVal dataVal = MDBVal.malloc(stack);
		ByteBuffer key = stack.malloc(keyLength(ids.length - 1));
		ByteBuffer val = stack.malloc(Varint.calcLengthUnsigned(ids[ids.length - 1]));

		for (int i = 0; i < ids.length - 1; i++) {
			Varint.writeUnsigned(key, ids[i]);
		}
		key.flip();
		keyVal.mv_data(key);

		Varint.writeUnsigned(val, ids[ids.length - 1]);
		val.flip();
		dataVal.mv_data(val);

		int rc = mdb_put(txn, dbi, keyVal, dataVal, MDB_NODUPDATA);
		if (rc != MDB_SUCCESS && rc != org.lwjgl.util.lmdb.LMDB.MDB_KEYEXIST) {
			E(rc);
		}
	}

	private void delLevel(MemoryStack stack, int dbi, long txn, long... ids) throws IOException {
		MDBVal keyVal = MDBVal.malloc(stack);
		MDBVal dataVal = MDBVal.malloc(stack);
		ByteBuffer key = stack.malloc(keyLength(ids.length - 1));
		ByteBuffer val = stack.malloc(Varint.calcLengthUnsigned(ids[ids.length - 1]));

		for (int i = 0; i < ids.length - 1; i++) {
			Varint.writeUnsigned(key, ids[i]);
		}
		key.flip();
		keyVal.mv_data(key);

		Varint.writeUnsigned(val, ids[ids.length - 1]);
		val.flip();
		dataVal.mv_data(val);

		// Delete exact duplicate; ignore NOTFOUND.
		int rc = org.lwjgl.util.lmdb.LMDB.mdb_del(txn, dbi, keyVal, dataVal);
		if (rc != MDB_SUCCESS && rc != org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND) {
			E(rc);
		}
	}

	private int dbForLevel(TrieDbs dbs, int level, boolean explicit) {
		int idx = explicit ? 0 : 1;
		switch (level) {
		case 1:
			return dbs.level1[idx];
		case 2:
			return dbs.level2[idx];
		case 3:
			return dbs.level3[idx];
		default:
			throw new IllegalArgumentException("Level must be 1..3");
		}
	}

	private int keyLength(int components) {
		// each component varint uses up to 9 bytes
		return components * 9;
	}

	private String dbName(String index, int level, boolean explicit) {
		return "trie_" + index + "_L" + level + (explicit ? "_exp" : "_inf");
	}

	private List<IndexOrder> parse(String csv) throws IOException {
		List<IndexOrder> result = new ArrayList<>();
		if (csv == null || csv.isEmpty()) {
			return result;
		}
		StringTokenizer tok = new StringTokenizer(csv, ", \t");
		while (tok.hasMoreTokens()) {
			String name = tok.nextToken().toLowerCase(Locale.ROOT);
			if (name.length() != 4 || name.indexOf('s') < 0 || name.indexOf('p') < 0 || name.indexOf('o') < 0
					|| name.indexOf('c') < 0) {
				throw new IOException("Invalid index spec '" + name + "'");
			}
			result.add(new IndexOrder(name));
		}
		return result;
	}

	static final class IndexOrder {
		final String name;
		final char[] order;

		IndexOrder(String name) {
			this.name = name;
			this.order = name.toCharArray();
		}

		long[] reorder(IdQuad quad) {
			long[] out = new long[4];
			for (int i = 0; i < 4; i++) {
				out[i] = pick(order[i], quad);
			}
			return out;
		}

		private long pick(char c, IdQuad quad) {
			switch (c) {
			case 's':
				return quad.s;
			case 'p':
				return quad.p;
			case 'o':
				return quad.o;
			case 'c':
				return quad.c;
			default:
				throw new IllegalArgumentException("Unknown component " + c);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof IndexOrder)) {
				return false;
			}
			return name.equals(((IndexOrder) obj).name);
		}
	}
}
