package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Represents a function call expression. */
public final class FunctionCall implements Expr {

	private final Iri functionIri;
	private final List<Expr> args;

	public FunctionCall(Iri functionIri, List<Expr> args) {
		this.functionIri = Objects.requireNonNull(functionIri, "functionIri");
		this.args = Collections.unmodifiableList(Objects.requireNonNull(args, "args"));
	}

	public Iri getFunctionIri() {
		return functionIri;
	}

	public List<Expr> getArgs() {
		return args;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FunctionCall)) {
			return false;
		}
		FunctionCall that = (FunctionCall) o;
		return functionIri.equals(that.functionIri) && args.equals(that.args);
	}

	@Override
	public int hashCode() {
		int result = functionIri.hashCode();
		result = 31 * result + args.hashCode();
		return result;
	}
}
