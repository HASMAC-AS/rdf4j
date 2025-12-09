package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Embedded SELECT subquery. */
public final class SubqueryPattern implements Pattern {

	private final SelectQuery subquery;

	public SubqueryPattern(SelectQuery subquery) {
		this.subquery = Objects.requireNonNull(subquery, "subquery");
	}

	public SelectQuery getSubquery() {
		return subquery;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SubqueryPattern)) {
			return false;
		}
		SubqueryPattern that = (SubqueryPattern) o;
		return subquery.equals(that.subquery);
	}

	@Override
	public int hashCode() {
		return subquery.hashCode();
	}
}
