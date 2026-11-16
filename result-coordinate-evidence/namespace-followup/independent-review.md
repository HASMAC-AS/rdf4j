# Independent review of the namespace follow-up

Review date: 2026-11-15. Reviewer: `audit_ownership`, independently of the production-change author and benchmark driver author.

This review covers the new characterization tests, the exact frame-directory guard, its matching correctness evidence, the prospective three-arm timing protocol, and the separate page-occupancy diagnostic. It does not claim a performance result. No build, test, benchmark, or Java process was launched by this reviewer. The reviewer added the authorized characterization tests and ran the repository copyright checker before the coordinator ran the matching gates.

All paths below are relative to `result-coordinate-evidence/namespace-followup/` unless identified as repository paths.

## Matching correctness evidence

The reviewer independently parsed the preserved Surefire XML in `validation/original-frame-characterization/` and `validation/frame-directory-guard/`. Both contain eight reports and exactly the same 103 testcase identities, with no failures, errors, or skips.

| Test class | Original | Guard candidate |
|---|---:|---:|
| `LmdbNativeIrOptionalFanoutCoordinateTest` | 2 | 2 |
| `LmdbNativeIrFactorCoordinateTransportTest` | 10 | 10 |
| `LmdbNativePackedFtreeTest` | 1 | 1 |
| `LmdbNativeProjectionValueReuseTest` | 12 | 12 |
| `NativeResultMaterializerTest` | 37 | 37 |
| `KernelResultCoordinateRepresentationsTest` | 7 | 7 |
| `KernelResultCoordinatesTest` | 19 | 19 |
| `KernelResultRowSelectionsTest` | 15 | 15 |
| **Total** | **103** | **103** |

The two `sources-after.json` maps have no added or removed paths. Their only changed path is the repository file `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java`. The test sources are identical across these gates. This is direct evidence for a matching selection, rather than an inference from equal total counts.

The two IR integration classes explicitly exercise both compiled and interpreted routes. Their source assertions verify route counters, exact result bags, reuse, captured rows after early close and cancellation, and concurrent cancellation/close ledger cleanup. These tests deliberately select tiers for correctness coverage. They are distinct from the timing experiment, which uses normal AUTO selection. The new 103-case gate is a focused candidate gate; earlier broader validation belongs to its separately preserved source checkpoint and must not be relabeled as a full-suite run of this new guard.

### Characterizations and hit proof

Nine new cases remain in `KernelResultCoordinatesTest` across six methods. Four cases initially characterized repeated managed namespaces: exact pin multiplicity through sentinel and unbound cells, invalid tokens encountered after repeated valid tokens, and collision deletion/directory growth between separate calls with frame pins intact.

Five subsequent cases characterize the directory-null boundary:

- Unmanaged frames preserve overwrite, aliases, partial restore, unbound cells, repeated release, and inactive-frame behavior without a namespace-directory admission.
- Three parameterized cases pass an unknown managed token through `set`, `retainBatch`, and `releaseBatch` before a directory exists. Each must disable transport, and subsequent frame operations cannot expose coordinates.
- A frame saved before the first managed namespace correctly retains a later managed origin, allows retirement exactly once after its last frame/register/window reference, and remains reusable for unmanaged coordinates afterward.

The tests call `save` and `releaseFrame` directly on both sides of the directory-allocation boundary. They retain the existing assertions; no failing assertion was weakened. The initial test preimage is in `baseline/preimages/`, and the additional preimage before the five frame-boundary cases is in `baseline/frame-guard-test-preimage/` under the full repository test path. The latter preimage SHA-256 is `125e6fa7668627ee9e740eb7abaf269f8458b7ce1b8788d362b8ce8ce6f656c6`.

## Exact production change

The reviewed delta against `baseline/preimages/` adds three `namespaceIds != null` guards and an explanatory comment. It guards only the old-frame release and new-frame retain in `save`, and the range release in `releaseFrame`. The reference loops, external batch validation, register writers, namespace directory implementation, admission accounting, transport representation, and default mode are unchanged.

