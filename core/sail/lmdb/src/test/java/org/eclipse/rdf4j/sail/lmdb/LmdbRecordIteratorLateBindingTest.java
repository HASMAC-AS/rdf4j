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

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbRecordIteratorLateBindingTest {

	@TempDir
	File dataDir;

	private TripleStore tripleStore;

	@BeforeEach
	void setUp() throws Exception {
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"), null);

		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 0, true);
		tripleStore.storeTriple(1, 5, 6, 0, true);
		tripleStore.storeTriple(1, 6, 9, 0, true);
		tripleStore.storeTriple(2, 5, 6, 0, true);
		tripleStore.commit();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tripleStore != null) {
			tripleStore.close();
		}
	}

	@Test
	void subjectObjectPatternStillFiltersWithMatcher() throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				LmdbRecordIterator iter = (LmdbRecordIterator) tripleStore.getTriples(txn, 1, -1, 6, -1, true)) {
			assertThat(iter).isInstanceOf(LmdbRecordIterator.class);
			assertThat(getBooleanField(iter, "needMatcher")).isTrue();

			List<long[]> seen = new ArrayList<>();
			long[] next;
			while ((next = iter.next()) != null) {
				seen.add(next.clone());
			}

			assertThat(seen).containsExactly(new long[] { 1, 5, 6, 0 });
			assertThat(getField(iter, "groupMatcher")).isNotNull();
		}
	}

	@Test
	void exhaustedIteratorTransfersResourcesToFreshProbe() throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator expected = tripleStore.getTriples(txn, 2, -1, 6, -1, true);
				LmdbRecordIterator first = (LmdbRecordIterator) tripleStore.getTriples(txn, 1, -1, 6, -1, true, null,
						null, true)) {
			assertThat(collect(expected)).containsExactly(new long[] { 2, 5, 6, 0 });
			assertThat(collect(first)).containsExactly(new long[] { 1, 5, 6, 0 });

			RecordIterator reused = tripleStore.getTriples(txn, 2, -1, 6, -1, true, null, first, true);
			assertThat(reused).isInstanceOf(LmdbRecordIterator.class);
			assertThat(reused).isNotSameAs(first);

			first.close();
			assertThat(collect(reused)).containsExactly(new long[] { 2, 5, 6, 0 });
			reused.close();
		}
	}

	private static List<long[]> collect(RecordIterator iterator) throws Exception {
		List<long[]> seen = new ArrayList<>();
		long[] next;
		while ((next = iterator.next()) != null) {
			seen.add(next.clone());
		}
		return seen;
	}

	private static boolean getBooleanField(Object instance, String name) throws Exception {
		Field field = LmdbRecordIterator.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.getBoolean(instance);
	}

	private static Object getField(Object instance, String name) throws Exception {
		Field field = LmdbRecordIterator.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(instance);
	}
}
