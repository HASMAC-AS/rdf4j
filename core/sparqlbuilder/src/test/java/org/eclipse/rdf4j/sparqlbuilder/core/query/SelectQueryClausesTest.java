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
package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

class SelectQueryClausesTest {

	@Test
	void qSel01_selectAllWithSingleTriplePattern() {
		Variable s = SparqlBuilder.var("s");
		Variable p = SparqlBuilder.var("p");
		Variable o = SparqlBuilder.var("o");

		TriplePattern pattern = GraphPatterns.tp(s, p, o);

		SelectQuery query = Queries.SELECT().where(pattern);

		assertSparqlEquals("SELECT * WHERE { ?s ?p ?o . }", query.getQueryString());
	}

	@Test
	void qSel02_selectExplicitProjectionOrder() {
		Variable s = SparqlBuilder.var("s");
		Variable p = SparqlBuilder.var("p");
		Variable o = SparqlBuilder.var("o");

		SelectQuery query = Queries.SELECT(s, p, o)
				.where(GraphPatterns.tp(Rdf.iri("http://example.com/ns#subject"), p, o));

		assertSparqlEquals(
				"SELECT ?s ?p ?o WHERE { <http://example.com/ns#subject> ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qSel03_projectionWithAliasExpression() {
		Variable s = SparqlBuilder.var("s");
		Variable o = SparqlBuilder.var("o");
		Variable label = SparqlBuilder.var("label");

		SelectQuery query = Queries.SELECT(SparqlBuilder.as(Expressions.str(o), label), o)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), o));

		assertSparqlEquals(
				"SELECT ( STR( ?o ) AS ?label ) ?o WHERE { ?s <http://example.com/ns#name> ?o . }",
				query.getQueryString());
	}

	@Test
	void qSel04_selectDistinctAllProjection() {
		SelectQuery query = Queries.SELECT()
				.distinct()
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o")));

		assertSparqlEquals(
				"SELECT DISTINCT * WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qPfx01_singlePrefixDeclaration() {
		SelectQuery query = Queries.SELECT()
				.prefix(SparqlBuilder.prefix("ex", Rdf.iri("http://example.com/ns#")))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o")));

		assertSparqlEquals(
				"PREFIX ex: <http://example.com/ns#> SELECT * WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qPfx02_multiplePrefixesNormalizedOrder() {
		SelectQuery query = Queries.SELECT()
				.prefix(SparqlBuilder.prefix("foaf", Rdf.iri("http://xmlns.com/foaf/0.1/")))
				.prefix(SparqlBuilder.prefix("ex", Rdf.iri("http://example.com/ns#")))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o")));

		assertSparqlEquals(
				"PREFIX ex: <http://example.com/ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT * WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qPfx03_baseDeclaration() {
		SelectQuery query = Queries.SELECT()
				.base(Rdf.iri("http://example.com/base/"))
				.prefix(SparqlBuilder.prefix("ex", Rdf.iri("http://example.com/ns#")))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o")));

		assertSparqlEquals(
				"BASE <http://example.com/base/> PREFIX ex: <http://example.com/ns#> SELECT * WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qDs01_singleFromDefaultGraph() {
		SelectQuery query = Queries.SELECT()
				.from(SparqlBuilder.from(Rdf.iri("http://example.com/graph")))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o")));

		assertSparqlEquals(
				"SELECT * FROM <http://example.com/graph> WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qDs02_singleFromNamedGraph() {
		SelectQuery query = Queries.SELECT()
				.from(SparqlBuilder.fromNamed(Rdf.iri("http://example.com/namedGraph")))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o")));

		assertSparqlEquals(
				"SELECT * FROM NAMED <http://example.com/namedGraph> WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qDs03_multipleFromClauses() {
		SelectQuery query = Queries.SELECT()
				.from(
						SparqlBuilder.from(Rdf.iri("http://example.com/defaultOne")),
						SparqlBuilder.fromNamed(Rdf.iri("http://example.com/namedOne")))
				.from(SparqlBuilder.from(Rdf.iri("http://example.com/defaultTwo")))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o")));

		assertSparqlEquals(
				"SELECT * FROM <http://example.com/defaultOne> FROM NAMED <http://example.com/namedOne> FROM <http://example.com/defaultTwo> WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qDs04_datasetViaBuilder() {
		SelectQuery query = Queries.SELECT()
				.from(SparqlBuilder.dataset(
						SparqlBuilder.from(Rdf.iri("http://example.com/default")),
						SparqlBuilder.fromNamed(Rdf.iri("http://example.com/named"))))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o")));

		assertSparqlEquals(
				"SELECT * FROM <http://example.com/default> FROM NAMED <http://example.com/named> WHERE { ?s ?p ?o . }",
				query.getQueryString());
	}

	@Test
	void qWh01_singleTriplePatternWhereClause() {
		Variable s = SparqlBuilder.var("s");
		Variable p = SparqlBuilder.var("p");
		Variable o = SparqlBuilder.var("o");

		SelectQuery query = Queries.SELECT()
				.where(GraphPatterns.tp(s, p, o));

		assertSparqlEquals("SELECT * WHERE { ?s ?p ?o . }", query.getQueryString());
	}

	@Test
	void qWh02_multiplePatternsCombined() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");
		Variable age = SparqlBuilder.var("age");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name))
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#age"), age));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#name> ?name . ?s <http://example.com/ns#age> ?age . }",
				query.getQueryString());
	}

	@Test
	void qWh03_prebuiltQueryPatternSuppliedToWhere() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");
		Variable age = SparqlBuilder.var("age");

		GraphPattern combined = GraphPatterns.and(
				GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name),
				GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#age"), age));

		SelectQuery query = Queries.SELECT(s).where(combined);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#name> ?name . ?s <http://example.com/ns#age> ?age . }",
				query.getQueryString());
	}

	@Test
	void qGrp01_groupBySingleVariable() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name))
				.groupBy(s);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#name> ?name . } GROUP BY ?s",
				query.getQueryString());
	}

	@Test
	void qGrp02_groupByExpression() {
		Variable name = SparqlBuilder.var("name");
		Variable count = SparqlBuilder.var("count");

		SelectQuery query = Queries.SELECT(SparqlBuilder.as(Expressions.count(name), count))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri("http://example.com/ns#name"), name))
				.groupBy(Expressions.str(name));

		assertSparqlEquals(
				"SELECT ( COUNT( ?name ) AS ?count ) WHERE { ?s <http://example.com/ns#name> ?name . } GROUP BY STR( ?name )",
				query.getQueryString());
	}

	@Test
	void qHav01_havingWithCountGreaterThanOne() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name))
				.groupBy(s)
				.having(Expressions.gt(Expressions.count(name), Rdf.literalOf(1)));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#name> ?name . } GROUP BY ?s HAVING ( COUNT( ?name ) > 1 )",
				query.getQueryString());
	}

	@Test
	void qOrd01_orderByAscendingVariable() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");

		SelectQuery query = Queries.SELECT(s, name)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name))
				.orderBy(name);

		assertSparqlEquals(
				"SELECT ?s ?name WHERE { ?s <http://example.com/ns#name> ?name . } ORDER BY ?name",
				query.getQueryString());
	}

	@Test
	void qOrd02_orderByDescendingExpression() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");

		SelectQuery query = Queries.SELECT(s, name)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name))
				.orderBy(Expressions.str(name).desc());

		assertSparqlEquals(
				"SELECT ?s ?name WHERE { ?s <http://example.com/ns#name> ?name . } ORDER BY DESC( STR( ?name ) )",
				query.getQueryString());
	}

	@Test
	void qOrd03_multipleOrderKeys() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");
		Variable age = SparqlBuilder.var("age");

		SelectQuery query = Queries.SELECT(s, name, age)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name))
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#age"), age))
				.orderBy(name, age.desc());

		assertSparqlEquals(
				"SELECT ?s ?name ?age WHERE { ?s <http://example.com/ns#name> ?name . ?s <http://example.com/ns#age> ?age . } ORDER BY ?name DESC( ?age )",
				query.getQueryString());
	}

	@Test
	void qLim01_limitClause() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");

		SelectQuery query = Queries.SELECT(s, name)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name))
				.limit(5);

		assertSparqlEquals(
				"SELECT ?s ?name WHERE { ?s <http://example.com/ns#name> ?name . } LIMIT 5",
				query.getQueryString());
	}

	@Test
	void qOff01_offsetClause() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");

		SelectQuery query = Queries.SELECT(s, name)
				.where(GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#name"), name))
				.offset(10);

		assertSparqlEquals(
				"SELECT ?s ?name WHERE { ?s <http://example.com/ns#name> ?name . } OFFSET 10",
				query.getQueryString());
	}
}
