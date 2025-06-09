/*****************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *****************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.model.Value;

final class PredStats {

	private final Sketch subjectSketch = new HllAgknAdapter();
	private final Sketch objectSketch = new HllAgknAdapter();
	private long count;

	void update(Value subj, Value obj) {
		subjectSketch.add(HashFunctions.hash64(subj.stringValue()));
		objectSketch.add(HashFunctions.hash64(obj.stringValue()));
		count++;
	}

	long getCount() {
		return count;
	}

	Sketch getSubjectSketch() {
		return subjectSketch;
	}

	Sketch getObjectSketch() {
		return objectSketch;
	}
}
