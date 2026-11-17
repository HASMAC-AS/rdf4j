# LMDB native Value memo: admission, phase changes and natural GC

Completed 17 November 2026. Accepted latency cohort: **comparison-v2**. The feature remains experimental and off by default.

## Decision

**Do not make `always` admission the default on the strength of these results.** Removing the first-window cutoff recovered late reuse, but did not demonstrate an end-to-end latency improvement on the late-repetition fixture. Relative to the current cutoff, it avoided 98,240 lazy source calls per query and reduced total Java allocation by a paired estimate of 2.47%, while the primary paired latency estimate increased 13.53%; the prescribed mean sensitivity increased 5.41%. These are descriptive estimates from six process pairs, with substantial variation.

The strongest new mechanism finding is the interaction between admission and cumulative growth history. The late fixture has a 32,768-unique prefix followed by only 64 repeating keys. Always admission nevertheless grows from 4,096 to 65,536 slots, with a 14,549,472-byte peak metadata reservation. The 64-key tail was already serving repeat hits before the first growth. A **fixed-4,096-slot versus adaptive-growth comparison under always admission** is therefore the next useful causal ablation. It is predicted to preserve the recovered reuse while isolating the cost of growth. Its latency result is still a hypothesis, not a result of this study.

The second new finding is about weak retention: ordinary source caching already preserves initialized small Values in the direct-ValueStore diagnostic. For large Values outside that cache, natural collection removes useful weak representatives, and later requests materialize again unless a consumer keeps them alive. Fewer source calls do not establish proportionally fewer physical decodes.

All 54 accepted JVMs and 702 queries passed result, exposure, route, owner and cleanup validation. The independently implemented auditor reproduced the primary estimates. Full source and strict apply-checked patches are linked below.

## What changed

The experimental implementation adds one constructor-captured selection:

```text
rdf4j.lmdb.nativeValueMemo.admission=initial|always
```

Missing selection means `initial`. `always` bypasses only the first 8,192-probe low-hit cutoff. It does not change weak ownership, full `(ID, position mask)` keys, hashing, source and semantic-identity checks, subsequent cumulative growth, ledger refusal, cleanup, or source-cache defaults. Unknown policy values are rejected. The parent feature remains disabled unless `rdf4j.lmdb.nativeValueMemo.enabled=true`.

This study uses the prior weak-retention implementation in every enabled latency arm. The pre-existing retention option still distinguishes `strong` and `weak`; strong mode is used only as a diagnostic reference here and still does not account for its retained payload graph. The new work does not claim to repair that strong-mode bound. Weak mode charges arrays and conservative table-owned reference metadata and does not own a strong path to the Value or its store owner. That accounting is an estimated metadata charge, not a complete query heap bound.

The complete integration is maintained as an experimental source/runtime overlay, so all latency arms use identical classpath bytes. The four-file feature patch is supplied for applying that overlay to module sources. The small admission-only patch shows this turn's policy change separately. No runtime dependency was added.

## Correctness before timing

The smallest sealed in-repository admission contract failed against the unchanged predecessor before implementation was edited:

```text
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
```

The full new class initially ran 12 tests with three failures, all demanding the missing always-admission behavior; its nine control cases passed. After the minimal change, all 12 passed. The final focused qualifications are:

| Selection | Passing executions |
| --- | ---: |
| New admission contracts | 12 |
| Default behavior, materializer, projection, coordinate and row representations | 138 |
| Weak retention with initial admission | 47 |
| Weak retention with always admission, integration and lifetimes | 28 |
| **Total** | **225** |

These are test executions across configurations, not 225 distinct test methods. Both compiled and interpreted IR lifetime paths were exercised, including retained results after early close/cancellation and racing cleanup. Initial and growth reservation refusal, source/semantic invalidation, exact admission boundaries, fresh owners, and idempotent close remain covered.

Final memo runtime-v2 is byte-identical in all seven prefix classes to the runtime that passed those checks. All 2,778 freshly built LMDB class files matched the preserved baseline classes. Java formatting passed through an explicit copy-only allowlist; the nine complete new Java sources passed the exact required header check. Historical frozen copies were not rewritten to clean up their older header warnings.

The mandatory root offline install and exact online retry both compiled and installed LMDB, but **the full reactor did not pass**. Both stopped in unrelated Spring modules on missing Kotlin 2.3.21; the online retry also recorded Maven Central DNS failure. This is not presented as a green root build. Initial failures, final test receipts and full build logs are preserved in [initial-evidence.txt](../../initial-evidence.txt), [contracts/validation](contracts/validation) and [recovery](recovery).

