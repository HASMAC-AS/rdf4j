package org.eclipse.rdf4j.sail.cassandra.adjacency;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

class StatementClassifierTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private NamespaceManager namespaceManager() {
		return NamespaceManager.builder()
				.defaultNodeNamespace("default-node")
				.defaultEdgeNamespace("default-edge")
				.addNodeNamespace(vf.createIRI("http://example.com/Account"), new GraphNamespaceId("member-account"))
				.addEdgeNamespace(vf.createIRI("http://example.com/startedWatching"),
						new GraphNamespaceId("started-watching"))
				.build();
	}

	@Test
	void identifiesNodeTypeStatements() {
		Resource subject = vf.createIRI("http://example.com/user/1");
		IRI type = vf.createIRI("http://example.com/Account");
		Statement statement = vf.createStatement(subject, RDF.TYPE, type);

		StatementClassifier classifier = new StatementClassifier(namespaceManager());
		ClassifiedStatement classified = classifier.classify(statement);

		assertThat(classified.getKind()).isEqualTo(StatementKind.NODE_TYPE);
		assertThat(classified.getNamespace().id()).isEqualTo("member-account");
	}

	@Test
	void identifiesNodePropertyStatements() {
		Resource subject = vf.createIRI("http://example.com/user/2");
		Statement statement = vf.createStatement(subject, vf.createIRI("http://example.com/name"),
				vf.createLiteral("Alice"));

		StatementClassifier classifier = new StatementClassifier(namespaceManager());
		ClassifiedStatement classified = classifier.classify(statement);

		assertThat(classified.getKind()).isEqualTo(StatementKind.NODE_PROPERTY);
		assertThat(classified.getNamespace().id()).isEqualTo("default-node");
	}

	@Test
	void identifiesEdgeStatements() {
		Resource subject = vf.createIRI("http://example.com/user/3");
		Resource target = vf.createIRI("http://example.com/title/7");
		IRI predicate = vf.createIRI("http://example.com/startedWatching");
		Statement statement = vf.createStatement(subject, predicate, target);

		StatementClassifier classifier = new StatementClassifier(namespaceManager());
		ClassifiedStatement classified = classifier.classify(statement);

		assertThat(classified.getKind()).isEqualTo(StatementKind.EDGE);
		assertThat(classified.getNamespace().id()).isEqualTo("started-watching");
	}
}
