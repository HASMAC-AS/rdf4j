# Continue optimizing binary query result writing

This ExecPlan follows `.agent/PLANS.md`. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current.

## Purpose / Big Picture

Reduce additional binary-result serialization CPU and allocation beyond the already published optimization, while preserving wire bytes, malformed-Unicode rejection, row visibility, stream ownership, and model equality. The user requested continued optimization after branch GH-0000-binary-query-writer-performance was published at 741b9389538e80214075692439a8cd25a8a9ae84. Extend that branch and publish complete source, a diff-only patch, and reproducible measurements. The baseline for every new timing comparison is that published commit, not the earlier unoptimized main.

## Progress

- [x] Verified local production source and remote branch at 741b9389538e80214075692439a8cd25a8a9ae84; existing evidence retained untouched.
- [x] Identified two remaining costs: allocating large-string encoder and multiple writes per small value.
- [x] Added independent boundary, malformed-input, and I/O-failure characterization tests before production edits.
- [x] Passed the complete pre-change binary suite with all new growth cases: 267 tests, including 204 encoding cases, no failures or skips. Candidate large-string, record-assembly, and small-document smoke checks passed through the real parser.
- [x] Compared isolated large-string and complete-record candidates, including small documents; retained all screening results.
- [x] Promoted AdaptiveRecord plus BoundedChunked after parser/exact-byte smoke checks and independent reviews; formatted, passed copyright checks, completed full offline root clean install, and passed all 267 binary tests plus both targeted server error-path tests.
- [x] Completed final matched benchmarks and profiles; committed/pushed production as c031714, updating PR #6440; verified both source patches and complete source ZIP. Evidence packaging and publication use the frozen artifact manifest.

## Surprises & Discoveries

The first patch retained the allocating strict CharsetEncoder for strings outside its 4 KiB scratch buffer. That leaves large ASCII literals at roughly the original runtime. String.getBytes(UTF_8) has optimized String-specific paths, but it replaces malformed surrogates and may allocate more for sparse Unicode or supplementary pairs. A candidate must validate first and must be measured across character distributions.

Current short canonical typed literals require three downstream writes, custom typed literals five, QName IRIs and language literals three, and blank nodes two. Complete short records can fit in the already retained buffer and be emitted once, without buffering output beyond the current callback.

The 4 KiB scratch is eager per writer. Large-document benchmarks amortize it; new empty/one/eight-row document measurements explicitly measure the construction tradeoff. The existing namespace map can still grow with distinct input namespaces; only new encoding scratch and datatype templates are bounded.

## Decision Log

Use Routine D for this serialization refactor, with source-only prototype classes before production edits. Preserve a passing pre-change compatibility run, then use the same tests after promotion. The initial root build and dependency setup from this conversation remain available; tight-loop module installs refresh the binary module before verification.

Keep large-string algorithm changes, complete-record assembly, ASCII-prefix reuse, and adaptive scratch allocation as separately named candidates. Do not weaken native equality or add a generic value cache. Measure timing sequentially, without concurrent builds, tests, or other benchmark processes.

The first one-fork screening matrices show that complete-record assembly substantially reduces short-value time and bounded chunk encoding removes large output arrays. Validated getBytes can be faster but increases allocations for several Unicode distributions; it is not a universal replacement. A final isolated bounded-loop chunk candidate removes per-character capacity checks, then the measured record and long-string paths will be combined. Small-document measurements determine whether adaptive scratch growth is useful. These screens select candidates; final claims will use fresh matched measurements of production.

## Outcomes & Retrospective

The production implementation is published as c03171474130c67ca2eed2b78e3cc82cd4fba345 on the existing performance branch and PR #6440. Short mixed rows improved about 44–45% in the primary local run, and most long-string cases improved time while eliminating output-sized encoding arrays. Empty, one-row and eight-row documents improved both time and allocation. Sparse-Unicode timing and whole-query effects remain unresolved. The separate query allocation check reversed the primary direction; both are retained. A JDK first-allocation-sample boundary artifact was identified and documented, with separate sensitivity summaries that preserve raw profiles and all CPU counts. Both source patches reproduce the published source tree, and the complete source archive has been checked byte-for-byte. Candidate code, research and fresh measurements live under binary-writer-round2; the previous evidence remains unchanged.

