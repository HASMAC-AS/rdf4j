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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * Node in a join tree for an acyclic basic graph pattern.
 */
public class JoinTreeNode {

	private final StatementPattern statementPattern;
	private final List<JoinTreeNode> children = new ArrayList<>();
	private Set<String> joinVariables = Collections.emptySet();

	JoinTreeNode(StatementPattern statementPattern) {
		this.statementPattern = Objects.requireNonNull(statementPattern);
	}

	public StatementPattern getStatementPattern() {
		return statementPattern;
	}

	public List<JoinTreeNode> getChildren() {
		return children;
	}

	public Set<String> getJoinVariables() {
		return joinVariables;
	}

	void setJoinVariables(Set<String> joinVariables) {
		this.joinVariables = Set.copyOf(joinVariables);
	}

	void addChild(JoinTreeNode child) {
		children.add(child);
	}
}
