package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** OPTIONAL pattern with optional condition. */
public final class LeftJoin implements Pattern {

	private final Pattern left;
	private final Pattern right;
	private final Expr condition; // may be null to indicate TRUE

	public LeftJoin(Pattern left, Pattern right, Expr condition) {
		this.left = Objects.requireNonNull(left, "left");
		this.right = Objects.requireNonNull(right, "right");
		this.condition = condition;
	}

	public Pattern getLeft() {
		return left;
	}

	public Pattern getRight() {
		return right;
	}

	public Expr getCondition() {
		return condition;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof LeftJoin)) {
			return false;
		}
		LeftJoin that = (LeftJoin) o;
		return left.equals(that.left)
				&& right.equals(that.right)
				&& Objects.equals(condition, that.condition);
	}

	@Override
	public int hashCode() {
		int result = left.hashCode();
		result = 31 * result + right.hashCode();
		result = 31 * result + (condition != null ? condition.hashCode() : 0);
		return result;
	}
}
