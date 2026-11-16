# LMDB ownership and transport validation

## Status and scope

The ownership repairs have passing focused reproductions and a completed clean **484-class LMDB broad selection at 4 GiB: 6,481 cases, 6,469 passed, zero failures or errors, and 12 audited skips**. Its exact source identity and evidence are appended below. Earlier targeted and broad runs remain preserved, including the 185-class selection and its fixture follow-up, two failed 4 GiB snapshots, and the incomplete reused-fork 2 GiB run. The separate telemetry census remains incomplete. This is therefore **not a claim that every repository test passed or that the skipped contracts were exercised**.

This report audits preserved logs, reports, source, and command manifests; it runs no additional builds or tests. Evidence paths below are relative to this directory. The source identity for the 185-class selection is recorded in [final-validation/source-identity.txt](final-validation/source-identity.txt), with source hashes alongside it. The final fixture repair is recorded separately.

An earlier normal-heap broad snapshot completed all **484 selected classes** at a **4 GiB maximum heap**, with **6,481 cases, zero failures, two fixture errors, and 12 skips**. Its exact result and skip limits remain below; that two-error run is not recorded as a pass, and the final clean selection does not replace the earlier OOM history.

## Production repairs

| Area | Correction and preserved contract | Evidence |
|---|---|---|
| Query scalars and child closure | The query owner retains scalar representatives without capturing the first child's source or resolver. A live resolver associates certified stored IDs later. Uncached matching uses identity to avoid resolving unrelated cold values. Publication is checked against closure. | `scalar-close-race/`; final `NativeValueResolutionContractTest`: **51/0/0**; `NativeWorkerConditionFrameLifetimeTest`: **1/0/0**. |
| Explicit invocation and preparation | Semantic EXISTS, prepared subqueries, worker scratch rows, groups, and wrappers bind to their actual source/session/execution owner. Projection and multiprojection forward their invocation carrier so outer ORDER/TOPK consumers retain preparation after inner exhaustion. Private caller binding names survive compiled-wrapper binding. | `order-carrier/`, `filter-scope/`; final generic-bridge, wrapper, group, and worker-scope reports. |
| Detached results and entry authority | Both native fragment emission paths produce detached input snapshots. Entry serialization preserves values and GRAPH provenance while discarding native-ID authority. An immutable detached generation can certify entry reuse without live scope tokens. VALUES and compiled input recipes preserve their ID-domain and source-authority requirements. | Final row-detachment, entry-seeding, ID-authority, VALUES-ownership, and wrapper reports; `accumulate-detachment/`. |
| Literal witnesses and EXISTS fallback | Exact native literals receive mutation-sensitive witnesses without cold lexical resolution. Unsupported native entry witnesses use semantic evaluation of the current input rather than becoming a false EXISTS result. Direct, borrowed, and worker paths preserve bag semantics, short-circuiting, and cleanup. | `compound-exists/`, `filter-scope/`; final literal-witness, input-fallback, and filter-scope reports. |
| Deferred constants | Generic value preparation occurs when a row reaches the evaluator. Unopened wrappers and LIMIT 0 preserve cold and cyclic constants. | Final `NativeWrapperConstantCaptureTest`: **6/0/0**. |
| Explanation | Optimized generic-root preview prepares interior structural plans within the existing compilation scope, restoring child native annotations without opening an iteration or retaining an execution. Ordinary preparation stays lazy. | `lateral-route/`; the identical parent test passed, checkpoint test failed, and repaired test passed. Ownership04 LateralDifferential: **18/0/0**. |
| Shared recipe accounting | Capture scratch is admitted before allocation; active queries share recipe/catalog charges by immutable identity and release them with preparation lifetimes. Range-name metadata and compiled diagnostic descriptions are bounded. | Final `NativeRecipeMemoryTest`: **9/0/0**; [recipe-range-regression/README.md](recipe-range-regression/README.md), [recipe-description-review/README.md](recipe-description-review/README.md). |

### Recipe memory boundary

