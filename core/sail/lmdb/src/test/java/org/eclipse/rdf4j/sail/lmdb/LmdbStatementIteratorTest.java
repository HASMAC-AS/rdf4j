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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbStatementIteratorTest {

	private ValueStore valueStore;
	private TripleStore tripleStore;

	@BeforeEach
	void setUp(@TempDir File dataDir) throws Exception {
		valueStore = new ValueStore(new File(dataDir, "values"), new LmdbStoreConfig());
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"), valueStore);

		valueStore.startTransaction(true);
		long s1 = valueStore.storeValue(Values.iri("ex:s1"));
		long p1 = valueStore.storeValue(Values.iri("ex:p1"));
		long o1 = valueStore.storeValue(Values.literal("one"));
		long c1 = valueStore.storeValue(Values.iri("ex:c1"));

		long s2 = valueStore.storeValue(Values.iri("ex:s2"));
		long p2 = valueStore.storeValue(Values.iri("ex:p2"));
		long o2 = valueStore.storeValue(Values.literal("two"));
		long c2 = valueStore.storeValue(Values.iri("ex:c2"));
		valueStore.commit();

		tripleStore.startTransaction();
		tripleStore.storeTriple(s1, p1, o1, c1, true);
		tripleStore.storeTriple(s2, p2, o2, c2, true);
		tripleStore.commit();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tripleStore != null) {
			tripleStore.close();
		}
		if (valueStore != null) {
			valueStore.close();
		}
	}

	@Test
	void fillStatementBatchMatchesScalarIteration() throws Exception {
		List<Statement> expected = new ArrayList<>();
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				LmdbStatementIterator iter = new LmdbStatementIterator(
						tripleStore.getTriples(txn, -1, -1, -1, -1, true), valueStore)) {
			while (iter.hasNext()) {
				expected.add(iter.next());
			}
		}

		Method fillStatements;
		try {
			fillStatements = LmdbStatementIterator.class.getDeclaredMethod("fillStatementBatch",
					Statement[].class, int.class, int.class);
			fillStatements.setAccessible(true);
		} catch (NoSuchMethodException e) {
			fail("LmdbStatementIterator.fillStatementBatch(Statement[], int, int) is required for batched iteration",
					e);
			return;
		}

		Statement[] buffer = new Statement[expected.size()];
		int total = 0;

		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				LmdbStatementIterator iter = new LmdbStatementIterator(
						tripleStore.getTriples(txn, -1, -1, -1, -1, true), valueStore)) {
			while (true) {
				Object result = fillStatements.invoke(iter, buffer, total, expected.size() - total);
				int loaded = ((Integer) result).intValue();
				if (loaded <= 0) {
					break;
				}
				total += loaded;
			}
		}

		assertEquals(expected.size(), total, "Batch iterator should materialise all statements");

		for (int i = 0; i < expected.size(); i++) {
			Statement expectedStatement = expected.get(i);
			Statement actualStatement = buffer[i];
			assertEquals(expectedStatement, actualStatement, "Statement " + i + " should match scalar iteration");
		}
	}
}
