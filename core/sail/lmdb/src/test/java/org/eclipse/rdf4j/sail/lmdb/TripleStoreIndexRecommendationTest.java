/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TripleStoreIndexRecommendationTest {

	@TempDir
	Path tempDir;

	private ConcurrentHashMap<String, LongAdder> recommendationCounters;

	@BeforeEach
	void resetRecommendationCounters() throws Exception {
		recommendationCounters = extractRecommendationCounters();
		recommendationCounters.clear();
	}

	@ParameterizedTest(name = "{0}: {1}")
	@MethodSource("recommendationCases")
	void recommendedIndexesMatchExpectations(String id, String description, BoundPattern pattern,
			List<String> existingIndexes, List<String> expectedPrefixes, List<String> expectedRecommendations)
			throws Exception {
		Path storeDir = Files.createTempDirectory(tempDir, id + "-");

		try (TripleStore tripleStore = createTripleStore(storeDir, existingIndexes);
				Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iterator = tripleStore.getTriples(txn, pattern.subj(), pattern.pred(),
						pattern.obj(), pattern.context(), true)) {
			List<String> recommendations = iterator.getRecommendedIndexes();

			assertThat(recommendations).containsExactlyElementsOf(expectedRecommendations);
			assertThat(recommendations).doesNotContainAnyElementsOf(existingIndexes);
			assertThat(recommendations).allSatisfy(index -> assertThat(matchesAnyPrefix(index, expectedPrefixes))
					.as("Index %s should start with one of %s", index, expectedPrefixes)
					.isTrue());
		}
	}

	@Test
	void recommendationsPreferFrequentlySuggestedIndexes() throws Exception {
		recommendationCounters.computeIfAbsent("OCPS", key -> new LongAdder()).add(5);

		BoundPattern ocBound = new BoundPattern(LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, 1L, 1L);
		Path storeDir = Files.createTempDirectory(tempDir, "freq-");

		try (TripleStore tripleStore = createTripleStore(storeDir, List.of("SPOC", "PSOC", "OPSC"));
				Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator iterator = tripleStore.getTriples(txn, ocBound.subj(), ocBound.pred(),
						ocBound.obj(), ocBound.context(), true)) {
			List<String> recommendations = iterator.getRecommendedIndexes();
			assertThat(recommendations)
					.containsExactly("OCPS", "COPS", "COSP", "OCSP");
		}
	}

	private boolean matchesAnyPrefix(String index, List<String> prefixes) {
		return prefixes.stream().anyMatch(index::startsWith);
	}

	private TripleStore createTripleStore(Path storeDir, List<String> existingIndexes)
			throws IOException, SailException {
		String indexSpec = existingIndexes.stream()
				.map(s -> s.toLowerCase(Locale.ROOT))
				.collect(Collectors.joining(","));
		return new TripleStore(storeDir.toFile(), new LmdbStoreConfig(indexSpec), null);
	}

	private ConcurrentHashMap<String, LongAdder> extractRecommendationCounters() throws Exception {
		Field field = TripleStore.class.getDeclaredField("RECOMMENDED_INDEX_COUNTS");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		ConcurrentHashMap<String, LongAdder> counters = (ConcurrentHashMap<String, LongAdder>) field.get(null);
		return counters;
	}

	private static Stream<Arguments> recommendationCases() {
		return Stream.of(
				Arguments.of("N01", "S bound only", pattern(true, false, false, false),
						List.of("POSC", "OPSC", "CPOS", "COPS"), List.of("S"),
						List.of("SCOP", "SCPO", "SOCP", "SOPC", "SPCO", "SPOC")),
				Arguments.of("N02", "P bound only", pattern(false, true, false, false),
						List.of("SPOC", "OSPC", "CSPO", "OCSP"), List.of("P"),
						List.of("PCOS", "PCSO", "POCS", "POSC", "PSCO", "PSOC")),
				Arguments.of("N03", "O bound only", pattern(false, false, true, false),
						List.of("SPOC", "PSOC", "CSPO", "COPS"), List.of("O"),
						List.of("OCPS", "OCSP", "OPCS", "OPSC", "OSCP", "OSPC")),
				Arguments.of("N04", "C bound only (graph fixed)", pattern(false, false, false, true),
						List.of("SPOC", "PSOC", "OSPC", "OPSC"), List.of("C"),
						List.of("COPS", "COSP", "CPOS", "CPSO", "CSOP", "CSPO")),
				Arguments.of("N05", "S and P bound", pattern(true, true, false, false),
						List.of("COPS", "OPSC", "OCSP"), List.of("SP", "PS"),
						List.of("PSCO", "PSOC", "SPCO", "SPOC")),
				Arguments.of("N06", "S and O bound", pattern(true, false, true, false),
						List.of("SPOC", "SPCO", "COPS"), List.of("SO", "OS"),
						List.of("OSCP", "OSPC", "SOCP", "SOPC")),
				Arguments.of("N07", "S and C bound", pattern(true, false, false, true),
						List.of("SPOC", "OPSC", "POCS"), List.of("SC", "CS"),
						List.of("CSOP", "CSPO", "SCOP", "SCPO")),
				Arguments.of("N08", "P and O bound", pattern(false, true, true, false),
						List.of("SPOC", "SCOP", "CPSO"), List.of("PO", "OP"),
						List.of("OPCS", "OPSC", "POCS", "POSC")),
				Arguments.of("N09", "P and C bound", pattern(false, true, false, true),
						List.of("SPOC", "OSPC", "SCOP"), List.of("PC", "CP"),
						List.of("CPOS", "CPSO", "PCOS", "PCSO")),
				Arguments.of("N10", "O and C bound", pattern(false, false, true, true),
						List.of("SPOC", "PSOC", "SPCO"), List.of("OC", "CO"),
						List.of("COPS", "COSP", "OCPS", "OCSP")),
				Arguments.of("N11", "S,P,O bound; C unbound", pattern(true, true, true, false),
						List.of("SCPO", "PCSO", "COPS"),
						List.of("SPO", "SOP", "PSO", "POS", "OSP", "OPS"),
						List.of("OPSC", "OSPC", "POSC", "PSOC", "SOPC", "SPOC")),
				Arguments.of("N12", "C,S,P bound; O unbound", pattern(true, true, false, true),
						List.of("SPOC", "OCSP", "OPCS"),
						List.of("CSP", "CPS", "SCP", "SPC", "PCS", "PSC"),
						List.of("CPSO", "CSPO", "PCSO", "PSCO", "SCPO", "SPCO")),
				Arguments.of("N13", "C,S,O bound; P unbound", pattern(true, false, true, true),
						List.of("SPOC", "PCSO", "COPS"),
						List.of("CSO", "COS", "SCO", "SOC", "OCS", "OSC"),
						List.of("COSP", "CSOP", "OCSP", "OSCP", "SCOP", "SOCP")),
				Arguments.of("N14", "C,P,O bound; S unbound", pattern(false, true, true, true),
						List.of("SPOC", "SPCO", "CSPO"),
						List.of("CPO", "COP", "PCO", "POC", "OCP", "OPC"),
						List.of("COPS", "CPOS", "OCPS", "OPCS", "PCOS", "POCS"))
		);
	}

	private static BoundPattern pattern(boolean subjBound, boolean predBound, boolean objBound, boolean contextBound) {
		long subj = subjBound ? 1L : LmdbValue.UNKNOWN_ID;
		long pred = predBound ? 1L : LmdbValue.UNKNOWN_ID;
		long obj = objBound ? 1L : LmdbValue.UNKNOWN_ID;
		long context = contextBound ? 1L : LmdbValue.UNKNOWN_ID;
		return new BoundPattern(subj, pred, obj, context);
	}

	private static final class BoundPattern {
		private final long subj;
		private final long pred;
		private final long obj;
		private final long context;

		private BoundPattern(long subj, long pred, long obj, long context) {
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.context = context;
		}

		long subj() {
			return subj;
		}

		long pred() {
			return pred;
		}

		long obj() {
			return obj;
		}

		long context() {
			return context;
		}
	}
}
