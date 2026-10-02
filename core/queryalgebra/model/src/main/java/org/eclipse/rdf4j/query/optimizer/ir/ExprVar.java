package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Template placeholder that binds to any {@link Expr}. */
public final class ExprVar implements ExprTemplate {

  private final String name;

  public ExprVar(String name) {
    this.name = Objects.requireNonNull(name, "name");
  }

  public String getName() {
    return name;
  }
}
