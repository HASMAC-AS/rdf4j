# Read-resource holder implementation

1. completed: pristine incumbent source/class provenance, baseline semantic/memory tests and expected structural red.
2. completed: minimal static owner-free holder patch in isolated incumbent source only; candidate semantic/memory/worker/replay qualification over base and depth.
3. completed: exact store-family freeze and independent review; release to prespecified actual HTTP four-arm measurements. No publication.

Candidate replaces per-store current-frame ThreadLocal mutations with an owner-free static holder current field. Keep every frame/supplier/parent union and all entry/close boundaries. Closing-thread lookup, mismatch-before-closed and already-closed fast return remain exact. Four current-head read sites, two map mutation sites only. Two ThreadLocal gets remain per completed scope; no frame allocation eliminated.

Persistent cost: one holder plus ThreadLocalMap entry per touched live store/thread pair, potentially S*T; stale weak-key capacity can linger. Empty static holder must retain no store, Thread, consumer, supplier or lease. Dynamic supplier lists and empty-frame estimator ownership remain observable and must not be consolidated.

Current source contains two later cancellation forwarding additions. Preserve it untouched. Baseline-source is git HEAD LmdbSailStore (those additions excluded), candidate compiles with sourcepath empty against immutable runtime dependencies. Require baseline class-family comparison against actual frozen incumbent before using candidate. No whole current-store overlay.

Baseline provenance:104 pristine-source compiled family classes all byte-identical to frozen incumbent, baseline-class-comparison.json. Baseline source SHAa081ffe0b56beb6d52caff0f43c7fb8f42378b6b02ccd8b93ff1fc55d6c52258.
Baseline Maven Surefire:8tests,7semantic/memory passes,1expected structural failure,0errors/skips (red-report.txt). Same isolated pristine runtime:8found,7passed,1same structural failure,0errors/skips (red-isolated.log). Production edit follows these captured reports.
Candidate patch exists only in candidate-source/LmdbSailStore.java. Current worktree store remains untouched/preserved with later cancellation wrapper changes; candidate compilation uses immutable baseline dependencies and empty sourcepath. Four head read sites retained; only enter/close head-map mutations become fields.

## Qualified mechanism and current evidence

The isolated candidate source SHA-256 is `f08b5729fc034e7221bdb8618d330a819a4bd32b67e40bf3b457907418d04639`. All 104 baseline class files exactly match the frozen incumbent. The candidate contains 105 family classes, adding only `LmdbSailStore$ReadResourceState`. Full family overlay is required: compilation changes line tables/constant-pool references in other inner classes as well; this is not a claim that every changed class contains a behavioral edit. See `semantic.patch` for the complete source delta and `candidate-manifest.json` for every emitted hash.

Independent source/bytecode review approved: static holder only has `current` and no synthetic enclosing-store field; close resolves the closing thread's state, verifies identity before consuming the token, and preserves the already-closed no-op. Evidence: `independent-holder.javap.txt`.

Focused qualification: 8/8 pass on base+holder and depth+holder independently. Base+holder integration: 113 found, 110 passed, 3 intentionally disabled memo-probe cases, zero failures/aborts, exit 0. Depth+holder integration also completed with 110 passed and 3 gated skips; the separate enabled corrected replay fixtures passed 3/3 on both arms. All selected 113 cases therefore pass across the two execution modes, without treating the default suite as an all-enabled pass.

The collection test retains empty holders strongly while their stores and suppliers become weakly collectible on a still-live worker. It establishes no store/supplier owner retention after successful close, not zero allocation or an exact footprint. Misordered/foreign-close frames remain active until the rightful owner closes them, deliberately matching baseline. Persistent empty slots can delay unrelated stale ThreadLocalMap cleanup compared with remove; per-store/thread cardinality remains a cost to weigh against measured throughput/latency.
