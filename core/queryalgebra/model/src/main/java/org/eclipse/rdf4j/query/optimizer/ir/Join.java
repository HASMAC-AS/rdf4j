package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Binary join pattern. */
public final class Join implements Pattern {

	private final Pattern left;
	private final Pattern right;

	public Join(Pattern left, Pattern right) {
		this.left = Objects.requireNonNull(left, "left");
		this.right = Objects.requireNonNull(right, "right");
	}

	public Pattern getLeft() {
		return left;
	}

	public Pattern getRight() {
		return right;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Join)) {
			return false;
		}
		Join join = (Join) o;
		return left.equals(join.left) && right.equals(join.right);
	}

	@Override
	public int hashCode() {
		int result = left.hashCode();
		result = 31 * result + right.hashCode();
		return result;
	}
}
