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
package org.eclipse.rdf4j.sail.lmdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdBGPQueryEvaluationStep;

/**
 * Placeholder WCOJ strategy that will route BGPs through a trie-backed Leapfrog Triejoin implementation. In this
 * initial version we delegate to the existing LMDB strategy while the trie engine is being iteratively integrated.
 */
@InternalUseOnly
public class LmdbWcojEvaluationStrategy extends LmdbEvaluationStrategy {

	public LmdbWcojEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncThreshold,
			EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncThreshold, evaluationStatistics,
				trackResultSize);
		setQueryEvaluationMode(QueryEvaluationMode.STRICT);
	}

	@Override
	protected QueryEvaluationStep prepare(Join node, QueryEvaluationContext context) {
		QueryEvaluationStep defaultStep = super.prepare(node, context);

		if (!(context instanceof LmdbDatasetContext)) {
			return defaultStep;
		}
		Optional<LmdbEvaluationDataset> dsOpt = ((LmdbDatasetContext) context).getLmdbDataset();
		if (dsOpt.isEmpty()) {
			return defaultStep;
		}
		LmdbEvaluationDataset ds = dsOpt.get();
		if (ds.hasTransactionChanges()) {
			return defaultStep;
		}
		if (containsMergeJoin(node)) {
			return defaultStep;
		}
		if (ds.getTrieIndexManager() == null || ds.getTxnManager() == null) {
			return defaultStep;
		}

		List<StatementPattern> patterns = new ArrayList<>();
		if (!LmdbIdBGPQueryEvaluationStep.flattenBGP(node, patterns) || patterns.isEmpty()) {
			return defaultStep;
		}
		boolean hasContextClause = patterns.stream().anyMatch(p -> p.getContextVar() != null);
		if (hasContextClause) {
			return defaultStep;
		}
		// WCOJ currently does not handle anonymous (blank node) join variables reliably; fall back in that case.
		boolean hasAnonymous = patterns.stream().anyMatch(p -> hasAnonymousVar(p));
		if (hasAnonymous) {
			return defaultStep;
		}
		LmdbWcojBGPQueryEvaluationStep step = new LmdbWcojBGPQueryEvaluationStep(patterns, context, ds,
				ds.getTrieIndexManager(), ds.getTxnManager(), defaultStep);
		if (!step.usesWcoj()) {
			return defaultStep;
		}
		return step;
	}

	private boolean hasAnonymousVar(StatementPattern p) {
		return isAnon(p.getSubjectVar()) || isAnon(p.getPredicateVar()) || isAnon(p.getObjectVar())
				|| isAnon(p.getContextVar());
	}

	private boolean containsMergeJoin(org.eclipse.rdf4j.query.algebra.TupleExpr expr) {
		if (expr instanceof Join) {
			Join join = (Join) expr;
			if (join.isMergeJoin()) {
				return true;
			}
			return containsMergeJoin(join.getLeftArg()) || containsMergeJoin(join.getRightArg());
		}
		return false;
	}

	private boolean isAnon(org.eclipse.rdf4j.query.algebra.Var v) {
		return v != null && !v.hasValue() && v.isAnonymous();
	}
}
