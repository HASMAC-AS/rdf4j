# Fixed-capacity versus adaptive native Value memo

This is the prospective protocol for a new, finite growth experiment. The preceding admission study and its observations remain immutable. No earlier timing, exact pilot, diagnostic or incomplete cohort becomes a primary observation here. Freeze the executable protocol and checkpoint the final qualified sources before launching the first comparison JVM.

## Question and treatment

The primary question is whether keeping the always-admitted weak memo at its initial 4,096 slots improves end-to-end query latency relative to cumulative adaptive growth. The preceding study found that a late 64-key repeating tail already reused successfully before four resizes. Equality of useful hits is a hypothesis to test, not an acceptance gate that discards an unfavorable fixed-capacity outcome.

The three arms are `ordinary`, `adaptive` and `fixed`. All explicitly select `rdf4j.lmdb.nativeValueMemo.admission=always`, `rdf4j.lmdb.nativeValueMemo.retention=weak` and `rdf4j.lmdb.resultCoordinateTransport=off`. Ordinary disables `rdf4j.lmdb.nativeValueMemo.enabled`; both enabled arms set it true. Ordinary and adaptive select `rdf4j.lmdb.nativeValueMemo.growth=adaptive`; fixed selects `fixed`. All arms run identical classpath bytes. Fixed skips table growth, retains the 4,096-slot initial capacity, and preserves initial reservation refusal, keys, hashing, identity checks, weak ownership and cleanup. It does not simulate fixed capacity by forcing a refused growth reservation, because refusal would disable the memo.

The feature remains off by default. This experiment does not select a production policy or introduce a new runtime dependency.

## Prospectively selected fixtures

Every fixture has 32,768 rows, four projected columns, 131,072 projected opportunities and 64-character IRI payloads. A hidden unique row key orders the result. The consumer reads every projected Value and lexical string and verifies the independently computed, row-position-sensitive checksum. The row key does not enter the memo. Each process populates its store deterministically, shuts it down, and reopens it with default source caches before querying.

| Fixture | Pattern | Prefix opportunities | Tail opportunities | Repeating keys | Purpose |
| --- | --- | ---: | ---: | ---: | --- |
| `early-cold-hot` | `cold-hot` | 4,096 unique | 126,976 | 64 | Equal-behavior/no-growth control |
| `late-cold-hot` | `cold-hot` | 32,768 unique | 98,304 | 64 | Primary avoidable-growth hypothesis |
| `warm-cycle-8192` | `warm-cycle` | 8,192 cycling 64 bootstrap keys | 122,880 | 8,192 disjoint tail keys | Larger-set counterexample/control |

The existing Java fixture and Python oracle already implement both patterns. `fixture-oracles-v1.json` contains all explicit dimensions, expected counts, checksums, exact query digests and insertion-recipe digests. The independent qualification builder reconstructs the checksums and recipes again using complete short strings and scalar Java-hash arithmetic. Exact pilot consumption additionally compares every returned lexical string, key and padding character.

## Runtime and process schedule

Use the pinned Temurin 25.0.5+8-LTS runtime and the same fixed heap/collector settings as the preceding study: `-Xms1024m -Xmx1024m -XX:+UseG1GC -XX:ObjectAlignmentInBytes=8 -XX:+UseCompressedOops -XX:+UseCompressedClassPointers`. Hot counters are enabled. The query ledger limit is 536,870,912 bytes and the process-authority limit is 1,073,741,824 bytes. Native evaluation and direct adjacency are enabled; direct adjacency remains PREFER; forceSync and value overlay remain disabled. Indexes are `spoc,ospc,psoc,posc`. Source-cache settings are unchanged.

Six rounds use all six permutations of the three arms. For fixture declaration order early, late, warm-cycle, the six fixture orders are `012`, `021`, `102`, `120`, `210`, `201`. Every fixture occupies every order position twice. The exact assignment is inherited from the previously declared balanced schedule and is selected before timing.

Each of the 54 fresh JVMs executes eight warmups followed by five measurements: 702 total queries, 432 warmups and 270 measurements. Every query is checked. Only the last five contribute to performance estimates. A JVM runs one fixture/arm and receives no other query shape. The coordinator launches one JVM per supervised `next` invocation, observes its terminal result, and checks its qualified route profile before launching another. No build, test, profiler, diagnostic, compression or heavy analysis runs concurrently with a timing JVM. No explicit GC is requested. Each slot has a fixed 300-second deadline unless a different positive deadline is frozen before any timing process.

