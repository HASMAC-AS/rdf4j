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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Values.Builder;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

class ValuesClauseTest {

	private static final IRI NAME = Values.iri("http://example.com/ns#name");
	private static final IRI AGE = Values.iri("http://example.com/ns#age");

	@Test
	void val01_queryLevelSingleVariableValues() {
		Variable name = SparqlBuilder.var("name");

		SelectQuery query = Queries.SELECT(name)
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri(NAME), name))
				.values(builder -> builder.variables(name).values(Rdf.literalOf("Alice"), Rdf.literalOf("Bob")));

		assertSparqlEquals(
				"SELECT ?name WHERE { ?s <http://example.com/ns#name> ?name . } VALUES ?name { \"Alice\" \"Bob\" }",
				query.getQueryString());
	}

	@Test
	void val02_multiVariableRowWiseValues() {
		Variable name = SparqlBuilder.var("name");
		Variable age = SparqlBuilder.var("age");

		SelectQuery query = Queries.SELECT(name, age)
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri(NAME), name))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri(AGE), age))
				.values(builder -> builder.variables(name, age)
						.values(Rdf.literalOf("Alice"), Rdf.literalOf(30))
						.values(Rdf.literalOf("Bob"), Rdf.literalOf(40)));

		assertSparqlEquals(
				"SELECT ?name ?age WHERE { ?s <http://example.com/ns#name> ?name . ?s <http://example.com/ns#age> ?age . } VALUES ( ?name ?age ) { ( \"Alice\" 30 ) ( \"Bob\" 40 ) }",
				query.getQueryString());
	}

	@Test
	void val03_mixedValueTypes() {
		Variable person = SparqlBuilder.var("person");
		Variable literal = SparqlBuilder.var("literal");

		SelectQuery query = Queries.SELECT(person, literal)
				.where(GraphPatterns.tp(person, Rdf.iri(NAME), literal))
				.values(builder -> builder.variables(person, literal)
						.values(Rdf.iri("http://example.com/ns#p1"), Rdf.literalOfLanguage("Alice", "en"))
						.values(Rdf.iri("http://example.com/ns#p2"), Rdf.literalOf(25))
						.values(null, null));

		assertSparqlEquals(
				"SELECT ?person ?literal WHERE { ?person <http://example.com/ns#name> ?literal . } VALUES ( ?person ?literal ) { ( <http://example.com/ns#p1> \"Alice\"@en ) ( <http://example.com/ns#p2> 25 ) ( UNDEF UNDEF ) }",
				query.getQueryString());
	}

	@Test
	void val04_valuesInsideGraphPattern() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NAME), name)
						.values(builder -> builder.variables(name).values(Rdf.literalOf("Alice"))));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#name> ?name . VALUES ?name { \"Alice\" } }",
				query.getQueryString());
	}

	@Test
	void val05_valuesArityMismatchThrows() {
		Variable name = SparqlBuilder.var("name");
		Variable age = SparqlBuilder.var("age");

		SelectQuery query = Queries.SELECT(name, age)
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri(NAME), name))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri(AGE), age));

		assertThatThrownBy(() -> query.values(builder -> builder.variables(name, age).values(Rdf.literalOf("Alice"))))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void val06_valuesBuilderRequiresRowsBeforeBuild() {
		Builder builder = (Builder) org.eclipse.rdf4j.sparqlbuilder.constraint.Values.builder();

		builder.variables(SparqlBuilder.var("x"));

		assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
	}
}
