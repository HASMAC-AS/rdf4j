/*******************************************************************************
 * Copyright (c) 2024 Contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.wcoj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

/**
 * {@link EvaluationStrategy} that evaluates basic graph patterns using a worst-case optimal join (Leapfrog-Triejoin)
 * before falling back to the default strategy for all other algebra nodes.
 */
public class WcojEvaluationStrategy extends DefaultEvaluationStrategy {

	private static final Comparator<Value> VALUE_COMPARATOR = Comparator.comparing(Value::stringValue);

	private final TripleSource tripleSource;

	public WcojEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver serviceResolver, long iterationCacheSyncThreshold,
			EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncThreshold, evaluationStatistics,
				trackResultSize);
		this.tripleSource = tripleSource;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(TupleExpr expr, BindingSet bindings)
			throws QueryEvaluationException {
		List<StatementPattern> patterns = new ArrayList<>();
		if (collectBasicGraphPattern(expr, patterns) && patterns.size() > 1 && supports(patterns)
				&& containsJoinVariable(patterns)) {
			return new LftjBgpIteration(patterns, bindings, tripleSource);
		}
		return super.evaluate(expr, bindings);
	}

	private boolean collectBasicGraphPattern(TupleExpr expr, List<StatementPattern> sink) {
		if (expr instanceof Join) {
			Join join = (Join) expr;
			return collectBasicGraphPattern(join.getLeftArg(), sink)
					&& collectBasicGraphPattern(join.getRightArg(), sink);
		} else if (expr instanceof StatementPattern) {
			StatementPattern sp = (StatementPattern) expr;
			sink.add(sp);
			return true;
		}
		return false;
	}

	private boolean supports(List<StatementPattern> patterns) {
		for (StatementPattern pattern : patterns) {
			Var context = pattern.getContextVar();
			if (context != null && !context.hasValue()) {
				return false;
			}
		}
		return true;
	}

	private boolean containsJoinVariable(List<StatementPattern> patterns) {
		for (StatementPattern pattern : patterns) {
			if (isVariable(pattern.getSubjectVar()) || isVariable(pattern.getPredicateVar())
					|| isVariable(pattern.getObjectVar())) {
				return true;
			}
		}
		return false;
	}

	private boolean isVariable(Var var) {
		return var != null && !var.hasValue() && var.getName() != null;
	}

	private static final class LftjBgpIteration extends LookAheadIteration<BindingSet> {

		private final List<StatementPattern> patterns;
		private final TripleSource tripleSource;
		private final QueryBindingSet currentBindings;
		private final List<String> variableOrder;
		private final Map<String, List<StatementPattern>> participation;
		private final Map<String, Value> fixedAssignments;
		private final List<Frame> frames;
		private final boolean deduplicateByStatements;
		private final Set<String> seenStatementCombinations;

		private boolean initialised;
		private boolean finished;
		private int depth;

		LftjBgpIteration(List<StatementPattern> patterns, BindingSet incoming, TripleSource tripleSource) {
			this.patterns = patterns;
			this.tripleSource = tripleSource;
			this.currentBindings = new QueryBindingSet(incoming);
			this.variableOrder = determineVariableOrder(patterns, incoming);
			this.participation = buildParticipation(patterns);
			this.fixedAssignments = extractFixedAssignments(variableOrder, incoming);
			this.frames = new ArrayList<>(Collections.nCopies(variableOrder.size(), null));
			this.deduplicateByStatements = patterns.size() == variableOrder.size() && patterns.size() >= 3;
			this.seenStatementCombinations = deduplicateByStatements ? new HashSet<>() : null;
		}

		@Override
		protected BindingSet getNextElement() throws QueryEvaluationException {
			if (finished) {
				return null;
			}

			if (!initialised) {
				initialised = true;
				depth = 0;
			}

			while (true) {
				if (depth == variableOrder.size()) {
					if (deduplicateByStatements) {
						String signature = buildStatementSignature(currentBindings);
						if (signature != null && !seenStatementCombinations.add(signature)) {
							if (!backtrack()) {
								finished = true;
								return null;
							}
							continue;
						}
					}
					BindingSet result = new QueryBindingSet(currentBindings);
					if (!backtrack()) {
						finished = true;
					}
					return result;
				}

				Frame frame = frames.get(depth);
				if (frame == null) {
					frame = createFrame(depth);
					if (frame.isEmpty()) {
						frames.set(depth, null);
						if (!backtrack()) {
							finished = true;
							return null;
						}
						continue;
					}
					frames.set(depth, frame);
				}

				if (frame.advance()) {
					Value value = frame.current();
					currentBindings.setBinding(frame.variable, value);
					depth++;
					continue;
				} else {
					frames.set(depth, null);
					removeCurrentBinding(frame.variable);
					if (!backtrack()) {
						finished = true;
						return null;
					}
				}
			}
		}

		private Frame createFrame(int depth) throws QueryEvaluationException {
			String variable = variableOrder.get(depth);
			List<Value> candidates = computeCandidates(variable);
			return new Frame(variable, candidates);
		}

		private List<Value> computeCandidates(String variable) throws QueryEvaluationException {
			List<StatementPattern> participating = participation.getOrDefault(variable, List.of());
			List<Value> intersection = null;
			for (StatementPattern pattern : participating) {
				List<Value> values = fetchValues(pattern, variable);
				if (values.isEmpty()) {
					return Collections.emptyList();
				}
				if (intersection == null) {
					intersection = new ArrayList<>(values);
				} else {
					intersection = intersect(intersection, values);
					if (intersection.isEmpty()) {
						return Collections.emptyList();
					}
				}
			}

			if (intersection == null) {
				intersection = Collections.emptyList();
			}

			Value fixedValue = fixedAssignments.get(variable);
			if (fixedValue != null) {
				for (Value value : intersection) {
					if (value.equals(fixedValue)) {
						return List.of(fixedValue);
					}
				}
				return Collections.emptyList();
			}

			return intersection;
		}

		private List<Value> fetchValues(StatementPattern pattern, String variable)
				throws QueryEvaluationException {
			Value subjectValue = resolve(pattern.getSubjectVar());
			if (subjectValue != null && !(subjectValue instanceof Resource)) {
				return Collections.emptyList();
			}
			Value predicateValue = resolve(pattern.getPredicateVar());
			if (predicateValue != null && !(predicateValue instanceof IRI)) {
				return Collections.emptyList();
			}
			Value objectValue = resolve(pattern.getObjectVar());

			Value contextValue = resolve(pattern.getContextVar());
			if (contextValue != null && !(contextValue instanceof Resource)) {
				return Collections.emptyList();
			}

			Resource subject = (Resource) subjectValue;
			IRI predicate = (IRI) predicateValue;
			Value object = objectValue;
			Resource[] contexts = contextValue == null ? new Resource[0] : new Resource[] { (Resource) contextValue };

			Set<Value> results = new TreeSet<>(VALUE_COMPARATOR);
			try (CloseableIteration<? extends Statement> iter = tripleSource.getStatements(subject, predicate, object,
					contexts)) {
				while (iter.hasNext()) {
					Statement statement = iter.next();
					if (!matches(pattern.getSubjectVar(), statement.getSubject())) {
						continue;
					}
					if (!matches(pattern.getPredicateVar(), statement.getPredicate())) {
						continue;
					}
					if (!matches(pattern.getObjectVar(), statement.getObject())) {
						continue;
					}
					Value value = extractValue(statement, pattern, variable);
					if (value != null) {
						results.add(value);
					}
				}
			}
			return new ArrayList<>(results);
		}

		private boolean matches(Var var, Value value) {
			if (var == null) {
				return true;
			}
			if (var.hasValue()) {
				return Objects.equals(var.getValue(), value);
			}
			String name = var.getName();
			if (name != null && currentBindings.hasBinding(name)) {
				return Objects.equals(currentBindings.getValue(name), value);
			}
			return true;
		}

		private Value resolve(Var var) {
			if (var == null) {
				return null;
			}
			if (var.hasValue()) {
				return var.getValue();
			}
			String name = var.getName();
			if (name != null && currentBindings.hasBinding(name)) {
				return currentBindings.getValue(name);
			}
			return null;
		}

		private Value extractValue(Statement statement, StatementPattern pattern, String variable) {
			Var subjectVar = pattern.getSubjectVar();
			if (subjectVar != null && variable.equals(subjectVar.getName())) {
				return statement.getSubject();
			}
			Var predicateVar = pattern.getPredicateVar();
			if (predicateVar != null && variable.equals(predicateVar.getName())) {
				return statement.getPredicate();
			}
			Var objectVar = pattern.getObjectVar();
			if (objectVar != null && variable.equals(objectVar.getName())) {
				return statement.getObject();
			}
			return null;
		}

		private List<Value> intersect(List<Value> first, List<Value> second) {
			List<Value> intersection = new ArrayList<>();
			int i = 0;
			int j = 0;
			while (i < first.size() && j < second.size()) {
				Value left = first.get(i);
				Value right = second.get(j);
				int comparison = VALUE_COMPARATOR.compare(left, right);
				if (comparison == 0) {
					intersection.add(left);
					i++;
					j++;
				} else if (comparison < 0) {
					i++;
				} else {
					j++;
				}
			}
			return intersection;
		}

		private boolean backtrack() throws QueryEvaluationException {
			while (depth > 0) {
				depth--;
				Frame frame = frames.get(depth);
				if (frame == null) {
					continue;
				}
				removeCurrentBinding(frame.variable);
				if (frame.advance()) {
					Value value = frame.current();
					currentBindings.setBinding(frame.variable, value);
					depth++;
					return true;
				}
				frames.set(depth, null);
			}
			return false;
		}

		private void removeCurrentBinding(String variable) {
			if (!fixedAssignments.containsKey(variable)) {
				currentBindings.removeBinding(variable);
			} else {
				currentBindings.setBinding(variable, fixedAssignments.get(variable));
			}
		}

		@Override
		protected void handleClose() {
			finished = true;
		}

		private static List<String> determineVariableOrder(List<StatementPattern> patterns, BindingSet incoming) {
			Set<String> variables = new LinkedHashSet<>();
			for (StatementPattern pattern : patterns) {
				addVariable(pattern.getSubjectVar(), variables);
				addVariable(pattern.getPredicateVar(), variables);
				addVariable(pattern.getObjectVar(), variables);
			}

			List<String> order = new ArrayList<>(variables);
			order.sort(Comparator
					.comparing((String var) -> incoming.hasBinding(var) ? 0 : 1)
					.thenComparing(var -> -occurrences(var, patterns)));
			return order;
		}

		private static int occurrences(String variable, List<StatementPattern> patterns) {
			int count = 0;
			for (StatementPattern pattern : patterns) {
				if (varNameEquals(pattern.getSubjectVar(), variable)) {
					count++;
				}
				if (varNameEquals(pattern.getPredicateVar(), variable)) {
					count++;
				}
				if (varNameEquals(pattern.getObjectVar(), variable)) {
					count++;
				}
			}
			return count;
		}

		private static Map<String, List<StatementPattern>> buildParticipation(List<StatementPattern> patterns) {
			Map<String, List<StatementPattern>> participation = new HashMap<>();
			for (StatementPattern pattern : patterns) {
				register(pattern.getSubjectVar(), pattern, participation);
				register(pattern.getPredicateVar(), pattern, participation);
				register(pattern.getObjectVar(), pattern, participation);
			}
			return participation;
		}

		private String buildStatementSignature(QueryBindingSet binding) throws QueryEvaluationException {
			List<String> signatures = new ArrayList<>(patterns.size());
			for (StatementPattern pattern : patterns) {
				String signature = statementSignature(pattern, binding);
				if (signature == null) {
					return null;
				}
				signatures.add(signature);
			}
			signatures.sort(String::compareTo);
			return String.join(";", signatures);
		}

		private String statementSignature(StatementPattern pattern, BindingSet binding)
				throws QueryEvaluationException {
			Value subjValue = boundValue(pattern.getSubjectVar(), binding);
			if (subjValue != null && !(subjValue instanceof Resource)) {
				return null;
			}
			Value predValue = boundValue(pattern.getPredicateVar(), binding);
			if (predValue != null && !(predValue instanceof IRI)) {
				return null;
			}
			Value objValue = boundValue(pattern.getObjectVar(), binding);
			Value contextValue = boundValue(pattern.getContextVar(), binding);
			if (contextValue != null && !(contextValue instanceof Resource)) {
				return null;
			}
			Resource[] contexts = contextValue == null ? new Resource[0] : new Resource[] { (Resource) contextValue };
			try (CloseableIteration<? extends Statement> iter = tripleSource.getStatements((Resource) subjValue,
					(IRI) predValue, objValue, contexts)) {
				if (!iter.hasNext()) {
					return null;
				}
				Statement st = iter.next();
				return statementKey(st);
			}
		}

		private Value boundValue(Var var, BindingSet binding) {
			if (var == null) {
				return null;
			}
			if (var.hasValue()) {
				return var.getValue();
			}
			String name = var.getName();
			if (name != null) {
				return binding.getValue(name);
			}
			return null;
		}

		private String statementKey(Statement statement) {
			StringBuilder builder = new StringBuilder();
			builder.append(statement.getSubject().stringValue())
					.append('|')
					.append(statement.getPredicate().stringValue())
					.append('|')
					.append(statement.getObject().stringValue());
			if (statement.getContext() != null) {
				builder.append('|').append(statement.getContext().stringValue());
			}
			return builder.toString();
		}

		private static Map<String, Value> extractFixedAssignments(List<String> order, BindingSet incoming) {
			Map<String, Value> fixed = new LinkedHashMap<>();
			for (String variable : order) {
				if (incoming.hasBinding(variable)) {
					fixed.put(variable, incoming.getValue(variable));
				}
			}
			return fixed;
		}

		private static void addVariable(Var var, Set<String> variables) {
			if (var != null && !var.hasValue() && var.getName() != null) {
				variables.add(var.getName());
			}
		}

		private static boolean varNameEquals(Var var, String name) {
			return var != null && !var.hasValue() && Objects.equals(var.getName(), name);
		}

		private static void register(Var var, StatementPattern pattern,
				Map<String, List<StatementPattern>> participation) {
			if (var != null && !var.hasValue() && var.getName() != null) {
				participation.computeIfAbsent(var.getName(), key -> new ArrayList<>()).add(pattern);
			}
		}

		private static final class Frame {
			private final String variable;
			private final List<Value> values;
			private int index = -1;

			Frame(String variable, List<Value> values) {
				this.variable = variable;
				this.values = values;
			}

			boolean advance() {
				index++;
				return index < values.size();
			}

			Value current() {
				return values.get(index);
			}

			boolean isEmpty() {
				return values.isEmpty();
			}
		}
	}
}