## Context and Orientation

The production writer is core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java. It emits format version 4: strings have a four-byte big-endian UTF-8 byte length. Typed literals require their datatype namespace declaration before the literal marker, and their label must be followed immediately by the datatype QName. Empty rows retain the previous nonempty row as the repeat reference. Existing shared core datatype templates are cloned and patched with document-local namespace IDs.

The binary module tests live beside it under src/test/java. The expanded BinaryQueryResultWriterEncodingTest compares complete independent expected streams, round trips through the real parser, malformed-surrogate exception type and length, and I/O failures. State tests cover mutable BindingSet reuse and multiple documents on one writer. The two downstream server tests are TupleQueryResultViewTest methods testRender_BinaryTupleTimeoutAfterRowsPreservesTypedTerminalError and testRender_BinaryCircuitBreakerAfterRowsPreservesItsReason in tools/server-spring.

## Plan of Work and Milestones

First freeze the published writer as a renamed PublishedBinaryQueryResultWriter in the standalone harness. Copy the existing benchmark driver into the new evidence directory and extend its fixtures with long Latin-1, CJK, supplementary and sparse-Unicode labels. Each document still has 4096 rows and ordinary measurements remain per row. A separate SmallDocumentBenchmark reports per document for zero, one, and eight rows.

Next compare a strict surrogate-validation-plus-getBytes fallback with an exact-length allocating encoder and a bounded chunked encoder. Counting and validation must complete before emitting any string length or payload. Preserve the original encoder path for extreme lengths whose worst-case byte count cannot fit a signed int, rather than introducing unchecked arithmetic. Independently compare complete short-record staging with the existing multi-write path, prefix reuse, and a small initial scratch buffer with bounded growth. Each prototype has its own class and full source snapshot.

Promote a combination only after full-document parser validation and meaningful benchmark improvements. Keep helpers local to the writer and introduce no public API or production dependency. Run the expanded compatibility suite before and after production changes, including the exact byte/exception cases. Install the changed module before downstream server verification. Finally run matched multi-fork comparisons of the actual production class, keep noisy or unfavorable results visible, and collect restricted JFR CPU/allocation evidence separately from timing.

## Concrete Steps

Work from the repository root /workspace/scratch/966a0d01c43b/rdf4j. Maven uses Java 25, offline mode, and .m2_repo. The existing helper /workspace/scratch/d0a0e51b9386/toolchain/run.py supplies these toolchain settings and records full output. A tight-loop dependency install is:

    python3 /workspace/scratch/d0a0e51b9386/toolchain/run.py maven-build.log -o -Dmaven.compiler.showWarnings=false -T 1C -Pquick -pl core/queryresultio/binary -am install

Verify the binary module with tests enabled, without -am or -q:

    python3 /workspace/scratch/d0a0e51b9386/toolchain/run.py binary-writer-round2/pre-change-tests.log -o -Pquick -DskipTests=false -pl core/queryresultio/binary verify

Benchmark compilation and selection use JAVA_HOME=/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8 and binary-writer-round2/harness/bench.py. `compile` builds the standalone sources and actual production source. `smoke` runs the full parser validation. `jmh NAME -- OPTIONS` records command, log and JSON files under binary-writer-round2. Exact commands and source hashes are saved with each experiment; no timings are reused across changed fixtures.

## Validation and Acceptance

Accept only identical valid wire bytes and parser results. Invalid surrogate sequences must still yield MalformedInputException with input length one, wrapped by normal tuple callbacks, before any string prefix or payload is emitted. Existing error-marker prefix behavior and immediate completed-callback visibility remain covered. Caller streams must not be closed, and downstream IOException causes must remain intact.

