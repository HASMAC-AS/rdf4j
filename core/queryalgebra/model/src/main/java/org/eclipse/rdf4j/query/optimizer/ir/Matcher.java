package org.eclipse.rdf4j.query.optimizer.ir;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Matches pattern and expression templates against concrete IR nodes. */
public final class Matcher {

  public Optional<Substitution> match(PatternTemplate template, Pattern pattern) {
    Substitution substitution = new Substitution();
    boolean ok = matchPattern(template, pattern, substitution);
    return ok ? Optional.of(substitution) : Optional.empty();
  }

  private boolean matchPattern(PatternTemplate template, Pattern pattern, Substitution substitution) {
    if (template instanceof PatternVar) {
      return substitution.bindPattern(((PatternVar) template).getName(), pattern);
    }

    if (template instanceof JoinTemplate && pattern instanceof Join) {
      JoinTemplate joinTemplate = (JoinTemplate) template;
      Join join = (Join) pattern;
      return matchPattern(joinTemplate.getLeft(), join.getLeft(), substitution)
          && matchPattern(joinTemplate.getRight(), join.getRight(), substitution);
    }

    if (template instanceof LeftJoinTemplate && pattern instanceof LeftJoin) {
      LeftJoinTemplate leftJoinTemplate = (LeftJoinTemplate) template;
      LeftJoin leftJoin = (LeftJoin) pattern;
      return matchPattern(leftJoinTemplate.getLeft(), leftJoin.getLeft(), substitution)
          && matchPattern(leftJoinTemplate.getRight(), leftJoin.getRight(), substitution)
          && matchExpr(leftJoinTemplate.getCondition(), leftJoin.getCondition(), substitution);
    }

    if (template instanceof FilterTemplate && pattern instanceof FilterPattern) {
      FilterTemplate filterTemplate = (FilterTemplate) template;
      FilterPattern filter = (FilterPattern) pattern;
      return matchExpr(filterTemplate.getCondition(), filter.getCondition(), substitution)
          && matchPattern(filterTemplate.getInner(), filter.getInner(), substitution);
    }

    return false;
  }

  private boolean matchExpr(ExprTemplate template, Expr expr, Substitution substitution) {
    if (template == null) {
      return expr == null;
    }
    if (template instanceof ExprVar) {
      return substitution.bindExpr(((ExprVar) template).getName(), expr);
    }
    return template.equals(expr);
  }

  /** Holds bindings produced during matching. */
  public static final class Substitution {

    private final Map<String, Pattern> patternBindings = new HashMap<>();
    private final Map<String, Expr> exprBindings = new HashMap<>();

    public Optional<Pattern> getPattern(String name) {
      return Optional.ofNullable(patternBindings.get(name));
    }

    public Optional<Expr> getExpr(String name) {
      return Optional.ofNullable(exprBindings.get(name));
    }

    boolean bindPattern(String name, Pattern pattern) {
      Pattern existing = patternBindings.get(name);
      if (existing == null) {
        patternBindings.put(name, pattern);
        return true;
      }
      return existing.equals(pattern);
    }

    boolean bindExpr(String name, Expr expr) {
      Expr existing = exprBindings.get(name);
      if (existing == null) {
        exprBindings.put(name, expr);
        return true;
      }
      return existing.equals(expr);
    }
  }
}
