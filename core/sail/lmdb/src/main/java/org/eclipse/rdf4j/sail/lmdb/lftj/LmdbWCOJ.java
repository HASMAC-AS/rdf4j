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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Tuple expression placeholder representing a multiway join intended for LMDB-backed Leapfrog Triejoin evaluation.
 */
public final class LmdbWCOJ extends AbstractQueryModelNode implements TupleExpr {

	private final List<StatementPattern> patterns;
	private final Set<String> bindingNames;
	private final Set<String> assuredBindingNames;

	public LmdbWCOJ(List<StatementPattern> patterns) {
		this.patterns = new ArrayList<>(patterns);
		this.bindingNames = computeBindingNames(patterns, false);
		this.assuredBindingNames = computeBindingNames(patterns, true);
	}

	public List<StatementPattern> getPatterns() {
		return patterns;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);
	}

	@Override
	public Set<String> getBindingNames() {
		return Set.copyOf(bindingNames);
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return Set.copyOf(assuredBindingNames);
	}

	@Override
	public LmdbWCOJ clone() {
		return new LmdbWCOJ(clonePatterns());
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		for (int i = 0; i < patterns.size(); i++) {
			if (patterns.get(i) == current && replacement instanceof StatementPattern) {
				patterns.set(i, (StatementPattern) replacement);
				return;
			}
		}
		throw new IllegalArgumentException("Could not replace child node " + current + " on " + this);
	}

	@Override
	public void replaceWith(QueryModelNode replacement) {
		super.replaceWith(replacement);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		for (StatementPattern pattern : patterns) {
			pattern.visit(visitor);
		}
	}

	private List<StatementPattern> clonePatterns() {
		List<StatementPattern> clones = new ArrayList<>(patterns.size());
		for (StatementPattern pattern : patterns) {
			clones.add(pattern.clone());
		}
		return clones;
	}

	private static Set<String> computeBindingNames(List<StatementPattern> patterns, boolean assuredOnly) {
		Set<String> names = new HashSet<>();
		for (StatementPattern pattern : patterns) {
			if (assuredOnly) {
				names.addAll(pattern.getAssuredBindingNames());
			} else {
				names.addAll(pattern.getBindingNames());
			}
		}
		return names;
	}
}
