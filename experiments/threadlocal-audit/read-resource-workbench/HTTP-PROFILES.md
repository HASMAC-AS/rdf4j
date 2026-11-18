# Separate actual HTTP profiles

Four fresh JVMs, one per arm, each with 60 warmups then 30 recorded requests. All 360 requests passed route/count/no-spill/cleanup witnesses. JFR recording and JSON export both use stack depth 128. Clean timing results are unchanged and no profile duration is pooled into them.

Java execution samples cover all server and Workbench JVM threads. Node CPU is absent; inter-request client setup and metrics are inside the recording window. Native-method samples are separate. Counts below are statistical sample visibility, not exact calls, instructions or wall-time shares. One profile per arm cannot establish a stable difference.

| Arm | Java samples | TLS-visible samples | Native samples | Sampled allocation weight GB |
|---|---:|---:|---:|---:|
| base | 1007 | 119 | 924 | 25.72 |
| depth | 1019 | 115 | 1030 | 25.22 |
| resource | 959 | 112 | 961 | 25.22 |
| both | 870 | 82 | 935 | 25.77 |

## ThreadLocal map operations

These are inclusive sampled frames, which overlap within a sample. Other ThreadLocals still mutate the map; the holder patch does not remove all map operations.

| Arm | ThreadLocal.set | ThreadLocal.remove | Map.set | Map.remove |
|---|---:|---:|---:|---:|
| base | 15 | 17 | 24 | 17 |
| depth | 25 | 9 | 34 | 8 |
| resource | 19 | 18 | 30 | 18 |
| both | 20 | 3 | 27 | 3 |

## Nearest ThreadLocal callers

### base

- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead`: 23 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer`: 21 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead`: 16 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext`: 16 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread`: 7 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach`: 5 samples.
- `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext`: 5 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init>`: 4 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.lambda$activateExternalValueInitializer$0`: 4 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.replay`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close`: 3 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.setExternalValueInitializer`: 3 samples.

### depth

- `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer`: 22 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach`: 17 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext`: 17 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead`: 11 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close`: 8 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead`: 8 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground`: 6 samples.
- `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext`: 4 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.setExternalValueInitializer`: 3 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeProbeDeadline.poll`: 3 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0`: 3 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.close`: 3 samples.

### resource

- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead`: 22 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead`: 21 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach`: 12 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer`: 11 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground`: 6 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.setExternalValueInitializer`: 6 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close`: 5 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activeExposure`: 5 samples.
- `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0`: 4 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.lambda$activateExternalValueInitializer$0`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init>`: 3 samples.

### both

- `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext`: 15 samples.
- `org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer`: 14 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach`: 13 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close`: 9 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead`: 5 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground`: 5 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead`: 4 samples.
- `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.lambda$activateForeground$0`: 3 samples.
- `org.eclipse.rdf4j.common.iteration.CancellationSignal.current`: 3 samples.
- `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread`: 2 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeProbeDeadline.poll`: 2 samples.
- `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init>`: 2 samples.

## Interpretation and limits

Source-level behavior is clear: read-resource enter/close now updates a field in an owner-free per-thread holder rather than setting/removing the ThreadLocal map entry for each frame. It still performs ThreadLocal lookup and creates the resource frame. The static holder retains a small empty slot per thread/store key; correctness tests require its current frame to clear, so this is not permission to retain the store or finished frame. Sampled allocations do not validate retained-heap behavior.

The clean factorial found no demonstrated HTTP benefit, so reduced map mutation cannot be presented as saved end-to-end time. Remaining initialization, tracked/observed read scopes, fused attach, binary/NDJSON serialization and binding materialization must be judged by actual-path profiles and independently tested changes, not by disappearing helper names. Do not re-promote the already-inconclusive initializer null-slot tweak simply because its callers remain sampled.

Sample allocation weights are approximate and can be dominated by rare stack/class events; they are neither exact bytes nor live heap. No per-query allocation or GC causality is inferred. Raw JFR and large full-stack JSON remain local; compact summaries preserve all category counts and top sampled stacks. No new optimization or extra timing round follows automatically.

## Nearest sampled work categories

Categories are exclusive by nearest matching frame; they do not assign wall time.

### base

{"binary_parser": 12, "binary_writer": 65, "cancellation": 93, "container_jdk_other": 140, "kernel": 128, "lifetime_read_replay": 257, "logging": 2, "memo_values": 40, "snapshot_binding": 118, "transport_mapping": 21, "workbench_ndjson": 131}

### depth

{"binary_parser": 15, "binary_writer": 64, "cancellation": 79, "container_jdk_other": 117, "kernel": 149, "lifetime_read_replay": 259, "logging": 6, "memo_values": 52, "snapshot_binding": 121, "transport_mapping": 6, "workbench_ndjson": 151}

### resource

{"binary_parser": 8, "binary_writer": 55, "cancellation": 66, "container_jdk_other": 104, "kernel": 118, "lifetime_read_replay": 237, "logging": 1, "memo_values": 39, "snapshot_binding": 142, "transport_mapping": 6, "workbench_ndjson": 183}

### both

{"binary_parser": 9, "binary_writer": 62, "cancellation": 73, "container_jdk_other": 103, "kernel": 99, "lifetime_read_replay": 198, "logging": 3, "memo_values": 56, "snapshot_binding": 136, "transport_mapping": 15, "workbench_ndjson": 116}
