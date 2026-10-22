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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Lateral;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * Recognizes syntactic RDF collections within one conjunctive group, never stored RDF lists. It creates a lateral
 * dependency boundary, so an optimizer cannot move parsing before its input providers. Caller algebra is not mutated.
 */
final class PropertyFunctionRewriter extends AbstractQueryModelVisitor<QueryEvaluationException> {
	private final Map<IRI, RdfParseTupleFunction> functions;
	private final String temporaryPrefix = "__rdf_parse_" + UUID.randomUUID().toString().replace("-", "") + "_";
	private int nextVariable;

	private PropertyFunctionRewriter(Map<IRI, RdfParseTupleFunction> functions) {
		this.functions = functions;
	}

	static TupleExpr rewrite(TupleExpr expression, Map<IRI, RdfParseTupleFunction> functions) {
		QueryRoot root = new QueryRoot(expression.clone());
		root.visit(new PropertyFunctionRewriter(functions));
		return root.getArg();
	}

	@Override
	public void meet(Service node) {
		// A SERVICE body belongs to its receiving evaluator. The local parse service rewrites its own body.
	}

	@Override
	public void meet(Join node) {
		List<TupleExpr> leaves = new ArrayList<>();
		flatten(node, leaves);
		List<StatementPattern> patterns = new ArrayList<>();
		for (TupleExpr leaf : leaves) {
			if (leaf instanceof StatementPattern pattern) {
				patterns.add(pattern);
			}
		}
		boolean hasCall = patterns.stream().anyMatch(pattern -> function(pattern) != null)
				|| leaves.stream().anyMatch(PropertyFunctionRewriter::isLocalService);
		if (!hasCall) {
			super.meet(node);
			return;
		}
		replaceGroup(node, leaves, patterns);
	}

	@Override
	public void meet(StatementPattern node) {
		if (function(node) != null) {
			replaceGroup(node, List.of(node), List.of(node));
		}
	}

	private void replaceGroup(TupleExpr original, List<TupleExpr> leaves, List<StatementPattern> patterns) {
		Set<TupleExpr> consumed = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<String> cells = new HashSet<>();
		List<Call> calls = new ArrayList<>();
		for (TupleExpr leaf : leaves) {
			if (isLocalService(leaf)) {
				Service local = (Service) leaf;
				Set<String> inputs = ServiceRequest.declaredInputs(local);
				Set<String> outputs = new LinkedHashSet<>(local.getBindingNames());
				outputs.removeAll(inputs);
				calls.add(new Call(local, inputs, outputs));
				consumed.add(local);
			}
		}
		for (StatementPattern pattern : patterns) {
			RdfParseTupleFunction function = function(pattern);
			if (function == null) {
				continue;
			}
			if (pattern.getContextVar() != null) {
				throw new QueryEvaluationException("RDF parser property calls must be outside GRAPH clauses");
			}
			consumed.add(pattern);
			List<Var> args = collection(pattern.getSubjectVar(), patterns, consumed, cells,
					function.isTurtle());
			List<Var> outputs = collection(pattern.getObjectVar(), patterns, consumed, cells,
					function.resultArity() == 1);
			int minArgs = function.isTurtle() ? 1 : 2;
			if (args.size() < minArgs || args.size() > minArgs + 1
					|| outputs.size() != function.resultArity()) {
				throw new QueryEvaluationException("Incorrect RDF parser argument or result arity for "
						+ pattern.getPredicateVar().getValue());
			}
			TupleFunctionCall call = new TupleFunctionCall();
			call.setURI(function.getURI());
			args.forEach(arg -> call.addArg(arg.clone()));
			TupleExpr output = call;
			Set<String> outputNames = new LinkedHashSet<>();
			// The generic tuple SPI does not promise intra-row checks for repeated output names. Make those
			// equalities explicit instead of letting the last tuple column overwrite an earlier binding.
			for (Var variable : outputs) {
				if (!variable.hasValue() && !outputNames.add(variable.getName())) {
					Var fresh = Var.of(temporaryPrefix + nextVariable++, true);
					call.addResultVar(fresh);
					output = new Filter(output, new SameTerm(variable.clone(), fresh.clone()));
				} else {
					call.addResultVar(variable.clone());
				}
			}
			Set<String> inputs = new LinkedHashSet<>();
			for (Var arg : args) {
				if (!arg.hasValue()) {
					inputs.add(arg.getName());
				}
			}
			calls.add(new Call(output, inputs, outputNames));
		}
		TupleExpr remaining = null;
		for (TupleExpr leaf : leaves) {
			if (!consumed.contains(leaf)) {
				leaf.visit(new AbstractQueryModelVisitor<QueryEvaluationException>() {
					@Override
					public void meet(Var var) {
						if (!var.hasValue() && cells.contains(var.getName())) {
							throw new QueryEvaluationException("An RDF parser argument-list cell is shared with data");
						}
					}
				});
				remaining = remaining == null ? leaf : new Join(remaining, leaf);
			}
		}
		if (remaining == null) {
			remaining = new SingletonSet();
		}
		Set<String> available = new HashSet<>(remaining.getBindingNames());
		while (!calls.isEmpty()) {
			Call ready = null;
			for (Call candidate : calls) {
				boolean blocked = false;
				for (String input : candidate.inputs) {
					if (!available.contains(input) && calls.stream().anyMatch(other -> other != candidate
							&& other.outputs.contains(input))) {
						blocked = true;
						break;
					}
				}
				if (!blocked) {
					ready = candidate;
					break;
				}
			}
			if (ready == null) {
				throw new QueryEvaluationException("Cyclic RDF parser function dependencies");
			}
			Set<String> imports = new LinkedHashSet<>(ready.inputs);
			imports.addAll(ready.outputs); // Preserve prebound output constraints as well as document inputs.
			remaining = new Lateral(remaining, ready.expression, imports);
			available.addAll(ready.outputs);
			calls.remove(ready);
		}
		original.replaceWith(remaining);
		remaining.visit(this);
	}

