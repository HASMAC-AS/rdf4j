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

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

/**
 * EvaluationStrategyFactory that wires in the worst-case optimal join (WCOJ) implementation backed by trie indexes.
 */
public class LmdbWcojEvaluationStrategyFactory extends LmdbEvaluationStrategyFactory {

	public LmdbWcojEvaluationStrategyFactory(FederatedServiceResolver resolver) {
		super(resolver);
	}

	@Override
	public EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {
		LmdbWcojEvaluationStrategy strategy = new LmdbWcojEvaluationStrategy(tripleSource, dataset,
				getFederatedServiceResolver(), getQuerySolutionCacheThreshold(), evaluationStatistics,
				isTrackResultSize());
		getOptimizerPipeline().ifPresent(strategy::setOptimizerPipeline);
		strategy.setCollectionFactory(collectionFactorySupplier);
		return strategy;
	}
}
