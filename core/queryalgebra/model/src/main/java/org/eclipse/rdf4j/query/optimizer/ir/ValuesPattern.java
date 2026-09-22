package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** VALUES clause. */
public final class ValuesPattern implements Pattern {

	private final List<Var> vars;
	private final List<List<TermOrUndef>> rows;

	public ValuesPattern(List<Var> vars, List<List<TermOrUndef>> rows) {
		this.vars = Collections.unmodifiableList(Objects.requireNonNull(vars, "vars"));
		this.rows = Collections.unmodifiableList(Objects.requireNonNull(rows, "rows"));
	}

	public List<Var> getVars() {
		return vars;
	}

	public List<List<TermOrUndef>> getRows() {
		return rows;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ValuesPattern)) {
			return false;
		}
		ValuesPattern that = (ValuesPattern) o;
		return vars.equals(that.vars) && rows.equals(that.rows);
	}

	@Override
	public int hashCode() {
		int result = vars.hashCode();
		result = 31 * result + rows.hashCode();
		return result;
	}
}
