package org.eclipse.rdf4j.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryWorstCaseJoinBenchmarkTest {

	private MemoryWorstCaseJoinBenchmark benchmark;

	@BeforeEach
	void setUp() {
		benchmark = new MemoryWorstCaseJoinBenchmark();
		benchmark.strategy = MemoryWorstCaseJoinBenchmark.Strategy.LEAPFROG.name();
		benchmark.setUp();
	}

	@AfterEach
	void tearDown() {
		benchmark.tearDown();
	}

	@Test
	void triangleQueryProducesResults() {
		int count = benchmark.triangleQuery();
		assertTrue(count > 0, "triangle query should produce results");
	}
}
