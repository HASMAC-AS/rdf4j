package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** COUNT aggregate expression. */
public final class CountAgg implements AggExpr {

	private final boolean distinct;
	private final Expr arg; // null represents COUNT(*)

	public CountAgg(boolean distinct, Expr arg) {
		this.distinct = distinct;
		this.arg = arg;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public Expr getArg() {
		return arg;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CountAgg)) {
			return false;
		}
		CountAgg countAgg = (CountAgg) o;
		return distinct == countAgg.distinct && Objects.equals(arg, countAgg.arg);
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode(distinct);
		result = 31 * result + (arg != null ? arg.hashCode() : 0);
		return result;
	}
}
