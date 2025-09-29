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
package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicateObjectList;
import org.junit.jupiter.api.Test;

class TriplePatternBuilderTest {

	private static final String NS = "http://example.com/ns#";

	@Test
	void rdfTp01_basicTriplePattern() {
		Variable s = SparqlBuilder.var("s");
		Variable o = SparqlBuilder.var("o");

		SelectQuery query = Queries.SELECT(s, o)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "predicate"), o));

		assertSparqlEquals(
				"SELECT ?s ?o WHERE { ?s <http://example.com/ns#predicate> ?o . }",
				query.getQueryString());
	}

	@Test
	void rdfTp02_multipleObjectsForPredicate() {
		Variable s = SparqlBuilder.var("s");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "tag"), Rdf.literalOf("alpha"), Rdf.literalOf("beta")));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#tag> \"alpha\", \"beta\" . }",
				query.getQueryString());
	}

	@Test
	void rdfTp03_predicateObjectListChaining() {
		Variable s = SparqlBuilder.var("s");
		Variable friend = SparqlBuilder.var("friend");

		RdfPredicateObjectList names = Rdf.predicateObjectList(Rdf.iri(NS + "name"), Rdf.literalOf("Alice"));
		RdfPredicateObjectList knows = Rdf.predicateObjectList(Rdf.iri(NS + "knows"), friend)
				.and(Rdf.literalOf("Bob"));

		SelectQuery query = Queries.SELECT(s, friend)
				.where(GraphPatterns.tp(s, names, knows));

		assertSparqlEquals(
				"SELECT ?s ?friend WHERE { ?s <http://example.com/ns#name> \"Alice\" ; <http://example.com/ns#knows> ?friend, \"Bob\" . }",
				query.getQueryString());
	}

	@Test
	void rdfTp04_propertiesBlankNodeTriplePattern() {
		RdfBlankNode.PropertiesBlankNode address = Rdf.bNode(Rdf.iri(NS + "street"), Rdf.literalOf("Main"))
				.andHas(Rdf.iri(NS + "number"), Rdf.literalOf(42));

		SelectQuery query = Queries.SELECT()
				.where(GraphPatterns.tp(address));

		assertSparqlEquals(
				"SELECT * WHERE { [ <http://example.com/ns#street> \"Main\" ; <http://example.com/ns#number> 42 ] . }",
				query.getQueryString());
	}
}
