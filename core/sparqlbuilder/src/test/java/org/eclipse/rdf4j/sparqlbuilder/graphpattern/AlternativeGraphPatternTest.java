package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

class AlternativeGraphPatternTest {

	@Test
	void unionWrapsOptionalGraphPatternInBraces() {
		Variable subject = SparqlBuilder.var("subject");

		GraphPattern unionPattern = GraphPatterns.union(
				GraphPatterns.optional(subject.isA(Rdf.iri("http://example.org/Class")))
		).union(subject.has(Rdf.iri("http://example.org/secondpred"), Rdf.literalOf("test")));

		assertThat(unionPattern.getQueryString()).isEqualTo(
				"{ OPTIONAL { ?subject a <http://example.org/Class> . } } UNION { ?subject <http://example.org/secondpred> \"test\" . }");
	}
}
