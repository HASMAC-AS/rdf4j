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
package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.sparqlbuilder.core.GraphTemplate;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

class ConstructQueryTest {

	@Test
	void qCon01_basicConstructQuery() {
		TriplePattern triple = GraphPatterns.tp(
				SparqlBuilder.var("s"),
				SparqlBuilder.var("p"),
				SparqlBuilder.var("o"));

		ConstructQuery query = Queries.CONSTRUCT()
				.construct(triple)
				.where(triple);

		assertSparqlEquals("CONSTRUCT { ?s ?p ?o . } WHERE { ?s ?p ?o . }", query.getQueryString());
	}

	@Test
	void qCon02_constructWithExplicitTemplate() {
		GraphTemplate template = SparqlBuilder.construct(
				GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri("http://example.com/ns#name"), SparqlBuilder.var("n")),
				GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri("http://example.com/ns#age"), SparqlBuilder.var("a")));

		TriplePattern wherePattern = GraphPatterns.tp(
				SparqlBuilder.var("s"),
				Rdf.iri("http://example.com/ns#name"),
				SparqlBuilder.var("n"));

		ConstructQuery query = Queries.CONSTRUCT()
				.construct(template)
				.where(wherePattern);

		assertSparqlEquals(
				"CONSTRUCT { ?s <http://example.com/ns#name> ?n . ?s <http://example.com/ns#age> ?a . } WHERE { ?s <http://example.com/ns#name> ?n . }",
				query.getQueryString());
	}

	@Test
	void qCon03_constructWithDatasetClauses() {
		TriplePattern triple = GraphPatterns.tp(
				SparqlBuilder.var("s"),
				SparqlBuilder.var("p"),
				SparqlBuilder.var("o"));

		ConstructQuery query = Queries.CONSTRUCT()
				.from(SparqlBuilder.from(Rdf.iri("http://example.com/default")))
				.from(SparqlBuilder.fromNamed(Rdf.iri("http://example.com/named")))
				.construct(triple)
				.where(triple);

		assertSparqlEquals(
				"CONSTRUCT { ?s ?p ?o . } FROM <http://example.com/default> FROM NAMED <http://example.com/named> WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}
}
