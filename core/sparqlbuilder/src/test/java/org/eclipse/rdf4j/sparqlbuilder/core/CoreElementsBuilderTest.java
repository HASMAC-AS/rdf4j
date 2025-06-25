/*****************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *****************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for basic SPARQL builder elements.
 */
public class CoreElementsBuilderTest {

	@Test
	public void testBaseAndPrefix() {
		Base base = SparqlBuilder.base(Rdf.iri("http://example.org/"));
		assertEquals("BASE <http://example.org/>", base.getQueryString());

		Prefix prefix = SparqlBuilder.prefix("ex", Rdf.iri("http://example.org/"));
		assertEquals("PREFIX ex: <http://example.org/>", prefix.getQueryString());
	}

	@Test
	public void testFromClauses() {
		From def = SparqlBuilder.from(Rdf.iri("http://example.org/graph"));
		From named = SparqlBuilder.fromNamed(Rdf.iri("http://example.org/named"));
		Dataset dataset = SparqlBuilder.dataset(def, named);
		assertEquals(
				"FROM <http://example.org/graph>\nFROM NAMED <http://example.org/named>",
				dataset.getQueryString());
	}

	@Test
	public void testTriplesTemplateAndGraphTemplate() {
		Iri subj = Rdf.iri("http://example.org/s");
		Iri p1 = Rdf.iri("http://example.org/p1");
		Iri o1 = Rdf.iri("http://example.org/o1");
		Iri p2 = Rdf.iri("http://example.org/p2");
		Iri o2 = Rdf.iri("http://example.org/o2");

		TriplePattern tp1 = subj.has(p1, o1);
		TriplePattern tp2 = subj.has(p2, o2);

		TriplesTemplate tt = SparqlBuilder.triplesTemplate(tp1, tp2);
		assertEquals(
				"{ <http://example.org/s> <http://example.org/p1> <http://example.org/o1> .\n"
						+ "<http://example.org/s> <http://example.org/p2> <http://example.org/o2> . }",
				tt.getQueryString());

		GraphTemplate gt = SparqlBuilder.construct(tp1, tp2);
		assertEquals(
				"CONSTRUCT { <http://example.org/s> <http://example.org/p1> <http://example.org/o1> .\n"
						+ "<http://example.org/s> <http://example.org/p2> <http://example.org/o2> . }",
				gt.getQueryString());
	}

	@Test
	public void testGroupByAndOrderBy() {
		Variable a = SparqlBuilder.var("a");
		Variable b = SparqlBuilder.var("b");

		GroupBy gb = SparqlBuilder.groupBy(a, b);
		assertEquals("GROUP BY ?a ?b", gb.getQueryString());

		OrderBy ob = SparqlBuilder.orderBy(SparqlBuilder.desc(a), b);
		assertEquals("ORDER BY DESC( ?a ) ?b", ob.getQueryString());
	}
}
