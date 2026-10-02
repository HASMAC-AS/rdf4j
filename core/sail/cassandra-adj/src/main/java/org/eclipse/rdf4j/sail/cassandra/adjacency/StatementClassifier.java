package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Classifies RDF statements into node types, node properties, or edges and associates them with graph namespaces.
 */
public final class StatementClassifier {

	private final NamespaceManager namespaceManager;

	public StatementClassifier(NamespaceManager namespaceManager) {
		this.namespaceManager = Objects.requireNonNull(namespaceManager, "namespaceManager");
	}

	public ClassifiedStatement classify(Statement statement) {
		Objects.requireNonNull(statement, "statement");

		Resource subject = statement.getSubject();
		IRI predicate = statement.getPredicate();
		Value object = statement.getObject();

		if (predicate.equals(RDF.TYPE) && object instanceof IRI) {
			GraphNamespaceId namespace = namespaceManager.nodeNamespaceFor((IRI) object);
			return new ClassifiedStatement(StatementKind.NODE_TYPE, namespace, subject, predicate, object);
		}

		if (object instanceof Literal) {
			GraphNamespaceId namespace = namespaceManager.nodeNamespaceFor(null);
			return new ClassifiedStatement(StatementKind.NODE_PROPERTY, namespace, subject, predicate, object);
		}

		if (object instanceof Resource) {
			GraphNamespaceId namespace = namespaceManager.edgeNamespaceFor(predicate);
			return new ClassifiedStatement(StatementKind.EDGE, namespace, subject, predicate, object);
		}

		throw new IllegalArgumentException("Unsupported statement object: " + object);
	}
}
