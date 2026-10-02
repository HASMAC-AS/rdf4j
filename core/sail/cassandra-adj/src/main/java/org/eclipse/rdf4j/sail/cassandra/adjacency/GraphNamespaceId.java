package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.util.Objects;

/**
 * Logical graph namespace identifier used to route nodes and edges.
 */
public final class GraphNamespaceId {

	private final String id;

	public GraphNamespaceId(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}

	public String id() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GraphNamespaceId that = (GraphNamespaceId) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id;
	}
}
