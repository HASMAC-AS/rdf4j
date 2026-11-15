/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.rdf4j.sail.lmdb.evaluation.BenchmarkRouteTelemetry;
import org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeExpressionCompiler;
import org.eclipse.rdf4j.sail.lmdb.evaluation.OptionalFanoutRouteAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * Actual AUTO-routed, full-consumption end-to-end ablation.
 * The production code differs only in optional coordinate bookkeeping; source,
 * result ownership, weak-value reuse and ledger admissions are identical.
 */
class OptionalFanoutAblationTest {

	private static final String TRANSPORT = "rdf4j.lmdb.lazyResultCoordinateTransport.enabled";

	private record Sample(boolean transport, long checksum, long nanos, long preparationNanos,
			long evaluationNanos, long threadAllocationBytes, long lazyCalls, long factorizedRows,
			String routes) {

		double milliseconds() {
			return nanos / 1_000_000.0;
		}
	}

	@Test
	@ResourceLock(Resources.SYSTEM_PROPERTIES)
	void compareOrdinaryWeakReuseAndCoordinateTransportWithAlternatingOrder() throws Exception {
		String prior = System.getProperty(TRANSPORT);
		OptionalFanoutBenchmark benchmark = new OptionalFanoutBenchmark();
		// Full fixed fixture, equal query, store, JVM and AUTO selection for both arms.
		benchmark.measurementCount = 100;
		benchmark.readingsPerMeasurement = 10;
		try {
			System.out.printf("ABLAT JVM=%s availableCpus=%d heapMax=%d auto=true fixture=100x10%n",
					System.getProperty("java.runtime.version"), Runtime.getRuntime().availableProcessors(),
					Runtime.getRuntime().maxMemory());
			benchmark.setup();
			try {
				com.sun.management.ThreadMXBean allocation = allocationBean();
				long checksum = 0L;
				List<Sample> results = new ArrayList<>();
				for (int round = 0; round < 8; round++) {
					boolean firstTransport = (round & 1) == 0;
					for (boolean enabled : new boolean[] { firstTransport, !firstTransport }) {
						System.setProperty(TRANSPORT, Boolean.toString(enabled));
						Sample sample = sample(benchmark, allocation, enabled);
						if (checksum == 0L) {
							checksum = sample.checksum;
						}
						assertThat(sample.checksum).isEqualTo(checksum);
						if (round >= 2) {
							results.add(sample);
							System.out.printf(Locale.ROOT,
									"ABLAT sample round=%d transport=%s elapsedMs=%.3f prepareMs=%.3f "
											+ "evaluateMs=%.3f threadAllocatedBytes=%d lazyCalls=%d "
											+ "factorizedRows=%d checksum=%d routes=%s%n",
									round - 2, enabled, sample.milliseconds(),
									sample.preparationNanos / 1_000_000.0,
									sample.evaluationNanos / 1_000_000.0,
									sample.threadAllocationBytes, sample.lazyCalls, sample.factorizedRows,
									sample.checksum, sample.routes);
						}
					}
				}
				for (boolean enabled : new boolean[] { false, true }) {
					List<Sample> arm = results.stream().filter(s -> s.transport == enabled)
							.sorted(Comparator.comparingLong(Sample::nanos)).toList();
					assertThat(arm).hasSize(6);
					double medianMs = (arm.get(2).milliseconds() + arm.get(3).milliseconds()) / 2.0;
					double avgMs = arm.stream().mapToDouble(Sample::milliseconds).average().orElseThrow();
					double medianAllocatedMb = (arm.get(2).threadAllocationBytes + arm.get(3).threadAllocationBytes)
							/ (2.0 * 1_048_576.0);
					System.out.printf(Locale.ROOT,
							"ABLAT summary transport=%s trials=%d medianMs=%.3f meanMs=%.3f "
									+ "medianByLatencyThreadAllocatedMiB=%.3f%n",
							enabled, arm.size(), medianMs, avgMs, medianAllocatedMb);
				}
			} finally {
				benchmark.tearDown();
			}
		} finally {
			if (prior == null) {
				System.clearProperty(TRANSPORT);
			} else {
				System.setProperty(TRANSPORT, prior);
			}
		}
	}

	private static Sample sample(OptionalFanoutBenchmark benchmark, com.sun.management.ThreadMXBean allocation,
			boolean transport) {
		long threadId = Thread.currentThread().threadId();
		long allocatedBefore = allocation == null ? -1L : allocation.getThreadAllocatedBytes(threadId);
		long lazyBefore = LmdbNativeExpressionCompiler.LAZY_VALUE_CALLS.get();
		long factorBefore = OptionalFanoutRouteAccess.factorizedRows();
		BenchmarkRouteTelemetry.Snapshot routesBefore = BenchmarkRouteTelemetry.snapshot();
		OptionalFanoutBenchmark.Timings timings = new OptionalFanoutBenchmark.Timings();
		long started = System.nanoTime();
		long checksum = benchmark.runQueryTimed(timings);
		long elapsed = System.nanoTime() - started;
		long allocatedAfter = allocation == null ? -1L : allocation.getThreadAllocatedBytes(threadId);
		return new Sample(transport, checksum, elapsed, timings.preparationNanos, timings.evaluationNanos,
				allocatedBefore < 0 || allocatedAfter < 0 ? -1L : allocatedAfter - allocatedBefore,
				LmdbNativeExpressionCompiler.LAZY_VALUE_CALLS.get() - lazyBefore,
				OptionalFanoutRouteAccess.factorizedRows() - factorBefore,
				BenchmarkRouteTelemetry.snapshot().minus(routesBefore).toString());
	}

	private static com.sun.management.ThreadMXBean allocationBean() {
		if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean
				&& bean.isThreadAllocatedMemorySupported()) {
			bean.setThreadAllocatedMemoryEnabled(true);
			return bean;
		}
		return null;
	}
}
