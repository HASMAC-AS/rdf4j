package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** NOT EXISTS subquery expression. */
public final class NotExistsExpr implements Expr {

	private final Pattern pattern;

	public NotExistsExpr(Pattern pattern) {
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
		if (!(o instanceof NotExistsExpr)) {
			return false;
		}
		NotExistsExpr that = (NotExistsExpr) o;
		return pattern.equals(that.pattern);
	}

	@Override
	public int hashCode() {
		return pattern.hashCode();
	}
}
