# Workbench combination recovery and profiling

## Completed unprofiled combination matrix

All rows below are means of two fresh-server forks, same 8M-row/30-column query, same ready LMDB fixture, default 1M-row emitted window. Both measured runs per arm passed compiled IR, memo, grouped DISTINCT, zero spill, count and zero retained ledger checks. Times are seconds.

| Stack | Writer | Full completion mean (range) | First received rows | 1M window |
|---|---|---:|---:|---:|
| Prior query stack | Old | 107.671 (106.779–108.563) | 0.994 | 21.341 |
| Prior query stack | #6440 | 78.238 (75.516–80.960) | 1.049 | 23.014 |
| + lifetime refinements | Old | 107.460 (105.929–108.990) | 1.026 | 23.130 |
| + lifetime refinements | #6440 | 79.391 (76.169–82.612) | 1.026 | 23.255 |
| + lifetime + improved-hash compact snapshots | #6440 | 77.027 (75.612–78.442) | 1.028 | 22.785 |

The writer benefit remains substantial. The additional lifetime/snapshot combinations do **not demonstrate an additional HTTP latency improvement**: ranges overlap. Full-combination mean is only 1.55% below prior-stack + writer. The full combination is selected for diagnostic profiling to expose its remaining bottlenecks, not declared conclusively faster. No upstream-main HTTP arm was run; none of these is a main comparison.

## Recovery provenance

Original order: lifetime/old, lifetime/new, hash+lifetime/new, then interrupted hash mirror. Only the first three completed. Recovery ran hash+lifetime/new, lifetime/new, lifetime/old under the shared native-benchmark filesystem lock. The incomplete `hash-h-on` has no result and is excluded. Completed original results were not repeated or overwritten. Recovery mirrors have identical command and class fingerprints to their matching original arms. The interruption between halves is a limitation: this is not an uninterrupted ABCCBA session. No heavy concurrent builds/tests were scheduled during recovery; checkpoint publication was lightweight file/API work.

Evidence: `life-e-off`, `life-f-on`, `hash-g-on`, `hash-h-recovery-on`, `life-i-recovery-on`, `life-j-recovery-off` JSONs and manifests. Existing original writer ABBA remains in RESULTS.md.

## Measurement boundary

Completion means Workbench consumed/counted 8M rows, not delivery of all 8M to the client. Only 1M rows are emitted as NDJSON. First received rows is not DOM paint. No browser rendering or IndexedDB measurement. Writer pinned at fca5653c2da8df975fac5760b457c436f3e12dc6.

## Diagnostic profile

One diagnostic full-combination run completed in 75.298s, with matching route/count/no-spill/cleanup witnesses. A separate exact-value run checked 100 emitted tuples including Unicode, language, null, domain and uniqueness and consumed/count-checked all 8M rows. These are not additional timing forks.

JFR analysis filters to the client request interval (75.841s including client setup), excluding server startup. 6,014 Java execution samples were classified by nearest matching stack frame:

| Category | Java sample share |
|---|---:|
| Value lifetime | 19.67% |
| Projection/memo | 17.41% |
| Row snapshot | 14.95% |
| Query kernel | 14.92% |
| Binary writer | 14.03% |
| Transport value mapping | 5.99% |
| Binary parser | 5.69% |
| Workbench page writer | 4.07% |
| Other | 3.28% |

These are sampled Java execution shares, not wall-time percentages or removable fractions. Native-method samples were excluded because idle accept/poll calls dominate them. The profile covers the colocated Server + Workbench JVM, not Node-client CPU, browser rendering, or separate physical deployments. Sampled allocation weight is not exact allocated bytes.

**Actionable combination interaction:** all 344 Java samples whose stack includes `CompactBindingSetSnapshot.materialize` lead to `BNodeOriginTransport.map` as the first external RDF4J caller. This is 5.72% of all sampled Java execution, nested within the 14.95% snapshot category, not additive. The mapper iterates bindings even when it maps every value to itself. Compact snapshot iteration deliberately materializes mutable `MapBindingSet` storage, erasing part of the representation benefit on this real transport route.

Parser `readStringV2` accounts for 36.72% of sampled allocation weight (~37.52GB out of ~102.20GB weighted samples), but the parser is only 5.69% of Java execution samples. Allocation reduction alone is not proof of latency benefit.

**Next experiment:** preserve transport value mapping semantics while avoiding iterator-triggered snapshot materialization for unchanged rows. Must retain blank-node origin negotiation, RDF-star recursive mapping, mutable-row and callback semantics, binding order and value identity/lifetime. Compare against this exact combined baseline with focused tests and repeated actual HTTP trials. Do not remove lifecycle machinery globally. Memo/value still costs meaningful execution, but the evidence argues for fixing this cross-layer representation interaction before more admission-policy tuning on this already high-hit fixture.

Raw JFR remains local. Sanitized summary: `combined-profile-summary.txt`; analyzer: `AnalyzeHttp.java`.
