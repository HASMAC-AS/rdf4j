# Depth-only broader correctness: first window

Partial qualification, not full-module/repository coverage. No production changes.

## Runtime identity
- Prior-best A:5ac89b5; depth-only AD:0750336e, using previously frozen cp_a.txt/cp_ad.txt.
- Actual origins in both logs: LmdbStoreConnection from read-depth/layer; QueryExecutionContext from original rdf4j-http-client dependency; LmdbRecordIterator, ValueStore and NativeValueMemo from immutable record-cancellation/baseline-classes.
- Excludes the unproven resource-restoration layer, slower cancellation changes and grouped test-only memo seam. No current production target/classes fallback.
- Historical LMDB test classes/resources from harness-test-dependencies, selected using source names present in that bundle. Exact selected class-family hashes in test-manifest.json. Three HTTP test families separately copied/frozen; hashes in http-test-manifest.json.

## Completed
`ad-01.log`:20 LMDB classes;390 found,387 passed,3 skipped,0 failed,0 aborted; launcher exit0. Sum of reported per-class test durations173063 ms. Covers adaptive replay, frozen/spill replay, adjacency contexts/executor lifecycle/snapshots, blank-node/reference lifecycle, growth/replay/cleanup isolation, native streaming worker contexts, variable-predicate isolation, NONE reads, nonrepeatable replay, mixed snapshots and optimistic isolation (117 cases).

Three skips are default memo-testProbe-gated replay tests. They were not enabled or silently replaced with repaired fixtures in this runtime. The separately qualified gated fixture is separate evidence, not this run.

`ad-http.log`:3 affected HTTP context classes;12 found/passed,0 failed/skipped/aborted; exit0. Nested initializer/value identity, replay worker scope and query-request context tests run against unchanged HTTP production runtime.

First-window total:399 passed,3 skipped across23classes. No observed failures required baseline reruns. These are correctness test durations, not throughput/latency measurements.

## Explicit remaining coverage
- Scope selection contains76 frozen LMDB classes;56 remain unexecuted in scope-02/03/04.txt.
- Source names absent from historical compiled bundle:RepositorySourcePullScopeTest, ValueStoreNativeValueCodecTest, ValueStoreWriterGuardTest. These are not counted here; prior separate SOURCE_PULL/writer-guard evidence must remain distinguishable.
- Native-evaluation419 source test classes and queryalgebra evaluation263 compiled test classes were inventoried but not run in this window. No full suite claim.
- Root prioritized Workbench reconstruction after this batch. CPU/build lock handed to inventory; resume remaining correctness only after its build/timing window releases.

Launcher source/script hashes recorded before use. No active-script edits, no module rebuild/install, no production edits and no publication performed.

## Second window: completed remaining scope selection
After Workbench measurements released CPU, immutable runtime verification checked6978 entries against the original AD runtime manifest, with zero mismatches (`runtime-reverification.json`). No source/jar rebuilding or substitute layers.

- batch02:20classes,263 passed,zero skips/failures;40634ms summed test durations.
- batch03:20classes,379 passed,zero skips/failures;12345ms.
- batch04:16classes,352 passed,zero skips/failures;21284ms.
- All launcher exits0. Injected native/cleanup error logs are expected tests whose final assertions passed; no failure suppression or assertion changes.

Completed all76 selected LMDB classes plus3HTTPclasses: **1393 passed,3 default-gated skips,0 failures/aborts across79classes**. `scope-summary.json` records per-batch counts/durations. No failures required matched baseline reruns. BaselineA remains available for any later failure triage.

The earlier remaining56 note is now superseded: those56 completed. Entire native-evaluation419/queryalgebra263 suites remain unrun. A focused next native-evaluation selection of28classes covering cancellation/context/replay/lifetime/materialization/workers/streaming is prepared in native-01/02.txt, with exact historical class hashes. NativeRecordCancellationTest is explicitly excluded because absent from frozen historical test bundle; this is the later, deliberately excluded cancellation candidate.

CPU handed back to inventory for2 requested HTTP profiles. No correctness JVM remains active. Resume those28 only after explicit handback; no full module claim.
