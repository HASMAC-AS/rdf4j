# Segmented + bulk + membership reuse regression

## Scope and provenance

Checkpoint **42c43d5af07d35ae7592ea1057f9fe72b2d07439**, before the subsequent cancellation/review fixes. Production SHA-256 checks for NativeExpressionDomain, NativeBudgetedWideDistinct and NativeSegmentedTupleTable were captured before compilation and verified unchanged after all tests.

Module-only offline `test-compile` succeeded (1,162 test source files). Selection is every compiled top-level `*Test.class` directly in `org.eclipse.rdf4j.sail.lmdb.evaluation`, with fully qualified names frozen before execution: **426 classes**, divided into eleven shards (ten of 40, final 26). This is **not** a full-module test claim, nor coverage of nested subpackages or tests outside this explicit selection.

Every shard used one fork with a 3 GiB maximum heap and all five flags enabled:

- `rdf4j.lmdb.nativeValueMemo.enabled=true`
- `rdf4j.lmdb.repository.partialReady=true`
- `rdf4j.lmdb.nativeDistinct.widePrimitive=true`
- `rdf4j.lmdb.expressionDomain.bulkIdRecordIo=true`
- `rdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true`

Production classes were compiled once before the sweep. The shared benchmark lock was released between shards so the separate frozen ordered-route experiment could run without concurrent heavy workloads. No production optimization edits were made by this regression worker.

## Results

**All eleven Maven invocations exited successfully.** Isolated XML reports cover all 426 selected classes with none missing:

- 5,045 reported tests
- 5,040 executed
- 0 failures
- 0 errors
- 5 intentional benchmark skips

Skipped: one `LmdbRealEstateQueryTimingTest` opt-in timing case (`-Drdf4j.realestate.timing=true`), plus four `SmallTransactionsTest` manual throughput cases explicitly disabled because they lack correctness assertions and exceed automatic test budgets.

Per-shard details: `spill-final-regression-summary.csv`. Exact selection, compile log, source provenance, shard exit codes, logs and selected TXT/XML reports are retained under `spill-final-regression/`. No failure isolation was necessary.

## Reproduction

`run-spill-final-regression.sh` performs the module-only compile and exact sharded selection. `summarize-spill-final-regression.py` reconciles selected classes with copied XML and produces the totals. The script uses no `-am` or quiet test execution.

These results are **pre-review-fix** evidence. Subsequent cancellation/migration changes require a distinct post-change run and cannot inherit this result automatically.
