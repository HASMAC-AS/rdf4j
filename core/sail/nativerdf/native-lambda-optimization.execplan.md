```md
# Accelerate NativeStore triple scans with lambda-based matchers

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

Reference: maintain this document in accordance with `PLANS.md` at the repository root.

## Purpose / Big Picture

NativeStore range scans currently use `ByteArrayUtil.matchesPattern`, which compares every byte in a triple record even when only a few components are constrained. LMDB now avoids this cost with lambda-selected matchers per binding pattern. By porting that idea, we expect fewer comparisons per record and better query throughput. Success means that NativeStore uses precomputed matchers (chosen via lambdas) while retaining functional parity; correctness is demonstrated by tests that assert the optimized matcher agrees with the legacy matcher for representative patterns.

## Progress

- [x] (2025-11-08 04:39Z) Establish failing regression test covering matcher equivalence (initial `mvn test` compile failure captured in chunk `c44434`).
- [x] (2025-11-08 04:45Z) Implement lambda-selected matcher in `RangeIterator` and integrate with iteration logic.
- [x] (2025-11-08 04:46Z) Update tests/documentation and verify module test suite passes (see chunk `1bf4c4`).

## Surprises & Discoveries

- Observation: Running `mvn test` within `core/sail/nativerdf` fails before compilation because dependent RDF4J artifacts are unresolved snapshots.
  Evidence: Maven error listing missing artifacts such as `rdf4j-sail-base:jar:5.2.1-SNAPSHOT` during the initial test attempt.
- Learned that loop indices referenced inside lambda suppliers must be copied into effectively final locals (`maskBits`) to satisfy the compiler.

## Decision Log

- Decision: Populate local Maven repository with project snapshots by running a root-level `mvn -DskipTests install` before module-specific tests.
  Rationale: Module-only test run failed due to missing sibling artifacts; installing once avoids `-am` usage during later test runs.
  Date/Author: 2025-11-08 / Assistant

## Outcomes & Retrospective

- Added a reusable `ValueMatcher` factory to `RangeIterator` that precomputes field comparisons and flag masks, mirroring the LMDB lambda approach, and validated it against the legacy matcher via new unit tests and the existing NativeStore suite.

## Context and Orientation

`core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/TripleStore.java` drives triple lookups using a B-Tree per index. It calls `BTree.iterateValues`/`iterateRangedValues`, implemented in `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/btree/BTree.java`. These iterators delegate filtering to `RangeIterator`, which currently uses `ByteArrayUtil.matchesPattern` on every record. LMDB’s counterpart replaces per-record mask checks with a `GroupMatcher` whose behavior is selected by lambdas (`IndexKeyWriters.MatcherFactory`). We will introduce a similar matcher for NativeStore records (subject, predicate, object, context, flags) and wire it into `RangeIterator`.

## Plan of Work

1. Add a unit test (likely under `core/sail/nativerdf/src/test/java/org/eclipse/rdf4j/sail/nativerdf/btree`) that exercises the new matcher factory, verifying that for various mask patterns, the optimized matcher agrees with `ByteArrayUtil.matchesPattern` across multiple sample values. This test will initially fail because the matcher factory does not yet exist.
2. Extend `RangeIterator` to construct a reusable matcher object during initialization. Implement the matcher as a nested static helper that precomputes which tuple components must match and selects a lambda-based `MatchFn` similar to LMDB’s `GroupMatcher`. The `next()` method should use this matcher instead of `ByteArrayUtil.matchesPattern`.
3. Ensure integration preserves behavior for existing callers (e.g., handle `null` masks/keys). Update or add any auxiliary documentation if necessary. Run `mvn test` from `core/sail/nativerdf` to confirm tests pass. Apply formatting if needed.

## Concrete Steps

1. Working directory `/workspace/rdf4j`: create `core/sail/nativerdf/src/test/java/org/eclipse/rdf4j/sail/nativerdf/btree/RangeIteratorValueMatcherTest.java` with scenarios covering combinations of bound/unbound components and flags. Run `mvn test` from `core/sail/nativerdf`; expect compilation failure because the matcher is not yet implemented.
2. Edit `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/btree/RangeIterator.java` to add the matcher helper and replace direct calls to `ByteArrayUtil.matchesPattern`. Ensure lambdas/method references cover all combinations efficiently. Re-run `mvn test` in the same module; expect green.
3. Review formatting (apply `mvn -pl core/sail/nativerdf -DskipTests formatter:format` if necessary) and rerun targeted tests. Prepare commit and PR message summarizing optimization and test coverage.

## Validation and Acceptance

- Run `mvn test` from `core/sail/nativerdf`; before implementing the matcher, the new unit test should fail (compilation or assertion). After implementation, it must pass along with existing tests.
- Confirm via the new test that the optimized matcher returns the same results as the legacy byte-wise matcher across diverse inputs.

## Idempotence and Recovery

- The matcher helper is pure and memoized per iterator; re-running tests is safe. If issues arise, revert the new matcher selection to the previous `ByteArrayUtil.matchesPattern` call and rerun tests to restore baseline behavior.

## Artifacts and Notes

- Capture snippets from failing and passing `mvn test` runs to demonstrate TDD adherence.

## Interfaces and Dependencies

- Introduce a package-private static helper within `RangeIterator` (e.g., `RangeIterator.ValueMatcher`) with factory `static ValueMatcher create(byte[] key, byte[] mask)`. The helper exposes `boolean matches(byte[] value)` and internally chooses a `MatchFn` lambda based on which tuple components are constrained.
```