## Correctness, source identity and route qualification

Before freezing, run every selected default, weak, adaptive and fixed ownership group against the final memo bytes, including the complete new growth contracts, all existing admission contracts and both IR lifetime tiers. `qualify_runtime.py` reads the fresh retained Surefire XML, case lists, results and input manifests, and checks that the selected final memo classes are byte-identical to the classes tested. Counts are derived from those receipts; an old qualification flag or a historical count is not authority.

Run nine fresh exact pilots, one per fixture/arm, with the full eight-plus-five sequence and the same statistics capacity thirteen. The builder verifies their commands, flags, classpath, raw seals, lifecycle, results, projection exposure, phase-counter reconciliation and actual execution routes. Their measurements are excluded from the timing cohort and are not analyzed to choose the treatment or fixtures.

Normal AUTO is authoritative. No strategy is forced to obtain a convenient route. The source-derived prediction for these four-column stars is 98,309 adjacency lookup hits on queries 1-8 and 98,308 on queries 9-13: three bound child probes per row plus four compiler predicate-count hits, with the additional root adjacency lookup before the statement-access learner begins sampling the LMDB alternative. All nine fresh pilots must reproduce the profile, with normal ordered-factorized execution and no generic fallback. The qualification records the complete observed per-query route/adjacency profile and its digest. If the prediction or cross-arm matching fails, retain the evidence and investigate before creating any timing cohort.

The measured queries therefore cover a matched AUTO alternative-source learning phase, not converged source selection. A longer warmup schedule would be a separate design requiring its own full-history qualification. The separately tested compiled/interpreted IR paths must not be described as the actual timed route when AUTO chooses ordered-factorized execution.

Before and after every timing JVM, verify all frozen source, classpath, JDK module/configuration/shared-library and environment identities. Every sample must have the expected row count/checksum, native projected-row count, full projected exposure, owner count, valid identity handling, nonforced route and zero ledger endpoints. Fixed used tables must stay at 4,096 slots with zero resizes. Both enabled modes use always admission and cannot report a low-hit cutoff. Reservation accounting and overlap must reconcile with the selected policy. Hit rates, source-call counts, adaptive capacities and latency are experimental outcomes, not favorable-result gates.

The complete qualified profile is checked after each sealed process and again for the full cohort using `check_route_profile.py`. That checker never computes timing or allocation summaries. A changed profile must be rejected even if a synthetic raw seal is recomputed. The runner retains claims, launch/process records, input checks, samples, console and GC output, validation and completion seals. A failed, partial, changed, unclaimed or mismatched slot permanently fails that cohort; there are no replacement observations.

## Metrics and instrumentation scope

Whole-query elapsed `totalNs` is the primary outcome. Record preparation, evaluation, first-row, prefix, tail, consumption, close and retained-read elapsed intervals; total all-thread Java allocation; caller allocation; process-authority ledger peak and endpoints; memo probes, hits, source calls, weak-entry allocations, collisions, growth and cleanup; actual routes and checksums.

The prespecified mechanism measurements are `tailConsumeNs`, `callerCpuTailNs` and `callerAllocatedTailBytes`. The harness takes exactly four bounded caller allocation/CPU snapshots: immediately around query start, after the declared prefix is consumed, at consumption completion, and at final query completion. There is no per-row CPU or allocation sampling. At each boundary the order is caller allocation, caller CPU, then elapsed timestamp. CPU support and enablement are required. Recorded caller CPU excludes GC, JIT and other worker threads.

The to-prefix caller interval includes connection, preparation, evaluation and prefix consumption. In the selected FULL consumer it denotes a fully consumed prefix; a retained consumer would defer some explicit consumption. The tail includes the final exhaustion `hasNext()` and any automatic cleanup it triggers, and ends before explicit result/connection close. The after-consumption interval includes those explicit closes and any deferred retained reads. These distinctions are explicit in each raw metadata record's `callerPhaseScope`. Each raw query's caller CPU and allocation phases must sum exactly to its aligned caller total; nullable prefix/tail observations are allowed only when the declared prefix was not reached. The selected full-consumption fixtures always reach it. Separately aggregated medians need not preserve sums.