`NativeRecipeMemory` charges temporary compilation scratch and charges an admitted immutable recipe once per query, irrespective of the number of borrowing child invocations. Frozen catalog metadata is charged independently, after sibling compilation has finished. The last preparation releases the shared charge; refusal, delegate failure, registration failure, and execution closure release partial claims. The nine final tests cover these lifetimes, re-admission, large VALUES/range refusal, and bounded descriptions.

**Idle prepared-query artifacts remain outside the query-memory manager's lifetime**, like the original algebra. The tests explicitly expect zero query-ledger usage after capture while an idle recipe still exists. Footprints are conservative estimates, not measured retained heap sizes. This work does not supply a global budget for an arbitrary collection of idle prepared queries. Ledger peaks must therefore not be presented as total JVM retained memory.

The pre-admission claim applies to **size-dependent descriptor capture**. `NativeRecipeMemory.Capture` constructs small fixed bootstrap containers before requesting its initial 512-byte scratch reservation; this is not a guarantee of literally zero Java allocation before an admission refusal. Recipe/catalog sharing is by immutable identity within one query owner, not content-based deduplication across independent queries. [Recipe-accounting source](../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeRecipeMemory.java).

## Fixture migrations versus production changes

Many failures came from direct operator fixtures that still supplied null sources, omitted fused sessions/preparation owners, used strategy mocks that could not bind invocations, or failed to certify their dictionary origins and generations. These fixtures were migrated to explicit owners and truthful source capabilities. Negative authority tests retain their deliberate refusal behavior. Strategy, result, memory, cancellation, and cleanup assertions were preserved.

Two examples make the distinction concrete:

- The accumulation test observed ownership through a source-backed row. After fragment detachment, it now observes the **live iteration carrier**, keeps the original authority/budget assertions, and additionally checks that projection input is detached and readable after producer closure.
- `NativeExpressionDomainFailureTest` reached a semantic callback without an invocation. Its repaired fixture owns that invocation and closes decoded inputs first. Exact deadline exception identity, callback counts, rejected value publication, accounted bytes, and cancellation assertions are unchanged. No production change was needed for this final failure.

Earlier broad runs also exposed fixture restoration failures that leaked memory overrides or forced strategies into subsequent classes. Those contaminated downstream failures were not treated as independent production regressions. Their preserved logs remain diagnostic history.

## Verification record

| Run | Recorded result | Interpretation / evidence |
|---|---|---|
| Untouched checkpoint reproduction | **282 tests; 10 failures; 55 errors** | [initial/tests.log](initial/tests.log). Expanded selection, not the user's earlier 163-case selection. |
| Ownership03 evaluation/codegen | **4,539 tests; 112 failures; 433 errors** | A fixture leaked forced parallel aggregation into later classes. This result is contaminated; see [ownership-03-evaluation/tests.log](ownership-03-evaluation/tests.log). |
| Ownership04 root build | **BUILD SUCCESS** | [ownership-04/root-build.log](ownership-04/root-build.log). Quick build is not a full repository test run. |
| Ownership04 broad tests | **400 completed classes; 5,318 tests; 5 failures; 32 errors; 12 skips**, then heap OOM | **Incomplete**. One reused fork, 2 GiB heap; stopped during `LmdbNativeZeroEstimateHashQuoteTest`. See [ownership-04/tests.log](ownership-04/tests.log), command/source manifests, and `ownership-04/surefire-dumps/`. |
| Standalone OOM-class investigation | **5/5 passed at the same 2 GiB heap limit** | Copied ownership04 bytes. No OOM dump produced. This does **not** identify the reused-fork OOM cause or establish absence of leaks. [zero-estimate-oom-probe-04/summary.json](zero-estimate-oom-probe-04/summary.json). |
| Fresh final batch 1 | **62 classes; 1,052/1,052 passed** | `final-validation/batch-1/`. |
| Fresh final batch 2 | **62 classes; 944/944 passed** | `final-validation/batch-2/`. |
| Fresh final batch 3 | **61 classes; 709/710 passed; one failure; zero errors/skips** | The one failure is the semantic-callback fixture described above. `final-validation/batch-3/`. |
| Final fixture repair, isolated | **5/5 passed** | Exact fresh gate classpath; outputs outside Maven targets. [expression-domain-invocation/after.log](expression-domain-invocation/after.log). |
| Final fixture repair, official | **10/10 passed** | Rebuilt module; five expression-domain cases and five benchmark-fixture cases. [final-validation/final-fixture/tests.log](final-validation/final-fixture/tests.log). |

