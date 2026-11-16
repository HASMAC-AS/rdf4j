# Method and correctness evidence

## What this experiment isolates

The question is whether a factor cache should allocate its original five 64-entry arrays for a page that currently contains one member. The preceding shared-tail diagnostic found that most admitted pages had only one stored weak-reference slot. That observation motivates an inline singleton representation, followed by one-way promotion when a second distinct occupied offset is inserted.

This is a factor-cache storage experiment. The name **DENSE** refers to the existing coordinate transport mode. The compact candidate still uses that transport; it does not introduce a new coordinate encoding. Original frame handling, weak-value reuse, detached outward results, mutation checks and the repaired ownership code are held constant.

The three arms are:

| Arm | Coordinate transport | Factor-page implementation |
|---|---|---|
| Current OFF | Disabled; ordinary reuse remains enabled | Preserved current implementation |
| Current DENSE | Existing dense coordinate transport | Original eager page arrays |
| Compact DENSE | Same dense coordinate transport | Inline singleton, then charged dense promotion |

Compact DENSE/current DENSE is the primary representation comparison. Current DENSE/current OFF is a coordinate-ablation comparison with identical ownership and production code. Compact DENSE/current OFF shows the combined tradeoff on the candidate. Earlier measurements from other source snapshots are kept as historical evidence and are not pooled with this study.

## Representation and ownership

A page keeps the same object identity throughout its life. Its key still contains the canonical target, namespace, group, parent, tuple column and long page index. This matters because existing coordinate memos retain the page object itself. The initial page stores its offset, value ID, semantic version, revision ID, revision-owner witness and weak value reference inline.

The compact shell reserves 512 bytes. A second distinct occupied position requests a separate 15,872-byte payload claim before allocating the original arrays. Successful promotion therefore retains the original 16,384-byte total dense allowance. All five arrays are allocated locally, the old complete witness tuple is copied to its original offset, and the arrays are published under the existing materializer lock. The incoming tuple is then inserted. An allocation exception closes the new claim and propagates; this rollback was source-reviewed, not fault-injected.

Promotion uses the existing non-reclaiming reservation path. A refusal leaves the singleton usable and does not poison admissions of other compact pages. The next eligible insertion retries, so a release by another owner can enable recovery. The implementation adds no memory-manager epoch, permanent refusal latch, demotion state or insertion-time weak-reference probe. It reuses an explicitly cleared inline slot without promotion and does not add strong references to result values.

The 512-byte and 16,384-byte charges are conservative accounting allowances. They are not measured object sizes. Removing mostly empty arrays can save allocated bytes, while admitting more pages can add objects and reduce resolver work. Those effects are measured separately from the ledger.

## Test-first verification

The mandatory offline root clean install passed before production edits. The new smallest failing case was then run against unchanged production. Under a 4,096-byte query/global allowance, it accessed A, B and A at positions on separate long-indexed pages. Values remained correct, but the original implementation made three resolver calls, `[42,43,42]`, instead of two. The fresh Surefire result was one test, one failure, zero errors and zero skips. This evidence was archived before the candidate source was changed.

After implementation, formatting, the module/dependency build and the focused correctness selection passed: **108 tests in eight reports, zero failures, errors or skips**. Independent XML reconciliation confirms that the selection retains every one of the prior 103 test identities and adds exactly five materializer cases. The test-source map before and after the gate matches the candidate's frozen module source map.

| Focused class or selection | Cases |
|---|---:|
| NativeResultMaterializerTest | 42 |
| LmdbNativeProjectionValueReuseTest | 12 |
| LmdbNativeIrFactorCoordinateTransportTest | 10 |
| LmdbNativeIrOptionalFanoutCoordinateTest | 2 |
| Delayed packed-row lifetime case | 1 |
| KernelResultCoordinatesTest | 19 |
| KernelResultCoordinateRepresentationsTest | 7 |
| KernelResultRowSelectionsTest | 15 |
| **Total** | **108** |

The selection exercises interpreted and compiled IR, alias/union/nested-optional bags, fanout, cancellation racing with close, retained rows, delayed fallback, weak-reference collection, mutation witnesses and coordinate representations. New cases cover small-budget replay, all 64 promoted positions at long indices, refusal followed by recovery, same-position replacement and reuse of an explicitly cleared inline cell, and retirement/ledger-first/double-close behavior for mixed page representations. The existing no-reclamation test was strengthened with a forced uncached replay and a setup based on actual ledger usage rather than the old first-page size.

