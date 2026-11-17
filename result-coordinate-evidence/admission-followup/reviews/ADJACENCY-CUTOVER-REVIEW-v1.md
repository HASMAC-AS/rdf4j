# Adjacency counter cutover review

The stopped first JVM and all nine fresh, exact-oracle qualification JVMs agree on the same route sequence: the first eight queries report 98,309 adjacency lookup hits, then the next five report 98,308. The source contains a normal AUTO learning transition at exactly eight completed traversals that explains this change. The harness does not execute a different query path at its warmup/measurement label boundary.

This is a read-only investigation. No Java, build, source edit, protocol edit, or latency/allocation analysis was performed. The original comparison-v1 rejection remains valid evidence of an overly specific short-pilot gate; this review does not accept that run for timing.

## Raw observations

The stopped run contains 13 valid queries with 32,768 result rows and the expected checksum in every query. All 13 release the query ledger. Its native route is consistently normal AUTO `orderedFactorizedSort`, with one semantic native compilation, one chunk engagement, one factorized engagement, zero hosted-generic/hard-decline counters, and zero IR binds/rows. The complete adjacency map remains ACTIVE at the same applied/current revision, with plannerStatsHits=1, exactMisses=0, and no fallbacks. Only lookupHits changes between query eight and query nine.

The nine completed `benchmark/qualification/{early,late,unique}-{ordinary,initial,always}-v3` attempts independently reproduce the entire 13-query signature, not merely the minimum count. Their 117 queries pass the exact ordered lexical oracle and ledger checks. `adjacency-cutover-audit-v1.json` records each raw file's SHA-256 and the non-timing signatures compared.

## What the harness actually captures

`PhaseMemoQueryHarness.run` reads `NamespaceAdjacencyObservation.snapshot` before opening the repository connection and calling `prepareTupleQuery`, then reads the ending snapshot after evaluation, full consumption, and connection/result closure. Consequently this delta includes compiler/planner metadata operations as well as execution. It does not exclude preparation. The only explicit per-query reset in the harness is the ledger peak reset. `iteration < 0` determines the emitted stage label; it does not branch evaluation, exact-oracle behavior, or adjacency accounting.

`NamespaceAdjacencyObservation` snapshots and subtracts store metrics. `LmdbAdjacencyMetrics.lookupHits` is a cumulative LongAdder: recordHit increments it and snapshot sums it. There is no per-query or warmup-boundary reset of that counter.

## Source-grounded explanation

`LmdbSailStore` owns one `LmdbStatementAccessArbiter.RuntimeEvidence` for the reopened store. It outlives individual query connections. Its variant key includes binding shape, source, order/range/existence classes and traversal type; the evidence is scoped to the adjacency publication regime.

`RuntimeEvidence.MIN_COMPLETIONS` is eight. Before both candidate variants have eight completions, structural estimates normally choose the preferred source. When those estimates overlap, eight completed preferred-source traversals enable up to eight decisions sampling the other source. That transition does not wait for a harness stage label. Evidence is recorded when a measured iterator is exhausted and closed, while early-closed iterators are censored.

For this star fixture the unbound, single-predicate root scan returns 32,768 rows. Its adjacency and LMDB work intervals overlap: LMDB's fixed startup term is small relative to the root cardinality. The first eight completed root scans therefore use the preferred adjacency source; the subsequent queries sample LMDB. Choosing LMDB omits exactly the adjacency root-open hit and does not report an adjacency fallback. The three bound child patterns each match one statement per root; their structural adjacency work is far below LMDB's fixed startup cost, so they do not have this overlapping-estimate exploration case.

The fixed four hits have a separate compiler path. `LmdbNativeAggregatePatternCompiler.compileStatementPattern` calls `provablyEmptyByAdjacency`, which asks `source.exactPredicateCount` once for each constant predicate. The direct store's `tryCountInternal` answers from `planeStatistics.quadCount` and increments lookupHits. All four fixture predicates are constant; both endpoints are variable. Thus the source and observed counts support:

| Queries | Bound child hits | Exact-predicate compile metadata | Root scan adjacency hit | Total |
|---|---:|---:|---:|---:|
| 1–8 | 3 × 32,768 | 4 | 1 | 98,309 |
| 9–13 | 3 × 32,768 | 4 | 0 | 98,308 |

This is source-grounded attribution corroborated by the exact boundary and the cross-arm pilot signatures. A per-call trace was not added to the frozen harness, so the aggregate metric should not be advertised as separately measured planning and execution counts.

## Recommended gate and interpretation

Keep comparison-v1 and its original protocol frozen as a stopped, rejected attempt. Qualify a fresh protocol from the complete 13-query exact-oracle pilots, retaining the exact per-ordinal adjacency signature together with result/ledger/strategy/counter checks. A minimum of 98,308 is compatible with the observations but provides less evidence on its own than the complete signature. The existing analyzer also compares full route signatures across arms and rounds; retain that check rather than stripping adjacency counts from it.

Do not force adjacency or disable the learner to restore the short-pilot count. Normal AUTO is part of the requested experimental setting. Describe this matched history accurately: eight harness warmups leave the root-source learner in its deliberate alternative-sampling phase for queries nine through thirteen. These measurements are not evidence of fully converged learned-source steady state. A study of the later learned choice needs a separate prospective protocol because both variants can be warmed only after further traversals, and then the choice may depend on measured evidence.

Finally, lookupHits is not an emitted-row counter and is not exclusively an execution-phase counter. The four exact-predicate planning metadata hits explain why a positive count alone cannot establish how every statement access was served.
