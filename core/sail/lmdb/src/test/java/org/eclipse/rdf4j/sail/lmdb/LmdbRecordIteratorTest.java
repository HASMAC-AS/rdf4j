/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbRecordIteratorTest {

	private TripleStore tripleStore;

	@BeforeEach
	void setUp(@TempDir File dataDir) throws Exception {
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"), null);

		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 0, true);
		tripleStore.storeTriple(1, 2, 4, 0, true);
		tripleStore.storeTriple(5, 6, 7, 0, true);
		tripleStore.storeTriple(8, 9, 10, 11, true);
		tripleStore.commit();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tripleStore != null) {
			tripleStore.close();
		}
	}

	@Test
	void fillBatchMatchesScalarIteration() throws Exception {
		List<long[]> expected = new ArrayList<>();
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iter = tripleStore.getTriples(txn, -1, -1, -1, -1, true)) {
			long[] quad;
			while ((quad = iter.next()) != null) {
				expected.add(Arrays.copyOf(quad, quad.length));
			}
		}

		Method fillBatch;
		try {
			fillBatch = RecordIterator.class.getMethod("fillBatch", long[].class, int.class, int.class);
		} catch (NoSuchMethodException e) {
			fail("RecordIterator.fillBatch(long[], int, int) is required for batch decoding", e);
			return;
		}

		long[] buffer = new long[expected.size() * 4];
		int total = 0;

		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iter = tripleStore.getTriples(txn, -1, -1, -1, -1, true)) {
			while (true) {
				Object result = fillBatch.invoke(iter, buffer, total * 4, expected.size() - total);
				int loaded = ((Integer) result).intValue();
				if (loaded <= 0) {
					break;
				}
				total += loaded;
			}
		}

		assertEquals(expected.size(), total, "Batch iterator should return all quads");

		for (int i = 0; i < expected.size(); i++) {
			long[] actual = Arrays.copyOfRange(buffer, i * 4, i * 4 + 4);
			assertArrayEquals(expected.get(i), actual, "Quad " + i + " should match scalar iteration");
		}
	}
}
