# Weak retention in the native lazy-Value memo

## Decision

Keep weak retention as the next experimental candidate, with the feature still disabled by default. It removes the memo's strong ownership of decoded values and revision graphs while preserving the allocation benefit on both tested query fixtures. The matched latency comparison found small median differences between weak and strong retention, with individual pairs going in both directions. It does not establish that weak references are intrinsically faster, that the memo bounds total query memory, or that the current admission policy is ready for production.

The decisive ownership observation is separate from timing: with an open memo and no consumer roots, strong retention kept 64 values carrying at least 128 MiB of decoded payload alive; weak retention kept none. With one consumer root, weak retention preserved exactly that consumer's 2 MiB value, including after memo close. The latency cohort then completed all 36 fresh JVMs and all 468 queries without a result, route, accounting-endpoint or cleanup failure. See [the controlled memory report](diagnostics/MEMORY-RESULTS.md), [the frozen analysis](analysis-v1/RESULTS.md), and [the complete numerical results](analysis-v1/results.json).

Against ordinary reuse, the primary weak-treatment estimate reduced query latency by 4.17% on fanout and 6.15% on shared-tail, and reduced allocation by 16.09% and 14.00%. Every one of the six weak/ordinary latency pairs favored weak retention on each fixture. Against strong retention, median latency ratios were 0.988624 and 0.981591, but the ranges crossed 1. These are descriptive engineering observations from two fixed workloads, not a significance test or a production-promotion criterion.

## What changed and what stayed fixed

The implementation remains an iteration-local direct-mapped memo keyed by the complete value ID and position mask. It does not cache rows, use factorization coordinates, or initialize a lazy value on a miss. The normal source, view, exposure, detachment and ownership paths are unchanged from the preceding direct-memo experiment.

The existing `rdf4j.lmdb.nativeValueMemo.enabled` property remains false by default. A new experimental property, `rdf4j.lmdb.nativeValueMemo.retention=strong|weak`, chooses the retention representation; its default is strong. All three timing arms loaded the same seven-class prefix. Ordinary reuse disabled the memo, while the two enabled arms selected strong or weak retention. The production checkout was not patched for these experiments; the complete source overlay and applicable production patch are supplied separately.

Strong mode stores a Value and an InternalIdentity witness. Weak mode stores a weak Value reference, a primitive revision number and a weak ValueStore witness. It keeps no strong path from an entry to the Value, InternalIdentity, revision or store. Lookup reads the Value reference once into a strong local before validation. A distinct null-owner state prevents a collected non-null owner witness from matching a later null owner. Equivalent identity replacement remains a hit; changed semantic version, ID, revision or owner remains a miss.

The materializer and projected binding-set sources are byte-identical across treatments. The full-key hash, 4,096-slot initial table, 65,536-slot maximum, cumulative growth rules, 8,192-probe first-window cutoff, fallback and close behavior remain fixed. The new strong control shares the weak-capable class's Object array, mode branch and counters; it is a fresh matched control, not a byte-identical replay of an older binary. The weak design review and primary implementation references are in [WEAK-REFERENCE-DESIGN.md](review/WEAK-REFERENCE-DESIGN.md) and [the independent source review](reviews/direct-memo-code-review-v1.md).

## Frozen comparison