The fresh 185-class selection includes all **67 classes not completed by ownership04**, together with changed or relevant regression suites. Its aggregate is recorded in [final-validation/batch-results.json](final-validation/batch-results.json). The runs overlap; their test counts must not be added as a unique-test total. Successful formatting, copyright, and module-build command records are preserved under `final-validation/`; the last fixture change has its own subsequent build record.

### Both IR tiers and transport representations

Additional official gates used the same production implementation with these transport properties:

| Transport | Cases | Failures / errors / skips |
|---|---:|---:|
| `off` | 84 | 0 / 0 / 0 |
| `changes` | 90 | 0 / 0 / 0 |
| `selection` | 90 | 0 / 0 / 0 |
| `rows` | 90 | 0 / 0 / 0 |

Evidence: [final-validation/modes/results.json](final-validation/modes/results.json) and each mode's command and XML reports. The factor-coordinate cases explicitly exercise **compiled and interpreted IR**, aliased UNION/nested OPTIONAL bags, retained rows after early close/cancellation, and cancellation/close races. Enabled compact modes also run both compiled and interpreted optional-fanout reuse tests and the targeted scan-backed hash-buffer case. The `off` selection deliberately omits transport-specific reuse expectations and those additional cases; its 84 cases are not identical to each enabled mode's 90.

DENSE remains the default. CHANGES, per-cell SELECTION, and shared-source-row ROWS are optional representations of the bounded output window. ROWS can withdraw coordinates when exact grouping/column invariants fail; ordinary reuse remains available. Upstream registers, saved frames, and producer sidecars still use five-word coordinates. These gates establish correctness, admission, cleanup, and actual tier coverage; they do not establish a latency or allocation advantage.

### Transport admission and lifetime qualifications

Withdrawing optional transport makes its coordinates unavailable, but the carrier retains its already admitted primitive buffers and reservations until its owner closes. Disabled namespace draining can retire materializer factor pages separately; it does not release the carrier's own storage. Compact buffers retain their admitted high-water capacities across windows, and resizing holds old and replacement reservations together. Logical compressed payload size therefore does not by itself determine peak admission cost or the memory held after fallback. [Carrier source](../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java).

The public initial/growth-refusal counters describe the instrumented carrier/record/selection admissions, not every optional decline. Sidecar allocation through `newWords()` and namespace-directory growth can return refusal without incrementing those counters. Initial eligibility checks also return no carrier for unsupported shapes, including more than 64 **register columns**, without recording a budget refusal; this is not a 64-output-alias limit. Accordingly, zero refusal counters mean no recorded refusal in those instrumented paths, not proof that every optional allocation or producer participated. Actual owner/window/row observations and route counters remain necessary, with their documented coverage limits. [Carrier admission and diagnostics](../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java); [design report](transport-design-report.md).

## Remaining limitations

- The reused-fork ownership04 OOM cause remains unresolved. The same class passing alone is evidence against an unconditional per-class failure, not proof of complete lifetime correctness.
- The separate **1,014-query telemetry census was not completed**. The ownership03 broad run was deliberately stopped after 18 preceding class reports, with no hang/failure conclusion; ownership04 explicitly excluded that census. See [ownership-03-broad/INTERRUPTED.md](ownership-03-broad/INTERRUPTED.md).
- The final 484-class LMDB selection is clean for its enabled cases. It is not a repository-wide run and excludes the audited disabled/opt-in cases and incomplete telemetry census. Its counts overlap the earlier targeted and broad selections.
- Query-ledger accounting excludes idle prepared artifacts and does not replace JVM allocation/heap measurement. Performance conclusions belong to the separately matched ablation experiments, with checksums and actual routes reported alongside timing.

## Later normal-heap broad gate

