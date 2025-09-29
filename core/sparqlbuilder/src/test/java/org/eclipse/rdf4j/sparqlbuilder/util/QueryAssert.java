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
package org.eclipse.rdf4j.sparqlbuilder.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QueryAssert {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern PREFIX_DECLARATION = Pattern
			.compile("PREFIX\\s+[^\\s]+:\\s*<[^>]+>", Pattern.CASE_INSENSITIVE);
	private static final Pattern BASE_DECLARATION = Pattern.compile("BASE\\s+<[^>]+>", Pattern.CASE_INSENSITIVE);

	private QueryAssert() {
	}

	public static void assertSparqlEquals(String expected, String actual) {
		assertThat(normalize(actual)).isEqualTo(normalize(expected));
	}

	private static String normalize(String sparql) {
		if (sparql == null) {
			return null;
		}
		String trimmed = sparql.trim();
		List<String> bases = extractDeclarations(BASE_DECLARATION, trimmed);
		String withoutBases = stripDeclarations(BASE_DECLARATION, trimmed);

		List<String> prefixes = extractDeclarations(PREFIX_DECLARATION, withoutBases);
		Collections.sort(prefixes);
		String remainder = stripDeclarations(PREFIX_DECLARATION, withoutBases);

		StringBuilder canonical = new StringBuilder();
		appendDeclarations(canonical, bases);
		appendDeclarations(canonical, prefixes);
		if (!remainder.isBlank()) {
			if (canonical.length() > 0) {
				canonical.append(' ');
			}
			canonical.append(remainder.trim());
		}

		return WHITESPACE.matcher(canonical.toString().trim()).replaceAll(" ");
	}

	private static List<String> extractDeclarations(Pattern pattern, String sparql) {
		Matcher matcher = pattern.matcher(sparql);
		List<String> declarations = new ArrayList<>();
		while (matcher.find()) {
			declarations.add(matcher.group().trim());
		}
		return declarations;
	}

	private static String stripDeclarations(Pattern pattern, String sparql) {
		return pattern.matcher(sparql).replaceAll(" ").trim();
	}

	private static void appendDeclarations(StringBuilder builder, List<String> declarations) {
		if (declarations.isEmpty()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(' ');
		}
		for (int i = 0; i < declarations.size(); i++) {
			if (i > 0) {
				builder.append(' ');
			}
			builder.append(declarations.get(i));
		}
	}
}
