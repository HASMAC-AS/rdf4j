# Separate corrected-consumer HTTP profiles

Four separate JVMs, each 60 warmups and 20 recorded requests. Programmatic JFR begins after warmup; 128-frame recording/export. Node V8 main-isolate profiles cover each recorded client process lifetime, including startup/session GET/metrics and idle, not just POST. Native worker CPU including gzip decompression is not comprehensively sampled. Diagnostic durations are not clean benchmark observations. All profile requests passed the same route/output/no-spill/origin gates.

## JVM execution samples

Nearest categories are mutually exclusive sampled-stack classification. Inclusive mechanisms and ThreadLocal-visible samples overlap and must not be summed into total elapsed fractions. These are neither exact invocation counts nor wall-time percentages.

| Arm | Java samples | TLS-visible | NDJSON nearest | Binding nearest | Lifetime nearest | Binary writer | Binary parser |
|---|---:|---:|---:|---:|---:|---:|---:|
| base | 660 | 95 | 88 | 80 | 169 | 36 | 13 |
| depth | 654 | 58 | 91 | 103 | 140 | 47 | 8 |
| serializer | 632 | 97 | 67 | 74 | 193 | 36 | 8 |
| stack | 620 | 72 | 50 | 85 | 171 | 43 | 11 |

## Inclusive dispatch and TLS caller witnesses

### base
{"genericPackedJson": 71, "incomingListBindingLookup": 11, "serverSnapshotBinaryWriter": 15, "workbenchMapAdapter": 6}

{"org.eclipse.rdf4j.common.iteration.CancellationSignal.current": 2, "org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer": 11, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close": 7, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext": 13, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead": 14, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead": 17, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground": 3, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0": 5, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.close": 3, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach": 6}

### depth
{"genericPackedJson": 74, "incomingListBindingLookup": 10, "serverSnapshotBinaryWriter": 22, "workbenchMapAdapter": 9}

{"org.eclipse.rdf4j.common.iteration.CancellationSignal.current": 2, "org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer": 9, "org.eclipse.rdf4j.http.client.QueryExecutionContext.setExternalValueInitializer": 2, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext": 5, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead": 5, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead": 6, "org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext": 7, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground": 4, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init>": 2, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach": 9}

### serializer
{"incomingListBindingLookup": 4, "serverSnapshotBinaryWriter": 8, "typedPackedJson": 58, "workbenchMapAdapter": 14}

{"org.eclipse.rdf4j.common.iteration.CancellationSignal.current": 4, "org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer": 11, "org.eclipse.rdf4j.http.client.QueryExecutionContext.replay": 6, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext": 8, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead": 19, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead": 18, "org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext": 4, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0": 4, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.close": 3, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach": 11}

### stack
{"incomingListBindingLookup": 3, "serverSnapshotBinaryWriter": 14, "typedPackedJson": 50, "workbenchMapAdapter": 12}

{"org.eclipse.rdf4j.common.iteration.CancellationSignal.current": 4, "org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer": 8, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close": 9, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext": 7, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread": 3, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead": 5, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead": 6, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activeExposure": 5, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0": 5, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach": 9}

## Corrected benchmark consumer

The new client uses line-splitter.mjs: only the newly decoded chunk is searched; retained fragments join once when a delimiter is found. No accumulated prefix is rescanned. The generator yields immediately so the first rows-batch timestamp still follows that record’s JSON.parse before subsequent records are processed. This is a benchmark correction, not a Workbench browser fix: the frozen actual frontend already uses equivalent linear framing.

V8 position ticks are sampled source positions, not exact operations or wall time. No controlled old-versus-new parser speedup is inferred from separate populations. Client JSON.parse is now line 34; splitter line 8 performs the delimiter search and line 17 joins completed fragments. Raw per-line maps below avoid attributing a moved helper name to eliminated work.

