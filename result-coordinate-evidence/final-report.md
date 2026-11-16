# LMDB ownership repairs and result-coordinate experiments

## Main findings

The reproduced ownership regressions have passing repairs, and the transport gates exercise both compiled and interpreted native IR. The matched performance experiments establish a narrower result: **on the default fully consumed OPTIONAL-fanout query, ordinary value reuse is faster than every timed coordinate representation (DENSE, CHANGES, and ROWS), while coordinates reduce allocation.** The final four-arm confirmation, with outer-root hosting explicitly audited, measured a median paired DENSE/OFF latency ratio of **1.33563** and allocation ratio of **0.83741**: **33.56% slower, with 16.26% less allocation**. CHANGES and ROWS also remained slower than OFF in every matched round. Compact output-window representations did not close the latency gap. These findings use identical ownership code within each comparison. [Validation record](ownership-validation-report.md); [root-qualified confirmation](root-qualified-transport-comparison/summary.json).

The final selected correctness gate completed all 484 requested classes: 6,469 cases passed, twelve were skipped, and there were zero failures or errors. All seven timing protocols are complete. The final confirmation closes the missing outer-root observation for its own sixteen forks; the original six protocols remain separately identified and retain that limitation. This is not a repository-wide test claim. The earlier broad reused-JVM run ended in a heap failure whose cause remains unresolved. Exact compact-mode penalties are sensitive to forks and measured-iteration selection. For the row-selection hypothesis, **ROWS participated fully but established no shared column groups in this benchmark: G/W = 1.0**.

## Source identity and historical boundary

Work began from branch `GH-0000-lmdb-lazy-result-coordinates`, commit `15fe7d5011d27d31d76436238363ee3f34b3ba50`, “GH-0000 Checkpoint LMDB result reuse and explicit ownership.” The frozen benchmark implementation is that checkpoint plus the repaired working tree, identified as:

`15fe7d5011+worktree-sha256-34767935d60f32ba61adfb44495c607e17ca760d5d2f77a17ddb2862c4f7a8ac`

This is a working-tree identity, not a new commit. The [frozen classpath provenance](frozen-current/provenance.json), [first comparison manifest](reuse-versus-dense/classpath-manifest.json), and [four-arm manifest](transport-comparison/classpath-manifest.json) identify the actual copied runtime artifacts. Earlier correctness runs have separate manifests. In particular, the 185-class gate records `15fe7d5011+worktree-sha256-d7cc3f92aa1a77a373e9812d3c499bbd49b8489f63eacf45d1f525df8161642b`; its final fixture repair has a subsequent official build and test record. Those stages should not be represented as one identical historical source snapshot. [Gate source identity](final-validation/source-identity.txt); [final fixture follow-up](final-validation/final-fixture/results.json).

The final delivered LMDB source/driver identity is `15fe7d5011+worktree-sha256-8c59454811c9927d79739c1780fa88238c4d613bc0c111cf8d4cde8feeb49059`. Relative to the frozen benchmark manifest, exactly two of the same 2,057 files change: cancellation fixtures in `LmdbNodePredicateRouteTest.java` acquire explicit fused-session ownership and cleanup, and `LmdbNativeGenericRootHostTest.java` awaits startup maintenance before asserting a steady-state native route. All 634 production source/resource files and both benchmark drivers remain byte-identical. After the final rebuild, all 2,780 files in the LMDB production class directory are byte-identical to the frozen benchmark; eleven test-class files change, all in those two fixture families. The reversible [test-only delta](final-validation/benchmark-to-delivery-tests.patch) and [delivery provenance](final-validation/delivered-source-provenance.json) preserve this distinction. These identities cover the declared LMDB source/driver set; the final package manifests cover the complete repository archive. [Bytecode comparison](final-validation/delivered-bytecode-comparison.json).

The intermediate NodePredicate-only identity `a7ab4a…` belongs to the repeated broad gate that exposed the startup-maintenance fixture race. Its original manifests and one-fixture delta remain preserved in [delivery history](final-validation/delivery-history/node-predicate-only-a7ab4a/README.md); that failed gate is not relabelled as a run of the final two-fixture source.

The supplied historical measurements—approximately **2.79 → 1.42 GB/query** and **2.21–2.25 → 2.68 seconds**—predate the checkpoint's present ownership migration. Their preserved binaries, complete commands, JVM flags, and raw results could not be recovered. A separately committed Stage-2 source state was not identified. The current numbers therefore cannot be substituted into that historical series or interpreted as a reproduction of it. [Historical provenance and protocol](EXPERIMENTS.md).

The checkpoint's parent, `e7c6ca7537ebde5a3a5198f4e367286707c68edd`, was reconstructed separately. Its label is `fresh-parent-e7c6ca+overlay-845db9492e0524849b575e46825b39fee8d6805925f0200131caca942ef9b22c`. The declared overlay adds the harness, checkpoint benchmark fixture/resources, and diagnostic ledger-peak reset; its production change does not alter admission or ownership. Dependencies and runtime artifacts were copied into an isolated reconstruction, whose root build and eight fixture tests passed. This comparison spans different ownership implementations and is supplemental to the same-source ablation. It is neither a recovered baseline binary nor a recovered Stage-2 artifact. [Reconstruction manifest](fresh-parent-e7c6ca/reconstruction.json); [exact overlay](fresh-parent-e7c6ca/harness-overlay.patch).

## Ownership repairs and accounting

### Query lifetime, invocation binding, and detached results

The scalar lifetime bug came from retaining the first child's source/resolver in query-scoped state. Closing that child invalidated values that the query owner still needed. The repaired query owner retains scalar representatives without retaining that child resolver; a live resolver can later associate certified stored IDs. Publication is checked against closure, and uncached matching avoids resolving unrelated cold values. The final value-resolution contract suite passed 51 cases, with an additional worker-condition lifetime case. [Ownership validation](ownership-validation-report.md).

Workers and wrappers now carry the invocation that actually owns their source, fused session, execution, and preparation. Prepared replay locally rebinds a strategy when kernel execution changes the session; worker scratch rows inherit the invocation explicitly. Projection and multiprojection forward their carrier so outer ORDER/TOPK consumers retain preparation through inner exhaustion. Compiled wrapper binding preserves private rewritten variable names even when the caller uses a fixed binding layout. Group iteration distinguishes an accepted external reader lease from an internal borrowed producer, removing a self-retaining close cycle. These are production lifetime repairs, rather than relaxed ownership checks. [Wrapper regression](wrapper-context-regression/README.md); [kernel-hook regression](kernel-hook-invocation-regression/README.md); [validation audit](ownership-validation-report.md).

