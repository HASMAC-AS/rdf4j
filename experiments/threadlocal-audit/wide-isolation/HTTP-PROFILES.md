# Wide30 separate JVM and Node diagnostics

Four fresh JVMs after clean timings, each 60 warmups + 20 recorded full-wide requests. All 320 diagnostic requests passed witnesses. JFR recording/export 128 frames, all Java execution/native/allocation events kept separately. Each recorded request also launches a fresh Node V8 main-isolate CPU profile. No diagnostic duration is pooled into clean results.

Node profiles span startup/session GET/metrics as well as POST receive/parse/count and idle waiting. Native worker CPU, including gzip decompression, is not comprehensively captured. JFR and Node sample populations must not be added or equated with exact CPU/wall time. No reliable first-batch CPU segmentation is claimed.

## JVM sampled work

| Arm | Java samples | TLS-visible | NDJSON nearest | Binding nearest | Lifetime/read nearest | Native samples |
|---|---:|---:|---:|---:|---:|---:|
| base | 1916 | 108 | 385 | 458 | 246 | 2526 |
| depth | 1991 | 55 | 432 | 560 | 221 | 2565 |
| serializer | 2014 | 93 | 423 | 471 | 274 | 2575 |
| stack | 1813 | 69 | 357 | 442 | 252 | 2509 |

Nearest work categories are exclusive; TLS visibility and the inclusive mechanism witnesses below overlap them. Never sum overlapping categories into an alleged time budget. Each named witness counts a matching sample once, not once per frame.

| Arm | Generic packed JSON | Typed packed JSON | Server snapshot/binary | Incoming ListBinding lookup | Workbench map adapter |
|---|---:|---:|---:|---:|---:|
| base | 345 | 0 | 85 | 5 | 53 |
| depth | 384 | 0 | 101 | 117 | 69 |
| serializer | 0 | 365 | 81 | 144 | 65 |
| stack | 0 | 308 | 77 | 98 | 47 |

## TLS/read-scope caller contrasts

Nearest caller assignment is exclusive within TLS-visible samples. Compare depth/base and stack/serializer for the depth factor, not disappearing helper names.

### base

- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead`: 23 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext`: 19 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer`: 15 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead`: 11 samples.
- `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext`: 6 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach`: 5 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeProbeDeadline.poll`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread`: 4 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.replay`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground`: 4 samples.

### depth

- `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer`: 14 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach`: 6 samples.
- `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.close`: 4 samples.
- `java.lang.System$1.getCarrierThreadLocal`: 3 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0`: 3 samples.
- `org.eclipse.rdf4j.common.iteration.CancellationSignal.current`: 3 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground`: 2 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeProbeDeadline.poll`: 2 samples.

### serializer

- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead`: 22 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer`: 16 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext`: 12 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead`: 11 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close`: 8 samples.
- `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext`: 6 samples.
- `org.eclipse.rdf4j.common.iteration.CancellationSignal.current`: 3 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.replay`: 3 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach`: 3 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.lambda$activateExternalValueInitializer$0`: 2 samples.

### stack

- `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer`: 20 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach`: 7 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext`: 6 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead`: 5 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.lambda$activateExternalValueInitializer$0`: 5 samples.
- `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead`: 4 samples.
- `org.eclipse.rdf4j.common.iteration.CancellationSignal.current`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeProbeDeadline.poll`: 2 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.close`: 2 samples.

## Node V8 main-isolate observations

These are whole-lifetime sampled leaf counts from 20 fresh recorded-request processes per arm, including idle/runtime frames. Missing native worker CPU prevents a full process CPU accounting.

### base

Total samples: 44593; profiles: 20.
- 27343 samples: ` @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/wide-isolation/client.mjs:1`.
- 3838 samples: `(idle) @ :0`.
- 3780 samples: `(garbage collector) @ :0`.
- 2500 samples: `processCallback @ node:zlib:530`.
- 1393 samples: `decodeUTF8 @ :0`.
- 743 samples: `decode @ node:internal/encoding:482`.
- 481 samples: `fetchParams.controller.resume @ node:internal/deps/undici/undici:13915`.
- 335 samples: `(program) @ :0`.
- 206 samples: `nextTick @ node:internal/process/task_queues:113`.
- 203 samples: `runMicrotasks @ :0`.
- 200 samples: `compileForInternalLoader @ node:internal/bootstrap/realm:383`.
- 182 samples: `processChunk @ node:zlib:510`.

### depth

Total samples: 45233; profiles: 20.
- 27626 samples: ` @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/wide-isolation/client.mjs:1`.
- 4058 samples: `(idle) @ :0`.
- 3789 samples: `(garbage collector) @ :0`.
- 2451 samples: `processCallback @ node:zlib:530`.
- 1395 samples: `decodeUTF8 @ :0`.
- 724 samples: `decode @ node:internal/encoding:482`.
- 419 samples: `fetchParams.controller.resume @ node:internal/deps/undici/undici:13915`.
- 333 samples: `(program) @ :0`.
- 270 samples: `nextTick @ node:internal/process/task_queues:113`.
- 252 samples: `compileForInternalLoader @ node:internal/bootstrap/realm:383`.
- 205 samples: `runMicrotasks @ :0`.
- 175 samples: `processChunk @ node:zlib:510`.

### serializer

Total samples: 45420; profiles: 20.
- 27781 samples: ` @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/wide-isolation/client.mjs:1`.
- 3960 samples: `(idle) @ :0`.
- 3700 samples: `(garbage collector) @ :0`.
- 2564 samples: `processCallback @ node:zlib:530`.
- 1349 samples: `decodeUTF8 @ :0`.
- 746 samples: `decode @ node:internal/encoding:482`.
- 498 samples: `fetchParams.controller.resume @ node:internal/deps/undici/undici:13915`.
- 320 samples: `(program) @ :0`.
- 239 samples: `runMicrotasks @ :0`.
- 231 samples: `compileForInternalLoader @ node:internal/bootstrap/realm:383`.
- 219 samples: `nextTick @ node:internal/process/task_queues:113`.
- 191 samples: `createAsyncIterator @ node:internal/streams/readable:1394`.

### stack

Total samples: 44701; profiles: 20.
- 27275 samples: ` @ file:///workspace/scratch/bfa8fbb86df5/rdf4j-threadlocal/experiments/threadlocal-audit/wide-isolation/client.mjs:1`.
- 3902 samples: `(idle) @ :0`.
- 3731 samples: `(garbage collector) @ :0`.
- 2495 samples: `processCallback @ node:zlib:530`.
- 1359 samples: `decodeUTF8 @ :0`.
- 742 samples: `decode @ node:internal/encoding:482`.
- 466 samples: `fetchParams.controller.resume @ node:internal/deps/undici/undici:13915`.
- 315 samples: `(program) @ :0`.
- 231 samples: `compileForInternalLoader @ node:internal/bootstrap/realm:383`.
- 222 samples: `nextTick @ node:internal/process/task_queues:113`.
- 209 samples: `processChunk @ node:zlib:510`.
- 197 samples: `runMicrotasks @ :0`.

