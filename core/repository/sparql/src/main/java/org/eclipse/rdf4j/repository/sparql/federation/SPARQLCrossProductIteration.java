/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.federation;

import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.sparql.query.SPARQLQueryBindingSet;

/**
 * Iteration which forms the cross product of a list of materialized input bindings with each result obtained from the
 * inner iteration. Example: <source> inputBindings := {b1, b2, ...} resultIteration := {r1, r2, ...} getNextElement()
 * returns (r1,b1), (r1, b2), ..., (r2, b1), (r2, b2), ... i.e. compute the cross product per result binding </source>
 * Note that this class is a fully equivalent copy of
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.iterator.CrossProductIteration}, and is only included here to avoid
 * a circular dependency between the algebra-evaluation module and the sparql-repository module.
 *
 * @author Andreas Schwarte
 */
public class SPARQLCrossProductIteration extends LookAheadIteration<BindingSet> {

	protected final List<BindingSet> inputBindings;

	protected final CloseableIteration<BindingSet> resultIteration;

	protected Iterator<BindingSet> inputBindingsIterator = null;

	protected BindingSet currentInputBinding = null;

	public SPARQLCrossProductIteration(CloseableIteration<BindingSet> resultIteration,
			List<BindingSet> inputBindings) {
		super();
		this.resultIteration = resultIteration;
		this.inputBindings = inputBindings;
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {

		if (currentInputBinding == null) {
			inputBindingsIterator = inputBindings.iterator();
			if (resultIteration.hasNext()) {
				currentInputBinding = resultIteration.next();
			} else {
				return null; // no more results
			}
		}

		if (inputBindingsIterator.hasNext()) {
			BindingSet next = inputBindingsIterator.next();
			SPARQLQueryBindingSet res = new SPARQLQueryBindingSet(next.size() + currentInputBinding.size());
			res.addAll(next);
			res.addAll(currentInputBinding);
			if (!inputBindingsIterator.hasNext()) {
				currentInputBinding = null;
			}
			return res;
		}

		return null;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		resultIteration.close();
	}
}