## Prospective comparison design

Every fixture has 32,768 rows, four projected columns and 131,072 projected opportunities. Values are 64-character IRIs. A hidden unique row key supplies deterministic `ORDER BY`; it is not projected into the memo. The primary consumer reads every binding, calls `stringValue()`, checks projection/type requirements, computes a position-sensitive lexical checksum, and closes the result and connection. Independent full lexical checks ran in qualification; timed queries use the checksum consumer without exact-oracle or primitive-trace overhead.

| Fixture | Prefix | Tail | Checksum |
| --- | --- | --- | --- |
| Early cold-hot | 4,096 unique opportunities | 126,976 opportunities cycling 64 keys | `-392792955594352548` |
| Late cold-hot | 32,768 unique opportunities | 98,304 opportunities cycling 64 keys | `-1128082570782671460` |
| Hot-unique | 8,192 opportunities cycling 64 keys | 122,880 new unique opportunities | `-7925248249943514788` |

The three arms are ordinary lazy Values with the memo disabled, weak memo with initial admission, and weak memo with always admission. All use the exact restored Temurin 25.0.5+8-LTS runtime, fixed 1 GiB G1 heap, eight-byte object alignment, compressed references/class pointers, 1 GiB process ledger limit and 512 MiB per-query limit. Native evaluation and direct adjacency remain enabled with PREFER mode; value overlay and forceSync remain disabled. Indexes are `spoc,ospc,psoc,posc`. Coordinate transport is off. Source caches keep their defaults. Each process generates the same fixture recipe, closes the store, and reopens it before querying.

The fixed schedule is six rounds × three fixtures × three arms = **54 fresh JVMs**. Each executes eight warmups and five measurements: **432 warmups and 270 measured queries**. Every arm permutation is used, and each fixture occupies each process-order position twice. One JVM ran per supervised invocation, with no concurrent builds, tests, diagnostics, archiving or heavy analysis. Full runtime/JDK/source hashes were checked before and after every process.

For each metric, the primary estimator reduces five measurements to one median per JVM, forms six same-round candidate/comparator ratios, then takes their median. The prescribed sensitivity substitutes each JVM's mean of the same five measurements. All observations are retained. There is no significance claim, outlier removal, automatic promotion threshold, or pooling with earlier baseline/Stage-2 studies. The prior multi-gigabyte allocation figures concern other preserved workloads and are not comparable absolute measurements for this fixture.

### AUTO qualification and the rejected first cohort

The first frozen cohort was stopped after its first JVM. All 13 results were correct, but a gate inferred from three-query pilots required at least 98,309 adjacency hits; the five measured queries recorded 98,308. The rejected cohort remains intact and contributes **zero accepted performance observations**. Its query latency and allocation values were not analyzed to choose the revision.

Source inspection explains the one-hit transition. Three bound tail probes per row contribute 98,304 hits; four constant-predicate compilation emptiness proofs contribute four metadata hits. An adjacency root scan contributes one more. The store-scoped statement-access arbiter deliberately samples the LMDB alternative after eight preferred-source completions when structural estimates overlap. This normal choice removes the root adjacency hit without an adjacency fallback or an outer-strategy change.

Nine fresh full-length exact-oracle pilots—**117 exact queries**—reproduced the complete profile: 98,309 hits for queries 1–8, then 98,308 for queries 9–13. The revised protocol seals the exact per-ordinal route and adjacency profile. A separate checker validated it after every JVM and again for the complete cohort, in addition to all original cross-arm/cross-round and correctness gates. A synthetic changed-profile control was rejected even after its raw seal was recomputed. Frozen v1 code, evidence and protocol were never edited.

Normal AUTO selected `ORDER / orderedFactorizedSort / normal`, with one native compile and chunk/factorized engagement, zero generic fallback and zero IR execution in these timed queries. IR ownership was tested separately; it is not mislabeled as the measured route. Operation-level attribution of the inner source transition follows code and aggregate counters, not a new per-call source trace.

**The measured queries are in a matched AUTO alternative-source learning phase.** Eight harness warmups do not establish converged source selection after both alternatives have accumulated observations. This scope is shared by all arms and is part of the frozen design.

## Absolute query measurements

