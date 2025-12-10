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
package org.eclipse.rdf4j.sail.lmdb;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to extract LMDB-specific dataset details from a {@link TripleSource}.
 */
public final class DatasetIntrospection {

	private static final Logger log = LoggerFactory.getLogger(DatasetIntrospection.class);

	private DatasetIntrospection() {
	}

	public static List<LmdbDatasetSnapshot> tryExtractDataset(TripleSource tripleSource) {
		if (tripleSource == null) {
			return List.of();
		}
		if (tripleSource instanceof org.eclipse.rdf4j.sail.lmdb.LmdbDatasetProvider) {
			LmdbDatasetSnapshot snapshot = ((org.eclipse.rdf4j.sail.lmdb.LmdbDatasetProvider) tripleSource)
					.getLmdbDatasetSnapshot();
			return snapshot != null ? List.of(snapshot) : List.of();
		}

		if (tripleSource instanceof SailDatasetTripleSource) {
			try {
				Field datasetField = SailDatasetTripleSource.class.getDeclaredField("dataset");
				datasetField.setAccessible(true);
				Object dataset = datasetField.get(tripleSource);
				List<LmdbDatasetSnapshot> snapshots = collectSnapshots(dataset);
				if (!snapshots.isEmpty()) {
					return snapshots;
				}
			} catch (ReflectiveOperationException ignored) {
				log.warn("Could not access dataset field on SailDatasetTripleSource", ignored);
				// fall through to empty list
			}
		}
		return List.of();
	}

	private static List<LmdbDatasetSnapshot> collectSnapshots(Object dataset) throws ReflectiveOperationException {
		if (dataset == null) {
			return List.of();
		}
		if (dataset instanceof LmdbDatasetSnapshot) {
			return List.of((LmdbDatasetSnapshot) dataset);
		}
		if (dataset instanceof org.eclipse.rdf4j.sail.lmdb.LmdbDatasetProvider) {
			LmdbDatasetSnapshot snapshot = ((org.eclipse.rdf4j.sail.lmdb.LmdbDatasetProvider) dataset)
					.getLmdbDatasetSnapshot();
			return snapshot != null ? List.of(snapshot) : List.of();
		}

		Class<?> clazz = dataset.getClass();

		if ("org.eclipse.rdf4j.sail.base.UnionSailDataset".equals(clazz.getName())) {
			Field left = clazz.getDeclaredField("dataset1");
			Field right = clazz.getDeclaredField("dataset2");
			left.setAccessible(true);
			right.setAccessible(true);
			List<LmdbDatasetSnapshot> snapshots = new ArrayList<>();
			snapshots.addAll(collectSnapshots(left.get(dataset)));
			snapshots.addAll(collectSnapshots(right.get(dataset)));
			return snapshots;
		}

		for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
			try {
				Field delegate = current.getDeclaredField("delegate");
				delegate.setAccessible(true);
				return collectSnapshots(delegate.get(dataset));
			} catch (NoSuchFieldException ignored) {
				// keep walking the hierarchy
			}
		}
		return List.of();
	}
}