Both native fragment emission paths now produce detached entry snapshots. Serialization preserves logical values and GRAPH provenance while dropping live native-ID authority. Detached generation evidence can certify appropriate entry reuse without pretending that closed live scope tokens remain usable. VALUES and compiled input recipes retain their domain requirements. This detaches engine-owned row and resolver state; it does not promise deep immutability for arbitrary application-supplied RDF value implementations. [Fragment regression](fragment-detachment/README.md); [entry and VALUES validation](ownership-validation-report.md).

Literal witnesses also needed a semantic correction: exact native literals had been omitted from entry-ID evidence, and a refused native seed could incorrectly become a false EXISTS result. Mutation-sensitive literal witnesses and semantic fallback preserve the current input, graph/frame bindings, compatibility checks, short-circuiting, and cleanup. Deferred generic scalar preparation preserves cold and cyclic constants when evaluation never reaches them, including LIMIT 0. Explanation preparation retains structural native annotations without opening a result iteration. [Production repair audit and focused evidence](ownership-validation-report.md).

### Shared recipes and the measured memory boundary

Compiled recipes are reusable descriptions; bound execution state belongs to an invocation. Size-dependent recipe-capture storage is admitted before allocation. Small fixed helper containers are constructed before the initial scratch claim, so this is not a guarantee of zero allocation before refusal. Active invocations within one query share an immutable recipe charge by identity rather than each child charging the same metadata again. Frozen value-catalog metadata is retained and charged independently after sibling compilation has finished. Preparation leases release shared charges when the last borrower closes. Refusal, capture/delegate failure, failed registration, and execution closure unwind partial claims. The final nine recipe-memory tests cover these lifetimes, re-admission, and large VALUES/range refusal. [Accounting audit](ownership-validation-report.md); [range regression](recipe-range-regression/README.md).

Footprint estimates include retained lexical strings and structural arrays. Compiled recipe descriptions are bounded summaries derived from detached metadata: capture no longer renders a potentially enormous physical-plan string before budget admission. Ordinary execution diagnostics remain available. These changes address both missing charges and allocation that occurred too early to be refused. [Description regression](recipe-description-review/README.md).

**Idle prepared-query artifacts remain outside the execution ledger**, as does the original algebra. Capture scratch can be fully released while an idle recipe still exists. The implementation supplies no global budget for an arbitrary collection of idle prepared queries. Accounted footprints are conservative estimates, and query-ledger peaks are not measurements of retained JVM heap, RSS, LMDB mappings, or application-retained result collections. That boundary applies to every performance table below. [Accounting boundary](ownership-validation-report.md); [measurement semantics](EXPERIMENTS.md).

### Fixture migrations are a separate category

Many direct operator tests still constructed ownerless rows, omitted fused sessions, mocked strategies that could not bind an invocation, or supplied sources without truthful origin/generation certification. Their migration uses explicit carriers, standalone fused runtimes, real test strategies, and explicit cleanup. Existing result, authority, deadline, cancellation, memory, and exception-identity assertions were retained. These changes make fixtures honor the new internal contract; they are not themselves production optimizations. [Fixture migration audit](ownership-validation-report.md).

Two fixture restoration bugs also leaked a tiny memory override or forced strategy into later classes. Consequently, large numbers of downstream errors in intermediate broad runs were not independent production failures. Their logs remain preserved, and the contaminated counts are not used as final correctness evidence. The remaining failure in the fresh 185-class gate was another ownerless semantic-callback fixture; its official repair passed without changing production behavior. The later broad gates exposed two missing fused-session cancellation fixtures and a startup race in the native-root classification fixture. In the latter, derived term-index CLEANUP temporarily removed the dictionary authority receipt, correctly causing generic root hosting with a native VALUES child. Waiting for the existing post-ready and index-completion barriers preserves the route assertions without changing production eligibility or safety checks. Fifty full-class repetitions then passed (1,150 case executions), followed by a formatter-enabled official install and a 23/23 official class run. [Exact startup-race diagnosis](final-validation/generic-root-diagnostic/README.md). [Verification record](ownership-validation-report.md).

## Verification achieved and limits

| Evidence stage | Recorded outcome | Interpretation |
| --- | --- | --- |
| Untouched checkpoint reproduction | 282 tests; 10 failures; 55 errors | Expanded selection, distinct from the user's earlier 163-case run. |
| Ownership03 evaluation/codegen | 4,539 tests; 112 failures; 433 errors | Contaminated by a leaked forced-strategy fixture setting. |
| Ownership04 broad execution | 400 completed classes; 5,318 tests; 5 failures; 32 errors; 12 skips, then heap OOM | Incomplete reused-fork run at 2 GiB. |
| Standalone OOM-class probe | 5/5 passed at 2 GiB | Does not identify or dismiss the reused-fork OOM cause. |
| Fresh final batches | 185 classes; 2,706 cases; 2,705 passed, one fixture failure, zero errors/skips | Three fresh serial JVM batches; includes all 67 classes unfinished by ownership04. |
| Official final fixture follow-up | 10/10 passed | Five repaired expression-domain cases plus five benchmark-fixture cases after rebuilding. |
| First normal-heap broad gate | 484 classes; 6,481 cases; zero failures, two fixture errors, twelve skips | Missing fused-session cancellation fixtures, subsequently repaired. |
| Repeated normal-heap broad gate | 484 classes; 6,481 cases; one failure, zero errors, twelve skips | Startup-maintenance route-assertion race, diagnosed and corrected with readiness barriers. |
| Final normal-heap broad gate | **484 classes; 6,481 cases; zero failures/errors; twelve skips** | **6,469 passed** in one reused 4 GiB fork, all requested classes reported, source hashes unchanged. |

