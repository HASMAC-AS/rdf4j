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
package org.eclipse.rdf4j.sparqlbuilder.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode;
import org.junit.jupiter.api.Test;

class GeneratorHelpersTest {

	private static final String NS = "http://example.com/ns#";

	@Test
	void gen01_varHelperProducesUniqueVariables() {
		SelectQuery query = Queries.SELECT();
		Variable s = query.var();
		Variable o = query.var();

		query.where(GraphPatterns.tp(s, Rdf.iri(NS + "link"), o));

		Set<String> names = new LinkedHashSet<>();
		names.add(s.getVarName());
		names.add(o.getVarName());

		assertThat(names).containsExactlyInAnyOrder("x0", "x1");
		assertSparqlEquals(
				"SELECT * WHERE { ?x0 <http://example.com/ns#link> ?x1 . }",
				query.getQueryString());
	}

	@Test
	void gen02_bNodeHelperProducesUniqueLabels() {
		SelectQuery query = Queries.SELECT();
		RdfBlankNode.LabeledBlankNode subject = query.bNode();
		RdfBlankNode.LabeledBlankNode friend = query.bNode();

		query.where(GraphPatterns.tp(subject, Rdf.iri(NS + "knows"), friend));

		String rendered = query.getQueryString();
		assertThat(rendered).contains("_:b0");
		assertThat(rendered).contains("_:b1");
		assertSparqlEquals(
				"SELECT * WHERE { _:b0 <http://example.com/ns#knows> _:b1 . }",
				rendered);
	}
}
