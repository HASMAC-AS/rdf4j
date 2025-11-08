/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

/**
 * Provides reusable flag-based filters for statement visibility so that the filters can be shared across iterators
 * without re-evaluating the mask logic on every record.
 */
final class StatementFlagMatcher {

	@FunctionalInterface
	private interface FlagPredicate {
		boolean test(byte flags);
	}

	private static final StatementFlagMatcher EXPLICIT = new StatementFlagMatcher(flags -> {
		boolean explicit = (flags & TripleStore.EXPLICIT_FLAG) != 0;
		boolean toggled = (flags & TripleStore.TOGGLE_EXPLICIT_FLAG) != 0;
		return explicit != toggled;
	});

	private static final StatementFlagMatcher IMPLICIT = new StatementFlagMatcher(
			flags -> (flags & TripleStore.EXPLICIT_FLAG) == 0);

	private final FlagPredicate predicate;

	private StatementFlagMatcher(FlagPredicate predicate) {
		this.predicate = predicate;
	}

	boolean matches(byte flags) {
		return predicate.test(flags);
	}

	static StatementFlagMatcher explicitFilter() {
		return EXPLICIT;
	}

	static StatementFlagMatcher implicitFilter() {
		return IMPLICIT;
	}
}
