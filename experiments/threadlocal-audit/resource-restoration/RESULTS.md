# External initializer restoration: matched factorial results

**The resource change has no demonstrated incremental throughput/latency benefit over depth in this experiment.** Both/depth pooled latency was 1.25% lower for regular and about 2.90% lower for each native route, but paired signs were mixed and every descriptive interval included zero. Janino both/base was 9.55% lower with all four blocks favorable, while depth/base was already 6.85% lower. That combined result does not establish an additional resource win or synergy. Keep depth as the better-supported candidate; treat resource restoration as experimental pending stronger evidence, not a proven default improvement.

## Question and exact runtime

The primary question is whether resource restoration improves the holder-depth candidate (both versus depth), not whether adding a previously slower cancellation path can be rescued. All four timed arms exclude record-cancellation changes and the test-only grouped-memo observer. The base is incumbent combined D, not upstream main or a claim of globally fastest configuration.

| Label | Harness arm | Published source |
|---|---|---|
| base | A | `5ac89b5c137030fea1aa6aa7160984423d7f4cd5` |
| depth | B | `0750336e71ab0ca0d216ce9c36483a6e9063337d` |
| resource | C | `395033f32fe8163b8e9994acaa5e7ae96c680cdc` |
| both | D2 | `11ee50564ce307ce0ad33d4eacef28554c287f13` |

All arms use the original immutable LMDB runtime and an identical frozen copy of its HTTP-client dependency jar. Depth adds only the 20-class LmdbStoreConnection family; resource adds only the 11-class QueryExecutionContext family. Five resource class bytes changed, six did not; no family additions/deletions. Source/class hashes, original jar SHA and loaded origins are retained. No full rebuilt dependency jar, current target/classes directory, cancellation layer or grouped-fix layer is on the timed classpaths.

## Safety, route qualification and memory tradeoff

The HTTP selection passed 17 tests. A clean isolated depth-plus-resource selection passed 107 tests with three gated skips and launcher exit 0. A separate repaired gated fixture selection passed all three tests on base, depth, and depth-plus-resource using a test-only populated-memo observer; that observer is excluded from all timed runtimes, even disabled. This is scoped qualification, not a full repository pass. Earlier failed fixture barriers did not demonstrate replay-safety failures; the repair reaches actual memo population with grouped-route proof before exposure.

All 12 untimed small-fixture arm/route qualifications passed dual checksums, route/factory/bind assertions and no-spill/returned-budget checks. Loaded origins distinguish resource-only and holder-only layers from baseline. These are execution checks, not a substitute for nested replay/ownership behavior tests.

The resource change restores a null initializer using ThreadLocal.set(null) rather than remove(). It retains one static-key null slot per touched live thread, without retaining the previous Consumer in that slot. It changes map occupancy and opportunistic stale-entry cleanup behavior, not merely syntax. No general retained-heap bound for unrelated stale entries is inferred from query-thread allocation counters; this tradeoff should remain explicit. Scope lifetime, tokens, callback and worker ownership remain unchanged by the intended patch.

## Prespecified protocol

48 fresh JVM forks: base/depth/resource/both × regular/interpreted/Janino × four independent forks. Each fork uses 20 warmups and 10 measured queries. Fixed four-block Latin order: A B D2 C / B C A D2 / C D2 B A / D2 A C B. Every arm occurs once per block and at every temporal position. All samples retained, with no outcome-dependent trimming or rerun.

Same 100-group/fanout-10 fixture: 26,700 triples, 100,000 six-slot OPTIONAL DISTINCT results. Full-term dual checksums 1899306305404719472 and 3694550068168048289; proven routes/binds, no spill or retained native budget. Same inherited measured flags including partialReady ON and grouped waves OFF, native 640 MiB cap and heap-only generic collections. JDK 25.0.5, -Xms2g/-Xmx3g. Trace/JFR OFF during clean timing; low-volume GC logs ON identically. First-row latency includes eager evaluation and complete projected values; full latency includes connection open, prepare, evaluate, consumption and close. Allocations are query-thread totals, not retained heap or all worker allocations. Not main/HTTP/Workbench/concurrent-QPS/tail-latency evidence.

