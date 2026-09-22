package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Represents a variable term. */
public final class Var implements Term {

	private final String name;

	public Var(String name) {
		this.name = Objects.requireNonNull(name, "name");
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Var)) {
			return false;
		}
		Var var = (Var) o;
		return name.equals(var.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "?" + name;
	}
}
