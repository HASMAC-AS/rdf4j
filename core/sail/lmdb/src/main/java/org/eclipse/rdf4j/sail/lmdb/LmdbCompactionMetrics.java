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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Metrics describing a compaction run.
 */
public final class LmdbCompactionMetrics {

	private final Duration copyDuration;
	private final List<DatabaseStats> databases;

	public LmdbCompactionMetrics(Duration copyDuration, List<DatabaseStats> databases) {
		this.copyDuration = Objects.requireNonNull(copyDuration, "copyDuration");
		this.databases = List.copyOf(Objects.requireNonNull(databases, "databases"));
	}

	public Duration getCopyDuration() {
		return copyDuration;
	}

	public List<DatabaseStats> getDatabases() {
		return Collections.unmodifiableList(databases);
	}

	public static final class DatabaseStats {

		private final String name;
		private final long entriesBefore;
		private final long entriesCopied;
		private final long pagesBefore;
		private final long pagesAfter;

		public DatabaseStats(String name, long entriesBefore, long entriesCopied, long pagesBefore,
				long pagesAfter) {
			this.name = Objects.requireNonNull(name, "name");
			this.entriesBefore = entriesBefore;
			this.entriesCopied = entriesCopied;
			this.pagesBefore = pagesBefore;
			this.pagesAfter = pagesAfter;
		}

		public String name() {
			return name;
		}

		public long entriesBefore() {
			return entriesBefore;
		}

		public long entriesCopied() {
			return entriesCopied;
		}

		public long pagesBefore() {
			return pagesBefore;
		}

		public long pagesAfter() {
			return pagesAfter;
		}
	}
}
