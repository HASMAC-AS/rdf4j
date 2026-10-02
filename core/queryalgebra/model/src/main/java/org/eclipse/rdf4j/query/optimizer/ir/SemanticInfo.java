package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Collections;
import java.util.Set;

/** Semantic metadata attached to a pattern. */
public final class SemanticInfo {

	private final Set<Var> vars;
	private final Set<Var> certainVars;
	private final boolean monotone;
	private final boolean wellDesigned;
	private final boolean hasNegation;
	private final boolean hasAggregates;
	private final boolean hasLimitOffset;
	private final boolean hasDistinct;

	public SemanticInfo(Set<Var> vars, Set<Var> certainVars, boolean monotone, boolean wellDesigned,
			boolean hasNegation, boolean hasAggregates, boolean hasLimitOffset, boolean hasDistinct) {
		this.vars = Collections.unmodifiableSet(vars);
		this.certainVars = Collections.unmodifiableSet(certainVars);
		this.monotone = monotone;
		this.wellDesigned = wellDesigned;
		this.hasNegation = hasNegation;
		this.hasAggregates = hasAggregates;
		this.hasLimitOffset = hasLimitOffset;
		this.hasDistinct = hasDistinct;
	}

	public Set<Var> getVars() {
		return vars;
	}

	public Set<Var> getCertainVars() {
		return certainVars;
	}

	public boolean isMonotone() {
		return monotone;
	}

	public boolean isWellDesigned() {
		return wellDesigned;
	}

	public boolean hasNegation() {
		return hasNegation;
	}

	public boolean hasAggregates() {
		return hasAggregates;
	}

	public boolean hasLimitOffset() {
		return hasLimitOffset;
	}

	public boolean hasDistinct() {
		return hasDistinct;
	}
}
