# Profile and optimize binary tuple result writing

This execution plan follows `.agent/PLANS.md` and is maintained as measurements and implementation decisions change.

## Purpose / Big Picture

Reduce CPU time and allocation when RDF4J serializes tuple query results, while retaining the existing binary format, value equality rules, malformed-Unicode rejection, and stream ownership. The user identified `writeQName`, `writeString`, `writeRepeat`, and `LmdbLiteral.equals` as expensive. Establish their costs with running code before selecting changes. Preserve all experiments and publish the tested change on the HASMAC fork.

## Progress

- [x] Verified upstream main at `5434e51194e6c2e1aa745e5d65a4c998918d6073` on 2026-11-17.
- [x] Created isolated branch `GH-0000-binary-query-writer-performance`.
- [x] Built and profiled unchanged writer; 142 compatibility tests passed and two state bugs reproduced.
- [x] Compared isolated implementation alternatives, CPU samples, and allocations.
- [x] Verified 197 binary-module tests, two server terminal-error tests, and 64 full document smoke round trips.
- [x] Measured the selected implementation against unchanged main in separate JVM forks, with a separate memory control and one documented longer query recheck.
- [x] Published the implementation at 741b9389538e80214075692439a8cd25a8a9ae84; prepared and verified complete source and diff-only artifacts.
- [x] Finalized the report, complete evidence bundle, and verified downloads for publication on GH-0000-binary-query-writer-evidence.

## Surprises & Discoveries

The format requires UTF-8 strings with a four-byte encoded length. A binary record format does not remove the need to encode text. The current convenience call `CharsetEncoder.encode(CharBuffer.wrap(s))` allocates an input wrapper and an output buffer on each string. Replacing this call with `String.getBytes(UTF_8)` alone would change malformed-surrogate behavior from an exception to replacement bytes.

`LmdbLiteral.equals` already checks object identity. Its native equality shortcut also validates semantic freshness, revision, and canonical-value identity. These checks must not be replaced with unqualified numeric-ID comparisons.

The writer defines datatype namespaces before the literal record because a parser cannot accept a namespace declaration between a literal label and its datatype record. Empty rows retain the previous nonempty row as the repeat reference. Both rules constrain any refactor.

## Decision Log

Use an isolated worktree and Maven repository so earlier ownership experiments cannot contaminate this main-branch comparison. Follow Routine D because the work combines encoding, per-column repeat state, and bounded record reuse. Record passing baseline tests before any production change even though this routine does not require an artificial failing performance test.

Use deterministic preconstructed rows to isolate writer costs, and a real LMDB query export to measure the interaction with value materialization and equality. Measure wall time and allocated bytes with warmup and separate JVM forks. Collect Java Flight Recorder CPU and allocation samples for the unchanged and selected writers. Run timed experiments sequentially.

## Outcomes & Retrospective

The selected implementation changes only the binary writer's internals. It uses shared core datatype templates with writer-local namespace IDs, a bounded strict UTF-8 fast path with immediate framed bulk writes, indexed previous-column state, and batched NULL/REPEAT markers. All 197 binary-module tests and the two selected server terminal-error tests pass. Full parse-and-value smoke checks pass for baseline/current, eight workloads, two output sinks, and stable/churn namespaces (64 selections). Final JMH comparisons and matching CPU profiles are complete. The six short-value/repeat/wide serializer scenarios improve 1.88–2.44×; long-string timing is unresolved with overlapping intervals. One disclosed longer query recheck averages 8.68 to 7.88 µs/row, with overlapping 99.9% intervals. Both query comparisons remain in the report. See REPORT.md for evidence, limitations, and final source links.

## Context and Orientation

The writer is `core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java`. `handleSolutionImpl` compares each value to the same column in a retained BindingSet, and writes one-byte repeat records when equal. `writeLiteral` and `writeQName` build namespace-based IRI records. `writeString` converts Java strings to UTF-8 on every call. The corresponding parser and constants are in the same package. The tests live in the module's `src/test/java` tree.

