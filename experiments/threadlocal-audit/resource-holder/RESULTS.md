# Read-resource holder: correctness qualification

## Change and identity

Candidate source: `candidate-source/LmdbSailStore.java`, SHA-256 `f08b5729fc034e7221bdb8618d330a819a4bd32b67e40bf3b457907418d04639`.

The incumbent source was compiled with JDK 25 `--release 25 -g`, an empty source path, and the frozen prior-best classpath. All **104** emitted store-family classes match the actual incumbent byte for byte (`baseline-class-comparison.json`). This establishes the relevant source identity independently of branch labels. The candidate changes only the source delta in `semantic.patch`; it emits **105** family classes, adding the static `ReadResourceState`. The manifest records all hashes and changed family members. Some other class bytes change through debug line tables and constant-pool references; the full family must be overlaid consistently.

The shared working-tree store, including later cancellation forwarding, remains untouched. Neither that code, the unfavorable cancellation candidate, the earlier external-initializer experiment, nor the test-only memo observer seam is in this candidate family.

## Test-first evidence

| Configuration | Found | Passed | Failed | Skipped |
|---|---:|---:|---:|---:|
| Baseline Maven focused | 8 | 7 | 1 expected structural red | 0 |
| Frozen incumbent focused | 8 | 7 | 1 same structural red | 0 |
| Base + holder focused | 8 | 8 | 0 | 0 |
| Depth + holder focused | 8 | 8 | 0 | 0 |
| Base + holder integration, 11 classes | 113 | 110 | 0 | 3 |
| Depth + holder integration, 11 classes | 113 | 110 | 0 | 3 |

All candidate invocations above exited zero; no aborts. The baseline structural test observes `ThreadLocal.set` in scope entry, the exact expected optimization failure. The seven baseline semantic and memory cases pass before implementation. Focused tests are included in the larger integration count and must not be added again.

Coverage includes dynamic nested identity-unioned leases, an empty frame's estimator-admission ownership, null/throwing suppliers, recoverable wrong-order close, foreign-thread close rejection without consuming the owner frame, independent stores and reusable worker threads, collector-confirmed store/supplier release while the worker and empty state remain alive, context admission, replay exposure/checkpoint, map growth, worker replay, registration failure, shutdown/cancellation and repository materialization. The three integration skips are deliberately gated memo-replay cases, qualified separately using the repaired fixture below.

## Semantics and limits

This changes map mutations, not scope boundaries: two ThreadLocal gets and the context-frame allocation remain for each completed enter/close pair. Every dynamic supplier, frame, parent union and estimator presence distinction remains intact. Close resolves the executing thread's holder; mismatch precedes token consumption; an already closed token remains an immediate no-op. Independent source and bytecode review found no blocker (`independent-holder.javap.txt`).

An empty static holder retains no store, supplier, lease or enclosing-instance pointer. The GC test verifies this owner-release claim, not exact memory usage. Persistent cost is a holder and ThreadLocalMap entry for every touched store/thread pair, potentially S*T. Weak-key stale slots and unrelated stale-entry cleanup can persist longer than under remove. In-flight or improperly abandoned frames still retain owners as before; this patch deliberately does not repair misuse.

No performance claim is made from these tests. Actual HTTP four-arm timing is a separate experiment. This is selected affected-area qualification, not a full repository or full module pass.

## Separately enabled replay qualification

`run-gated.sh` runs the final single-snapshot observer fixture over candidate store classes and the exact frozen base/depth runtimes. Both `gated-a.log` and `gated-ad.log` report **3/3 passed, zero skipped/aborted/failed, exit 0**. These are the same three cases skipped in the default integration run, not three additional distinct tests. Thus all 113 selected cases passed for each candidate arm across the default and separately gated executions.

The fixture explicitly enables `rdf4j.lmdb.nativeValueMemo.testProbe`, `rdf4j.lmdb.nativeValueMemo.enabled`, and `rdf4j.lmdb.janinoCodegen.synchronous`. Logs prove two compiled `ROW:irKernel` bindings across replay, a populated memo barrier with one occupied entry before exposure, and grouping certificate rechecks after map growth. This qualification-only overlay contains the already-reviewed NativeValueMemo observer family and repaired test families. It is excluded from the timed candidate layer. Exact classpaths are retained in `gated-{a,ad}.classpath.txt`; final fixture hashes are in the existing `resource-restoration/grouped-fix/class-sha256.txt`.

CPU/build ownership was returned to the HTTP benchmark worker after both gated runs completed. Candidate source and classes are frozen; no benchmark or production source edits remain running.
