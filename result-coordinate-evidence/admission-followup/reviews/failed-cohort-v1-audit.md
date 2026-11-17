# Why comparison-v1 stopped

The first JVM completed normally and all thirteen result, native-route, memo-lifecycle and ledger-endpoint checks inspected here passed. The cohort correctly stopped when its frozen adjacency gate failed. It remains invalid: **zero processes from comparison-v1 enter a performance estimate**, and its protocol, definitions, qualification, raw records and failure receipt are unchanged. This audit did not examine or calculate query latency or allocation measurements.

The failing gate required at least 98,309 adjacency lookup hits per query. Its qualification used three-query pilots. The full process instead recorded eight queries at 98,309 followed by five at 98,308, with ACTIVE/current adjacency, revision zero, one planner-statistics hit, zero exact misses and an empty fallback map throughout. Every query returned 32,768 rows, checksum `-392792955594352548`, and 131,072 projected values. Every query selected normal AUTO `ORDER / orderedFactorizedSort`, with one semantic native compilation, one chunk engagement, one factorized engagement and zero IR or generic-host execution counters. The ordinary arm opened no memo owner and released its ledger to zero.

The selected non-performance fields, raw hashes and source hashes are preserved in [failed-cohort-v1-audit.json](failed-cohort-v1-audit.json). The independent companion review is [ADJACENCY-CUTOVER-REVIEW-v1.md](ADJACENCY-CUTOVER-REVIEW-v1.md).

## Source-derived explanation

The bound is specific to this full-consumption four-predicate star: every row has a unique subject and exactly one object per subject/predicate. It also depends on the qualified active full adjacency snapshot, current source bytes and the observed ordered-factorized route.

| Operation | Source path | Adjacency lookup contribution |
|---|---|---:|
| Compile each constant-predicate pattern | `LmdbNativeAggregatePatternCompiler.compileStatementPattern` → `provablyEmptyByAdjacency` → `NativeSource.exactPredicateCount` → `LmdbDirectAdjacencyStore.tryCountInternal` | 4 |
| Evaluate the three projected tail branches for each unique subject | `LmdbNativeFactorizedRows.FlatCursor.next` → `TailBranch.collectEnum` → `PatternPlan.openRaw` → `RetainedNativeProbe.openArbitrated` | 3 × 32,768 |
| Open and exhaust the single flat-prefix root | `LmdbNativeChunkPipeline.ScanStage.fill` → `PatternPlan.openRaw` → `NativeSource.statementsArbitrated` | 1 when adjacency is selected; 0 when LMDB is selected |

The factorized split keeps the first star pattern in the flat prefix because it produces the subject used by later branches. Each remaining fresh object is projected and independent of later branches, making all three tails ENUM branches. Their unique subject keys prevent branch-result reuse from removing accesses.

For a bound child, adjacency's estimated work is one matching row, or 1.125 with a context filter. LMDB's estimate includes eight units of opening work and is at least nine. After the store's 0.75/1.25 interval expansion, adjacency still strictly dominates. `LmdbStatementAccessArbiter.RuntimeEvidence` therefore does not explore the alternative for these accesses. Each successful direct bound-row open records one lookup hit.

The compiler's exact-empty pruning is enabled by default. Each of the four patterns has a constant positive predicate and variable subject/object endpoints. It performs one exact predicate-count read and no constant-endpoint degree proof. The direct store's predicate-plane count records one lookup hit.

These operations establish the floor independently of the first process's measured count:

`(projectionWidth - 1) × fullRows + projectionWidth = 3 × 32,768 + 4 = 98,308`.

The root is different. Its two work intervals overlap: the scan visits 32,768 rows and LMDB's opening term is small relative to that scan. Runtime evidence is held by `LmdbSailStore` across query connections. `RuntimeEvidence.MIN_COMPLETIONS` is eight. Once the preferred variant has eight complete observations, the arbiter deliberately selects its unobserved alternative for up to eight trials, provided the structural intervals overlap. Comparative elapsed-time evidence cannot decide this branch until both variants have eight completions.

Thus the source predicts the observed single-hit change: queries 1–8 include an adjacency root open; queries 9–13 sample the native LMDB root alternative. A normally selected LMDB iterator is neither a generic-root fallback nor an adjacency failure. Its absence from the adjacency-hit counter is expected. `NamespaceAdjacencyObservation` simply subtracts cumulative metric snapshots and contains no warmup reset.

The per-operation attribution above follows the source and fixture structure. The raw harness records aggregate adjacency counts rather than individual inner-access selections; they are not a per-call access trace.

## Prospective correction

The replacement qualification uses nine fresh processes, one for each fixture/arm combination, each with the complete eight-warmup/five-query sequence and exact lexical validation. All 117 exact queries passed. Their complete thirteen-entry native-route/adjacency profiles match by ordinal across the three arms for each fixture, including the source-predicted `98,309 × 8` followed by `98,308 × 5` counts. Those pilots do not enter the timing cohort.

The new fixture definitions use the source-derived 98,308 floor. The immutable qualification receipt also seals the full per-ordinal route profiles. The additive [check_qualified_route_profile_v2.py](check_qualified_route_profile_v2.py) checks each completed, sealed JVM against those qualified profiles before performance results are accepted. It excludes resolver/materialization outcomes from its route key, preserving those outcomes as experimental measurements. The existing runner and analyzer remain unchanged and continue enforcing identical same-ordinal profiles across arms and rounds.

The eight-warmup/five-query design is retained. Its last five queries are **matched normal AUTO alternative-source learning observations**, not evidence of a converged post-learning steady state. Behavior after both root alternatives have accumulated eight observations is outside this comparison.
