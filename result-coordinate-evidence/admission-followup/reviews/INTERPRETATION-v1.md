# Admission study: interpretation and next ablation

The next decisive experiment is **always admission with adaptive growth versus always admission fixed at 4,096 slots**, keeping weak retention, hashing, exposure/ownership checks, source caches, query input, JVM settings and AUTO history identical. The completed study establishes late reuse recovery and a large metadata reservation consequence. It does not establish that the four resizes caused the observed whole-query latency difference.

This review uses [the completed comparison audit](../comparison-v2/audit.json), the [untimed cyclic trace results](../diagnostics/TRACE-RESULTS.md), the [natural-GC audit](../diagnostics/analysis/natural-audit-v1/RESULTS.md), and the preserved [memo implementation](../implementation-src/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemo.java). It adds interpretation, not another cohort or numeric estimator. No Java process or production/protocol change was made.

## What the measured queries establish

Each query has 131,072 projected opportunities. The entries below are constant across the measured owners within each fixture/arm. Ordinary always makes 131,072 source calls and has no memo reservation; the process ledger also accounts for other work.

| Fixture and arm | Memo hits | Source calls | Weak-entry allocations | Peak slots / resizes | Peak memo metadata reservation |
|---|---:|---:|---:|---:|---:|
| Early cold-hot, initial or always | 126,944 | 4,128 | 4,128 | 4,096 / 0 | 606,448 B |
| Late cold-hot, initial | 0 | 131,072 | 8,191 | 4,096 / 0 | 606,448 B |
| Late cold-hot, always | 98,240 | 32,832 | 32,832 | 65,536 / 4 | 14,549,472 B |
| Hot-unique, initial or always | 8,128 | 122,944 | 122,944 | 16,384 / 2 | 3,637,728 B |

The early fixture has 4,096 unique opportunities followed by a 64-key hot tail. Both policies pass the initial cutoff; they have the same hits, source calls, collisions and capacity. Their whole-query hit rate is 96.8506%, and only 32 of 126,976 tail opportunities call the source.

The late fixture has a much longer **32,768-unique** prefix, followed by 98,304 opportunities cycling over 64 keys. Initial admission disables at probe 8,192 before any repeat. Always admission recovers 98,240 source calls: 74.9512% of all opportunities and 99.9349% of the tail hit. Relative to initial, recovery requires 122,880 additional memo probes, 24,641 additional weak-entry allocations and four table resizes. The peak metadata reservation is about 24 times larger. These are observed operation/accounting consequences, not measured CPU costs per operation.

Hot-unique supplies the opposite phase change: 8,192 opportunities over 64 keys, then 122,880 unique opportunities. Both policies make identical decisions. All 8,128 hits occur in the prefix; the entire tail calls the source. Historical hits nevertheless permit two resizes before cumulative hit rate falls below the growth threshold. The table never performs a second admission cutoff. Consequently this fixture does not compare a continuing cache with a dynamically disabled cache: both arms continue the same behavior after reuse stops.

## Are the late resizes necessary for this hot tail?

The evidence strongly supports **no extra reuse benefit from those resizes on this observed trace**. The always arm ends with 28,738 cumulative collisions, only 64 total tail source calls, zero collected-entry encounters/invalidations/clears/refusals, and 4,094 occupied metadata slots in a 65,536-slot table. Occupied slots are not independently measured live referents.

The policy cannot grow during the 32,768-unique prefix because it has no hits. At probe 40,960, even a perfect tail could provide only 8,192 hits, below the 10,240 required. Thus the first 16,384 tail opportunities still use the 4,096-slot table. A persistent conflict among the 64 repeatedly visited hot keys would produce hundreds of misses over those cycles, inconsistent with only 64 source calls in the entire tail. The small tail already fits the initial hash table without continuing conflicts.

After reuse starts, historical cold-prefix collisions remain in the cumulative numerator. Combining the known phase schedule with the source thresholds predicts growth at probes 49,152, 57,344, 65,536 and 73,728, matching the observed four doublings and final capacity. These boundary locations are a source-derived reconstruction; the full-query harness reports final counters, not a checkpoint trace at each resize. Growth happens after the hot set has stabilized. Fixed-hash doubling preserves the existing entries and does not create new reuse opportunities for this conflict-free hot set.

The four resizes allocate four replacement sets of five arrays and scan 61,440 old slots in total. Summed array footprint estimates across initial allocation and all resizes are 4,571,536 B versus 147,536 B for one fixed 4,096-slot table. These are estimates from the accounting formula, not measured array allocation bytes. The much larger 14,549,472 B peak also reserves conservative per-slot weak-reference metadata and overlapping old/new tables. It is neither a payload bound nor a total live-heap observation.

