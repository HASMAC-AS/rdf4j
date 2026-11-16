# Materializer follow-up: independent design review

Status: read-only source inspection and primary-source research. No Java process, build, test, or production edit was performed for this review. The earlier source, timing evidence, and delivery archives remain unchanged. This document proposes bounded experiments; it does not attribute the measured regression to a method or claim a speedup.

## Findings that constrain the experiment

The current DENSE/OFF comparison changes both producer provenance work and the consumer’s factor cache. These components should be separated before a storage-format conclusion is drawn. A carrier with caching disabled is a useful control, but it measures more than output copying.

The relevant source locations are:

| Location | Current work |
|---|---|
| [LmdbNativeKernelEmitter.java](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelEmitter.java), `emitRowSink`, line 2709; [LmdbNativeKernelInterpreter.java](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelInterpreter.java), terminal emission around line 756 | Both IR tiers call the same carrier’s `emit()` after terminal filters/slicing. Changing the carrier can preserve generated operator selection and the emitted ID loop. |
| [KernelResultCoordinates.java](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java), `emit`, line 537 | DENSE copies five words for each eligible output cell, then performs namespace/window and changed-slot bookkeeping. Other methods also maintain registers, saved frames, and source sidecars. |
| [LmdbNativeKernelExecution.java](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelExecution.java), `KernelRowCursor.next`, line 2491 | Each enabled row clears a 64-entry slot-to-output map and rebuilds it while binding IDs; rejected rows also participate in coordinate lifecycle. OFF has no such mapping array. |
| Same file, `coordinateValid`, `mayReplay`, `resultCoordinate`, lines 2577, 2584, and 2589 | `coordinateValid` checks buffered-coordinate availability. `mayReplay` currently repeats that whole check. `resultCoordinate` validates again before acquiring the coordinate. |
| [LmdbNativeRowStep.java](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java), `NativeRowsIteration.getNextElement`, around line 4217 | The iteration invalidates the materializer’s coordinate position before producer advance and publishes the accepted live position afterward. This is distinct from copying a batch’s coordinates. |
| [NativeResultMaterializer.java](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java), `invalidateCoordinates` and `position`, lines 181 and 196 | Publication maintains sequence/activation checks, drains namespace retirements, and advances the coordinate memo’s change stamps. |
| Same file, `find`, line 248; `rememberFactor`, line 522 | Ordinary last-value reuse is attempted first. Even when it succeeds, the result is published to its factor coordinate, potentially admitting a page for another accepted member carrying the same ID. |
| Same file, `captureCoordinate`, line 443; `FactorMemo.load`, line 816 | A valid memo entry still copies five words to shared scratch. The surrounding producer checks remain active on memo hits. |
| Same file, `memoPage`, line 567; `FactorMemo.remember`, line 827 | The remembered page key is checked again because replacing memo coordinates does not currently clear the old page pointer. |

For a `KernelRowCursor`, a successful coordinate-memo hit traverses **three** `coordinateValid`/`hasCoordinate` checks: the first explicit check, the check inside `mayReplay`, and the final explicit check. A memo miss adds the check inside `resultCoordinate` before acquisition. A memo hit also copies five coordinate words and checks the cached page’s full key. This is a source-level call count, not a CPU attribution: inlining and elimination may change the machine-level cost.

There is a useful scale distinction. At 1,152,000 output rows and 11 eligible columns, five-word emission represents 12,672,000 short copies and 506,880,000 bytes of copied payload. Clearing the 64-int cursor map represents another 294,912,000 bytes of stores. These are illustrative source-level write volumes, not Java allocation or measured DRAM traffic. The bounded arrays are reused, and cache locality/JIT behavior matter.

## Review gates for the proposed first optimizations

The proposed first step—reading coordinate memo entries without copying and maintaining a cached-page association—addresses repeated work while preserving the factor cache’s intended behavior. The separate `mayReplay` eligibility change should have its own comparison. A draft still needs an independent invariant audit; the following are the required conditions.

### Read memo coordinates without copying

A cache hit can select an existing materializer-owned backing array and base offset instead of copying five words into `coordinateScratch`. A miss can still acquire into scratch. All subsequent readers must consume the selected array/offset consistently: position-to-page calculation, page-key lookup, hashing, admission, namespace, and memo initialization. Leaving even one reader on the old scratch array risks mixing coordinates from different projected targets.

