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
package org.eclipse.rdf4j.sparqlbuilder.propertypath;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import java.util.function.Consumer;

import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.EmptyPropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

class PropertyPathBuilderTest {

	private static final String NS = "http://example.com/ns#";

	private static Variable var(String name) {
		return SparqlBuilder.var(name);
	}

	private static SelectQuery selectWithPath(Consumer<EmptyPropertyPathBuilder> propertyPath,
			Variable subject, Variable object) {
		return Queries.SELECT(subject)
				.where(GraphPatterns.tp(subject, propertyPath, object));
	}

	@Test
	void path01_sequence() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.pred(Rdf.iri(NS + "parent"))
				.then(Rdf.iri(NS + "child")), s, o);

		assertSparqlEquals("SELECT ?s WHERE { ?s <http://example.com/ns#parent> / <http://example.com/ns#child> ?o . }",
				query.getQueryString());
	}

	@Test
	void path02_alternative() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.pred(Rdf.iri(NS + "name"))
				.or(Rdf.iri(NS + "nickname")), s, o);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s ( <http://example.com/ns#name> | <http://example.com/ns#nickname> ) ?o . }",
				query.getQueryString());
	}

	@Test
	void path03_zeroOrMore() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.pred(Rdf.iri(NS + "ancestor")).zeroOrMore(), s, o);

		assertSparqlEquals("SELECT ?s WHERE { ?s <http://example.com/ns#ancestor>* ?o . }", query.getQueryString());
	}

	@Test
	void path04_oneOrMore() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.pred(Rdf.iri(NS + "ancestor")).oneOrMore(), s, o);

		assertSparqlEquals("SELECT ?s WHERE { ?s <http://example.com/ns#ancestor>+ ?o . }", query.getQueryString());
	}

	@Test
	void path05_zeroOrOne() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.pred(Rdf.iri(NS + "spouse")).zeroOrOne(), s, o);

		assertSparqlEquals("SELECT ?s WHERE { ?s <http://example.com/ns#spouse>? ?o . }", query.getQueryString());
	}

	@Test
	void path06_inverse() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.pred(Rdf.iri(NS + "child")).inv(), s, o);

		assertSparqlEquals("SELECT ?s WHERE { ?s ^( <http://example.com/ns#child> ) ?o . }", query.getQueryString());
	}

	@Test
	void path07_nestedSubtreeSequence() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.pred(Rdf.iri(NS + "parent"))
				.then(sub -> sub.pred(Rdf.iri(NS + "child")).or(Rdf.iri(NS + "guardian")))
				.group(), s, o);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s ( <http://example.com/ns#parent> / ( <http://example.com/ns#child> | <http://example.com/ns#guardian> ) ) ?o . }",
				query.getQueryString());
	}

	@Test
	void path08_negatedPropertySet() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.negProp()
				.pred(Rdf.iri(NS + "blocked"))
				.invPred(Rdf.iri(NS + "bannedBy")), s, o);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s !( <http://example.com/ns#blocked> | ^<http://example.com/ns#bannedBy> ) ?o . }",
				query.getQueryString());
	}

	@Test
	void path09_complexGroupedExpression() {
		Variable s = var("s");
		Variable o = var("o");

		SelectQuery query = selectWithPath(builder -> builder.pred(Rdf.iri(NS + "ancestor"))
				.then(sub -> sub.pred(Rdf.iri(NS + "parent"))
						.or(sub2 -> sub2.pred(Rdf.iri(NS + "sibling")).inv()))
				.group()
				.oneOrMore(), s, o);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s ( <http://example.com/ns#ancestor> / ( <http://example.com/ns#parent> | ^( <http://example.com/ns#sibling> ) ) )+ ?o . }",
				query.getQueryString());
	}
}
