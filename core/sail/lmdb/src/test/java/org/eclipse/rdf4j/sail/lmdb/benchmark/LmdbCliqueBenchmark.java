/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.benchmark;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategyFactory;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmarks a clique SPARQL pattern on the LMDB store comparing worst-case optimal join (LFTJ) with the standard join
 * implementation.
 */
@Fork(1)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LmdbCliqueBenchmark {

	@Param({ "wcoj", "standard" })
	public String joinStrategy;

	@Param({ "50" })
	public int nodeCount;

	@Param({ "3" })
	public int cliqueSize;

	@Param({ "3" })
	public int queryCliqueSize;

	private File dataDir;

	private SailRepository repository;

	private RepositoryConnection connection;

	private String cliqueQuery;

	private long expectedResultCount;

	@Setup(Level.Trial)
	public void setup() throws IOException {
		if (nodeCount < cliqueSize) {
			throw new IllegalArgumentException("nodeCount must be >= cliqueSize");
		}

		dataDir = Files.createTempDirectory("lmdb-clique-benchmark").toFile();

		LmdbStore store = new LmdbStore(dataDir);
		if ("standard".equalsIgnoreCase(joinStrategy)) {
			store.setEvaluationStrategyFactory(new DefaultEvaluationStrategyFactory());
		}

		repository = new SailRepository(store);
		repository.init();
		connection = repository.getConnection();

		loadCompleteGraph();

		cliqueQuery = buildCycleQuery(queryCliqueSize);
		expectedResultCount = permutations(nodeCount, cliqueSize);
	}

	private void loadCompleteGraph() {
		ValueFactory vf = connection.getValueFactory();
		List<IRI> nodes = new ArrayList<>(nodeCount);
		for (int i = 0; i < nodeCount; i++) {
			nodes.add(vf.createIRI("urn:node:" + i));
		}

		connection.begin();
		for (int i = 0; i < nodeCount; i++) {
			IRI subject = nodes.get(i);
			for (int j = 0; j < nodeCount; j++) {
				if (i == j) {
					continue;
				}
				connection.add(subject, FOAF.KNOWS, nodes.get(j));
			}
		}
		connection.commit();
	}

	private String buildCycleQuery(int size) {
		StringBuilder builder = new StringBuilder("SELECT ");
		for (int i = 0; i < size; i++) {
			builder.append("?n").append(i).append(' ');
		}
		builder.append("{\n");
		for (int i = 0; i < size; i++) {
			int next = (i + 1) % size;
			builder.append("  ?n")
					.append(i)
					.append(" <")
					.append(FOAF.KNOWS.stringValue())
					.append("> ?n")
					.append(next)
					.append(" .\n");
		}
		builder.append('}');
		return builder.toString();
	}

	private long permutations(int n, int k) {
		long result = 1;
		for (int i = 0; i < k; i++) {
			result *= (n - i);
		}
		return result;
	}

	@Benchmark
	public long cliqueQuery() {
		long count = 0;
//		System.out.println(cliqueQuery);
		try (TupleQueryResult result = connection.prepareTupleQuery(cliqueQuery).evaluate()) {
			while (result.hasNext()) {
				result.next();
				count++;
			}
		}

		if (nodeCount == 50 && cliqueSize == 3 && queryCliqueSize == 3) {
			if (count != 117600) {
				throw new IllegalStateException("Unexpected result size: " + count + " (expected " + 117600 + ')');
			}
		}
		return count;
	}

	public Explanation explainCliqueQuery() {
		return connection.prepareTupleQuery(cliqueQuery).explain(Explanation.Level.Executed);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws IOException {
		if (connection != null) {
			connection.close();
		}
		if (repository != null) {
			repository.shutDown();
		}
		if (dataDir != null) {
			FileUtils.deleteDirectory(dataDir);
		}
	}

	public static void main(String[] args) throws RunnerException, IOException {
		Options options = new OptionsBuilder()
				.include(".*" + LmdbCliqueBenchmark.class.getSimpleName() + ".*")
				.forks(0)
				.build();
		new Runner(options).run();
	}
}
