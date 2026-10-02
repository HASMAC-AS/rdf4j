package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Binary operator expression. */
public final class BinaryExpr implements Expr {

	public enum Op {
		AND,
		OR,
		EQ,
		NEQ,
		LT,
		GT,
		LE,
		GE,
		PLUS,
		MINUS,
		MULT,
		DIV
	}

	private final Op op;
	private final Expr left;
	private final Expr right;

	public BinaryExpr(Op op, Expr left, Expr right) {
		this.op = Objects.requireNonNull(op, "op");
		this.left = Objects.requireNonNull(left, "left");
		this.right = Objects.requireNonNull(right, "right");
	}

	public Op getOp() {
		return op;
	}

	public Expr getLeft() {
		return left;
	}

	public Expr getRight() {
		return right;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BinaryExpr)) {
			return false;
		}
		BinaryExpr that = (BinaryExpr) o;
		return op == that.op && left.equals(that.left) && right.equals(that.right);
	}

	@Override
	public int hashCode() {
		int result = op.hashCode();
		result = 31 * result + left.hashCode();
		result = 31 * result + right.hashCode();
		return result;
	}
}