This is a focused correctness gate, not a claim that every repository test was run. Independent invariant review is recorded in `independent-review.md`; raw XML, commands, build logs and source maps are preserved under `validation/`.

## Matched timing protocol

The fixed study has two full-consumption fixtures. Fanout uses M=100, R=10, width 12 and two statuses: 1,152,000 rows with checksum 3123987218479850048. Shared-tail uses 64 roots, 128 left members, 128 right members and two columns per tail: 1,048,576 rows with checksum 8327963502356915200. Its independent oracle verifies IRI types and lexical values, parent/tuple associations, legal unique row keys, count and checksum.

Each fixture receives all six permutations of the three arms. Every arm runs in a fresh JVM with eight warmups and all five measured queries. Complete three-arm blocks stay together; the first fixture alternates by round. The complete schedule is 36 JVMs, 288 warmups and 180 measured queries, totaling 468 queries. No samples may be excluded, replaced or selected from a favorable tail; no extra rounds or post-data tolerance changes are permitted.

All arms use Java 25.0.5+8, a fixed 1 GiB G1 heap, the same existing hot counters, a 1 GiB global query-memory authority and a 256 MiB per-query allowance. Native evaluation and direct adjacency PREFER are enabled; strategy and IR tier selection use normal AUTO. The original per-fixture store configuration is preserved. The fanout and shared-tail store budgets are not assumed identical, and no cross-fixture result is pooled.

Every query must have the expected complete result and zero ledger endpoints. Measured queries must use compiled native IR, with matching route histograms within each fixture. Post-main audits reconcile native compilation counts and require no hosted-generic outer root. Direct-adjacency observations require ACTIVE state, unchanged applied/current revisions and a positive lookup-hit delta across each query. These hits establish successful query-time adjacency participation, potentially including planning/probes; they do not establish that every output row came through adjacency.

The shared-tail harness adds startup-maintenance waiting and adjacency snapshots outside the timing/allocation window. All arms use the same new common prefix. Fanout, fixture, oracle and launcher class bytes are preserved. Candidate and control share the original dependency artifacts; only the LMDB production entry changes. Within that entry, changed bytes are confined to the materializer and its FactorPage/FactorMemo classes. The original frame source remains unchanged.

Timing runs are serial and isolated from other JVMs, builds, profiling, compression and optional hashing. Instrumented page diagnostics are separate and are excluded from every performance estimate.

## Statistical and measurement interpretation

For each fresh JVM, the estimate is the median of all five measured queries. A paired ratio divides candidate and comparator JVM medians from the same round and fixture. The primary summary is the median and complete observed range of all six paired ratios. These ranges are not confidence intervals, and ratios of unpaired descriptive medians do not replace the paired calculation.

The prospective engineering screen requires every shared-tail ledger ratio to be at most 0.25 and every shared-tail all-thread allocation ratio to be below one. On each fixture, the median paired total-latency ratio must be at most one, and no individual latency pair may exceed 1.10. Every condition is required. This is an engineering screen for a memory optimization, not a statistical noninferiority result or a population bound. A separate consistent-latency-win claim requires all six latency ratios below one on the relevant fixture.

Allocation is reported for both the caller and all observed JVM threads. Total timing covers connection acquisition, query preparation, evaluation, consumption/checking and close. `prepareNs` measures `prepareTupleQuery`; `evaluateNs` includes `evaluate`, which can contain planning and iteration construction. `firstRowAfterEvaluateNs` is part of consumption, so it must not be added again to total time. Oracle working storage is allocated before the measurement window; row consumption and checking remain inside it. The first cold query and every warmup are preserved separately from warmed estimates.

Ledger peaks measure reservations managed by the query-memory authority. They do not measure whole-JVM heap, RSS, retained decoded values or the separately configured adjacency budget. Full consumption, one host and one fixed JVM configuration do not establish results for early close, retained-row consumption, concurrent memory pressure, other cardinalities or other strategies. Those remain distinct experiments.

The complete prospective protocol is `protocol.json`, SHA-256 `8ef50b935b83cd22c525616438949f730042ed09c179eff332e39c5601f9b374`. The independently reviewed production patch is `665dca2f8fc662b510fa8d7f285f417a57546e8aec71418f169340c1b8153aa1`; the frozen candidate production tree is `efbb57205da275bd4032ee0f127eb2028010e6707dde8b4538807f94c5542595`.
