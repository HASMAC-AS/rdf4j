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
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.data.Offset;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbStatisticsPenaltyTest {

	private static final long TYPE = 2L;
	private static final long PERSON = 100L;
	private static final long CONTEXT = 1L;

	private static final int MATCH_COUNT = 5;
	private static final int TOTAL_STATEMENTS = 405;

	@TempDir
	File tempDir;

	@Test
	void nonOptimalIndexHasHigherCardinalityEstimate() throws Exception {
		double optimal = computeCardinality("spoc,posc");
		double nonOptimal = computeCardinality("psoc");

		assertThat(optimal).isCloseTo(MATCH_COUNT, withinTolerance());
		assertThat(nonOptimal).isGreaterThan(optimal);
		assertThat(nonOptimal).isGreaterThanOrEqualTo(TOTAL_STATEMENTS);
	}

	private double computeCardinality(String indexSpec) throws Exception {
		File dataDir = new File(tempDir, indexSpec.replace(',', '_'));
		dataDir.mkdir();

		List<long[]> statements = createStatements();

		TripleStore store = new TripleStore(dataDir, new LmdbStoreConfig(indexSpec), null);
		try {
			store.startTransaction();
			for (long[] st : statements) {
				store.storeTriple(st[0], st[1], st[2], st[3], true);
			}
			store.commit();

			return store.cardinality(LmdbValue.UNKNOWN_ID, TYPE, PERSON, LmdbValue.UNKNOWN_ID);
		} finally {
			store.close();
		}
	}

	private List<long[]> createStatements() {
		List<long[]> statements = new ArrayList<>();

		for (int i = 0; i < MATCH_COUNT; i++) {
			statements.add(new long[] { i + 1, TYPE, PERSON, CONTEXT });
		}

		for (int i = 0; i < 400; i++) {
			statements.add(new long[] { i + 100, TYPE, 1_000 + i, CONTEXT });
		}

		for (int i = 0; i < 200; i++) {
			statements.add(new long[] { i + 500, TYPE + 100 + (i % 3), PERSON, CONTEXT });
		}

		return statements;
	}

	private Offset<Double> withinTolerance() {
		return Offset.offset(1.0d);
	}
}
