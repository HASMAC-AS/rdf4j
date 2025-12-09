package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** FILTER wrapper. */
public final class FilterPattern implements Pattern {

	private final Expr condition;
	private final Pattern inner;

	public FilterPattern(Expr condition, Pattern inner) {
		this.condition = Objects.requireNonNull(condition, "condition");
		this.inner = Objects.requireNonNull(inner, "inner");
	}

	public Expr getCondition() {
		return condition;
	}

	public Pattern getInner() {
		return inner;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FilterPattern)) {
			return false;
		}
		FilterPattern that = (FilterPattern) o;
		return condition.equals(that.condition) && inner.equals(that.inner);
	}

	@Override
	public int hashCode() {
		int result = condition.hashCode();
		result = 31 * result + inner.hashCode();
		return result;
	}
}
