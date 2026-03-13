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
import static org.junit.jupiter.api.Assertions.assertEquals;

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

class SubjectPredicateIndexTest {

	@TempDir
	File dataDir;

	private TripleStore tripleStore;

	@BeforeEach
	void setUp() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("posc");
		config.setDupsortIndices(true);
		config.setDupsortRead(true);
		tripleStore = new TripleStore(dataDir, config, null);

		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 0, true);
		tripleStore.storeTriple(1, 2, 4, 0, true);
		tripleStore.storeTriple(5, 2, 6, 0, true);
		tripleStore.storeTriple(5, 2, 7, 0, true);
		tripleStore.commit();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tripleStore != null) {
			tripleStore.close();
		}
	}

	@Test
	void subjectPredicatePatternUsesDupIterator() throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iter = tripleStore.getTriples(txn, 1, 2, -1, -1, true)) {
			assertThat(iter).isInstanceOf(LmdbDupRecordIterator.class);

			int count = 0;
			while (iter.next() != null) {
				count++;
			}
			assertEquals(2, count);
		}
	}

	@Test
	void subjectPredicateDupsortPersistsAcrossRestart() throws Exception {
		tripleStore.close();

		LmdbStoreConfig config = new LmdbStoreConfig("posc");
		config.setDupsortIndices(true);
		config.setDupsortRead(true);
		tripleStore = new TripleStore(dataDir, config, null);

		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 5, 0, true);
		tripleStore.commit();

		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iter = tripleStore.getTriples(txn, 1, 2, -1, -1, true)) {
			assertThat(iter).isInstanceOf(LmdbDupRecordIterator.class);

			int count = 0;
			while (iter.next() != null) {
				count++;
			}
			assertEquals(3, count);
		}
	}

	@Test
	void subjectPredicateDupsortCacheFlushMaintainsIndex() throws Exception {
		tripleStore.startTransaction();

		TxnRecordCache cache = new TxnRecordCache(dataDir);
		try {
			cache.storeRecord(new long[] { 1, 2, 5, 0 }, true);
			Field recordCacheField = TripleStore.class.getDeclaredField("recordCache");
			recordCacheField.setAccessible(true);
			recordCacheField.set(tripleStore, cache);

			tripleStore.updateFromCache();
			recordCacheField.set(tripleStore, null);
		} finally {
			// updateFromCache closes the cache, nothing to do here
		}
		tripleStore.commit();

		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iter = tripleStore.getTriples(txn, 1, 2, -1, -1, true)) {
			assertThat(iter).isInstanceOf(LmdbDupRecordIterator.class);

			int count = 0;
			while (iter.next() != null) {
				count++;
			}
			assertEquals(3, count);
		}
	}

	@Test
	void predicateObjectPatternFallsBackToStandardIterator() throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iter = tripleStore.getTriples(txn, -1, 2, 3, -1, true)) {
			assertThat(iter).isNotInstanceOf(LmdbDupRecordIterator.class);

			int count = 0;
			while (iter.next() != null) {
				count++;
			}
			assertEquals(1, count);
		}
	}

	@Test
	void exhaustedDupIteratorTransfersResourcesToFreshProbe() throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator expected = tripleStore.getTriples(txn, 5, 2, -1, -1, true);
				RecordIterator first = tripleStore.getTriples(txn, 1, 2, -1, -1, true, null, null, true)) {
			assertThat(collect(expected)).containsExactly(new long[] { 5, 2, 6, 0 }, new long[] { 5, 2, 7, 0 });
			assertThat(collect(first)).containsExactly(new long[] { 1, 2, 3, 0 }, new long[] { 1, 2, 4, 0 });

			RecordIterator reused = tripleStore.getTriples(txn, 5, 2, -1, -1, true, null, first, true);
			assertThat(reused).isInstanceOf(LmdbDupRecordIterator.class);
			assertThat(reused).isNotSameAs(first);

			first.close();
			assertThat(collect(reused)).containsExactly(new long[] { 5, 2, 6, 0 }, new long[] { 5, 2, 7, 0 });
			reused.close();
		}
	}

	@Test
	void incompatiblePreviousIteratorFallsBackToFreshDupIterator() throws Exception {
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator previous = tripleStore.getTriples(txn, -1, 2, 3, -1, true, null, null, true)) {
			assertThat(previous).isNotInstanceOf(LmdbDupRecordIterator.class);
			assertThat(collect(previous)).containsExactly(new long[] { 1, 2, 3, 0 });

			RecordIterator fresh = tripleStore.getTriples(txn, 1, 2, -1, -1, true, null, previous, true);
			assertThat(fresh).isInstanceOf(LmdbDupRecordIterator.class);
			assertThat(fresh).isNotSameAs(previous);
			assertThat(collect(fresh)).containsExactly(new long[] { 1, 2, 3, 0 }, new long[] { 1, 2, 4, 0 });
			fresh.close();
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
}
