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

package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailReadOnlyException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.features.ThreadSafetyAware;
import org.eclipse.rdf4j.sail.helpers.DefaultSailChangedEvent;
import org.eclipse.rdf4j.sail.memory.evaluation.MemoryTripleSourceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Sail Connection for memory stores.
 *
 * @author Arjohn Kampman
 * @author jeen
 */
public class MemoryStoreConnection extends SailSourceConnection implements ThreadSafetyAware {

private static final Logger logger = LoggerFactory.getLogger(MemoryStoreConnection.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	protected final MemoryStore sail;

	private volatile DefaultSailChangedEvent sailChangedEvent;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected MemoryStoreConnection(MemoryStore sail) {
		super(sail, sail.getSailStore(), sail.getEvaluationStrategyFactory());
		this.sail = sail;
		sailChangedEvent = new DefaultSailChangedEvent(sail);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void startTransactionInternal() throws SailException {
		if (!sail.isWritable()) {
			throw new SailReadOnlyException("Unable to start transaction: data file is locked or read-only");
		}
		super.startTransactionInternal();
		sail.cancelSyncTask();
	}

	@Override
	protected void commitInternal() throws SailException {
		super.commitInternal();

		sail.notifySailChanged(sailChangedEvent);
		sail.scheduleSyncTask();

		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(sail);
	}

	@Override
	protected void rollbackInternal() throws SailException {
		super.rollbackInternal();
		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(sail);
	}

@Override
protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
// assume the triple is not yet present in the triple store
sailChangedEvent.setStatementsAdded(true);
}

@Override
protected CloseableIteration<? extends BindingSet> evaluateInternal(TupleExpr tupleExpr, Dataset dataset,
BindingSet bindings, boolean includeInferred) throws SailException {

logger.trace("Incoming query model:\n{}", tupleExpr);

if (!(tupleExpr instanceof QueryRoot)) {
// Add a dummy root node to the tuple expressions to allow the optimizers to modify the actual root node
tupleExpr = new QueryRoot(tupleExpr);
}

SailSource branch = null;
MemorySailStore.MemorySailDataset rdfDataset = null;
CloseableIteration<BindingSet> iteration = null;
boolean allGood = false;
try {
branch = branch(IncludeInferred.fromBoolean(includeInferred));
rdfDataset = (MemorySailStore.MemorySailDataset) branch.dataset(getIsolationLevel());

MemoryTripleSourceWrapper tripleSource = new MemoryTripleSourceWrapper(rdfDataset, getValueFactory());
EvaluationStrategy strategy = getEvaluationStrategy(dataset, tripleSource);
if (isTrackResultSize()) {
strategy.setTrackResultSize(true);
}

tupleExpr = strategy.optimize(tupleExpr, sail.getSailStore().getEvaluationStatistics(), bindings);
logger.trace("Optimized query model:\n{}", tupleExpr);
QueryEvaluationStep qes = strategy.precompile(tupleExpr);
iteration = qes.evaluate(EmptyBindingSet.getInstance());
iteration = interlock(iteration, rdfDataset, branch);
allGood = true;
return iteration;
} catch (QueryEvaluationException e) {
throw new SailException(e);
} finally {
if (!allGood) {
if (iteration != null) {
iteration.close();
}
if (rdfDataset != null) {
rdfDataset.close();
}
if (branch != null) {
branch.close();
}
}
}
}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		boolean ret = super.addInferredStatement(subj, pred, obj, contexts);
		// assume the triple is not yet present in the triple store
		sailChangedEvent.setStatementsAdded(true);
		return ret;
	}

	@Override
	protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		sailChangedEvent.setStatementsRemoved(true);
	}

	@Override
	public boolean removeInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		boolean ret = super.removeInferredStatement(subj, pred, obj, contexts);
		sailChangedEvent.setStatementsRemoved(true);
		return ret;
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		super.clearInternal(contexts);
		sailChangedEvent.setStatementsRemoved(true);
	}

	@Override
	public void clearInferred(Resource... contexts) throws SailException {
		super.clearInferred(contexts);
		sailChangedEvent.setStatementsRemoved(true);
	}

	public MemoryStore getSail() {
		return sail;
	}

	@Override
	public boolean supportsConcurrentReads() {
		return getTransactionIsolation() != null && getTransactionIsolation() != IsolationLevels.SERIALIZABLE;
	}
}
