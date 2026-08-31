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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Describes the positional order of quad components inside an index key.
 */
public final class QuadKeyOrder {
	private static final int KEY_LENGTH = 4;

	private final Slot[] positions;

	private QuadKeyOrder(Slot[] positions) {
		this.positions = positions;
	}

	public static QuadKeyOrder of(Slot... positions) {
		Objects.requireNonNull(positions, "positions");
		if (positions.length != KEY_LENGTH) {
			throw new IllegalArgumentException("QuadKeyOrder must contain exactly four slots");
		}
		EnumSet<Slot> unique = EnumSet.noneOf(Slot.class);
		unique.addAll(Arrays.asList(positions));
		if (unique.size() != KEY_LENGTH) {
			throw new IllegalArgumentException("QuadKeyOrder must contain each slot exactly once");
		}
		return new QuadKeyOrder(Arrays.copyOf(positions, positions.length));
	}

	public List<Slot> positions() {
		return List.copyOf(Arrays.asList(positions));
	}

	public Slot positionAt(int index) {
		return positions[index];
	}

	public int indexOf(Slot slot) {
		for (int i = 0; i < positions.length; i++) {
			if (positions[i] == slot) {
				return i;
			}
		}
		return -1;
	}
}
