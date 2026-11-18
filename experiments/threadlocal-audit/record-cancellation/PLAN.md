# Direct record cancellation: benchmark preparation

No compilation or timing yet. Native worker owns behavior-test/build window. The design is `../RECORD-CANCELLATION-DESIGN.md`, particularly the terminal native-scanner owner refinement. This is not a blanket removal of ambient cancellation from generic record iterators.

## Immutable baseline

`baseline-classes/` is a full copy of the previous measured runtime with SOURCE_PULL and writer guard layers applied, exactly arm D's production byte sets. It represents published combined D6c1c805b semantics underlying current published 5ac89b5 source state, not a fresh build from the stale local HEAD. Previous full proof: `../source-pull/four-arm-01/class-manifest.json` arm D. No previous frozen directory is modified.

`cp-a.txt` uses that full combined baseline and the previous immutable test/dependency classpath. After behavioral validation, obtain the exact changed production-prefix list from the worker, freeze matching candidate outer/inner classes into B, compare added/deleted prefix members, and record source SHA/class SHA/published identity. Do not introduce unreviewed target/classes changes from unrelated builds. Print loaded class origins.

## Proposed controls and hit proof

Broad repository query: same narrow six-slot 100k result OPTIONAL fixture, regular/interpreted/Janino routes, same inherited measured-combination flags including partial-ready on and grouped waves off. Prior work did not prove the record-owned path ran; do not infer it from irKernel/irKernelInterpreted alone.

Targeted candidates for untimed qualification:

1. `audit.shape=scan`, `audit.adjacency=false`: full `SELECT ?s ?p ?o WHERE { ?s ?p ?o }`, no DISTINCT or aggregate shortcut. Same fixture gives 26,700 source/result triples at100 groups; scale groups identically across A/B after diagnostic duration qualification if too short.
2. `audit.shape=narrow`, `audit.adjacency=false`: same 100k repeated OPTIONAL/probe fixture, to exercise multiple correlated scan-site resets. Disabling direct adjacency alone is not proof that on-demand/dynamic adjacency was bypassed.

The new `RecordCancellationQueryBenchmark.java` is an isolated donor adaptation accepting scan and adjacency flags, retaining first-row/full consumption, full-term dual checksums, route/bind/factory proof and no-spill assertions. It has not been compiled. Owned-path diagnostics remain to be agreed with worker; absence of candidate-specific instrumentation on baseline must not falsely fail correctness. Record scanner/probe creation, actual physical LMDB scan visits, owned-versus-ambient checkpoint counts and reset reuse per owner in separate diagnostic runs. Do not put contended counters in clean timing. Cancellation publication/acknowledgement checks stay behavioral validation, not inferred from throughput.

Only promote targeted cells whose diagnostics prove the intended path. The broad query may be an unaffected control if it routes through adjacency. Record N/A or missed path, not a speedup claim about code that never ran.

## Timing after explicit approval

Preserve generous20 warmups/10 measurements, four independent forks per arm in A B B A B A A B order; exact selected cell matrix fixed after untimed qualification, before timing outcomes. All results retained, low-volume per-JVM GC logs, no JFR in clean forks. Same native640MiB cap and heap-only generic collections; no spill or retained ledger reservation allowed. First row includes eager evaluate and full projected-value consumption; total includes connection/prepare/consume/close.

Report candidate baseline identity as previous combined experimental D, never upstream main. Use fork means/ranges and paired contrasts; query-thread allocations are not retained heap. Separate longer profiles after clean timing, preserving context-owner/cancellation semantics and actual path proof. Do not launch the timing series until the parent grants the window and independent behavioral validation is green.

## Approved refinement before any timing: three arms

The implementation will initially expose its owned fast path behind a default-off feature flag. Once the worker supplies the exact property and untimed diagnostic seam, use:

- **A** previous combined D immutable runtime (published source state 5ac89b5), regardless of earlier guard-only C's better pooled Janino observation. Do not cherry-pick a new baseline.
- **B0** newly validated candidate with owned fast path OFF, including generic cooperative-cancellation support.
- **B1** exactly the same candidate bytes with owned fast path ON.

A→B0 measures added generic support overhead; B0→B1 isolates the feature-switch effect; A→B1 measures the total proposed delta. Report all three. This supersedes the initial two-arm timing paragraph above.

First run untimed/small correctness and route diagnostics, with counting absent or disabled for clean timing. Confirm B0 preserves behavior and B1 actually reaches owned physical LMDB record checkpoints. Only then fix the broad/targeted cell list before inspecting timing outcomes. Candidate acknowledgement/cleanup bounds are separate behavior tests, not throughput evidence.

Proposed fixed order per chosen cell: A B0 B1 / B0 B1 A / B1 A B0 / B1 B0 A, giving four independent forks per arm, each20 warmups/10 measured queries. Three cyclic blocks plus a reversed block; with four blocks and three positions exact positional balance is impossible, and this limitation must be disclosed. Independent within-block contrasts, ranges, first-row/total latency, reciprocal throughput, allocations and GC logs all retained. No post-hoc sample deletion or protocol shortening. The parent approves the final cell list after hit-proof diagnostics and before timing begins.

## Finalized feature and diagnostic switches

- `rdf4j.lmdb.recordCancellation.owned` (default false): B0=false, B1=true.
- `rdf4j.lmdb.recordCancellation.trace` (static-final, untimed only): reflective `LmdbRecordIterator.OWNED_BINDS`, `OWNED_POLLS`, `OWNED_RESETS` AtomicLong deltas prove physical leaf use, not just a flag/source-level claim.

`run-diagnostics.sh` compiles only the isolated harness after the worker grants a slot, then runs all three arms over tiny broad/scan/repeated-probe shapes and three routes. Unsupported forced routes are retained as failures/unsupported, never relabeled as another engine. `summarize-diagnostics.py` verifies completed arms and dual checksums, B0 zero owned counters and B1 positive physical binds/polls. Old A tolerates missing fields and explicitly reports unavailable metrics. Inspect resets separately for repeated-probe claims. No diagnostic elapsed times enter performance comparisons.

Clean timing must set trace=false explicitly; no metrics reflection/counters execute in the measured hot path. Candidate source/class freeze and complete changed-prefix list are still pending the worker's admission-window fix and final safety run.
