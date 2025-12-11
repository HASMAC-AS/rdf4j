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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.eclipse.rdf4j.sail.memory.MemorySailStore.MemorySailDataset;

public class MemoryTripleSourceWrapper extends SailDatasetTripleSource {

private final MemorySailDataset dataset;

public MemoryTripleSourceWrapper(MemorySailDataset dataset, ValueFactory valueFactory) {
super(valueFactory, dataset);
this.dataset = dataset;
}

public MemorySailDataset getDataset() {
return dataset;
}

@Override
public CloseableIteration<? extends Statement> getStatements(StatementOrder order, Resource subj, IRI pred, Value obj,
Resource... contexts) throws QueryEvaluationException {
return super.getStatements(order, subj, pred, obj, contexts);
}
}