The all-thread allocation counter retains the prior harness interval: it is read after `SelectionCapture.close()` in the finally block. The new aligned caller end snapshot precedes that bookkeeping. Therefore **all-thread total minus aligned caller total is not a pure estimate of other-thread allocation**. Each raw metadata record states this explicitly in `allThreadAllocationScope`. The two scopes are reported separately; neither is used to assign GC or JIT cost by subtraction. All counter reads impose bounded instrumentation overhead shared by all arms.

The ordered wrapper can retain consumed binding sets until close, and these short payloads are eligible for source caching. This cohort does not isolate weak-reference clearing or useful-key rematerialization under streaming consumption. A source lazy-call count is not a physical LMDB decode count. The ledger peak measures tracked process-authority claims and estimated memo metadata, not complete Java/native memory or a separately exposed active-query peak.

GC logs cover the whole JVM, including population, warmups and measurements. Report pause counts/time separately from concurrent cycles. These logs cannot assign a pause or concurrent cycle to one measured query without additional evidence.

## Prespecified analysis and decision

The primary estimator takes the median of all five measured queries within each JVM, forms six same-round candidate/comparator ratios for each fixture, then takes their median. Report every ratio and its range. The mean sensitivity substitutes each JVM's arithmetic mean of those same five observations and never replaces the primary estimator. Comparisons are fixed/adaptive, adaptive/ordinary and fixed/ordinary. Zero or unavailable denominator metrics yield undefined ratios rather than invented values. Absolute results are medians of six process medians, not pooled query medians.

Report the new caller CPU/allocation decomposition and tail comparisons alongside whole-query results. A reduced metadata reservation does not prove a latency win. A first-row difference cannot directly be attributed to the current query's later resizes. Equal-event early controls help assess flag/JIT/process variation. The larger working set tests whether suppressing growth loses useful reuse. Keep allocation clusters, direction reversals and unfavorable runs visible. Do not remove outliers, select a preferred estimator retrospectively or pool nested measurements as independent process replicates.

The experiment is descriptive, with no significance or automatic-promotion claim. Report whether fixed capacity preserves late reuse, whether the tail measurements improve, whether that carries through to whole-query latency, and what is lost on the larger working set. Independent review must reproduce estimates and seals before publication. A future controller based on recent history requires another separately qualified experiment.

## Commands and immutable outputs

From the repository root, compile the closed fixture/harness using `result-coordinate-evidence/growth-followup/compile_support.py` and an explicitly qualified memo classpath. Then run `python3 -B result-coordinate-evidence/growth-followup/benchmark/run_pilot.py FIXTURE ARM --attempt NEW_NAME --classpath FINAL_CLASSPATH --slot-granted` once per supervised pilot. Defaults are the required eight warmups and five exact queries.

After all nine pilots and the final tests, invoke `python3 -B result-coordinate-evidence/growth-followup/benchmark/qualify_runtime.py --classpath FINAL_CLASSPATH` with nine repeated `--pilot DIRECTORY` and six final passing `--test-receipt FILE` arguments. It writes only new `fixture-definitions-v1.json` and `qualification/final-runtime-v1.json` within the benchmark directory by default; use fresh output names after any failed qualification attempt.

Freeze with `python3 -B result-coordinate-evidence/growth-followup/benchmark/run_comparison.py freeze --cohort NEW_COHORT --fixtures DEFINITIONS --classpath FINAL_CLASSPATH --qualification QUALIFICATION` and explicit repeated `--source` inputs covering implementation sources, harness sources, contracts, this protocol, the compiler helper, the qualification builder and the profile checker. Publish the qualified checkpoint before timing. Run exactly one pending slot with that script's `next --cohort NEW_COHORT --slot-released` command, then check its completed prefix with `python3 -B result-coordinate-evidence/growth-followup/benchmark/check_route_profile.py --cohort NEW_COHORT --allow-incomplete --output NEW_RECEIPT`.

After all 54 slots, repeat the profile check without `--allow-incomplete`, run the primary `audit` action and the separately implemented independent auditor, and preserve all observations and complete sources. Every artifact writer refuses to overwrite an existing attempt. No historical source or observation is rewritten to accommodate this experiment.
