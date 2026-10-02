package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Basic graph pattern. */
public final class Bgp implements Pattern {

	private final List<TriplePattern> triples;

	public Bgp(List<TriplePattern> triples) {
		this.triples = Collections.unmodifiableList(Objects.requireNonNull(triples, "triples"));
	}

	public List<TriplePattern> getTriples() {
		return triples;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Bgp)) {
			return false;
		}
		Bgp bgp = (Bgp) o;
		return triples.equals(bgp.triples);
	}

	@Override
	public int hashCode() {
		return triples.hashCode();
	}
}
