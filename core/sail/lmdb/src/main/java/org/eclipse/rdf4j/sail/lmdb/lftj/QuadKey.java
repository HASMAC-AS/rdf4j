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

import java.util.Objects;

/**
 * Immutable tuple representing a quad key with subject, predicate, object, and context identifiers.
 */
public final class QuadKey {
	private final long s;
	private final long p;
	private final long o;
	private final long c;

	public QuadKey(long s, long p, long o, long c) {
		this.s = s;
		this.p = p;
		this.o = o;
		this.c = c;
	}

	public long s() {
		return s;
	}

	public long p() {
		return p;
	}

	public long o() {
		return o;
	}

	public long c() {
		return c;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof QuadKey)) {
			return false;
		}
		QuadKey quadKey = (QuadKey) o;
		return s == quadKey.s && p == quadKey.p && this.o == quadKey.o && c == quadKey.c;
	}

	@Override
	public int hashCode() {
		return Objects.hash(s, p, o, c);
	}

	@Override
	public String toString() {
		return "QuadKey{" + "s=" + s + ", p=" + p + ", o=" + o + ", c=" + c + '}';
	}
}
