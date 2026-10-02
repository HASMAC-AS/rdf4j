package org.eclipse.rdf4j.sail.cassandra.adjacency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.CassandraContainer;

class CassandraAdjacencySailIntegrationTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final String KEYSPACE = "rdf4j_adj_test";

	private static CassandraContainer<?> cassandra;

	private static Repository repository;
	private static CassandraTestGraphStore graphStore;

	@BeforeAll
	static void setup() {
		boolean dockerAvailable;
		try {
			dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
		} catch (Throwable t) {
			dockerAvailable = false;
		}

		Assumptions.assumeTrue(dockerAvailable, "Docker is required for Cassandra tests");
		cassandra = new CassandraContainer<>("cassandra:4.1");
		cassandra.start();

		graphStore = CassandraTestGraphStore.create(cassandra, KEYSPACE, VF);
		NamespaceManager namespaceManager = NamespaceManager.builder()
				.defaultNodeNamespace("nodes")
				.defaultEdgeNamespace("edges")
				.build();

		repository = new SailRepository(new CassandraAdjacencySail(graphStore, namespaceManager));
		repository.init();
	}

	@AfterAll
	static void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
		if (graphStore != null) {
			graphStore.close();
		}
		if (cassandra != null) {
			cassandra.stop();
		}
	}

	@Test
	void writesAndReadsStatementsAgainstCassandra() {
		IRI subject = VF.createIRI("urn:subject");
		IRI predicate = VF.createIRI("urn:predicate");
		Literal object = VF.createLiteral("value");
		Resource context = VF.createIRI("urn:context");

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.add(subject, predicate, object, context);
			connection.commit();
		}

		try (RepositoryConnection connection = repository.getConnection()) {
			assertThat(connection.hasStatement(subject, predicate, object, false, context)).isTrue();
		}

		assertThat(graphStore.fetchTripleCount()).isEqualTo(1L);
	}

	@Test
	void loadRdfAndQueryThroughCassandra() {
		IRI predicate = VF.createIRI("http://example.com/p");

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.add(VF.createIRI("http://example.com/s1"), predicate, VF.createLiteral("o1"));
			connection.add(VF.createIRI("http://example.com/s2"), predicate, VF.createLiteral("o2"));
			connection.commit();
		}

		try (RepositoryConnection connection = repository.getConnection()) {
			var results = connection.prepareTupleQuery(
					"SELECT ?s ?o WHERE { ?s <http://example.com/p> ?o } ORDER BY ?s")
					.evaluate()
					.stream()
					.map(bs -> bs.getValue("s") + " " + bs.getValue("o").stringValue())
					.collect(Collectors.toList());

			assertThat(results).containsExactly(
					"http://example.com/s1 o1",
					"http://example.com/s2 o2");
		}
	}
}
