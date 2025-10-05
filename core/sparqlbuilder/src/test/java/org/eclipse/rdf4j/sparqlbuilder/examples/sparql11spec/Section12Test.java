/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.SubSelect;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

public class Section12Test extends BaseExamples {
	@Test
	public void example_12() {
		Prefix base = SparqlBuilder.prefix(iri("http://people.example/"));

// using this method of variable creation, as ?y and ?minName will be
// used in both the outer and inner queries
		Variable y = SparqlBuilder.var("y"), minName = SparqlBuilder.var("minName"), name = SparqlBuilder.var("name");

		SubSelect sub = GraphPatterns.select();
		sub.select(y, Expressions.min(name).as(minName)).where(y.has(base.iri("name"), name)).groupBy(y);

		query.prefix(base, base) // SparqlBuilder even fixes typos for you ;)
				.select(y, minName)
				.where(base.iri("alice").has(base.iri("knows"), y), sub);
		assertThat(query.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://people.example/>\n"
						+ "SELECT ?y ?minName\n"
						+ "WHERE {\n"
						+ "  :alice :knows ?y .\n"
						+ "  {\n"
						+ "    SELECT ?y (MIN(?name) AS ?minName)\n"
						+ "    WHERE {\n"
						+ "      ?y :name ?name .\n"
						+ "    } GROUP BY ?y\n"
						+ "  }\n"
						+ "}"));
	}

	@Test
	public void example_12_values_clause() {
		Prefix base = SparqlBuilder.prefix(iri("http://people.example/"));
		Variable y = SparqlBuilder.var("y");
		Variable name = SparqlBuilder.var("name");

		SubSelect sub = GraphPatterns.select();
		sub.select(y)
				.where(y.has(base.iri("name"), name))
				.values(v -> v.variables(y).value(base.iri("alice")));

		query.prefix(base)
				.select(y)
				.where(base.iri("alice").has(base.iri("knows"), y), sub);

		assertThat(query.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://people.example/>\n"
						+ "SELECT ?y\n"
						+ "WHERE {\n"
						+ "  :alice :knows ?y .\n"
						+ "  {\n"
						+ "    SELECT ?y\n"
						+ "    WHERE {\n"
						+ "      ?y :name ?name .\n"
						+ "    }\n"
						+ "    VALUES ?y { :alice }\n"
						+ "  }\n"
						+ "}"));
	}

	@Test
	public void example_12_inline_values_in_where_clause() {
		Prefix base = SparqlBuilder.prefix(iri("http://people.example/"));
		Variable y = SparqlBuilder.var("y");
		Variable name = SparqlBuilder.var("name");

		SubSelect sub = GraphPatterns.select();
		sub.select(y)
				.where(y.has(base.iri("name"), name)
						.values(v -> v.variables(name).value(Rdf.literalOf("Alice"))));

		assertThat(sub.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"{\n"
						+ "  SELECT ?y\n"
						+ "  WHERE {\n"
						+ "    ?y :name ?name .\n"
						+ "    VALUES ?name { \"Alice\" }\n"
						+ "  }\n"
						+ "}"));
	}

	@Test
	public void example_12_values_clause_without_where_patterns() {
		Prefix base = SparqlBuilder.prefix(iri("http://people.example/"));
		Variable y = SparqlBuilder.var("y");

		SubSelect sub = GraphPatterns.select();
		sub.select(y).values(v -> v.variables(y).value(base.iri("alice")));

		assertThat(sub.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"{\n"
						+ "  SELECT ?y\n"
						+ "  WHERE { }\n"
						+ "  VALUES ?y { :alice }\n"
						+ "}"));
	}
}