## Arm means

| Route | Arm | Full ms | Fork-mean range ms | First row ms | Allocation MB |
|---|---|---:|---:|---:|---:|
| broad-regular | base | 1567.457 | 1505.017–1637.065 | 8.731 | 2320.80 |
| broad-regular | depth | 1515.489 | 1488.026–1560.108 | 8.974 | 2276.40 |
| broad-regular | resource | 1502.298 | 1438.186–1568.978 | 8.952 | 2336.14 |
| broad-regular | both | 1496.559 | 1450.868–1568.839 | 8.316 | 2341.35 |
| broad-interpreted | base | 229.160 | 225.323–235.292 | 12.316 | 188.98 |
| broad-interpreted | depth | 225.346 | 214.975–236.186 | 12.321 | 181.47 |
| broad-interpreted | resource | 224.800 | 217.388–231.326 | 13.071 | 181.98 |
| broad-interpreted | both | 218.822 | 212.002–221.971 | 12.397 | 174.40 |
| broad-janino | base | 175.851 | 167.559–184.120 | 12.720 | 177.95 |
| broad-janino | depth | 163.802 | 155.013–171.568 | 13.179 | 171.69 |
| broad-janino | resource | 178.810 | 166.469–192.719 | 13.258 | 172.37 |
| broad-janino | both | 159.051 | 152.578–171.424 | 12.201 | 165.91 |

## Matched contrasts

Positive reduction favors the numerator. Throughput is reciprocal latency, not the negative latency percentage. Independent fork means, not measured iterations, are analysis units. Descriptive intervals use four paired log ratios and t(3) = 3.182, assuming independent approximately normal block effects. Exploratory and unadjusted for multiple comparisons; no general workload guarantee.

| Route | Contrast | Pooled latency reduction | Throughput change | Four paired reductions | Descriptive 95% interval |
|---|---|---:|---:|---|---|
| broad-regular | depth/base | +3.32% | +3.43% | -0.89%, +9.10%, +0.94%, +3.69% | -3.98% to +10.05% |
| broad-regular | resource/base | +4.16% | +4.34% | -4.25%, +10.61%, +2.30%, +7.38% | -6.61% to +13.86% |
| broad-regular | both/depth | +1.25% | +1.26% | +3.25%, +2.50%, +4.01%, -4.91% | -5.38% to +7.51% |
| broad-regular | both/resource | +0.38% | +0.38% | +6.37%, +0.85%, +2.67%, -9.08% | -10.48% to +10.15% |
| broad-regular | both/base | +4.52% | +4.74% | +2.39%, +11.37%, +4.91%, -1.03% | -4.30% to +12.59% |
| broad-interpreted | depth/base | +1.66% | +1.69% | -3.25%, +3.77%, +5.41%, +0.68% | -4.51% to +7.55% |
| broad-interpreted | resource/base | +1.90% | +1.94% | -1.13%, +4.09%, +1.08%, +3.52% | -1.97% to +5.65% |
| broad-interpreted | both/depth | +2.90% | +2.98% | +6.02%, +6.37%, -2.90%, +1.65% | -4.26% to +9.49% |
| broad-interpreted | both/resource | +2.66% | +2.73% | +4.04%, +6.05%, +1.60%, -1.25% | -2.48% to +7.53% |
| broad-interpreted | both/base | +4.51% | +4.72% | +2.96%, +9.90%, +2.67%, +2.32% | -1.56% to +10.23% |
| broad-janino | depth/base | +6.85% | +7.36% | +5.69%, +4.04%, +7.49%, +10.09% | +2.63% to +10.90% |
| broad-janino | resource/base | -1.68% | -1.65% | +2.19%, +6.90%, -15.02%, -1.52% | -17.23% to +12.04% |
| broad-janino | both/depth | +2.90% | +2.99% | +6.44%, +10.63%, -2.49%, -3.55% | -8.75% to +13.38% |
| broad-janino | both/resource | +11.05% | +12.42% | +9.79%, +7.89%, +17.56%, +8.29% | +3.28% to +18.05% |
| broad-janino | both/base | +9.55% | +10.56% | +11.77%, +14.25%, +5.18%, +6.90% | +2.64% to +16.06% |

