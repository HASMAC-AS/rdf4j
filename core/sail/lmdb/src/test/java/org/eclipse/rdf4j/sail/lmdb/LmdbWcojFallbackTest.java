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

import java.nio.file.Path;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdJoinIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbWcojFallbackTest {

	private static final String NS = "http://example.com/";

	@Test
	void acyclicBgpUsesIdJoinsWhenWcojEnabled(@TempDir Path tempDir) throws Exception {
		String previous = System.getProperty("rdf4j.lmdb.useWcojForBgp");
		System.setProperty("rdf4j.lmdb.useWcojForBgp", "true");

		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
			conn.add(bob, likes, pizza);
		}

		String query = "SELECT ?person ?item\n" +
				"WHERE {\n" +
				"  ?person <http://example.com/knows> ?other .\n" +
				"  ?person <http://example.com/likes> ?item .\n" +
				"}";

		try (RepositoryConnection conn = repository.getConnection()) {
			TupleExpr expr = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null).getTupleExpr();

			// run the query to ensure evaluation succeeds and plan is populated
			TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tq.evaluate().close();

			// optimize and inspect the plan
			((org.eclipse.rdf4j.repository.sail.SailRepositoryConnection) conn).getSailConnection()
					.explain(Explanation.Level.Optimized, expr, null, EmptyBindingSet.getInstance(), true, 0);
			TupleExpr joinExpr = unwrap(expr);
			assertThat(joinExpr).isInstanceOf(Join.class);
			Join join = (Join) joinExpr;
			assertThat(join.getAlgorithmName()).isEqualTo(LmdbIdJoinIterator.class.getSimpleName());
		} finally {
			repository.shutDown();
			if (previous == null) {
				System.clearProperty("rdf4j.lmdb.useWcojForBgp");
			} else {
				System.setProperty("rdf4j.lmdb.useWcojForBgp", previous);
			}
		}
	}

	private TupleExpr unwrap(TupleExpr expr) {
		if (expr instanceof org.eclipse.rdf4j.query.algebra.Projection) {
			return unwrap(((org.eclipse.rdf4j.query.algebra.Projection) expr).getArg());
		}
		if (expr instanceof org.eclipse.rdf4j.query.algebra.QueryRoot) {
			return unwrap(((org.eclipse.rdf4j.query.algebra.QueryRoot) expr).getArg());
		}
		return expr;
	}
}
