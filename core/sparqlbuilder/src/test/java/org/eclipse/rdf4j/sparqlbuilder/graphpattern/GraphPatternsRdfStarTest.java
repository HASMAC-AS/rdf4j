package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.junit.jupiter.api.Test;

class GraphPatternsRdfStarTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final String NS = "http://example.org/";

	@Test
	void tripleResourceSubjectIsSupported() {
		Triple subject = VF.createTriple(iri("subject"), iri("predicate"), iri("object"));
		RdfPredicate predicate = Rdf.iri(iri("has"));
		RdfObject object = Rdf.iri(iri("value"));

		TriplePattern pattern = GraphPatterns.tp(subject, predicate, object);

		String expected = toEmbeddedTripleString(subject) + " " + predicate.getQueryString() + " "
				+ object.getQueryString() + " .";

		assertThat(pattern.getQueryString()).isEqualTo(expected);
	}

	@Test
	void tripleValueObjectIsSupported() {
		IRI subject = iri("resource");
		IRI predicate = iri("predicate");
		Triple object = VF.createTriple(iri("embeddedSubject"), iri("embeddedPredicate"), iri("embeddedObject"));

		TriplePattern pattern = GraphPatterns.tp(subject, predicate, object);

		String expected = Rdf.iri(subject).getQueryString() + " " + Rdf.iri(predicate).getQueryString() + " "
				+ toEmbeddedTripleString(object) + " .";

		assertThat(pattern.getQueryString()).isEqualTo(expected);
	}

	private static IRI iri(String localName) {
		return VF.createIRI(NS, localName);
	}

	private static String toEmbeddedTripleString(Triple triple) {
		return "<<" + toQueryString(triple.getSubject()) + " " + toQueryString(triple.getPredicate()) + " "
				+ toQueryString(triple.getObject()) + ">>";
	}

	private static String toQueryString(Value value) {
		if (value instanceof Triple) {
			return toEmbeddedTripleString((Triple) value);
		}
		if (value instanceof IRI) {
			return Rdf.iri((IRI) value).getQueryString();
		}
		throw new IllegalArgumentException("Unexpected value type: " + value.getClass());
	}
}
