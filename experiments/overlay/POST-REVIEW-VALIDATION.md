# Post-review validation: c8f2848

## Regression

Validated production checkpoint `c8f2848318b9efb62599f8f493012e472a79e8f8`. All eleven shards exited 0; all 429 selected top-level evaluation test classes have isolated XML reports. **5,053 reported tests, 5,048 executed, zero failures/errors, five intentional benchmark skips.** No missing classes. The three production source hashes match the captured pre-run hashes. This is scoped evaluation-package coverage, not the whole repository or whole LMDB module.

The first three shards finished before an execution interruption. Shard 03 was incomplete and rerun in full; its original log is retained. Shards 04–10 then completed. No production changes or recompilation occurred during recovery. The selection and source provenance are retained in `spill-post-review-regression/`; the reconciled counts are published in `spill-post-review-regression-summary.csv`. All five experimental flags were on, one fork, 3 GiB test heap.

## Matched pre/post safety-hardening ABBA

A fresh full ABBA was run after recovery, excluding the interrupted earlier partial ABBA. Frozen pre-review and final post-review production classes use the same frozen harness, fixture, flags and dependency classpath. Class hashes were verified. The changes under comparison are cancellation/lifecycle safety hardening, not memo versus no memo. Each fork uses two warmups and three measured runs, 1,000,000 rows, 30 columns, all values consumed, 5 GiB heap and the default 640 MiB query budget.

| Fork | Measured times (seconds) | Mean |
|---|---|---:|
| A1 pre | 4.541, 4.560, 4.412 | 4.504 |
| B2 post | 4.759, 4.344, 4.327 | 4.477 |
| B3 post | 4.511, 6.334, 5.234 | 5.360 |
| A4 pre | 4.793, 6.195, 6.167 | 5.718 |

Pooled mean: pre **5.111 s**, post **4.918 s** (post 3.8% lower); pooled medians **4.676 s / 4.635 s** (0.9% lower). The later forks slow substantially on both sides. This supports no demonstrated steady-state performance regression from the safety fixes; **it is not convincing evidence of a speedup**.

All twenty query executions returned exactly 1,000,000 rows and checksum `-2986462905361302464`, used the compiled IR route and exercised the memo and primitive DISTINCT. All four JVMs exited 0. Budget returned to zero and spill create/delete counters balanced each run. This fixture needed no spill; peak accounted memory was 259,447,840 bytes. Consequently this ABBA does not benchmark spill performance. Logs contain existing prepared-fixture metadata warnings on initialization/close; these did not fail the query assertions or process.

Raw per-query figures are in `review-recovered-abba-summary.csv`; the rerun command is `run-recovered-abba.sh` with `POST_REVIEW_CLASSES=experiments/overlay/review-post-classes`. Local raw logs, timing JSON, class hashes and reports are preserved. No production code was changed in this validation.
