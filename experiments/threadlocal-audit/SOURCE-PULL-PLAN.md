# Consolidate repository source-pull owner binding

Living ExecPlan. Candidate is behavior-neutral boundary consolidation, not cancellation removal. Owner files: RepositoryValueIteration.java and RepositorySourcePullScopeTest.java. Parent owns build scheduling and performance evidence.

## Progress
- [x] Inspect prepareNext, SOURCE_PULL helpers, close and existing materialization tests.
- [x] Add seven adversarial scope cases; pre-change selection passed 21 tests.
- [x] Implement single source-only scope; same selection plus native safety passed 202 tests.
- [ ] Parent: broader lifecycle tests and matched benchmark/profile.

## Design and boundaries
Successful new row currently performs SOURCE_PULL get/set/restore separately around source.hasNext and source.next. Use one try/finally spanning only these two source calls and the existing intermediate closed.get guard. Keep availability, closed-after-hasNext and raw row in locals; no wrapper allocation. Restore before extractor.snapshot, sourceEnded handling, close, extraction and value waves. Preserve snapshot even if close raced during source.next, then use existing post-snapshot closed guard. Preserve exceptions, nested owner restoration, buffered hasNext and no speculative lookahead.

Test-only reflection reads raw SOURCE_PULL identity rather than isPullingSource (which hides a leaked owner after close). Tests prove normal, exhaustion, buffered, nested, exceptional and async-close paths. Existing RepositoryValueIterationTest, RepositoryPartialReadyTest and RepositoryGroupedWaveTest cover cold/composite values and reader lifecycle.

## Commands/evidence
Planned Maven: `experiments/threadlocal-audit/mvn.sh -pl core/sail/lmdb -Dtest=RepositorySourcePullScopeTest,RepositoryValueIterationTest,RepositoryPartialReadyTest,RepositoryGroupedWaveTest test`. Same selection before and after. Add feature-flag variants only under parent approval/lock. Do not derive speedup from test duration. No production edits before prequalification.

## Decision log
Use local booleans/raw row, not a tuple/helper object. Do not wrap the entire prepareNext: snapshot and cleanup must observe restored owner. Cancellation checks and materialization behavior remain unchanged.

## Outcome
Implementation and behavioral qualification complete. Performance measurement pending in the four-arm experiment.

## Validation receipts
- `source-pull-baseline.log`: original production with seven new scope cases plus existing RepositoryValueIterationTest, RepositoryPartialReadyTest, RepositoryGroupedWaveTest: **21 tests, 0 failures/errors/skips**.
- `source-pull-green-safety.log`: identical 21 plus ten classes from the prior 181-test cancellation/replay safety suite: **202 tests, 0 failures/errors/skips**.
- `source-pull-flags-green.log`: same 21 repository tests with `-Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.repository.groupedValueWaves=true`, executed via `surefire:test` against already compiled classes. See final log for result.
- `source-pull-copyright.log`: repository scanner ran and reports pre-existing unrelated shortened headers; no automatic rewriting performed. New test header is canonical, copied from RepositoryValueIterationTest, with Codex signature. No unrelated source modified intentionally.

The raw SOURCE_PULL assertions cover source.hasNext/source.next, nested owner restoration before inner snapshot and EOF close, outer snapshot, exception cleanup and async close. Counters prove the source path executes once for a buffered row and no further next is called when close wins during hasNext. Baseline production classes were frozen by the benchmark owner before implementation. No timing conclusion from test duration.

## Feature-flag qualification and frozen layer

The both-flags 21-test run did **not** pass: five pre-existing RepositoryValueIterationTest cases fail because their expected ordinary reader/decode observations are not reached in grouped mode. Before classifying this, ran the exact same 21 tests with both flags against the frozen **original** production classes: the same five methods failed (16 passed). Evidence: `source-pull-flags-baseline.log` versus `source-pull-flags-green.log`. No assertion was weakened and no production change was made in response.

Methods failing under both original and candidate with both flags:
- cancellationAndFatalErrorsDoNotWaitBehindBufferedRows
- multipleResultsBorrowTheTransactionReaderAndBufferedRowsSurviveItsCompletion
- sortedLaterDecodeFailureWaitsBehindValidRows
- parkedBatchReaderRenewsAcrossDictionaryMapGrowth
- dictionaryWavesReadRootsInKeyOrderWithoutReadingAnotherRow

The flag-compatible focused suite (seven new scope cases, two grouped-wave cases, one partial-ready case) separately passed **10/0/0** with both flags, recorded in `source-pull-flags-focused.log`. Baseline flag comparison and focused flag rerun used a JUnit Platform launcher with the exact Surefire classpath and the explicit baseline overlay first where applicable. This is not a full release-validation claim for the experimental flags.

Frozen only `RepositoryValueIteration.class` and `$PendingValue.class` under `source-pull/scope-layer/org/eclipse/rdf4j/sail/lmdb/`. `source-pull/scope-layer.sha256` records production source and both bytecode hashes. Outer class SHA256: `ad880c607f61f0f2175b743dc56d4566a7083ee18a00c7d13a8a229bfb11aa72`. Source SHA256: `a2b26f990af81874c717b17b2a2904783b76f5f1637369ec37772a455d1fe1ac`. Maven/JVM lock released for independent no-writer-guard build and four-arm benchmarks. Scope candidate does not include writer changes.
