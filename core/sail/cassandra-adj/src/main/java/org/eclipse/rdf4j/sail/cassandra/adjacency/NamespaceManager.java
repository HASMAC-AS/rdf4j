package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;

/**
 * Resolves RDF classes and predicates to logical graph namespaces.
 */
public final class NamespaceManager {

	private final Map<IRI, GraphNamespaceId> nodeNamespaces;
	private final Map<IRI, GraphNamespaceId> edgeNamespaces;
	private final GraphNamespaceId defaultNodeNamespace;
	private final GraphNamespaceId defaultEdgeNamespace;

	private NamespaceManager(Map<IRI, GraphNamespaceId> nodeNamespaces, Map<IRI, GraphNamespaceId> edgeNamespaces,
			GraphNamespaceId defaultNodeNamespace, GraphNamespaceId defaultEdgeNamespace) {
		this.nodeNamespaces = nodeNamespaces;
		this.edgeNamespaces = edgeNamespaces;
		this.defaultNodeNamespace = defaultNodeNamespace;
		this.defaultEdgeNamespace = defaultEdgeNamespace;
	}

	public static Builder builder() {
		return new Builder();
	}

	public GraphNamespaceId nodeNamespaceFor(IRI type) {
		if (type != null && nodeNamespaces.containsKey(type)) {
			return nodeNamespaces.get(type);
		}
		return defaultNodeNamespace;
	}

	public GraphNamespaceId edgeNamespaceFor(IRI predicate) {
		if (predicate != null && edgeNamespaces.containsKey(predicate)) {
			return edgeNamespaces.get(predicate);
		}
		return defaultEdgeNamespace;
	}

	public GraphNamespaceId defaultNodeNamespace() {
		return defaultNodeNamespace;
	}

	public GraphNamespaceId defaultEdgeNamespace() {
		return defaultEdgeNamespace;
	}

	public static final class Builder {

		private final Map<IRI, GraphNamespaceId> nodeNamespaces = new HashMap<>();
		private final Map<IRI, GraphNamespaceId> edgeNamespaces = new HashMap<>();
		private GraphNamespaceId defaultNodeNamespace;
		private GraphNamespaceId defaultEdgeNamespace;

		public Builder defaultNodeNamespace(String namespaceId) {
			this.defaultNodeNamespace = new GraphNamespaceId(namespaceId);
			return this;
		}

		public Builder defaultEdgeNamespace(String namespaceId) {
			this.defaultEdgeNamespace = new GraphNamespaceId(namespaceId);
			return this;
		}

		public Builder addNodeNamespace(IRI type, GraphNamespaceId namespaceId) {
			nodeNamespaces.put(Objects.requireNonNull(type), Objects.requireNonNull(namespaceId));
			return this;
		}

		public Builder addEdgeNamespace(IRI predicate, GraphNamespaceId namespaceId) {
			edgeNamespaces.put(Objects.requireNonNull(predicate), Objects.requireNonNull(namespaceId));
			return this;
		}

		public NamespaceManager build() {
			Objects.requireNonNull(defaultNodeNamespace, "Default node namespace must be configured");
			Objects.requireNonNull(defaultEdgeNamespace, "Default edge namespace must be configured");
			return new NamespaceManager(Collections.unmodifiableMap(new HashMap<>(nodeNamespaces)),
					Collections.unmodifiableMap(new HashMap<>(edgeNamespaces)), defaultNodeNamespace,
					defaultEdgeNamespace);
		}
	}
}
