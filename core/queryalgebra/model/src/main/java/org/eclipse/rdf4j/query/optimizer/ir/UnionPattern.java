package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** UNION pattern with multiple alternatives. */
public final class UnionPattern implements Pattern {

	private final List<Pattern> alternatives;

	public UnionPattern(List<Pattern> alternatives) {
		this.alternatives = Collections.unmodifiableList(Objects.requireNonNull(alternatives, "alternatives"));
	}

	public List<Pattern> getAlternatives() {
		return alternatives;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UnionPattern)) {
			return false;
		}
		UnionPattern that = (UnionPattern) o;
		return alternatives.equals(that.alternatives);
	}

	@Override
	public int hashCode() {
		return alternatives.hashCode();
	}
}
