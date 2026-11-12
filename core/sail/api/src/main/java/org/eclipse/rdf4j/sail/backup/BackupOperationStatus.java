/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.backup;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * Immutable, point-in-time description of one executing backup-service operation. Phases describe work, not a
 * percentage estimate. The ID and start time remain stable when the phase changes. This is process-local status, not a
 * durable job history.
 */
@Experimental
public final class BackupOperationStatus {
	private final String operationId;
	private final String operation;
	private final Optional<BackupType> backupType;
	private final Instant startedAt;
	private final String phase;

	public BackupOperationStatus(String operationId, String operation, Optional<BackupType> backupType,
			Instant startedAt, String phase) {
		this.operationId = Objects.requireNonNull(operationId, "operationId");
		this.operation = Objects.requireNonNull(operation, "operation");
		this.backupType = Objects.requireNonNull(backupType, "backupType");
		this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
		this.phase = Objects.requireNonNull(phase, "phase");
	}

	public String getOperationId() {
		return operationId;
	}

	/** BACKUP or RESTORE. Consumers should tolerate future operation names. */
	public String getOperation() {
		return operation;
	}

	public Optional<BackupType> getBackupType() {
		return backupType;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	/** PREPARING, COPYING, COMPRESSING, CHECKSUMMING, VERIFYING, RESTORING, REPLAYING, or PUBLISHING. */
	public String getPhase() {
		return phase;
	}
}