## Combination interaction

The earlier interpreted depth benefit did not reproduce at the same magnitude: here depth/base is 1.66% lower with mixed signs, versus 7.63% in the previous matched study. Janino depth remains favorable in all four blocks here. This variation limits generalization; do not pool studies post hoc or discard the unfavorable blocks. Resource-only Janino was 1.68% slower pooled, dominated by an adverse third block that remains included. No interaction interval excludes zero.

Paired log(both/depth) minus log(resource/base). Negative means the resource effect is larger with depth. This is not equivalent to beating depth or base in absolute latency.

- broad-regular: blocks -0.07468, +0.08688, -0.01763, +0.12459; mean +0.02979, descriptive 95% interval [-0.11665, +0.17623].
- broad-interpreted: blocks -0.07327, -0.02402, +0.03948, +0.01917; mean -0.00966, descriptive 95% interval [-0.08922, +0.06989].
- broad-janino: blocks -0.04446, -0.04094, -0.11530, +0.01986; mean -0.04521, descriptive 95% interval [-0.13316, +0.04274].

## Convergence and whole-JVM GC

GC totals include startup, fixture/readiness, warmup and measurements. No timestamp join attributes a pause to a particular query and no GC-related filtering was applied.

### broad-regular
- base measured-index means ms: 1636.74, 1607.20, 1587.92, 1570.23, 1615.28, 1530.74, 1525.06, 1576.88, 1533.07, 1491.46.
- depth measured-index means ms: 1540.41, 1532.68, 1555.98, 1607.83, 1553.72, 1494.41, 1497.28, 1482.55, 1446.66, 1443.36.
- resource measured-index means ms: 1512.57, 1506.57, 1510.41, 1613.91, 1496.12, 1554.34, 1492.30, 1435.39, 1477.98, 1423.40.
- both measured-index means ms: 1551.09, 1549.10, 1548.14, 1470.15, 1526.97, 1491.24, 1443.62, 1450.83, 1458.44, 1476.01.
- broad-regular-01-A.log: mean 1505.017 ms, within-fork 1393.353–1638.515 ms; GC 66 pauses, total 1899.8 ms, max 62.8 ms.
- broad-regular-02-B.log: mean 1518.361 ms, within-fork 1352.600–1730.815 ms; GC 63 pauses, total 1951.4 ms, max 120.6 ms.
- broad-regular-03-D2.log: mean 1468.988 ms, within-fork 1415.532–1526.825 ms; GC 66 pauses, total 1923.0 ms, max 75.7 ms.
- broad-regular-04-C.log: mean 1568.978 ms, within-fork 1469.932–1665.251 ms; GC 66 pauses, total 1840.7 ms, max 60.8 ms.
- broad-regular-05-B.log: mean 1488.026 ms, within-fork 1403.995–1600.542 ms; GC 63 pauses, total 1880.1 ms, max 50.9 ms.
- broad-regular-06-C.log: mean 1463.370 ms, within-fork 1393.809–1529.379 ms; GC 66 pauses, total 1941.5 ms, max 76.5 ms.
- broad-regular-07-A.log: mean 1637.065 ms, within-fork 1524.989–1739.744 ms; GC 63 pauses, total 2056.0 ms, max 58.1 ms.
- broad-regular-08-D2.log: mean 1450.868 ms, within-fork 1377.280–1534.912 ms; GC 66 pauses, total 1890.7 ms, max 52.8 ms.
- broad-regular-09-C.log: mean 1538.660 ms, within-fork 1408.962–1801.624 ms; GC 65 pauses, total 1827.5 ms, max 59.4 ms.
- broad-regular-10-D2.log: mean 1497.541 ms, within-fork 1421.820–1654.446 ms; GC 66 pauses, total 2060.5 ms, max 65.3 ms.
- broad-regular-11-B.log: mean 1560.108 ms, within-fork 1414.492–1738.021 ms; GC 66 pauses, total 1822.8 ms, max 61.5 ms.
- broad-regular-12-A.log: mean 1574.930 ms, within-fork 1409.910–1748.685 ms; GC 66 pauses, total 1793.6 ms, max 59.0 ms.
- broad-regular-13-D2.log: mean 1568.839 ms, within-fork 1441.501–1741.019 ms; GC 66 pauses, total 1810.1 ms, max 86.1 ms.
- broad-regular-14-A.log: mean 1552.815 ms, within-fork 1407.593–1704.520 ms; GC 66 pauses, total 1720.4 ms, max 49.1 ms.
- broad-regular-15-C.log: mean 1438.186 ms, within-fork 1373.598–1525.820 ms; GC 66 pauses, total 1879.8 ms, max 60.9 ms.
- broad-regular-16-B.log: mean 1495.461 ms, within-fork 1388.571–1562.642 ms; GC 66 pauses, total 1928.3 ms, max 60.9 ms.

