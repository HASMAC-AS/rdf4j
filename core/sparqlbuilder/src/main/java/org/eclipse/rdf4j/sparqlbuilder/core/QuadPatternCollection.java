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

import java.util.Collections;
import java.util.function.Function;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * Represents a collection of quad patterns (triples or named graph blocks) used in SPARQL Update operations.
 */
public class QuadPatternCollection extends StandardQueryElementCollection<GraphPattern> {
	private static final Function<String, String> WRAPPER = SparqlBuilderUtils::getBracedString;

	public QuadPatternCollection(GraphPattern... patterns) {
		super("\n");
		setWrapperMethod(WRAPPER);
		and(patterns);
	}

	/**
	 * Add graph patterns to this collection.
	 *
	 * @param patterns the patterns to add
	 * @return this collection
	 */
	public QuadPatternCollection and(GraphPattern... patterns) {
		Collections.addAll(elements, patterns);

		return this;
	}
}
