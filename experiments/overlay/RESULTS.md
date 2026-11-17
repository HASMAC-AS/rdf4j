# Overlay materialization experiment (in progress)

Source: HASMAC/rdf4j main `8fc7b0137164ffdc1f5d080b943fd362931137dd`, fresh checkout. JDK25.0.5, Maven3.9.11. Old query memo checkout is unavailable; no memo-on/off comparison has been performed. User private dataset/query not available: these are synthetic fixtures, not reproduced private workload numbers.

## What was actually tested

Default-off `rdf4j.lmdb.valueOverlay.typedMaterialization` prototype integrates typed overlay callback into **batch** ValueStore.resolveValues. Callback copies lexical bytes into bounded scratch, constructs ordinary owned Strings, preserves native fallback. Baseline12tests pass; prototype164tests pass covering ValueStore,overlay,snapshot and lexical forms.

Cold-shell micro times only batched resolution (not shell allocation/cache clearing/checksum),524,288 values per iteration,50k-term mix,overlay enabled. Four forks ABBA,12iterations each,discard4:

|Path|fork means ms|mean ms|
|---|---|---|
|Baseline serialized record|327.847,285.292|306.570|
|Typed callback v1|413.202,400.267|406.735|

Typed v1 ~33% slower; allocations729MB→642MB per iteration (~12% less). All checksums match. Do not enable this based on allocation savings.

Matched JFR2ms profiles (larger1,048,576value iterations,16iterations,final8) baseline679.24ms vs typed759.17ms (+11.8% under profiling). Sampling corroborates vector byte-at-time field lookup and token callback work; JIT/instrumentation alter measured magnitude. Raw profiles and reproducible parser retained.

## Profile-guided vector fix

VectorPageCodec.openRecord lacked bulk copy override. Default copy called byteAt for every lexical byte, doing binary field search and native metadata reads repeatedly. Added range-copy template once then decode each intersecting field once. Exhaustive partial-range test across digit/alphabet/raw cases plus existing page tests:23 green pre/post.

Matched typed-only ABBA v1 vs bulk-vector, same micro:

|Path|fork means ms|mean ms|
|---|---|---|
|Typed v1|339.823,329.305|334.564|
|Typed + vector bulk copy|322.226,306.981|314.604|

~6.0% improvement; equal checksum. These runs show drift relative to earlier tests, so comparing314.6 directly with earlier306.6 is not a matched baseline result. New baseline-vs-vector ABBA underway.

## Crucial end-to-end integration discovery

30column SELECT DISTINCT with OPTIONAL fanout and nested missing optional fixture:26,700triples,100k outputrows,compiled IR asserted,full lexical consumption and checksum. With cache1024, six-fork final5 means:
- overlay true/typed false:1200.923,1116.826ms
- overlay true/typed true:1218.802,1061.046ms
- overlay false/typed false:1142.214,1232.169ms

These numbers **do not measure typed improvement**. JFR shows query bypasses batch resolveValues and follows retained-reader scalar initialization. No OverlayMaterializer/visitRecord samples; allocations almost identical. Query profile hot spots are getLazyValue, IdentityHashMap tracking, captureReadyOwners,resolveWaves,ThreadLocal access,GroupKey equality, plus scalar decoding. Scalar typed integration must be implemented/tested/proven hit before a valid end-to-end comparison. This discovery was immediately reported rather than presenting timing variation as gain.

## Limits and next steps

Keep prototype default-off. No publication/merge. Finish scalar integration with preservation of completed-revision cache semantics,retained-reader lifetime,writer delta and fallback; profile hit proof; repeat matched end-to-end timings. No full8m-row benchmark yet. Batch-unique ID/lease amortization remains untested. Existing batch API already borrows once per batch; retained scalar path is a different integration problem. Token decode remains double-pass in both baseline and typed: count/copy alone does not prove regression cause.

### Matched baseline comparison after vector fix
Four frozen-class ABBA forks:baseline335.241/275.459ms vs typed+vector324.247/327.315ms. Means305.350 vs325.781ms (+6.7% slower typed), with substantial between-fork baseline drift. The vector improvement is real directionally in its isolated comparison but does not establish overall net benefit.

### Scalar integration and hit proof
The default-off scalar path now uses existing readTransaction+snapshot lease, preserves completed-revision cache publication,falls back on unsupported records/missing overlay and RDF-star,and guards reentrant datatype/namespace resolution. Scratch is bounded64KiB in existing thread context. Callback is static; store/target/revision/id cleared after decode, avoiding a ThreadLocal→inner callback→ValueStore retention cycle.

Matched query JFR now **does hit the prototype**:503 inclusive accept samples,344lexical,401visitRecord vs0baseline. Final5mean baseline1150.890ms vs typed1240.748ms (~7.8%slower under profiling). It remains slower, despite less per-query allocation. Broader165-test retry passed; initial attempt had missing manually bypassed lifecycle tempdir,not behavioral errors. Final unprofiled ABBA pending.

### Final unprofiled scalar query ABBA
Cache1024,100krows,30columns,compiled IR,overlayon,2warm+8measured/fork,final5means:
- baseline1180.696/1133.940ms; mean1157.318ms
- scalar+batch typed+vector1152.429/1139.918ms; mean1146.174ms
- difference ~0.96% faster,well within fork variability,not a meaningful demonstrated win.
- allocation baseline~1,564.61MB vs typed~1,550.50MB (~0.9% reduction).
- all output cardinalities/checksums and compiled route match.

Final validation:168 distinct broad tests passed plus23vector/page tests (some suites separate),zero behavioral failures. **Do not enable typed prototype**: cold decoder remains worse overall; end-to-end gain negligible/noisy. Vector bulk copy removes a verified inefficiency but is not the major query lever. Next highest-value investigation is repeated lazy-shell creation/readiness/owner tracking,then a genuinely distinct-ID batched scalar path if still hot. No claim about8m private query speedup.
