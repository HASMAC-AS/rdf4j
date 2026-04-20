/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbCompactionIT {

	@TempDir
	Path tempDir;

	private SailRepository repository;
	private LmdbStore store;

	@BeforeEach
	void setUp() {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");
		config.setAutoGrow(true);
		store = new LmdbStore(tempDir.toFile(), config);
		repository = new SailRepository(store);
		repository.init();
	}

	@AfterEach
	void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Test
	void compactShrinksValueStoreAndPreservesData() throws Exception {
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			ValueFactory vf = connection.getValueFactory();
			IntStream.range(0, 4_000).forEach(i -> {
				IRI subject = vf.createIRI("http://example.com/s" + i);
				connection.add(subject, RDF.TYPE, RDF.STATEMENT);
			});
			connection.commit();

			connection.begin();
			IntStream.range(0, 2_000).forEach(i -> {
				IRI subject = vf.createIRI("http://example.com/s" + i);
				connection.remove(subject, RDF.TYPE, RDF.STATEMENT);
			});
			connection.commit();
		}

		repository.shutDown();

		Path valuesDataFile = tempDir.resolve("values").resolve("data.mdb");
		long beforeSize = Files.size(valuesDataFile);

		LmdbCompactionOptions options = LmdbCompactionOptions.builder()
				.progressListener(progress -> {
				})
				.verifyAfterCopy(true)
				.build();

		LmdbCompactionResult result = store.compact(options);

		assertThat(result.getBytesAfter()).isLessThan(beforeSize);
		assertThat(result.getMetrics().getCopyDuration()).isGreaterThan(Duration.ZERO);
		assertThat(result.getMetrics().getDatabases())
				.extracting(LmdbCompactionMetrics.DatabaseStats::name,
						LmdbCompactionMetrics.DatabaseStats::entriesCopied)
				.contains(tuple("values", result.getMetrics()
						.getDatabases()
						.stream()
						.filter(stats -> stats.name().equals("values"))
						.findFirst()
						.orElseThrow()
						.entriesCopied()));

		SailRepository reopened = new SailRepository(new LmdbStore(tempDir.toFile(), new LmdbStoreConfig("spoc,posc")));
		reopened.init();
		try (RepositoryConnection connection = reopened.getConnection()) {
			long count = connection.size();
			assertThat(count).isEqualTo(2_000);
		} finally {
			reopened.shutDown();
		}
	}
}
