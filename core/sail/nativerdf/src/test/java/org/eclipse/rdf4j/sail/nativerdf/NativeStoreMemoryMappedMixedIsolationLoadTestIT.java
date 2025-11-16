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
package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Exercises the NativeStore's memory-mapped data files with a mix of isolation levels. Multiple writer threads perform
 * thousands of transactions using {@link IsolationLevels#NONE} and {@link IsolationLevels#SERIALIZABLE} while reader
 * threads continuously iterate statements with {@link IsolationLevels#SNAPSHOT_READ}. The test fails if any thread
 * experiences a concurrency exception or if the persisted data cannot be reloaded after the workload.
 */
@Tag("slow")
@Isolated
public class NativeStoreMemoryMappedMixedIsolationLoadTestIT {

	@TempDir
	File dataDir;

	private boolean previousSoftFail;

	@BeforeEach
	public void disableSoftFail() {
		previousSoftFail = NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES;
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
	}

	@AfterEach
	public void restoreSoftFail() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = previousSoftFail;
	}

	@Test
	public void mixedIsolationWorkloadPreservesData() throws Exception {
		NativeStore store = new NativeStore(dataDir, "spoc,posc");
		store.init();
		SailRepository repository = new SailRepository(store);
		repository.init();

		int lowIsolationWriters = 6;
		int highIsolationWriters = 2;
		int readers = 3;
		int lowIsolationTransactions = 1_200;
		int highIsolationTransactions = 700;
		int baselineTriples = 48;

		preloadBaseline(repository, baselineTriples);

		ExecutorService pool = Executors.newFixedThreadPool(lowIsolationWriters + highIsolationWriters + readers);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch writersDone = new CountDownLatch(lowIsolationWriters + highIsolationWriters);
		List<Future<?>> futures = new ArrayList<>();

		final SailRepository repoRef = repository;

		for (int writer = 0; writer < lowIsolationWriters; writer++) {
			final int writerId = writer;
			futures.add(pool.submit(() -> {
				try (RepositoryConnection conn = repoRef.getConnection()) {
					start.await();
					ValueFactory vf = conn.getValueFactory();
					for (int i = 0; i < lowIsolationTransactions; i++) {
						conn.begin(IsolationLevels.NONE);
						try {
							IRI subject = vf.createIRI("urn:writer:none:" + writerId + ":" + i);
							IRI predicate = vf.createIRI("urn:p" + (i % 8));
							String lexical = (i % 120 == 0) ? buildLargeLiteral(i) : "value" + i;
							conn.add(subject, predicate, vf.createLiteral(lexical));
							conn.commit();
						} catch (Throwable t) {
							try {
								conn.rollback();
							} catch (Throwable ignore) {
							}
							throw t;
						}
					}
				} finally {
					writersDone.countDown();
				}
				return null;
			}));
		}

		for (int writer = 0; writer < highIsolationWriters; writer++) {
			final int writerId = writer;
			futures.add(pool.submit(() -> {
				try (RepositoryConnection conn = repoRef.getConnection()) {
					start.await();
					ValueFactory vf = conn.getValueFactory();
					for (int i = 0; i < highIsolationTransactions; i++) {
						conn.begin(IsolationLevels.SERIALIZABLE);
						try {
							IRI subject = vf.createIRI("urn:writer:serial:" + writerId + ":" + i);
							IRI predicate = vf.createIRI("urn:q" + (i % 5));
							String lexical = (i % 75 == 0) ? buildLargeLiteral(i + 50) : "serial" + i;
							conn.add(subject, predicate, vf.createLiteral(lexical));
							conn.commit();
						} catch (Throwable t) {
							try {
								conn.rollback();
							} catch (Throwable ignore) {
							}
							throw t;
						}
					}
				} finally {
					writersDone.countDown();
				}
				return null;
			}));
		}

		for (int reader = 0; reader < readers; reader++) {
			futures.add(pool.submit(() -> {
				try (RepositoryConnection conn = repoRef.getConnection()) {
					start.await();
					long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
					while (writersDone.getCount() > 0 || System.nanoTime() < deadline) {
						conn.begin(IsolationLevels.SNAPSHOT_READ);
						try (RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false)) {
							while (statements.hasNext()) {
								statements.next();
							}
							conn.commit();
						} catch (Throwable t) {
							try {
								conn.rollback();
							} catch (Throwable ignore) {
							}
							throw t;
						}
						Thread.onSpinWait();
					}
				}
				return null;
			}));
		}

		start.countDown();

		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} finally {
			pool.shutdownNow();
			pool.awaitTermination(30, TimeUnit.SECONDS);
		}

		repository.shutDown();
		store.shutDown();

		store = new NativeStore(dataDir, "spoc,posc");
		store.init();
		repository = new SailRepository(store);
		repository.init();

		int expectedStatements = baselineTriples + (lowIsolationWriters * lowIsolationTransactions)
				+ (highIsolationWriters * highIsolationTransactions);

		try (RepositoryConnection conn = repository.getConnection()) {
			long size = conn.size();
			assertThat(size)
					.as("total statement count after mixed isolation workload")
					.isEqualTo(expectedStatements);

			ValueFactory vf = conn.getValueFactory();
			assertThat(conn.hasStatement(
					vf.createIRI("urn:writer:none:0:0"),
					vf.createIRI("urn:p0"),
					vf.createLiteral(buildLargeLiteral(0)),
					false))
					.isTrue();

			assertThat(conn.hasStatement(
					vf.createIRI(String.format(Locale.ROOT, "urn:writer:serial:%d:%d", highIsolationWriters - 1,
							highIsolationTransactions - 1)),
					vf.createIRI("urn:q" + ((highIsolationTransactions - 1) % 5)),
					vf.createLiteral("serial" + (highIsolationTransactions - 1)),
					false))
					.isTrue();
		}

		repository.shutDown();
		store.shutDown();
	}

	private static void preloadBaseline(SailRepository repository, int count) {
		try (RepositoryConnection conn = repository.getConnection()) {
			conn.begin(IsolationLevels.SNAPSHOT);
			ValueFactory vf = conn.getValueFactory();
			for (int i = 0; i < count; i++) {
				IRI subject = vf.createIRI("urn:baseline:" + i);
				conn.add(subject, vf.createIRI("urn:bp" + (i % 3)), vf.createLiteral("baseline" + i));
			}
			conn.commit();
		}
	}

	private static String buildLargeLiteral(int seed) {
		int length = 16_384 + (seed % 1_024);
		StringBuilder builder = new StringBuilder(length);
		while (builder.length() < length) {
			builder.append((char) ('a' + (seed % 26)));
		}
		return builder.toString();
	}
}
