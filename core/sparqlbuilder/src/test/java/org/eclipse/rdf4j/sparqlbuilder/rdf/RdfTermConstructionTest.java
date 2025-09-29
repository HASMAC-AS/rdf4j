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
package org.eclipse.rdf4j.sparqlbuilder.rdf;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode;
import org.junit.jupiter.api.Test;

class RdfTermConstructionTest {

	private static final String NS = "http://example.com/ns#";

	private static Variable var(String name) {
		return SparqlBuilder.var(name);
	}

	@Test
	void rdfVal01_inlineIriRendering() {
		Variable s = var("s");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "predicate"), Rdf.iri(NS + "object")));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#predicate> <http://example.com/ns#object> . }",
				query.getQueryString());
	}

	@Test
	void rdfVal02_plainStringLiteral() {
		Variable s = var("s");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "label"), Rdf.literalOf("plain value")));

		assertSparqlEquals("SELECT ?s WHERE { ?s <http://example.com/ns#label> \"plain value\" . }",
				query.getQueryString());
	}

	@Test
	void rdfVal03_languageTaggedLiteral() {
		Variable s = var("s");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "label"), Rdf.literalOfLanguage("name", "en")));

		assertSparqlEquals("SELECT ?s WHERE { ?s <http://example.com/ns#label> \"name\"@en . }",
				query.getQueryString());
	}

	@Test
	void rdfVal04_typedLiteral() {
		Variable s = var("s");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "created"),
						Rdf.literalOfType("2024-01-01", XSD.DATE)));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#created> \"2024-01-01\"^^<http://www.w3.org/2001/XMLSchema#date> . }",
				query.getQueryString());
	}

	@Test
	void rdfVal05_booleanAndNumericLiterals() {
		Variable s = var("s");

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "active"), Rdf.literalOf(true))
						.andHas(Rdf.iri(NS + "score"), Rdf.literalOf(42)));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#active> true ; <http://example.com/ns#score> 42 . }",
				query.getQueryString());
	}

	@Test
	void rdfVal06_anonymousBlankNodeSubject() {
		Variable o = var("o");

		SelectQuery query = Queries.SELECT(o)
				.where(GraphPatterns.tp(Rdf.bNode(), Rdf.iri(NS + "knows"), o));

		assertSparqlEquals("SELECT ?o WHERE { [] <http://example.com/ns#knows> ?o . }", query.getQueryString());
	}

	@Test
	void rdfVal07_labeledBlankNodeSubject() {
		Variable o = var("o");

		SelectQuery query = Queries.SELECT(o)
				.where(GraphPatterns.tp(Rdf.bNode("person"), Rdf.iri(NS + "knows"), o));

		assertSparqlEquals("SELECT ?o WHERE { _:person <http://example.com/ns#knows> ?o . }", query.getQueryString());
	}

	@Test
	void rdfVal08_propertiesBlankNodeExpansion() {
		Variable s = var("s");

		RdfBlankNode.PropertiesBlankNode profile = Rdf.bNode(Rdf.iri(NS + "name"), Rdf.literalOf("Alice"))
				.andHas(Rdf.iri(NS + "knows"), Rdf.bNode("friend"))
				.andHas(builder -> builder.pred(Rdf.iri(NS + "parent")).zeroOrMore(), Rdf.literalOf("Bob"));

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "profile"), profile));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#profile> [ <http://example.com/ns#name> \"Alice\" ; <http://example.com/ns#knows> _:friend ; <http://example.com/ns#parent>* \"Bob\" ] . }",
				query.getQueryString());
	}

	@Test
	void rdfVal09_stringEscapingWithNewlinesAndQuotes() {
		Variable s = var("s");

		String tricky = "He said \"Hello\\nWorld\"";

		SelectQuery query = Queries.SELECT(s)
				.where(GraphPatterns.tp(s, Rdf.iri(NS + "note"), Rdf.literalOf(tricky)));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://example.com/ns#note> \"He said \\\"Hello\\\\nWorld\\\"\" . }",
				query.getQueryString());
	}
}
