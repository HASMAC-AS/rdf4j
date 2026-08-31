package org.eclipse.rdf4j.benchmark;

import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode({ Mode.AverageTime })
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G" })
public class MemoryWorstCaseJoinBenchmark {

	public enum Strategy {
		LEAPFROG,
		DEFAULT
	}

	@Param({ "LEAPFROG", "DEFAULT" })
	public String strategy;

	private SailRepository repository;

	private ValueFactory valueFactory;

	private IRI p1;

	private IRI p2;

	private IRI p3;

	private String triangleQuery;

	private int expectedCount;

	@Setup
	public void setUp() {
		repository = new SailRepository(createStore());
		repository.init();
		valueFactory = SimpleValueFactory.getInstance();
		p1 = valueFactory.createIRI("urn:p1");
		p2 = valueFactory.createIRI("urn:p2");
		p3 = valueFactory.createIRI("urn:p3");
		triangleQuery = "SELECT ?a ?b ?c WHERE { ?a <urn:p1> ?b . ?a <urn:p2> ?c . ?b <urn:p3> ?c . }";
		loadDataset();
	}

	@TearDown
	public void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Benchmark
	public int triangleQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(triangleQuery);
			try (TupleQueryResult result = query.evaluate()) {
				int count = 0;
				while (result.hasNext()) {
					result.next();
					count++;
				}
				if (count != expectedCount) {
					throw new IllegalStateException("Expected " + expectedCount + " results but got " + count);
				}
				return count;
			}
		}
	}

	private MemoryStore createStore() {
		MemoryStore store = new MemoryStore();
		if (Strategy.DEFAULT.name().equals(strategy)) {
			store.setEvaluationStrategyFactory(new DefaultEvaluationStrategyFactory());
		}
		return store;
	}

	private void loadDataset() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin();

			int subjects = 25;
			int fanout = 20;
			expectedCount = subjects * fanout * fanout;

			for (int a = 0; a < subjects; a++) {
				Resource subject = valueFactory.createIRI("urn:a" + a);
				for (int b = 0; b < fanout; b++) {
					Resource bNode = valueFactory.createIRI("urn:b" + b);
					connection.add(subject, p1, bNode);
				}
				for (int c = 0; c < fanout; c++) {
					Resource cNode = valueFactory.createIRI("urn:c" + c);
					connection.add(subject, p2, cNode);
				}
			}

			for (int b = 0; b < fanout; b++) {
				Resource bNode = valueFactory.createIRI("urn:b" + b);
				for (int c = 0; c < fanout; c++) {
					Resource cNode = valueFactory.createIRI("urn:c" + c);
					connection.add(bNode, p3, cNode);
				}
			}

			connection.commit();
		}
	}
}
