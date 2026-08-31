/*******************************************************************************
 * Copyright (c) 2024 Contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.wcoj;

import java.util.function.Supplier;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

/**
 * {@link DefaultEvaluationStrategyFactory} variant that evaluates basic graph patterns using a worst-case optimal join
 * implementation.
 */
public class WcojEvaluationStrategyFactory extends DefaultEvaluationStrategyFactory {

	private Supplier<CollectionFactory> collectionFactorySupplier;

	public WcojEvaluationStrategyFactory() {
		super();
	}

	public WcojEvaluationStrategyFactory(FederatedServiceResolver resolver) {
		super(resolver);
	}

	@Override
	public void setCollectionFactory(Supplier<CollectionFactory> collectionFactory) {
		super.setCollectionFactory(collectionFactory);
		this.collectionFactorySupplier = collectionFactory;
	}

	@Override
	public EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {
		WcojEvaluationStrategy strategy = new WcojEvaluationStrategy(tripleSource, dataset,
				getFederatedServiceResolver(), getQuerySolutionCacheThreshold(), evaluationStatistics,
				isTrackResultSize());
		getOptimizerPipeline().ifPresent(strategy::setOptimizerPipeline);
		if (collectionFactorySupplier != null) {
			strategy.setCollectionFactory(collectionFactorySupplier);
		}
		return strategy;
	}
}
