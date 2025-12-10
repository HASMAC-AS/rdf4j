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
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;

/**
 * Rewrites pure join chains of statement patterns into a single {@link LmdbWCOJ} placeholder node.
 */
public class LmdbWCOJOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		if (bindings != null && !bindings.isEmpty()) {
			return;
		}

		TupleExpr target = unwrap(tupleExpr);
		List<StatementPattern> patterns = new ArrayList<>();
		if (isJoinTree(target, patterns) && patterns.size() >= 3 && hasSharedJoinVariable(patterns)) {
			LmdbWCOJ wcoj = new LmdbWCOJ(patterns);
			target.replaceWith(wcoj);
		}
	}

	private TupleExpr unwrap(TupleExpr expr) {
		TupleExpr current = expr;
		if (current instanceof QueryRoot) {
			current = ((QueryRoot) current).getArg();
		}
		if (current instanceof Projection) {
			current = ((Projection) current).getArg();
		}
		return current;
	}

	private boolean hasSharedJoinVariable(List<StatementPattern> patterns) {
		java.util.Map<String, Integer> counts = new java.util.HashMap<>();
		for (StatementPattern pattern : patterns) {
			countVar(pattern.getSubjectVar(), counts);
			countVar(pattern.getPredicateVar(), counts);
			countVar(pattern.getObjectVar(), counts);
			countVar(pattern.getContextVar(), counts);
		}
		long joinVars = counts.values().stream().filter(count -> count >= 2).count();
		return joinVars >= 2;
	}

	private void countVar(org.eclipse.rdf4j.query.algebra.Var var, java.util.Map<String, Integer> counts) {
		if (var == null || var.isAnonymous() || var.hasValue()) {
			return;
		}
		counts.merge(var.getName(), 1, Integer::sum);
	}

	private boolean isJoinTree(TupleExpr expr, List<StatementPattern> patterns) {
		if (expr instanceof StatementPattern) {
			patterns.add((StatementPattern) expr);
			return true;
		}
		if (expr instanceof Join) {
			Join join = (Join) expr;
			return isJoinTree(join.getLeftArg(), patterns) && isJoinTree(join.getRightArg(), patterns);
		}
		return false;
	}
}
