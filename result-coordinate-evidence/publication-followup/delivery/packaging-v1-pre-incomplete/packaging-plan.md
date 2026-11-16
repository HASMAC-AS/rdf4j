# Publication-ablation delivery plan

These additive helpers are preparation only. Execute them after the coordinator ends the timing quiet period and freezes the final report, decision and selected-source receipt. They do not run Java, builds, tests or benchmarks. Packaging, hashing and extraction must not overlap the timed JVMs. The prior compact-page delivery is immutable.

## Frozen inputs and remaining inputs

| Input | Required value |
| --- | --- |
| Repository | `/workspace/scratch/37853bcaea36/rdf4j` |
| Checkpoint for the full patch | `15fe7d5011d27d31d76436238363ee3f34b3ba50` |
| Previous selected source tree | `993e4358ab1968e6a329c762c5f5ab2109670eed` |
| Prior delivery | `/workspace/scratch/73b19a6d6f5c/lmdb-compact-page-deliverables` |
| New delivery | `/workspace/scratch/73b19a6d6f5c/lmdb-publication-deliverables` |
| Frozen primary protocol | `result-coordinate-evidence/publication-followup/protocol.json` |
| Protocol SHA-256 | `2a94e81f10d63ec177cb998d3270fb9cb48adee569ca1808370f6fc81465107b` |
| Completed primary output | `result-coordinate-evidence/publication-followup/comparison-v1` |
| Frozen experimental runtime | `result-coordinate-evidence/publication-followup/runtimes/publication-overlay-v1/runtime-descriptor.json` |
| Selected production materializer SHA-256 | `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e` |
| Selected core delta from the prior delivery | Empty; assert `--allow-empty-core-delta` |
| Final report, decision and selected-source receipt | Coordinator supplies final repository-relative paths after timing |

The explicit diagnostic inventory consists of six suites, in this requested reproduction order: `original512`, `overlay-all512`, `overlay-fanout-omit512`, `overlay-shared-omit512`, `overlay-shared-all256`, and `overlay-shared-omit256`. They contain eight JVM commands and 32 queries. `original512` pins the original diagnostic driver; all five overlay suites pin its additive v2. The catalog selects the exact driver named by each preserved protocol and verifies its source hashes.

## Products and verification

| Product | Scope and verification |
| --- | --- |
| `source-and-evidence/rdf4j-source.tar.gz` | Complete selected project source, initialized external submodule sources, carried-forward task source, and all new experiment Java/Python/documentation. Current production stays selected. Every archive member is checked against the source manifest. |
| `source-and-evidence/rdf4j-full.patch` | Full binary Git patch from checkpoint HEAD to the complete selected source snapshot. Verified in private Git indexes with exact-tree equality. |
| `source-and-evidence/rdf4j-evidence.tar.gz` | Fresh raw publication-phase evidence, receipts, protocols, all samples and diagnostic records, logs, final reports and supporting text. Stores, heaps, recording binaries and copied class trees are excluded. Source and evidence archives share the `rdf4j/` prefix. |
| `turn-delta/rdf4j-from-prior-delivery.patch` | Complete phase delta from previous tree `993e4358…`; forward and reverse exact-tree verification. |
| `turn-delta/rdf4j-publication-code.patch` | Explicitly empty core-only delta, with a receipt proving the selected LMDB source/test paths did not change. Do not present this zero-byte file as the new experimental patch. |
| `experimental-overlay/selected-to-publication-overlay.patch` | One-file experimental patch against the newly delivered selected source tree. Both this Git patch and the generator's original patch are applied and reversed as exact trees in private indexes. |
| `experimental-overlay/production-source/…/NativeResultMaterializer.java` | Complete experimental materializer source, with separately copied complete Java support sources, isolated tests, diagnostics and preimages under `experimental-support/`. |
| `runtime/rdf4j-publication-runtime.tar.gz` | Self-contained frozen classpath artifacts, complete phase text evidence, compile outputs, source snapshots and commands. Every archive member and all unique artifact hashes are reconstructed on archive readback. JDK and regenerated stores remain external. |
| `relocated-launches/` | Fresh extraction verified by SHA-256; five derivative classpaths and 44 commands with only Java/artifact/store/output/working-directory paths changed. No Java executes. These are launch plans, not a portable validation/analysis pipeline. |

The runtime catalog expects five compositions and 131 unique classpath artifacts: original 127, common harness 128, publication timing 129, original diagnostic 129, and publication diagnostic 130. The timing prefix contains only the outer materializer and plan classes. The frozen original nested `FactorPage` and `FactorMemo` class bytes remain selected. All four timing compiler outputs and both five-class diagnostic compiler outputs are preserved separately; unused emitted classes do not enter the classpath.

The catalog requires all 36 fixed timed JVMs and all 468 query records, including 180 measurements, plus all 12 successful dense-pair audits. Each included diagnostic suite must be complete and valid. Negative performance findings remain packageable: the mechanism effect screen does not have to pass. A package cannot turn a failed correctness/identity gate into a qualified comparison. Recorded timed and diagnostic JVM intervals must not overlap.

