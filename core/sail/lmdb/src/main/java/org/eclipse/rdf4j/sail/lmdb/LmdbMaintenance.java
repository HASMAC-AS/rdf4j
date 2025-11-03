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

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;

/**
 * Convenience entry point for maintenance activities on LMDB data directories.
 */
public final class LmdbMaintenance {

	private final LmdbStore store;

	public LmdbMaintenance(LmdbStore store) {
		this.store = Objects.requireNonNull(store, "store");
	}

	public Optional<LmdbCompactionReport> compactIfNeeded(LmdbCompactionOptions options) throws IOException {
		Objects.requireNonNull(options, "options");
		LmdbStoreConfig config = store.getStoreConfig();
		if (!config.isCompactionAutoEnabled()) {
			return Optional.empty();
		}
		return store.compactIfNeeded(options, config.getCompactionFragmentationThreshold());
	}
}
