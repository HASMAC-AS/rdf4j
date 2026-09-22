package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;

/**
 * Utilities for mapping RDF values to stable string encodings suitable for Cassandra keys.
 */
public final class ValueEncoding {

	public static final String DEFAULT_CONTEXT = "";

	private ValueEncoding() {
	}

	public static String encodeResource(Resource resource) {
		Objects.requireNonNull(resource, "resource");
		if (resource instanceof BNode) {
			return "_:" + resource.stringValue();
		}
		return resource.stringValue();
	}

	public static Resource decodeResource(String encoded, ValueFactory valueFactory) {
		Objects.requireNonNull(encoded, "encoded");
		Objects.requireNonNull(valueFactory, "valueFactory");
		if (encoded.startsWith("_:")) {
			return valueFactory.createBNode(encoded.substring(2));
		}
		return valueFactory.createIRI(encoded);
	}

	public static LiteralFields encodeLiteral(Literal literal) {
		Objects.requireNonNull(literal, "literal");
		String language = literal.getLanguage().orElse(null);
		return new LiteralFields(literal.getLabel(), literal.getDatatype().stringValue(),
				Optional.ofNullable(language));
	}

	public static Literal decodeLiteral(LiteralFields fields, ValueFactory valueFactory) {
		Objects.requireNonNull(fields, "fields");
		Objects.requireNonNull(valueFactory, "valueFactory");
		if (fields.language().isPresent()) {
			return valueFactory.createLiteral(fields.lexical(), fields.language().get());
		}
		return valueFactory.createLiteral(fields.lexical(), valueFactory.createIRI(fields.datatype()));
	}

	public static String encodeContext(Resource context) {
		return context == null ? DEFAULT_CONTEXT : context.stringValue();
	}

	public static Resource decodeContext(String encoded, ValueFactory valueFactory) {
		if (encoded == null || DEFAULT_CONTEXT.equals(encoded)) {
			return null;
		}
		return decodeResource(encoded, valueFactory);
	}

	public static final class LiteralFields {

		private final String lexical;
		private final String datatype;
		private final Optional<String> language;

		public LiteralFields(String lexical, String datatype, Optional<String> language) {
			this.lexical = Objects.requireNonNull(lexical, "lexical");
			this.datatype = Objects.requireNonNull(datatype, "datatype");
			this.language = language == null ? Optional.empty() : language;
		}

		public String lexical() {
			return lexical;
		}

		public String datatype() {
			return datatype;
		}

		public Optional<String> language() {
			return language;
		}
	}
}
