/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.helpers.collectors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * An efficient QueryModelVisitor that collects StatementPattern's from a query model.
 */
public class StatementPatternCollector extends AbstractSimpleQueryModelVisitor<RuntimeException> {

	public StatementPatternCollector() {
		super(true);
	}

	public static List<StatementPattern> process(QueryModelNode node) {
		StatementPatternCollector collector = new StatementPatternCollector();
		node.visit(collector);
		return collector.getStatementPatterns();
	}

	private final List<StatementPattern> statementPatterns = new ArrayList<>();

	public List<StatementPattern> getStatementPatterns() {
		return statementPatterns;
	}

	@Override
	public void meet(Filter node) {
		// Skip boolean constraints
		node.getArg().visit(this);
	}

	@Override
	public void meet(Join node) throws RuntimeException {
		TupleExpr leftArg = node.getLeftArg();
		TupleExpr rightArg = node.getRightArg();

		// INSERT clause is often a deeply nested join. Recursive approach may cause stack overflow. Attempt
		// non-recursive (or at least less-recursive) approach first.
		while (true) {
			if (leftArg instanceof Join && !(rightArg instanceof Join)) {
				rightArg.visit(this);

				Join join = (Join) leftArg;
				leftArg = join.getLeftArg();
				rightArg = join.getRightArg();

			} else if (rightArg instanceof Join && !(leftArg instanceof Join)) {
				leftArg.visit(this);

				Join join = (Join) rightArg;
				leftArg = join.getLeftArg();
				rightArg = join.getRightArg();

			} else {
				leftArg.visit(this);
				rightArg.visit(this);
				return;
			}
		}

	}

	@Override
	public void meet(StatementPattern node) {
		statementPatterns.add(node);
	}
}
