/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;

/**
 * This class wraps an iterator and increments the "resultSizeActual" of the query model node that the iterator
 * represents. This means we can track the number of tuples that have been retrieved from this node.
 *
 * @deprecated For internal use only. Do not rely on this class in your code.
 */
@InternalUseOnly
@Deprecated
public class ResultSizeCountingIterator implements CloseableIteration<BindingSet, QueryEvaluationException> {

	CloseableIteration<BindingSet, QueryEvaluationException> iterator;
	QueryModelNode queryModelNode;

	public ResultSizeCountingIterator(CloseableIteration<BindingSet, QueryEvaluationException> iterator,
			QueryModelNode queryModelNode) {
		this.iterator = iterator;
		this.queryModelNode = queryModelNode;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return iterator.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		queryModelNode.setResultSizeActual(queryModelNode.getResultSizeActual() + 1);
		return iterator.next();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		iterator.remove();
	}

	@Override
	public void close() throws QueryEvaluationException {
		iterator.close();
	}
}
