package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Template for {@link Join} nodes. */
public final class JoinTemplate implements PatternTemplate {

  private final PatternTemplate left;
  private final PatternTemplate right;

  public JoinTemplate(PatternTemplate left, PatternTemplate right) {
    this.left = Objects.requireNonNull(left, "left");
    this.right = Objects.requireNonNull(right, "right");
  }

  public PatternTemplate getLeft() {
    return left;
  }

  public PatternTemplate getRight() {
    return right;
  }
}
