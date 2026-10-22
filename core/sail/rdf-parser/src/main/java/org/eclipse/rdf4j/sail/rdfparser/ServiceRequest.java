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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Lateral;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/** Extracts one top-level configuration record without treating nested graph patterns as configuration. */
record ServiceRequest(TupleExpr body, Var content, Var format, Var base) {
	static ServiceRequest from(Service service) {
		QueryRoot root = new QueryRoot(service.getServiceExpr().clone());
		List<StatementPattern> parameters = new ArrayList<>();
		collect(root.getArg(), parameters);
		Map<IRI, Var> values = new HashMap<>();
		Object subject = null;
		for (StatementPattern parameter : parameters) {
			Var current = parameter.getSubjectVar();
			if (!current.hasValue() && !current.isAnonymous()) {
				throw new QueryEvaluationException("Use [] or a constant IRI as the RDF service configuration subject");
			}
			Object key = current.hasValue() ? current.getValue() : current.getName();
			if (subject != null && !subject.equals(key)) {
				throw new QueryEvaluationException("Exactly one RDF service configuration record is required");
			}
			subject = key;
			IRI predicate = (IRI) parameter.getPredicateVar().getValue();
			if (values.put(predicate, parameter.getObjectVar().clone()) != null) {
				throw new QueryEvaluationException("Duplicate RDF service option: " + predicate);
			}
			parameter.replaceWith(new SingletonSet());
		}
		if (!values.containsKey(RdfParseVocabulary.CONTENT) || !values.containsKey(RdfParseVocabulary.FORMAT)) {
			throw new QueryEvaluationException("RDF service requires top-level parse:content and parse:format options");
		}
		root.visit(new AbstractQueryModelVisitor<QueryEvaluationException>() {
			@Override
			public void meet(Service nested) {
				throw new QueryEvaluationException("Nested SERVICE is not allowed inside the local RDF parser service");
			}
		});
		return new ServiceRequest(root.getArg(), values.get(RdfParseVocabulary.CONTENT),
				values.get(RdfParseVocabulary.FORMAT), values.get(RdfParseVocabulary.BASE_IRI));
	}

	static Set<String> declaredInputs(Service service) {
		List<StatementPattern> parameters = new ArrayList<>();
		collect(service.getServiceExpr(), parameters);
		Set<String> inputs = new LinkedHashSet<>();
		for (StatementPattern parameter : parameters) {
			Var value = parameter.getObjectVar();
			if (!value.hasValue()) {
				inputs.add(value.getName());
			}
		}
		return inputs;
	}

	Value value(Var variable, BindingSet bindings) {
		if (variable == null) {
			return null;
		}
		Value value = variable.hasValue() ? variable.getValue() : bindings.getValue(variable.getName());
		if (value == null) {
			throw new QueryEvaluationException("RDF service input is unbound: ?" + variable.getName()
					+ "; bind it before entering SERVICE");
		}
		return value;
	}

	private static void collect(TupleExpr node, List<StatementPattern> parameters) {
		if (node instanceof Join join) {
			collect(join.getLeftArg(), parameters);
			collect(join.getRightArg(), parameters);
		} else if (node instanceof LeftJoin optional) {
			collect(optional.getLeftArg(), parameters);
		} else if (node instanceof Difference minus) {
			collect(minus.getLeftArg(), parameters);
		} else if (node instanceof Lateral lateral) {
			collect(lateral.getLeftArg(), parameters);
		} else if (node instanceof Filter filter) {
			collect(filter.getArg(), parameters);
		} else if (node instanceof Extension extension) {
			collect(extension.getArg(), parameters);
		} else if (node instanceof StatementPattern pattern && pattern.getContextVar() == null) {
			Value predicate = pattern.getPredicateVar().getValue();
			if (RdfParseVocabulary.CONTENT.equals(predicate) || RdfParseVocabulary.FORMAT.equals(predicate)
					|| RdfParseVocabulary.BASE_IRI.equals(predicate)) {
				parameters.add(pattern);
			}
		}
	}
}
