/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbQueryExplanationTest {

	@TempDir
	File dataDir;

	private SailRepository repository;

	@BeforeEach
	void setUp() throws SailException {
		Sail sail = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc"));
		sail.init();
		repository = new SailRepository(sail);
		repository.init();

		try (RepositoryConnection connection = repository.getConnection()) {
			IRI alice = Values.iri("http://example.com/alice");
			IRI knows = Values.iri("http://example.com/knows");
			IRI bob = Values.iri("http://example.com/bob");
			IRI carol = Values.iri("http://example.com/carol");

			connection.add(alice, knows, bob);
			connection.add(bob, knows, carol);
			connection.add(carol, knows, alice);
		}
	}

	@AfterEach
	void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Test
	void executedExplanationIncludesIndexName() {
		try (RepositoryConnection connection = repository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL,
					"SELECT ?o WHERE { <http://example.com/alice> <http://example.com/knows> ?o }");
			Explanation explanation = query.explain(Explanation.Level.Executed);
			String plan = explanation.toString();

			assertThat(plan).contains("[index: spoc]");
			assertThat(plan).doesNotContain("recommended indexes");
		}
	}

	@Test
	void executedExplanationSuggestsIndexesWhenSequentialScan() {
		try (RepositoryConnection connection = repository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL,
					"SELECT ?s WHERE { ?s ?p <http://example.com/bob> } LIMIT 1");
			Explanation explanation = query.explain(Explanation.Level.Executed);
			String plan = explanation.toString();

			assertThat(plan).contains("recommended indexes: o");
		}
	}
}
