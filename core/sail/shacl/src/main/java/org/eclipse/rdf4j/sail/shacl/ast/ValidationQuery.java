package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationReportNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

public class ValidationQuery {

	private String query;
	private ConstraintComponent.Scope scope;
	private ConstraintComponent.Scope scope_validationReport;

	private final List<StatementMatcher.Variable> variables;

	int targetIndex;
	int valueIndex;

	int targetIndex_validationReport;
	int valueIndex_validationReport;

	private SourceConstraintComponent constraintComponent;
	private SourceConstraintComponent constraintComponent_validationReport;

	private Severity severity;
	private Shape shape;

	public ValidationQuery(String query, List<StatementMatcher.Variable> targets, StatementMatcher.Variable value,
			ConstraintComponent.Scope scope, SourceConstraintComponent constraintComponent, Severity severity,
			Shape shape) {
		this.query = query;

		List<StatementMatcher.Variable> variables = new ArrayList<>(targets);
		variables.add(value);
		this.variables = Collections.unmodifiableList(variables);
		if (scope == ConstraintComponent.Scope.propertyShape) {
			targetIndex = targets.size() - 1;
			// TODO handle property shape without value
		} else {
			targetIndex = variables.size() - 1;
		}

		valueIndex = variables.size() - 1;
		this.scope = scope;
		this.constraintComponent = constraintComponent;
		this.severity = severity;
		this.shape = shape;
	}

	public ValidationQuery(String query, ConstraintComponent.Scope scope, List<StatementMatcher.Variable> variables,
			int targetIndex, int valueIndex) {
		this.query = query;
		this.scope = scope;
		this.variables = Collections.unmodifiableList(variables);
		this.targetIndex = targetIndex;
		this.valueIndex = valueIndex;
	}

	public static ValidationQuery union(ValidationQuery a, ValidationQuery b) {
		assert a.getTargetVariable(false).equals(b.getTargetVariable(false));
		assert a.getValueVariable(false).equals(b.getValueVariable(false));
		assert a.scope == b.scope;
		assert a.targetIndex == b.targetIndex;
		assert a.valueIndex == b.valueIndex;

		String unionQuery = "{\n" + a.getQuery() + "\n} UNION {\n" + b.query + "\n}";

		ValidationQuery validationQuery = new ValidationQuery(unionQuery, a.scope,
				a.variables.subList(0, a.valueIndex + 1), a.targetIndex, a.valueIndex);

		return validationQuery;

	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public PlanNode getValidationPlan(SailConnection baseConnection) {

		assert query != null;

		StringBuilder fullQuery = new StringBuilder();

		fullQuery.append("select distinct ");
		fullQuery.append("?").append(getTargetVariable(true)).append(" ");
		if (!getValueVariable(true).equals(getTargetVariable(true))) {
			fullQuery.append("?").append(getValueVariable(true)).append(" ");
		}
		fullQuery.append("{\n").append(query).append("\n}");

		Select select = new Select(baseConnection, fullQuery.toString(), null, bindings -> {

			return new ValidationTuple(bindings.getValue(getTargetVariable(true)),
					bindings.getValue(getValueVariable(true)),
					scope_validationReport, true);
		});

		return new ValidationReportNode(select, t -> {
			return new ValidationResult(t.getActiveTarget(), t.getValue(), shape,
					constraintComponent_validationReport, severity, t.getScope());
		});

	}

	private String getValueVariable(boolean forValidationReport) {
		if (forValidationReport) {
			return variables.get(valueIndex_validationReport).name;
		}
		return variables.get(valueIndex).name;
	}

	private String getTargetVariable(boolean forValidationReport) {
		if (forValidationReport) {
			return variables.get(targetIndex_validationReport).name;
		}
		return variables.get(targetIndex).name;
	}

	public ValidationQuery withSeverity(Severity severity) {
		this.severity = severity;
		return this;
	}

	public ValidationQuery withShape(Shape shape) {
		this.shape = shape;
		return this;
	}

	public void popTargetChain() {
		assert scope == ConstraintComponent.Scope.propertyShape;
		targetIndex--;
		valueIndex--;
	}

	public void shiftToNodeShape() {
		this.scope = ConstraintComponent.Scope.nodeShape;
		valueIndex--;
	}

	public void shiftToPropertyShape() {
		this.scope = ConstraintComponent.Scope.propertyShape;
		targetIndex--;
	}

	public ValidationQuery withConstraintComponent(SourceConstraintComponent constraintComponent) {
		this.constraintComponent = constraintComponent;
		return this;
	}

	public void makeCurrentStateValidationReport() {
		valueIndex_validationReport = valueIndex;
		targetIndex_validationReport = targetIndex;
		scope_validationReport = scope;
		constraintComponent_validationReport = constraintComponent;
	}
}
