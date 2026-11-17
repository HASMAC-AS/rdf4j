# Compact transport mapping: matched Workbench result

## Conclusion

The identity-preserving transport path improves this full-stack fixture **in combination** with compact snapshots, lifetime refinements and PR #6440. It does not change output or relax the repository value-materialization boundary. The candidate remains default-off; this is not release/merge qualification or a comparison against upstream main.

## Unprofiled matched ABBA

All four fresh-server runs used the same frozen query/API/HTTP-client outputs, improved-hash compact snapshots, lifetime LMDB outputs, pinned writer fca5653, query, ready fixture and memory cap. Only `rdf4j.http.compactIdentityMapping` differed. Order OFF / ON / ON / OFF, under the shared benchmark lock, with no competing builds/tests. Every trial emitted 1M rows, consumed/counted 8M, and passed compiled IR, memo, grouping, no spill and zero retained ledger witnesses. Runtime class origins and the feature property were verified.

| Metric | OFF mean (range) | ON mean (range) |
|---|---:|---:|
| Full query/count completion | 79.994s (75.815–84.172) | 68.828s (68.233–69.422) |
| First received rows | 0.966s (0.893–1.038) | 0.968s (0.937–1.000) |
| 1M-row window received | 23.490s (22.942–24.037) | 21.522s (21.412–21.632) |

**13.96% lower full completion time / 16.22% higher derived full-query row throughput**, and **8.38% lower window time**. No first-row improvement. The OFF arm has substantial variation; two forks per arm are limited evidence, although the observed ranges do not overlap. These are fresh-process/first-query trials after readiness, not warmed steady-state or concurrent-load throughput.

Completion is when Workbench finishes consuming/counting 8M rows; only the 1M-row window is transferred as NDJSON. Neither first received rows nor window completion measures browser DOM paint or IndexedDB. Raw NDJSON byte totals include timing-dependent progress records and are not checksums.

## Correctness

Before timing, exact-value OFF and ON HTTP smokes emitted100 rows and consumed/counted8M, checking terms, Unicode, language, nulls, tuple domain and uniqueness. Both produced checksum `495a217169c1db06ff284e3b4d8a765a6cc75b575cc8179fa1e448b09628c301`. Actual frozen runtime origins and dispatch property were verified. Focused semantic qualification is in QUALIFICATION.md (44 tests plus13 assertions-disabled tests and two real compact/BNode nested-triple integration processes). This is not a full-module/repository pass.

The first transport smoke failed before any query because the new harness classpath filter also removed the HTTP-client SPI jar. The filter was corrected to match versioned module jars precisely; `transport-smoke2-off/on` are the successful smokes. No failed run was counted.

## Evidence

`../workbench-e2e/transport-a-off`, `transport-b-on`, `transport-c-on`, `transport-d-off` JSONs and manifests. Exact-value receipts: `transport-smoke2-off/on`. All module fingerprints and query hashes match. Safety source checkpoint6e6bc3eaeb, documented reproduction a5add75c7c. The harness override loads identical frozen modules for OFF/ON, rather than comparing a fresh candidate build against a differently compiled baseline.

## Diagnostic mechanism check

Separate ON JFR completed in75.428s with all witnesses passing. It is not pooled into unprofiled timings. In6,102 Java execution samples there were **zero sampled `CompactBindingSetSnapshot.materialize` frames**, compared with344 in the preceding combined profile. The direct compact+BNode integration also verifies that the identity path leaves compact storage intact.

Remaining nearest-stack sample shares: writer20.89%, lifetime19.19%, projection/memo17.54%, kernel12.29%, snapshot9.29%, parser7.23%, Workbench page5.20%, transport mapping4.47%, other3.90%. These are Java execution sample shares, not wall-time/removable percentages. Profiled-run variation means share changes alone are not absolute time changes.

Compact hash lookup (`slot`) is now a5.69% leaf, versus1.46% in the preceding profile: preserving compact storage exposes the repeated name lookup to the writer. The next scoped experiment is a sequential-name hint with exact hash fallback for missing/out-of-order/repeated names, not an assumption that observed order is guaranteed.

Sampled allocation weight was~89.92GB, down from~102.20GB in the preceding profile; neither is an exact allocation count. Parser string decoding remains the largest allocation site (~37.64GB weighted), but top-level GC pauses totalled0.978s (72 pauses, max39.57ms) in the76.254s recorded window. The preceding profile had1.043s total pauses. This supports prioritizing execution/copy/lookup costs rather than claiming parser allocation alone is the latency bottleneck.

Raw JFR stays local. Sanitized summary `../workbench-e2e/transport-profile-on-summary.txt` and analyzer `AnalyzeHttp.java` record the mechanism and GC observations.
