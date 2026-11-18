# Broader in-memory Workbench matrix: fixture and safety audit

Read-only support for the authorized base `5ac89b5` versus qualified depth+serializer `d94617b3` validation. No production optimization, JVM/build or new timing was performed for this note. Inventory owns protocol, fixtures, CPU and execution.

## Fixture algebra and useful matrix

The existing loader is `read-depth/ReadDepthQueryBenchmark.java:load`; the current HTTP query is `depth-workbench/query.mjs`. Let G be groups and F the positive per-arm fanout. Each group has one type, label and seed, and three arms with F nodes each. Each node has eight functional attribute predicates except a7 is absent when member index m is divisible by seven.

- Statements: `G * (3 + 27*F - 3*ceil(F/7))`. At F=10 this is **267*G**.
- Output cardinality: **G*F^3**. Every arm supplies F nodes, and optional attributes never multiply those nodes. Projection includes group and all three node IRIs, so every member triple remains distinguishable and DISTINCT does not reduce this fixture's row count.
- Six-slot query: s, label, seed, n0, n1, n2.
- Thirty-slot query: s, label, seed, then for each arm a: na and va_0 through va_7. Keep the same nested OPTIONAL body; only projection width changes. Optimizer pruning/order/certification can still differ with projection, so record actual routes rather than assuming identical internal work.

| Workload | Groups | Fanout | Rows | Slots | Statements | Raw 8-byte full-key payload if globally retained |
|---|---:|---:|---:|---:|---:|---:|
| Smaller narrow | 10 | 10 | 10,000 | 6 | 2,670 | 480,000 bytes |
| Existing narrow | 100 | 10 | 100,000 | 6 | 26,700 | 4,800,000 bytes |
| Wider | 100 | 10 | 100,000 | 30 | 26,700 | 24,000,000 bytes |
| Approved larger narrow | 250 | 10 | 250,000 | 6 | 66,750 | 12,000,000 bytes |

A 10k output window on the existing 100k fixture is **not** a 10k query workload: browser evaluation may drain the same 100k query for the exact total. Use the matching G fixture (or explicitly describe a different query restriction/plan). Keep request batch size equal to full expected rows for complete-output comparisons. The larger fixture needs a newly loaded store; do not reuse the 100-group stamp under a different name.

These raw key payload figures are explanatory lower bounds, **not** budget estimates. Hash buckets, fingerprints, chunk slack/growth overlap, fallback workspace, memo/runtime values and other operators consume memory; grouped DISTINCT may instead avoid global retention. The proposed cases look plausible under the unchanged640MiB query cap, but only measured witnesses establish the no-spill claim. Do not silently enlarge budgets or relabel a spill case as in-memory.

## Independent complete-term oracle

Use `ns = urn:overlay-optional:` and parse canonical group/member integers from the emitted IRIs. Require the exact variable list for the selected width, every row's exact width, and the following full compact-term arrays:

- s: `[0, ns + "group/" + g]`, with `0 <= g < G`.
- label: `[2, "Group label " + g, "http://www.w3.org/2001/XMLSchema#string"]`.
- seed: `[2, "Seed " + g, "http://www.w3.org/2001/XMLSchema#string"]`.
- na: `[0, ns + "node/" + g + "/" + a + "/" + ma]`, with `0 <= ma < F`.
- va_c for c=0..3: `[2, "Shared lexical prefix é 漢字 " + g + "/" + a + "/" + ma + "/" + c, null, "nb"]`.
- va_c for c=4..7: **null when ma%7==0**, otherwise the same language-literal formula above.

The whole four-column suffix is null when a7 is absent, because a4..a7 are one nested OPTIONAL basic graph pattern. Validating only v_a7 as null would be wrong. At F=10, members 0 and 7 have this missing suffix. Never normalize missing/null slots into literals or ignore RDF term type/language/datatype.

For every row compute `index = ((g*F + m0)*F + m1)*F + m2`. Reject a previously set bit in a bitset of G*F^3 bits. Require exactly G*F^3 rows and all terms to match. There are exactly that many legal distinct indices, so valid terms + no duplicates + exact count proves the complete expected result bag without relying on hash collision assumptions or row ordering. For 250k rows the bitset is 31,250 bytes. No ORDER BY means row sequence is not a cross-arm correctness contract.