### broad-interpreted
- base measured-index means ms: 219.39, 239.13, 222.90, 232.74, 245.96, 243.01, 221.45, 221.89, 225.59, 219.53.
- depth measured-index means ms: 225.67, 213.08, 219.57, 225.63, 223.01, 223.67, 234.37, 235.10, 228.93, 224.42.
- resource measured-index means ms: 228.20, 226.07, 224.67, 219.62, 234.16, 228.44, 229.63, 217.39, 223.09, 216.71.
- both measured-index means ms: 230.00, 214.40, 230.00, 217.77, 216.22, 213.88, 214.93, 222.14, 217.29, 211.57.
- broad-interpreted-01-A.log: mean 228.749 ms, within-fork 211.956–243.175 ms; GC 8 pauses, total 53.6 ms, max 17.3 ms.
- broad-interpreted-02-B.log: mean 236.186 ms, within-fork 219.167–272.663 ms; GC 8 pauses, total 80.4 ms, max 22.5 ms.
- broad-interpreted-03-D2.log: mean 221.971 ms, within-fork 205.065–250.200 ms; GC 8 pauses, total 58.5 ms, max 15.4 ms.
- broad-interpreted-04-C.log: mean 231.326 ms, within-fork 214.442–261.357 ms; GC 8 pauses, total 67.3 ms, max 17.7 ms.
- broad-interpreted-05-B.log: mean 226.426 ms, within-fork 211.622–239.979 ms; GC 8 pauses, total 62.0 ms, max 17.9 ms.
- broad-interpreted-06-C.log: mean 225.659 ms, within-fork 212.655–243.870 ms; GC 8 pauses, total 57.4 ms, max 18.2 ms.
- broad-interpreted-07-A.log: mean 235.292 ms, within-fork 218.538–267.243 ms; GC 8 pauses, total 65.7 ms, max 21.2 ms.
- broad-interpreted-08-D2.log: mean 212.002 ms, within-fork 205.068–219.159 ms; GC 8 pauses, total 63.2 ms, max 19.6 ms.
- broad-interpreted-09-C.log: mean 224.825 ms, within-fork 214.906–244.273 ms; GC 8 pauses, total 60.4 ms, max 22.5 ms.
- broad-interpreted-10-D2.log: mean 221.219 ms, within-fork 209.405–244.688 ms; GC 8 pauses, total 76.9 ms, max 21.9 ms.
- broad-interpreted-11-B.log: mean 214.975 ms, within-fork 203.001–261.861 ms; GC 8 pauses, total 56.9 ms, max 15.0 ms.
- broad-interpreted-12-A.log: mean 227.276 ms, within-fork 217.230–240.529 ms; GC 8 pauses, total 70.3 ms, max 19.1 ms.
- broad-interpreted-13-D2.log: mean 220.096 ms, within-fork 210.347–243.432 ms; GC 8 pauses, total 74.5 ms, max 27.9 ms.
- broad-interpreted-14-A.log: mean 225.323 ms, within-fork 215.406–246.527 ms; GC 8 pauses, total 63.2 ms, max 16.4 ms.
- broad-interpreted-15-C.log: mean 217.388 ms, within-fork 210.534–223.688 ms; GC 8 pauses, total 47.1 ms, max 12.9 ms.
- broad-interpreted-16-B.log: mean 223.798 ms, within-fork 203.823–244.883 ms; GC 8 pauses, total 69.3 ms, max 17.3 ms.

