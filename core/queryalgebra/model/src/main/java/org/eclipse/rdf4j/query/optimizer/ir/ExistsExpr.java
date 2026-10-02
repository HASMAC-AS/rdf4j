package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** EXISTS subquery expression. */
public final class ExistsExpr implements Expr {

	private final Pattern pattern;

	public ExistsExpr(Pattern pattern) {
		this.pattern = Objects.requireNonNull(pattern, "pattern");
	}

	public Pattern getPattern() {
		return pattern;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ExistsExpr)) {
			return false;
		}
		ExistsExpr that = (ExistsExpr) o;
		return pattern.equals(that.pattern);
	}

	@Override
	public int hashCode() {
		return pattern.hashCode();
	}
}