Reviewed source SHA-256:

`61797cdbfe196ac2629b79555d5bd2044e283a21911e94fbfc836059f9e25028`

Reviewed production patch SHA-256, using the candidate freezer's exact `a/<repository path>` / `b/<repository path>` unified-diff format:

`0ceed5b1f28931ae334671960f8f875e94d61883ab4e4ed1da29a3f3db70ac0e`

The narrow safety argument is supported by the actual field and writer paths:

1. Enabled private registers obtain their coordinates through `set`, `copy`, `restore`, and `clear`. A valid managed token requires a successful `newNamespace`, which allocates the directory first.
2. An unknown managed token passed to `set` reaches `namespaceIndex` and disables the owner. A subsequent enabled frame save cannot publish that token. External source batches remain on the original validation path and are covered by the new fail-closed cases.
3. `namespaceIds` stays nonnull after its first allocation, including after all namespaces retire. It is nulled only during owner close, after the owner is closed. Thus all later mixed or managed frames conservatively keep the original scans.
4. `save` preserves the original `multiplyExact` and `activeFrames[frame]` access order. `releaseFrame` preserves its enabled and index checks. Both methods preserve active-frame updates and all coordinate copying.

The original v2 diagnostic independently establishes that this optimization has work to remove in its chosen optional-fanout target: every observed frame range entered with a null directory. Each compiled query visited 17,237,000 frame cells in each direction, or 34,474,000 total frame-cell visits. It observed zero managed cells/directory lookups. Consequently, a repeated-token directory-lookup memo had no opportunity in this target and was not implemented. These counts establish path participation and potential scan elimination; they are not latency estimates and do not describe all query shapes. Evidence: `diagnostic-original-v2/diagnostic-summary.json` and its raw JSONL.

## Prospective timing protocol review

The reviewed frozen protocol is `frame-directory-guard/protocol.json`, SHA-256 `bbad71c3c879b49022ff6cb22d229ee94353961d1f512defdec722fe2a368f54`. The reviewed driver is `frame-directory-guard/run-comparison.py`, SHA-256 `e941e8c2a2fbfe86a564b1aa32a5ee4058ecb974010298757843aa1ce748455e`. The candidate freezer SHA-256 is `b1aa500db027c40d0a06b365f00c303f8335375fe57554ef217a1e6e1e76f7f7`.

The protocol compares original OFF, original DENSE, and candidate DENSE using all six permutations, one fresh JVM for each arm per round, eight warmups and all five measured queries per JVM. That is 18 JVMs, 144 warmup queries, and 90 measured queries. Each estimate uses the median of all five measurements within a JVM, then paired ratios within a round, then the median and full range of all six ratios. It does not treat individual queries within a JVM as independent replicates, drop an unfavorable tail, or replace failed forks.

The runtime controls are adequate for this narrow comparison:

- All three arms use the same uninstrumented common adjacency harness prefix. The candidate replaces only the original LMDB production entry; the other 126 entries of the preserved 127-entry base runtime remain exact. The old test harness, fixtures, dependencies, and root launcher are preserved.
- The frozen protocol binds the exact independently reviewed production patch hash, the candidate's source-matched passing validation, the driver/freezer/identity helper hashes, and runtime manifests. The driver checks its control files and common harness before each fork and verifies complete preserved identities around the study.
- Every fork must retain the same input and JVM metadata, graph/query hashes, independent row count and checksum, zero endpoint ledgers, and the expected coordinate admission behavior. Every query must show a native semantic compilation, and the post-main root audit must reconcile thirteen compiles with no hosted generic root.
- Native evaluation and direct adjacency are enabled; adjacency must remain ACTIVE at the exact unchanged data revision and show positive query-time lookup hits. The latter establishes successful query-time use, not that every row used adjacency or that all hits occurred only during execution rather than preparation.
- Measured queries must use compiled IR exclusively under observed nonforced AUTO selection. At review request, route-histogram inequality was only reported in the summary. This was corrected before freezing: every measured route/tier histogram must equal the first qualified fork, or the driver preserves the differing fork and stops without accepted complete comparison estimates.

