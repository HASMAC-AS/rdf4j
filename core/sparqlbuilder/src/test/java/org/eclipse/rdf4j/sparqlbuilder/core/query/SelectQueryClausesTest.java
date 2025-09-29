package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
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
}
