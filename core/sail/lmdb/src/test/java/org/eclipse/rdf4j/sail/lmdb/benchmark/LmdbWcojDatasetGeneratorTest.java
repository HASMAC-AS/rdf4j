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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.eclipse.rdf4j.sail.lmdb.benchmark.WcojBenchmarkQueries;
import org.eclipse.rdf4j.sail.lmdb.benchmark.WcojDatasetGenerator;
import org.eclipse.rdf4j.sail.lmdb.benchmark.WcojDatasetGenerator.GenerationResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbWcojDatasetGeneratorTest {

        @TempDir
        File dataDir;

        @Test
        void generatorBuildsCyclicDataAndQueriesReturnExpectedCounts() {
                SailRepository repository = new SailRepository(new LmdbStore(dataDir, new LmdbStoreConfig("spoc,psoc,opsc")));
                repository.init();

                GenerationResult result;
                try (RepositoryConnection connection = repository.getConnection()) {
                        WcojDatasetGenerator generator = new WcojDatasetGenerator(connection.getValueFactory());
                        result = generator.generate(connection, 5, 2);
                }

                assertThat(result.getTriangleCount()).isEqualTo(5);
                assertThat(result.getStatementCount()).isEqualTo(5L * (5 + (4 * 2)));

                try (RepositoryConnection connection = repository.getConnection()) {
                        long triangleCount = connection.prepareTupleQuery(WcojBenchmarkQueries.TRIANGLE_COUNT_QUERY)
                                        .evaluate()
                                        .stream()
                                        .findFirst()
                                        .map(binding -> binding.getValue("count"))
                                        .map(value -> value.stringValue())
                                        .map(Long::parseLong)
                                        .orElseThrow();

                        long fourCycleCount = connection.prepareTupleQuery(WcojBenchmarkQueries.FOUR_CYCLE_COUNT_QUERY)
                                        .evaluate()
                                        .stream()
                                        .findFirst()
                                        .map(binding -> binding.getValue("count"))
                                        .map(value -> value.stringValue())
                                        .map(Long::parseLong)
                                        .orElseThrow();

                        assertThat(triangleCount).isEqualTo(5);
                        assertThat(fourCycleCount).isEqualTo(5);
                } finally {
                        repository.shutDown();
                }
        }
}