	private List<Var> collection(Var head, List<StatementPattern> patterns, Set<TupleExpr> consumed,
			Set<String> cells, boolean allowScalar) {
		if (allowScalar && (head.hasValue() || !head.isAnonymous())) {
			return List.of(head);
		}
		Map<String, StatementPattern> first = new HashMap<>();
		Map<String, StatementPattern> rest = new HashMap<>();
		for (StatementPattern pattern : patterns) {
			if (pattern.getContextVar() != null || pattern.getSubjectVar().hasValue()
					|| !pattern.getSubjectVar().isAnonymous()) {
				continue;
			}
			Map<String, StatementPattern> index = RDF.FIRST.equals(pattern.getPredicateVar().getValue()) ? first
					: RDF.REST.equals(pattern.getPredicateVar().getValue()) ? rest : null;
			if (index != null && index.put(pattern.getSubjectVar().getName(), pattern) != null) {
				throw new QueryEvaluationException("Ambiguous RDF collection in parser call");
			}
		}
		List<Var> values = new ArrayList<>();
		Var current = head;
		while (!RDF.NIL.equals(current.getValue())) {
			if (current.hasValue() || !current.isAnonymous() || !cells.add(current.getName()) || values.size() >= 8) {
				throw new QueryEvaluationException("Parser arguments must be unshared, acyclic syntactic RDF lists");
			}
			StatementPattern f = first.get(current.getName());
			StatementPattern r = rest.get(current.getName());
			if (f == null || r == null || !consumed.add(f) || !consumed.add(r)) {
				throw new QueryEvaluationException("Incomplete or shared RDF argument list in parser call");
			}
			values.add(f.getObjectVar());
			current = r.getObjectVar();
		}
		return values;
	}

	private RdfParseTupleFunction function(StatementPattern pattern) {
		return pattern.getPredicateVar().getValue() instanceof IRI predicate ? functions.get(predicate) : null;
	}

	private static boolean isLocalService(TupleExpr expression) {
		return expression instanceof Service service && service.getServiceRef().hasValue()
				&& RdfParseVocabulary.SERVICE.equals(service.getServiceRef().getValue().stringValue());
	}

	private static void flatten(TupleExpr expression, List<TupleExpr> leaves) {
		if (expression instanceof Join join) {
			flatten(join.getLeftArg(), leaves);
			flatten(join.getRightArg(), leaves);
		} else {
			leaves.add(expression);
		}
	}

	private record Call(TupleExpr expression, Set<String> inputs, Set<String> outputs) {
	}
}
