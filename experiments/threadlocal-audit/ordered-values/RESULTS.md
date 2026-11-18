# Ordered Workbench values: qualification

## Candidate and boundaries

The query evaluator still collects every source binding in declared order before invoking any lexical/value packing code. For the concrete TupleResultBuilder and eligible compact page writer, its privately owned scratch List<Value> is consumed synchronously by position, avoiding the intermediate Object[] / QueryBindingSet / SimpleBinding round-trip and second binding-name lookup. It does **not** remove packed row/term arrays, materialization, JSON validation, batching, flushing or cancellation checkpoints.

Eligibility requires compact mode, unique/non-null writer-owned header names, exact width, and current builder-column alignment. Alignment is checked every call after all source getters because builder.variables retains a caller-mutable name array. Subclasses, inline mocks that do not accept the boolean attempt, ordinary page/legacy/download writers and ineligible shapes retain original result(Object...) dispatch. The generic public object conversion API is unchanged.

The new route is package-private, distinctly named, and not a public mutable-list API. Production lists are exclusively evaluator-local; the concrete builder and final writer neither retain them nor expose them to callbacks. Shared beginRow and commitPackedRow helpers preserve existing synchronized state transitions. Independent review approved this ownership and scope analysis without a blocker.

## Independent runtime provenance

- Both original nine-class families were independently compiled and matched the frozen actual HTTP baseline families **byte for byte**.
- `base-candidate-source` applies only the ordered-values delta to base5ac89b5; it has no typed serializer helpers.
- `stack-candidate-source` applies the same delta to qualified depth+serializer d94617b3. Existing writePackedRow/writePackedValue source is unchanged byte for byte.
- Both frozen candidate overlays contain exactly nine classes across QueryEvaluator, TupleResultBuilder and WorkbenchPageResultWriter, including their inner classes. The stack overlay also matches the final Maven-tested classes byte for byte.
- Exact source/class hashes: `candidate-source-hashes.json`, `candidate-manifests.json`; baseline proof: `baseline-family-comparison.json`.

No resource-holder, cancellation experiment or test-seam classes are introduced into these production families. Do not use arbitrary current target/classes as a benchmark overlay.

## Test-first and final results

| Execution | Result |
|---|---|
| Initial new baseline tests | 7 semantic passes; 1 intended typed-route structural red |
| Expanded final tests against frozen original baseline | 10 semantic passes; 2 intended route/hit reds; zero container failures |
| Corrected candidate focused selection | 124/124 passed |
| Final full Workbench JVM module run | **1,574 passed**, zero failures/errors/skips; 3m16s |
| Independent base + ordered overlay | **126/126 passed**, zero skipped/aborted/failed tests or containers |
| Qualified stack + ordered overlay | **128/128 passed**, zero skipped/aborted/failed tests or containers |

The two-case difference between isolated arms consists of typed-serializer structural checks, which intentionally do not apply to the independent base. Both arms run the same six serializer behavioral regressions, 12 ordered-values tests, existing 17 page writer tests, 20 evaluator tests and 71 batch cases. Results overlap and must not be summed as unique coverage. No frontend/browser or whole-repository pass is claimed.

New adapter tests cover getter-before-lexical ordering, source failure before output, callback mutation of original source values, caller name-array mutation, duplicate-column semantics, null-name fallback, zero-width and deferred-empty header transitions, mutable source/scratch reuse, subclass dispatch, legacy CSV/generic object conversion and actual positional-route hit proof. Existing regressions cover exact compact bytes, cap/batch/oversized behavior, failed serialization cleanup, heartbeat/progress interleaving, disconnect, breaker cancellation, paging and totals.

## Failures retained and corrected

Five existing evaluator tests initially caught inline Mockito instrumentation retaining the exact class. A boolean attempt with caller-side fallback preserves the old virtual result call; those tests pass unchanged. Two test-compilation attempts failed because StackWalker Boolean inference selected a BooleanSupplier assertion overload; a primitive local fixed the fixture. Those attempts executed no tests and are not counted.

First isolated application launches lacked the test-only spring-test JAR. JUnit discovery failed a container and ran zero tests, exposing an inherited launcher check that only inspected test failures. These logs are preserved and explicitly excluded. The final launcher fails on failed/aborted containers or tests and requires exactly 126 or 128 successful cases. The missing existing Spring test JAR was added only to safety classpaths, never production/timing layers. Final logs include actual class origins and exited zero after all expected cases ran.

## Performance handoff

CPU/build ownership was released after qualification. Benchmark only the frozen family for each corresponding arm. The O(width) alignment guard, packed allocations and JSON generator costs remain; structural removal does not establish a latency or throughput gain. Actual matched HTTP measurement is separate.
