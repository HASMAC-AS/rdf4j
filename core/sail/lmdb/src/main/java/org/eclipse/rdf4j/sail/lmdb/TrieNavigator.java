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
 * Backend-neutral navigation interface for tries used by WCOJ.
 */
interface TrieNavigator extends AutoCloseable {

	/** Position at the root (no prefix). */
	void openRoot() throws IOException;

	/** Position at the child domain defined by the given prefix (level components). Returns false if prefix absent. */
	boolean openPrefix(long... prefix) throws IOException;

	/** Move to next value in current level domain; false if exhausted. */
	boolean next() throws IOException;

	/** Seek to smallest value >= target within current domain; false if past end. */
	boolean seek(long target) throws IOException;

	/** Current value at the active level. */
	long key();

	/** True if current domain is exhausted. */
	boolean atEnd();

	/** Descend to children of current key, adjusting internal domain to the next level. */
	void descend() throws IOException;

	/** Ascend back one level restoring previous range; used for backtracking. */
	void ascend();

	/** Current depth (0=root level values[0], etc.). */
	int level();

	@Override
	void close();
}
