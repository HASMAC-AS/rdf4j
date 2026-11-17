# Lifetime simplification experiments

Recovered baseline: remote `4724600b10af6b044cc28154309352416d0660cf`, same combined production as earlier b8104ecf51. Fresh cloud workspace: old checkout, dependency cache, fixture and classes were lost. Restored JDK25.0.5+8, Maven3.9.11, full root quick install (125 modules) and deterministic fixture. Do not compare these timings directly to old environments or to main.

## Foreground activation only

`ValueResolutionScope.activateForeground` now captures only the previous ACTIVE scope and restores it with set. Original `activate(false)` additionally captured current thread, current reader and generation for a worker-release branch that cannot run. Worker activation, closed checks, generation checks, ownership, epoch retention and public-row preparation are unchanged. Independent source review found no blocker.

Pre/post identical selection: 17 tests, zero failures/errors/skips. Direct new characterization covers nested foreground/worker activations, restoring closed prior exposure, cross-store nesting and callback exception unwind. Its retireReaders call has no admitted native reader and is not itself lease-retirement proof. Broader selection: 228 reported, 225 executed, 3 explicit testProbe-property skips, zero failures/errors. Includes actual map growth, repository/raw callback exposure, worker context, cancellation, close races and frozen replay. This is not full-module/repository coverage. See test-summary.txt.

8M rows, 30 columns, 2.136M triples; no binary writer or HTTP. Best combined flags retained; 5GiB heap, 2.5GiB global ledger, 640MiB per query. Separate JVM per sample, no query warmup, shared validated fixture, exclusive benchmark lock. Fixture creation run excluded. Identical row checksum -3441391656945048864, irKernel bind, grouping and memo witnesses; zero spill and zero retained budget in each run.

| Order | Variant | Seconds | Query-thread allocation GB |
|---|---|---:|---:|
| A | baseline |39.887609|32.189122192|
| B | foreground |39.185862|31.992133616|
| B | foreground |40.746739|31.981079432|
| A | baseline |41.302468|32.345014272|

Mean 40.595039→39.966301 seconds (1.55% lower elapsed), allocation 32.267068232→31.986606524 GB (0.87% lower). Run ranges overlap; two forks per variant do **not** demonstrate a reliable speedup. Do not treat this as solving the sampled lifetime category.

An isolated 10M-activation loop in four alternating JVMs measured zero steady-state allocated bytes in both versions after warmup. It is a diagnostic, not query throughput; HotSpot can eliminate the activation object in that simple loop. Raw logs and JFR remain local. No JIT-level elimination claim is made for the full query.

## Next isolated trial

RepositoryValueIteration SOURCE_PULL is a plain ThreadLocal consumed only via identity comparison. Restoring null using set(null), rather than remove(), should retain no owner but may avoid repeatedly recreating ThreadLocal entries on foreground row pulls. Keep nested exact-owner restoration unchanged. Measure it separately from the foreground change and rerun exposure/cancellation/replay gates.

## SOURCE_PULL and direct combined comparison

The second isolated change restores `SOURCE_PULL` with `set(previous)` even for null. The null entry retains no iteration/store. Nested owner identity and the closed-owner check are unchanged. Previously each top-level hasNext/next pull removed the entry, causing a new entry at the next get/set. No admission, cancellation, transaction, replay or readiness checks were removed.

An incremental foreground-versus-both ABBA gave 41.026252/39.102723 versus 37.917598/38.530982 seconds: means 40.064488→38.224290 (4.59% lower elapsed) and 31.911448444→31.405135092 GB (1.59% less allocation). **Do not claim this as a stable speedup:** the subsequent direct comparison showed substantial variance.

Final direct original-best-baseline versus both simplifications, identical settings:

| Order | Variant | Seconds | Query-thread allocation GB |
|---|---|---:|---:|
| A | original best baseline |40.648823|32.178687448|
| B | both changes |38.471383|31.280421648|
| B | both changes |42.065038|31.279452048|
| A | original best baseline |41.245336|32.154870752|

Means 40.947080→40.268211 seconds (1.66% lower elapsed); overlapping ranges and large candidate-fork variation mean **no convincing latency improvement established**. Allocation 32.166779100→31.279936848 GB is ~886.8 MB/2.76% lower, consistent across the two final candidate forks. This compares against our best combined experimental code, not main. All 12 measured ABBA queries returned identical 8M rows/checksum, compiled irKernel/grouping/memo witnesses, zero spill, zero retained budget. Fixture preparation and diagnostic profiles are excluded from the means.

Final code safety: 84 focused tests pass; the broader 228-case selection reports 225 executed, 3 skips, zero failures/errors. All 3 skipped testProbe-gated cases were run separately with their required flags and pass: real compiled memo recreation across map growth, grouped DISTINCT rechecking across growth, caller context restoration on reopen failure. Counts overlap and should not be added blindly. Final broader rerun also passed. This is not a full-module/repository test run.

## Fresh diagnostic profiles

Single JFR runs (not the timing comparison): baseline 40.673s/32.202GB, combined 39.096s/31.333GB. 3,764 versus 3,621 main-thread CPU samples. Nearest-frame categories baseline→combined: projection/memo 27.34%→27.34%; lifetime 25.77%→25.88%; kernel 19.69%→20.91%; snapshot 10.41%→9.75%; decoding 1.49%→1.30%. Shares are not wall-time percentages or removable costs.

Weighted allocation samples support the intended mechanism: ThreadLocal.Entry 2.58→1.71GB; scope activation lambda 0.583→0.247GB. These are sampling estimates, not exact byte savings. Snapshot binding construction still accounts for ~50% of weighted allocation (MapBindingSet.addBinding plus MutableBindingSet.addBinding). Stable row snapshots remain the stronger next target; dropping ownership safeguards is not justified.

## Outcome and limits

Keep the two narrow simplifications as an experimental allocation cleanup, not a demonstrated throughput breakthrough or release/merge recommendation. Next investigate compact stable row snapshots while preserving value identity, retained-row validity, reused source containers and all repository lifetime boundaries. No writer/HTTP/end-to-end Workbench benchmark was run here.

Publication was paused after two approval denials; local commits are preserved and parent is resolving owner confirmation. No alternate connector route was attempted after the denial. GH-0000 is used because no issue number was supplied. Raw JFR and full test logs stay local; public-ready summaries contain only synthetic benchmark/source metrics.

Formatting: scoped check covers ValueResolutionScope, RepositoryValueIteration and ValueResolutionScopeActivationTest. The global copyright script reports pre-existing unrelated headers (none of these changed files); new files use the standard full header.
