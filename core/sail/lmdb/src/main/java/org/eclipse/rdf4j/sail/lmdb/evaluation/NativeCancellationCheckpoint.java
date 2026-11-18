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
package org.eclipse.rdf4j.sail.lmdb.evaluation;

/**
 * Signals captured at a co-scan execution boundary, never on a reusable plan or source. Ambient scopes remain installed
 * for callbacks; polling these references does not consult or modify a callback's thread-local state.
 */
final class NativeCancellationCheckpoint {
	private final NativeCancellationToken query;
	private final LmdbNativeProbeDeadline probe;

	NativeCancellationCheckpoint(NativeCancellationToken query, LmdbNativeProbeDeadline probe) {
		this.query = query;
		this.probe = probe;
	}

	static NativeCancellationCheckpoint capture() {
		return new NativeCancellationCheckpoint(NativeCancellationToken.current(),
				LmdbNativeProbeDeadline.currentOrNull());
	}

	/**
	 * Resolve absent dispatch signals once, after worker scopes are installed. A null inherit is a no-op, so the
	 * effective ambient signal can still be non-null. Explicit dispatch signals retain precedence.
	 */
	static NativeCancellationCheckpoint captureMissing(NativeCancellationToken query, LmdbNativeProbeDeadline probe) {
		return new NativeCancellationCheckpoint(query != null ? query : NativeCancellationToken.current(),
				probe != null ? probe : LmdbNativeProbeDeadline.currentOrNull());
	}

	/** Preserve the co-scan's 1024-tick cadence and probe-before-query exception priority. */
	void poll(int tick) {
		if ((tick & 1023) == 0) {
			if (probe != null && probe.expired()) {
				throw LmdbNativeProbeDeadlineExceeded.INSTANCE;
			}
			checkQuery();
		}
	}

	void checkQuery() {
		if (query != null) {
			query.check();
		}
	}
}