The subsequent broad selection ran in one Surefire fork with `rdf4j.lmdb.test.maxHeap=4G` and alphabetical class order. It completed all **484 selected class reports**, recording **6,481 cases: 6,467 passed, zero failures, two errors, and 12 skips**. The command exited with status **1** because of the errors. Its source label is `15fe7d5011+worktree-sha256-34767935d60f32ba61adfb44495c607e17ca760d5d2f77a17ddb2862c4f7a8ac`, and the command record reports 2,057 source files verified before execution. The completed [results.json](final-validation/normal-heap-broad/results.json), [command manifest](final-validation/normal-heap-broad/command.json), and [test log](final-validation/normal-heap-broad/tests.log) are the authority for these totals.

Both errors occur in `LmdbNodePredicateRouteTest`, whose 12 cases otherwise include ten passes:

| Case | Recorded error | Interpretation |
| --- | --- | --- |
| `routedCorrelatedPayloadStopsWhenRunDecodingRequestsCancellation` | `IllegalStateException: native row has no fused invocation` | The direct cancellation fixture reaches correlated payload construction without its required fused invocation. |
| `routedPayloadStopsWhenRunDecodingRequestsCancellation` | `IllegalStateException: native row has no fused invocation` | The direct cancellation fixture reaches payload-cursor construction without its required fused invocation. |

These are fixture initialization errors before the intended cancellation assertions. A test-only ownership migration is being handled separately; no passing follow-up is claimed here until its actual result is recorded. The two errors remain part of this broad run's result. [Preserved class XML](final-validation/normal-heap-broad/surefire-reports/TEST-org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNodePredicateRouteTest.xml).

### Exact skip coverage

The completed result contains the same 12 skips classified in the [skip audit](final-validation/normal-heap-broad/skips-audit.md). That audit was written while the gate was running and classifies skips only; the completed result above supplies the final totals.

| Skip category | Cases | Unexercised coverage |
| --- | ---: | --- |
| Unconditional, reasonless `@Disabled` methods in `LmdbNativeStrategyPriorityTest` | 10 | Existing strategy-priority, cost/proposal, placement, and telemetry assertions. These are not passing cases or environment-dependent assumption failures. |
| `LmdbNativeSelectorCoverageMatrixTest#compareEveryControlledShapeWithTheGenericOracle` | 1 | Opt-in selector matrix; `rdf4j.lmdb.selector.matrix.enabled` was absent. |
| `LmdbNativeStrategyRegretHarnessTest#measureEveryAdmittedStrategy` | 1 | Opt-in strategy-regret measurements; `rdf4j.lmdb.regret.enabled` was absent. |

The audit verified that all three affected source files are unchanged relative to checkpoint `15fe7d5011d27d31d76436238363ee3f34b3ba50`; these skip gates were not introduced by the ownership or transport changes. No reason for the ten unconditional disables is inferred beyond their annotations.

This completed **LMDB selection** is broader evidence than the earlier targeted batches, but it is not a repository-wide pass, does not include the unfinished 1,014-query telemetry census, and does not exercise the disabled/opt-in contracts above. Completion at 4 GiB without a recorded OOM does not establish why the earlier reused fork exhausted 2 GiB, nor prove absence of a retained-memory issue. The broad gate's heap also differs from the matched 1 GiB timing protocol. Its counts overlap prior runs and must not be added to them as a unique-test total. The two-error observation, subsequent fixture repair, and any follow-up verification must retain separate records.

## Repeated normal-heap broad gate

The rebuilt final-source gate completed the same **484 classes / 6,481 cases**
in one reused 4 GiB fork: **6,468 passed, one failure, zero errors, 12 skips**.
All selected classes produced fresh reports, and all source hashes were stable
throughout the run. Both corrected NodePredicate cancellation cases passed.
The sole failure is the unchanged
`LmdbNativeGenericRootHostTest.valuesOnlyQueryOnEmptyStoreStaysNative`: rows were
correct and a native compilation was counted, but the hosted-generic count rose
by one instead of zero. A native interior VALUES child can account for the native
compilation count after a generic root decline, so these assertions do not prove
that the root itself stayed native. The preceding normal-heap run passed this
class with identical production bytecode.

