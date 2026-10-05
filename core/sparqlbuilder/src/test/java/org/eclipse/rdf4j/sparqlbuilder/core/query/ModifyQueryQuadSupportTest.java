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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.tp;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for https://github.com/eclipse-rdf4j/rdf4j/issues/2615.
 */
class ModifyQueryQuadSupportTest {

	@Test
	@DisplayName("ModifyQuery should expose a delete overload accepting graph patterns")
	void modifyDeleteShouldSupportGraphPatterns() {
		boolean hasDeleteOverload = hasMethodWithParameter(ModifyQuery.class, "delete", GraphPattern[].class);

		assertThat(hasDeleteOverload).as("ModifyQuery.delete(GraphPattern...) should exist to support quads").isTrue();
	}

	@Test
	@DisplayName("ModifyQuery should expose an insert overload accepting graph patterns")
	void modifyInsertShouldSupportGraphPatterns() {
		boolean hasInsertOverload = hasMethodWithParameter(ModifyQuery.class, "insert", GraphPattern[].class);

		assertThat(hasInsertOverload).as("ModifyQuery.insert(GraphPattern...) should exist to support quads").isTrue();
	}

	@Test
	@DisplayName("ModifyQuery.delete accepts named graph patterns and renders quads")
	void modifyDeleteRendersNamedGraphPatterns() {
		Iri graph = iri("http://example.com/graph");
		TriplePattern triple = tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o"));
		GraphPattern quadPattern = triple.from(graph);

		ModifyQuery modify = Queries.MODIFY().delete(quadPattern).where(triple);

		String queryString = modify.getQueryString();

		assertThat(queryString).contains("DELETE {");
		assertThat(queryString).contains("GRAPH <http://example.com/graph> {");
		assertThat(queryString).contains("?s ?p ?o .");
	}

	@Test
	@DisplayName("ModifyQuery.insert accepts named graph patterns and renders quads")
	void modifyInsertRendersNamedGraphPatterns() {
		Iri graph = iri("http://example.com/graph");
		TriplePattern triple = tp(SparqlBuilder.var("s"), SparqlBuilder.var("p"), SparqlBuilder.var("o"));
		GraphPattern quadPattern = triple.from(graph);

		ModifyQuery modify = Queries.MODIFY().insert(quadPattern).where(triple);

		String queryString = modify.getQueryString();

		assertThat(queryString).contains("INSERT {");
		assertThat(queryString).contains("GRAPH <http://example.com/graph> {");
		assertThat(queryString).contains("?s ?p ?o .");
	}

	private boolean hasMethodWithParameter(Class<?> type, String methodName, Class<?> parameterType) {
		return Arrays.stream(type.getMethods())
				.filter(method -> method.getName().equals(methodName))
				.map(Method::getParameterTypes)
				.anyMatch(params -> params.length == 1 && params[0].equals(parameterType));
	}
}
