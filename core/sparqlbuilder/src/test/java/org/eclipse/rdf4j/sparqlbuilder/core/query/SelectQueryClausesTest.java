package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

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
}
