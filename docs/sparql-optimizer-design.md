# SPARQL Optimizer Architecture

This document outlines a practical architecture for a rewrite-driven SPARQL optimizer that is provably safe, transparent, and aggressive in the shapes it can explore. The design is split into the logical IR, static analyses, rule language, validator, and rewrite engine.

## End-to-end pipeline

```
SPARQL text
   ↓ parse
AST (syntax tree)
   ↓ normalize
Logical IR (object model)
   ↓ static analyses (+ annotations)
Annotated IR
   ↓ rewrite engine (using rule language)
Optimized IR
   ↓ lower to SPARQL algebra
Algebra tree
   ↓ physical planning, execution
Plan
```

## Logical IR

Use an immutable, algebra-lite IR that mirrors SPARQL semantics without syntactic sugar.

### Queries

- **SelectQuery**: `distinct`, `projection`, `where`, `orderBy`, `limit`, `offset`, `groupBy`, optional `having`.
- **ConstructQuery**: `template`, `where`, `orderBy`, `limit`, `offset`.
- **AskQuery**: `where` only.
- **DescribeQuery**: `describeTerms`, optional `where`.
- **ProjectionElement** pairs an expression (variable or arbitrary expression) with an output variable.

### Patterns

Core pattern nodes are intentionally small but expressive:

- `EmptyPattern`: identity for joins.
- `Bgp`: list of triple patterns.
- `Join`: binary join.
- `LeftJoin`: OPTIONAL with join condition.
- `UnionPattern`: list of alternatives.
- `FilterPattern`: condition plus inner pattern.
- `MinusPattern`: MINUS.
- `GraphPattern`: named graph wrapper.
- `ServicePattern`: SERVICE, with `silent` flag.
- `ValuesPattern`: VALUES rows.
- `SubqueryPattern`: embedded SELECT.

Terms cover `Var`, `Iri`, `Literal`, `BlankNode`, and `TriplePattern` captures subject/predicate/object. Property paths can be normalized separately.

### Expressions

Expression IR includes variable refs, term literals, unary/binary operators (logical/comparison/arithmetic), function calls, EXISTS/NOT EXISTS, and aggregate expressions (COUNT, SUM, MIN, MAX, AVG, GROUP_CONCAT, SAMPLE, etc.). All nodes are immutable for easy structural equality.

## Normalization

A deterministic normalizer converts parsed ASTs into IR, expanding syntactic sugar (`;`, `,`, PREFIX/BASE), canonicalizing OPTIONAL/UNION/FILTER forms, flattening trivial nesting, mapping FILTER EXISTS into EXISTS expressions, and making `SELECT *` explicit. This pass removes surface syntax so all downstream work uses the IR only.

## Static analyses and annotations

Attach a `SemanticInfo` object per pattern:

- `vars`: variables mentioned.
- `certainVars`: variables always bound when a solution is produced.
- `monotone`: absence of negation, aggregates, LIMIT/OFFSET, DISTINCT.
- `wellDesigned`: OPTIONAL well-designedness flag.
- Optional flags: `hasNegation`, `hasAggregates`, `hasLimitOffset`, `hasDistinct`.

Suggested passes:

1. **Vars/flags (bottom-up):** collect `vars` for each node and flags for negation/aggregates/limits/distinct; compute `exprVars` for expressions.
2. **Certain vars (bottom-up):**
   - BGP/Join: union of child certain vars.
   - Filter: same as child.
   - LeftJoin: certain vars from the left only.
   - Union: intersection of child certain vars.
   - Minus: certain vars of the left.
   - Values: treat columns as non-certain unless all rows bind them.
   - Subquery: projected vars are certain.
3. **Monotonicity (local flags):** `monotone = !hasNegation && !hasAggregates && !hasLimitOffset && !hasDistinct`.
4. **Well-designed OPTIONAL (top-down):** compute `varsOutside` per node; a LeftJoin is well-designed if every variable in its right child that also appears outside the LeftJoin also appears in the left child. Cache the overall boolean.

Type/domain analysis can refine rewrite conditions (e.g., subject position implies IRI/blank nodes only).

## Rule language

Rules pair templates with side conditions:

- **RewriteRule**: `name`, `before` template, `after` template, optional `condition` (`MatchContext → boolean`).
- **Templates** mirror IR shapes but allow meta-variables; a matcher binds meta-variables to concrete nodes, producing a substitution. Substitution recreates the `after` fragment with captured parts plugged in.
- **Conditions** use semantic info: `vars`, `certainVars`, `monotone`, `wellDesigned`, `exprVars`, optional `domain(var)`, etc.

Example (VALUES into UNION):

```
before: Join(Values($vs, $rows), Union($p1, $p2))
after:  Union(Join(Values($vs, $rows), $p1), Join(Values($vs, $rows), $p2))
when:   true (cost model can refine)
```

## Validator

The validator replays claimed rewrites to ensure safety:

1. Locate the target subtree in the current IR by a path-based location.
2. Match `before` against the subtree; ensure the provided substitution is consistent with matcher output.
3. Check the rule’s `condition` with semantic annotations.
4. Instantiate `after` with the substitution and verify it matches the optimizer’s produced subtree.
5. Repeat for all steps; final IR must equal the optimizer’s output and remain well-formed.

Immutability makes replay cheap and equality checks reliable.

## Rewrite engine

Position the engine after analysis and before algebra lowering. Use a worklist to try rules at each pattern node, optionally organized in phases (canonicalization → filter pushdown/join assoc/comm → OPTIONAL/UNION rewrites → BIND/VALUES duplication → subquery reshaping). Apply a cost model to pick among equivalent candidates and re-run local analyses when subtrees change.

## Advanced rewrites enabled

- **BIND/VALUES duplication** into UNION/OPTIONAL/SERVICE to expose pushdown opportunities.
- **OPTIONAL factorization** via sequences of associativity/distributivity rules guarded by well-designedness.
- **Subquery introduction/elimination** to control evaluation boundaries.
- **Type-aware equality merging**: replace `FILTER(?x = ?y)` with variable unification when both are certain, monotone context holds, and domains restrict to IRIs/blank nodes (with BIND added if the alias is projected).

## Engineering guidance

- Keep passes pure and layered (parse → normalize → analyze → optimize → lower).
- Provide logging/dry-run modes to trace applied rules and before/after IRs.
- Test rules with focused cases and randomized differential testing against a reference engine.
- Cache analyses for unchanged subtrees; consider structural sharing to limit memory.
- Add new rules by encoding algebraic equivalences with explicit conditions and tests, marking experimental ones behind feature flags if desired.
