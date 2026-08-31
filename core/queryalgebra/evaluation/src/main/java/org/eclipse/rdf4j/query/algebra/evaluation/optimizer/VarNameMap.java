/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Query-local mapping from public binding names to dense integer ids.
 * <p>
 * This deliberately preserves the current RDF4J string based public API at the boundary, while allowing optimizer code
 * to do repeated variable set operations as bit operations.
 */
final class VarNameMap {

	private final Map<String, Integer> ids = new LinkedHashMap<>();
	private final List<String> names = new ArrayList<>();

	int idOf(String name) {
		Objects.requireNonNull(name, "name must not be null");

		Integer id = ids.get(name);
		if (id == null) {
			id = names.size();
			ids.put(name, id);
			names.add(name);
		}
		return id;
	}

	String nameOf(int id) {
		if (id < 0 || id >= names.size()) {
			throw new IndexOutOfBoundsException("Unknown variable id: " + id);
		}
		return names.get(id);
	}

	VarNameMask maskOf(Iterable<String> names) {
		Objects.requireNonNull(names, "names must not be null");

		VarNameMask.Builder builder = VarNameMask.builder();
		for (String name : names) {
			builder.add(idOf(name));
		}
		return builder.build();
	}

	int size() {
		return names.size();
	}
}