This is a particularly clear history problem: obsolete collisions cause growth after the stream becomes easy to cache, while the hot-unique fixture shows old hits financing growth after the stream stops reusing values. The data justify isolating growth before inventing another controller.

## Why the latency result does not identify resize cost

The prespecified late always/initial paired total-time ratio is 1.1353; the mean-based sensitivity gives 1.0541, while the allocation ratio is 0.9753. Against ordinary, the corresponding total-time ratios are 1.2220 and 1.0528, with allocation ratio 0.9743. Late reuse recovery therefore did not demonstrate an end-to-end win in this cohort, despite its large and deterministic source-call reduction.

However, the primary always/ordinary first-row ratio is 1.2391; the central absolute first-row observations are approximately 400.9 versus 494.9 ms. Current-query growth has not happened at the first row: projected demand is still at the beginning of the cold prefix. Previous-query heap state, compilation, runtime variability or other costs may contribute, but none was diagnosed here. The tail-only always/ordinary ratio is 1.02355 with a broad 0.725–1.305 range. It cannot isolate the four resizes either.

There are useful internal warnings against stronger causal claims. Early initial and always have the same memo event trace, yet their paired total-time ratios range from 0.765 to 1.188 and their allocation point ratio is 0.9662. Late initial/ordinary changes direction between the primary ratio, 1.0772, and mean sensitivity, 0.9344. These finite measurements support reporting the observed ratios and their variability, not assigning the whole difference to memo growth or claiming statistical significance. The ledger difference is precise; the latency attribution remains unresolved.

The [adjacency cutover review](ADJACENCY-CUTOVER-REVIEW-v1.md) also remains relevant: eight harness warmups leave the root-source AUTO learner sampling its alternative during the five measured queries. Routes are matched, but converged AUTO behavior and converged JIT behavior have not been established.

## What the other diagnostics contribute

The cyclic traces show why fixed capacity is an **ablation**, not a proposed universal replacement. Cold W=4,096 with always admission eventually grows to 65,536 and records 106,410 hits; initial admission cuts off after only 1,515 hits. Yet cold W=8,192 remains at 4,096 even under always admission, with 12.93% hits, because the same cumulative hit gate prevents growth. A hot bootstrap lets the latter tail grow and achieve 73.24% hits. Those traces isolate the real hash and policy with deliberate live consumers, but synthetic sequential IDs; they do not measure query performance or GC behavior.

The real-ValueStore diagnostic separates source calls from decoding. For 1,024-character IRIs, ordinary makes 136 source calls but has only eight cold-completion observations and 128 same-prior returns. The source cache already preserves the decoded Values. Both memo modes reduce calls to eight. For 1,048,577-character IRIs outside the configured lexical-cache limit, weak/none makes 136 calls with 128 same-key collected misses, while strong makes eight calls. One retained Value gives exactly 16 hits for that key and 112 collected misses for the other seven. These are bounded, untimed mechanism observations; cold completion is not an instrumented physical-decode count, and the retained consumer is one Value rather than a query row.

The full-query fixtures use 64-character values and an ordered result path that retains materialized rows. Their zero collected misses do not establish weak-memo performance for streaming rows, memory pressure, large uncached payloads or early close. Ledger release also does not imply all source-cache, native or consumer-owned memory has disappeared.

## Minimal prospective follow-up

Use the late cold-hot fixture as the primary case. Compare the current always/adaptive arm with always/fixed-4,096 using the same runtime except the growth switch. Preserve the default behavior, prepare a focused failing contract before changing the experimental copy, and keep all ownership/cleanup and reservation-refusal behavior applicable to initial allocation unchanged.

The mechanism prediction is exact: fixed capacity should retain 98,240 hits and 32,832 source calls, with zero resizes and a 606,448 B peak metadata reservation. Verify that in an untimed qualification first; record capacity and hit/collision checkpoints separately from timing. Early cold-hot is a useful negative control because neither arm should grow there. An existing cyclic working-set trace can check that the ablation is capable of losing beneficial growth, without adding a new cache algorithm or promoting fixed capacity.

Then run a fresh alternating matched comparison, retaining first-row, prefix/tail, full-query allocation, metadata peaks and route signatures. Keep the query/AUTO history matched rather than simultaneously changing warmup policy. If source calls stay identical but allocation/reservation and latency improve, growth has an identifiable cost in this case. If latency does not improve, the memory reduction remains established and the whole-query timing difference needs another explanation. This single comparison is more decisive than choosing windowed collision decay, periodic admission, associativity or a new eviction policy from the present evidence.
