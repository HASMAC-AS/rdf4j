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
package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

class GraphPatternCombinatorsTest {

	private static final String NS = "http://example.com/ns#";

	private static Variable var(String name) {
		return SparqlBuilder.var(name);
	}

	@Test
	void gpOpt01_optionalWrapsPattern() {
		Variable s = var("s");
		Variable name = var("name");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "name"), name).optional());

		assertSparqlEquals("SELECT ?s WHERE { OPTIONAL { ?s <http://example.com/ns#name> ?name . } }",
				query.getQueryString());
	}

	@Test
	void gpOpt02_optionalToggleOffRemovesWrapper() {
		Variable s = var("s");
		Variable p = var("p");
		Variable o = var("o");

		GraphPattern pattern = GraphPatterns.tp(s, p, o).optional();
		pattern.optional(false);

		SelectQuery query = Queries.SELECT(s).where(pattern);

		assertSparqlEquals("SELECT ?s WHERE { ?s ?p ?o . }", query.getQueryString());
	}

	@Test
	void gpUni01_unionOfTwoBranches() {
		Variable s = var("s");

		GraphPattern union = GraphPatterns.union(
				GraphPatterns.tp(s, Rdf.iri(NS + "name"), Rdf.literalOf("Alice")),
				GraphPatterns.tp(s, Rdf.iri(NS + "name"), Rdf.literalOf("Bob")));

		SelectQuery query = Queries.SELECT(s).where(union);

		assertSparqlEquals(
				"SELECT ?s WHERE { { ?s <http://example.com/ns#name> \"Alice\" . } UNION { ?s <http://example.com/ns#name> \"Bob\" . } }",
				query.getQueryString());
	}

	@Test
	void gpMin01_minusPattern() {
		Variable s = var("s");
		Variable o = var("o");

		GraphPattern pattern = GraphPatterns.tp(s, Rdf.iri(NS + "name"), o)
				.minus(GraphPatterns.tp(s, Rdf.iri(NS + "deprecated"), o));

		SelectQuery query = Queries.SELECT(s).where(pattern);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#name> ?o . MINUS { ?s <http://example.com/ns#deprecated> ?o . } }",
				query.getQueryString());
	}

	@Test
	void gpFlt01_filterOnExpression() {
		Variable s = var("s");
		Variable age = var("age");

		GraphPattern pattern = GraphPatterns.tp(s, Rdf.iri(NS + "age"), age)
				.filter(Expressions.gt(age, Rdf.literalOf(18)));

		SelectQuery query = Queries.SELECT(s).where(pattern);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#age> ?age . FILTER ( ?age > 18 ) }",
				query.getQueryString());
	}

	@Test
	void gpExi01_filterExists() {
		Variable s = var("s");
		Variable name = var("name");

		GraphPattern pattern = GraphPatterns.tp(s, Rdf.iri(NS + "name"), name)
				.filterExists(GraphPatterns.tp(name, Rdf.iri(NS + "given"), Rdf.literalOf("A")));

		SelectQuery query = Queries.SELECT(s).where(pattern);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#name> ?name . FILTER EXISTS { ?name <http://example.com/ns#given> \"A\" . } }",
				query.getQueryString());
	}

	@Test
	void gpNex01_filterNotExists() {
		Variable s = var("s");
		Variable name = var("name");

		GraphPattern pattern = GraphPatterns.tp(s, Rdf.iri(NS + "name"), name)
				.filterExists(false, GraphPatterns.tp(name, Rdf.iri(NS + "deprecated"), Rdf.literalOf(true)));

		SelectQuery query = Queries.SELECT(s).where(pattern);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#name> ?name . FILTER NOT EXISTS { ?name <http://example.com/ns#deprecated> true . } }",
				query.getQueryString());
	}

	@Test
	void gpGra01_namedGraphWrapper() {
		Variable s = var("s");
		Variable p = var("p");
		Variable o = var("o");

		GraphPattern pattern = GraphPatterns.tp(s, p, o).from(Rdf.iri(Values.iri(NS + "graph")));

		SelectQuery query = Queries.SELECT(s).where(pattern);

		assertSparqlEquals(
				"SELECT ?s WHERE { GRAPH <http://example.com/ns#graph> { ?s ?p ?o . } }",
				query.getQueryString());
	}
}
