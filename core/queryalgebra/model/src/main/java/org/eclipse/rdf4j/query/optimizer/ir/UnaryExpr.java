package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Unary operator expression. */
public final class UnaryExpr implements Expr {

	public enum Op {
		NOT,
		PLUS,
		MINUS
	}

	private final Op op;
	private final Expr arg;

	public UnaryExpr(Op op, Expr arg) {
		this.op = Objects.requireNonNull(op, "op");
		this.arg = Objects.requireNonNull(arg, "arg");
	}

	public Op getOp() {
		return op;
	}

	public Expr getArg() {
		return arg;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UnaryExpr)) {
			return false;
		}
		UnaryExpr unaryExpr = (UnaryExpr) o;
		return op == unaryExpr.op && arg.equals(unaryExpr.arg);
	}

	@Override
	public int hashCode() {
		int result = op.hashCode();
		result = 31 * result + arg.hashCode();
		return result;
	}
}
