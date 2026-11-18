# Separate corrected-consumer HTTP profiles

Four separate JVMs, each 60 warmups and 20 recorded requests. Programmatic JFR begins after warmup; 128-frame recording/export. Node V8 main-isolate profiles cover each recorded client process lifetime, including startup/session GET/metrics and idle, not just POST. Native worker CPU including gzip decompression is not comprehensively sampled. Diagnostic durations are not clean benchmark observations. All profile requests passed the same route/output/no-spill/origin gates.

## JVM execution samples

Nearest categories are mutually exclusive sampled-stack classification. Inclusive mechanisms and ThreadLocal-visible samples overlap and must not be summed into total elapsed fractions. These are neither exact invocation counts nor wall-time percentages.

| Arm | Java samples | TLS-visible | NDJSON nearest | Binding nearest | Lifetime nearest | Binary writer | Binary parser |
|---|---:|---:|---:|---:|---:|---:|---:|
| base | 2205 | 118 | 544 | 411 | 298 | 378 | 79 |
| depth | 2104 | 72 | 490 | 502 | 229 | 200 | 93 |
| serializer | 1856 | 110 | 402 | 434 | 250 | 162 | 82 |
| stack | 1752 | 70 | 319 | 452 | 221 | 183 | 100 |

## Inclusive dispatch and TLS caller witnesses

### base
{"genericPackedJson": 472, "incomingListBindingLookup": 94, "serverSnapshotBinaryWriter": 80, "workbenchMapAdapter": 47}

{"org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer": 15, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close": 4, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext": 15, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread": 5, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead": 12, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead": 19, "org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext": 9, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground": 5, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0": 5, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach": 15}

### depth
{"genericPackedJson": 443, "incomingListBindingLookup": 141, "serverSnapshotBinaryWriter": 88, "workbenchMapAdapter": 56}

