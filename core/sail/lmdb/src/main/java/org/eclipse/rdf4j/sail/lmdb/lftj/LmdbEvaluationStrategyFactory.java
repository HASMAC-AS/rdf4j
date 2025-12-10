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

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;

/**
 * Evaluation strategy factory enabling LMDB-specific optimizer extensions.
 */
public class LmdbEvaluationStrategyFactory extends StrictEvaluationStrategyFactory {

	public LmdbEvaluationStrategyFactory() {
		this(null);
	}

	public LmdbEvaluationStrategyFactory(FederatedServiceResolver resolver) {
		super(resolver);
	}

	@Override
	public EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {
		LmdbEvaluationStrategy strategy = new LmdbEvaluationStrategy(tripleSource, dataset,
				getFederatedServiceResolver(),
				getQuerySolutionCacheThreshold(), evaluationStatistics, isTrackResultSize());
		strategy.setCollectionFactory(collectionFactorySupplier);
		strategy.setOptimizerPipeline(getOptimizerPipeline()
				.orElseGet(() -> new LmdbQueryOptimizerPipeline(strategy, tripleSource, evaluationStatistics)));
		return strategy;
	}
}
