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
package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.rdf4j.sail.nativerdf.btree.RecordComparator;
import org.junit.jupiter.api.Test;

class TripleComparatorTest {

	@Test
	void rejectsUnknownFieldSequence() {
		assertThatThrownBy(() -> instantiateComparator("zzzz"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("zzzz");
	}

	private static RecordComparator instantiateComparator(String fieldSequence) {
		try {
			Class<?> comparatorClass = Class.forName("org.eclipse.rdf4j.sail.nativerdf.TripleStore$TripleComparator");
			var ctor = comparatorClass.getDeclaredConstructor(String.class);
			ctor.setAccessible(true);
			return (RecordComparator) ctor.newInstance(fieldSequence);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
