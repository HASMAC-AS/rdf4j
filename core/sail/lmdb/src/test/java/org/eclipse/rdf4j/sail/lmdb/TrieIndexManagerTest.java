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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrieIndexManagerTest {

	@TempDir
	File dataDir;

	private LmdbStore store;

	@BeforeEach
	void setUp() {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc").setMaintainTrieIndexes(true);
		store = new LmdbStore(dataDir, config);
		store.init();
	}

	@AfterEach
	void tearDown() {
		if (store != null) {
			store.shutDown();
		}
	}

	@Test
	void trieReflectsWrites() throws Exception {
		IRI painter = store.getValueFactory().createIRI("urn:painter");
		IRI painting = store.getValueFactory().createIRI("urn:painting");

		try (NotifyingSailConnection cxn = store.getConnection()) {
			cxn.begin();
			cxn.addStatement(painter, RDF.TYPE, RDFS.CLASS);
			cxn.addStatement(painting, RDF.TYPE, RDFS.CLASS);
			cxn.commit();
		}

		LmdbSailStore backingStore = store.getBackingStore();
		TrieIndexManager manager = backingStore.getTrieIndexManager();
		TxnManager txnManager = backingStore.getTxnManager();

		long painterId = backingStore.getValueStore().getId(painter);
		long typeId = backingStore.getValueStore().getId(RDF.TYPE);
		long classId = backingStore.getValueStore().getId(RDFS.CLASS);

		List<Long> predicates = new ArrayList<>();
		List<Long> objects = new ArrayList<>();

		txnManager.doWith((stack, txn) -> {
			try (TrieLevelCursor l1 = manager.openCursor("spoc", 1, true, txn)) {
				l1.openPrefix(painterId);
				while (!l1.atEnd()) {
					predicates.add(l1.key());
					if (!l1.next()) {
						break;
					}
				}
			}

			try (TrieLevelCursor l2 = manager.openCursor("spoc", 2, true, txn)) {
				l2.openPrefix(painterId, typeId);
				while (!l2.atEnd()) {
					objects.add(l2.key());
					if (!l2.next()) {
						break;
					}
				}
			}
			return null;
		});

		assertThat(predicates).containsExactly(typeId);
		assertThat(objects).containsExactly(classId);
	}
}