### broad-janino
- base measured-index means ms: 189.10, 162.89, 161.56, 167.92, 176.35, 177.46, 181.07, 201.08, 171.97, 169.11.
- depth measured-index means ms: 173.07, 162.56, 161.27, 168.30, 160.83, 163.55, 158.93, 162.31, 166.99, 160.22.
- resource measured-index means ms: 201.78, 176.41, 167.78, 171.57, 190.46, 187.51, 188.13, 170.38, 171.53, 162.54.
- both measured-index means ms: 164.41, 166.48, 153.07, 148.77, 149.60, 160.67, 154.40, 156.56, 166.35, 170.20.
- broad-janino-01-A.log: mean 172.925 ms, within-fork 158.081–191.564 ms; GC 8 pauses, total 68.6 ms, max 17.5 ms.
- broad-janino-02-B.log: mean 163.084 ms, within-fork 154.305–179.627 ms; GC 8 pauses, total 89.8 ms, max 23.2 ms.
- broad-janino-03-D2.log: mean 152.578 ms, within-fork 146.144–161.420 ms; GC 8 pauses, total 61.4 ms, max 14.7 ms.
- broad-janino-04-C.log: mean 169.139 ms, within-fork 160.869–176.221 ms; GC 8 pauses, total 57.6 ms, max 16.0 ms.
- broad-janino-05-B.log: mean 171.568 ms, within-fork 158.543–184.578 ms; GC 8 pauses, total 80.3 ms, max 25.8 ms.
- broad-janino-06-C.log: mean 166.469 ms, within-fork 154.050–179.222 ms; GC 8 pauses, total 64.7 ms, max 15.2 ms.
- broad-janino-07-A.log: mean 178.800 ms, within-fork 157.656–213.797 ms; GC 8 pauses, total 81.7 ms, max 23.8 ms.
- broad-janino-08-D2.log: mean 153.329 ms, within-fork 146.734–180.570 ms; GC 8 pauses, total 84.8 ms, max 22.6 ms.
- broad-janino-09-C.log: mean 192.719 ms, within-fork 171.470–234.931 ms; GC 8 pauses, total 57.0 ms, max 15.2 ms.
- broad-janino-10-D2.log: mean 158.873 ms, within-fork 147.000–173.295 ms; GC 8 pauses, total 76.9 ms, max 26.7 ms.
- broad-janino-11-B.log: mean 155.013 ms, within-fork 147.300–172.569 ms; GC 8 pauses, total 76.3 ms, max 28.2 ms.
- broad-janino-12-A.log: mean 167.559 ms, within-fork 158.844–182.827 ms; GC 8 pauses, total 77.5 ms, max 22.4 ms.
- broad-janino-13-D2.log: mean 171.424 ms, within-fork 148.127–199.422 ms; GC 8 pauses, total 67.6 ms, max 16.2 ms.
- broad-janino-14-A.log: mean 184.120 ms, within-fork 156.384–256.399 ms; GC 8 pauses, total 62.7 ms, max 17.0 ms.
- broad-janino-15-C.log: mean 186.912 ms, within-fork 158.503–270.612 ms; GC 8 pauses, total 69.1 ms, max 16.7 ms.
- broad-janino-16-B.log: mean 165.544 ms, within-fork 151.404–184.857 ms; GC 8 pauses, total 72.4 ms, max 18.4 ms.

