/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Reproduces GH-3317 by exercising a heavy concurrent workload that partially reads tuple query results against native
 * repositories. The expected behaviour is that FedX closes every iteration cleanly without triggering the
 * CleanerIteration warning. The current bug leaves iterations open which are force-closed by the cleaner. The assertion
 * below therefore fails with the current implementation and documents the regression.
 */
public class MediumConcurrencyNativeReadLeakIT extends SPARQLBaseTest {

	private static final String[] QUERIES = new String[] { "query01", "query02", "query03", "query04", "query05",
			"query06", "query07", "query08", "query09", "query10", "query11", "query12" };

	private static final int THREADS = 20;
	private static ExecutorService executor;
	private static Logger cleanerLogger;
	private static ListAppender<ILoggingEvent> appender;

	@BeforeAll
	public static void beforeClass() {
		Assumptions.assumeTrue("NATIVE".equals(System.getProperty("repositoryType")),
				"Only relevant for native repositories");

		executor = Executors.newFixedThreadPool(THREADS);
		cleanerLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		appender = new ListAppender<>();
		appender.start();
		cleanerLogger.addAppender(appender);
		cleanerLogger.setLevel(Level.WARN);
	}

	@AfterAll
	public static void afterClass() throws InterruptedException {
		if (executor != null) {
			executor.shutdownNow();
			executor.awaitTermination(30, TimeUnit.SECONDS);
		}

		if (cleanerLogger != null && appender != null) {
			long forcedClosures = appender.list.stream()
					.filter(event -> {
						String message = event.getFormattedMessage();
						return message.contains("Forced closing of unclosed iteration")
								|| message.contains("Unclosed iteration")
								|| message.contains("Failed to close connection")
								|| message.contains("Closing active connection due to shut down");
					})
					.count();

			Assertions.assertEquals(0, forcedClosures,
					"All iterations should be closed without relying on the cleaner");

			cleanerLogger.detachAppender(appender);
			appender.stop();
		}
	}

	@Test
	public void queryMixShouldNotLeakIterations() throws Throwable {
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl",
				"/tests/medium/data3.ttl", "/tests/medium/data4.ttl"));

		final int maxQueries = 500;
		final int attempts = 3;
		final Random rand = new Random(12345);

		for (int attempt = 0; attempt < attempts; attempt++) {
			CountDownLatch startLatch = new CountDownLatch(1);
			List<Future<String>> futures = new ArrayList<>();

			for (int i = 0; i < maxQueries; i++) {
				Future<String> f = submit(QUERIES[rand.nextInt(QUERIES.length)], attempt * maxQueries + i,
						startLatch);
				futures.add(f);
			}

			startLatch.countDown();

			try {
				final String message = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
					for (Future<String> f : futures) {
						f.get(30, TimeUnit.SECONDS);
					}
					return "OK";
				});
				Assertions.assertEquals("OK", message);
			} catch (Throwable t) {
				futures.forEach(future -> future.cancel(true));
				throw t;
			}
		}
	}

	protected Future<String> submit(final String query, final int queryId, CountDownLatch startLatch) {
		return executor.submit(() -> {
			startLatch.await();
			log.info("Executing query " + queryId + ": " + query);
			executeReadPartial("/tests/medium/" + query + ".rq");
			return "Ok";
		});
	}

	private void executeReadPartial(String queryFile) throws Exception {
		String queryString = readQueryString(queryFile);

		org.eclipse.rdf4j.query.Query query = queryManager().prepareQuery(queryString);

		if (query instanceof org.eclipse.rdf4j.query.TupleQuery) {
			org.eclipse.rdf4j.query.TupleQueryResult queryResult = ((org.eclipse.rdf4j.query.TupleQuery) query)
					.evaluate();
			try {
				if (queryResult.hasNext()) {
					queryResult.next();
				}
			} finally {
				queryResult.close();
			}
		} else if (query instanceof org.eclipse.rdf4j.query.GraphQuery) {
			org.eclipse.rdf4j.query.GraphQueryResult graphResult = ((org.eclipse.rdf4j.query.GraphQuery) query)
					.evaluate();
			try {
				if (graphResult.hasNext()) {
					graphResult.next();
				}
			} finally {
				graphResult.close();
			}
		} else if (query instanceof org.eclipse.rdf4j.query.BooleanQuery) {
			((org.eclipse.rdf4j.query.BooleanQuery) query).evaluate();
		} else {
			throw new RuntimeException("Unexpected query type: " + query.getClass());
		}
	}
}
