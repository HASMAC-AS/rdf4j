# Prospective admission comparison, revision 2

This document describes the comparison before any accepted timing JVM is launched. The executable frozen protocol is `comparison-v2/protocol.json`, sealed by its SHA-256 sidecar. Qualification pilots, exact-oracle runs, direct-table traces, and natural-GC diagnostics are excluded from the latency dataset.

## Revision and full-sequence qualification

Revision 1 is permanently rejected and remains intact. Its first process exited successfully with all 13 result checks correct, but the validator rejected five queries whose adjacency lookup count was 98,308 rather than the 98,309 minimum inferred from short qualification. No latency or allocation values from that failed cohort were analyzed or used to select this revision. No sample is carried forward. The implementation, JVM settings, three fixtures, six-round schedule, eight warmups, five measurements and estimators remain unchanged.

Source inspection explains the transition. Four constant-predicate compiler emptiness proofs contribute four adjacency metadata hits. Three bound tail branches contribute one hit for each of 32,768 rows, giving a source-derived minimum of `(4 - 1) * 32,768 + 4 = 98,308`. The unbound root contributes one additional adjacency hit while it is the preferred source. The store-scoped statement access arbiter deliberately samples the LMDB alternative after eight completed preferred-source observations when its structural intervals overlap. A normal LMDB source choice is not an adjacency fallback. The outer normal `orderedFactorizedSort` route remains unchanged.

All nine fresh full-length exact-oracle pilots, 117 queries in total, reproduced the same complete route profile: the first eight queries have 98,309 adjacency hits, and the next five have 98,308; planner statistics record one hit, ACTIVE state and revisions agree, and exact misses and fallbacks remain zero. The new qualification receipt seals every ordinal's complete route/adjacency profile. The source attribution is supported by the code paths and this aggregate trace; the harness does not record an individual source label for every lookup.

After each sealed process, run the separately frozen `reviews/check_qualified_route_profile_v2.py` against the completed prefix. It must match the immutable qualified profile for each fixture and ordinal, in addition to all existing result, exposure, memory and cross-arm/cross-round checks. A profile failure stops this new cohort; it is never replaced. Require the full-profile check again before accepting or analyzing latency after all 54 processes complete.

Eight harness warmups do not finish the store-scoped source learner. Queries 9–13 fall within its alternative-source sampling phase. This is a comparison under matched normal AUTO history and routes, not an estimate of fully converged source selection. Determining performance after both alternatives have completed their learning budgets is a separate experiment.

## Question and arms

Does removing the first-8,192-probe low-hit cutoff recover enough later reuse to justify continued lookup and metadata costs? A separate negative case starts with repetition and then becomes unique. That case tests the cost of the current policy's failure to reconsider an already admitted table.

Every arm uses one identical compiled classpath and the same ownership implementation. `ordinary` disables the direct memo. `initial` enables weak retention with the existing one-time 25% cutoff. `always` enables weak retention and bypasses only that cutoff. Subsequent cumulative growth, direct-map hashing, full-mask and semantic identity checks, reservation refusal, source cache defaults and cleanup are identical. The entire feature remains off by default, and unspecified admission remains `initial`.

This is not a fixed-capacity comparison. Removing the cutoff allows later cumulative-growth decisions that the disabled control never reaches; those capacity changes are part of the measured causal effect and are reported explicitly. Neither enabled arm can recover from ledger refusal, closure, or an unsafe source by selecting `always`.

## Fixed fixtures and consumer

Each fixture has 131,072 projected opportunities, width four, 32,768 rows, and a 64-key repetitive set. Objects are 64-character IRIs. The unprojected unique row key supplies deterministic `ORDER BY` output. The independent Python oracle computes every expected query/recipe identity and checksum.

| Fixture | Prefix | Tail | Expected checksum |
| --- | --- | --- | --- |
| Early cold-hot | 4,096 unique keys | 126,976 opportunities cycling the first 64 keys | `-392792955594352548` |
| Late cold-hot | 32,768 unique keys | 98,304 opportunities cycling the first 64 keys | `-1128082570782671460` |
| Hot-unique | 8,192 opportunities cycling 64 keys | 122,880 new unique keys | `-7925248249943514788` |

The primary consumer reads every binding, requires a bound IRI and the expected projection width, calls `stringValue()`, and accumulates an ordinal-sensitive lexical checksum. It consumes the full result and closes the result and connection. Exact comparisons of keys and every padding character run separately during qualification. Primary commands explicitly set `trace=false` and `exact-oracle=false`; diagnostic work is not charged to primary timing or allocation.

The ordered result wrapper can retain consumed binding sets until close. These timings therefore do not measure a streaming consumer's loss of weak referents under collection. Large payloads and explicit retained consumers are studied separately, and those outcomes will not be pooled with these timings.

## Runtime and execution route

