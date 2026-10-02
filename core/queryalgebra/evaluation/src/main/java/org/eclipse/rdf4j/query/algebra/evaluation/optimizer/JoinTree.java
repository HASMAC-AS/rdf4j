/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.Objects;

/**
 * A join tree covering an acyclic basic graph pattern.
 */
public class JoinTree {

	private final JoinTreeNode root;

	public JoinTree(JoinTreeNode root) {
		this.root = Objects.requireNonNull(root);
	}

	public JoinTreeNode getRoot() {
		return root;
	}
}
