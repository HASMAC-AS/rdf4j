# Experimental code deliverables

These artifacts preserve the complete code used by the qualified `identity-witness-v2` candidate. The experiment compiled an isolated prefix over the preserved ownership runtime; the selected production files in the checkout were not replaced.

The v2 source package additionally preserves the finite supervision controller used for the completed 36-JVM comparison and provides standalone reproduction instructions. Every member of the original source package remains byte-identical in v2. The original archive and receipts remain unchanged.

| File | Contents and verification |
|---|---|
| `direct-memo-complete-source-v2.tar.gz` | The original 33 members, the frozen supervision runner/README/tests, `REPRODUCTION-v2.md`, and a new manifest: 38 members in total, each read back and verified. `SOURCE-CONTENTS-v2.json` pins the 37 payload members; the external receipt also pins that manifest. |
| `REPRODUCTION-v2.md` | Self-contained source review/build and exact-runtime reproduction instructions, including dependencies, fresh finite supervision, audit, and independent analysis. |
| `complete-source-v2-receipt.json` | V2 archive size and SHA-256, every member's size and SHA-256, preservation of all original members, and unchanged production/supervision source checks. |
| `direct-memo-complete-source.tar.gz` | All four changed production classes in full, both new test classes, launcher and statistics sources, generators, contract runners, and benchmark/analyzer sources. All 33 archive members were read back and verified. `SOURCE-CONTENTS.json` pins their sizes and hashes. |
| `direct-memo-source.patch` | The complete four-file production diff, including the statistics helper required by the memo. Applying it to an isolated copy of the selected baseline reproduces the tested source bytes exactly. |
| `identity-witness-v2-prefix.tar.gz` | The six selected compiled classes, their manifest, the compile receipt, and restoration notes. All nine members were read back and verified. This prefix requires the unchanged common128 runtime; it is not a standalone RDF4J distribution. |
| `complete-source-receipt.json` | Source archive size, SHA-256, and completeness checks. |
| `patch-roundtrip-receipt.json` | Exact hashes of all four files reconstructed by applying the source patch. |
| `artifact-receipt.json` | Prefix archive and production-patch hashes. |
| `direct-memo-paired-ratios.png` and `.svg` | All six paired latency and allocation ratios for each primary comparison, with medians and observed ranges. The PNG was visually inspected; all 48 plotted ratios match the accepted independent analysis. |
| `plot-paired-ratios.py` and `direct-memo-paired-ratios.json` | Complete figure source and exact plotted values, input identity and output hashes. |

The source baseline is commit `97284c5229fc826b156275b15f105aac4dd801d6`, tree `4db5b366c00c68f5e89c7301612b5a821fc0e5d0`. The candidate passed 146 test executions, including both IR tiers, plus the six-process lifecycle qualification. The separate four-array counterexample is intentionally invalid and is not present in the candidate prefix.

The memo is off by default. The measured direct treatment uses both settings:

```text
-Drdf4j.lmdb.nativeValueMemo.enabled=true
-Drdf4j.lmdb.resultCoordinateTransport=off
```

Coordinate transport has its own existing setting and defaults to dense on this branch. The memo does not consult factor coordinates; turning transport off also removes the producer's transport work in the direct treatment. The ordinary and dense controls use the same compiled prefix with the memo disabled.

The prototype reserves its five arrays and any wide-projection scratch. It does not account for the wrappers, decoded payloads, or revision capabilities retained through its strong references. The one-time cutoff after 8,192 probes is also deliberately unchanged. These deliverables support reproducing and reviewing the experiment; the array ledger is not a production retained-memory bound.