These are medians of the six JVM medians, not pooled medians and not the source of the paired ratios below. Allocation covers all measured Java threads; MB is decimal. Ledger peaks are tracked process-authority claims, not total live heap, native memory or a separately observed query-ledger peak.

| Fixture | Arm | Total ms | Java allocation MB | Prepare ms | Tracked ledger peak bytes | Lazy source calls |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| early-cold-hot | ordinary | 520.51 | 302.204 | 1.086 | 1,152 | 131,072 |
| early-cold-hot | initial | 471.82 | 298.720 | 0.973 | 606,448 | 4,128 |
| early-cold-hot | always | 484.63 | 291.807 | 1.028 | 606,448 | 4,128 |
| late-cold-hot | ordinary | 481.46 | 325.476 | 1.039 | 1,152 | 131,072 |
| late-cold-hot | initial | 536.38 | 326.352 | 1.152 | 606,448 | 131,072 |
| late-cold-hot | always | 587.88 | 318.200 | 1.038 | 14,549,472 | 32,832 |
| hot-unique | ordinary | 771.64 | 455.725 | 1.187 | 1,152 | 131,072 |
| hot-unique | initial | 705.87 | 456.731 | 0.976 | 3,637,728 | 122,944 |
| hot-unique | always | 711.49 | 456.731 | 1.011 | 3,637,728 | 122,944 |

Preparation is about 1 ms and `evaluate()` about 4.5–5.4 ms in these process summaries. Most work occurs while consuming the blocking ordered result. The full interval decomposition, all six process ratios and whole-JVM GC summaries are preserved in [comparison-v2/RESULTS.md](comparison-v2/RESULTS.md) and [audit.json](comparison-v2/audit.json).

## Paired effects

A ratio below one favors the numerator. These ratios must not be reconstructed by dividing the absolute medians above: the order of aggregation differs.

| Fixture | Comparison | Latency: primary | Latency: mean sensitivity | Java allocation | Primary latency range |
| --- | --- | ---: | ---: | ---: | --- |
| early-cold-hot | always/initial | 1.0542 | 1.0305 | 0.9662 | 0.765–1.188 |
| early-cold-hot | initial/ordinary | 0.9172 | 0.9050 | 0.9708 | 0.740–1.219 |
| early-cold-hot | always/ordinary | 0.9414 | 0.9173 | 0.9563 | 0.838–1.036 |
| late-cold-hot | always/initial | 1.1353 | 1.0541 | 0.9753 | 0.906–1.174 |
| late-cold-hot | initial/ordinary | 1.0772 | 0.9344 | 0.9889 | 0.908–1.655 |
| late-cold-hot | always/ordinary | 1.2220 | 1.0528 | 0.9743 | 0.822–1.512 |
| hot-unique | always/initial | 1.0004 | 1.0614 | 1.0000 | 0.977–1.129 |
| hot-unique | initial/ordinary | 0.9356 | 0.9141 | 1.0022 | 0.659–1.059 |
| hot-unique | always/ordinary | 0.9188 | 0.9348 | 1.0022 | 0.744–1.053 |

The late always/initial primary estimate is 13.53% higher latency, with four of six process pairs slower; mean sensitivity is 5.41% higher. Always/ordinary is 22.20% higher under the primary estimator and 5.28% higher under mean sensitivity, with five of six primary pairs slower. Allocation falls 2.47% relative to initial and 2.57% relative to ordinary. This establishes recovered reuse with modest total-allocation savings on this workload; it does not demonstrate a latency win.

The late initial/ordinary comparison reverses direction: primary ratio 1.0772 versus mean sensitivity 0.9344. No primary latency comparison in the entire study is faster in all six process pairs. Hot-unique is especially important to report without confirming a preconceived result: enabled modes do extra metadata work and allocate about 0.22% more in the paired summary, yet their latency estimates are lower than ordinary. This cohort does **not** establish a hot-unique CPU regression.

### Why the elapsed-time attribution remains uncertain

The late always/ordinary first-row ratio is 1.2391. Absolute first-row summaries are about 400.9 ms for ordinary and 494.9 ms for always, whereas hot-tail consumption is about 44.3 and 45.1 ms. The paired tail ratio is 1.02355, with a 0.725–1.305 range. The first-row interval includes blocking ordered-result production and ends before the explicit fixture consumer processes that row. The current query cannot grow its memo until much later. Thus the early interval cannot directly be labeled current-query resize CPU; earlier queries' heap/JIT effects and between-process variation remain possible.

