# Actual Workbench JVM profile follow-up

**The profiles do not overturn the clean result: no demonstrated Workbench HTTP improvement from holder depth.** They show remaining read/resource-context ThreadLocal work and real JSON serialization downstream of the engine. They cannot prove exactly why the embedded signal failed to carry through, or assign removable wall-time fractions.

## Separate protocol and scope

Two fresh application JVMs, base then depth, same frozen production layers/flags/full 100k query as the clean series. Each performed 20 warmups, then 30 complete diagnostic requests. All 100 requests across the two JVMs passed count, route, memo, no-spill, cleanup and code-origin checks. Programmatic JFR start excluded warmups; stop followed the final request. Diagnostic endpoint/harness classes are separate from the frozen clean harness. No clean timing rerun or outcome-dependent flag change occurred.

JFR covers the colocated Server and Workbench JVM across servlet and parser/executor threads, not only main. Node client CPU/JSON decode is absent, as are browser rendering/IndexedDB. The window includes Node startup/session and metrics gaps between query POSTs; idle/native samples are separated. Recorded durations are not pooled with clean timings. Allocation weights are sampled estimates, not exact allocated/retained bytes.

Initial jfr print exports used the tool default of five frames despite recordings configured for 128. That was corrected by re-exporting the SAME recordings with --stack-depth 128; no workload was rerun. Final summaries use full exported stacks. Original shallow exports remain local for provenance and are not the analysis basis.

## Sample scope and nearest categories

| Nearest Java category | Base samples | Depth samples |
|---|---:|---:|
| binary_parser | 13 | 13 |
| binary_writer | 49 | 52 |
| cancellation | 61 | 70 |
| container_jdk_other | 153 | 124 |
| kernel | 133 | 127 |
| lifetime_read_replay | 264 | 216 |
| logging | 2 | 2 |
| memo_values | 58 | 30 |
| snapshot_binding | 135 | 113 |
| transport_mapping | 9 | 10 |
| workbench_ndjson | 137 | 85 |

Total Java execution samples: base 1014, depth 842. ThreadLocal-visible stacks: base 130, depth 105. Categories select the nearest matching Java frame and are mutually exclusive under the documented classifier. Inclusive frame tables overlap. Neither counts nor percentages are wall-time measurements, exact calls or causally removable fractions.

Workbench NDJSON is genuine additional downstream work: Jackson UTF8JsonGenerator._verifyValueWrite was the top sampled leaf (99 base, 63 depth); full stacks connect the serializer to the page writer. This supports inspecting serialization/batching as an actual-path candidate, not claiming it alone hides the engine gain. Binding/snapshot work and kernel execution remain substantial. The narrow fixture has few binary-parser samples; do not generalize that to wider/larger result sets.

## Remaining actual-path ThreadLocal callers

| Nearest visible caller | Base | Depth |
|---|---:|---:|
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext` | 13 | 30 |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer` | 17 | 14 |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach` | 9 | 12 |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead` | 23 | 7 |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead` | 15 | 6 |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0` | 2 | 6 |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close` | 7 | 5 |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext` | 7 | 4 |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground` | 5 | 4 |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeProbeDeadline.poll` | 5 | 3 |
| `org.eclipse.rdf4j.common.iteration.CancellationSignal.current` | 6 | 3 |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext.setExternalValueInitializer` | 2 | 3 |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread` | 5 | 2 |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.close` | 2 | 2 |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext.lambda$activateExternalValueInitializer$0` | 6 | 1 |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init>` | 2 | 1 |

In the depth recording, enterReadResourceContext is the strongest remaining nearest TLS caller (30 samples), followed by external-value initializer activation (14), fused runtime attach (12), tracked read (7), observed read (6), and foreground-scope cleanup (6). These rank investigation candidates within this recording, not absolute speedup potential. The source-level internal-depth removal is real; total map work does not disappear. ThreadLocal.remove appears in 15 base and 7 depth samples, while set appears in 18 base and 35 depth samples. Different denominators, JIT/map-layout effects and one profile per arm prevent a causal accounting of the delta.

## Next candidates, in order

1. Audit repeated read-resource-context entry/close and fused-runtime attach on the actual HTTP query path. Seek redundant nested setup only where source-level ownership proves restoration can be consolidated. Preserve cross-thread handoff, callbacks, replay, cancellation and exposure boundaries. A generic “skip ThreadLocal” change is not justified.
2. Examine Workbench NDJSON serialization/batching and snapshot/binding representation across the transport boundary. The full pipeline exposes these costs, unlike the embedded drain. Confirm a change with byte/term-equivalent outputs and full-window latency, not a serializer-only microbenchmark.
3. Keep external initializer activation/replay in the inventory, but do not revive the already-inconclusive null-slot restoration experiment solely because it is visible here. Any stronger consolidation needs an independent semantic proof and matched combination test.

## Native and allocation limitations

- base: 930 native-method samples; leading entries sun.nio.ch.EPoll.wait (436), sun.nio.ch.Net.accept (434), java.util.zip.Deflater.deflateBytesBytes (35).
- depth: 899 native-method samples; leading entries sun.nio.ch.Net.accept (419), sun.nio.ch.EPoll.wait (418), java.util.zip.Deflater.deflateBytesBytes (36).

Native samples are dominated by idle socket accept/poll, so they are excluded from Java category percentages; Deflater appears separately but is not a wall-time compression cost. Allocation samples include arrays, maps and Jackson contexts, but unstable class-weight rankings are not retained-heap evidence and do not establish GC as the latency limiter. Node/client CPU and an actual browser profile would be needed for a complete end-to-end CPU account; browser execution remains blocked by the documented environment restriction.

## Evidence

`runs/profile-base` and `runs/profile-depth` retain 50 request results each, manifests, server/GC logs and compact final profile summaries. Full recordings/JSON stay local. `analyze-http-profile.py` documents classification; `PROFILE-PLAN.md`, `run-profile.py` and `profile-source/ProfileBenchmarkServer.java` document collection. These are a diagnostic addendum, not replacement clean measurements.
