# Prospective frame directory guard comparison

This study is separate from the unexecuted namespace-memo draft and from all earlier disabled-adjacency timings. Its source hypothesis and observational evidence are in `../DIAGNOSTIC-FINDINGS.md`.

The fixed design uses three arms: original ordinary reuse (`off`), original dense coordinates (`dense`), and the verified frame directory guard candidate (`dense`). It runs all six permutations, so each arm appears twice in each position and every ordered pair occurs equally often. Every arm gets a fresh JVM, eight warmups and all five measured queries: 18 JVMs, 144 warmup queries and 90 measured queries. The fixed fixture has M=100, R=10, width=12 and two statuses, with full result consumption.

All arms use the same uninstrumented v1 common harness, which enables native evaluation and direct adjacency, awaits actual maintenance/current-revision readiness, and reads existing adjacency metrics outside query timing/allocation windows. The preserved post-main root launcher and every non-production dependency remain identical. The timing binary contains none of the namespace diagnostic counters. The candidate changes only the original LMDB production entry; the shared harness prefix is prepended afterward.

## Freeze only after the candidate passes its matching correctness gate

The parent owns production source changes, matching pre/post validation and the production snapshot. Its `../freeze-candidate-runtime.py` creates the candidate descriptor, complete modified source, exact production patch, validation records and production/dependency manifests. Independent review must identify the exact patch SHA-256.

From the repository root, bind the verified candidate and reviewed patch into a new immutable protocol:

```bash
python3 result-coordinate-evidence/namespace-followup/frame-directory-guard/run-comparison.py \
  --protocol result-coordinate-evidence/namespace-followup/frame-directory-guard/protocol-draft.json \
  --freeze-candidate result-coordinate-evidence/namespace-followup/runtimes/frame-directory-guard/runtime-descriptor.json \
  --frozen-protocol result-coordinate-evidence/namespace-followup/frame-directory-guard/protocol.json \
  --reviewed-production-patch-sha256 REVIEWED_SHA256
```

`REVIEWED_SHA256` is a required explicit identity, not a guessed value. Freezing invokes no JVM. It validates the candidate's passing source-matched records, the single permitted production source delta, the original 126 unchanged base entries, common uninstrumented harness, diagnostic target evidence and source/bytecode identity.

Then validate without launching Java:

```bash
python3 result-coordinate-evidence/namespace-followup/frame-directory-guard/run-comparison.py \
  --protocol result-coordinate-evidence/namespace-followup/frame-directory-guard/protocol.json \
  --validate-only
```

## Execute only in an exclusive JVM slot

```bash
python3 result-coordinate-evidence/namespace-followup/frame-directory-guard/run-comparison.py \
  --protocol result-coordinate-evidence/namespace-followup/frame-directory-guard/protocol.json \
  --output result-coordinate-evidence/namespace-followup/frame-directory-guard/comparison-v1 \
  --slot-released
```

Every output directory must be new. Exact argv, raw JSONL, console, store, exit status, result/route validation, root-host audit and consumed identities are retained. Driver/helper/common-harness identities are checked before each JVM; full runtime identities are checked before and after the study. No overlapping benchmark, test/build JVM or optional background hashing is permitted during measurements.

Each query must satisfy the full expected checksum and row count, nonnegative measured allocation/timing metrics, and zero ledger endpoints. Configuration pins direct adjacency PREFER, a 1 GiB global ledger limit and 256 MiB query limit. Every query must have one semantic native compilation, successful normal AUTO selection, native IR participation, actual query-time direct-adjacency lookup hits, ACTIVE state and stable applied/current revision. DENSE must open its coordinate owner without initial/growth refusal. Measured queries must use compiled IR only, and every measured route/tier histogram must match the first qualified fork. The absolute post-main root audit must reconcile 13 native compilations and zero hosted-generic roots.

Any failed process, invalid result, changed identity or unqualified/changed route preserves the fork and stops the study. There are no replacement forks, favorable-tail selection, extra rounds or forced route retries. A changed AUTO route remains a recorded result; it cannot establish this isolated target comparison.

## Analysis

For each fresh JVM, use the median of all five measured queries. For each matched round, divide candidate/comparator JVM medians, then report the median and full range of the six ratios. Compare candidate DENSE/original DENSE, candidate DENSE/original OFF, and original DENSE/original OFF. Keep allocation, preparation, evaluation, first-row, consumption, close and ledger-peak ratios distinct from total latency. Report resolver calls and route evidence alongside them.

All six total-latency ratios below one support a consistent gain on this fixture. A range crossing one is inconclusive. The raw warmups preserve initial preparation costs but are not pooled with warmed primary measurements. Diagnostic timings are never used as performance estimates. Query-ledger peaks describe admitted query reservations, not all JVM heap or the separate direct-adjacency budget.
