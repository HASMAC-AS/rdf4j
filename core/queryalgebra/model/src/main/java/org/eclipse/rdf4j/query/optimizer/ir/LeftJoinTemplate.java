package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Template for {@link LeftJoin} nodes. */
public final class LeftJoinTemplate implements PatternTemplate {

  private final PatternTemplate left;
  private final PatternTemplate right;
  private final ExprTemplate condition;

  public LeftJoinTemplate(PatternTemplate left, PatternTemplate right, ExprTemplate condition) {
    this.left = Objects.requireNonNull(left, "left");
    this.right = Objects.requireNonNull(right, "right");
    this.condition = condition;
  }

  public PatternTemplate getLeft() {
    return left;
  }

  public PatternTemplate getRight() {
    return right;
  }

  public ExprTemplate getCondition() {
    return condition;
  }
}
