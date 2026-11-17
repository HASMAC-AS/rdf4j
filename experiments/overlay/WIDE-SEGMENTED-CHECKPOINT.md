# Segmented budgeted wide DISTINCT (in progress)

Baseline: bounded flat wide DISTINCT, public checkpoint 5bb76d, frozen classes `budgeted-wide-final-classes`. Parent verified 8m exact rows: generous 3.5GiB query cap 40.392s with 3,116,898,144-byte peak/no spill; default 640MiB cap 175.682s, migration at 1,048,576 rows, 20 spill files all closed. These baseline timings are retained, not overwritten.

Change scope: only NativeBudgetedWideDistinct's canonical primitive table is replaced. Legacy PrimitiveTupleTable, global/query budget policy, and NativeExpressionDomain spill implementation are untouched.

- Keys and full hashes use fixed 256-row chunks; existing payload chunks are never copied for growth.
- Chunk directories grow with conservative 8-byte reference accounting and simultaneous old/new reservation.
- Open-addressed bucket/fingerprint indexes still grow by rehash, reserving old plus new arrays. Hashing and exact equality match the former primitive algorithm.
- Probe duplicates before requesting capacity. Allocation-free inserts make no ledger reservation call. Denied chunk/index admission returns refusal without dropping or adding a key; delegate migrates exact insertion-order keys to its already-admitted spill membership.
- Clear retains chunks/index capacity and resets membership. Close drops references and releases ledger claims. Width and bucket limits prevent array arithmetic overflow; max bucket ceiling has an injectable test seam.
- Estimated retained 8m×30 memory is approximately 2.04GB including keys/hashes/buckets/directories, expected to fit an explicit 2.5GiB query cap. This is a prediction, not a benchmark result.

TDD red: `wide-segmented-red.log` runs existing tracker with 1100 unique 30-column rows and a 400,000-byte cap. Existing flat code migrates (expected 0 migrations, actual 1), although incremental chunks should fit. This is a behavioral failing test, not a missing-API test.

Focused validation and actual query telemetry pending compilation after parent's final flat-baseline regression shards. Added direct tests cover 5000-row chunk/directory growth and clear, collision chains/injected bucket ceiling, denied chunk growth retaining exact old keys, and invalid size arithmetic. Existing budget tests retain global/shared limits, semantic aliases, spill/cleanup and batch/null coverage.

## Focused green

`wide-segmented-green.log`: 31 tests, zero failures/errors/skips, completed 2026-11-17 08:39:24 UTC. The 1100-key/400,000-byte behavioral test now remains primitive with no migration. Direct chunk/ceiling tests pass; prior exact spill, independent/shared budget, semantic alias and lifecycle regressions pass.

Only NativeSegmentedTupleTable and NativeBudgetedWideDistinct production files were compiled. Full classes snapshot saved to `experiments/overlay/segmented-only-classes`, retaining the unchanged prior NativeExpressionDomain implementation for isolated benchmarks. No global budget policy changed. Parent owns matched 8m runs and will report measured timing/peak/spill counts separately.

## Actual8-million-row measurement

Sameverifiedfixture,5GiBheap,compiledIR,memo+partial-ready,onecoldquery/no warmup perJVM:
- Explicit2.5GiB/query (unchanged2.5GiBglobal):40.382837s;peak2,042,135,040B;no spill;8mprimitive rows. This lowers accountedpeak~34.5% versus flat3,116,898,144B while preserving approximately40s runtime. The perquerycap is an experiment setting, not a policy/default change.
- Default640MiB/query:179.816675s;peak671,067,808B (under671,088,640cap);2,642,432primitive then5,357,568spill rows;20files created/deleted. Flatdefault was175.682105s. No default-budget speedwin: once migration occurs, all8mkeys are eventually written, including the retainedprefix.
Both return exactchecksum-3441391656945048864,8mrows,compiledroute/memohitproof,and0budgetbytes afterclose. Single-run timing, not statisticalclaim. BulkspillI/O follows as separateflaggedexperiment.
