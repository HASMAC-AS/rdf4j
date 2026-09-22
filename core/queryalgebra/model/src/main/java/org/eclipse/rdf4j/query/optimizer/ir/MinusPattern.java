package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** MINUS pattern representing negation. */
public final class MinusPattern implements Pattern {

	private final Pattern left;
	private final Pattern right;

	public MinusPattern(Pattern left, Pattern right) {
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
		if (!(o instanceof MinusPattern)) {
			return false;
		}
		MinusPattern that = (MinusPattern) o;
		return left.equals(that.left) && right.equals(that.right);
	}

	@Override
	public int hashCode() {
		int result = left.hashCode();
		result = 31 * result + right.hashCode();
		return result;
	}
}