LMDB values are implemented in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model`. IDs are meaningful only within compatible revisions of the same store; some legacy stores have multiple IDs for semantically equal values. The writer's generic module must not depend on LMDB internals.

## Plan of Work

First build the unchanged tree, run the binary result module tests, and save the original writer source as a separately named benchmark implementation. Add independent byte-level and Unicode tests before modifying production. Construct benchmark inputs outside timed sections, covering unique ASCII values, multilingual values, repeated identical objects, distinct equal LMDB values, and wider rows. Observe allocation and CPU stacks.

Next compare small changes independently: indexed previous-column values and identity check; reusable strict UTF-8 encoding; avoiding repeated datatype namespace/local-name work; and reducing repeated stream calls where compatible. Reject an alternative if it regresses common cases, changes emitted bytes, grows memory with the number of results, or changes stream flushing/ownership. Cache only bounded immutable information. Keep native equality semantics unchanged unless measurements and a separate semantic test justify a localized change.

Finally repeat matching benchmarks for the selected implementation and run all binary module tests plus a real LMDB export/parse comparison. Record exact JVM, base revision, benchmark parameters, raw results, limitations, and commands. Format changed Java files, commit the code and evidence, push to HASMAC, and provide a complete source archive and a diff-only patch with verified contents.

## Concrete Steps

Work from the repository root. Maven uses Java 25 and a private `.m2_repo`. The initial build command is `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`, with full output in `maven-build.log`. Run tests without `-am` or `-q`. Use the existing `mvnf` runner when applicable, otherwise run targeted Maven verify after installing dependencies. Keep compact pre-change evidence in `initial-evidence.txt` and full reports under `binary-writer-evidence`.

The benchmark harness and exact commands will be recorded here after their first successful execution. Never overlap builds or profiling with timed comparison runs.

## Validation and Acceptance

Require byte-identical output for valid Unicode and supported value kinds; preserve malformed-surrogate exceptions. Existing roundtrip tests and independent exact-byte tests must pass before and after. Verify datatype namespaces, repeated and absent columns, empty rows, language direction, and triple terms. A parsed export from a real LMDB query must match the expected rows.

Performance claims must name workload, sink, row count, JDK, fork count, and allocation unit. A synthetic writer-only speedup is not an end-to-end server speedup. Attribute CPU samples inclusively or exclusively and state which. Do not add overlapping inclusive percentages.

## Idempotence and Recovery

Keep the immutable original writer in the evidence harness and all raw measurements. Never reset the unrelated worktree. Retain untracked artifacts. Publish on the new GH-0000 branch because no issue number was provided. Changes are ordinary reversible commits; the wire format version remains unchanged.

## Artifacts and Notes

All durable task evidence and benchmark sources are under `binary-writer-evidence`. Large generated build outputs remain outside committed evidence unless needed to reproduce a result.

## Interfaces and Dependencies

No new production dependency or public configuration is planned. Use Java's standard IO and Unicode APIs, existing RDF4J model/query classes, existing JMH dependencies for measurements, and Java Flight Recorder for profiling.

Initial plan authored on 2026-11-17 before production changes.

2026-11-17 update: In-process JFR profiling works without external attach. Initial mixed fixture used 4,115 namespaces; retain it as a churn stress case and use an explicit stable namespace parameter for final comparisons. The screen showed marker batching reducing repeated-native-value time and bounded UTF-8 drastically reducing allocation, while persistent buffering was rejected because it changes callback visibility. Add a framed-string experiment to combine the four-byte length and payload in one immediate bulk write.

2026-11-17 update: Incorporated the user's core datatype suggestion as an immutable-by-encapsulation static template table covering XSD/RDF/GEO. Built-in local names and their byte lengths are encoded once per defining class loader. A writer clones a used QName record and patches its document-local namespace ID; custom and noncanonical datatype IRIs retain the generic path. Exhaustive template tests include nonzero namespace IDs, interleaved writers, and changed namespace order across reused documents. Source review found no actionable defect. The retained scratch array is 4,100 bytes per writer; this is a deliberate fixed memory cost and large-result benchmarks do not characterize tiny documents. Non-ASCII strings longer than 1,365 UTF-16 units retain the allocating strict fallback.
