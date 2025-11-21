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
 * Immutable holder for the four internal long identifiers that represent a quad in LMDB storage.
 */
final class IdQuad {
	final long s;
	final long p;
	final long o;
	final long c;

	IdQuad(long s, long p, long o, long c) {
		this.s = s;
		this.p = p;
		this.o = o;
		this.c = c;
	}
}
