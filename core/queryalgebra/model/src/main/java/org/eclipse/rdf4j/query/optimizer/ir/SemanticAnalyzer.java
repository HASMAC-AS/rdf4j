package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Computes semantic annotations (vars, certainVars, monotone, well-designed, etc.) for patterns. */
public final class SemanticAnalyzer {

	public AnalysisResult analyze(Pattern root) {
		Map<Pattern, InterimInfo> interim = new IdentityHashMap<>();
		compute(root, interim);
		Map<Pattern, SemanticInfo> finalInfo = new IdentityHashMap<>();
		computeWellDesigned(root, Collections.emptySet(), interim, finalInfo);
		return new AnalysisResult(finalInfo);
	}

	private InterimInfo compute(Pattern pattern, Map<Pattern, InterimInfo> interim) {
		InterimInfo cached = interim.get(pattern);
		if (cached != null) {
			return cached;
		}

		InterimInfo info;
		if (pattern instanceof EmptyPattern) {
			info = InterimInfo.empty();
		} else if (pattern instanceof Bgp) {
			info = fromBgp((Bgp) pattern);
		} else if (pattern instanceof Join) {
			info = fromJoin((Join) pattern, interim);
		} else if (pattern instanceof LeftJoin) {
			info = fromLeftJoin((LeftJoin) pattern, interim);
		} else if (pattern instanceof UnionPattern) {
			info = fromUnion((UnionPattern) pattern, interim);
		} else if (pattern instanceof FilterPattern) {
			info = fromFilter((FilterPattern) pattern, interim);
		} else if (pattern instanceof MinusPattern) {
			info = fromMinus((MinusPattern) pattern, interim);
		} else if (pattern instanceof GraphPattern) {
			info = fromGraph((GraphPattern) pattern, interim);
		} else if (pattern instanceof ServicePattern) {
			info = fromService((ServicePattern) pattern, interim);
		} else if (pattern instanceof ValuesPattern) {
			info = fromValues((ValuesPattern) pattern);
		} else if (pattern instanceof SubqueryPattern) {
			info = fromSubquery((SubqueryPattern) pattern, interim);
		} else {
			throw new IllegalArgumentException("Unknown pattern type: " + pattern.getClass());
		}

		interim.put(pattern, info);
		return info;
	}

	private InterimInfo fromBgp(Bgp bgp) {
		Set<Var> vars = new HashSet<>();
		for (TriplePattern tp : bgp.getTriples()) {
			collectVarsFromTerm(tp.getSubject(), vars);
			collectVarsFromTerm(tp.getPredicate(), vars);
			collectVarsFromTerm(tp.getObject(), vars);
		}
		return new InterimInfo(vars, new HashSet<>(vars), false, false, false, false);
	}

	private InterimInfo fromJoin(Join join, Map<Pattern, InterimInfo> interim) {
		InterimInfo left = compute(join.getLeft(), interim);
		InterimInfo right = compute(join.getRight(), interim);
		Set<Var> vars = union(left.vars, right.vars);
		Set<Var> certain = union(left.certainVars, right.certainVars);
		return combine(vars, certain, left, right);
	}

	private InterimInfo fromLeftJoin(LeftJoin leftJoin, Map<Pattern, InterimInfo> interim) {
		InterimInfo left = compute(leftJoin.getLeft(), interim);
		InterimInfo right = compute(leftJoin.getRight(), interim);
		ExprInfo conditionInfo = leftJoin.getCondition() == null ? ExprInfo.empty()
				: exprInfo(leftJoin.getCondition());

		Set<Var> vars = union(left.vars, right.vars);
		vars.addAll(conditionInfo.vars);

		Set<Var> certain = new HashSet<>(left.certainVars);

		boolean hasNegation = left.hasNegation || right.hasNegation || conditionInfo.hasNegation;
		boolean hasAggregates = left.hasAggregates || right.hasAggregates || conditionInfo.hasAggregates;
		boolean hasLimitOffset = left.hasLimitOffset || right.hasLimitOffset;
		boolean hasDistinct = left.hasDistinct || right.hasDistinct;

		return new InterimInfo(vars, certain, hasNegation, hasAggregates, hasLimitOffset, hasDistinct);
	}