The [validation report](ownership-validation-report.md) links commands, source hashes, XML, logs, and the [fresh-batch aggregate](final-validation/batch-results.json). Root/module build and formatting records passed, but build success is not a full test pass. Counts overlap across selections and must not be added as a unique-test total. The [final repeated gate](final-validation/normal-heap-verified/results.json) covers the complete declared 484-class selection after both last fixture corrections. Ten pre-existing unconditional disables and two opt-in harnesses account for all twelve skips; the separate 1,014-query telemetry census remains unverified. The earlier 2 GiB reused-fork OOM remains causally unresolved despite completion at the module's normal 4 GiB heap. [Skip audit](final-validation/normal-heap-broad/skips-audit.md).

Official additional transport gates passed **OFF 84/84**, **CHANGES 90/90**, **SELECTION 90/90**, and **ROWS 90/90**, with no failures, errors, or skips. Factor-coordinate cases explicitly execute compiled and interpreted IR, aliased UNION/nested OPTIONAL bags, retained rows after early close/cancellation, and cancellation/close races. Enabled compact selections additionally include compiled/interpreted optional-fanout reuse and a scan-backed hash-buffer case. OFF omits transport-specific reuse expectations; its selection is not identical to the enabled selections. These gates validate tested semantics and cleanup, not performance superiority. [Mode-gate results](final-validation/modes/results.json).

## Matched experimental method

The default fixture contains 100 measurements × 10 readings, 3,278 triples, and 12 projected columns. Full consumption returns **1,152,000 rows** and performs **13,824,000 binding reads**. An independently calculated lexical multiset checksum is **3123987218479850048**. Canonical graph SHA-256 is `cf637a7e80dbd9468d7e8b04116da072f726038282dfab15380c267c8cc82e9b`; query SHA-256 is `9cffa2dc37bbb1e2743442b228d5ab9a41115bf7c4a6667506231fbda3bfc301`. [Protocol](EXPERIMENTS.md); [four-arm metadata audit](transport-comparison/independent-review.md).

Each arm runs in a fresh JVM against frozen copied artifacts. Java is **25.0.5+8-LTS**, with `-Xms1024m -Xmx1024m -XX:+UseG1GC`; transport is the only arm-specific JVM property. Every fork reports nine available processors. The process ledger limit is 1 GiB and the per-query limit 256 MiB. Builds/tests are stopped during serial measurement. The driver checks runtime artifact digests before and after the run. Normal AUTO selection is retained; no strategy is forced. [First configuration](reuse-versus-dense/configuration.json); [four-arm configuration](transport-comparison/configuration.json); [protocol](EXPERIMENTS.md).

Allocation is the JVM-wide thread-allocation counter, with caller allocation recorded separately. Samples record connection, public prepare, evaluate, consumption, close, retained-read, and total time. Public `prepareTupleQuery()` timing does not isolate all compiler work: evaluation or first pull may prepare further state. First-row latency overlaps consumption and is not added again. The ledger peak is reset between serial queries without changing admission; nonzero usage before or after a sample invalidates it. A bounded observer records successful native AUTO operator choices and their frequencies, alongside compiled/interpreted binds and native counters. The original schema does not record outer-root hosting: a generic outer root containing one native kernel can have the same observed native signature. It therefore establishes operator/tier matching, not complete outer-plan equivalence. Missing diagnostics in older binaries mean unavailable, not zero. [Measurement definitions](EXPERIMENTS.md).

The first experiment alternates OFF/DENSE order across four matched fork pairs, with three warmups and five measured queries per fork. The four-arm experiment uses four balanced rotations, five warmups, and five measurements: every arm occupies every order position once. Each JVM's measurements are reduced to a median, then candidate/baseline ratios are calculated within matched rounds. Each matched round contains one separate JVM per arm; each ratio divides the corresponding per-JVM medians. Reported ranges are the individual matched-pair ratios, not confidence intervals. The two experiments are analyzed separately because their warmup protocols differ. [Run records](reuse-versus-dense/runs.json); [four-arm run records](transport-comparison/runs.json).

The final inventory contains **152 fresh JVM forks and 16,448 validated queries**, comprising 13,448 warmups and 3,000 measurements across seven separately analyzed protocols. The original six account for 136 forks and 16,288 queries; the root-qualified confirmation adds sixteen forks and 160 queries. The combined CSV contains 28 comparison rows, including three new confirmation rows, while byte-exact copies of the original six-protocol CSV/count inventory are preserved. Shared baseline queries must not be double-counted by adding CSV rows, and repeated measurements within one JVM are not independent fork replicates. No protocol is pooled into another estimator. [Verified inventory](comparison-summary-counts.json); [comparison CSV](comparison-summary.csv); [original six-protocol inventory](comparison-summary-original-six-protocols-counts.json).

## Root-classification coverage and final confirmation

The startup-race investigation demonstrated that a generic outer root can still compile a native VALUES child. A follow-up audit found that `SelectionCapture` receives but discards the selected expression identity, and `semanticNativeCompiles` counts both root and interior compilations. The original logs do not fill that gap: generic dispatch uses INFO logging, while the frozen logging configuration uses WARN. None of the original 16,288 query records contains a hosted-root count. Existing latency, allocation, checksum, ledger, and observed native dispatch measurements are retained unchanged, with this limitation.

A separate four-arm confirmation uses the same frozen implementation and original timed harness, with an external launcher that reads root counters only after the original main returns. Its unique 3,481-byte class is appended after the original 126 classpath entries; it replaces no engine or harness class and adds no pre-main counter read/reset or timed-loop instrumentation. The prespecified protocol uses four balanced rotations, five warmups and five measurements per arm, normal AUTO, and the original fixed 1 GiB G1 heap. Any failed fork or root audit would stop the whole control without replacement and block its aggregates. This confirmation addresses a concrete missing observation; it does not select favorable samples from earlier runs. [Locked protocol](root-qualified-confirmation-protocol.json); [launcher](root-qualified-launcher/README.md).

**All sixteen forks and 160 queries passed, with no exclusion or replacement.** Every fresh JVM's absolute `HOSTED_GENERIC` count is zero, and each records exactly ten semantic compilations, matching one compilation per query. In this native-enabled, non-null-source harness, every QueryRoot passes through `compileHostedPlan`, which either returns a supported native step or increments the hosted counter when constructing a GenericRootPlan. The zero totals therefore cover all warmups and measurements in these forks. All eighty measurements have the same native AUTO selection and compiled-kernel bind frequencies, valid full-result checksums, no omitted selection events, and zero ledger usage before/after closure. Artifact and protocol checks passed before and after timing. This does not prove absence of every generic interior operator. The observed zero `UNHOSTED_HARD_DECLINES` count is a consistency diagnostic only: that counter has no production mutation in this source. [Completed confirmation](root-qualified-transport-comparison/summary.json); [post-run preservation](root-qualified-transport-comparison/post-run-preservation.json).