The helpers maintain immutable output semantics: an existing output is an error. Temporary source-patch operations use separate Git indexes and object stores; the real index and HEAD are checked before and after. The previous delivery is only read. No helper fetches dependencies, installs artifacts, modifies the production source or selects the fixture mask for production.

## Release order

The coordinator first creates the new top-level delivery directory and assigns the three final repository-relative paths below. These commands are a packaging recipe to execute only after timing and report writing have stopped.

```bash
PUB_REPO=/workspace/scratch/37853bcaea36/rdf4j
PUB_PRIOR=/workspace/scratch/73b19a6d6f5c/lmdb-compact-page-deliverables
PUB_OUT=/workspace/scratch/73b19a6d6f5c/lmdb-publication-deliverables
PUB_PHASE=result-coordinate-evidence/publication-followup
PUB_HELPERS="$PUB_PHASE/delivery"
```

Set `PUB_REPORT`, `PUB_DECISION`, and `PUB_SELECTED` to the final report, source-selection decision, and selected-source receipt paths relative to the repository. A second final report may be supplied with another `--report` in both selection and runtime packaging commands.

```bash
cd "$PUB_REPO"
python3 "$PUB_HELPERS/finalize-helper-provenance.py" \
  --prior-delivery "$PUB_PRIOR/source-and-evidence"
python3 "$PUB_HELPERS/prepare-selection.py" \
  --report "$PUB_REPORT" --decision "$PUB_DECISION" --selected-source-record "$PUB_SELECTED"
python3 result-coordinate-evidence/package-coordinate-deliverables.py \
  --repo "$PUB_REPO" --base 15fe7d5011d27d31d76436238363ee3f34b3ba50 \
  --manifest "$PUB_HELPERS/selection.json" --output "$PUB_OUT/source-and-evidence"
python3 "$PUB_HELPERS/build-turn-delta.py" \
  --repo "$PUB_REPO" --prior-delivery "$PUB_PRIOR/source-and-evidence" \
  --new-delivery "$PUB_OUT/source-and-evidence" --output "$PUB_OUT/turn-delta" \
  --allow-empty-core-delta
python3 "$PUB_HELPERS/build-selected-overlay-patch.py" \
  --repo "$PUB_REPO" --source-delivery "$PUB_OUT/source-and-evidence" \
  --runtime-descriptor "$PUB_PHASE/runtimes/publication-overlay-v1/runtime-descriptor.json" \
  --output "$PUB_OUT/experimental-overlay"
python3 "$PUB_HELPERS/bundle-publication-runtime.py" \
  --repo "$PUB_REPO" --output "$PUB_OUT/runtime" \
  --report "$PUB_REPORT" --decision "$PUB_DECISION" --selected-source-record "$PUB_SELECTED" \
  --protocol "$PUB_PHASE/protocol.json" --comparison "$PUB_PHASE/comparison-v1" \
  --diagnostic-suite original512 --diagnostic-suite overlay-all512 \
  --diagnostic-suite overlay-fanout-omit512 --diagnostic-suite overlay-shared-omit512 \
  --diagnostic-suite overlay-shared-all256 --diagnostic-suite overlay-shared-omit256
```

After publishing the runtime archive, extract it into a new directory. Invoke the **extracted** `publication-followup/delivery/verify-and-relocate-runtime.py` with `--repo` pointing to its extracted `rdf4j` directory, `--expected-manifest-sha256` equal to the runtime-verification receipt's manifest digest, `--java` pointing to the compatible Java executable, and `--output` pointing to a new sibling launch-plan directory. `--validate-only` may replace `--java`/`--output` when only verification is needed. The helper hashes files and checks Java executable existence; it does not run the JVM or assert its version.

The coordinator owns the consolidated final README, manifest, direct-copy verification, downloadable artifact existence checks and final publication. Do not alter repository reports or selection inputs after source packaging: a later edit requires a new complete package and new tree/patch receipts.

## Derivation

`package-coordinate-deliverables.py` and the shared `materializer-followup` inventory/archive helpers are reused unchanged. The previous compact delivery's `prepare-selection.py`, `build-turn-delta.py`, `bundle-compact-runtime.py`, `runtime_bundle_io.py` and `verify-and-relocate-runtime.py` are preserved and adapted only into this new directory. The selected-to-overlay publisher derives from the prior external `build-selected-candidate-patch.py`, whose prior delivery manifest records SHA-256 `b26284b35266df2d2c6b55df332fac738827dc720416948b89958db837fc763f`.

After timing, `finalize-helper-provenance.py` verifies the original helper hashes against the prior complete source manifest and records exact before/after hashes and unified diffs in `delta-helper-derivation.json`. It also records the unchanged generic source packager and shared inventory helper hashes. The prior package summary is pinned to SHA-256 `89522dc48ea234a8ee7addcdb446fe0f6a11e2e5d046a39f902cceb4059fcec0`; its full checkpoint patch is pinned to `4fcda12cce87a7dbf9e2e6b0a51bb456d6ccc729c7faaa638a8ff8e24ef5fd09`. No prior archive is re-created or overwritten.