{"org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer": 10, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close": 2, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext": 9, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread": 2, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead": 11, "org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext": 8, "org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.restoreSourcePull": 2, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground": 4, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.close": 6, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach": 8}

### serializer
{"incomingListBindingLookup": 134, "serverSnapshotBinaryWriter": 66, "typedPackedJson": 369, "workbenchMapAdapter": 65}

{"org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer": 18, "org.eclipse.rdf4j.http.client.QueryExecutionContext.lambda$activateExternalValueInitializer$0": 7, "org.eclipse.rdf4j.http.client.QueryExecutionContext.replay": 2, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext": 9, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead": 13, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead": 16, "org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext": 5, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0": 5, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init>": 3, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach": 17}

### stack
{"incomingListBindingLookup": 113, "serverSnapshotBinaryWriter": 96, "typedPackedJson": 288, "workbenchMapAdapter": 61}

{"org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer": 18, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close": 3, "org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext": 10, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread": 3, "org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead": 5, "org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext": 5, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground": 4, "org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0": 3, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init>": 2, "org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach": 6}

## Corrected benchmark consumer

The new client uses line-splitter.mjs: only the newly decoded chunk is searched; retained fragments join once when a delimiter is found. No accumulated prefix is rescanned. The generator yields immediately so the first rows-batch timestamp still follows that record’s JSON.parse before subsequent records are processed. This is a benchmark correction, not a Workbench browser fix: the frozen actual frontend already uses equivalent linear framing.

V8 position ticks are sampled source positions, not exact operations or wall time. No controlled old-versus-new parser speedup is inferred from separate populations. Client JSON.parse is now line 34; splitter line 8 performs the delimiter search and line 17 joins completed fragments. Raw per-line maps below avoid attributing a moved helper name to eliminated work.

| Arm | Node samples | Client source-line ticks | Splitter source-line ticks |
|---|---:|---|---|
| base | 31002 | {"1": 2, "12": 1, "22": 1, "23": 3, "24": 1, "26": 1, "29": 1, "30": 48, "31": 1, "32": 65, "34": 9470, "35": 1, "36": 1, "37": 1, "38": 1, "42": 1, "47": 43, "58": 33, "59": 4, "60": 1} | {"10": 18, "17": 2261, "21": 2, "5": 24, "7": 1, "8": 113} |
| depth | 30148 | {"1": 2, "14": 1, "18": 1, "22": 1, "23": 4, "24": 1, "29": 3, "30": 45, "31": 3, "32": 65, "34": 9195, "37": 1, "42": 1, "47": 51, "49": 1, "52": 3, "58": 37, "59": 2, "60": 2} | {"10": 23, "11": 2, "15": 1, "17": 2277, "5": 28, "7": 2, "8": 105} |
| serializer | 26679 | {"1": 6, "13": 4, "14": 1, "20": 1, "22": 1, "23": 1, "29": 1, "30": 26, "32": 56, "34": 8503, "36": 5, "37": 1, "39": 1, "47": 41, "52": 1, "58": 33, "59": 3, "60": 2} | {"1": 2, "10": 17, "11": 1, "17": 2253, "21": 1, "23": 1, "5": 15, "7": 2, "8": 95} |
| stack | 26828 | {"1": 3, "13": 1, "18": 1, "22": 1, "23": 1, "24": 2, "30": 35, "32": 51, "33": 1, "34": 8686, "35": 2, "37": 1, "42": 2, "47": 47, "58": 35, "59": 4, "60": 1} | {"10": 12, "11": 2, "13": 1, "16": 1, "17": 2215, "21": 2, "5": 23, "7": 2, "8": 118, "9": 1} |

The intended client mechanism is no longer dominant: corrected splitter delimiter-search positions have 95–118 ticks per arm, versus 8,503–9,470 for JSON.parse and 2,215–2,277 for fragment joining. The former accumulated-prefix loop is absent by source construction. This confirms the specific scan work is bounded and small in these diagnostic samples, not an exact controlled speedup or proof about native gzip cost. JVM profiles retain substantial NDJSON and binding work; generic dispatch appears in base/depth and typed dispatch in serializer/stack, as intended. Depth arms have fewer TLS-visible samples, but different sample denominators and durations prevent treating that as saved wall time.

## Leading Node leaves

### base
-  @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-consumer-workbench/client.mjs:1: 9700
- (idle) @ :0: 9694
- push @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-consumer-workbench/line-splitter.mjs:5: 2419
- processCallback @ node:zlib:530: 2199
- (garbage collector) @ :0: 1152
- decodeUTF8 @ :0: 1064
- decode @ node:internal/encoding:482: 701
- fetchParams.controller.resume @ node:internal/deps/undici/undici:13915: 288
- (program) @ :0: 253
- compileForInternalLoader @ node:internal/bootstrap/realm:383: 220
### depth
-  @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-consumer-workbench/client.mjs:1: 9439
- (idle) @ :0: 8963
- push @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-consumer-workbench/line-splitter.mjs:5: 2438
- processCallback @ node:zlib:530: 2269
- (garbage collector) @ :0: 1178
- decodeUTF8 @ :0: 1048
- decode @ node:internal/encoding:482: 647
- fetchParams.controller.resume @ node:internal/deps/undici/undici:13915: 314
- (program) @ :0: 246
- nextTick @ node:internal/process/task_queues:113: 216
### serializer
-  @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-consumer-workbench/client.mjs:1: 8706
- (idle) @ :0: 6789
- push @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-consumer-workbench/line-splitter.mjs:5: 2385
- processCallback @ node:zlib:530: 2091
- decodeUTF8 @ :0: 1184
- (garbage collector) @ :0: 1099
- decode @ node:internal/encoding:482: 614
- fetchParams.controller.resume @ node:internal/deps/undici/undici:13915: 267
- compileForInternalLoader @ node:internal/bootstrap/realm:383: 228
- (program) @ :0: 198
### stack
-  @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-consumer-workbench/client.mjs:1: 8894
- (idle) @ :0: 6636
- push @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/linear-consumer-workbench/line-splitter.mjs:5: 2377
- processCallback @ node:zlib:530: 2272
- decodeUTF8 @ :0: 1131
- (garbage collector) @ :0: 1090
- decode @ node:internal/encoding:482: 588
- fetchParams.controller.resume @ node:internal/deps/undici/undici:13915: 274
- (program) @ :0: 234
- compileForInternalLoader @ node:internal/bootstrap/realm:383: 213

## Allocation and native cautions

Allocation sample weights are statistical weights, not exact allocated bytes or retained heap. Native accept/epoll/socket samples do not establish blocked duration or backpressure. Gzip work exists on both sides of the observed decoded stream; Node V8 samples omit some native-worker CPU. Pinned Jackson uses a concurrent-deque recycler, not ThreadLocal.

- base sampled allocation path weights: {"other_or_unresolved": 54441080168, "server_binary_writer": 230961208, "workbench_adapter": 3018046960, "workbench_page_serialization": 2951962640}

- depth sampled allocation path weights: {"other_or_unresolved": 50224479640, "server_binary_writer": 307817752, "workbench_adapter": 3171783096, "workbench_page_serialization": 6241822600}

- serializer sampled allocation path weights: {"other_or_unresolved": 48992295792, "server_binary_writer": 460509304, "workbench_adapter": 3054599392, "workbench_page_serialization": 2762799368}

- stack sampled allocation path weights: {"other_or_unresolved": 50052532240, "server_binary_writer": 376681808, "workbench_adapter": 2949411592, "workbench_page_serialization": 6576051248}

## Decision limits

Use clean RESULTS.md for the matched outcome. Profiles identify mechanisms and remaining work; they do not prove the cause of arm differences, erase variability, or establish universal rollout/merge safety. No further optimization or clean rerun is included in this phase.
