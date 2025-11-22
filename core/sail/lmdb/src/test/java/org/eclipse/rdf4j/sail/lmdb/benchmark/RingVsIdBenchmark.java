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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.KnowsCliqueDataGenerator;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
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

/**
 * JMH benchmark to compare ID-based join vs WCOJ (LTJ) vs WCO Ring on the same dataset.
 *
 * Modes: - ID: classic ID join (useWcojForBgp=false) - LTJ: WCOJ using LeapfrogTrieJoin (useWcojForBgp=true,
 * strategy=ltj) - RING: WCOJ using Ring (useWcojForBgp=true, strategy=ring) - AUTO: WCOJ with auto (ring on cyclic, LTJ
 * on acyclic)
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class RingVsIdBenchmark {

	@Param({ "ID", "LTJ", "RING", "AUTO" })
	private String mode;

	private SailRepository repository;
	private File dataDir;

	private static final String TRIANGLE = "SELECT ?a ?b ?c WHERE { "
			+ "?a <" + FOAF.KNOWS.stringValue() + "> ?b . "
			+ "?b <" + FOAF.KNOWS.stringValue() + "> ?c . "
			+ "?c <" + FOAF.KNOWS.stringValue() + "> ?a }";

	@Setup(Level.Trial)
	public void setup() throws IOException {
		// Configure toggles
		boolean useWcoj = !"ID".equalsIgnoreCase(mode);
		if (useWcoj) {
			System.setProperty("rdf4j.lmdb.wcoj.strategy", mode.toLowerCase());
		} else {
			System.clearProperty("rdf4j.lmdb.wcoj.strategy");
		}
		System.clearProperty("rdf4j.lmdb.wcoj.trackPartial");

		dataDir = Files.newTemporaryFolder();
		LmdbStoreConfig cfg = new LmdbStoreConfig("spoc,posc").setMaintainTrieIndexes(true).setUseWcojForBgp(useWcoj);

		LmdbStore store = new LmdbStore(dataDir, cfg);
		repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
		KnowsCliqueDataGenerator.Config cliqueCfg = new KnowsCliqueDataGenerator.Config(vf)
				.cliqueCount(13)
				.cliqueSize(12)
				.bidirectional(true)
				.includeTypeAssertion(false);
		List<Statement> statements = KnowsCliqueDataGenerator.withConfig(cliqueCfg).generate();

		try (SailRepositoryConnection conn = repository.getConnection()) {
			conn.begin(IsolationLevels.NONE);
			for (Statement st : statements) {
				conn.add(st);
			}
			conn.commit();
		}
	}

	@TearDown(Level.Trial)
	public void tearDown() throws IOException {
		if (repository != null) {
			repository.shutDown();
		}
		if (dataDir != null) {
			FileUtils.deleteDirectory(dataDir);
		}
		System.clearProperty("rdf4j.lmdb.wcoj.strategy");
		System.clearProperty("rdf4j.lmdb.wcoj.trackPartial");
	}

	@Benchmark
	public long triangleQuery() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			try (TupleQueryResult result = conn.prepareTupleQuery(TRIANGLE).evaluate()) {
				return count(result);
			}
		}
	}

	private long count(TupleQueryResult evaluate) {
		try (Stream<BindingSet> stream = evaluate.stream()) {
			return stream.count();
		}
	}
}
