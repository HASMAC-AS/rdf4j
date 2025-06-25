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
package example.sketchmem;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

/**
 * EvaluationStatistics that uses HyperLogLog sketches for cardinality estimation when possible.
 */
public class SketchEvaluationStatistics extends EvaluationStatistics {
	private final SketchRegistry registry;
	private final EvaluationStatistics fallback = new EvaluationStatistics();

	/**
	 * @param registry registry containing predicate statistics
	 */
	public SketchEvaluationStatistics(SketchRegistry registry) {
		this.registry = registry;
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new SketchCardinalityCalculator();
	}

	private class SketchCardinalityCalculator extends CardinalityCalculator {
		@Override
		protected double getCardinality(StatementPattern sp) {
			Var predVar = sp.getPredicateVar();
			Value pred = predVar != null ? predVar.getValue() : null;
			if (pred instanceof IRI) {
				PredStats ps = registry.forPred(pred.stringValue());
				Var subjVar = sp.getSubjectVar();
				Var objVar = sp.getObjectVar();
				boolean subjBound = subjVar != null && subjVar.getValue() != null;
				boolean objBound = objVar != null && objVar.getValue() != null;

				if (!subjBound && !objBound) {
					return Math.max(ps.subjCount(), ps.objCount());
				} else if (!subjBound) {
					return ps.subjCount() * ps.dupSubj();
				} else if (!objBound) {
					return ps.objCount() * ps.dupObj();
				} else {
					return ps.tripleCount();
				}
			}
			// default behavior
			return fallback.getCardinality(sp);
		}
	}
}
