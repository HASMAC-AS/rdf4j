# Existing ordered DISTINCT: bounded-state proof and route tradeoff

No production changes. Harness accepts `overlay.benchmark.route` with only irKernel/orderedDistinct, default irKernel unchanged. IR retains compiled-bind assertion; both require exactly the forced route, expected count, checksum parity, memo hits and no resource leak. Optional expectedChecksum enforces comparison across JVMs. Frozen production classes: probe-segmented-classes (segmented + bulk ID I/O + miss-slot reuse). Memo, partial-ready, wide primitive and both spill optimizations enabled throughout. Parallel disabled.

## Source-grounded eligibility

NativeUnorderedInput defaults to HASH. acceptKernel does not export ordering, so the benchmark's forced ordinary IR route cannot use the separately derived ordered plan. Only the orderedDistinct winner marks ORDERED. IR distinct sinking rejects root LeftJoin and LeftProbe/LeftGroup nodes. Existing ordered planner propagates a proven left prefix through OPTIONAL, with a barrier preventing unsafe deeper-order claims. NativeOrderedDistinctTracker clears residual membership at partition transitions.

Successful orderedDistinct route plus one budgeted residual admission proves use of PARTITIONED_HASH rather than ADJACENT (no residual) or GLOBAL_HASH (not eligible for orderedDistinct proposal). This experiment does not independently identify the specific partition slots or prove ordinary compiled IR preserves the same grouping. A future compiled certificate must follow final join order, producer orientation and scheduling, and reject unsafe opaque/parallel paths.

## Measurements

Fresh JVMs, -Xms2g -Xmx5g, global ledger cap2.5GiB; each100k run has one warmup and one measured iteration. Allocation is query-thread allocated bytes, not retained heap or RSS. Ledger peak is total query/process tracked memory, not a residual-only attribution.

| Rows | Route | Query cap | Warmed ms | Thread allocated bytes | Ledger peak bytes | Spill files |
|---|---|---|---:|---:|---:|---:|
|100,000|irKernel|4MiB|1731.330|394,918,728|4,178,880|14|
|100,000|orderedDistinct|4MiB|4703.049|31,393,166,008|3,797,344|0|
|20,000|orderedDistinct|1MiB|1315.320|6,270,173,808|558,400|0|

100k checksum1220594000736094224 on both routes and all warm/measured iterations.20k checksum-9217758912286922416 matches independent IR baseline. Every run closes with zero ledger bytes and created=deleted spill files.100k memo probes2,760,000, hits~2,731,750.1MiB20k has475,136 probes/469,372hits, fewer than unrestricted20k552,000probes, so low-budget cache activity is not identical.

The existing ordered route is2.72x slower and~79.5x more allocating at100k, despite avoiding spill. These are small single-pair warmed diagnostics, not an ABBA8m performance result. No8m ordered run was attempted given the allocation warning.

Cold20k proof: IR1425.499ms/160,515,840B/5,698,976Bpeak versus ordered5298.377ms/6,334,320,184B/1,143,136Bpeak. Cold timings include JIT/setup and are not used as steady-state comparison.

## Allocation evidence

Separate100k ordered JFR diagnostic (one warmup + one measurement) sampled62.385GB main-thread weighted allocation:
-48.51% (30.26GB): PackedLongBlocks.ensureCapacity:5870 via NodeData → Chunk → Runtime.nextChunk.
-16.74% (10.45GB): RunRootProducer constructor:4641.
-5.67% (3.54GB): Planner.plan:1960 HashSet allocations via openLeaf/FactorAlgebra.

Source confirms every nonempty PackedLongBlocks initially allocates long[VECTOR_SIZE] (default2048), NodeData eagerly creates one, and RunRootProducer eagerly allocates neighborWindow[ROOT_BATCH_SIZE] (default1024). The evidence implicates repeated packed factor-tree setup and buffers, not a generic claim that interpreted execution alone causes this cost. JFR sample shares are allocation-weighted, not CPU-time percentages; whole diagnostic JVM includes setup. File ordered-allocation-summary.txt records the aggregation.

If mandatory root groups can be certified for compiled execution,1k residual tuples of29columns need~232KB raw keys versus8m×30~1.92GB global keys. This remains a conditional opportunity, not measured compiled eligibility. Existing configurable vectorRows/rootBatchRows could test oversized buffers independently, but no tuning run is included here.

## Reproduction/artifacts

run-ordered-route.sh route groups queryBudgetBytes logStem [expectedChecksum] [warmups] [iterations]
-ordered-small-ir.log / ordered-small-ordered.log
-ordered-100k-ir.log / ordered-100k-ordered.log
-ordered-small-1mib.log
-ordered-100k-profile.log / ordered-100k.jfr / ordered-allocation-summary.txt

All heavy work used native-benchmark.lock and allowed final regression shards between runs.

## Existing-buffer-knob diagnostic

No production edits; same frozen classes,100k fixture,4MiB query cap, memo retained, one warmup + one measured run per fresh JVM. Settings are printed from actual initialized LmdbNativePackedFtree fields in the harness.

| Effective vector/root/initial rows | Warmed ms | Thread allocated bytes |
|---|---:|---:|
|2048/1024/2048 (prior default run)|4703.049|31,393,166,008|
|2048/64/2048 (rootBatchRows=64 only)|3707.882|25,207,896,240|
|64/64/64 (both knobs=64)|2850.392|11,777,421,760|

Every run retains exact100k count/checksum1220594000736094224, no spill,3,797,344-byte tracked peak, zero ledger bytes after close,2,760,000 memo probes/~2,731,750hits. Root-only reduces allocated bytes~19.7%; both64 reduce them~62.5%. Single warmed timings improve~21.2% and~39.4%, respectively; this is a diagnostic, not replicated ABBA evidence. Both64 remains slower and vastly more allocating than the prior matched IR1.731s/395MB run.

RootBatchRows is clamped to VECTOR_SIZE, so vectorRows=64 alone would already force the effective root size64. Root-only versus both64 is therefore the meaningful isolation. INITIAL_NODE_CAPACITY also becomes64 when vectorRows=64; its vectors no longer round up to2048-long blocks. These settings directly support the repeated oversized-buffer allocation hypothesis. They do not eliminate repeated packed planning/setup, and should not be made global defaults on this small-correlated-arm fixture alone.

Reproduction:
- `PACKED_ROOT_ROWS=64 bash experiments/overlay/run-ordered-route.sh orderedDistinct 100 4194304 ordered-100k-root64 1220594000736094224 1 1`
- `PACKED_VECTOR_ROWS=64 PACKED_ROOT_ROWS=64 bash experiments/overlay/run-ordered-route.sh orderedDistinct 100 4194304 ordered-100k-both64 1220594000736094224 1 1`

Logs: ordered-100k-root64.log and ordered-100k-both64.log. No8m run. These diagnostics ran only after the focused migration/cancellation worker released lock priority.