	private InterimInfo fromUnion(UnionPattern union, Map<Pattern, InterimInfo> interim) {
		Set<Var> vars = new HashSet<>();
		Set<Var> certain = null;
		boolean hasNegation = false;
		boolean hasAggregates = false;
		boolean hasLimitOffset = false;
		boolean hasDistinct = false;

		for (Pattern alt : union.getAlternatives()) {
			InterimInfo altInfo = compute(alt, interim);
			vars.addAll(altInfo.vars);
			certain = certain == null ? new HashSet<>(altInfo.certainVars)
					: intersect(certain, altInfo.certainVars);
			hasNegation |= altInfo.hasNegation;
			hasAggregates |= altInfo.hasAggregates;
			hasLimitOffset |= altInfo.hasLimitOffset;
			hasDistinct |= altInfo.hasDistinct;
		}

		if (certain == null) {
			certain = new HashSet<>();
		}

		return new InterimInfo(vars, certain, hasNegation, hasAggregates, hasLimitOffset, hasDistinct);
	}

	private InterimInfo fromFilter(FilterPattern filter, Map<Pattern, InterimInfo> interim) {
		InterimInfo child = compute(filter.getInner(), interim);
		ExprInfo exprInfo = exprInfo(filter.getCondition());
		Set<Var> vars = new HashSet<>(child.vars);
		vars.addAll(exprInfo.vars);
		boolean hasNegation = child.hasNegation || exprInfo.hasNegation;
		boolean hasAggregates = child.hasAggregates || exprInfo.hasAggregates;
		return new InterimInfo(vars, new HashSet<>(child.certainVars), hasNegation, hasAggregates,
				child.hasLimitOffset, child.hasDistinct);
	}

	private InterimInfo fromMinus(MinusPattern minus, Map<Pattern, InterimInfo> interim) {
		InterimInfo left = compute(minus.getLeft(), interim);
		InterimInfo right = compute(minus.getRight(), interim);
		Set<Var> vars = union(left.vars, right.vars);
		Set<Var> certain = new HashSet<>(left.certainVars);
		boolean hasNegation = true;
		boolean hasAggregates = left.hasAggregates || right.hasAggregates;
		boolean hasLimitOffset = left.hasLimitOffset || right.hasLimitOffset;
		boolean hasDistinct = left.hasDistinct || right.hasDistinct;
		return new InterimInfo(vars, certain, hasNegation, hasAggregates, hasLimitOffset, hasDistinct);
	}

	private InterimInfo fromGraph(GraphPattern graph, Map<Pattern, InterimInfo> interim) {
		InterimInfo inner = compute(graph.getInner(), interim);
		Set<Var> vars = new HashSet<>(inner.vars);
		collectVarsFromTerm(graph.getGraphName(), vars);
		return new InterimInfo(vars, new HashSet<>(inner.certainVars), inner.hasNegation, inner.hasAggregates,
				inner.hasLimitOffset, inner.hasDistinct);
	}

	private InterimInfo fromService(ServicePattern service, Map<Pattern, InterimInfo> interim) {
		InterimInfo inner = compute(service.getInner(), interim);
		Set<Var> vars = new HashSet<>(inner.vars);
		collectVarsFromTerm(service.getServiceIri(), vars);
		return new InterimInfo(vars, new HashSet<>(inner.certainVars), inner.hasNegation, inner.hasAggregates,
				inner.hasLimitOffset, inner.hasDistinct);
	}

	private InterimInfo fromValues(ValuesPattern values) {
		Set<Var> vars = new HashSet<>(values.getVars());
		Set<Var> certain = new HashSet<>();
		for (int i = 0; i < values.getVars().size(); i++) {
			Var var = values.getVars().get(i);
			boolean allBound = true;
			for (List<TermOrUndef> row : values.getRows()) {
				if (i >= row.size() || row.get(i).isUndef()) {
					allBound = false;
					break;
				}
			}
			if (allBound) {
				certain.add(var);
			}
		}
		return new InterimInfo(vars, certain, false, false, false, false);
	}

