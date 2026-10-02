package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * Result of classifying an RDF statement into a Cassandra namespace and category.
 */
public final class ClassifiedStatement {

	private final StatementKind kind;
	private final GraphNamespaceId namespace;
	private final Resource subject;
	private final IRI predicate;
	private final Value object;

	public ClassifiedStatement(StatementKind kind, GraphNamespaceId namespace, Resource subject, IRI predicate,
			Value object) {
		this.kind = Objects.requireNonNull(kind, "kind");
		this.namespace = Objects.requireNonNull(namespace, "namespace");
		this.subject = Objects.requireNonNull(subject, "subject");
		this.predicate = Objects.requireNonNull(predicate, "predicate");
		this.object = Objects.requireNonNull(object, "object");
	}

	public StatementKind getKind() {
		return kind;
	}

	public GraphNamespaceId getNamespace() {
		return namespace;
	}

	public Resource getSubject() {
		return subject;
	}

	public IRI getPredicate() {
		return predicate;
	}

	public Value getObject() {
		return object;
	}
}
