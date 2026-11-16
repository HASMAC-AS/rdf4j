# Publication-ablation delivery plan

These additive helpers are preparation only. Execute them after the coordinator ends every timed or targeted diagnostic JVM and freezes the final report, decision and selected-source receipt. They do not run Java, builds, tests or benchmarks. Packaging, hashing and extraction must not overlap JVM experiments. The prior compact-page delivery is immutable.

The primary study stopped incomplete after nine of 36 planned JVMs: 117 queries and 45 measurements. Packaging preserves that stopped state, its original error and failed audit. It supplies no accepted timing comparison, replacement run or lower acceptance gate. The prepared complete-study helpers were copied byte-for-byte into `packaging-v1-pre-incomplete/` with a SHA-256 preservation receipt before this adaptation; they had never executed packaging.

## Frozen inputs and remaining inputs

| Input | Required value |
| --- | --- |
| Repository | `/workspace/scratch/37853bcaea36/rdf4j` |
| Checkpoint for the full patch | `15fe7d5011d27d31d76436238363ee3f34b3ba50` |
| Previous selected source tree | `993e4358ab1968e6a329c762c5f5ab2109670eed` |
| Prior delivery | `/workspace/scratch/73b19a6d6f5c/lmdb-compact-page-deliverables` |
| New delivery | `/workspace/scratch/73b19a6d6f5c/lmdb-publication-deliverables` |
| Runtime readback extraction | `/workspace/scratch/73b19a6d6f5c/publication-runtime-extraction-v1` — outside the delivery directory |
| Frozen primary protocol | `result-coordinate-evidence/publication-followup/protocol.json` |
| Protocol SHA-256 | `2a94e81f10d63ec177cb998d3270fb9cb48adee569ca1808370f6fc81465107b` |
| Stopped primary output | `result-coordinate-evidence/publication-followup/comparison-v1` |
| Frozen experimental runtime | `result-coordinate-evidence/publication-followup/runtimes/publication-overlay-v1/runtime-descriptor.json` |
| Selected production materializer SHA-256 | `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e` |
| Selected core delta from the prior delivery | Empty; assert `--allow-empty-core-delta` |
| Final report | `result-coordinate-evidence/publication-followup/final-report.md` |
| Selection decision | `result-coordinate-evidence/publication-followup/decision.json` |
| Selected-source receipt | `result-coordinate-evidence/publication-followup/selected-source-verification.json` |
| Resolver diagnostic output | `result-coordinate-evidence/publication-followup/diagnostic/resolver-variation/probe-v1` |
| Resolver diagnostic protocol SHA-256 | `0ba7dd6c7bce031e8c7495e6be958c13444a670ada1b966dde2fdfcb1f54ac5d` |
| Resolver diagnostic scope | Four shared-tail JVMs in all/omit/omit/all order, eight warmups plus five observed queries each; separate from the stopped timing study |

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
| `relocated-launches/` | Fresh extraction verified by SHA-256; five derivative classpaths and 21 recorded commands after the four-command resolver probe completes: nine stopped-primary, eight initial diagnostic and four resolver diagnostic commands. Only Java/artifact/store/output/working-directory paths change, including the new `gc.log` paths. No unexecuted primary commands are fabricated. No Java executes. These are launch plans, not a portable validation/analysis pipeline. |

The runtime catalog expects five compositions and 131 unique classpath artifacts: original 127, common harness 128, publication timing 129, original diagnostic 129, and publication diagnostic 130. The timing prefix contains only the outer materializer and plan classes. The frozen original nested `FactorPage` and `FactorMemo` class bytes remain selected. All four timing compiler outputs and both five-class diagnostic compiler outputs are preserved separately; unused emitted classes do not enter the classpath.

The catalog requires the exact first nine positions of the unchanged 36-JVM protocol, all 117 raw query records and 45 measurements, the original stop error, and all three recorded dense-pair audits. It rejects extra, replaced or missing primary fork directories. It rebuilds the pair audits from raw records and requires the preserved sole mismatch: shared-tail round 1, warmup iteration -7, with 32,833 resolver calls for DENSE/all and 32,834 for DENSE/omit. Route/tier and adjacency comparisons at that query remain equal. The original summary must retain `complete: false`, an empty comparisons array and unevaluated mechanism evidence. All raw underlying JVM results remain included; passing those result/route/ledger checks does not undo the failed mechanism-control gate.

The eight base diagnostic commands must remain complete and valid. The four resolver diagnostic commands have their own frozen protocol, source pins, runtime, prior reference records, command arrays, GC logs, page/owner records, raw samples and analysis. The catalog checks the fixed all/omit/omit/all order, all 52 observations, the unchanged 130-entry masked diagnostic runtime and the recorded 900-second per-process deadline. It keeps their purpose separate from the primary experiment and verifies the summary's `originalTimingEventCauseEstablished: false`. These runs do not replace a main fork, rescue the performance comparison or supply accepted ratios. Recorded JVM intervals must not overlap. Runtime and relocation manifests explicitly record `acceptedTimingComparison: false`.