The instrumented namespace diagnostic is separate from these timing runtimes. No diagnostic timings are pooled with the primary comparison. This review approved the experiment controls before its performance outcome was known; it does not independently certify any eventual ratios.

## Page-occupancy diagnostic review

The separate diagnostic is under `shared-selection/page-occupancy/`. Reviewed files include `protocol.md`, `run_page_diagnostic.py`, `diagnostic-overlay.patch`, the copied complete `NativeResultMaterializer.java`, and `FactorPageDiagnostics.java`.

The overlay preserves the original cache keys, publication and admission decisions, reservations, producer calls, and ownership operations. Its additions are counters and observations. It records page, directory, and factor-memo charges separately, including the old/new directory reservation overlap. It allocates fixed counters once per materializer, adds no per-row allocations, and builds a primitive page-key table at final close. The original cleanup is protected by `finally`, and JSON output follows cleanup.

The initial draft would have required unchanged nested classes to hash identically after recompilation. Inserted source lines can change their debug line tables. This was corrected before compilation: all compiler output is preserved, but only the changed outer materializer class and the new diagnostic helper enter the actual overlay. Complete source regions for the projection plan, `FactorMemo`, and `FactorPage` are checked unchanged, and their runtime classes resolve from the preserved original entries. This prevents debug-only recompilation differences from expanding the runtime change.

The runner pins the exact original preimage, source/patch/protocol hashes, shared fixture and harness, and original runtime. It rejects `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`, and `_JAVA_OPTIONS`, and checks the recorded instrumented source identity, exact JVM arguments, Java version/vendor, heap, ledger limits, and configured native/PREFER adjacency. Its inherited result oracle validates the exact bag and checksum; the root and measured route checks establish the native compiled-IR path. This harness waits for adjacency readiness but does not expose positive adjacency lookup hits. The protocol correctly limits its adjacency claim to configuration and readiness.

### Interpretation limits

Stored occupancy means nonnull weak-reference slots at removal; it is neither lifetime ever-touched occupancy nor a count of semantically valid current values. Live-referent occupancy is a transient `WeakReference.get()` observation and can briefly strengthen a referent. Extra counters, final table allocation, output allocation, and I/O can affect performance and GC. These are observations of an instrumented execution, with no latency, allocation, or GC comparison inferred from it.

The raw final table includes only pages still present before final materializer cleanup. Earlier namespace retirements and bulk clears have separate counters and histograms; their keys are not recovered. When those counts are nonzero, namespace/parent multiplicity claims must be restricted to the final retained pages. Parent identities are scoped by namespace and adjacency group; per-target grouping also includes the tuple column.

`Reservation.bytes()` is the remaining claim at observation time. A prior query-ledger close can release a claim before explicit materializer cleanup. The corrected report therefore separates grants, bytes observed before explicit release, declared structural page charges, exact page-close counts, and final ledger endpoints. A grant-minus-observed-release difference alone is not evidence of a leak. Directory overlap now reads the actual old reservation's remaining bytes instead of accumulating a stale running balance.

The diagnostic's proposed uncapped page geometry is explicitly a source-based hypothesis. It is not a measured result or proof that a different page layout would improve runtime. Any layout or admission-policy change would need a separately reviewed candidate and its own matched experiment.

## Review disposition

The exact guard and its matching focused correctness evidence pass this review. The prospective primary timing driver and the final page diagnostic source/runner/protocol pass source review after the corrections described above. Earlier artifacts remain preserved. Performance results and the page diagnostic's measured findings require their separate execution and analysis records; this report does not substitute for them.
