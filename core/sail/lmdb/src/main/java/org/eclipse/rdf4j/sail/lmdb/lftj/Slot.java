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

/**
 * Logical slots for quad components. The ordering of these slots inside a {@link QuadKeyOrder} determines how quad IDs
 * are laid out in LMDB index keys.
 */
public enum Slot {
	S,
	P,
	O,
	C
}