Use the restored, hash-verified Temurin 25.0.5+8-LTS distribution, 1,024 MiB fixed initial/maximum heap, G1, eight-byte object alignment and compressed object/class pointers. Native evaluation, direct adjacency and PREFER mode remain enabled; value overlay and forceSync remain disabled. Indexes are `spoc,ospc,psoc,posc`. The process ledger limit is 1 GiB and the per-query limit is 512 MiB. Coordinate transport is off in every arm. Source cache settings are unchanged. The store is generated identically from the fixed recipe, shut down, and reopened before querying.

AUTO remains authoritative. The complete-sequence qualification pilots selected the native `ORDER / orderedFactorizedSort / normal` route with one semantic native compile, one chunk/factorized engagement, and zero IR bind counters. Final fixture definitions must bind each fixture to its actually observed route; no strategy may be forced to fit an assumed tier. Both interpreted and compiled IR ownership are verified in the separate contract suite and are not relabeled as the execution route of these ordered queries.

Before freezing, require exact lexical-oracle qualification for every fixture/arm, matching independent checksums and recipe hashes, one opened/closed owner per enabled query, zero owners in ordinary, zero ledger endpoints and no generic fallback. The short primitive trace additionally reconciles each projected ID/mask and per-row source/materialization change with an independent replay of the direct-map algorithm. Fixed outcome gates must not require a desired hit rate, capacity, source-call reduction or speedup.

## Replication and order

Run 54 fresh JVMs: six rounds times three fixtures times three arms. Each JVM executes eight warmup queries followed by five measured queries, giving 702 total queries, 432 warmups and 270 measurements. The six arm orders enumerate every permutation of ordinary, initial and always. Fixture index orders are fixed in advance as `012, 021, 102, 120, 210, 201`, so each fixture appears in each position twice. This schedule was selected from the design alone, before observing any primary timing.

One `next` invocation launches at most one JVM. The coordinator must observe its terminal status and validation before launching the next. No concurrent build, test, diagnostic, profiler, archive compression, or other heavy analysis runs during the timing cohort. Root controls the Java slot. Every command has a five-minute deadline, a durable claim, full input hashes before and after execution, process status, raw outputs and a completion seal. A failed, partial, changed or unclaimed slot permanently stops the cohort; do not insert a replacement. Any revised attempt gets a new complete protocol and fresh controls.

## Measurements and estimators

Measure connection, preparation, evaluate, first-row, prefix/tail consumption, close and total latency; all-Java-thread and caller-thread allocation; process-ledger peak and zero endpoints; memo reservation peak; actual routes and adjacency observations; owner lifecycle and all memo counters; and result checksum. The active query ledger's separate peak is unavailable and must remain explicitly null. Reservation peaks do not represent complete Java/native heap accounting. GC logs cover the whole JVM, including fixture setup and warmup; do not label them per-query pauses or add overlapping concurrent-cycle durations to pauses.

For each fixture and metric, reduce the five measured queries to a per-JVM median. Compare arms within the same round. The primary descriptive estimate is the median of the six paired process ratios, retaining all six ratios and their range. The three comparisons are always/initial, initial/ordinary and always/ordinary. Repeated queries in one JVM are not independent process replicates.

Always report the prespecified secondary sensitivity: replace each per-JVM median with the mean of those same five queries, then take the median of the six paired ratios. Flag a change in latency direction. This sensitivity is prospective for this cohort. It does not replace the primary estimator or discard inconvenient queries. There is no significance claim or automatic promotion threshold with six process pairs.

Cold first-query observations, preparation costs, allocations, reservation peaks, cutoff/growth events and whole-JVM natural GC provide context. A cleared weak slot is not by itself an additional useful-key miss; reconcile hit and source-call changes before interpreting it. Historical benchmark results are context only and are never pooled with this cohort.

## Reproduction

The runner's `freeze --help` documents the exact fixture and qualification schemas. The final revision-2 qualification receipt binds fixture definitions, classpath file, complete runtime inputs, Java executable and JDK inputs by hash. Source inputs include the two runner files, the complete experimental implementation, the final harness sources, this protocol, the exact route-profile checker, and the independent fixture oracle. Mutable reports and reviews are outside those frozen source trees.

After a qualified protocol is frozen and checkpointed, invoke from the repository root:

```bash
python3 result-coordinate-evidence/admission-followup/benchmark/run_comparison.py next \
  --cohort result-coordinate-evidence/admission-followup/comparison-v2 --slot-released
```

Immediately after each successful `next`, run:

```bash
python3 result-coordinate-evidence/admission-followup/reviews/check_qualified_route_profile_v2.py \
  --cohort result-coordinate-evidence/admission-followup/comparison-v2 \
  --allow-incomplete --output FRESH_PROFILE_RECEIPT.json
```

Repeat only after observing both commands' successful terminal results. Once all 54 processes have sealed, repeat the route-profile check without `--allow-incomplete`, then run `audit` and independently recompute the paired estimates from the raw samples. Preserve every output and publish complete code and the implementation patch with the findings.
