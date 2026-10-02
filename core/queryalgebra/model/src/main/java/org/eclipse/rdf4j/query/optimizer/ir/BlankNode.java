package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Represents a blank node term. */
public final class BlankNode implements Term {

	private final String id;

	public BlankNode(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BlankNode)) {
			return false;
		}
		BlankNode that = (BlankNode) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "_:" + id;
	}
}
