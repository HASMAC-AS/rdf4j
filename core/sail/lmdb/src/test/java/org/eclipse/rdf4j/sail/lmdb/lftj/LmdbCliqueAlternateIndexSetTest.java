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
package org.eclipse.rdf4j.sail.lmdb.lftj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LmdbCliqueAlternateIndexSetTest {

	private SailRepository repository;
	private RepositoryConnection connection;
	private File dataDir;

	@Test
	void compareIndexSets() throws Exception {
		long defaultCount = runCliqueWithIndexes("spoc,posc,opsc,cosp,sopc,cpso,oscp");
		assertThat(defaultCount).isEqualTo(117600);

		assertThatThrownBy(() -> runCliqueWithIndexes("opsc"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Missing required index");
	}

	private long runCliqueWithIndexes(String indexSet) throws IOException {
		setupRepository(indexSet);

		String query = String.join("\n",
				"SELECT ?n0 ?n1 ?n2 {",
				"  ?n0 <" + FOAF.KNOWS.stringValue() + "> ?n1 .",
				"  ?n1 <" + FOAF.KNOWS.stringValue() + "> ?n2 .",
				"  ?n2 <" + FOAF.KNOWS.stringValue() + "> ?n0 .",
				"}");

		long count = 0;
		try (var result = connection.prepareTupleQuery(query).evaluate()) {
			while (result.hasNext()) {
				result.next();
				count++;
			}
		}

		tearDown();
		return count;
	}

	private void setupRepository(String indexSet) throws IOException {
		dataDir = Files.createTempDirectory("lmdb-clique-alt").toFile();
		LmdbStore store = new LmdbStore(dataDir, new LmdbStoreConfig(indexSet));
		repository = new SailRepository(store);
		repository.init();
		connection = repository.getConnection();
		loadCompleteGraph(50);
	}

	private void loadCompleteGraph(int nodeCount) {
		ValueFactory vf = connection.getValueFactory();
		List<IRI> nodes = new ArrayList<>(nodeCount);
		for (int i = 0; i < nodeCount; i++) {
			nodes.add(vf.createIRI("urn:node:" + i));
		}

		connection.begin();
		for (int i = 0; i < nodeCount; i++) {
			IRI subject = nodes.get(i);
			for (int j = 0; j < nodeCount; j++) {
				if (i == j) {
					continue;
				}
				connection.add(subject, FOAF.KNOWS, nodes.get(j));
			}
		}
		connection.commit();
	}

	@AfterEach
	void tearDown() throws IOException {
		if (connection != null) {
			connection.close();
		}
		if (repository != null) {
			repository.shutDown();
		}
		if (dataDir != null) {
			org.apache.commons.io.FileUtils.deleteDirectory(dataDir);
		}
	}
}