## Allocation and downstream limits

JFR allocation class/path weights are statistical, not exact allocation bytes or retained heap. They may include setup/metrics and rare events with large weights. Per-arm path weights are retained below but cannot establish a GC mechanism. Socket/native sample counts do not establish blocking duration or backpressure. The shared client observes decompressed stream bytes while response Content-Encoding remains gzip. Pinned Jackson recycler is concurrent-deque based, not ThreadLocal.

- base: {"other_or_unresolved": 49898399360, "server_binary_writer": 320347168, "workbench_adapter": 2697722144, "workbench_page_serialization": 7661342104}
- depth: {"other_or_unresolved": 49425430920, "server_binary_writer": 225311448, "workbench_adapter": 2838459664, "workbench_page_serialization": 7554770336}
- serializer: {"other_or_unresolved": 45771274712, "server_binary_writer": 190365864, "workbench_adapter": 3164020520, "workbench_page_serialization": 6194881544}
- stack: {"other_or_unresolved": 49926519888, "server_binary_writer": 402483760, "workbench_adapter": 3050700400, "workbench_page_serialization": 7064855440}

## Decision boundary

The clean factorial established no wide-shape gain, no conclusive regression and no interaction benefit. Profiles can confirm dispatch or caller changes and identify remaining work, but cannot retrospectively explain the adverse fourth block or prove a conditional rollout safe. Keep the previously supported narrow completion improvements as separate evidence; do not pool them with wide outcomes. No new optimization, adaptive timing round or merge-readiness claim follows automatically.

## Client delimiter scanning deserves separate verification

V8 position ticks refine the anonymous client.mjs module frame. Line31 repeatedly searches carry.indexOf(newline) from offset zero; line33 parses the complete JSON record. These line ticks are sampled positions, not exact invocation counts or process CPU percentages.

| Arm | All Node samples | Delimiter scan line31 ticks | JSON.parse line33 ticks |
|---|---:|---:|---:|
| base | 44593 | 18340 | 8709 |
| depth | 45233 | 18682 | 8662 |
| serializer | 45420 | 18787 | 8695 |
| stack | 44701 | 18511 | 8538 |

The delimiter scan has about twice the JSON.parse position ticks in every arm. Source inspection shows accumulated partial rows records are rescanned after each chunk; this can be width/chunk-size sensitive. This is substantial shared benchmark-client work, not a demonstrated cause of between-arm differences and not evidence about the real browser frontend. The existing clean results remain measurements of that exact Node-consumed pipeline, not pure server throughput. Before attributing the inconclusive wide result to engine or serializer changes, a separately approved linear-scan client validation could test this mechanism while holding the client identical across arms and retaining exact stream/full-term correctness. Do not discard or relabel the original timing population.

### Exact benchmark-consumer loop and accumulator lifetime

This is the Node benchmark harness, not evidence that the real Workbench browser frontend uses the same parser. In client.mjs, carry is initialized outside the asynchronous response-body loop (line25). Each chunk is UTF8-decoded and concatenated into carry (line29). The newline loop calls carry.indexOf(newline) with no start offset (line31), slices off a complete line and retains the suffix (line32), then JSON.parse runs (line33). When no newline is present, the unconsumed partial line remains in carry for the next chunk, but the next search starts again at zero, rescanning its old prefix. Thus repeated delimiter searching is structurally distinct from the concatenation/UTF8 decode at line29. V8 position ticks favor line31; JIT attribution still does not give exact costs for indexOf versus concatenation/rope flattening. A scan-offset fix would address repeated searching, not prove all string-copying costs linear or disappear. Any replacement must preserve UTF8 streaming, partial lines, error records, EOF validation and first parsed-batch timestamp, and must be evaluated as a separately labeled harness version. No fix or new timing was performed in this published phase.
