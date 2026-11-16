# Materializer follow-up: frozen-runtime JFR diagnosis

## Finding

Coordinate-enabled consumption spends additional sampled CPU in both the materializer's factor-cache work and the coordinate carrier's namespace bookkeeping. The profiles support testing these costs separately. They do **not** establish that copying five longs alone causes the runtime regression, and they provide no evidence of long monitor contention.

This is one diagnostic JVM per arm, with JFR enabled. Instrumented query times are retained as diagnostics and are not an additional controlled performance estimate. Neither the original harness nor any original runtime artifact changed.

## Recording and validity

Both arms used the exact prior 127-entry root-qualified runtime, Java 25.0.5+8, 1 GiB/G1, the original 1,152,000-row full-consumption fixture, normal AUTO, five warmups, and five measured queries. The only application setting changed was `rdf4j.lmdb.resultCoordinateTransport=off|dense`.

All 20 query records passed the independent full result checksum, row count, and ledger endpoint checks. Each process reported `HOSTED_GENERIC=0` and 10 semantic native compilations, exactly one per query. Each measured query recorded one normal ROW→irKernel selection and one compiled bind, with no interpreted bind. Both processes began with two interpreted warmups, followed by three compiled warmups. DENSE admitted one coordinate owner per query with no initial/growth refusal; OFF admitted none. Resolver calls remained 2,770,101 per OFF query and 1,147 per DENSE query. Ledger peaks were 2,432 and 2,556,992 bytes respectively; before/after use was zero for every query.

The configured root-hard-decline counter is inert; its zero is not used as evidence. The substantive outer-root observation is the monotonic fresh-process `HOSTED_GENERIC` total, read after the unchanged harness returns.

## Measured consumption window

JFR recorded exactly 11 writes to each samples.jsonl file. Their order and byte counts match the metadata line plus all ten query lines exactly. Let P be the previous write's end, W the current write's start, T the recorded total query duration, A the sum of connection/prepare/evaluate durations, and C the consume duration. Query start lies in `[P, W−T]`; therefore the interval `[W−T+A, P+A+C]` is guaranteed to lie inside consumption. We aggregate only that intersection. An independent external arrival trace is also preserved.

| Coverage | OFF | DENSE |
|---|---:|---:|
| Guaranteed consumption across five queries | 23.077408 s | 31.002058 s |
| Harness-reported consumption | 23.080435 s | 31.005656 s |
| Aggregate coverage | 99.98689% | 99.98839% |
| Minimum coverage of any measured query | 99.98181% | 99.97819% |
| Maximum uncertain bracket slack | 0.834518 ms | 1.402363 ms |

These are actual main-thread observations: Java thread 3 / OS thread 6 in OFF, and Java thread 3 / OS thread 62 in DENSE. Only two other CPU-time samples occurred inside OFF's consumption intervals and four in DENSE's, so the workload was sampled on the consuming thread rather than inferred from a wall-clock process profile.

## CPU stack attribution

The CPU-time sampler period was 10 ms. Main-thread counts were 2,187 OFF and 2,992 DENSE, representing nominal captured CPU periods of 21.87 and 29.92 seconds. These are sampled CPU weights, not exact CPU accounting; lost samples are reported below. The ordinary Java execution sampler ran every 5 ms and independently agreed in direction.

| Stack category | OFF CPU samples | DENSE CPU samples | OFF Java execution samples | DENSE Java execution samples |
|---|---:|---:|---:|---:|
| Materializer family, inclusive | 208 | 569 | 451 | 941 |
| rememberFactor, inclusive | 5 | 199 | 3 | 341 |
| findFactor, inclusive | 2 | 143 | 4 | 212 |
| captureCoordinate, inclusive | 0 | 140 | 0 | 211 |
| KernelResultCoordinates outside materializer, inclusive | 0 | 255 | 0 | 398 |

Inclusive rows overlap and must not be added. Materializer-family samples include its nested memo/page types and materializer-plan support. The direct coordinate-carrier category is explicitly outside materializer stacks.

DENSE's exclusive leaf samples include 133 in rememberFactor, 112 in captureCoordinate, 56 in findFactor, and 10 in FactorMemo.remember. The corresponding frozen-source line mappings point to the factor-page probe and memo refresh paths. FactorMemo.load does not appear as an exposed sampled frame; inlining and instruction attribution mean this is not proof that copying is free. A candidate that removes copying while preserving all guards is still an experiment whose effect must be measured.