This representation must not allocate a new per-cell object or retain a producer-owned array. `FactorMemo.entries` is already owned by the materializer; keeping an internal view into it adds no producer or source lifetime. The scratch path must continue to work when factor-memo admission is refused. The row-sequence, source/view, activation, and before/after producer-validity checks remain unchanged.

### Maintain the page/key association

Removing `memoPage`’s repeated `page.matches(...)` is sound only if a non-null `pages[target]` is guaranteed to refer to the page for the **currently accepted memo coordinate** and that target’s canonical alias. The page key consists of canonical target, namespace, group, parent, tuple column, and the full-width member position shifted by `PAGE_SHIFT`.

The easiest safe invariant is conservative invalidation: clear the target’s page pointer whenever its stored coordinate changes, before exposing the replacement memo entry. A more selective key-only invalidation can retain the page across positions within the same page, but must compare every key component. That is an optimization of the invariant, not a prerequisite for establishing it.

Check these transitions explicitly:

- `changed(...)` can make an entry stale without clearing its pointer. No pointer may be trusted until a successful current-entry validation or a refreshed coordinate has re-established the association.
- A failed final producer-validity check must clear the target entry and page pointer.
- Scope changes, producer/activation changes, abandon/failure paths, and close must remove both coordinate views and page references.
- Namespace retirement must invalidate every affected target, including aliases pointing at the same page, before closing that page.
- Directory resizing moves table slots but keeps page objects; it must not accidentally invalidate or misassociate cached pointers.
- Dynamic duplicate targets can switch the winning source slot. Canonical projection aliases and equal numeric IDs do not establish coordinate equality.
- Full-width member positions and all namespace/key fields remain untruncated.

The ordinary value witnesses remain essential. A correctly associated page can still contain a cleared weak reference or a value whose semantic version/revision changed. The optimization must retain `matches(...)`, ID checks, weak dereference handling, and final result-view revalidation.

### Make `mayReplay` a conservative eligibility query

[The interface](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedRows.java), `NativeResultCoordinates`, line 51, explicitly permits `mayReplay` to include slots that do not replay. Its job is eligibility; it need not repeat buffered-cell availability.

For `KernelRowCursor`, cheap bounds/owner/map eligibility can replace its delegated `coordinateValid` call, provided both live checks in `captureCoordinate` and the validity check in `resultCoordinate` remain. Review every caller, including `BoundCursor.captureCoordinates`, to ensure nobody treats `mayReplay` alone as a certificate. Preserve conservative behavior on missing/closed owners. This changes a redundant eligibility probe, not the source generation or mutation witness.

## Minimal layered causal controls

The optimization comparison and the mechanism experiment answer different questions. Do not automatically run their entire cross product. First validate the chosen candidate and compare it with the legacy materializer in the same newly frozen artifact. If the remaining penalty needs attribution, use the smallest appropriate controls below.

| Control | Producer coordinate work | Consumer coordinate lifecycle | Factor lookup/population | What its comparison can establish |
|---|---|---|---|---|
| Existing OFF | Disabled; documented scaffolding remains | Disabled | Disabled; ordinary weak last-value reuse remains | Ordinary-reuse reference. |
| Existing DENSE/FULL | Unchanged | Unchanged | Unchanged | Full current mechanism. |
| DENSE/TRANSPORT-ONLY | Unchanged, including emission, maps, namespaces, frames, and sidecars | Disabled | Disabled; ordinary reuse remains | Versus OFF: cost of the admitted producer provenance path, **not just five-word output copies**. Versus FULL: net consumer-cache effect, including saved resolutions. |
| DENSE/NO-HIT-PUBLICATION | Unchanged | Unchanged | Keep factor lookup and ordinary-miss population; skip only `rememberFactor` reached from a successful ordinary last-value hit | Whether that publication policy earns its cost. A change in later factor misses/resolver calls is part of the policy tradeoff. |

These two added modes are a compact starting point. They should freeze a configuration once per JVM or execution, rather than call `System.getProperty` per row. Use one source tree and one frozen classpath for all arms; record exact properties, runtime hashes, generated route/tier frequencies, root-host totals, and results. Existing ownership, source binding, checksums, cancellation, close, and non-reclaiming admission rules stay intact. No strategy is forced.

If `position`/`invalidateCoordinates` itself is a concrete remaining question, add **DENSE/LIFECYCLE-ONLY**: call the existing lifecycle methods but suppress factor acquisition, lookup, and publication. Comparing this with TRANSPORT-ONLY holds ordinary resolution behavior fixed and estimates lifecycle overhead. It does not include the full cache’s `FactorMemo.changed` work when no memo exists; report that boundary.