The targeted diagnostic trace confirmed the transient cause: the startup
term-index **CLEANUP writer** temporarily makes the detached dictionary lookup
generation unavailable. The native authority guard therefore correctly declines
the root while an interior VALUES child can still compile natively. The trace
records a null captured generation during `runTermIndexMaintenance`, rather than
an incorrect result or a weakened ownership guard. [Preserved failure source](final-validation/generic-root-diagnostic/LmdbNativeGenericRootHostTest.benchmark.java);
[scope trace](final-validation/generic-root-diagnostic/scope-repeat.log);
[diagnostic overlay](final-validation/generic-root-diagnostic/scope-src-diagnostic.patch).

The fixture now waits for the existing **PostReady initialization barrier**, then
the **index-completion barrier**, before making its steady-store route assertions.
Existing assertions and production code are unchanged. With that fixture change,
**50 full-class repetitions passed 1,150/1,150 cases** against the uninstrumented
installed production targets; diagnostic production overlays were absent from
that rerun. [Fixture-only patch](final-validation/generic-root-diagnostic/fixture-fix.patch);
[repetition command](final-validation/generic-root-diagnostic/fixture-repeat-command.json);
[repetition log](final-validation/generic-root-diagnostic/fixture-repeat.log);
[diagnostic and rerun summary](final-validation/generic-root-diagnostic/diagnostic-results.json).

The formatted official build, 23-case follow-up, and third broad gate are
recorded separately below.
This repeated broad run retains its one-failure result and is not converted into
a clean broad-suite pass by the targeted repetitions.

[Repeated command](final-validation/normal-heap-final/command.json);
[complete repeated results](final-validation/normal-heap-final/results.json);
[production byte equality](final-validation/delivered-bytecode-comparison.json).

## Final normal-heap broad gate: completed clean selection

After the startup-readiness fixture correction, the formatter-enabled official
module build completed successfully, followed by **23/23 GenericRootHost cases
with zero failures, errors, or skips**. This official result follows the separate
1,150-case repetition experiment; their overlapping counts are not added as
unique coverage. [Build command](final-validation/generic-root-diagnostic/build-command.json);
[build log](final-validation/generic-root-diagnostic/build.log);
[official test command and counts](final-validation/generic-root-diagnostic/official-test-command.json);
[official test log](final-validation/generic-root-diagnostic/official-tests.log).

The final broad gate then completed all **484 selected classes / 6,481 cases**
in one reused **4 GiB** Surefire fork, in alphabetical order: **6,469 passed,
zero failures, zero errors, and 12 skips**, with process exit **0**. There were
no missing or unexpected class reports and no source changes during the run.
Both final fixture families passed in this complete selection. [Final results](final-validation/normal-heap-verified/results.json);
[exact command](final-validation/normal-heap-verified/command.json);
[test log](final-validation/normal-heap-verified/tests.log).

The delivered source identity is
`15fe7d5011+worktree-sha256-8c59454811c9927d79739c1780fa88238c4d613bc0c111cf8d4cde8feeb49059`.
The 2,057-file source manifest differs from the frozen benchmark source only in
the NodePredicate invocation fixture and GenericRootHost startup-readiness
fixture. Production sources/resources and rebuilt production bytecode are
identical to the frozen measured implementation; the original benchmark drivers
and frozen runtime metadata also remain unchanged. [Source provenance](final-validation/normal-heap-verified/delivered-source-provenance.json);
[delivered identity](final-validation/normal-heap-verified/delivered-source-identity.txt);
[production byte comparison](final-validation/normal-heap-verified/delivered-bytecode-comparison.json).

The same **12 skips** remain: ten unconditional strategy-priority disables and
the two opt-in selector-matrix/regret harnesses classified in the [skip audit](final-validation/normal-heap-broad/skips-audit.md).
They are not counted as passes. This clean selected LMDB run does not complete
the separate 1,014-query telemetry census or prove repository-wide coverage.
It also does not identify the cause of the earlier reused-fork 2 GiB OOM;
successful completion at 4 GiB is a distinct observation. All earlier failed,
contaminated, and incomplete records above remain part of the verification history.
