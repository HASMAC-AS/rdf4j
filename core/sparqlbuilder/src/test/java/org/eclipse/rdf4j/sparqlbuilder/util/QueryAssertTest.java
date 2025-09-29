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
package org.eclipse.rdf4j.sparqlbuilder.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QueryAssertTest {

	@Test
	void assertSparqlEquals_requiresQueryToParse() {
		String invalidQuery = "SELECT * WHERE { ?s ?p ?o .";

		assertThatThrownBy(() -> QueryAssert.assertSparqlEquals(invalidQuery, invalidQuery))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	void assertSparqlEquals_acceptsUpdates() {
		String update = "INSERT DATA { <urn:a> <urn:b> \"c\" . }";

		assertThatCode(() -> QueryAssert.assertSparqlEquals(update, update)).doesNotThrowAnyException();
	}
}
