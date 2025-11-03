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

/**
 * Listener that receives progress callbacks during LMDB environment compaction.
 */
@FunctionalInterface
public interface LmdbCompactionProgressListener {

	/**
	 * Invoked whenever a compaction phase makes progress.
	 *
	 * @param progress details about the current state of the compaction run.
	 */
	void onProgress(LmdbCompactionProgress progress);
}
