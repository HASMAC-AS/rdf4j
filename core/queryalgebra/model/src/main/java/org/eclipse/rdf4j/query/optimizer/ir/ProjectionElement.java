package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Projection expression paired with output variable. */
public final class ProjectionElement {

	private final Expr expr;
	private final Var as;

	public ProjectionElement(Expr expr, Var as) {
		this.expr = Objects.requireNonNull(expr, "expr");
		this.as = Objects.requireNonNull(as, "as");
	}

	public Expr getExpr() {
		return expr;
	}

	public Var getAs() {
		return as;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ProjectionElement)) {
			return false;
		}
		ProjectionElement that = (ProjectionElement) o;
		return expr.equals(that.expr) && as.equals(that.as);
	}

	@Override
	public int hashCode() {
		int result = expr.hashCode();
		result = 31 * result + as.hashCode();
		return result;
	}
}
