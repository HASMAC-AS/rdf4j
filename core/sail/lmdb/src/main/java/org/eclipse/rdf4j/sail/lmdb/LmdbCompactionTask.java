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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Convenience wrapper that executes LMDB compaction asynchronously.
 */
public final class LmdbCompactionTask implements Callable<LmdbCompactionReport> {

	private final LmdbStore store;
	private final LmdbCompactionOptions options;

	public LmdbCompactionTask(LmdbStore store, LmdbCompactionOptions options) {
		this.store = Objects.requireNonNull(store, "store");
		this.options = Objects.requireNonNull(options, "options");
	}

	@Override
	public LmdbCompactionReport call() throws IOException {
		return store.compact(options);
	}

	public CompletableFuture<LmdbCompactionReport> submit(Executor executor) {
		CompletableFuture<LmdbCompactionReport> future = new CompletableFuture<>();
		executor.execute(() -> {
			try {
				future.complete(call());
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		return future;
	}
}