Absolute values below are medians of four fork medians; allocation uses decimal GB. The ratio estimator remains the median of within-round candidate/baseline ratios, rather than a quotient of these descriptive arm medians.

| Arm | Total seconds | Allocated GB/query | Ledger peak bytes | Lazy-value calls/query |
| --- | ---: | ---: | ---: | ---: |
| OFF | 4.440520 | 1.630676 | 2,432 | 2,770,101 |
| DENSE | 5.973090 | 1.365590 | 2,556,992 | 1,147 |
| CHANGES | 6.350202 | 1.365501 | 2,474,904 | 1,147 |
| ROWS | 6.202750 | 1.365623 | 2,478,245 | 1,147 |

| Paired JVM comparison | Median time ratio | Individual-pair range | Median allocation ratio |
| --- | ---: | ---: | ---: |
| DENSE/OFF | **1.335632** | **1.173093–1.528292** | **0.837405** |
| CHANGES/OFF | **1.408327** | **1.302667–1.481122** | **0.837383** |
| ROWS/OFF | **1.434418** | **1.224647–1.480662** | **0.837458** |
| CHANGES/DENSE | 1.055339 | 0.969136–1.110455 | 0.999948 |
| ROWS/DENSE | 1.063471 | 0.938096–1.100516 | 1.000024 |

Thus every coordinate arm is slower than OFF in every matched round. Compact/DENSE comparisons are less settled: their median penalties are 5.53% and 6.35%, but both ranges cross parity. DENSE's third-round JVM also allocates approximately 1.320 GB/query, below its other forks' approximately 1.366 GB. Its paired allocation ratio is 0.809214, versus 0.837387–0.837509 in the other rounds; compact/DENSE allocation ratios reach approximately 1.0348–1.0349 in that round. Preserve this real variation instead of describing enabled-arm allocation as uniformly identical. The median allocation reduction remains approximately 16.3% for each enabled representation. These ranges describe four observed pairs, not confidence intervals. [Raw fork medians](root-qualified-transport-comparison/fork-medians.csv); [independent raw-record audit](root-qualified-transport-comparison/independent-review.md).

The prespecified secondary last-three sensitivity gives DENSE/OFF **1.392715** (range **1.140441–1.663401**), CHANGES/DENSE **1.025793** (0.887651–1.112643), and ROWS/DENSE **1.041916** (0.871413–1.128846). Every enabled/OFF ratio remains above one; compact/DENSE ranges still cross parity. In the third round, DENSE's five measured durations move from 5.91 to 7.07 seconds while the paired OFF JVM falls from 4.65 to 4.05 seconds. DENSE's lower allocation level is stable across that measured block and already appears late in warmup. These observations limit a steady-state interpretation; neither unfavorable samples nor the allocation difference are removed. No further performance run was added. [Sensitivity and per-iteration values](root-qualified-transport-comparison/independent-review.md).

Public prepare medians are 2.742/2.757/2.910/3.222 ms and evaluate medians 18.879/20.530/21.324/23.752 ms for OFF/DENSE/CHANGES/ROWS. Consumption medians are 4.416/5.950/6.326/6.169 seconds. The latency difference again occurs overwhelmingly during consumption, which can include first-pull preparation; public prepare is not an isolated compiler benchmark. Per-phase medians do not sum to the median total. No unmeasured JVM or scheduling cause is assigned to the variation.

ROWS again reports 4,500 valid windows, 1,152,000 encoded rows, 49,500 column-window positions, and 49,500 groups per query, with no recorded invariant refusal. Its G/W ratio is one: participation is complete, but this fixture does not exercise cross-column sharing. Compact peaks fall by the same 82,088/78,747 bytes relative to DENSE as in the original four-arm run. The original comparisons below remain useful separate evidence, with their original root-observation limitation; the new counter observations do not retrospectively classify them. [Independent participation audit](root-qualified-transport-comparison/independent-review.json).

## Original ordinary-reuse comparison

| Matched round | OFF median seconds | DENSE median seconds | DENSE/OFF time | DENSE/OFF allocation |
| --- | ---: | ---: | ---: | ---: |
| 0 | 4.431494 | 5.798777 | 1.308538 | 0.837459 |
| 1 | 4.487988 | 5.822174 | 1.297279 | 0.837417 |
| 2 | 4.402769 | 6.203821 | 1.409072 | 0.837531 |
| 3 | 4.698938 | 5.895741 | 1.254696 | 0.837433 |
| Median paired ratio | — | — | **1.302909** | **0.837446** |

DENSE is **30.29% slower** and allocates **16.26% less** by the paired medians. All 40 measured samples validate results, record the same successful native AUTO selection and compiled-tier counters, and begin/end with zero ledger usage. Each DENSE query opens one coordinate carrier with no recorded initial/growth admission refusal; OFF opens none. This records a net latency cost for coordinate use on this workload under the repaired ownership implementation, with the original outer-root observation gap described above. It does not attribute that cost to a single instruction or reconstruct the historical baseline. [Raw fork medians](reuse-versus-dense/fork-medians.csv); [paired summary](reuse-versus-dense/summary.json).

## Original compact comparison, participation, and sensitivity

The following absolute values are medians of four fork medians. Allocation uses decimal GB.

| Arm | Total seconds | Allocated GB/query | Ledger peak bytes | Lazy-value calls/query |
| --- | ---: | ---: | ---: | ---: |
| OFF | 4.399386 | 1.630699 | 2,432 | 2,770,101 |
| DENSE | 5.713344 | 1.365619 | 2,556,992 | 1,147 |
| CHANGES | 6.367309 | 1.365559 | 2,474,904 | 1,147 |
| ROWS | 5.978856 | 1.365577 | 2,478,245 | 1,147 |