The final query inventory is separated by purpose: 117 stopped-primary queries, 32 initial diagnostic queries and 52 resolver diagnostic queries, totaling 201. The probe's last five queries per JVM retain the harness's `measurement` label but remain diagnostic observations. Do not aggregate them with the 45 primary measurements or infer latency, allocation, GC cost or natural miss rates. The narrow adapter requires the probe to finish its declared four-run scope; a stopped probe must first receive an exact partial-prefix inventory instead of being mislabeled complete.

The helpers maintain immutable output semantics: an existing output is an error. Temporary source-patch operations use separate Git indexes and object stores; the real index and HEAD are checked before and after. The previous delivery is only read. No helper fetches dependencies, installs artifacts, modifies the production source or selects the fixture mask for production.

## Release order

The coordinator first creates the new top-level delivery directory and assigns the three final repository-relative paths below. These commands are a packaging recipe to execute only after timing and report writing have stopped.

```bash
PUB_REPO=/workspace/scratch/37853bcaea36/rdf4j
PUB_PRIOR=/workspace/scratch/73b19a6d6f5c/lmdb-compact-page-deliverables
PUB_OUT=/workspace/scratch/73b19a6d6f5c/lmdb-publication-deliverables
PUB_EXTRACT=/workspace/scratch/73b19a6d6f5c/publication-runtime-extraction-v1
PUB_PHASE=result-coordinate-evidence/publication-followup
PUB_HELPERS="$PUB_PHASE/delivery"
PUB_REPORT="$PUB_PHASE/final-report.md"
PUB_DECISION="$PUB_PHASE/decision.json"
PUB_SELECTED="$PUB_PHASE/selected-source-verification.json"
```

The three final report/decision/verification files must have stopped changing before selection. A second final report may be supplied with another `--report` in both selection and runtime packaging commands.

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
  --diagnostic-suite overlay-shared-all256 --diagnostic-suite overlay-shared-omit256 \
  --resolver-variation "$PUB_PHASE/diagnostic/resolver-variation/probe-v1"
```

After publishing the runtime archive, extract it into the new `PUB_EXTRACT` directory above, outside `PUB_OUT`. Keep only archives, complete source/support files and receipts in the final delivery; do not duplicate the extracted classpath trees there. Set `PUB_RUNTIME_MANIFEST_SHA` from `runtime/runtime-verification.json`'s `manifestSha256` field, then invoke the **extracted** helper:

```bash
mkdir -- "$PUB_EXTRACT"
tar -xzf "$PUB_OUT/runtime/rdf4j-publication-runtime.tar.gz" -C "$PUB_EXTRACT"
python3 "$PUB_EXTRACT/rdf4j/$PUB_HELPERS/verify-and-relocate-runtime.py" \
  --repo "$PUB_EXTRACT/rdf4j" --expected-manifest-sha256 "$PUB_RUNTIME_MANIFEST_SHA" \
  --java /workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8/bin/java \
  --output "$PUB_OUT/relocated-launches"
```

`--validate-only` may replace `--java`/`--output` when only verification is needed. The helper hashes files and checks Java executable existence; it does not run the JVM or assert its version. Relocation receipts and launch plans remain under the deliverables directory, while their referenced runtime files remain in the separate extraction.

The coordinator owns the consolidated final README, manifest, direct-copy verification, downloadable artifact existence checks and final publication. Do not alter repository reports or selection inputs after source packaging: a later edit requires a new complete package and new tree/patch receipts.

## Derivation

`package-coordinate-deliverables.py` and the shared `materializer-followup` inventory/archive helpers are reused unchanged. The previous compact delivery's `prepare-selection.py`, `build-turn-delta.py`, `bundle-compact-runtime.py`, `runtime_bundle_io.py` and `verify-and-relocate-runtime.py` are preserved and adapted only into this new directory. The selected-to-overlay publisher derives from the prior external `build-selected-candidate-patch.py`, whose prior delivery manifest records SHA-256 `b26284b35266df2d2c6b55df332fac738827dc720416948b89958db837fc763f`.

After every experiment stops, `finalize-helper-provenance.py` verifies the original helper hashes against the prior complete source manifest and records exact before/after hashes and unified diffs in `delta-helper-derivation.json`. It also verifies the prepared v1 snapshot and records the complete-study-to-stopped-study adaptation, plus the unchanged generic source packager and shared inventory helper hashes. The prior package summary is pinned to SHA-256 `89522dc48ea234a8ee7addcdb446fe0f6a11e2e5d046a39f902cceb4059fcec0`; its full checkpoint patch is pinned to `4fcda12cce87a7dbf9e2e6b0a51bb456d6ccc729c7faaa638a8ff8e24ef5fd09`. No prior archive is re-created or overwritten.
