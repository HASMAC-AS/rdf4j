# Run the fresh retention comparison

The two Python scripts use only the standard library. The runner launches at most one JVM per invocation; the analyzer reads sealed raw outputs directly and does not import the runner or its summaries. No historical or diagnostic timings enter the estimator.

## Preconditions and freeze

Complete ownership, metadata-accounting and native-route qualification first. The coordinator supplies a JSON receipt whose `qualifiedForTiming` value is true and whose contents identify the actual validation evidence. A compilation success alone is not that qualification. The runner hashes this receipt together with the supplied source trees, itself, the analyzer, every classpath entry and the selected Java runtime inputs.

Before freezing the main cohort, exercise the final runtime on both original fixtures with `qualify_fixtures.py`. For the final formatted runtime attempt:

```bash
python3 -B result-coordinate-evidence/retention-followup/benchmark/qualify_fixtures.py \
  --runtime result-coordinate-evidence/retention-followup/runtime-v2 \
  --output result-coordinate-evidence/retention-followup/qualification-v1 --slot-granted
```

This performs two sequential weak-arm pilots, each with the same eight warmups, five measurements, JVM flags, properties and fixture arguments as the main design. It checks actual format-2 statistics and full-fixture lifecycle behavior, hashes its runtime/source inputs, seals raw outputs and emits a scope-limited `qualification.json`. A failure stops the helper and preserves the attempt; use a new output name only after diagnosing the failure. Its receipt explicitly requires the separate unit/IR contract evidence. Pilot timings are never pooled into the main study or used to tune its design.

Run from the RDF4J repository root. This example uses the rebuilt source and classpath paths established in this study; use the actual qualification receipt path when it is created:

```bash
python3 -B result-coordinate-evidence/retention-followup/benchmark/run_comparison.py freeze \
  --cohort result-coordinate-evidence/retention-followup/comparison-v1 \
  --qualification result-coordinate-evidence/retention-followup/runtime-v1/qualification.json \
  --source result-coordinate-evidence/retention-followup/runtime-v1/src \
  --source core/sail/lmdb/src/main/java \
  --source result-coordinate-evidence/compact-page-followup/harness-src \
  --source result-coordinate-evidence/root-qualified-launcher/src
```

The default base classpath file is `retention-followup/recovery/runtime-v1/direct/classpath.txt`; the default new prefix is `retention-followup/runtime-v1/classes`. The runner prepends the new prefix if it is not already first. Both paths are relative to `result-coordinate-evidence`. Override them with `--classpath` and `--overlay` if the recovery attempt has a different versioned path. All three arms must use the same resulting classpath.

The `freeze` command creates a fresh cohort directory and records the complete schedule before any JVM starts. It refuses an existing destination. The code, compiled files and selected source inputs become immutable for this cohort. If they need correction, retain the old cohort and freeze a new one after diagnosis.

## Design

The fixed schedule contains all six permutations of ordinary, strong and weak retention, each on both original fixtures. Fixture order reverses on alternate rounds. Every JVM uses eight warmups and five measured queries, giving 36 JVMs, 468 queries and 180 measured observations.

Ordinary reuse disables the direct memo. Strong and weak reuse enable it and select the corresponding retention property. Coordinate transport is off for every arm. AUTO strategy selection, the full-consumption harness, store caches, fixture inputs, a fixed 1 GiB G1 heap, 512 MiB query ledger and 1 GiB global ledger remain matched. Object alignment is explicitly eight bytes; compressed object and class pointers are enabled. The metadata reservation uses 112 bytes per weak slot plus 160 bytes for cached-owner/replacement overlap, in addition to the existing five-array estimate.

The fanout fixture uses 100 measurements, 10 readings, projection width 12 and two statuses. The shared-tail fixture uses 64 roots, 128 left tuples, 128 right tuples and tuple width two. Expected row counts, checksums, graph/query hashes and source identity are sealed in `protocol.json`.

## One JVM at a time

Release the exclusive Java slot before each call. Do not run builds, tests, profiles, compression or other heavy work while its JVM is active:

```bash
python3 -B result-coordinate-evidence/retention-followup/benchmark/run_comparison.py next \
  --cohort result-coordinate-evidence/retention-followup/comparison-v1 --slot-released
```

Observe that invocation's actual terminal exit code zero before issuing the next invocation. If the execution interface returns a session identifier, poll that same session through completion. Do not wrap the 36 calls in one long-running tool process. A durable claim precedes each JVM, and a completion receipt seals its raw outputs only after normal exit and validation. A missing completion, failed oracle, changed runtime, unexpected route, overlapping process interval or incomplete claim blocks continuation. No fork is retried, replaced, skipped or imported from elsewhere.

`status --cohort ...` validates the frozen inputs and the completed prefix without launching Java. The prescribed per-JVM deadline is 300 seconds. Full byte checks run before and after each JVM, outside the measured process; their inter-process overhead is not a query-latency observation.

## Audit and analyze

After the 36th invocation finishes normally:

```bash
python3 -B result-coordinate-evidence/retention-followup/benchmark/run_comparison.py audit \
  --cohort result-coordinate-evidence/retention-followup/comparison-v1
python3 -B result-coordinate-evidence/retention-followup/benchmark/analyze_comparison.py \
  --cohort result-coordinate-evidence/retention-followup/comparison-v1 \
  --output result-coordinate-evidence/retention-followup/analysis-v1
```

The analyzer refuses incomplete cohorts and existing output destinations. It rechecks raw checksums, exact shared-tail bags, full consumption, native AUTO execution, compiled measured IR, matched per-query routes and adjacency, zero ledger endpoints, all 27 primitive memo fields, owner-to-query mapping, reservation arithmetic and sealed output hashes. Resolver differences, collection, hit-rate changes, growth and the original low-hit cutoff remain measured outcomes; no favorable timing subset is selected.

The estimator is the median of six same-round ratios of per-JVM medians, separately for each fixture. JSON retains every paired ratio and its range, plus absolute medians, preparation, allocation, ledger peaks and memo counters. The report makes no significance or production-promotion claim.

All arms write unified GC logs with the same logging settings. The analyzer reports pause events and pause duration separately from completed concurrent-cycle duration. These are whole-JVM observations, including fixture setup and warmups, and cannot be attributed solely to measured queries. It rejects explicit/diagnostic GC causes. No forced collection, profiler or heap walk belongs in a latency fork.

Strong ledger charges cover arrays; weak ledger charges additionally reserve conservative weak metadata. Neither is measured retained heap. Memory diagnostics and source-cache retention need their own evidence. `retainedEntriesAtClose` counts occupied wrappers, including weak wrappers whose referents may already be cleared; `collectedMisses` counts collection observed by probes, not a complete GC census.

## Parser/controller checks before timing

On 2026-11-17, both scripts parsed and their help commands completed. A temporary synthetic parser fixture based on the historical raw schema exercised these five cases without launching any subprocess: a valid strong run and distinct GC pause/concurrent durations; rejection of a claimed slot without completion; rejection of a checksum mismatch; rejection of explicit-GC contamination; and rejection of accepted estimates from an incomplete cohort. All five passed. These are controller checks, not new benchmark observations, and the temporary fixture was removed.

After confirming that the new launcher exports statistics format 2, two additional synthetic checks passed: acceptance of a weak record with an observed collected miss and the 606,448-byte initial reservation, and rejection of an understated replacement-overlap reservation. No JVM was launched for these checks.