	private InterimInfo fromSubquery(SubqueryPattern subqueryPattern, Map<Pattern, InterimInfo> interim) {
		SelectQuery query = subqueryPattern.getSubquery();
		InterimInfo whereInfo = compute(query.getWhere(), interim);
		Set<Var> projectedVars = query.getProjection()
				.stream()
				.map(ProjectionElement::getAs)
				.collect(Collectors.toCollection(HashSet::new));

		boolean hasAggregates = whereInfo.hasAggregates || query.getProjection()
				.stream()
				.anyMatch(pe -> exprInfo(pe.getExpr()).hasAggregates);
		if (query.getHaving() != null) {
			ExprInfo havingInfo = exprInfo(query.getHaving());
			hasAggregates |= havingInfo.hasAggregates;
		}
		boolean hasNegation = whereInfo.hasNegation;
		boolean hasLimitOffset = query.getLimit() != null || query.getOffset() != null || whereInfo.hasLimitOffset;
		boolean hasDistinct = query.isDistinct() || whereInfo.hasDistinct;

		return new InterimInfo(projectedVars, new HashSet<>(projectedVars), hasNegation, hasAggregates,
				hasLimitOffset, hasDistinct);
	}

	private InterimInfo combine(Set<Var> vars, Set<Var> certain, InterimInfo left, InterimInfo right) {
		boolean hasNegation = left.hasNegation || right.hasNegation;
		boolean hasAggregates = left.hasAggregates || right.hasAggregates;
		boolean hasLimitOffset = left.hasLimitOffset || right.hasLimitOffset;
		boolean hasDistinct = left.hasDistinct || right.hasDistinct;
		return new InterimInfo(vars, certain, hasNegation, hasAggregates, hasLimitOffset, hasDistinct);
	}

	private void collectVarsFromTerm(Term term, Set<Var> target) {
		if (term instanceof Var) {
			target.add((Var) term);
		}
	}

	private ExprInfo exprInfo(Expr expr) {
		if (expr == null) {
			return ExprInfo.empty();
		}
		if (expr instanceof VarRef) {
			Set<Var> vars = new HashSet<>();
			vars.add(((VarRef) expr).getVar());
			return new ExprInfo(vars, false, false);
		} else if (expr instanceof TermExpr) {
			return ExprInfo.empty();
		} else if (expr instanceof UnaryExpr) {
			UnaryExpr unary = (UnaryExpr) expr;
			ExprInfo child = exprInfo(unary.getArg());
			return new ExprInfo(child.vars, child.hasNegation || unary.getOp() == UnaryExpr.Op.NOT,
					child.hasAggregates);
		} else if (expr instanceof BinaryExpr) {
			BinaryExpr binary = (BinaryExpr) expr;
			ExprInfo left = exprInfo(binary.getLeft());
			ExprInfo right = exprInfo(binary.getRight());
			Set<Var> vars = union(left.vars, right.vars);
			return new ExprInfo(vars, left.hasNegation || right.hasNegation, left.hasAggregates || right.hasAggregates);
		} else if (expr instanceof FunctionCall) {
			FunctionCall call = (FunctionCall) expr;
			Set<Var> vars = new HashSet<>();
			boolean hasNegation = false;
			boolean hasAggregates = false;
			for (Expr arg : call.getArgs()) {
				ExprInfo info = exprInfo(arg);
				vars.addAll(info.vars);
				hasNegation |= info.hasNegation;
				hasAggregates |= info.hasAggregates;
			}
			return new ExprInfo(vars, hasNegation, hasAggregates);
		} else if (expr instanceof ExistsExpr) {
			ExistsExpr exists = (ExistsExpr) expr;
			Set<Var> vars = collectVars(exists.getPattern());
			return new ExprInfo(vars, false, false);
		} else if (expr instanceof NotExistsExpr) {
			NotExistsExpr notExists = (NotExistsExpr) expr;
			Set<Var> vars = collectVars(notExists.getPattern());
			return new ExprInfo(vars, true, false);
		} else if (expr instanceof AggExpr) {
			Set<Var> vars = new HashSet<>();
			if (expr instanceof CountAgg) {
				CountAgg count = (CountAgg) expr;
				if (count.getArg() != null) {
					vars.addAll(exprInfo(count.getArg()).vars);
				}
			}
			return new ExprInfo(vars, false, true);
		}
		throw new IllegalArgumentException("Unknown expression type: " + expr.getClass());
	}

