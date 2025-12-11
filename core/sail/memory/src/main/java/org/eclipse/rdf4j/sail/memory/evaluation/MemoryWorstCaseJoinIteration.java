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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.memory.MemorySailStore.MemorySailDataset;
import org.eclipse.rdf4j.sail.memory.model.MemStatement;

class MemoryWorstCaseJoinIteration extends LookAheadIteration<BindingSet> {

private final List<StatementPattern> statementPatterns;
private final MemorySailDataset dataset;
private final QueryEvaluationContext context;
private final BindingSet baseBindings;
private Iterator<BindingSet> materializedResults;

MemoryWorstCaseJoinIteration(List<StatementPattern> statementPatterns, MemorySailDataset dataset,
QueryEvaluationContext context, BindingSet bindings) {
this.statementPatterns = statementPatterns;
this.dataset = dataset;
this.context = context;
this.baseBindings = bindings;
}

@Override
protected BindingSet getNextElement() throws QueryEvaluationException {
if (materializedResults == null) {
materializeResults();
}
if (materializedResults.hasNext()) {
return materializedResults.next();
}
return null;
}

private void materializeResults() throws QueryEvaluationException {
Set<String> variableOrder = collectVariableOrder();
List<BindingSet> results = new ArrayList<>();

MutableBindingSet seed = new QueryBindingSet();
for (Binding binding : baseBindings) {
seed.addBinding(binding);
}

recurse(new ArrayList<>(variableOrder), 0, seed, results);
materializedResults = results.iterator();
}

private Set<String> collectVariableOrder() {
Map<String, Integer> counts = new HashMap<>();
for (StatementPattern pattern : statementPatterns) {
collect(pattern.getSubjectVar(), counts);
collect(pattern.getPredicateVar(), counts);
collect(pattern.getObjectVar(), counts);
collect(pattern.getContextVar(), counts);
}
Set<String> order = new LinkedHashSet<>();
counts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
.forEach(e -> order.add(e.getKey()));
return order;
}

private void collect(Var var, Map<String, Integer> counts) {
if (var != null && !var.hasValue()) {
counts.merge(var.getName(), 1, Integer::sum);
}
}

private void recurse(List<String> variableOrder, int idx, MutableBindingSet currentBindings,
Collection<BindingSet> sink) throws QueryEvaluationException {
if (idx >= variableOrder.size()) {
if (patternsSatisfied(currentBindings)) {
sink.add(new QueryBindingSet(currentBindings));
}
return;
}

String varName = variableOrder.get(idx);
if (currentBindings.hasBinding(varName)) {
recurse(variableOrder, idx + 1, currentBindings, sink);
return;
}

Set<Value> candidates = intersectCandidates(varName, currentBindings);
for (Value candidate : candidates) {
MutableBindingSet next = new QueryBindingSet(currentBindings);
next.addBinding(varName, candidate);
recurse(variableOrder, idx + 1, next, sink);
}
}

private boolean patternsSatisfied(MutableBindingSet bindings) throws QueryEvaluationException {
for (StatementPattern pattern : statementPatterns) {
if (!hasMatchingStatement(pattern, bindings)) {
return false;
}
}
return true;
}

private Set<Value> intersectCandidates(String varName, BindingSet bindings) throws QueryEvaluationException {
Set<Value> intersection = null;
for (StatementPattern pattern : statementPatterns) {
if (!usesVariable(pattern, varName)) {
continue;
}

Set<Value> values = collectValues(pattern, varName, bindings);
if (intersection == null) {
intersection = values;
} else {
intersection.retainAll(values);
}

if (intersection.isEmpty()) {
return Collections.emptySet();
}
}

return intersection != null ? intersection : Collections.emptySet();
}

private boolean usesVariable(StatementPattern pattern, String varName) {
return varName.equals(optionalName(pattern.getSubjectVar())) || varName.equals(optionalName(pattern.getPredicateVar()))
|| varName.equals(optionalName(pattern.getObjectVar())) || varName.equals(optionalName(pattern.getContextVar()));
}

private String optionalName(Var var) {
return var == null ? null : var.getName();
}

private Set<Value> collectValues(StatementPattern pattern, String targetVar, BindingSet bindings)
throws QueryEvaluationException {
Set<Value> values = new HashSet<>();

Resource subject = resourceValue(pattern.getSubjectVar(), bindings);
IRI predicate = iriValue(pattern.getPredicateVar(), bindings);
Value object = value(pattern.getObjectVar(), bindings);
Resource[] contexts = computeContexts(pattern.getContextVar(), bindings);
if (contexts == null) {
return Collections.emptySet();
}

try (CloseableIteration<MemStatement> stmts = dataset.getStatements(subject, predicate, object, contexts)) {
while (stmts.hasNext()) {
MemStatement st = stmts.next();
if (targetVar.equals(optionalName(pattern.getSubjectVar()))) {
values.add(st.getSubject());
} else if (targetVar.equals(optionalName(pattern.getPredicateVar()))) {
values.add(st.getPredicate());
} else if (targetVar.equals(optionalName(pattern.getObjectVar()))) {
values.add(st.getObject());
} else if (targetVar.equals(optionalName(pattern.getContextVar()))) {
values.add(st.getContext());
}
}
} catch (QueryEvaluationException e) {
throw e;
} catch (Exception e) {
throw new QueryEvaluationException(e);
}

return values;
}

private boolean hasMatchingStatement(StatementPattern pattern, BindingSet bindings) throws QueryEvaluationException {
Resource subject = resourceValue(pattern.getSubjectVar(), bindings);
IRI predicate = iriValue(pattern.getPredicateVar(), bindings);
Value object = value(pattern.getObjectVar(), bindings);
Resource[] contexts = computeContexts(pattern.getContextVar(), bindings);
if (contexts == null) {
return false;
}

try (CloseableIteration<MemStatement> stmts = dataset.getStatements(subject, predicate, object, contexts)) {
return stmts.hasNext();
} catch (QueryEvaluationException e) {
throw e;
} catch (Exception e) {
throw new QueryEvaluationException(e);
}
}

private Resource[] computeContexts(Var contextVar, BindingSet bindings) {
if (contextVar == null || (!contextVar.hasValue() && !bindings.hasBinding(contextVar.getName()))) {
return new Resource[0];
}

Value ctxValue = contextVar.hasValue() ? contextVar.getValue() : bindings.getValue(contextVar.getName());
if (ctxValue instanceof Resource) {
return new Resource[] { (Resource) ctxValue };
}
return null;
}

private Resource resourceValue(Var var, BindingSet bindings) {
Value v = value(var, bindings);
return v instanceof Resource ? (Resource) v : null;
}

private IRI iriValue(Var var, BindingSet bindings) {
Value v = value(var, bindings);
return v instanceof IRI ? (IRI) v : null;
}

private Value value(Var var, BindingSet bindings) {
if (var == null) {
return null;
}
if (var.hasValue()) {
return var.getValue();
}
Binding existing = bindings.getBinding(var.getName());
return existing != null ? existing.getValue() : null;
}
}
