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

import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

/**
 * Evaluation strategy that recognizes {@link LmdbWCOJ} placeholders.
 */
public class LmdbEvaluationStrategy extends StrictEvaluationStrategy {

	public LmdbEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
			EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, evaluationStatistics,
				trackResultSize);
	}

	@Override
	public QueryEvaluationStep precompile(TupleExpr expr, QueryEvaluationContext context) {
		if (expr instanceof LmdbWCOJ) {
			TupleExpr delegate = rebuildJoin((LmdbWCOJ) expr);
			return super.precompile(delegate, context);
		}
		return super.precompile(expr, context);
	}

	private TupleExpr rebuildJoin(LmdbWCOJ wcoj) {
		List<StatementPattern> patterns = wcoj.getPatterns();
		Iterator<StatementPattern> iterator = patterns.iterator();
		TupleExpr current = iterator.next().clone();
		while (iterator.hasNext()) {
			current = new Join(current, iterator.next().clone());
		}
		return current;
	}
}