	private Set<Var> collectVars(Pattern pattern) {
		if (pattern instanceof Bgp) {
			Set<Var> vars = new HashSet<>();
			for (TriplePattern tp : ((Bgp) pattern).getTriples()) {
				collectVarsFromTerm(tp.getSubject(), vars);
				collectVarsFromTerm(tp.getPredicate(), vars);
				collectVarsFromTerm(tp.getObject(), vars);
			}
			return vars;
		} else if (pattern instanceof Join) {
			Join join = (Join) pattern;
			return union(collectVars(join.getLeft()), collectVars(join.getRight()));
		} else if (pattern instanceof LeftJoin) {
			LeftJoin lj = (LeftJoin) pattern;
			Set<Var> vars = union(collectVars(lj.getLeft()), collectVars(lj.getRight()));
			if (lj.getCondition() != null) {
				vars.addAll(exprInfo(lj.getCondition()).vars);
			}
			return vars;
		} else if (pattern instanceof UnionPattern) {
			Set<Var> vars = new HashSet<>();
			for (Pattern alt : ((UnionPattern) pattern).getAlternatives()) {
				vars.addAll(collectVars(alt));
			}
			return vars;
		} else if (pattern instanceof FilterPattern) {
			FilterPattern filter = (FilterPattern) pattern;
			Set<Var> vars = collectVars(filter.getInner());
			vars.addAll(exprInfo(filter.getCondition()).vars);
			return vars;
		} else if (pattern instanceof MinusPattern) {
			MinusPattern minus = (MinusPattern) pattern;
			return union(collectVars(minus.getLeft()), collectVars(minus.getRight()));
		} else if (pattern instanceof GraphPattern) {
			GraphPattern graph = (GraphPattern) pattern;
			Set<Var> vars = collectVars(graph.getInner());
			collectVarsFromTerm(graph.getGraphName(), vars);
			return vars;
		} else if (pattern instanceof ServicePattern) {
			ServicePattern service = (ServicePattern) pattern;
			Set<Var> vars = collectVars(service.getInner());
			collectVarsFromTerm(service.getServiceIri(), vars);
			return vars;
		} else if (pattern instanceof ValuesPattern) {
			return new HashSet<>(((ValuesPattern) pattern).getVars());
		} else if (pattern instanceof SubqueryPattern) {
			return ((SubqueryPattern) pattern).getSubquery()
					.getProjection()
					.stream()
					.map(ProjectionElement::getAs)
					.collect(Collectors.toCollection(HashSet::new));
		} else if (pattern instanceof EmptyPattern) {
			return new HashSet<>();
		}
		throw new IllegalArgumentException("Unknown pattern type: " + pattern.getClass());
	}

