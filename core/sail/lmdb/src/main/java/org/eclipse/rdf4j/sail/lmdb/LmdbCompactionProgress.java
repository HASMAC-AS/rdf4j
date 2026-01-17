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

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable progress payload emitted during LMDB compaction.
 */
public final class LmdbCompactionProgress {

	public enum Phase {
		/** The compaction request has been accepted and validation is in progress. */
		VALIDATING,
		/** All LMDB environments are currently being copied to the staging location. */
		COPYING_ENVIRONMENTS,
		/** Verification of the staged environment is running. */
		VERIFYING,
		/** The compacted environment is being swapped into place. */
		SWAPPING,
		/** Final clean-up after a successful compaction. */
		FINISHED
	}

	private final Phase phase;
	private final String message;
	private final long completed;
	private final long total;
	private final Instant timestamp;

	private LmdbCompactionProgress(Phase phase, String message, long completed, long total, Instant timestamp) {
		this.phase = Objects.requireNonNull(phase, "phase");
		this.message = message;
		this.completed = completed;
		this.total = total;
		this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
	}

	public static LmdbCompactionProgress of(Phase phase, String message) {
		return new LmdbCompactionProgress(phase, message, -1, -1, Instant.now());
	}

	public static LmdbCompactionProgress of(Phase phase, long completed, long total, String message) {
		return new LmdbCompactionProgress(phase, message, completed, total, Instant.now());
	}

	public Phase getPhase() {
		return phase;
	}

	public Optional<String> getMessage() {
		return Optional.ofNullable(message);
	}

	public Optional<Long> getCompleted() {
		return completed >= 0 ? Optional.of(completed) : Optional.empty();
	}

	public Optional<Long> getTotal() {
		return total >= 0 ? Optional.of(total) : Optional.empty();
	}

	public Instant getTimestamp() {
		return timestamp;
	}
}
