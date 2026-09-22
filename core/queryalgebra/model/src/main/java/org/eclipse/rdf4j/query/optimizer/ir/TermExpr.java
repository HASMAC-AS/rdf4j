package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Wraps a constant term as an expression. */
public final class TermExpr implements Expr {

	private final Term term;

	public TermExpr(Term term) {
		this.term = Objects.requireNonNull(term, "term");
	}

	public Term getTerm() {
		return term;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TermExpr)) {
			return false;
		}
		TermExpr termExpr = (TermExpr) o;
		return term.equals(termExpr.term);
	}

	@Override
	public int hashCode() {
		return term.hashCode();
	}
}
