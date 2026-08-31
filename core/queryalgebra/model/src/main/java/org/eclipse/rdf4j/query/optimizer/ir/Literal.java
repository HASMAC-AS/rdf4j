package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Represents a literal term. */
public final class Literal implements Term {

	private final String lexicalForm;
	private final Iri datatype;
	private final String lang;

	public Literal(String lexicalForm, Iri datatype, String lang) {
		this.lexicalForm = Objects.requireNonNull(lexicalForm, "lexicalForm");
		this.datatype = Objects.requireNonNull(datatype, "datatype");
		this.lang = lang;
	}

	public String getLexicalForm() {
		return lexicalForm;
	}

	public Iri getDatatype() {
		return datatype;
	}

	public String getLang() {
		return lang;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Literal)) {
			return false;
		}
		Literal literal = (Literal) o;
		return lexicalForm.equals(literal.lexicalForm)
				&& datatype.equals(literal.datatype)
				&& Objects.equals(lang, literal.lang);
	}

	@Override
	public int hashCode() {
		int result = lexicalForm.hashCode();
		result = 31 * result + datatype.hashCode();
		result = 31 * result + (lang != null ? lang.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		if (lang != null) {
			return '"' + lexicalForm + "@" + lang + '"';
		}
		return '"' + lexicalForm + '"' + "^^" + datatype;
	}
}
