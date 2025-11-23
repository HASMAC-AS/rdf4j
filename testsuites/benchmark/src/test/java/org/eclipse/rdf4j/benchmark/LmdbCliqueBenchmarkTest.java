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
package org.eclipse.rdf4j.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;

class LmdbCliqueBenchmarkTest {

	private LmdbCliqueBenchmark benchmarkToClose;

	@Test
	void benchmarkClassExposesStrategiesAndBenchmarkMethod() {
		Class<?> clazz;
		try {
			clazz = Class.forName("org.eclipse.rdf4j.benchmark.LmdbCliqueBenchmark");
		} catch (ClassNotFoundException e) {
			fail("Expected LmdbCliqueBenchmark to be available for JMH", e);
			return;
		}

		Field strategyField;
		try {
			strategyField = clazz.getDeclaredField("joinStrategy");
		} catch (NoSuchFieldException e) {
			fail("Benchmark should expose a joinStrategy @Param", e);
			return;
		}

		Param param = strategyField.getAnnotation(Param.class);
		assertNotNull(param, "joinStrategy must be annotated with @Param");
		assertEquals(2, param.value().length, "joinStrategy should offer two options");
		assertTrue(Arrays.asList(param.value()).containsAll(Arrays.asList("wcoj", "standard")),
				"joinStrategy should allow wcoj and standard values");

		boolean hasBenchmark = Arrays.stream(clazz.getDeclaredMethods())
				.anyMatch(m -> m.getAnnotation(Benchmark.class) != null);
		assertTrue(hasBenchmark, "A @Benchmark method is required to compare strategies");
	}

	@Test
	void cliqueQueryUsesCyclePattern() throws Exception {
		LmdbCliqueBenchmark benchmark = new LmdbCliqueBenchmark();
		benchmarkToClose = benchmark;
		benchmark.joinStrategy = "wcoj";
		benchmark.nodeCount = 4;
		benchmark.cliqueSize = 4;
		benchmark.queryCliqueSize = 4;

		benchmark.setup();

		Field queryField = LmdbCliqueBenchmark.class.getDeclaredField("cliqueQuery");
		queryField.setAccessible(true);
		String query = (String) queryField.get(benchmark);

		String expected = String.join("\n",
				"SELECT ?n0 ?n1 ?n2 ?n3 {",
				"  ?n0 <http://xmlns.com/foaf/0.1/knows> ?n1 .",
				"  ?n1 <http://xmlns.com/foaf/0.1/knows> ?n2 .",
				"  ?n2 <http://xmlns.com/foaf/0.1/knows> ?n3 .",
				"  ?n3 <http://xmlns.com/foaf/0.1/knows> ?n0 .",
				"}");

		assertEquals(expected, query);
	}

	@AfterEach
	void tearDownBenchmark() throws Exception {
		if (benchmarkToClose != null) {
			benchmarkToClose.tearDown();
		}
	}
}
