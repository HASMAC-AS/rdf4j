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

package org.eclipse.rdf4j.sparqlbuilder.rdf;

import java.util.Objects;

/**
 * Represents an embedded RDF-star triple.
 */
class RdfTriple implements RdfResource {

	private final RdfSubject subject;
	private final RdfPredicate predicate;
	private final RdfObject object;

	RdfTriple(RdfSubject subject, RdfPredicate predicate, RdfObject object) {
		this.subject = Objects.requireNonNull(subject, "subject");
		this.predicate = Objects.requireNonNull(predicate, "predicate");
		this.object = Objects.requireNonNull(object, "object");
	}

	@Override
	public String getQueryString() {
		return "<<" + subject.getQueryString() + " " + predicate.getQueryString() + " " + object.getQueryString()
				+ ">>";
	}
}