If page population needs its own test, **DENSE/WRITE-ONLY** can keep normal publication while bypassing factor-cache returns. Against LIFECYCLE-ONLY it measures the write/admission/coordinate-validation path under ordinary reuse. It is not a perfect estimate of FULL’s population cost: FULL’s factor hits skip some resolver and `remember` calls. Record those operation counts; do not assume additive costs.

Suppressing publication on an ordinary hit is semantically safe because the ordinary hit was independently validated. It may lose future reuse. A concrete sequence is `A@p0, A@p1, B@p2, A@p1`: FULL can publish the second occurrence into `p1` and reuse it later; the ablation may resolve the final A again. Existing FULL assertions about this benefit must remain intact.

### A literal output-copy control requires an outward-only boundary

Globally removing DENSE’s `System.arraycopy` is not an isolated copy experiment. [BoundCursor.fillCoordinates](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelBindings.java), line 1565, can consume a nested `KernelRowCursor`’s buffered coordinates and export them into an outer sidecar. Losing those copies changes the outer producer’s provenance work as well.

A defensible copy-off control must therefore be limited to a sink whose only coordinate consumer is the deliberately disabled outward materializer. Keep the DENSE rows allocation/reservation, register updates, namespace pins, changed-slot bookkeeping, row counts, and release order identical; omit only the payload copies. Do not call `disable()` or return a null carrier, because those also remove other work. Inner/exported carriers remain normal DENSE.

An explicit consumer-kind marker would be safer than inferring this from kernel nesting or the fact that a benchmark currently opens one owner. `NativeRowsIteration.initializeWithinScope` establishes outward demand, while `BoundPlan.openRows` establishes export demand on its forked row. Any unexpected coordinate read from a discard-only sink must make the diagnostic run invalid, rather than silently affect upstream work. Check compiled output to ensure the intended copy loop actually differs. Until this boundary exists, use the honest label **producer transport cost**.

## Correctness and participation checks

Keep the existing assertions and run the chosen focused selection in both IR tiers before timing. The most relevant existing [NativeResultMaterializerTest](../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializerTest.java) cases cover accepted replay-hit publication, long positions, positive namespaces with identical coordinates, retirement of one namespace while another remains live, changed-source masks, skipped intermediate changes, weak misses, mutation witnesses, scope changes, failure rollback, and refusal without reclaiming shared memory.

Add a small targeted regression for each newly introduced invariant:

1. Alternate targets and page keys after a successful memo hit so a stale scratch array or stale page pointer cannot pass; include a page-boundary crossing, changed namespace, and equal IDs from distinct coordinates.
2. Revisit the same coordinate after an ordinary-hit publication; the default FULL behavior must retain its existing reuse assertion. Diagnostic modes must prove semantic equality and their intended difference in producer/cache calls.
3. Refuse the memo reservation while admitting an ordinary/page cache where possible. No-copy access must fall back to the scratch representation without uncharged retained state.
4. Change source activation or retire a namespace between sampling checks. Keep the failed-capture/abandon path and detached retained rows correct after producer close.
5. For conservative `mayReplay`, prove the two live checks still reject an unavailable/currently unbound coordinate; include direct `resultCoordinate` callers.
6. If a copy-off control is built, prove nested exported coordinates remain byte-identical and only the outward discard-only sink skips copies. Cover early close, rejected rows, optional null arms, and a full output-window rollover.

Performance telemetry should distinguish ordinary hits, factor hits, weak misses, coordinate memo hits/misses, coordinate acquisitions, page admissions/refusals, and publication attempts/actual writes. Prefer instance-local counters published once at close; avoid global atomics per cell and per-call clocks. Keep instrumentation identical across timed arms, or use an explicitly separate diagnostic run. Counter equality is especially useful when claiming a candidate preserved cache effectiveness.

## Primary implementation evidence