| Paired JVM comparison | Median time ratio | Individual-pair range | Median allocation ratio |
| --- | ---: | ---: | ---: |
| DENSE/OFF | 1.275393 | 1.202053–1.333021 | 0.837444 |
| CHANGES/OFF | 1.436966 | 1.265783–1.587597 | 0.837421 |
| ROWS/OFF | 1.360063 | 1.244140–1.391416 | 0.837432 |
| CHANGES/DENSE | 1.121997 | 1.051968–1.204949 | 0.999954 |
| ROWS/DENSE | 1.037872 | 1.021328–1.116877 | 0.999965 |

Compact/DENSE ratios are computed directly within matched rounds, not by dividing separately summarized OFF-relative ratios. Under the prespecified full-five statistic, DENSE is the fastest enabled arm; CHANGES is 12.20% slower and ROWS 3.79% slower. Enabled-arm allocation differences are below 0.01% in paired medians and smaller than individual-sample spread. CHANGES lowers the accounted peak by 82,088 bytes (3.21%) and ROWS by 78,747 bytes (3.08%) relative to DENSE. [Independent raw-record analysis](transport-comparison/independent-review.md).

All 80 measurements and 80 warmups validate. Every measured query selects one normal `ROW → irKernel` route, binds one compiled kernel and no interpreted/aggregate kernel, and records the same IR row count. No selection events are omitted; replay, chunk, and factorized-route engagement counters are zero. Enabled arms each open one carrier with no recorded initial/growth refusal. Thus the compact comparisons show no mismatch in the observed native operator/tier frequencies and no extra resolver-call reduction beyond DENSE. [Route and participation audit](transport-comparison/independent-review.md).

Each ROWS query reports one owner, **4,500 valid windows**, **1,152,000 encoded rows**, **49,500 tracked column-window positions**, **49,500 groups**, and zero invariant refusals. Eleven tracked outputs become eleven singleton groups per window: **G/W = 1**. This is actual participation, not silent fallback. It measures four-word records, integer selection, exact comparison, and validation in the singleton case. It does not establish the performance of useful cross-column sharing or prove that no different partition could expose such sharing. [ROWS counter definitions](transport-design-report.md); [observed counters](transport-comparison/independent-review.md).

Many measured sequences still decline after five warmups. A post hoc last-three sensitivity analysis supplements the primary full-five results:

| Comparison | Full-five median ratio | Last-three median ratio | Last-three range |
| --- | ---: | ---: | ---: |
| DENSE/OFF | 1.275393 | 1.306599 | 1.227890–1.374628 |
| CHANGES/DENSE | 1.121997 | 1.030545 | 0.997400–1.229008 |
| ROWS/DENSE | 1.037872 | 1.053884 | 1.022176–1.113121 |

OFF's advantage remains. ROWS remains slower than DENSE in every pair. The exact CHANGES penalty is less settled: 12.20% becomes 3.05%, with one pair approximately equal and another 22.90% slower. These are not demonstrated steady-state penalties. Consumption contributes at least 98.9% of every measured total; public prepare medians are 2.88–3.00 ms, evaluate 18.01–24.91 ms, and close 0.098–0.122 ms. Separate phase medians do not sum to the total median. The large observed difference occurs during consumption, whose work can include first-pull preparation. [Sensitivity and phase evidence](transport-comparison/independent-review.md).

## What the representations test

DENSE copies five full-width coordinate words per tracked cell. CHANGES records dirty cells and reconstructs coordinates into a reusable row; backward reads can replay a prefix. Supplementary SELECTION stores an integer per cell into changed records; it received correctness coverage but was not a timed arm. ROWS stores four-word source-row records, one integer per group/emitted row, and fixed column mappings. Its first row groups outputs by exact source-group, parent, position, and namespace equality. Later divergence, partial unbinding, or a changed tuple column withdraws optional coordinates; ordinary reuse continues. Unknown first-row columns begin in separate groups. [Implementation and storage formulas](transport-design-report.md).

For the observed 256-row windows and eleven tracked outputs, the DENSE output-coordinate payload is `256 × 11 × 5 × 8 = 112,640 bytes` (110 KiB), before headers and common state. The owner reuses this bounded storage across 4,500 windows. Compressing that reusable buffer is therefore a much smaller allocation target than the roughly 1.366 GB allocated over the whole query. Reuse does not eliminate copying: the corresponding logical payload writes total `1,152,000 × 11 × 5 × 8 = 506,880,000 bytes` over the full result. That is arithmetic from the representation, not a hardware-memory-traffic measurement. This storage scale helps explain the negligible difference in total allocation between enabled formats; it does not identify the CPU bottleneck. [Storage formulas and reuse](transport-design-report.md).

The compact experiment changes **bounded output-window transport only**. Upstream registers, saved frames, and producer sidecars remain five-word coordinates; the materializer still validates and consumes reconstructed five-word coordinates. OFF bypasses coordinate acquisition/cache use but retains demand propagation, shape analysis, generated fields/null guards, interfaces, common ownership code, and the ordinary cache's five-long scratch buffer. It is a runtime ablation, not source deletion. Compact modes still scan/validate outputs, and ROWS may perform quadratic grouping work in width per window. Admitted primitive buffers retain their high-water capacity until owner close, including after optional transport is withdrawn; namespace draining may release materializer pages earlier. Admission counters do not cover every eligibility or sidecar/directory decline, so participation and route evidence must accompany them. Those costs plausibly explain why smaller buffers need not reduce latency; no profile here assigns the regression to one particular operation. [Design boundary](transport-design-report.md).

