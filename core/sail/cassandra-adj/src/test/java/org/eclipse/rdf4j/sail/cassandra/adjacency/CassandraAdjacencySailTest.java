package org.eclipse.rdf4j.sail.cassandra.adjacency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CassandraAdjacencySailTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();
	private CassandraAdjacencySail sail;

	@AfterEach
	void tearDown() throws SailException {
		if (sail != null) {
			sail.shutDown();
		}
	}

	@Test
	void addQueryAndRemoveStatementsThroughSail() throws SailException {
		InMemoryGraphStore graphStore = new InMemoryGraphStore();
		sail = new CassandraAdjacencySail(graphStore, namespaceManager());
		sail.initialize();

		SailRepository repository = new SailRepository(sail);
		IRI subject = vf.createIRI("http://example.com/s");
		IRI predicate = vf.createIRI("http://example.com/p");

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.add(subject, predicate, vf.createLiteral("value"));
		}

		try (RepositoryConnection connection = repository.getConnection()) {
			List<Statement> statements = connection.getStatements(null, null, null).asList();
			assertThat(statements).hasSize(1);
			connection.remove(subject, null, null);
		}

		try (RepositoryConnection connection = repository.getConnection()) {
			assertThat(connection.getStatements(null, null, null).asList()).isEmpty();
		}
	}

	private NamespaceManager namespaceManager() {
		return NamespaceManager.builder()
				.defaultNodeNamespace("default-node")
				.defaultEdgeNamespace("default-edge")
				.build();
	}

	private static final class InMemoryGraphStore implements CassandraGraphStore {

		private final Set<Statement> statements = new LinkedHashSet<>();

		@Override
		public synchronized void applyMutations(List<Statement> adds, List<Statement> removes) {
			statements.removeAll(removes);
			statements.addAll(adds);
		}

		@Override
		public synchronized CloseableIteration<? extends Statement> queryStatements(Resource subj, IRI pred, Value obj,
				Resource[] contexts, boolean includeInferred) {
			return new CloseableIteratorIteration<>(statements.stream()
					.filter(st -> subj == null || st.getSubject().equals(subj))
					.filter(st -> pred == null || st.getPredicate().equals(pred))
					.filter(st -> obj == null || st.getObject().equals(obj))
					.filter(st -> contexts == null || contexts.length == 0
							|| st.getContext() == null && contexts.length == 1 && contexts[0] == null
							|| st.getContext() != null && List.of(contexts).contains(st.getContext()))
					.collect(Collectors.toCollection(ArrayList::new))
					.iterator());
		}
	}
}