Early initial and always have identical memo event traces, yet their latency ratios range from 0.765 to 1.188 and their paired allocation ratio is 0.9662. Hot-unique initial/always also have identical memo event traces. These controls caution against attributing small process differences solely to the admission branch.

Allocation is tightly clustered within many JVMs but differs between them. Late initial JVM medians cluster near 320.060 or 326.352 MB, with typical within-process five-query spreads around 0.015–0.024%. Early ordinary process medians range roughly 290.655–317.064 MB. Most observed allocation is on the caller thread; all-thread minus caller differences are under approximately 60 KB per measured query. JIT/escape-analysis sensitivity is plausible, but compilation evidence was not collected, so no cause is claimed and no cluster is removed. Exact sequences are in the [independent numerical review](reviews/cohort-v2-numerical-review.json).

Whole-JVM GC pause-count medians are eight for early, nine for late, and twelve for hot-unique. Pause-time medians are approximately 86–118 ms, 144–152 ms, and 253–260 ms respectively. These logs include fixture creation and warmups; they cannot assign GC cost to an individual measured query. Concurrent-cycle duration is not added to pause time.

## Stable memo behavior and memory consequences

The following outcomes were identical across measured queries of each enabled fixture/arm. The raw audit also checks all warmups. A close occupancy is a metadata-slot count, not a count of live weak referents.

| Fixture / policy | Probes | Hits | Source calls | Resizes | Capacity at close | Peak metadata reservation |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Early / initial or always | 131,072 | 126,944 | 4,128 | 0 | 4,096 | 606,448 B |
| Late / initial | 8,192 | 0 | 131,072 | 0 | 0, after cutoff | 606,448 B |
| Late / always | 131,072 | 98,240 | 32,832 | 4 | 65,536 | 14,549,472 B |
| Hot-unique / initial or always | 131,072 | 8,128 | 122,944 | 2 | 16,384 | 3,637,728 B |

Early repetition yields a 96.85% memo hit rate. Late always yields 74.95%; initial admission permanently shuts down after its first 8,192 misses. Hot-unique ends at a 6.20% cumulative hit rate, with no hits in the unique tail. The original policy does not reconsider an admitted table when later repetition disappears.

Late always makes 122,880 more memo probes than late initial and allocates 24,641 more weak entries, while avoiding 98,240 source calls. It grows four times and peaks at about 24 times the initial policy's metadata reservation. Its close occupancy is only 4,094 entries in 65,536 slots. The estimated array footprint peaks at 3,539,104 bytes during resize overlap; the larger charge includes conservative weak-reference metadata. At 4,096 slots, the estimated array charge is 147,536 bytes within the 606,448-byte charge.

The growth driver is accumulated history. There are only 64 source calls in the entire 98,304-opportunity hot tail, with zero collected misses or invalidations. The unchanged cumulative policy cannot first grow before probe 49,152, so at least 16,384 tail opportunities already execute at 4,096 slots. A persistent conflict among those 64 hot keys would generate far more than 64 tail misses before that first growth. The observations support that the hot set has stabilized while old cold-prefix collisions continue to trigger larger tables. This establishes the reservation/copying consequence; it does not isolate its share of total latency.

Across all 702 queries, 468 memo owners opened and closed. There were 78 initial cutoffs, all in late initial, and 624 resizes. Collected misses, invalidations, unsupported values, reservation refusals, rehash discards and post-close array charges were zero. All ledger endpoints were zero. A zero ledger endpoint proves release of tracked reservations; it is not proof that all Java/native memory is freed. A separate active-query peak was unavailable and remains explicitly unclaimed.

The native ordered wrapper can retain consumed binding sets until close, and the 64-character Values are eligible for source caching. These zero collected misses do not establish weak-rematerialization behavior for streaming or oversized uncached results.

## Untimed reuse-distance and bootstrap experiment

Three fresh diagnostic JVMs exercised 30 cases and 3,932,160 requests. An independent Python model reconciled all 510 checkpoints, checksum groups and ledger endpoints. Values were intentionally kept alive by consumers to isolate direct-map collisions and admission/growth behavior. These are synthetic-ID mechanism results, not query timings.

Each cold-start trace has 131,072 requests:

| Cyclic distinct keys | Initial-policy hits | Always-policy hits | Always final capacity |
| --- | ---: | ---: | ---: |
| 64 | 131,008 | 131,008 | 4,096 |
| 4,096 | 1,515, then cutoff | 106,410 | 65,536 |
| 8,192 | 0, then cutoff | 16,950 | 4,096 |
| 16,384 | 0, then cutoff | 2,051 | 4,096 |
| 65,536 | 0, then cutoff | 0 | 4,096 |

