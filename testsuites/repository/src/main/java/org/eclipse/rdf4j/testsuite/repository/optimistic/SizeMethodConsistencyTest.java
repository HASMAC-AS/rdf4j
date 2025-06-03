package org.eclipse.rdf4j.testsuite.repository.optimistic;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.testsuite.repository.OptimisticIsolationTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SizeMethodConsistencyTest {

	@BeforeClass
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private Repository repo;
	private IsolationLevel level = IsolationLevels.SNAPSHOT;
	private ValueFactory vf;
	private IRI context1;
	private IRI context2;

	@Before
	public void setUp() throws Exception {
		repo = OptimisticIsolationTest.getEmptyInitializedRepository(SizeMethodConsistencyTest.class);
		vf = repo.getValueFactory();
		context1 = vf.createIRI("urn:ctx1");
		context2 = vf.createIRI("urn:ctx2");
	}

	@After
	public void tearDown() {
		repo.shutDown();
	}

	@Test
	public void testSizeConsistentWithIteration() throws Exception {
		int threads = 4;
		int iterations = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch latch = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			executor.submit(() -> {
				try (RepositoryConnection connection = repo.getConnection()) {
					Random random = new Random();
					for (int j = 0; j < iterations; j++) {
						connection.begin(level);
						IRI subj = vf.createIRI("urn:s" + random.nextInt(1000));
						Resource ctx = random.nextBoolean() ? context1 : context2;
						connection.add(subj, RDF.TYPE, RDFS.RESOURCE, ctx);
						if (random.nextBoolean()) {
							connection.remove(subj, RDF.TYPE, RDFS.RESOURCE, ctx);
						}
						connection.commit();

						if (j % 10 == 0) {
							connection.begin(level);
							assertSizeConsistent(connection);
							assertSizeConsistent(connection, context1);
							assertSizeConsistent(connection, context2);
							connection.commit();
						}
					}
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();
	}

	private void assertSizeConsistent(RepositoryConnection connection, Resource... ctx) {
		long size;
		if (ctx == null || ctx.length == 0) {
			size = connection.size();
		} else {
			size = connection.size(ctx);
		}
		try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, false, ctx)) {
			int manual = QueryResults.asList(statements).size();
			assertEquals(manual, size);
		}
	}
}