	private boolean computeWellDesigned(Pattern pattern, Set<Var> varsOutside,
			Map<Pattern, InterimInfo> interim, Map<Pattern, SemanticInfo> finalInfo) {
		InterimInfo info = Objects.requireNonNull(interim.get(pattern), "Missing interim info");
		boolean childrenWellDesigned = true;
		boolean localWellDesigned = true;

		if (pattern instanceof Join) {
			Join join = (Join) pattern;
			Set<Var> outsideLeft = union(varsOutside, interim.get(join.getRight()).vars);
			Set<Var> outsideRight = union(varsOutside, interim.get(join.getLeft()).vars);
			boolean leftOk = computeWellDesigned(join.getLeft(), outsideLeft, interim, finalInfo);
			boolean rightOk = computeWellDesigned(join.getRight(), outsideRight, interim, finalInfo);
			childrenWellDesigned = leftOk && rightOk;
		} else if (pattern instanceof LeftJoin) {
			LeftJoin leftJoin = (LeftJoin) pattern;
			Set<Var> rightVars = interim.get(leftJoin.getRight()).vars;
			Set<Var> leftVars = interim.get(leftJoin.getLeft()).vars;

			Set<Var> outsideLeft = union(varsOutside, rightVars);
			Set<Var> outsideRight = union(varsOutside, leftVars);

			boolean leftOk = computeWellDesigned(leftJoin.getLeft(), outsideLeft, interim, finalInfo);
			boolean rightOk = computeWellDesigned(leftJoin.getRight(), outsideRight, interim, finalInfo);
			childrenWellDesigned = leftOk && rightOk;

			Set<Var> problem = new HashSet<>(rightVars);
			problem.retainAll(varsOutside);
			problem.removeAll(leftVars);

			Set<Var> shared = intersect(leftVars, rightVars);
			if (shared.size() > 1) {
// If the OPTIONAL is correlated with its left side on multiple variables, allow right-only vars to be used outside.
				localWellDesigned = true;
			} else {
				localWellDesigned = problem.isEmpty();
			}
		} else if (pattern instanceof FilterPattern) {
			FilterPattern filter = (FilterPattern) pattern;
			childrenWellDesigned = computeWellDesigned(filter.getInner(), varsOutside, interim, finalInfo);
		} else if (pattern instanceof UnionPattern) {
			UnionPattern union = (UnionPattern) pattern;
			for (Pattern alt : union.getAlternatives()) {
				childrenWellDesigned &= computeWellDesigned(alt, varsOutside, interim, finalInfo);
			}
		} else if (pattern instanceof MinusPattern) {
			MinusPattern minus = (MinusPattern) pattern;
			boolean leftOk = computeWellDesigned(minus.getLeft(),
					union(varsOutside, interim.get(minus.getRight()).vars), interim,
					finalInfo);
			boolean rightOk = computeWellDesigned(minus.getRight(),
					union(varsOutside, interim.get(minus.getLeft()).vars), interim,
					finalInfo);
			childrenWellDesigned = leftOk && rightOk;
		} else if (pattern instanceof GraphPattern) {
			GraphPattern graph = (GraphPattern) pattern;
			childrenWellDesigned = computeWellDesigned(graph.getInner(), varsOutside, interim, finalInfo);
		} else if (pattern instanceof ServicePattern) {
			ServicePattern service = (ServicePattern) pattern;
			childrenWellDesigned = computeWellDesigned(service.getInner(), varsOutside, interim, finalInfo);
		} else if (pattern instanceof SubqueryPattern) {
			SubqueryPattern subquery = (SubqueryPattern) pattern;
			childrenWellDesigned = computeWellDesigned(subquery.getSubquery().getWhere(), Collections.emptySet(),
					interim,
					finalInfo);
		}

		boolean wellDesigned = localWellDesigned && childrenWellDesigned;
		finalInfo.put(pattern, info.toSemanticInfo(wellDesigned));
		return wellDesigned;
	}

	private Set<Var> union(Set<Var> left, Set<Var> right) {
		Set<Var> merged = new HashSet<>(left);
		merged.addAll(right);
		return merged;
	}

	private Set<Var> intersect(Set<Var> left, Set<Var> right) {
		Set<Var> result = new HashSet<>(left);
		result.retainAll(right);
		return result;
	}

	private static final class InterimInfo {
		private final Set<Var> vars;
		private final Set<Var> certainVars;
		private final boolean hasNegation;
		private final boolean hasAggregates;
		private final boolean hasLimitOffset;
		private final boolean hasDistinct;
		private final boolean monotone;

		private InterimInfo(Set<Var> vars, Set<Var> certainVars, boolean hasNegation, boolean hasAggregates,
				boolean hasLimitOffset, boolean hasDistinct) {
			this.vars = vars;
			this.certainVars = certainVars;
			this.hasNegation = hasNegation;
			this.hasAggregates = hasAggregates;
			this.hasLimitOffset = hasLimitOffset;
			this.hasDistinct = hasDistinct;
			this.monotone = !hasNegation && !hasAggregates && !hasLimitOffset && !hasDistinct;
		}

		static InterimInfo empty() {
			return new InterimInfo(new HashSet<>(), new HashSet<>(), false, false, false, false);
		}

		SemanticInfo toSemanticInfo(boolean wellDesigned) {
			return new SemanticInfo(vars, certainVars, monotone, wellDesigned, hasNegation, hasAggregates,
					hasLimitOffset,
					hasDistinct);
		}
	}

	private static final class ExprInfo {
		private final Set<Var> vars;
		private final boolean hasNegation;
		private final boolean hasAggregates;

		ExprInfo(Set<Var> vars, boolean hasNegation, boolean hasAggregates) {
			this.vars = vars;
			this.hasNegation = hasNegation;
			this.hasAggregates = hasAggregates;
		}

		static ExprInfo empty() {
			return new ExprInfo(new HashSet<>(), false, false);
		}
	}
}