| Arm | Node samples | Client source-line ticks | Splitter source-line ticks |
|---|---:|---|---|
| base | 11026 | {"1": 1, "13": 2, "14": 2, "22": 4, "24": 1, "29": 1, "30": 4, "32": 19, "34": 2054, "39": 1, "47": 21, "58": 1, "59": 1, "60": 1} | {"1": 1, "17": 322, "5": 3, "8": 9} |
| depth | 11276 | {"1": 1, "13": 2, "14": 1, "22": 4, "23": 1, "28": 2, "30": 4, "32": 19, "34": 2052, "42": 1, "47": 26, "52": 1, "58": 4, "59": 4, "6": 1, "60": 2} | {"1": 2, "10": 4, "17": 329, "5": 4, "7": 2, "8": 5} |
| serializer | 10029 | {"1": 1, "20": 1, "22": 2, "30": 3, "32": 16, "34": 2028, "40": 1, "47": 24, "58": 1, "59": 3, "60": 1} | {"10": 4, "17": 283, "5": 5, "7": 2, "8": 5} |
| stack | 9969 | {"1": 1, "13": 2, "14": 1, "18": 1, "21": 1, "22": 2, "26": 1, "28": 2, "30": 3, "32": 17, "34": 2007, "47": 24, "49": 1, "52": 1, "58": 2, "59": 2} | {"10": 1, "15": 1, "17": 285, "5": 7, "8": 8} |

## Leading Node leaves

### base
- (idle) @ :0: 5160
-  @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-narrow-recovery/client.mjs:1: 2133
- processCallback @ node:zlib:530: 433
- (garbage collector) @ :0: 408
- push @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-narrow-recovery/line-splitter.mjs:5: 334
- compileForInternalLoader @ node:internal/bootstrap/realm:383: 214
- decode @ node:internal/encoding:482: 180
- fetchParams.controller.resume @ node:internal/deps/undici/undici:13915: 153
- __require @ node:internal/deps/undici/undici:5: 129
- nextTick @ node:internal/process/task_queues:113: 93
### depth
- (idle) @ :0: 5036
-  @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-narrow-recovery/client.mjs:1: 2145
- (garbage collector) @ :0: 472
- processCallback @ node:zlib:530: 386
- push @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-narrow-recovery/line-splitter.mjs:5: 344
- compileForInternalLoader @ node:internal/bootstrap/realm:383: 252
- decode @ node:internal/encoding:482: 166
- __require @ node:internal/deps/undici/undici:5: 141
- fetchParams.controller.resume @ node:internal/deps/undici/undici:13915: 136
- (program) @ :0: 117
### serializer
- (idle) @ :0: 4323
-  @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-narrow-recovery/client.mjs:1: 2101
- (garbage collector) @ :0: 402
- processCallback @ node:zlib:530: 385
- push @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-narrow-recovery/line-splitter.mjs:5: 299
- compileForInternalLoader @ node:internal/bootstrap/realm:383: 211
- decode @ node:internal/encoding:482: 165
- fetchParams.controller.resume @ node:internal/deps/undici/undici:13915: 145
- __require @ node:internal/deps/undici/undici:5: 142
- (program) @ :0: 104
### stack
- (idle) @ :0: 4256
-  @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-narrow-recovery/client.mjs:1: 2088
- processCallback @ node:zlib:530: 420
- (garbage collector) @ :0: 416
- push @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-narrow-recovery/line-splitter.mjs:5: 302
- compileForInternalLoader @ node:internal/bootstrap/realm:383: 203
- decode @ node:internal/encoding:482: 165
- fetchParams.controller.resume @ node:internal/deps/undici/undici:13915: 150
- __require @ node:internal/deps/undici/undici:5: 127
- (program) @ :0: 89

## Allocation and native cautions

Allocation sample weights are statistical weights, not exact allocated bytes or retained heap. Native accept/epoll/socket samples do not establish blocked duration or backpressure. Gzip work exists on both sides of the observed decoded stream; Node V8 samples omit some native-worker CPU. Pinned Jackson uses a concurrent-deque recycler, not ThreadLocal.

- base sampled allocation path weights: {"other_or_unresolved": 19061727800, "server_binary_writer": 186713968, "workbench_adapter": 664997416, "workbench_page_serialization": 3156446664}

- depth sampled allocation path weights: {"other_or_unresolved": 18970166240, "server_binary_writer": 227508968, "workbench_adapter": 691811256, "workbench_page_serialization": 3185650312}

- serializer sampled allocation path weights: {"other_or_unresolved": 17983327904, "server_binary_writer": 214096704, "workbench_adapter": 674170344, "workbench_page_serialization": 4836644784}

- stack sampled allocation path weights: {"other_or_unresolved": 14800836336, "server_binary_writer": 227517008, "workbench_adapter": 2254433344, "workbench_page_serialization": 4556520480}

## Decision limits

Use clean RESULTS.md for the matched outcome. Profiles identify mechanisms and remaining work; they do not prove the cause of arm differences, erase variability, or establish universal rollout/merge safety. No further optimization or clean rerun is included in this phase.
