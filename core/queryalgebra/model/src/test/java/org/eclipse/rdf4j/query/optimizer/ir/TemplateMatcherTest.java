package org.eclipse.rdf4j.query.optimizer.ir;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.rdf4j.query.optimizer.ir.Matcher.Substitution;
import org.junit.jupiter.api.Test;

class TemplateMatcherTest {

  private final Var s = new Var("s");
  private final Var p = new Var("p");
  private final Var o = new Var("o");

  @Test
  void matchesJoinWithSharedPatternVar() {
    Bgp bgp = new Bgp(Collections.singletonList(new TriplePattern(s, p, o)));
    Pattern join = new Join(bgp, bgp);

    PatternTemplate template = new JoinTemplate(new PatternVar("P"), new PatternVar("P"));
    Matcher matcher = new Matcher();

    Optional<Substitution> result = matcher.match(template, join);

    assertThat(result).isPresent();
    assertThat(result.get().getPattern("P")).hasValue(bgp);
  }

  @Test
  void refusesInconsistentBindings() {
    Bgp left = new Bgp(Collections.singletonList(new TriplePattern(s, p, o)));
    Bgp right = new Bgp(Collections.singletonList(new TriplePattern(o, p, s)));
    Pattern join = new Join(left, right);

    PatternTemplate template = new JoinTemplate(new PatternVar("P"), new PatternVar("P"));
    Matcher matcher = new Matcher();

    assertThat(matcher.match(template, join)).isEmpty();
  }

  @Test
  void bindsExpressionsAndPatterns() {
    Expr condition = new BinaryExpr(BinaryExpr.Op.EQ, new VarRef(s), new VarRef(o));
    FilterPattern filter =
        new FilterPattern(condition, new Bgp(Collections.singletonList(new TriplePattern(s, p, o))));

    PatternTemplate template = new FilterTemplate(new ExprVar("F"), new PatternVar("P"));
    Matcher matcher = new Matcher();

    Optional<Substitution> result = matcher.match(template, filter);

    assertThat(result).isPresent();
    assertThat(result.get().getExpr("F")).hasValue(condition);
    assertThat(result.get().getPattern("P")).hasValue(filter.getInner());
  }
}
