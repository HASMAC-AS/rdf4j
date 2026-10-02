package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** GRAPH pattern to scope to a named graph. */
public final class GraphPattern implements Pattern {

	private final Term graphName;
	private final Pattern inner;

	public GraphPattern(Term graphName, Pattern inner) {
		this.graphName = Objects.requireNonNull(graphName, "graphName");
		this.inner = Objects.requireNonNull(inner, "inner");
	}

	public Term getGraphName() {
		return graphName;
	}

	public Pattern getInner() {
		return inner;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof GraphPattern)) {
			return false;
		}
		GraphPattern that = (GraphPattern) o;
		return graphName.equals(that.graphName) && inner.equals(that.inner);
	}

	@Override
	public int hashCode() {
		int result = graphName.hashCode();
		result = 31 * result + inner.hashCode();
		return result;
	}
}