[DuckDB's execution-format documentation](https://duckdb.org/docs/2.0/internals/vector) describes dictionary vectors as a child vector plus selection indices and a unified consumer view over encodings. [Velox's vector documentation](https://facebookincubator.github.io/velox/develop/vectors.html#dictionary-vector-scalar-types) describes shared indices across columns, join fanout, and separate null handling. These motivate a genuine source-row selection experiment distinct from per-cell indirection.

[Velox's dictionary guide](https://facebookincubator.github.io/velox/develop/dictionary-encoding.html#decodedvector) discusses the cost of recursive lookups and consolidating mappings in reusable decoded storage. Its [DictionaryVector implementation](https://github.com/facebookincubator/velox/blob/main/velox/vector/DictionaryVector.h) separates base, indices, nulls, and lazy loading. The corresponding design inference here is bounded direct reconstruction with explicit source lifetime, not adopting live value-vector ownership. These precedents support the experiment's structure; they provide no performance evidence for this RDF4J patch.

## Workload sweep

The exploratory screen completed **56 fresh JVM forks**: 14 nondefault cases, two OFF/DENSE pairs per case, alternating order, three warmups, and three measurements. The frozen binary, fixed 1 GiB heap, G1, and normal AUTO policy match the main experiment. All 336 recorded queries passed the applicable result and ledger checks, with no excluded fork or omitted selection event. Full queries use the complete independent bag checksum; partial queries use prefix membership/multiplicity because output order is unspecified. Retained rows are read after both result and connection close. [Configuration](workload-screen/configuration.json); [independent audit](workload-screen/independent-analysis.md).

The following cases have matching, stable measured tiers. All use compiled IR except the one-row early close, which is interpreted in both arms. Values are medians of the **two paired DENSE/OFF ratios**, with their full paired ranges; these ranges are descriptive, not confidence intervals.

| Case | Time ratio, median (range) | Allocation ratio, median (range) | Qualification |
| --- | ---: | ---: | --- |
| Many measurements | 1.2669 (1.2419–1.2919) | 0.8495 (0.8373–0.8616) | M=1000, R=1; 1,152,000 rows. |
| Width 1 | 1.0746 (1.0478–1.1013) | 1.0000 (0.9999–1.0001) | No DENSE coordinate owner observed. |
| Width 4 | 1.1768 (1.1403–1.2133) | 1.0000 (1.0000–1.0001) | Coordinate transport admitted. |
| Low fanout | 1.3586 (1.3508–1.3664) | 0.8174 (0.8166–0.8182) | One status; 576,000 rows. |
| High fanout | 1.3642 (1.2773–1.4511) | 0.9023 (0.9016–0.9030) | Eight statuses, M=25; 1,152,000 rows. |
| One payload value | 1.2951 (1.2037–1.3864) | 0.8529 (0.8434–0.8624) | Separate literal-payload query family. |
| 16 payload values | 1.2474 (1.1832–1.3116) | 0.8166 (0.7885–0.8447) | Same payload-family graph shape. |
| 1,000 payload values | 1.3873 (1.3496–1.4251) | 0.8439 (0.8435–0.8444) | Same payload-family graph shape. |
| Early close after 1 row | 1.1528 (1.0097–1.2958) | 1.0220 (0.9793–1.0647) | Interpreted IR throughout. |
| Retain 4,096 rows | 1.2951 (1.2630–1.3271) | 0.8375 (0.8374–0.8375) | Full 1,152,000-row result; retained reads after closure. |
| 1 MiB ledger | 1.4146 (1.4071–1.4221) | 0.8373 (0.8373–0.8374) | Both global and per-query reservation limits constrained. |

Every paired latency estimate in this subset favors OFF. That includes width 1, where the observation cannot be attributed to copying an admitted output-coordinate carrier because none opened. At width 4, DENSE reduces instrumented lazy calls from 4,101 to 1,104 but produces essentially no allocation saving. Wider fully consumed queries retain material allocation reductions across these repetition, fanout, and payload cases; the screen establishes no monotonic relationship between payload cardinality and savings. [Raw paired summary](workload-screen/summary.json); [counter and phase audit](workload-screen/independent-analysis.md).

Under the **1 MiB reservation limit**, DENSE's tracked peak is **1,047,616 bytes**, below the 1,048,576-byte limit, versus 2,432 bytes for OFF. DENSE remains compiled, admits one carrier, records no initial/growth refusal, and retains the 1,147 versus 2,770,101 lazy-call difference. It therefore measures admitted transport under a constrained reservation ledger with matching observed native operator selections and compiled-kernel binds. The heap remains 1 GiB; a 1 MiB ledger is not a 1 MiB heap cap. All observed ledgers return to zero. [Constrained-memory evidence](workload-screen/independent-analysis.md).

Projection width changes the projected variable mix, not just row byte width. The payload family adds a mandatory pattern and substitutes a literal for the reading IRI. High fanout also changes measurement count to hold output cardinality constant; the many-measurements case changes M and R together. These cases locate behavior in specific fixture families, rather than a universal crossover threshold or a strict one-factor isolation of every parameter. [Fixture definitions](EXPERIMENTS.md).

### Short queries: three separate protocols

Three screen cases contained measured tier transitions: `small` fully consumes 1,152 rows; `early-1024` closes the default large result after 1,024 rows; `retained-small-all` retains and reads all 1,152 small-case rows after closure. The original screen, a **32-warmup/7-measurement** confirmation, and a **512-warmup/101-measurement** extension remain separate experiments. The latter two use four matched pairs per case. All measurements in each fork contribute to its median; no favorable protocol or pair replaces another.

| Protocol | Case | DENSE/OFF time, median (paired range) | DENSE/OFF allocation, median (paired range) |
| --- | --- | ---: | ---: |
| Screen: 3 warmups / 3 measurements, 2 pairs | Small | 0.60884 (0.4883–0.7294) | 0.73120 (0.7082–0.7542) |
| Screen: 3 / 3, 2 pairs | Early 1,024 | 1.33561 (1.1756–1.4956) | 1.10147 (1.0900–1.1129) |
| Screen: 3 / 3, 2 pairs | Retained small | 1.23102 (1.0315–1.4305) | 1.00789 (0.9398–1.0760) |
| Confirmation: 32 / 7, 4 pairs | Small | 1.01954 (0.96692–1.29094) | 1.01005 (1.00809–1.01309) |
| Confirmation: 32 / 7, 4 pairs | Early 1,024 | 0.88924 (0.59631–1.32299) | 1.01418 (1.01228–1.02118) |
| Confirmation: 32 / 7, 4 pairs | Retained small | 1.38477 (0.45198–1.58844) | 1.00903 (1.00214–1.01453) |
| Extended: 512 / 101, 4 pairs | Small | 1.09047 (0.93238–1.37620) | 1.00792 (1.00407–1.01126) |
| Extended: 512 / 101, 4 pairs | Early 1,024 | 0.84768 (0.72097–1.01756) | 1.02627 (1.00876–1.03973) |
| Extended: 512 / 101, 4 pairs | Retained small | 1.06650 (0.93795–1.12853) | 1.00827 (0.99973–1.00974) |

The screen's apparent small-query speedup coincides with mixed interpreted/compiled execution and one mismatched tier-frequency pair. Early-1,024 also has mismatched tier frequencies; retained-small has promotion during measurement even where histograms match. Both longer protocols have matching observed native AUTO operator selections and compiled-kernel bind frequencies throughout their measured blocks. DENSE opens one carrier per measured query with no recorded initial/growth admission refusal. Nevertheless, total-time ratios cross parity between fork pairs in every short case under both longer protocols. The screen's large apparent small-case allocation benefit does not persist. [Screen audit](workload-screen/independent-analysis.md); [confirmation audit](short-query-confirmation/independent-analysis.md); [extended audit](short-query-extended/independent-analysis.md).

The extension's independent audit validates **14,712 queries**, including **2,424 measured queries**, with no missing/excluded sample, checksum or prefix/retention discrepancy, omitted observation, or nonzero ledger endpoint. All 12 measured route/tier histograms match within pairs. The 101 measurements within one JVM are not 101 independent fork replicates. Cumulative warmup allocation exceeded the heap capacity several times in each fork, but that exposure does not measure GC count/duration or establish convergence of every JVM optimization. [Extended raw-record and chronology audit](short-query-extended/independent-analysis.md).

Phase variability matters. In the 32/7 confirmation, retained-small's evaluation ratios span **0.57352–1.57745**, and its consumption ratios **0.59253–1.68812**. Early-1,024 evaluation spans **0.48067–1.06881**, while consumption spans **0.80448–1.60669**. Matched compiled-route counters therefore do not isolate all of the total-time spread to transport. [Confirmation phases](short-query-confirmation/independent-analysis.md).

The extended run reduces absolute query times but still produces the following phase ratios, each calculated within matched rounds:

| Extended case | Prepare, median (range) | Evaluate, median (range) | Consume, median (range) |
| --- | ---: | ---: | ---: |
| Small | 1.01972 (0.85375–1.19964) | 0.99153 (0.85770–1.25118) | 1.24140 (1.07311–1.41249) |
| Early 1,024 | 0.86352 (0.78481–0.93533) | 0.79897 (0.66924–0.94598) | 0.99995 (0.77840–1.16093) |
| Retained small | 1.00244 (0.88782–1.06445) | 0.95125 (0.85370–1.02393) | 1.22962 (1.08631–1.26528) |

The favorable extended early-1,024 total median accompanies lower preparation/evaluation costs, while its median consumption ratio is effectively one and individual consumption ratios favor both arms. It is an observed result of this protocol, with three of four total-time pairs favoring DENSE, rather than an isolated proof that coordinate copying accelerates partial consumption. Small and retained-small consumption is slower with DENSE in every extended pair; retained-read work after closure has its own timing. The short-query evidence does not establish a consistent overall latency advantage or penalty across these workloads. Longer warmup alone does not prove all JVM or scheduling effects settled, and no cause for the residual phase variation is claimed. [Extended paired phases and chronology](short-query-extended/independent-analysis.md); [raw summary](short-query-extended/summary.json). The [combined comparison CSV](comparison-summary.csv) retains each completed experiment and case separately without pooling protocols.

## Fresh parent comparison

The supplemental cross-source run completed **eight JVM forks**, four matched pairs with alternating order, five warmups, and five measurements per fork. The fresh parent's source and diagnostic overlay are identified above. Current OFF uses the same frozen repaired implementation as the coordinate experiments. Input/query digests, expected results, Java, heap, G1, processor count, and ledger limits match; ownership, result handling, reuse, and other source changes differ. The parent retains its format-1 harness and unavailable coordinate diagnostics, while current format 2 adds diagnostic extraction outside the timed interval. [Configuration](fresh-parent-comparison/configuration.json); [independent comparison audit](fresh-parent-comparison/independent-analysis.md).

| Metric | Fresh parent, descriptive median | Current OFF, descriptive median | Median paired current/parent ratio | Paired range |
| --- | ---: | ---: | ---: | ---: |
| Total time | 4.748691 s | 4.395400 s | **0.930083** | **0.907870–1.018019** |
| All-thread allocation | 3.742497 GB/query | 1.630705 GB/query | **0.435725** | **0.435711–0.435741** |
| Lazy-value calls | 13,824,000/query | 2,770,101/query | — | — |
| Tracked ledger peak | 0 bytes | 2,432 bytes | Not comparable as a footprint ratio | — |

The paired estimator gives **6.99% lower latency** and **56.43% less allocation** for current OFF. Three latency pairs favor current OFF; the fourth is **1.80% slower**. Allocation reduction is consistent across all four pairs and is stronger evidence than the modest, nonuniform timing change. Descriptive arm medians are not divided to produce the paired statistic. These observations cannot be assigned solely to weak reuse or solely to ownership repairs. [Raw paired summary](fresh-parent-comparison/summary.json).

All 80 recorded queries, including warmups, pass row/checksum and ledger checks; all 40 measurements have the same normal `ROW → irKernel` selection and one compiled bind, with zero interpreted/aggregate binds. No sample or fork is excluded. Public prepare medians are 2.743/2.670 ms for parent/current, evaluate 20.626/20.799 ms, and consumption 4.7223/4.3702 seconds. Consumption dominates; its paired ratio is 0.930408, ranging from 0.906796 to 1.016129. [Route, phase, and validation audit](fresh-parent-comparison/independent-analysis.md).

The parent's zero tracked peak does not mean zero memory use. Reservation coverage changed between sources, so these ledger values are not comparable total footprints, retained heap, LMDB maps, or RSS. Both observed ledgers return to zero after each query. The reconstruction is useful supplemental evidence about the whole source-version change under matched current settings, and remains separate from both the same-ownership transport ablation and the unrecovered historical baseline/Stage-2 measurements.

## Final verification and deliverable reconciliation

The final 484-class gate ran after the two last fixture corrections, with the declared source hashes unchanged from start to finish. It passed 6,469 cases and recorded twelve existing skips. Copyright checks preceded the final formatter-enabled official install; formatting changed no files. The final production output is byte-identical to the frozen benchmark, while eleven compiled test files reflect the two documented fixture corrections. The benchmark and delivered-source manifests are preserved separately, including the reversible test-only delta. A post-timing pre-delivery check again verified all 2,057 declared source files, the original branch/HEAD, and a clean tracked whitespace check. [Final gate](final-validation/normal-heap-verified/results.json); [production/test comparison](final-validation/delivered-bytecode-comparison.json); [official follow-up](final-validation/generic-root-diagnostic/README.md); [pre-delivery check](final-validation/pre-delivery-verification.json).

The selected gate covers the repaired ownership families and broad native strategy/IR behavior. It does not cover the entire repository or every optional strategy harness. Ten pre-existing unconditional strategy-priority disables and two opt-in harnesses account for its skips. The separately excluded telemetry census remains unverified. The earlier reused 2 GiB JVM exhausted heap; the successful normal 4 GiB run and isolated passing class do not establish the cause of that failure. These are coverage and memory limits of the evidence, not silently discarded failures. [Validation audit](ownership-validation-report.md); [skip audit](final-validation/normal-heap-broad/skips-audit.md).

The complete source deliverable includes the superproject's tracked working-tree files, new LMDB source/tests, experiment drivers, reports, and the exact clean theme dependency at `85d93d0e4a64c04809fc6cc17af10d102c988ca6` (314 files). The patch is based on `15fe7d5011`; no commit or push is required to apply or review it. Evidence includes the successful and failed verification stages, raw timing records, commands, source identities, and independent analyses. Source and evidence archives both extract beneath `rdf4j/`, preserving relative report links. Build caches and private Git metadata are excluded. [Theme verification](theme-dependency-availability/verified-installation.json); [selection rules](delivery-selection.json).

The separate measured-runtime archive preserves all 126 original classpath entries for each of the current and fresh-parent implementations. Its readback verified 14,323 file hashes and 252 artifact digests, and a fresh-extraction check verified both rebased classpaths, exact entry order, and unchanged original metadata. The root-classification launcher's exact additional class is supplied separately; it is appended to the current runtime without replacing any measured class. The final add-on readback verified all 38 members and twenty file hashes. Its fresh-extraction exercise validated the 127-entry classpath and new derivative protocol while preserving all 37 original metadata files. These binaries preserve the measured implementation rather than substituting a later rebuild. The Java installation itself is a reproduction prerequisite, not part of those runtime archives. [Runtime archive verification](runtime-delivery/runtime-verification.json); [base extraction verification](runtime-delivery/helper-verification.json); [add-on verification](runtime-delivery/runtime-addon-verification.json); [add-on extraction verification](runtime-delivery/addon-helper-verification.json); [reproduction guide](EXPERIMENTS.md).

The source/evidence packager creates a private Git tree and replays the binary full-index patch into a separate temporary index, requiring exact tree equality. It reads back every archive entry against its SHA-256 manifest and checks that the inputs, repository HEAD, and real index did not change during packaging. Publication occurs only after these checks. `package-summary.json`, the source/evidence manifests, and `SHA256SUMS` beside the archives record the actual delivery verification; the [packager](package-coordinate-deliverables.py) and exact [selection manifest](delivery-selection.json) are included for review.

## Recommendation

**Retain the ownership repairs and use ordinary reuse as the performance baseline.** The repaired OFF configuration cuts allocation by about 56.4% relative to the separately reconstructed parent. Its median paired latency is about 7.0% lower, although one of four pairs is 1.8% slower; the allocation benefit is much firmer than a uniform cross-source latency improvement. The final same-source confirmation, with zero generic outer-root hosts in every fresh JVM, measures **33.6% higher DENSE latency and 16.3% less allocation** on the default fully consumed query. Every DENSE/OFF pair is slower, ranging from 17.3% to 52.8%. The original ablations measured 27.5–30.3% higher latency with similar median allocation savings; they matched native operator/tier observations but did not record outer-root hosting. These separate protocols support a measured tradeoff, rather than an overall runtime optimization.

The experimental patch retains DENSE as the checkpoint's default. To select the measured ordinary-reuse configuration, launch a fresh JVM with:

```text
-Drdf4j.lmdb.resultCoordinateTransport=off
```

The property is read once per JVM. It changes optional coordinate acquisition/cache behavior while preserving ownership, ordinary weak value-reference reuse, and AUTO strategy selection. Based on these workloads, a latency-oriented default should favor OFF; DENSE is a choice for applications that value its allocation reduction enough to accept the observed latency and reservation costs. The short-query experiments do not establish a dependable general crossover. Their favorable early-close median is nonuniform across forks and coincides with preparation/evaluation differences rather than a consistent consumption-phase advantage.

**Do not promote either compact output-window prototype as the solution to the runtime gap.** CHANGES and ROWS reduce aggregate ledger peaks by about 3%, retain similar median allocation savings and the same lazy-value-call reduction, and remain slower than OFF in every round of the root-qualified control. Their precise latency relative to DENSE varies across forks and measured-iteration selection; the new compact/DENSE ranges cross parity. The unusually lower allocation of one DENSE fork also prevents a claim of uniform allocation equivalence. ROWS' default-workload result covers eleven singleton groups, so it does not reject the broader idea of shared selections for coherent multi-column sources. A future sharing experiment needs a workload that actually produces `groups / columns < 1`, together with unchanged ownership checks and observed AUTO routes.

The next useful experiment should separate materializer publication, factor-page population/validation, and upstream coordinate propagation. Compressing the emitted window alone leaves the rest of that work in place. Keep the existing result/closure/budget checks, count actual participation, and rerun the same alternating-round protocol after each isolated change. Avoid weakening provenance or lifetime checks to obtain a faster number.

The available evidence supports that direction for the tested OPTIONAL-fanout family. It does not establish all query shapes, every heap size, a complete strategy census, or the unavailable historical Stage-2 state. The final verification section records the remaining coverage and reused-suite memory limits explicitly.


**Follow-up hypothesis: materializer work beyond output copying.** `NativeResultMaterializer.find()` already checks ordinary last-value reuse before the factor cache. A successful ordinary hit still calls `rememberFactor()` when its row sequence is nonzero; that path can capture/validate provenance, locate or admit a factor page, and refresh page/memo state. Enabled `position()` and `invalidateCoordinates()` also maintain coordinate lifetime per row, including sequence advancement, namespace retirement, activation checks, and memo invalidation. This supports a separate experiment distinguishing output-window copying from factor-page population and validation, while preserving ownership checks and recording actual participation/routes. The measured slowdown has not been attributed to locks or any single method. [Materializer source](../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java).


