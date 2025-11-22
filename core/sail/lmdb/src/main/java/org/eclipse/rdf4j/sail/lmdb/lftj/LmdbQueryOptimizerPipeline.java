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

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.StandardQueryOptimizerPipeline;

/**
 * Standard optimizer pipeline extended with LMDB-specific WCOJ rewriting.
 */
public class LmdbQueryOptimizerPipeline extends StandardQueryOptimizerPipeline {

	public LmdbQueryOptimizerPipeline(EvaluationStrategy strategy, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics) {
		super(strategy, tripleSource, evaluationStatistics);
	}

	@Override
	public Iterable<QueryOptimizer> getOptimizers() {
		List<QueryOptimizer> optimizers = new ArrayList<>();
		super.getOptimizers().forEach(optimizers::add);
		optimizers.add(new LmdbWCOJOptimizer());
		return optimizers;
	}
}
