package org.eclipse.rdf4j.sparqlbuilder.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

public final class QueryAssert {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private QueryAssert() {
	}

	public static void assertSparqlEquals(String expected, String actual) {
		assertThat(normalize(actual)).isEqualTo(normalize(expected));
	}

	private static String normalize(String sparql) {
		if (sparql == null) {
			return null;
		}
		return WHITESPACE.matcher(sparql.trim()).replaceAll(" ");
	}
}
