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

	public static Object tryExtractDataset(TripleSource tripleSource) {
		if (tripleSource == null) {
			return null;
		}
		if (tripleSource instanceof org.eclipse.rdf4j.sail.lmdb.LmdbDatasetProvider) {
			return ((org.eclipse.rdf4j.sail.lmdb.LmdbDatasetProvider) tripleSource).getLmdbDatasetSnapshot();
		}

		if (tripleSource instanceof SailDatasetTripleSource) {
			try {
				Field datasetField = SailDatasetTripleSource.class.getDeclaredField("dataset");
				datasetField.setAccessible(true);
				Object dataset = datasetField.get(tripleSource);
				Object unwrapped = unwrapDataset(dataset);
				if (unwrapped != null) {
					return unwrapped;
				}
			} catch (ReflectiveOperationException ignored) {
				log.warn("Could not access dataset field on SailDatasetTripleSource", ignored);
				// fall through to null
			}
		}
		return null;
	}

	private static Object unwrapDataset(Object dataset) throws ReflectiveOperationException {
		if (dataset == null) {
			return null;
		}
		if (hasMethod(dataset, "getTxn") && hasMethod(dataset, "indexHandles") && hasMethod(dataset, "valueStore")) {
			return dataset;
		}

		Class<?> clazz = dataset.getClass();

		if ("org.eclipse.rdf4j.sail.base.UnionSailDataset".equals(clazz.getName())) {
			Field left = clazz.getDeclaredField("dataset1");
			Field right = clazz.getDeclaredField("dataset2");
			left.setAccessible(true);
			right.setAccessible(true);
			Object unwrapped = unwrapDataset(left.get(dataset));
			if (unwrapped != null) {
				return unwrapped;
			}
			return unwrapDataset(right.get(dataset));
		}

		for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
			try {
				Field delegate = current.getDeclaredField("delegate");
				delegate.setAccessible(true);
				return unwrapDataset(delegate.get(dataset));
			} catch (NoSuchFieldException ignored) {
				// keep walking the hierarchy
			}
		}
		return null;
	}

	private static boolean hasMethod(Object target, String name) {
		if (target == null) {
			return false;
		}
		try {
			target.getClass().getMethod(name);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
}