## Separate profiles

All 12 separate JVM recordings completed and matched the 100k dual checksums: four arms across three routes. Each excludes 20 warmups; regular records five queries and native routes 30. Trace counters are OFF. Inclusive categories overlap, inlining and stack truncation hide frames, and samples are neither exact calls nor wall-time percentages. Profile durations are not clean timing comparisons. No retained-heap conclusion follows from these CPU samples.

| Route/arm | Main-thread samples | TLS visible | TL remove | Map cleanSomeSlots | Read/lifetime/replay visible |
|---|---:|---:|---:|---:|---:|
| broad-interpreted/base | 726 | 93 | 17 | 16 | 80 |
| broad-interpreted/depth | 686 | 68 | 6 | 20 | 66 |
| broad-interpreted/resource | 694 | 92 | 16 | 4 | 71 |
| broad-interpreted/both | 623 | 67 | 5 | 8 | 53 |
| broad-janino/base | 503 | 107 | 14 | 7 | 66 |
| broad-janino/depth | 490 | 65 | 3 | 11 | 47 |
| broad-janino/resource | 500 | 88 | 11 | 45 | 83 |
| broad-janino/both | 492 | 61 | 0 | 17 | 56 |
| broad-regular/base | 760 | 24 | 2 | 3 | 60 |
| broad-regular/depth | 702 | 31 | 0 | 1 | 68 |
| broad-regular/resource | 691 | 30 | 1 | 1 | 76 |
| broad-regular/both | 685 | 15 | 0 | 0 | 69 |

The source removes one specific null-restoration remove branch, not every ThreadLocal.remove. In profiles, both versus depth has fewer visible remove samples in interpreted (5 versus 6) and Janino (0 versus 3), while total TLS samples are similar in those native routes. Cleanup is not uniformly lower: Janino resource-only has 45 cleanSomeSlots samples versus base 7, and both has 17 versus depth 11. These single recordings cannot establish a causal explanation or retained-memory effect. They do not overturn the inconclusive incremental timing result.

### Remaining nearest visible ThreadLocal callers

- broad-interpreted/both: org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (11); (truncated caller) (8); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead (8); org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach (6); org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.close (4); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close (4); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (3); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (3).
- broad-janino/both: (truncated caller) (11); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (8); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead (7); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (6); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (5); org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach (4); org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0 (4); org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (3).
- broad-regular/both: org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (4); org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activeExposure (3); org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0 (2); org.eclipse.rdf4j.http.client.QueryExecutionContext.replay (2); org.eclipse.rdf4j.http.client.QueryExecutionContext.checkpointReplaySafepoint (2); org.eclipse.rdf4j.common.iteration.CancellationSignal.current (1); org.eclipse.rdf4j.http.client.QueryExecutionContext.getHandle (1).

Resource context entry/close, read tracking, replay, fused-runtime attach and repository value ownership remain visible. Further work should inspect redundant nested context setup with source-backed ownership proofs rather than assume map-entry retention is universally faster. Keep callbacks, worker handoff, cancellation, replay and exposure contracts. The current evidence supports keeping depth as the better-established candidate and retaining resource as an experiment; no extra timing round or production optimization was added after observing results.

## Evidence

Exact source identities, class/jar manifests, all qualification and timing logs, commands and GC logs are retained. The 7.55 MB raw runtime manifest remains local; its 1.40 MB deduplicated form reconstructs it byte-for-byte, verified with original SHA. All profile summaries and per-method sampled counts are retained. Frozen classes, full JFR/sample files and fixtures remain local. Historical read-depth documents include a cross-reference to subsequent fixture repair without rewriting the earlier outcome.
