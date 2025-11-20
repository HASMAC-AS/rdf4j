package org.eclipse.rdf4j.sail.cassandra.adjacency;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

class ValueEncodingTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void encodesAndDecodesResources() {
		IRI iri = vf.createIRI("http://example.com/id/1");
		Resource bnode = vf.createBNode("b1");

		String encodedIri = ValueEncoding.encodeResource(iri);
		String encodedBnode = ValueEncoding.encodeResource(bnode);

		assertThat(ValueEncoding.decodeResource(encodedIri, vf)).isEqualTo(iri);
		assertThat(ValueEncoding.decodeResource(encodedBnode, vf)).isEqualTo(bnode);
	}

	@Test
	void encodesAndDecodesLiterals() {
		Literal literal = vf.createLiteral("Alice", "en");
		ValueEncoding.LiteralFields encoded = ValueEncoding.encodeLiteral(literal);

		assertThat(encoded.lexical()).isEqualTo("Alice");
		assertThat(encoded.datatype()).isEqualTo(literal.getDatatype().stringValue());
		assertThat(encoded.language()).contains("en");

		Literal decoded = ValueEncoding.decodeLiteral(encoded, vf);
		assertThat(decoded).isEqualTo(literal);
	}

	@Test
	void encodesAndDecodesContexts() {
		Resource context = vf.createIRI("http://example.com/graph");
		String encoded = ValueEncoding.encodeContext(context);
		String encodedNull = ValueEncoding.encodeContext(null);

		assertThat(ValueEncoding.decodeContext(encoded, vf)).isEqualTo(context);
		assertThat(ValueEncoding.decodeContext(encodedNull, vf)).isNull();
	}
}
