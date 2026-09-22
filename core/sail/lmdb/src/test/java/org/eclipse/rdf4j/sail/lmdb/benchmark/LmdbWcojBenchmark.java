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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.benchmark.WcojDatasetGenerator.GenerationResult;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Fork(1)
@State(Scope.Thread)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LmdbWcojBenchmark {

	@Param({ "5000", "20000" })
	public int triangles;

	@Param({ "2" })
	public int fanoutPerNode;

	private Path dataDir;
	private Repository repository;
	private RepositoryConnection connection;
	private GenerationResult generationResult;

	@Setup(Level.Trial)
	public void setup() throws IOException {
		dataDir = Files.createTempDirectory("lmdb-wcoj-bench");

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,psoc,opsc");
		repository = new SailRepository(new LmdbStore(dataDir.toFile(), config));
		repository.init();

		try (RepositoryConnection setupConnection = repository.getConnection()) {
			WcojDatasetGenerator generator = new WcojDatasetGenerator(setupConnection.getValueFactory());
			generationResult = generator.generate(setupConnection, triangles, fanoutPerNode);
		}

		connection = repository.getConnection();
	}

	@TearDown(Level.Trial)
	public void tearDown() throws IOException {
		connection.close();
		repository.shutDown();
		FileUtils.deleteDirectory(dataDir.toFile());
	}

	@Benchmark
	public void triangleCount(Blackhole blackhole) {
		blackhole.consume(runCountQuery(WcojBenchmarkQueries.TRIANGLE_COUNT_QUERY));
	}

	@Benchmark
	public void fourCycleCount(Blackhole blackhole) {
		blackhole.consume(runCountQuery(WcojBenchmarkQueries.FOUR_CYCLE_COUNT_QUERY));
	}

	public long runCountQuery(String query) {
		try (TupleQueryResult result = connection.prepareTupleQuery(query).evaluate()) {
			if (result.hasNext()) {
				Literal count = (Literal) result.next().getValue("count");
				return count.longValue();
			}
			return 0L;
		}
	}

	public GenerationResult getGenerationResult() {
		return generationResult;
	}

	public static void main(String[] args) throws RunnerException {
		String regex = ".*" + LmdbWcojBenchmark.class.getSimpleName() + ".*";
		new Runner(new OptionsBuilder().include(regex).build()).run();
	}
}
