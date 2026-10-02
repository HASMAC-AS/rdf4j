package org.eclipse.rdf4j.query.optimizer.ir;

/** Identity pattern for joins. */
public final class EmptyPattern implements Pattern {

	@Override
	public boolean equals(Object o) {
		return o instanceof EmptyPattern;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}
