/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.rdfparser;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.impl.SimpleDataset;

/** Indexed, read-only source with an explicit default graph, never a union of all parsed graphs. */
final class ParsedDatasetSource implements TripleSource {
	private final Model model;
	private final List<Resource> names;

	ParsedDatasetSource(Model model) {
		this.model = model;
		this.names = model.contexts().stream().filter(java.util.Objects::nonNull).toList();
	}

	SimpleDataset dataset() {
		SimpleDataset dataset = new SimpleDataset();
		dataset.setExplicitReadDataset(true);
		dataset.addDefaultGraph(RDF4J.NIL);
		for (Resource name : names) {
			dataset.addNamedGraph(name);
		}
		return dataset;
	}

	@Override
	public CloseableIteration<? extends Statement> getStatements(Resource subject, IRI predicate, Value object,
			Resource... contexts) {
		return new CloseableIteratorIteration<>(model.filter(subject, predicate, object, contexts).iterator());
	}

	@Override
	public CloseableIteration<? extends Resource> getContextIDs() {
		return new CloseableIteratorIteration<>(names.iterator());
	}

	@Override
	public long getStatementCount(Resource subject, IRI predicate, Value object, Resource... contexts) {
		return model.filter(subject, predicate, object, contexts).size();
	}

	@Override
	public boolean hasStatements(Resource subject, IRI predicate, Value object, Resource... contexts) {
		return model.contains(subject, predicate, object, contexts);
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}
}
