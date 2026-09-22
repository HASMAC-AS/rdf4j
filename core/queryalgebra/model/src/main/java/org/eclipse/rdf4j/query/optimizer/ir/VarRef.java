package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Reference to a variable. */
public final class VarRef implements Expr {

	private final Var var;

	public VarRef(Var var) {
		this.var = Objects.requireNonNull(var, "var");
	}

	public Var getVar() {
		return var;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof VarRef)) {
			return false;
		}
		VarRef varRef = (VarRef) o;
		return var.equals(varRef.var);
	}

	@Override
	public int hashCode() {
		return var.hashCode();
	}
}
