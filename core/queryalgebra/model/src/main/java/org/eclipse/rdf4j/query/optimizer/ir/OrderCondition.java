package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** ORDER BY clause element. */
public final class OrderCondition {

	public enum Direction {
		ASC,
		DESC
	}

	private final Expr expression;
	private final Direction direction;

	public OrderCondition(Expr expression, Direction direction) {
		this.expression = Objects.requireNonNull(expression, "expression");
		this.direction = Objects.requireNonNull(direction, "direction");
	}

	public Expr getExpression() {
		return expression;
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof OrderCondition)) {
			return false;
		}
		OrderCondition that = (OrderCondition) o;
		return expression.equals(that.expression) && direction == that.direction;
	}

	@Override
	public int hashCode() {
		int result = expression.hashCode();
		result = 31 * result + direction.hashCode();
		return result;
	}
}
