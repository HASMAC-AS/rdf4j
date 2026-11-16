# Experimental code deliverables

These artifacts preserve the complete code used by the qualified `identity-witness-v2` candidate. The experiment compiled an isolated prefix over the preserved ownership runtime; the selected production files in the checkout were not replaced.

| File | Contents and verification |
|---|---|
| `direct-memo-complete-source.tar.gz` | All four changed production classes in full, both new test classes, launcher and statistics sources, generators, contract runners, and benchmark/analyzer sources. All 33 archive members were read back and verified. `SOURCE-CONTENTS.json` pins their sizes and hashes. |
| `direct-memo-source.patch` | The complete four-file production diff, including the statistics helper required by the memo. Applying it to an isolated copy of the selected baseline reproduces the tested source bytes exactly. |
| `identity-witness-v2-prefix.tar.gz` | The six selected compiled classes, their manifest, the compile receipt, and restoration notes. All nine members were read back and verified. This prefix requires the unchanged common128 runtime; it is not a standalone RDF4J distribution. |
| `complete-source-receipt.json` | Source archive size, SHA-256, and completeness checks. |
| `patch-roundtrip-receipt.json` | Exact hashes of all four files reconstructed by applying the source patch. |
| `artifact-receipt.json` | Prefix archive and production-patch hashes. |

The source baseline is commit `97284c5229fc826b156275b15f105aac4dd801d6`, tree `4db5b366c00c68f5e89c7301612b5a821fc0e5d0`. The candidate passed 146 test executions, including both IR tiers, plus the six-process lifecycle qualification. The separate four-array counterexample is intentionally invalid and is not present in the candidate prefix.

The memo is off by default. The measured direct treatment uses both settings:

```text
-Drdf4j.lmdb.nativeValueMemo.enabled=true
-Drdf4j.lmdb.resultCoordinateTransport=off
```

Coordinate transport has its own existing setting and defaults to dense on this branch. The memo does not consult factor coordinates; turning transport off also removes the producer's transport work in the direct treatment. The ordinary and dense controls use the same compiled prefix with the memo disabled.

The prototype reserves its five arrays and any wide-projection scratch. It does not account for the wrappers, decoded payloads, or revision capabilities retained through its strong references. The one-time cutoff after 8,192 probes is also deliberately unchanged. These deliverables support reproducing and reviewing the experiment; the array ledger is not a production retained-memory bound.
