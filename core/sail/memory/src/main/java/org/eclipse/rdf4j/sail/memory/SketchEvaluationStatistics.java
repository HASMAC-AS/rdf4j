/*****************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *****************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

class SketchEvaluationStatistics extends EvaluationStatistics {

	private final SketchRegistry registry;

	SketchEvaluationStatistics(SketchRegistry registry) {
		this.registry = registry;
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new SketchCardinalityCalculator();
	}

	private class SketchCardinalityCalculator extends CardinalityCalculator {

		@Override
		public double getCardinality(StatementPattern sp) {
			Var predVar = sp.getPredicateVar();
			if (predVar != null && predVar.hasValue()) {
				String iri = predVar.getValue().stringValue();
				PredStats stats = registry.get(iri);
				if (stats != null) {
					boolean subjBound = sp.getSubjectVar() != null && sp.getSubjectVar().hasValue();
					boolean objBound = sp.getObjectVar() != null && sp.getObjectVar().hasValue();
					long total = stats.getCount();
					if (subjBound && objBound) {
						return 1.0;
					} else if (subjBound) {
						long distinct = stats.getSubjectSketch().cardinality();
						return total / (double) Math.max(1L, distinct);
					} else if (objBound) {
						long distinct = stats.getObjectSketch().cardinality();
						return total / (double) Math.max(1L, distinct);
					} else {
						return total;
					}
				}
			}
			return super.getCardinality(sp);
		}

		@Override
		public void meet(Join node) {
			if (node.getLeftArg() instanceof StatementPattern && node.getRightArg() instanceof StatementPattern) {
				StatementPattern left = (StatementPattern) node.getLeftArg();
				StatementPattern right = (StatementPattern) node.getRightArg();
				Var join = joinVar(left, right);
				if (join != null
						&& left.getPredicateVar() != null && left.getPredicateVar().hasValue()
						&& right.getPredicateVar() != null && right.getPredicateVar().hasValue()) {
					String leftPred = left.getPredicateVar().getValue().stringValue();
					String rightPred = right.getPredicateVar().getValue().stringValue();
					PredStats lStats = registry.get(leftPred);
					PredStats rStats = registry.get(rightPred);
					if (lStats != null && rStats != null) {
						Sketch ls = pickSketch(lStats, left, join);
						Sketch rs = pickSketch(rStats, right, join);
						if (ls != null && rs != null) {
							long inter = registry.intersection(ls, rs);
							cardinality = inter;
							return;
						}
					}
				}
			}
			super.meet(node);
		}

		private Sketch pickSketch(PredStats stats, StatementPattern sp, Var join) {
			if (join == sp.getSubjectVar()) {
				return stats.getSubjectSketch();
			} else if (join == sp.getObjectVar()) {
				return stats.getObjectSketch();
			}
			return null;
		}

		private Var joinVar(StatementPattern left, StatementPattern right) {
			if (left.getSubjectVar() != null && left.getSubjectVar() == right.getSubjectVar()) {
				return left.getSubjectVar();
			}
			if (left.getSubjectVar() != null && left.getSubjectVar() == right.getObjectVar()) {
				return left.getSubjectVar();
			}
			if (left.getObjectVar() != null && left.getObjectVar() == right.getSubjectVar()) {
				return left.getObjectVar();
			}
			if (left.getObjectVar() != null && left.getObjectVar() == right.getObjectVar()) {
				return left.getObjectVar();
			}
			return null;
		}
	}
}