Performance evidence must distinguish per-row large-document results from per-document small-result results. Report both time and allocated bytes, benchmark inputs and sink, JDK, fork/warmup/measurement counts, and shared-host limits. Reject new complexity that has no clear benefit or merely shifts a large cost into another important workload. Preserve extreme and mixed character distributions when assessing allocation.

## Idempotence and Recovery

All prototypes are additive, and the immutable published writer can be regenerated from Git. Keep untracked artifacts and the prior evidence directory. Continue the already authorized performance branch and update its remote ref with an expected-head check; do not overwrite unrelated work. Put measurement artifacts on the evidence branch. Verify source archive contents, patch application against the pinned baseline, and published blob identities before handing over links.

## Artifacts and Notes

The new harness, source analyses, logs, JMH JSON, restricted JFR recordings, report and downloads belong under binary-writer-round2. Existing dependency and build outputs remain local. JFR permits CPU/allocation/GC/load events only; environment, properties and process data are disabled and actual event types are inspected before publication.

## Interfaces and Dependencies

Use existing RDF4J model/query APIs, Java public String/IO APIs, JMH 1.37, and JDK Flight Recorder. No JDK-internal byte access, reflective access bypass, new library dependency, or LMDB-specific dependency may enter the generic writer.

Plan created for the continued optimization request on 2026-11-17 before new production edits.

Progress update: pre-change characterization passed all 267 tests. Initial large-string and record-assembly screens completed; a bounded inner-loop chunk variant and small-document screen are the remaining selection checks. Upstream main advanced by two server heartbeat-policy commits to be0c4c724f582e22d1a2936b87bb24969cc9154a; none changes the writer or benchmarked heartbeat stream implementation. The continued branch retains its pinned baseline for comparable measurements.

Selection update: promoted the combined candidate. Its screening mixed-long time was 34.7 microseconds/row versus the first published screen at57.6, with output-sized arrays eliminated. These are candidate-selection observations, not final claimed gains. Isolated bounded ASCII timings were noisy/unfavorable relative to basic chunks, so no independent ASCII-loop speedup is claimed. Final comparisons use the actual formatted production class against published 741b938, two fresh forks per implementation and workload, five one-second warmup and measurement iterations, run sequentially one workload at a time with implementation order alternating between workloads. Small results are measured separately per document.

Validation update: full offline root clean install succeeded (2m26s); binary verify passed267 tests and Spotless checks; server verify passed the two selected terminal-error methods. Production is frozen for final measurements and its full source hashes are captured in final-source-identity.json. No builds or tests run concurrently with timing.

Final primary measurements: all 15 heartbeat workloads and three small-document sizes completed using frozen production source 4a05a2794ff59c05b52cf0c7f70cacedc1a5602d080f1c5a55bae8599c455799. Short/wide rows and most long strings show clear local timing gains; identity-repeat, mediumUnicode, sparseUnicode, and full-query intervals overlap. SparseUnicode has strongly bimodal fork means in both implementations. Whole-query allocation rose7049→7451B/row ( +5.7%) despite lower writer CPU share in an independent JFR profile. This remains an explicit measured tradeoff, under allocation-profile inspection; do not hide it or claim all allocations fall. Small documents improved both time and allocation. Remaining gates are memory-sink controls, final profile attribution and publication/artifact verification.

Additional targeted gate: the primary query allocation increase is material enough to check once with reversed implementation order and longer warmup/measurement (two forks, ten 1s warmups, five 2s measurements). This recheck is prespecified to assess whether the allocation difference persists, not to replace primary results. Preserve both result sets and report any unresolved difference; no repeated search for a favorable run. Direct-memory controls are complete and retained separately.

Completion record: source commit c03171474130c67ca2eed2b78e3cc82cd4fba345/tree 32efd9174b509fd46a0c0c63168eaff03c69dd43 was published with an expected-head check. PR #6440 reports that exact head. Both diff-only patches reproduce this tree in isolated indexes, and all three complete Java sources were checked against Git. The evidence manifest seals the round-two results and download bytes for the separate evidence-branch publication; final remote blob checks are performed after its ref update.
