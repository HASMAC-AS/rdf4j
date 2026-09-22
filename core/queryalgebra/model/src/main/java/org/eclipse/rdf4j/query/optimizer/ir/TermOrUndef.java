package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;
import java.util.Optional;

/** Represents a term column that may be UNDEF in a VALUES row. */
public final class TermOrUndef {

	private static final TermOrUndef UNDEF = new TermOrUndef(null);

	private final Term term;

	private TermOrUndef(Term term) {
		this.term = term;
	}

	public static TermOrUndef of(Term term) {
		return new TermOrUndef(Objects.requireNonNull(term, "term"));
	}

	public static TermOrUndef undef() {
		return UNDEF;
	}

	public Optional<Term> getTerm() {
		return Optional.ofNullable(term);
	}

	public boolean isUndef() {
		return term == null;
	}
}