With 4,096 cyclic keys, the first window's 1,515 hits are only 18.49%, below the 25% threshold; initial admission loses the later opportunity. Always admission eventually recovers to 81.18% overall hits. But removing the cutoff alone leaves the 8,192-key cold cycle at 4,096 slots and 12.93% overall hits, too low to satisfy the cumulative growth gate. A separate admitted hot bootstrap allows that 8,192-key case to grow to 65,536 slots and achieve 73.24% hits in its cyclic tail. These are different access histories, not matched latency treatments.

Admission and growth therefore constrain one another. Permanent disabling loses delayed reuse, while a growth policy that requires a sufficiently high current-capacity hit rate can fail to discover a larger beneficial capacity. Conversely, old successes and collisions can keep an unproductive or oversized table active. See [TRACE-RESULTS.md](diagnostics/TRACE-RESULTS.md) for the complete replay and bootstrap definitions.

## Natural GC with the real ValueStore

Ten fresh 128 MiB G1 JVMs used eight noncolliding stored IDs with default source caches. Each reopened its seeded store, made an initial read and 16 revisits, and validated all lexical content: 136 requests per process. Each revisit followed 32 MiB of escaping byte-array payload allocation, totaling 512 MiB per process, plus headers and other allocation. No explicit GC was requested. The optional consumer intentionally retained one Value, not a query row.

| IRI characters | Arm / retained consumer | Source calls | Cold-to-initialized observations | Memo hits | Collected same-key misses |
| --- | --- | ---: | ---: | ---: | ---: |
| 1,024 | Ordinary / none | 136 | 8 | — | — |
| 1,024 | Strong / none or one | 8 | 8 | 128 | 0 |
| 1,024 | Weak / none or one | 8 | 8 | 128 | 0 |
| 1,048,577 | Ordinary / none | 136 | 136 | — | — |
| 1,048,577 | Strong / none or one | 8 | 8 | 128 | 0 |
| 1,048,577 | Weak / none | 136 | 136 | 0 | 128 |
| 1,048,577 | Weak / one | 120 | 120 | 16 | 112 |

For small IRIs, ordinary source access already returns the same initialized objects on 128 revisits. The memo removes source calls but does not reduce the eight observed cold completions further. For the oversized IRIs, weak entries lose useful representatives under collection; keeping one consumer Value alive preserves exactly its 16 revisit hits. In the oversized weak runs, before the last revisit, the original Values had cleared 8/8 with no retained consumer and 7/8 with one retained consumer.

The diagnostic has no bucket conflicts and never reaches an admission or growth boundary, so the added weak misses reconcile to collected same-key entries. This is stronger than interpreting a general collected-slot counter, which can count a cleared entry before its key is compared. A cold completion is an observed uninitialized-to-initialized transition, **not an instrumented physical LMDB decode or storage-read count**.

Collection can also occur during subsequent consumption. In particular, oversized ordinary/weak cases had zero canaries observed cleared at the pre-demand checkpoint despite later collections and directly observed Value clearing. The evidence does not assign every clearing event to the preceding pressure phase. These are single-process mechanism cases, not latency estimates or a statistical GC comparison.

All 190 structured records, 170 rounds and 1,360 requests passed the independent audit, including both independently regenerated lexical checksums. Every ledger endpoint was zero and every source closed. Complete evidence: [natural-GC report](diagnostics/analysis/natural-audit-v1/RESULTS.md).

## What should be tested next

The smallest useful next ablation is **adaptive growth versus a fixed 4,096-slot table, both with always admission and identical weak ownership**. Keep the memory budgets unchanged; forcing a failed growth reservation would disable the memo and would be a different treatment. Keep ordinary source access as a fresh control. Qualify the same results, source call counts, full exposure and native route history before timing.

For the late fixture, the prospective mechanism prediction is unchanged 98,240 hits and 32,832 source calls, zero resizes, and a 606,448-byte metadata peak instead of 14,549,472. That counterfactual has not been run. A future latency benefit must be measured, and the current first-row variation means it cannot be inferred from the reservation reduction alone. A larger cyclic working set must remain a counterexample/control before generalizing a fixed-capacity policy.

