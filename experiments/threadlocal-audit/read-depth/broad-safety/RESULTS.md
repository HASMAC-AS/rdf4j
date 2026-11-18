# Depth-only broader correctness qualification

**1,705 tests passed, 3 gated tests skipped, zero failures or aborts across 107 selected classes.** This is broader selected correctness coverage, not a complete module/repository pass and not a throughput claim.

## Exact runtime

Prior-best baseline A is the frozen runtime associated with `5ac89b5`; candidate AD adds only the read-depth holder family associated with `0750336e`. All launches used the original `read-depth/cp_ad.txt`, not a fresh build from the dirty/current checkout.

Every log reports code origins: LmdbStoreConnection from `read-depth/layer`; QueryExecutionContext from the original HTTP dependency; LmdbRecordIterator, ValueStore and NativeValueMemo from immutable baseline classes. No resource null-restoration, slower record-cancellation candidate, test-only grouped replay seam, or current production target/classes was added. At the end, all 6,978 runtime manifest entries matched their original hashes. Test-family hashes and launcher hashes are also preserved.

LMDB tests came from the frozen historical compiled test bundle. Selection used matching source names present in that bundle; it did not silently replace those classes with newer tests. Three affected HTTP test families were separately frozen and hashed, and ran against unchanged HTTP production classes.

## Results

| Batch | Classes | Passed | Skipped | Failed |
|---|---:|---:|---:|---:|
| Scope/lifecycle 01 | 20 | 387 | 3 | 0 |
| Scope/lifecycle 02 | 20 | 263 | 0 | 0 |
| Scope/lifecycle 03 | 20 | 379 | 0 | 0 |
| Scope/lifecycle 04 | 16 | 352 | 0 | 0 |
| HTTP context | 3 | 12 | 0 | 0 |
| Native evaluation 01 | 20 | 261 | 0 | 0 |
| Native evaluation 02 | 8 | 51 | 0 | 0 |
| **Total** | **107** | **1,705** | **3** | **0** |

The scope batches cover adaptive/frozen/spill replay; adjacency contexts, lifecycle and snapshots; map growth; read admission; snapshot and writer value lifetimes; cleanup failures; shutdown/cancellation; repository value materialization; isolation/transactions; value caches, codecs, revisions and maintenance. Native batches add coordinator/group/indexed/morsel/worker cancellation, backpressure, sampled/bounded materialization, runtime interner lifetime, worker contexts, lazy/lexical/result replay and ordinary compiled-kernel cancellation. HTTP tests cover external initializer identity/nesting, replay worker scopes and request context.

All selected batches completed with zero failing classes. No observed failure required a matched baseline rerun. Expected fault-injection diagnostics in logs belong to tests whose assertions passed; they are not suppressed failures. Classes run sequentially inside fresh 20-class JVM batches, with the existing Mockito javaagent and a 4 GiB heap. Existing test deadlines/gates were unchanged; a 12-minute outer batch bound was never reached. The canonical CPU lock prevented overlap with builds/timing/profiling.

## Limits and exclusions

The three skips are the default `nativeValueMemo.testProbe`-gated map-growth replay tests. They were not silently enabled or supplied a repaired memo seam here. Separately published gated-fixture evidence remains separate from this qualification.

Current source classes absent from the historical test bundle were explicitly excluded: RepositorySourcePullScopeTest, ValueStoreNativeValueCodecTest, ValueStoreWriterGuardTest and NativeRecordCancellationTest. Prior focused source-pull/writer-guard checks remain separate evidence; the later record-cancellation candidate is deliberately not in this runtime.

This phase did not run all 419 inventoried native-evaluation classes, all 263 queryalgebra-evaluation compiled classes, all LMDB tests, or the repository suite. No assertions, production source, property gates or runtime layers were changed to obtain these results. No publication was performed by this worker.

## Evidence

`final-summary.json` contains per-batch counts and test durations. Logs: `ad-01.log` through `ad-04.log`, `ad-http.log`, `ad-native-01.log`, `ad-native-02.log`. Selections, exclusions, exact test class hashes, code-origin witnesses and final runtime re-verification accompany them. `PARTIAL-RESULTS.md` preserves the staged execution history; this report supersedes its pending-coverage notes for the completed 28-class native subset.
