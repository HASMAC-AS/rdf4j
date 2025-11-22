/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb.benchmark;

/**
 * SPARQL query templates used by the LMDB WCOJ benchmarks and tests.
 */
public final class WcojBenchmarkQueries {

	public static final String NAMESPACE = "http://example.org/wcoj/";

	public static final String TRIANGLE_COUNT_QUERY = "SELECT (COUNT(*) AS ?count) WHERE { "
			+ "?a <" + NAMESPACE + "connected> ?b . "
			+ "?b <" + NAMESPACE + "connected> ?c . "
			+ "?c <" + NAMESPACE + "connected> ?a . "
			+ "FILTER(STR(?a) < STR(?b) && STR(?b) < STR(?c)) }";

	public static final String FOUR_CYCLE_COUNT_QUERY = "SELECT (COUNT(*) AS ?count) WHERE { "
			+ "?a <" + NAMESPACE + "connected> ?b . "
			+ "?b <" + NAMESPACE + "connected> ?c . "
			+ "?c <" + NAMESPACE + "connected> ?d . "
			+ "?d <" + NAMESPACE + "connected> ?a . "
			+ "FILTER(STR(?a) < STR(?b) && STR(?b) < STR(?c) && STR(?c) < STR(?d)) }";

	private WcojBenchmarkQueries() {
		// utility
	}
}
