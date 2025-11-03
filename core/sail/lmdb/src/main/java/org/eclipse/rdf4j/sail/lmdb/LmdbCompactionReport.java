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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result payload for a completed compaction run.
 */
public final class LmdbCompactionReport {

	private final LmdbCompactionMetrics metrics;
	private final Path backupDirectory;
	private final Path compactedDirectory;
	private final boolean verificationAttempted;
	private final boolean verificationSucceeded;
	private final List<String> warnings;

	public LmdbCompactionReport(LmdbCompactionMetrics metrics, Path backupDirectory, Path compactedDirectory,
			boolean verificationAttempted, boolean verificationSucceeded, List<String> warnings) {
		this.metrics = Objects.requireNonNull(metrics, "metrics");
		this.backupDirectory = backupDirectory;
		this.compactedDirectory = compactedDirectory;
		this.verificationAttempted = verificationAttempted;
		this.verificationSucceeded = verificationSucceeded;
		this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
	}

	public LmdbCompactionMetrics getMetrics() {
		return metrics;
	}

	public Optional<Path> getBackupDirectory() {
		return Optional.ofNullable(backupDirectory);
	}

	public Optional<Path> getCompactedDirectory() {
		return Optional.ofNullable(compactedDirectory);
	}

	public boolean isVerificationAttempted() {
		return verificationAttempted;
	}

	public boolean isVerificationSucceeded() {
		return verificationSucceeded;
	}

	public List<String> getWarnings() {
		return warnings;
	}
}
