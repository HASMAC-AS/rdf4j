/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Builds a join tree for acyclic basic graph patterns composed of {@link Join}, {@link Filter}, and
 * {@link StatementPattern} nodes.
 */
public final class AcyclicJoinTreeBuilder {

	private AcyclicJoinTreeBuilder() {
	}

	public static Optional<JoinTree> build(TupleExpr tupleExpr) {
		List<StatementPattern> patterns = new ArrayList<>();
		if (!collectPatterns(tupleExpr, patterns)) {
			return Optional.empty();
		}

		Map<StatementPattern, Set<String>> originalVars = buildVarTable(patterns);
		if (patterns.size() == 1) {
			JoinTreeNode root = new JoinTreeNode(patterns.get(0));
			return Optional.of(new JoinTree(root));
		}

		return buildJoinTree(patterns, originalVars);
	}

	private static Optional<JoinTree> buildJoinTree(List<StatementPattern> patterns,
			Map<StatementPattern, Set<String>> originalVars) {
		List<StatementPattern> workingEdges = new ArrayList<>(patterns);
		Map<StatementPattern, Set<String>> workingVars = workingEdges.stream()
				.collect(Collectors.toMap(edge -> edge, edge -> new HashSet<>(originalVars.get(edge))));

		Map<StatementPattern, StatementPattern> parent = new HashMap<>();

		boolean changed;
		do {
			changed = false;
			Map<String, Integer> frequency = variableFrequency(workingVars.values());

			for (StatementPattern edge : new ArrayList<>(workingEdges)) {
				Set<String> vars = workingVars.get(edge);
				if (vars.removeIf(v -> frequency.getOrDefault(v, 0) == 1)) {
					changed = true;
				}
			}

			StatementPattern removed = null;
			StatementPattern parentEdge = null;
			outer: for (StatementPattern edge : workingEdges) {
				for (StatementPattern candidateParent : workingEdges) {
					if (edge == candidateParent) {
						continue;
					}
					Set<String> edgeVars = workingVars.get(edge);
					Set<String> parentVars = workingVars.get(candidateParent);
					if (!edgeVars.isEmpty() && parentVars.containsAll(edgeVars)) {
						removed = edge;
						parentEdge = candidateParent;
						break outer;
					}
				}
			}

			if (removed != null) {
				workingEdges.remove(removed);
				parent.put(removed, parentEdge);
				changed = true;
			}
		} while (changed && workingEdges.size() > 1);

		if (workingEdges.size() != 1) {
			boolean allVarsRemoved = workingEdges.stream().allMatch(edge -> workingVars.get(edge).isEmpty());
			if (!allVarsRemoved) {
				return Optional.empty();
			}
		}

		StatementPattern preferredRoot = patterns.get(0);
		StatementPattern rootEdge = workingEdges.contains(preferredRoot) ? preferredRoot : workingEdges.get(0);
		Map<StatementPattern, JoinTreeNode> nodes = new HashMap<>();
		patterns.forEach(p -> nodes.put(p, new JoinTreeNode(p)));

		Set<StatementPattern> attached = new HashSet<>();
		for (Map.Entry<StatementPattern, StatementPattern> entry : parent.entrySet()) {
			StatementPattern child = entry.getKey();
			StatementPattern parentPattern = entry.getValue();
			attachChild(nodes, originalVars, parentPattern, child);
			attached.add(child);
		}

		for (StatementPattern pattern : patterns) {
			if (pattern == rootEdge || attached.contains(pattern)) {
				continue;
			}
			attachChild(nodes, originalVars, rootEdge, pattern);
		}

		return Optional.of(new JoinTree(nodes.get(rootEdge)));
	}

	private static void attachChild(Map<StatementPattern, JoinTreeNode> nodes,
			Map<StatementPattern, Set<String>> originalVars,
			StatementPattern parentPattern, StatementPattern childPattern) {
		Set<String> joinVars = intersection(originalVars.get(childPattern), originalVars.get(parentPattern));
		JoinTreeNode childNode = nodes.get(childPattern);
		childNode.setJoinVariables(joinVars);
		nodes.get(parentPattern).addChild(childNode);
	}

	private static Map<StatementPattern, Set<String>> buildVarTable(List<StatementPattern> patterns) {
		Map<StatementPattern, Set<String>> vars = new HashMap<>();
		for (StatementPattern pattern : patterns) {
			vars.put(pattern, extractVarNames(pattern));
		}
		return vars;
	}

	private static boolean collectPatterns(TupleExpr expr, List<StatementPattern> out) {
		if (expr instanceof Join) {
			Join join = (Join) expr;
			return collectPatterns(join.getLeftArg(), out) && collectPatterns(join.getRightArg(), out);
		}

		if (expr instanceof Filter) {
			return collectPatterns(((Filter) expr).getArg(), out);
		}

		if (expr instanceof StatementPattern) {
			out.add((StatementPattern) expr);
			return true;
		}

		return false;
	}

	private static Map<String, Integer> variableFrequency(Iterable<Set<String>> edgeVars) {
		Map<String, Integer> frequency = new HashMap<>();
		for (Set<String> vars : edgeVars) {
			for (String var : vars) {
				frequency.merge(var, 1, Integer::sum);
			}
		}
		return frequency;
	}

	private static Set<String> extractVarNames(StatementPattern pattern) {
		Set<String> names = new HashSet<>();
		collectVar(pattern.getSubjectVar(), names);
		collectVar(pattern.getPredicateVar(), names);
		collectVar(pattern.getObjectVar(), names);
		collectVar(pattern.getContextVar(), names);
		return names;
	}

	private static void collectVar(Var var, Set<String> names) {
		if (var != null && !var.hasValue()) {
			names.add(var.getName());
		}
	}

	private static Set<String> intersection(Set<String> left, Set<String> right) {
		if (left.isEmpty() || right.isEmpty()) {
			return Set.of();
		}
		Set<String> result = new HashSet<>(left);
		result.retainAll(right);
		return result;
	}
}
