# Publication omission under natural weak-reference collection

This document specifies a new prospective experiment. The executable freeze operation will write a separate immutable `protocol.json` only after the restored sources, contracts, wrapper, and runtime have qualified. No timing JVM is authorized by this document alone; the coordinator releases the serial execution slot after qualification and freeze.

## Question and outcome

Does omitting factor-cache publication for the previously selected bindings reduce total query cost under normal garbage collection? The primary comparison is DENSE omit divided by DENSE all. The same ownership implementation, input, JVM options, original dense-page representation, and normal AUTO strategy apply to both arms. OFF provides a contemporaneous ordinary-reuse reference.

The outcome includes any change in weak-reference availability and resolver work caused by omission. Raw resolver differences do not disqualify a query. No estimated resolver cost is subtracted from latency or allocation, and no resolver-minus-weak residual is used as a qualification gate. A weak miss is an observed event, not a reason to replace a query or select a favorable tail.

The previous nine-JVM timing prefix remains incomplete. Its separate four-JVM diagnostic probe established that a terminal factor weak miss can add a resolution while retaining correct results and cleanup. That observer does not retrospectively identify the earlier timing event, and neither dataset enters this experiment's estimates.

## Fixed design

The experiment uses three arms: OFF with all publications, DENSE with all publications, and DENSE with the selected publications omitted. Fanout omits `measurement,reading`; shared-tail omits `l0,l1`. The masks are frozen from earlier diagnostic evidence. They are fixture-specific experimental interventions, not proposed production rules.

Each fixture uses each of the six arm-order permutations once, in this order:

| Round | First fixture | Arm order within each fixture |
|---|---|---|
| 1 | Fanout | OFF, DENSE all, DENSE omit |
| 2 | Shared-tail | OFF, DENSE omit, DENSE all |
| 3 | Fanout | DENSE all, OFF, DENSE omit |
| 4 | Shared-tail | DENSE all, DENSE omit, OFF |
| 5 | Fanout | DENSE omit, OFF, DENSE all |
| 6 | Shared-tail | DENSE omit, DENSE all, OFF |

There are exactly 36 fresh JVMs. Each executes eight warmup queries followed by five measurements, yielding 468 queries: 288 warmups and 180 measurements. Each process gets a fresh store and output directory and a 300-second deadline. JVMs run serially; builds, tests, profiling, compression, and optional heavy hashing do not overlap timed runs. A process or qualification failure preserves the attempted prefix and ends the study. There are no replacements, exclusions, extra rounds, forced routes, or post-hoc changes to the acceptance screen.

JDK `25.0.5+8-LTS`, `-Xms1024m -Xmx1024m -XX:+UseG1GC`, hot counters enabled, a 1 GiB global memory ledger, and a 512 MiB per-query ledger match the preserved experiment. Native evaluation and direct adjacency remain enabled with PREFER selection; value overlay and force sync are disabled. Indexes are `spoc,ospc,psoc,posc`. Fanout retains its original AUTO direct-adjacency budget; shared-tail retains its original explicit 1 GiB adjacency budget. All arms match within each fixture.

Fanout uses 100 measurements, 10 readings, projection width 12, two statuses, and no unique payloads. It fully consumes 1,152,000 rows with expected checksum `3123987218479850048`. Shared-tail uses 64 roots, 128 left and 128 right choices, and tuple width two; its six-column projection fully consumes 1,048,576 rows with expected checksum `8327963502356915200`. Graph, insertion, query, projection, and oracle identities are copied unchanged into the frozen protocol.

## What the observer records

The original 15 common harness class files remain unchanged. A new launcher allocates a fixed primitive buffer for thirteen materializer lifetimes before delegating to the exact prior root-qualified launcher. The materializer copies scalar observations into that buffer at idempotent close. Only after the original launcher returns does the adapter create report objects and write `miss-accounting.jsonl`.

The timed paths gain counters only for rare weak-reference failures, witness rejection, abandon or repair, admission refusal, and owner lifecycle boundaries. There are no page or target snapshots, no successful-hit or remember counters, no additional weak-reference dereference, and no retained Values, rows, sources, or coordinates. The observer adds work and primitive storage to every arm; event-dependent writes may differ by treatment. Measurements therefore describe this common observed runtime. They do not measure the observer's cost or an uninstrumented weak-miss rate. The existing ledger continues to describe the engine's admitted query state; observation metadata is experimental bookkeeping.

Each exported owner has sixteen scalar fields:

| Field | Meaning |
|---|---|
| `ownerOrdinal` | Opening sequence number for the materializer |
| `compileOrdinalAtOpen`, `compileOrdinalAtClose` | Existing cumulative semantic native compile counter at lifecycle boundaries |
| `lazyValueCallsAtOpen`, `lazyValueCallsAtClose` | Existing cumulative lazy resolver counter at lifecycle boundaries |
| `ordinaryWeak` | Ordinary weak-reference dereferences that returned null |
| `ordinaryWeakRecovered` | Those ordinary failures followed by a successful factor lookup |
| `ordinaryWeakTerminalOther` | Those ordinary failures followed by a terminal factor failure other than a factor weak failure |
| `jointWeakTerminal` | A single lookup with both ordinary and terminal factor weak failure |
| `factorWeakTerminal` | Terminal factor weak dereferences that returned null, including the joint subset |
| `ordinaryWitnessFailures`, `factorWitnessFailures` | Cached value rejected by its source or value witness |
| `abandonCalls` | Existing materializer abandon calls, including provisional-hit repair or failed capture rollback |
| `ordinaryAdmissionRefusals`, `factorMemoAdmissionRefusals`, `factorPageAdmissionRefusals` | Failed memory claims for the three recorded materializer allocation categories |

