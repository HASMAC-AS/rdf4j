package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Represents an IRI term. */
public final class Iri implements Term {

	private final String iri;

	public Iri(String iri) {
		this.iri = Objects.requireNonNull(iri, "iri");
	}

	public String getIri() {
		return iri;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Iri)) {
			return false;
		}
		Iri iri1 = (Iri) o;
		return iri.equals(iri1.iri);
	}

	@Override
	public int hashCode() {
		return iri.hashCode();
	}

	@Override
	public String toString() {
		return "<" + iri + ">";
	}
}
