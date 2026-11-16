# Independent profile and comparison review

This is a read-only review of the completed diagnostic profiles and the prospective materializer comparison. It does not add a performance measurement. The [candidate invariant audit](design-review.md#independent-audit-of-scalar-draft-01) remains separate from this review. No Java process, build, test, runtime copy, or archive job was run for this review.

At the time of the initial review, [the comparison protocol](optimization-comparison-protocol.json) was a draft with both candidate runtime descriptors unresolved. Source validation, exact candidate freezing, and the parent's quiet-period release remain prerequisites to timing. The observations below distinguish completed profile evidence from the prospective protocol.

## Consumption-window reconstruction

The algebra in [analyze-profiles.py](analyze-profiles.py) is correct for the harness's timing boundaries. [OptionalFanoutExperiment.run](../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/OptionalFanoutExperiment.java) records a contiguous set of phases with `System.nanoTime`. Their durations telescope exactly to `totalNs`. The synchronous JSONL write occurs after the recorded query interval, post-query validation, allocation snapshots, and counter reads. The next query begins after the preceding record's write completes.

Let `P` be the preceding JSONL write's end, `W` the current write's start, `T` the reported query duration, `A` the sum of connection, preparation, and evaluation durations, and `C` the consumption duration. The unknown query start `S` is bounded by:

```
P <= S <= W - T
```

Each possible consumption interval is `[S + A, S + A + C]`. Their common intersection is therefore:

```
[W - T + A, P + A + C]
```

Its length is `C - (W - P - T)`. This excludes uncertain time at both ends instead of assigning the entire inter-write gap to consumption. I independently checked these equations and the phase sums for all twenty diagnostic query records against the saved analyses and raw JSONL. The mapping script also requires all eleven records in each JVM, including metadata, to match eleven JFR writes in order and exact byte length. External polling arrival times are retained as corroboration; they do not define the tighter windows.

The minimum retained fraction of measured consumption is **99.9818% for OFF and 99.9782% for DENSE**. DENSE measurement 1 has 1,402,363 ns of bracket slack; the OFF maximum is 834,518 ns. Thus “more than 99.97% in every measured query” is supported. A blanket maximum of 0.83 ms or minimum of 99.98% would slightly overstate the evidence. This reconstruction relies on the recorded JFR time conversion and harness durations describing the same elapsed intervals; it does not provide exact phase-transition timestamps.

Evidence: [OFF analysis](frozen-off/profile-analysis.json), [DENSE analysis](frozen-dense/profile-analysis.json), [OFF raw records](frozen-off/samples.jsonl), [DENSE raw records](frozen-dense/samples.jsonl), and [extractor](ExtractJfr.java).

## What the samples support

The main-thread denominators in the conservative measured-consumption windows are:

| Observation | OFF | DENSE |
|---|---:|---:|
| CPU-time sample events | 2,187 | 2,992 |
| CPU-time samples marked biased | 405 | 449 |
| Main-thread loss counts reported by events timestamped inside the windows | 88 | 73 |
| Ordinary execution samples | 3,474 | 4,871 |
| Samples with any materializer frame, CPU-time sampler | 208 | 569 |
| Samples with coordinate-carrier frames and no materializer frame, CPU-time sampler | 0 | 255 |

The materializer inclusive fractions are about 9.51% and 19.02% of the respective observed main-thread CPU-time sample sets. DENSE's coordinate-carrier group outside the materializer is about 8.52%. These are fractions of retained samples, not measured wall-time shares, percentages of whole-process CPU, or predicted speedups. Multiplying the sample counts by the nominal 10 ms period produces 21.87 and 29.92 sampled CPU seconds; it does not reconstruct omitted samples or replace a direct CPU-time measurement.

The analysis preserves both biased and unbiased subsets. Most of the materializer and carrier observations remain in the subset without the bias flag, which strengthens their usefulness as targets. It does not make either sample set unbiased in a statistical sense. The extracted all-event counts contain no `jdk.DataLoss` event, while explicit CPU-sampler losses are present. Loss records have aggregate counts rather than the individual missing stack traces: their timestamps support the reported diagnostics, not exact attribution of every missing sample to one phase or method. Do not create method-level confidence bounds by assuming those losses are random.

The source definitions distinguish CPU-time samples, execution samples of Java threads, and native-method samples; their denominators should remain separate. The CPU event's `biased` and `failed` fields are meaningful diagnostics. See [OpenJDK JFR event metadata](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/jfr/metadata/metadata.xml) and the [OpenJDK cooperative-sampling design](https://bugs.openjdk.org/secure/attachment/116356/Cooperative_JFR_Sampling_8352251-1.pdf). These sources explain the profiling model; the saved recording's actual fields and counts govern this analysis.

The aggregation correctly deduplicates repeated occurrences of a method within one stack before counting it inclusively, and explicitly labels its stack groups as overlapping. In particular, `captureCoordinate` may appear beneath `rememberFactor` or `findFactor`. Summing those groups would double-count samples. The carrier-without-materializer group is explicitly disjoint from the materializer group by construction, but even their union does not isolate the removable work of either candidate.

The DENSE carrier leaf samples include namespace release, lookup, and retention operations as well as emission. That is useful evidence against assuming the entire producer-side penalty is five-word payload copying. Conversely, the absence of a separate `FactorMemo.load` frame is not proof that the memo copy costs nothing: inlining, intrinsics, and sampling precision affect frame visibility. The candidates need their matched timing comparison to establish a net effect.

There are no recorded measured-bracket monitor-entry events above the configured 10 ms threshold. This does not measure uncontended synchronization or establish that locks are free. Allocation-event weights likewise remain statistical; the harness's allocation counters are the exact reported query allocation measure. One diagnostic JVM per mode, in a fixed OFF-then-DENSE order, is insufficient for a causal latency comparison. Keep these instrumented latencies separate from every uninstrumented protocol.

## Warmup and the prospective order design

The DENSE profile contains a level-4 `KernelResultCoordinates.releaseBatch` compilation in measurement 0. It starts about 0.65 seconds after the bounded query start, or 0.610 seconds after the conservative consumption-window start, and lasts 7.68 ms. This establishes a JIT transition during the measured block. It does not quantify that transition's effect on query time or prove that the first five warmups were the only source of drift.

The new protocol's **eight fixed warmups for every arm**, chosen before new results, are a defensible response. Eight warmups do not certify a steady state for changed code. Preserve all five measured values, individual fork medians, their range, and any visible progression. A later-tail calculation, if reported, must be a sensitivity analysis alongside the all-five primary estimate, with no exclusion or extension chosen to obtain a favorable result.

The prespecified Williams orders are:

| Round | First | Second | Third | Fourth |
|---|---|---|---|---|
| 1 | Original OFF | Original DENSE | Candidate 1+2 DENSE | Candidate 1 DENSE |
| 2 | Original DENSE | Candidate 1 DENSE | Original OFF | Candidate 1+2 DENSE |
| 3 | Candidate 1 DENSE | Candidate 1+2 DENSE | Original DENSE | Original OFF |
| 4 | Candidate 1+2 DENSE | Original OFF | Candidate 1 DENSE | Original DENSE |

An independent enumeration confirms that every arm occupies every position once, each of the twelve ordered adjacent pairs appears once, and every pair occurs in each relative order twice. This is stronger order control than simple cyclic rotations. It cannot eliminate changing machine conditions or arbitrary nonlinear drift, so the four paired ratios and their range remain necessary.

The design yields sixteen fresh JVMs, 128 warmup queries, and eighty measured queries: **208 queries in total**. No profile instrumentation belongs in those timed JVM arguments.

## Runtime and validation guards

The [candidate freezer](freeze-candidate-runtime.py) uses the original 127-entry classpath and replaces only entry 1, the LMDB production class tree. The other 126 entries include the exact original harness, fixture, diagnostics, dependencies, and final root-audit launcher. This is replacement at a fixed position, not an extra overriding directory with uncertain class-loader precedence. It preserves the old artifacts and records candidate source maps, reviewed production deltas, class deltas, validation-record hashes, and copied class-tree identity.

The [new comparison driver](run-optimization-comparison.py) distinguishes runtime/source identity from the transport property. It explicitly requires OFF on the original runtime and DENSE on each of the three relevant runtimes. It validates fixture and query hashes, projection/input parameters, Java version, heap, G1, normal JVM arguments without profiling, all raw records, checksum, coordinate participation, refusal counters, and zero ledger endpoints. JVM environment overrides are rejected. The unchanged post-main launcher is correctly checked against **thirteen semantic compiles per JVM, one per query**, and zero `HOSTED_GENERIC`. The inert hard-decline counter is not treated as independent evidence. Root-host coverage still does not certify every interior operator.

One additional identity condition was requested and added during review: **Candidate 1+2 must retain Candidate 1's materializer source and bytecode exactly, with its incremental production changes confined to the kernel execution class family.** Three different whole-tree hashes alone do not prove this cumulative relationship. The revised freezer compares production source maps and class-file maps against Candidate 1, requires the sole incremental production source change to be `LmdbNativeKernelExecution.java`, and rejects incremental class/resource changes outside that class family. It also requires the actual production source-path set to equal the caller's declared set, and requires each passing validation record's `sources-after.json` to match current module sources. I reviewed these added checks and subsequently inspected the resulting small metadata records; no candidate runtime was executed as part of this review.

The [frozen cumulative delta](runtimes/candidate1-and-2/candidate1-to-candidate2-delta.json) changes only `LmdbNativeKernelExecution.java` and its `KernelRowCursor.class` relative to Candidate 1. It retains the corrected Candidate 1 descriptor's digest and records the materializer source/class bytes as unchanged. The cumulative [source provenance](runtimes/candidate1-and-2/source-provenance.json) names source identity `87ef1e586e18172afe76564a3bef6b7b1f6149ca681a32c11d584895aa36607b` under the explicitly labelled follow-up source-map algorithm and production tree `7965ccd5506514774b8943d2c4ec940b79a24cad2568c29e98e9bffd1d38cc41`. Both candidate records contain the same 94-case passing validation selection. This is inspection of the freeze records; the driver separately verifies the actual runtime artifacts before and after timing.

The driver retains successful selection/tier histograms and compares them within each matched round. A difference must be retained and qualify any same-route claim; forcing a strategy or discarding a slower fork would change the question. Cache-effectiveness counters and ledger peaks should also be compared across enabled arms. A candidate can preserve result semantics while changing weak-cache survival or work counts, so report those observations instead of assuming they are identical.

### Pre-run source-map correction

Before comparison timing began, the parent identified a source-provenance mismatch in the first follow-up helper: its original-runtime label correctly named `34767935…`, but its comparison map was the older `final-validation/source-hashes.json`. I independently read and hashed the two small map files. Both have 2,057 entries; their sole difference is `NativeExpressionDomainFailureTest.java`. Every production entry is identical.

| Map | Exact SHA-256 | Meaning |
|---|---|---|
| [benchmark-source-hashes.json](../final-validation/benchmark-source-hashes.json) | `34767935d60f32ba61adfb44495c607e17ca760d5d2f77a17ddb2862c4f7a8ac` | Correct source map for the immutable measured runtime. |
| [source-hashes.json](../final-validation/source-hashes.json) | `b804d62aca5c30d2ee8edd0d2dcdec5a5d549f55be84a5a3a1716328365cbb91` | Earlier validation map; retained as historical evidence. |

The [original frozen-runtime provenance](../frozen-current/provenance.json) and [benchmark source provenance](../final-validation/benchmark-source-provenance.json) already identify the measured map correctly. The mistake affected the first follow-up metadata's comparison baseline, not the frozen production/test bytes or the completed diagnostic recordings.

Against the correct measured map, Candidate 1 changes one production source, `NativeResultMaterializer.java`, and three test sources: the two earlier delivery fixture corrections (`LmdbNativeGenericRootHostTest` and `LmdbNodePredicateRouteTest`) plus the new `NativeResultMaterializerTest` characterization. My earlier four-test observation reflected the erroneous comparison and is superseded by this direct map comparison. Candidate 1's own source map, production patch, class bytes, and source identity remain unchanged.

The [superseding Candidate 1 descriptor](runtimes/candidate1-metadata-corrected/runtime-descriptor.json), [corrected provenance](runtimes/candidate1-metadata-corrected/source-provenance.json), and [correction record](runtimes/candidate1-metadata-corrected/correction-record.json) passed an independent small-file audit. The descriptor's SHA-256 is `fa05806af83c234114935d60aa818d7c38b4deab19e88b5611390578f998304f`; the provenance's is `c600eaa5f6f02b506f14d2f8e5659f4a0760500042dbcdcf689d98057806aeb6`. I checked the old/new metadata hashes, unchanged classpath/manifest/patch/source-map references, the recomputed source-role delta and candidate source identity, and the saved historical helpers against their recorded digests. This audit did not rehash the large runtime trees.

The comparison protocol now embeds the corrected descriptor. The identity helper pins the exact measured map digest; the driver validates that baseline path and each candidate's source-map identity. Candidate 1+2's freezer follows the corrected descriptor and verifies Candidate 1's source/provenance/manifest hashes plus its class-file map digest before checking the cumulative delta. The runtime bundle/rebase validator also pins the measured map and the active candidate baseline digest. Its inventory includes the corrected metadata, historical helpers and map, and correction generator, preserving the complete explanation. The first metadata files remain intact as explicitly superseded records. No performance results are discarded or rerun because of this pre-run correction.

## Interpretation of the planned ratios

Each primary ratio is the candidate JVM's median of all five measurements divided by its comparator JVM's median in the same round. The reported aggregate is the median and complete range of four such ratios. These are **matched-round ratios from different JVMs**, not within-JVM comparisons, eighty independent replicates, confidence intervals, or ratios of pooled medians.

- Original DENSE versus original OFF retests the complete coordinate mechanism in one unchanged runtime.
- Candidate 1 DENSE versus original DENSE estimates the combined materializer rewrite: reduced memo copying, cached-page association checks, and reference-tuple publication checks. It does not isolate copying alone.
- Candidate 1+2 DENSE versus Candidate 1 DENSE estimates the eligibility change conditional on Candidate 1 and the verified cumulative source relationship. It does not measure Candidate 2 independently on the original materializer.
- Each candidate versus original OFF compares the optimized enabled path with the preserved original-runtime ordinary-reuse reference. This is a comparison across approved source variants, not a mode ablation within the candidate's own runtime. Candidate OFF performance is unmeasured; OFF and other-mode correctness gates do not supply those timings. Compute these paired ratios directly; multiplying aggregate medians from other comparisons is not equivalent.

All six comparisons, absolute phase distributions, allocation and ledger observations, route qualification, and raw progressions should remain visible. The new protocol has different source arms and a different warmup policy from earlier experiments; preserve it as a separate experiment and do not pool it with the diagnostic profiles or historical transport results.