Optional order-independent full-term checksums can be independently generated from these formulas and retained as compact evidence, but should supplement the direct oracle. A byte/line SHA depends on row order and record batching and is not the only cross-arm criterion. The current client hardcodes six columns, F10, group bound expected/1000, and two six-slot 100k hash constants. Changing EXPECTED_ROWS alone is insufficient, and those constants must not be reused for 30 slots or a different cardinality.

## Acceptance and measurement witnesses

For every fixture and both arms:

1. Freeze query, loader/stamp, database files and production family origins before measurement. Confirm statement count matches formula and data/adjacency readiness completes. No use of archived holder, cancellation, ordered-values or test observer layers.
2. Run an untimed full-term validation with full output and the independent oracle. Confirm exactly one completed terminal record, expected total and emitted rows, no error records, proper vars/header/protocol and complete EOF.
3. Preserve actual Server binary transport and Workbench compact NDJSON negotiation. Keep request, forced route, memo/overlay/parallel flags and budgets identical within each contrast.
4. Require compiled route evidence and no interpreted fallback on these intended compiled cases; record grouping/primitive/object distinct witnesses. Projection changes can affect certification, so compare corresponding routes and disclose plan differences across workloads.
5. Require no native spill-file creation/deletion delta, no migration if exposed, and ledger usage restored to its pre-request value. Existing observation loop exposes spill counts but not NativeBudgetedWideDistinct.MIGRATED/ADMITTED; adding read-only harness counters would strengthen the in-memory qualification. Process peak is cumulative across requests, not an exact per-query peak, and does not include all process/native/heap memory.
6. Keep validation CPU/hash/bitset work out of clean timing, but count/terminal/resource witnesses in every timed request. Report first row, complete window, terminal and EOF separately. Full response width changes encoded/compressed byte volume, parser/serializer and transport work; do not interpret it as a pure engine-only width cost.
7. Prespecify balanced fresh-server runs, warmup and trial counts, no outcome-dependent reordering or cap changes. If any approved case cannot qualify under fixed limits, report that boundary instead of quietly substituting another workload. A 1m/6-slot case (G=1000) is only a possible future unrun extension, not part of this bounded matrix.

## Safety evidence versus merge readiness

Qualified depth+serializer is promising experimental code, not automatically release-ready:

- Depth-only frozen-runtime qualification: **1,705 passed, 3 gated skips across 107 selected classes**. This covers substantial read/lifecycle/replay/map-growth/materialization/cancellation/worker behavior, but not every LMDB class, all 419 native-evaluation classes, all 263 queryalgebra classes, or the repository suite. Some newer source tests were absent from the historical compiled bundle. Gated replay-fixture checks remain separate evidence, not silently part of this total.
- Serializer: **1,561 full Workbench JVM-module tests passed**, then a test-only structural addition and **25/25 focused tests**, including isolated base and depth runtime overlays. Relevant checks include exact output, nested terms/directions, prepacking failures, oversized row disconnect, progress atomicity, depth-error partial bytes/diagnostics and failure-close semantics. The implementation-facing Jackson ClassUtil dependency requires maintenance care across Jackson upgrades.
- Subsequent ordered-values experiments and their larger test totals are **not** the candidate in this matrix. Their performance/safety results should not be credited to `d94617b3` without identifying the actual source/runtime being tested.
- The actual application runtime was reconstructed from real modules and frozen dependencies after recovery, not proven byte-identical to a historical release distribution. Final merge should validate the intended release build and dependency set.
- Browser execution was blocked before navigation by Chromium `process_singleton_posix.cc: socket() failed: Operation not permitted`. No DOM/paint/IndexedDB/row-worker/render latency or browser interaction pass exists. Successful frontend build and Node HTTP measurements do not establish browser behavior.
- This matrix is serial in-memory HTTP validation, not concurrent production load, durable-storage correctness, spill stress, broad engine-mode coverage or repository-wide release CI. No approval to merge follows from a benchmark.

Relevant detailed evidence: `read-depth/broad-safety/RESULTS.md`, `compact-json/RESULTS.md`, `serializer-workbench/RESULTS.md`, and `depth-workbench/RESULTS.md`.
