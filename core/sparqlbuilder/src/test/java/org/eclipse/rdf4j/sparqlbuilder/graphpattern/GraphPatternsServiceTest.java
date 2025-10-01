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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.lang.reflect.Method;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.junit.jupiter.api.Test;

class GraphPatternsServiceTest {

	@Test
	void serviceClauseRendered() {
		SelectQuery query = Queries.SELECT();
		Variable subject = SparqlBuilder.var("s");
		Variable object = SparqlBuilder.var("o");
		Variable endpoint = SparqlBuilder.var("serviceEndpoint");

		query.select(subject)
				.where(GraphPatterns.service(endpoint, subject.has(iri("http://example.org/p"), object)));

		assertThat(query.getQueryString())
				.contains("SERVICE ?serviceEndpoint { ?s <http://example.org/p> ?o . }");
	}

	@Test
	void serviceSilentClauseRendered() {
		SelectQuery query = Queries.SELECT();
		Variable subject = SparqlBuilder.var("s");
		Variable object = SparqlBuilder.var("o");

		query.select(subject)
				.where(GraphPatterns.service(true, iri("http://example.org/service"),
						subject.has(iri("http://example.org/p"), object)));

		assertThat(query.getQueryString())
				.contains("SERVICE SILENT <http://example.org/service> { ?s <http://example.org/p> ?o . }");
	}

	@Test
	void graphPatternInterfaceExposesServiceShortcuts() throws Exception {
		Method nonSilent = GraphPattern.class.getMethod("service", GraphName.class, GraphPattern[].class);
		Method silent = GraphPattern.class.getMethod("service", boolean.class, GraphName.class,
				GraphPattern[].class);

		assertThat(nonSilent.getReturnType()).isEqualTo(GraphPattern.class);
		assertThat(silent.getReturnType()).isEqualTo(GraphPattern.class);
	}
}
