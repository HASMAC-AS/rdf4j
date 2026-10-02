package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Represents a triple pattern. */
public final class TriplePattern {

	private final Term subject;
	private final Term predicate;
	private final Term object;

	public TriplePattern(Term subject, Term predicate, Term object) {
		this.subject = Objects.requireNonNull(subject, "subject");
		this.predicate = Objects.requireNonNull(predicate, "predicate");
		this.object = Objects.requireNonNull(object, "object");
	}

	public Term getSubject() {
		return subject;
	}

	public Term getPredicate() {
		return predicate;
	}

	public Term getObject() {
		return object;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TriplePattern)) {
			return false;
		}
		TriplePattern that = (TriplePattern) o;
		return subject.equals(that.subject)
				&& predicate.equals(that.predicate)
				&& object.equals(that.object);
	}

	@Override
	public int hashCode() {
		int result = subject.hashCode();
		result = 31 * result + predicate.hashCode();
		result = 31 * result + object.hashCode();
		return result;
	}
}