The protocol was frozen and published before any primary timing JVM launched. Its SHA-256 is `e151245f589b655d7b2f8240998552486e84836d0895147ab7b8428d7fe111af`. The qualified implementation checkpoint is [9b262140b7e49eedd4521c3521a70938b02722cd](https://github.com/HASMAC-AS/rdf4j/commit/9b262140b7e49eedd4521c3521a70938b02722cd).

The design used all six permutations of ordinary, strong and weak, on both fixtures, reversing fixture order on alternate rounds. Each of 36 fresh JVMs ran eight warmups and five measurements: 288 warmups, 180 measured queries and 468 queries in total. Every process was supervised individually to terminal exit zero before the next began. No failed, partial, skipped or replacement fork exists in this cohort. The runner verified all frozen source, classpath and selected JDK bytes before and after each JVM, outside the query timing window.

Both fixtures used normal AUTO selection and full lexical consumption, with coordinate transport off for all arms. Fanout returned 1,152,000 rows with 12 projected bindings; shared-tail returned 1,048,576 rows with six bindings. Expected checksums were `3123987218479850048` and `8327963502356915200`; shared-tail also checked an independent exact bag. Native execution, measured compiled IR, matching routes and adjacency observations, one memo owner per enabled query, and zero endpoint ledger usage were required.

The runtime was Temurin 25.0.5+8-LTS, with a fixed 1 GiB G1 heap, eight-byte object alignment, compressed object and class pointers, a 512 MiB query ledger and 1 GiB global ledger. Source-cache settings and input/JVM settings were identical across arms. No build, explicit collection, profiler, heap inspection or other heavy experiment ran concurrently with a timing JVM. Natural GC logs cover the complete JVM, including setup and warmup. Historical timings and the two qualification pilots were excluded from every estimate.

The primary estimator takes the median of the five measured query values within each JVM, forms six same-round candidate/comparator ratios per fixture, then reports their median and full range. It describes typical query latency. It is not a throughput or tail-latency estimate. Medians of pairwise ratios need not equal ratios of aggregate medians and are not transitive across three treatments.

## Latency and allocation

A ratio below 1 favors the numerator. All six pairs enter every reported primary estimate.

| Fixture | Comparison | Median latency ratio | Six-pair latency range | Median allocation ratio | Six-pair allocation range |
| --- | --- | ---: | ---: | ---: | ---: |
| Fanout | Weak / strong | 0.988624 | 0.908447–1.088229 | 1.001065 | 1.000918–1.035967 |
| Fanout | Strong / ordinary | 0.935842 | 0.854843–1.041044 | 0.838262 | 0.809988–0.862670 |
| Fanout | Weak / ordinary | 0.958302 | 0.851157–0.977661 | 0.839131 | 0.839082–0.863498 |
| Shared-tail | Weak / strong | 0.981591 | 0.812478–1.055481 | 1.006100 | 1.006031–1.006135 |
| Shared-tail | Strong / ordinary | 0.934329 | 0.790926–1.188451 | 0.854790 | 0.854768–0.854816 |
| Shared-tail | Weak / ordinary | 0.938527 | 0.785763–0.977167 | 0.860004 | 0.859964–0.860032 |

Weak retention's small median timing advantage over strong retention is much smaller than the observed pair spread. Two fanout pairs and one shared-tail pair were slower with weak retention. The conclusion supported by these runs is that weak retention preserved useful end-to-end reuse on these fixtures without a large median penalty. A general weak-versus-strong speed claim would exceed the evidence.

After the cohort completed, an independent reviewer also calculated a **post hoc sensitivity analysis** using the arithmetic mean of each JVM's same five measured queries before forming the six paired ratios. This was not part of the frozen primary estimator and does not replace it:

| Fixture | Comparison | Primary ratio, inner median | Secondary ratio, inner mean |
| --- | --- | ---: | ---: |
| Fanout | Weak / strong | 0.988624 | 1.012401 |
| Fanout | Strong / ordinary | 0.935842 | 0.945604 |
| Fanout | Weak / ordinary | 0.958302 | 0.976231 |
| Shared-tail | Weak / strong | 0.981591 | 0.980153 |
| Shared-tail | Strong / ordinary | 0.934329 | 0.937626 |
| Shared-tail | Weak / ordinary | 0.938527 | 0.902513 |

Fanout's weak/strong direction changes from 1.14% faster to 1.24% slower. The mean-based weak/ordinary aggregate remains below 1, but its fifth round is 1.060929, so the primary all-six improvement pattern is not robust to that summary choice. This reinforces the conclusion that the ownership benefit is established more clearly than a latency ranking of weak and strong retention. See [the independent audit report](reviews/raw-comparison-audit-v1.md) and [its numerical evidence](reviews/raw-comparison-audit-v1.json).

The additional allocation was measurable: the median weak/strong increase was 0.1065% on fanout and 0.6100% on shared-tail. Fanout allocation varied across forks: one weak/strong pair was 3.5967% higher, so the median must not be presented as a universal per-query overhead. Caller-thread allocation shows the same spread (weak/strong range 1.000929–1.035950), so the effect cannot be explained solely by allocating on other threads. Identical resolver and memo-probe counts do not identify the cause of those allocation differences. No profiling was performed to attribute them to JIT decisions, wrapper escape behavior or another mechanism.

The following absolute values are medians of the six JVM medians. Decimal GB means 1,000,000,000 allocated bytes. Allocated bytes measure allocation during the harness query window; they are not retained heap or LMDB's native mapped memory.

| Fixture | Arm | Query seconds | Allocated GB | Preparation ms | Query ledger peak bytes | Source lazy-value calls |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Fanout | Ordinary | 3.523619 | 1.630367272 | 1.837997 | 2,432 | 2,770,101 |
| Fanout | Strong | 3.232626 | 1.366672540 | 2.138138 | 147,536 | 33,386 |
| Fanout | Weak | 3.282007 | 1.368056908 | 2.440059 | 606,448 | 33,386 |
| Shared-tail | Ordinary | 2.465851 | 1.320732948 | 1.259567 | 1,472 | 2,113,601 |
| Shared-tail | Strong | 2.396675 | 1.128937688 | 1.126407 | 147,536 | 172,090 |
| Shared-tail | Weak | 2.259718 | 1.135815572 | 1.259591 | 606,448 | 172,090 |

Preparation remains on a millisecond scale, while full consumption takes seconds. Fanout's weak/ordinary preparation ratio was 1.238381, with all six preparation ratios above 1; its absolute preparation median was 2.440 ms versus 1.838 ms. Shared-tail's weak/ordinary preparation ratio was 0.882091, with a range crossing 1. These small phase timings do not justify a general startup-cost conclusion. The JSON preserves evaluate, first-row, consume and close times as well as both caller-thread and all-thread allocation.

## Reuse, collection and accounting

All 30 measured enabled queries per fixture and mode had identical source-call and hit counts within that fixture. Fanout made 13,824,000 memo probes with 13,790,614 hits, a 99.758492% hit rate. Shared-tail made 6,291,456 probes with 6,119,366 hits, a 97.264703% hit rate. Both treatments stayed at 4,096 slots. No growth, cutoff, semantic invalidation, unsupported-value decline or reservation refusal occurred in the timing cohort.

| Measured per-query observation | Fanout, strong | Fanout, weak | Shared-tail, strong | Shared-tail, weak |
| --- | ---: | ---: | ---: | ---: |
| Source lazy-value calls | 33,386 | 33,386 | 172,090 | 172,090 |
| Weak entry allocations | 0 | 33,386 | 0 | 172,090 |
| Weak owner-reference allocations | 0 | 1 | 0 | 1 |
| Encountered collected entries | 0 | 0 | 0 | 3–14 |
| Array reservation peak | 147,536 | 147,536 | 147,536 | 147,536 |
| Total memo reservation peak | 147,536 | 606,448 | 147,536 | 606,448 |
| Occupied slots at close | 981 | 981 | 4,096 | 4,096 |

Across 30 measured shared-tail weak queries, there were 236 encountered collected entries. This counter is checked before key comparison, so it includes cleared slots that otherwise would have been different-key collisions. In these records, `collisions + collectedMisses` equals the strong collision count, and the total hit and source-call counts are unchanged. These observations demonstrate natural collection of some memo referents; they do **not** demonstrate additional resolver work caused by collection of a still-useful same-key entry. The first progress observation of collection should be read with this qualification. The timing cohort does not yet stress costly GC-driven rematerialization.

The independent audit found one warmup exception: in the fourth round's shared-tail query at iteration -7, weak retention made 172,091 source calls versus 172,090 for strong, with one fewer hit. Its 14 collected-entry encounters accompanied 13 fewer collisions. All other 155 strong/weak query pairs had equal source-call and hit counts and reconciled collected encounters exactly to the collision difference. Across warmups and measurements together, shared-tail weak mode encountered 633 collected entries. Warmup values remain excluded from the primary estimates; this single extra call is evidence that same-key loss can occur, not a measurement of its cost at scale.

Occupied weak slots are metadata occupancy, not a census of live Values. The source and retained consumers can independently own the referent. These distinctions also explain why reduced resolver calls cannot be translated directly into a claim of one decode per distinct ID.

Strong mode reserves `80 + 36N` bytes for its five arrays: 147,536 bytes at 4,096 slots. It still excludes strongly retained values, payloads and revision graphs. Weak mode adds `112N + 160` bytes for conservatively estimated table-owned weak metadata, including an independent last-owner witness and replacement overlap: 606,448 bytes initially. Growth reserves new storage before releasing the old reservation. A 500,000-byte ledger refused the weak initial reservation in the contracts and fell back without a leaked reservation.

The weak reservation bounds the declared table-owned metadata under the pinned layout assumptions. It does not bound unreachable garbage awaiting collection, VM reference processing, consumer-retained rows, source caches, ledger/memo object overhead, native memory or the complete query heap.

Whole-JVM GC medians are reported below as supporting observations. They include fixture setup and all eight warmups, so they cannot be attributed solely to the five measured queries. Concurrent-cycle time overlaps application work and is not added to pause time.

| Fixture | Arm | GC pause count | GC pause ms | Completed concurrent cycles | Concurrent-cycle ms |
| --- | --- | ---: | ---: | ---: | ---: |
| Fanout | Ordinary | 37.5 | 267.959 | 1 | 33.918 |
| Fanout | Strong | 32 | 254.358 | 1 | 50.570 |
| Fanout | Weak | 33.5 | 247.317 | 1.5 | 65.275 |
| Shared-tail | Ordinary | 31.5 | 319.118 | 1 | 61.168 |
| Shared-tail | Strong | 28 | 286.656 | 1 | 36.175 |
| Shared-tail | Weak | 29 | 297.912 | 1 | 55.535 |

## Controlled retention and source-cache evidence

The four diagnostic JVMs deliberately used explicit collection; their timings never entered the latency cohort. A synthetic uncached source produced 64 distinct 2,097,152-character ASCII payloads, with only weak observers outside intentional consumers. Each run performed 128 probes and 64 hits, then checked six lifecycle points with an independent collection control. All 24 checkpoints passed.

At the checkpoint where the memo remained open and consumer roots had been released:

| Treatment | Consumer-held values | Live values | Logical live payload lower bound | Whole-process heap after GC | Memo reservation |
| --- | ---: | ---: | ---: | ---: | ---: |
| Strong | 0 | 64 | 128 MiB | 209,516,768 bytes | 147,536 bytes |
| Weak | 0 | 0 | 0 | 8,185,088 bytes | 606,448 bytes |
| Weak | 1 | 1 | 2 MiB | 11,330,904 bytes | 606,448 bytes |
| Strong | 1 | 64 | 128 MiB | 209,516,960 bytes | 147,536 bytes |

After close, every ledger reservation was zero and no unowned values remained. A retained consumer value remained readable after close in both modes; after that consumer's final release, all value, payload, revision and canary observers cleared. Whole-process heap includes other objects and collector effects. Its difference is not a dominator-tree measurement of memo-exclusive retained bytes. The known payload sizes and weak-observer counts establish the ownership result.

A separate real ValueStore diagnostic reopened a store with unchanged default cache settings. Decoded IRI representatives and their payloads at 1,024 and 1,048,576 characters remained source-cache-owned. A 1,048,577-character representative was excluded from that cache and became collectible while the store stayed open. That is why both the isolated diagnostic and unchanged-cache query comparison were necessary: weakening one owner cannot release a value still owned elsewhere.

## Verification, preserved failures and scope

The fresh baseline passed 106 focused tests. Test-first evidence then recorded the smallest new retention contract failing as intended (one test, one failure, zero errors/skips), followed by all eight new contracts with exactly five expected failures. After implementation, the same eight-case selection passed. Final runtime qualification passed 193 test executions: 138 default/strong cases, 47 weak cases and eight enabled strong lifetime cases, with zero failures, errors or skips. Both interpreted and compiled IR covered retained rows, early close, cancellation and exact bags. The real-source cache diagnostic added one passing test; the four memory JVMs added 24 checkpoints; the two full-fixture pilots added 26 checked queries and owners. Pilot timings were excluded.

The first post-change default qualification was misconfigured with coordinate transport forced off, causing 38 coordinate assertions to fail among 138 tests. The 32 direct-table/integration cases passed. The failed attempt is preserved as `qualified-default-v1`; the correction changed only the runner configuration, and `qualified-default-v2` passed all 138 cases. No assertion or implementation was changed to make that selection pass.

The offline and required online root build attempts compiled and installed LMDB successfully. The full reactor remained blocked in unrelated Spring modules by unavailable `org.jetbrains.kotlin:kotlin-stdlib:2.3.21`; the online retry failed Maven Central DNS. This work does not claim a green full reactor or exhaustive validation of the entire earlier ownership migration. The isolated copy-only formatting apply/check passed. The final seven class files are byte-identical before and after the Javadoc formatting change; the sealed retention test remains unchanged, with its whitespace-only formatted review copy supplied separately.

Surefire snippets, commands, failures and receipts are preserved in [initial-evidence.txt](../../initial-evidence.txt), [the ExecPlan](EXECPLAN.md), and the `contracts/validation` directories. The [qualification and delivery review](reviews/qualification-delivery-review-v1.md) independently reconciles the counts and artifact hashes. The primary audit completed all 36 slots and 468 query-pair checks. The frozen analyzer independently read the sealed raw outputs and produced [analysis-v1/results.json](analysis-v1/results.json). A second raw-data audit reproduced the estimates without importing either analyzer's summaries and confirmed matching routes across every round: at each query ordinal, all 18 JVMs for a fixture had the same route; the first warmup used the interpreted retry path and the remaining 12 queries used compiled IR. No cross-round route variation explains the paired timing spread.

## What remains worth testing

The next decisive experiment is a reuse-distance and admission test with weak retention held fixed. Sweep the first repeat across the 8,192-probe cutoff and repeat distances across the 4,096–65,536-slot range. Include values outside the shared source-cache size boundary and enough ordinary allocation to create natural GC between reuses. Record whether a useful key was collected, evicted, invalidated or bypassed after admission shutoff. This will distinguish the known permanent-cutoff problem from a real cost of weak retention.

Use the present fixed admission policy as the control before changing it. A later treatment can test periodic reconsideration or a bounded sampling window, but changing admission and retention together would obscure the result. Preserve normal AUTO selection, matched ownership code, fresh contexts on replay, alternating order, checksums, preparation, allocation and ledger endpoints. Extend the same comparison to early close, retained rows and constrained budgets. Existing lifetime and reservation-refusal contracts cover selected correctness cases; constrained-memory query behavior and performance still need a dedicated sweep. The primary performance cohort covers only full consumption of two fixed fixtures.

There is no evidence here for choosing a universal cache size, enabling the experiment by default, or removing the semantic/identity checks to save CPU. The ownership result supports continuing with weak retention; the remaining uncertainty is which reuse patterns justify its metadata and lookup cost.

## Complete deliverables and replay

- [Complete source archive](artifacts/complete-source-v1.tar.gz): 59 hashed payloads plus manifest, including all four production files, contracts, diagnostics, runners and required historical harness sources.
- [Production patch](artifacts/complete-native-value-memo.patch): exactly the four production-file changes, applicable to the unmodified checkout at the stated prerequisite commit.
- [Archive README](artifacts/complete-source-v1.README.md), [source manifest](artifacts/complete-source-v1.manifest.json) and [verification receipt](artifacts/source-delivery-verification-v1.json): prerequisite checkout/toolchain requirements and complete read-back verification.
- [Exact seven-class prefix](artifacts/retention-v2-prefix.tar.gz) and [prefix receipt](artifacts/prefix-receipt.json): the selected compiled overlay, not a claim to contain the complete dependency runtime.
- [Frozen analysis](analysis-v1/RESULTS.md), [all numerical results](analysis-v1/results.json), [memory observations](diagnostics/MEMORY-RESULTS.md), and raw per-JVM files under `comparison-v1`.
- [Independent audit source](reviews/raw-comparison-audit-v1.py) and [its numerical results](reviews/raw-comparison-audit-v1.json): complete post-study verification code, including the separately labeled inner-mean sensitivity. This later audit source is supplied alongside the pre-timing source archive.

Archive SHA-256: `9b324c4b55c68e45eefc60a899c957e672491d015dcf633ad4c49b933bd1aae7`.

Patch SHA-256: `3e17b818d384dcacf8c36d507cc670b8ef8609b9e4f18e1ce59596929b217c9b`.

The complete source archive requires the full RDF4J checkout, the selected JDK/Maven and dependencies. It contains no dependency jars or benchmark database. All 60 archive files were extracted and read back; manifest hashes matched, `git apply --check` passed, and strict in-memory patch reconstruction reproduced all four final runtime sources. Raw large pre-timing evidence, including the exact protocol, is preserved losslessly in `recovery/preserved-large-v1`; restore missing originals with:

```bash
python3 result-coordinate-evidence/retention-followup/recovery/preserve_evidence.py restore \
  --archive result-coordinate-evidence/retention-followup/recovery/preserved-large-v1
```

The restore verifies compressed and raw hashes and refuses to overwrite different bytes. Exact original classpath binaries are separate from source recovery. A rebuild without those original inputs requires a new runtime identity and a new comparison protocol; it must not be relabeled as this completed cohort.
