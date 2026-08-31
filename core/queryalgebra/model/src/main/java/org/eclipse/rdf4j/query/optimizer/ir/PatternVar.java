package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.Objects;

/** Template placeholder that binds to any {@link Pattern}. */
public final class PatternVar implements PatternTemplate {

  private final String name;

  public PatternVar(String name) {
    this.name = Objects.requireNonNull(name, "name");
  }

  public String getName() {
    return name;
  }
}
