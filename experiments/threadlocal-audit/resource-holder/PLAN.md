# Read-resource holder experiment

## Goal and evidence
The actual HTTP profile at archival checkpoint `3fe0ab78605b5e3fbb159e34ace30bb3fe6a8859` ranks read-resource entry/restoration among the strongest remaining ThreadLocal callers. This is an investigation candidate, not proof of removable wall time. The prior external-initializer set(null) experiment was inconclusive and is excluded. Owned record cancellation was slower and is excluded.

## Bounded design
Replace the per-store ThreadLocal resource-frame head with a static owner-free holder containing only a mutable head. Keep every frame, dynamic supplier, parent union, null-versus-empty context distinction and entry/exit boundary. Each scope entry resolves the current thread holder; each first close resolves the CLOSING thread holder and preserves nesting check before consuming the token. No cached activating-thread holder in escaping close. Retain idempotent completed close and exact out-of-order recovery behavior.

Expected source effect: avoid repeated ThreadLocal map set/remove mutations, retaining current-thread gets and using holder field writes. Extra cost: one owner-free holder and retained entry per touched store/thread; empty head must not retain the store/supplier/leases. Stale entries/map capacity can persist; no unmeasured retained-heap claim. OOM failure ordering may differ and must be disclosed if applicable.

## Runtime identities and source isolation
Base is immutable `5ac89b5c137030fea1aa6aa7160984423d7f4cd5`; depth adds only `0750336e71ab0ca0d216ce9c36483a6e9063337d`. Reconstruct candidate store family from exact incumbent source, NOT current source containing later cancellation changes. Compile and compare exact class-family manifests. New source and combined identities will be published independently from archival branch.

## Plan
- [in_progress] Establish baseline contracts and bounded implementation
- [todo] Independently review scope and retention correctness
- [todo] Qualify exact layered routes and outputs
- [todo] Measure prespecified factorial actual HTTP pipeline
- [todo] Publish complete evidence and qualified decision

## Ownership and coordination
writer_guard_design owns baseline tests, implementation and first CPU/build window. native_token_path independently reviews source/memory/thread semantics. inventory owns runtime provenance and HTTP factorial after qualification. Coordinator reviews, integrates and publishes. Canonical experiments/overlay/native-benchmark.lock excludes timing/build/test overlap. No blanket staging/reset or lost artifacts. Test-first required before production edits; keep baseline failures and final results.

## Timing protocol
Four balanced Latin-square blocks, four arms: base, depth, holder, depth+holder. Four fresh server JVMs per arm. Fixed 60 warmups and 30 measured full 100k-row requests per JVM, improving on prior 20/10 convergence limitation without outcome-driven trimming or reruns. Primary incremental contrast: holder+depth versus depth. Report all arm contrasts and factorial interaction using independent fork means, not requests as independent samples. Preserve early/middle/late windows as convergence diagnostics. Same actual Server→binary→Workbench→NDJSON frozen common runtime and writer/compact transport flags. Exact full-term validation before timing; route/memo/no-spill/budget witnesses for every request. Browser excluded after prior pre-navigation Unix-socket denial; no paint/IndexedDB claim. No causal or universally-fastest claim from profiles.
