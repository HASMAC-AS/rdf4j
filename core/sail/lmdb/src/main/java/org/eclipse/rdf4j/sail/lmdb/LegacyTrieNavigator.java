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

/**
 * Minimal adapter that exposes the value/seek/next surface of a single-level {@link TrieLevelCursor} via
 * {@link JoinCursor} semantics. Used only to unify handling of explicit/inferred cursors when mixing backends.
 */
final class LegacyTrieNavigator {

	private final TrieLevelCursor cursor;

	LegacyTrieNavigator(TrieLevelCursor cursor) {
		this.cursor = cursor;
	}

	boolean next() throws IOException {
		return cursor.next();
	}

	boolean seek(long target) throws IOException {
		return cursor.seek(target);
	}

	long key() {
		return cursor.key();
	}

	boolean atEnd() {
		return cursor.atEnd();
	}

	void close() {
		cursor.close();
	}
}
