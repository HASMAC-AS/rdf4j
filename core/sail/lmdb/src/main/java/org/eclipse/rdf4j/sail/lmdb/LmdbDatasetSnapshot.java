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

import java.util.Map;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyOrder;

/**
 * Snapshot-specific accessors exposed by {@link LmdbSailStore} datasets to LMDB-aware evaluation components.
 */
interface LmdbDatasetSnapshot {

	Txn getTxn();

	Map<QuadKeyOrder, Integer> indexHandles();

	ValueStore valueStore();

	boolean isExplicit();
}
