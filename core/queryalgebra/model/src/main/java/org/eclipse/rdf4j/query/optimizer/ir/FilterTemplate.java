package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Template for {@link FilterPattern} nodes. */
public final class FilterTemplate implements PatternTemplate {

  private final ExprTemplate condition;
  private final PatternTemplate inner;

  public FilterTemplate(ExprTemplate condition, PatternTemplate inner) {
    this.condition = Objects.requireNonNull(condition, "condition");
    this.inner = Objects.requireNonNull(inner, "inner");
  }

  public ExprTemplate getCondition() {
    return condition;
  }

  public PatternTemplate getInner() {
    return inner;
  }
}