- [Velox expression evaluation](https://facebookincubator.github.io/velox/develop/expression-evaluation.html) distinguishes peeling shared encodings from memoizing results across batches. It also documents a fast path for cheap expressions where encoding/null metadata overhead is visible. **Inference here:** representation and memoization merit separate controls; this does not predict an RDF4J speedup.
- [Velox configuration definitions](https://github.com/facebookincubator/velox/blob/main/velox/docs/configs.rst) expose separate debug switches for expression peeling and memoization. This is a concrete precedent for distinguishing the mechanisms while retaining the same engine build; it is not a recommendation to change AUTO selection.
- [Velox Expr.h](https://github.com/facebookincubator/velox/blob/main/velox/expression/Expr.h) separates dictionary identity state, cached values, and valid dictionary indices; it also distinguishes weak/raw base identity from a later strong base reference. **Boundary:** RDF4J’s mutable values, source generations, and weak result contract differ. Do not import the strong-retention policy or remove mutation witnesses.

The saved DuckDB/Velox vector-format research remains applicable, but it does not establish that smaller coordinates alone resolve this regression. The next decision should follow the matched candidate comparison and the specific mechanism it actually changes.

## Independent audit of scalar draft 01

Reviewed the complete proposed files and patches under [materializer-no-copy-draft-01](../followup-performance/materializer-no-copy-draft-01/README.md). The [draft manifest](../followup-performance/materializer-no-copy-draft-01/draft-manifest.json) hashes were independently checked, including equality between each saved preimage and the still-unmodified production file at review time:

| Draft | Proposed source SHA-256 |
|---|---|
| Candidate 1, materializer | `aea9ec4452ef1303ca99433fa939a402682e4c2736f867790e7a63a2df0f7513` |
| Candidate 2, kernel eligibility | `54f109c010b3efecdb65a2b60f98f1eec0239d9eacd7e07646a81745e08e46c9` |

**Static verdict: no blocking correctness defect found.** This does not establish compilation, test success, or a speedup. The parent’s pre-change characterization/build and subsequent matched verification remain the gates for applying the drafts.

Candidate 1’s negative sentinels are distinct from valid memo offsets. Every downstream position/key read selects either `coordinateScratch` at offset zero or the admitted memo’s entries at the returned offset. The page-directory lookup and admission methods receive that array/offset explicitly. The only remaining direct scratch reads are on fresh producer acquisition; newly allocating a memo also copies from that fresh scratch path. No per-row wrapper object is introduced.

The page association is maintained on fresh capture. `FactorMemo.remember` checks canonical target, namespace, group, parent, tuple column, and full-width page index against the candidate coordinate, and clears a mismatched pointer. An unchanged-stamp hit can therefore reuse the pointer. A stale stamp forces fresh capture first. Failed capture invalidates the entry; namespace retirement checks both the memo coordinate and the pointed page; whole-cache teardown removes the memo. Retaining the pointer across positions within the same page is correct because the current position still chooses the cell and its ID/witness remains checked.

The simplified publication guard is supported by an exhaustive review of non-null reference assignments in the proposed class. `remember` constructs a new `WeakReference` whenever it installs a witness tuple. `findFactor` copies the whole page tuple and its reference into ordinary reuse. `rememberFactor` copies that tuple/reference into a page. Clear/close paths only remove references. Therefore equality of the exact non-null reference object proves that the already-stored ID/version/revision-owner tuple is the same tuple; it does not claim that the referent is immutable or still live. The existing `matches(...)` and weak-miss checks are preserved.

Candidate 1 includes three related reductions—memo-hit copying, repeated page-key checks, and repeated tuple-field comparisons. A latency change should be attributed to that combined materializer rewrite unless a further control distinguishes them; the name “no-copy” is not a proof that copying alone caused the result.

The no-memo case retains its directory/page lookup path. The existing `factorPagePayloadAdmissionDoesNotReclaimSharedQueryMemory` test is a particularly useful guard: its 17,300-byte limit admits the 17,248-byte ordinary cache/directory/first-page combination, refuses the memo and a second page, and still expects reuse of the first page without reclamation. Keep this exact assertion path in the matched selection. The new mutation characterization separately exercises rejection and republishing of a stale factor-page witness, followed by a memo hit.

Candidate 2 keeps closed/unpositioned/disabled-owner, range, and slot-mapping checks but removes the repeated `hasCoordinate` scan. Repository-wide call-site inspection found two non-forwarding consumers: `NativeResultMaterializer.captureCoordinate` and `BoundCursor.captureCoordinates`. Both independently check live coordinate validity, and acquisition retains its own check. Other call sites forward the predicate through cursor wrappers. This supports conservative eligibility under the documented interface, without changing its source authority.

For a production-candidate comparison, exact frozen source/binary identities will necessarily distinguish the approved rewrite from the baseline. Preserve a reviewable diff limited to these changes and identical ownership behavior. The earlier recommendation to use one build for all modes applies to diagnostic ablation switches; it does not justify relabelling a changed candidate artifact as the old source identity or pooling it with historical timings.
