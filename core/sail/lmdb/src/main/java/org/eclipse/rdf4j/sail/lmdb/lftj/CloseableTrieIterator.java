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
package org.eclipse.rdf4j.sail.lmdb.lftj;

import java.io.Closeable;

public interface CloseableTrieIterator extends TrieIterator, Closeable {

	@Override
	void close();

	Slot slot();

	default int slotDbi() {
		throw new UnsupportedOperationException("DBI not exposed");
	}

	default QuadKeyOrder slotOrder() {
		throw new UnsupportedOperationException("Order not exposed");
	}
}
