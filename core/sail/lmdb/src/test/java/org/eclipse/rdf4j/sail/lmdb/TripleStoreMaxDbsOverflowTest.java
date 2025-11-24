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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TripleStoreMaxDbsOverflowTest {

	private File dataDir;

	@BeforeEach
	void setUp(@TempDir File dataDir) {
		this.dataDir = dataDir;
	}

	@Test
	void sevenIndexesShouldSucceed() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc,opsc,cosp,sopc,cpso,oscp");

		TripleStore tripleStore = assertDoesNotThrow(() -> new TripleStore(dataDir, config, null));
		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 1, true);
		tripleStore.commit();

		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			var it = tripleStore.getTriples(txn, 1, 2, 3, 1, true);
			assertThat(it.next()).isNotNull();
		}
	}
}