Before choosing a different warmup schedule, separately qualify its full AUTO history and allocation behavior. More warmups can move the inner access learner into a different source-selection regime; they are not a neutral substitute for the present matched learning-phase experiment. Compilation evidence would help investigate the stable per-JVM allocation clusters without assuming their cause or dropping observations.

A recent-window or bounded-reconsideration controller is a later design candidate, after the growth ablation. TinyLFU ages frequency history, and Caffeine combines recency and frequency for admission; these are useful primary-source precedents for handling phase changes. They do not justify importing a broader cache policy into this direct-mapped iteration memo without measuring its extra work. No Caffeine code or dependency was added. Sources and the limited inference are in [RESEARCH.md](RESEARCH.md).

## Deliverables and reproduction

In a fresh Git checkout, `artifacts/complete-source-v2.tar.gz`, `comparison-v2/protocol.json`, and `reviews/cohort-v2-independent-audit.json` must first be restored from their losslessly preserved objects using the commands below. Their links point to the restored paths. These complete files are already present in the working copy used for this report.

- [Complete implementation and experiment source archive](artifacts/complete-source-v2.tar.gz): 139 regular files, including 41 Java and 35 Python sources, with full readback and manifest verification. SHA-256 `831feacaf27d260da3c8b0c53bd2503b1a4d9000b7f5662490a940d024d70bb0`.
- [Dependency/reproduction README](artifacts/complete-source-v2.README.md) and [source manifest](artifacts/complete-source-v2.manifest.json). A full RDF4J checkout, pinned toolchain and dependencies are required; this is not an installed standalone runtime distribution.
- [Full four-file feature patch](artifacts/complete-native-value-memo.patch), against `47e6eba9bd1b238ea84c1b9ad4cd8cdf28caebaf`. SHA-256 `5ebfd63f5804123af798e5734b971ec3bc2c065c4bef77911b1678b0b8492a91`.
- [Admission-only diff against the prior retention memo](artifacts/admission-only-versus-retention.patch). SHA-256 `900cf80358e7eae3b493f1db4eef86beadfd2974d93959cdf3cedaed0e9405cd`.
- [Frozen protocol](benchmark/PROTOCOL-v2.md), [executable protocol](comparison-v2/protocol.json), [primary results](comparison-v2/audit.json), [independent audit](reviews/cohort-v2-independent-audit.json), and [independent interpretation](reviews/COHORT-V2-INDEPENDENT-REVIEW.md).
- [Delivery verification](artifacts/source-delivery-verification-v2.json) and [root archive readback](artifacts/root-delivery-readback-v2.json). Both patches passed strict hunk reconstruction, `git apply --check`, actual disposable application and exact canonical-source comparison.

The accepted protocol SHA-256 is `239c97fd23424922cde7b91ad507bf3dad4cdc0629f411fef63078fa6266149c`. All frozen sources remain unchanged. Large raw manifests, both source archives, and the independent audit are preserved losslessly with exact hashes, bounded binary chunks/gzip objects and verified restore helpers under `recovery`; originals remain available. The entire 54-process raw cohort is checkpointed as ordinary files, including claims, command/process records, samples, GC logs, seals and profile checks.

To reconstruct the large artifacts in a fresh Git checkout, run from the repository root:

```bash
python3 result-coordinate-evidence/admission-followup/recovery/preserve_evidence.py restore --archive result-coordinate-evidence/admission-followup/recovery/preserved-pretime-v1
python3 result-coordinate-evidence/admission-followup/recovery/preserve_evidence.py restore --archive result-coordinate-evidence/admission-followup/recovery/preserved-pretime-v2
python3 result-coordinate-evidence/admission-followup/recovery/preserve_evidence.py restore --archive result-coordinate-evidence/admission-followup/recovery/preserved-final-v1
python3 result-coordinate-evidence/admission-followup/recovery/preserve_binary_artifacts.py restore --archive result-coordinate-evidence/admission-followup/recovery/preserved-source-archives-v1
```

Restoration verifies complete hashes, creates only missing files, accepts byte-identical existing files and refuses different existing content. It does not overwrite another version of an artifact.

Qualified checkpoints were pushed before timing: `4a5f982b2ced59966af63084abf2a353ce994650` for v1 and `7b83cb71663b96bd2b9a2dacdf29b5c1d49db619` for the full-sequence v2 qualification. The final results checkpoint uses the same `GH-0000-lmdb-lazy-result-coordinates` branch in HASMAC-AS/rdf4j. Publication receipts bind local and remote trees and record the expected-parent nonforced updates. No production-default promotion is part of this work.