The separate transport-carrier leaf samples include releaseNamespace 89, findNamespace 55, namespaceIndex 52, retainRange 28, releaseRange 14, and emit 12. Namespace reference counting and hash lookup therefore deserve separate treatment from coordinate-array copying. No causal percentage of the overall slowdown is assigned to any one method.

## Sampling and JIT qualifications

Inside the conservative consumption windows, the CPU-time sampler reported 88 lost main-thread samples for OFF and 73 for DENSE, approximately 3.87% and 2.38% of captured-plus-lost main samples. These counts come from loss events timestamped inside the windows; the missing stack samples do not have individual timestamps. Across the full recordings it reported 365 and 292 lost samples. There were 405 and 449 safepoint-biased main-thread samples (18.52% and 15.01%). Excluding those biased samples still leaves 206 OFF versus 554 DENSE materializer-family samples, and 242 DENSE coordinate-carrier samples outside the materializer. No failed or truncated sampled stacks occurred in the analyzed main-thread windows.

JFR recorded no monitor-enter event above the configured 10 ms threshold in either measured bracket. This does not isolate the cost of uncontended monitor operations or rule out shorter waits.

All recorded C2 compilations of the materializer methods completed during warmup. In DENSE, releaseBatch was compiled at C2 about 0.65 seconds into measured query 0; two namespace-management methods received tier-3/C1 compilations about 0.11 seconds into that query. General JIT work continued: 308 OFF and 347 DENSE compilation events began in the measured brackets, with summed compilation-event durations of 0.918 and 1.497 seconds. These are compiler-event elapsed durations, potentially overlapping, and are not added to main CPU totals. Eight warmups have therefore been chosen prospectively for the next comparison; every arm will use that same fixed count and all five measured queries will remain primary.

Allocation event weights identify allocation pressure, while exact per-query allocation remains the original ThreadMXBean all-thread counter. The raw and aggregated allocation records are preserved; sampled weights are not substituted for exact byte deltas.

## Files and next experiment

- [Scrubbed OFF diagnostic copy](delivery-profiles/frozen-off/profile.jfr), [raw queries](frozen-off/samples.jsonl), [root audit](frozen-off/root-host-audit.json), and [analysis](frozen-off/profile-analysis.json).
- [Scrubbed DENSE diagnostic copy](delivery-profiles/frozen-dense/profile.jfr), [raw queries](frozen-dense/samples.jsonl), [root audit](frozen-dense/root-host-audit.json), and [analysis](frozen-dense/profile-analysis.json).
- [Diagnostic protocol](protocol.json), [JFR settings](materializer-profile.jfc), [runner](profile-frozen.py), [extractor](ExtractJfr.java), and [window/stack analyzer](analyze-profiles.py).
- [Profile hashes and coverage](profile-summary.json), [before classpath manifest](classpath-manifest-before.json), [after manifest](classpath-manifest-after.json), and [run result](run-result.json).
- Exact argv, console output, JFR event counts, loss records, and JSONL arrival timestamps are retained in each arm directory. The expanded event JSONL is reproducible from the compact diagnostic recording and is omitted from delivery to avoid duplicating hundreds of megabytes of derived data.

The follow-on fixed comparison used a four-arm Williams square: original OFF, original DENSE, Candidate 1 DENSE, and cumulative Candidate 1+2 DENSE. Each of the four matched rounds used four separate fresh JVMs with eight warmups and five measurements. Primary effects are ratios of JVM medians within each matched round, then the median and full range of the four paired ratios. The unchanged harness and root audit remain in every arm. Candidates replace only the LMDB production classpath entry; source deltas, validation-state hashes, and exact class changes are frozen before timing. See the [frozen prospective protocol](optimization-comparison-protocol.json) and [completed comparison](comparison-report.md).

## Diagnostic-copy disclosure and reproduction

The downloadable JFR files are explicitly scrubbed diagnostic copies. Original raw recordings remain private and unchanged; their original hashes remain in profile-summary.json and the disclosure receipt. The copies omit only `jdk.InitialEnvironmentVariable`, `jdk.InitialSecurityProperty`, `jdk.InitialSystemProperty`, and `jdk.SystemProcess` events. All retained event counts and complete logical payload digests match the originals. The [disclosure verification receipt](delivery-profiles/disclosure-verification.json) records those checks without exposing detected sensitive values. The [analysis regeneration proof](delivery-profiles/analysis-regeneration-verification.json) confirms byte-identical extracted performance-event JSONL and equivalent analysis fields; explicitly listed differences are ordering/cutoff choices among equal-weight top-N ties caused by the original analyzer's randomized set iteration. No application benchmark was rerun for sanitization or verification.
