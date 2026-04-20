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

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a compaction run.
 */
public final class LmdbCompactionResult {

	private final long bytesBefore;
	private final long bytesAfter;
	private final Path compactedDirectory;
	private final Path backupDirectory;
	private final LmdbCompactionMetrics metrics;

	LmdbCompactionResult(long bytesBefore, long bytesAfter, Path compactedDirectory, Path backupDirectory,
			LmdbCompactionMetrics metrics) {
		this.bytesBefore = bytesBefore;
		this.bytesAfter = bytesAfter;
		this.compactedDirectory = Objects.requireNonNull(compactedDirectory, "compactedDirectory");
		this.backupDirectory = backupDirectory;
		this.metrics = Objects.requireNonNull(metrics, "metrics");
	}

	public long getBytesBefore() {
		return bytesBefore;
	}

	public long getBytesAfter() {
		return bytesAfter;
	}

	public Path getCompactedDirectory() {
		return compactedDirectory;
	}

	public Optional<Path> getBackupDirectory() {
		return Optional.ofNullable(backupDirectory);
	}

	public LmdbCompactionMetrics getMetrics() {
		return metrics;
	}

	public long getBytesFreed() {
		return Math.max(0, bytesBefore - bytesAfter);
	}
}