The ordinary weak categories are mutually exclusive and exhaustive:

    ordinaryWeak = ordinaryWeakRecovered + ordinaryWeakTerminalOther + jointWeakTerminal
    jointWeakTerminal <= factorWeakTerminal
    terminalWeakUnion = factorWeakTerminal + ordinaryWeakTerminalOther

An ordinary failure recovered by a factor hit does not cause a resolution. A joint weak failure causes only one fallback, so adding ordinary and factor weak counts would double count. These fully bound unaliased fixtures require `terminalWeakUnion <= raw lazyValueCalls`. This is an event-accounting bound, not a causal decomposition of execution time.

Classification occurs inside the existing ordinary-null branch and materializer monitor, around its single factor lookup. It uses the per-instance terminal-factor counter and the returned value. Java monitors permit reentrancy; this method is therefore scoped to these serial native fixtures, whose callbacks do not reenter the same materializer. The observer is not claimed to be a general reentrancy-safe API. Source review and counter contracts establish that scope before timing.

## Qualification

Every query must pass its original result and consumption checks. Shared-tail additionally requires its independent exact-bag oracle. Every query must record the complete expected bound-value read count, zero ledger usage before and after execution, released query memory, valid allocation observations, normal AUTO, one native IR result, and active adjacency at the same revision. Measured queries must use compiled native IR. Root audits require thirteen semantic native compiles and zero hosted generic roots per JVM.

Exactly thirteen accounting records must be exported without overflow. Owner ordinals and both compile-ordinal columns must each be `1..13`. For every record, the difference between its lazy-value close and open counters must equal the original query's raw resolver count; neighboring owner intervals must meet without a resolver gap. These checks establish the one-owner-per-query alignment rather than inferring it from an equal total count alone.

The weak partition and union bound must hold exactly. Ordinary and factor witness failures, abandon or repair, and all three materializer refusal counts must be zero. The original coordinate counters must separately show zero initial and growth admission refusal. At each same-round DENSE query iteration, route, tier, and complete adjacency observations must match. Raw resolver counts may differ by any amount consistent with the gates; there is no tolerance selected from the prior two diagnostic events.

Source and compiled identity are checked before and after the entire run. The new prefix contains exactly `NativeResultMaterializer.class`, `PublicationQualifiedAccounting.class`, `PublicationQualifiedAccounting$Snapshot.class`, and `PublicationQualifiedLauncher.class`, ahead of the exact preserved 129-entry publication runtime. Its plan, nested page and memo classes, fifteen harness classes, and all remaining runtime entries are frozen. The protocol pins the driver, independent analysis script, identity helper, runtime descriptor, source files, validation receipt, and selection evidence before timing starts.

## Estimation and engineering screen

Each fresh JVM contributes the median of all five measured queries. Each ratio pairs the same fixture and round. Report the median and complete range of all six paired ratios for each comparison and metric. Fixtures are never pooled, and query samples are not treated as independent JVM replicates.

Report total latency, all-thread and caller-thread allocation, preparation, evaluation, time to first row after evaluation, consumption, close, ledger peaks, raw resolver counts, weak-event partitions, and actual execution routes. Report the first query separately as a descriptive cold observation after fixture construction; it excludes JVM launch and fixture setup. Keep all eight warmup records.

The predeclared primary screen requires all of the following separately for each fixture:

- Median DENSE omit / DENSE all total latency ratio at most 0.95.
- Every one of the six paired latency ratios strictly below 1.0.
- Median all-thread allocation ratio at most 1.0.
- Every paired all-thread allocation ratio at most 1.02.

These are engineering thresholds, not a significance test, population confidence interval, or permission to promote a target-name mask to production. A completed study that misses the screen remains a valid completed measurement. Full consumption at 512 MiB does not establish behavior for early close, retained rows, other repetitions or projections, different unique-value counts, or constrained-budget latency.

## Freeze and independent audit

Before the timing runtime is frozen, a separate lifecycle preflight runs both full-size fixtures across all three arms, in the fixed order fanout OFF/all/omit followed by shared-tail OFF/all/omit. Each of those six fresh JVMs executes eight warmups and five observed iterations, yielding 78 queries and 78 aligned owner records. Its complete protocol and command schedule are written before the first JVM. It checks the same result, route, compiled-tier, adjacency, accounting, and cleanup gates as the primary study. Raw timing fields remain preserved by the unchanged harness, but this preflight computes no latency or allocation estimates and contributes no samples to the primary comparison.

`run-comparison.py --freeze` requires a validated runtime descriptor and the preserved selection file, writes a new protocol exclusively, and prints its SHA-256 without launching a JVM. `--validate-only` rechecks that frozen protocol and runtime. Execution additionally requires an explicit `--slot-released` argument and a fresh output directory. This coordination flag records process isolation; it does not request new user permission.

The independent script is pinned before the freeze and accepts the protocol hash printed by that freeze as `--expected-protocol-sha256`. After the coordinator releases the post-timing slot, it recomputes results directly from each fork's raw command, process result, samples, owner records, and root audit. It imports no runner code and reads neither `runs.json` nor `summary.json`. Full compiled-byte preservation is also checked by the runtime verifier. No accepted comparison is emitted for an incomplete or unqualified study.
