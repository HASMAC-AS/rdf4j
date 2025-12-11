/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.memory.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep.DelayedEvaluation;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.EvaluationStatistics;
import org.eclipse.rdf4j.sail.memory.MemorySailStore.MemorySailDataset;

public class MemoryEvaluationStrategy extends DefaultEvaluationStrategy {

public MemoryEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
FederatedServiceResolver serviceResolver, long iterationCacheSyncTreshold,
EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
super(tripleSource, dataset, serviceResolver, iterationCacheSyncTreshold, evaluationStatistics,
trackResultSize);
}

@Override
protected QueryEvaluationStep prepare(Join node, QueryEvaluationContext context) throws QueryEvaluationException {
if (tripleSource instanceof MemoryTripleSourceWrapper) {
MemoryTripleSourceWrapper memoryTripleSource = (MemoryTripleSourceWrapper) tripleSource;
List<StatementPattern> statementPatterns = new ArrayList<>();
if (collectStatementPatterns(node, statementPatterns)) {
node.setAlgorithm("LeapfrogTrieJoin");
return prepareWorstCaseJoin(statementPatterns, memoryTripleSource.getDataset(), context);
}
}

return super.prepare(node, context);
}

private QueryEvaluationStep prepareWorstCaseJoin(List<StatementPattern> statementPatterns,
MemorySailDataset dataset, QueryEvaluationContext context) {
return new DelayedEvaluation(bindings -> worstCaseJoin(statementPatterns, dataset, context, bindings), context);
}

private CloseableIteration<BindingSet> worstCaseJoin(List<StatementPattern> statementPatterns,
MemorySailDataset dataset, QueryEvaluationContext context, BindingSet bindings) {
return new MemoryWorstCaseJoinIteration(statementPatterns, dataset, context, bindings);
}

private boolean collectStatementPatterns(TupleExpr node, List<StatementPattern> statementPatterns) {
if (node instanceof StatementPattern) {
statementPatterns.add((StatementPattern) node);
return true;
} else if (node instanceof Join) {
Join join = (Join) node;
return collectStatementPatterns(join.getLeftArg(), statementPatterns)
&& collectStatementPatterns(join.getRightArg(), statementPatterns);
}
return false;
}
}
