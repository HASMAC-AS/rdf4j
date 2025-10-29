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

import java.util.Objects;

/**
 * Progress event produced while compaction work is running.
 */
public final class LmdbCompactionProgress {

	public enum Stage {
		PREPARE,
		COPY_VALUES,
		COPY_TRIPLES,
		VERIFY,
		SWAP,
		COMPLETE
	}

	private final Stage stage;
	private final String detail;
	private final double fraction;

	public LmdbCompactionProgress(Stage stage, String detail, double fraction) {
		this.stage = Objects.requireNonNull(stage, "stage");
		this.detail = detail == null ? "" : detail;
		this.fraction = fraction;
	}

	public Stage stage() {
		return stage;
	}

	public String detail() {
		return detail;
	}

	public double fraction() {
		return fraction;
	}
}
