# Materializer follow-up delivery plan

This is a preparation-stage record. When it was drafted, the helpers described here had not been run and no follow-up archive had been created. The previous complete source, evidence, runtime and add-on archives remain unchanged. Run packaging only after the comparison, final reports, recording sanitization and all evidence writers have finished.

## Deliverables and preserved boundaries

| Output directory | Contents | Relationship to previous delivery |
| --- | --- | --- |
| `result-coordinate-materializer-deliverables/` in the repository | Complete updated project source, selected text evidence, full binary Git patch against `15fe7d5011`, manifests and verification receipt | A new complete checkpoint package; the old directory is untouched |
| `result-coordinate-materializer-deltas/` in the repository | Complete selected-source delta from the previous delivered tree, a small LMDB source/test-only delta, forward/reverse verification and checksums | Applies to the exact previous delivery, not merely the original branch checkpoint |
| `coordinate-materializer-runtime-delivery/` beside the repository | One self-contained comparison runtime archive, its manifest and readback receipt | Contains the original 127 artifacts once plus both candidate production trees; no earlier runtime download is needed |

Both complete source/evidence archives and the comparison runtime archive extract under `rdf4j/`. They can be extracted together into a new directory, preserving relative report and evidence links. All publication destinations must be new. The helpers refuse to overwrite an existing output.

The runtime archive contains the exact benchmark artifact bytes and original metadata/scripts. It also contains **sanitized delivery copies** of the two diagnostic recordings, with receipts linking them to the original raw JFR hashes. Original recordings remain private and are not copied into downloads. The sanitization removes exactly `jdk.InitialEnvironmentVariable`, `jdk.InitialSystemProperty`, `jdk.SystemProcess` and `jdk.InitialSecurityProperty` events, verifies the retained performance evidence, and records a names/presence audit without publishing values. A separately verified sanitization receipt is a packaging prerequisite.

## Existing delivery identity

The following values were read from the existing verified receipts. They have not been recomputed during the timing quiet period.

| Item | Preserved value |
| --- | --- |
| Checkpoint commit | `15fe7d5011d27d31d76436238363ee3f34b3ba50` |
| Previous delivered Git snapshot tree | `a5a1135fc91970dcd6c130cda5e8561f9116b618` |
| Previous package summary SHA-256 | `3bf898677e5cf677b7d3ddc6fa1c99ff0f485e0d40641d1b7479113f62c0e82f` |
| Previous checkpoint patch SHA-256 | `d51d0ce7222f3fda31137202261a18b40dc05c41100dbb9271c7ed73e7060d39` |
| Previous complete source archive SHA-256 | `6fb5f2dfb29d1b71dc94b54a853969ee61a80b5915bf53b966923edbbfbba49c` |
| Previous evidence archive SHA-256 | `8df79ff7cf903929ee3107910fd0e1b3410b8f441f7d9f9e967ac35610a322c8` |
| Exact measured original source-map SHA-256 | `34767935d60f32ba61adfb44495c607e17ca760d5d2f77a17ddb2862c4f7a8ac` |
| Original production runtime tree SHA-256 | `f822ad378729e2ebdd5c7e91d2420e9dbf1eb92aab382e76c35fba00989eab8c` |

The previous package included 19,058 source entries and 10,015 evidence entries, including the exact external Hugo theme commit `85d93d0e4a64c04809fc6cc17af10d102c988ca6`. Its receipt recorded archive readback, exact checkpoint-patch application/tree equality, unchanged real indexes and stable inputs. These are previous-package counts, not projected follow-up counts.

## Complete source and evidence selection

The previous `package-coordinate-deliverables.py` remains unchanged. It already packages every tracked project file, all new LMDB source files, selected task files and the complete initialized external theme. It also builds the full checkpoint patch in a temporary Git index/object store and verifies application in a separate temporary index.

The old evidence selector intentionally rejects `.jfr` and compiled artifacts. The follow-up therefore uses its normal text-evidence path plus a separate self-contained runtime bundle. `prepare-delivery-selection.py` calls only the old packager's read-only evidence-selection function, then creates a closed explicit inventory with `evidence_roots: []`.

`selection-policy.json` permits omission only of the two complete expanded JFR `events.jsonl` files and the historical partial extraction files. The two complete expansions are approximately 343 MB together and are derived from the recordings. Every present omission receives a path, byte count, SHA-256 and reason. The files remain on disk. The failed partial extraction is historical intermediate output, not a regeneration target.

Sample JSONL, root-host audits, external arrival traces, command records, process results, compact analysis, validation logs/XML, corrected source provenance, original metadata history, exact extractor/analyzer source and the recording configuration remain selected. The source package contains the updated core source and tests automatically. The selection also includes the new delivery/reproduction helpers and follow-up reports.

Generate the selection only after its membership is final. If new evidence files appear afterward, generate a new selection filename; do not silently reuse an incomplete closed inventory.

## Self-contained runtime and reproducibility

`bundle-comparison-runtime.py` consumes the frozen comparison protocol and active runtime descriptors. It validates the original ordered 127-entry runtime and each candidate's single replacement at zero-based index 1. The other 126 entries—including the exact original test harness, fixture, diagnostics and final root-audit launcher—must match. Shared artifacts are archived once, resulting in 129 distinct runtime artifacts across the three classpaths.

The exact measured source map is `final-validation/benchmark-source-hashes.json`, whose digest is pinned above. The older `source-hashes.json` is included only where the preserved metadata correction history references it. Active original and candidate descriptors must identify the correct measured map. Candidate 1's corrected metadata is followed through the protocol; its original artifact tree and old metadata remain unchanged. The correction directory, helper history and correction record are included.

Artifact-directory hashing matches the original freezer: relative paths sorted by path components, UTF-8 relative name plus a NUL byte, then the raw SHA-256 digest of each file. The bundle verifies every archive member and reconstructs each of the 129 artifact hashes from archive readback. It includes no store, JDK or Maven cache. Sanitized recordings are selected explicitly; original raw recordings are forbidden archive inputs.

`rebase-materializer-comparison.py` validates the extracted bundle and creates a new reproduction directory. It preserves original inputs and writes separately identified script copies with four exact path substitutions: Java path guard, repository root, original classpath file and original base manifest. It writes three ordered classpaths and a derivative protocol. Result/checksum, ledger, count, balanced-order, artifact and root-host guards remain unchanged. The helper launches no Java.

JFR regeneration also needs one path adaptation: `analyze-profiles.py` compares recorded `FileWrite.path` strings with its current folder. The reproduction plan creates a separate analyzer copy with only that comparison mapped to the original recorded prefix. It writes explicit ordered commands for source-launching the exact `ExtractJfr.java` against the sanitized delivery recordings into new files, then runs the derivative analyzer. Original recordings, analyzer and saved analysis remain untouched. Regeneration is explicitly identified as using sanitized input; original expansion hashes remain provenance and are not assumed to equal regenerated output without a recorded equivalence check.

## Proposed execution order after timing

First finish the final comparison/report work and recording sanitization. Freeze all source and evidence writers. The following commands are a plan, not an execution record. Paths assume the current checkout; every output directory must be absent.

```bash
python3 result-coordinate-evidence/materializer-followup/bundle-comparison-runtime.py \
  --results result-coordinate-evidence/materializer-followup/optimization-comparison \
  --output ../coordinate-materializer-runtime-delivery
```

Extract that archive into a new empty directory and run the packaged relocation helper there, supplying a compatible local Java executable. This performs identity validation and creates a plan without starting Java. Record the helper verification and a Python-only `--validate-only` check of its generated comparison driver before final delivery. Performance experiments need not be repeated as part of packaging.

Copy the small runtime verification receipt into the task evidence and finish report download links. Then freeze the final source/evidence selection, supplying the actual follow-up report path with `--report` if it is not already included by the inherited selection:

```bash
python3 result-coordinate-evidence/materializer-followup/prepare-delivery-selection.py

python3 result-coordinate-evidence/package-coordinate-deliverables.py \
  --manifest result-coordinate-evidence/materializer-followup/delivery-selection.json \
  --output result-coordinate-materializer-deliverables

python3 result-coordinate-evidence/materializer-followup/build-delivery-delta.py \
  --prior-delivery result-coordinate-deliverables \
  --new-delivery result-coordinate-materializer-deliverables \
  --output result-coordinate-materializer-deltas
```

The delta helper requires the previous receipt, patch and tree to equal the pinned identity. It reconstructs the previous and new trees from their complete checkpoint patches in private indexes sharing a private object directory with read-only access to existing Git objects. It compares each reconstructed tree to both the receipt and its source-manifest entries, including the exact unchanged external dependency.

The complete prior-delivery patch is applied and reversed in further private indexes; the forward tree must equal the new full source tree and the reverse tree must equal the previous tree. The code-only patch receives a separate exact-tree and reverse check. Neither production candidate met the prospective adoption rule, so the active-source delta must contain only the retained `NativeResultMaterializerTest.java` characterization. That is the delta helper's sole default permitted source path; any additional code path requires an explicit reviewed argument. Both rejected candidates remain preserved with their complete modified sources and runtime bytes in the comparison bundle. The live index and HEAD are checked before publication. `GIT_NO_LAZY_FETCH=1` prevents implicit network fetches in partial clones.

The delta helper does not extract or rehash the full source archives; the source packager's own receipts establish archive readback. It verifies the exact source trees from the receipt-bound patches and manifests. This division is recorded explicitly in its verification output.

## Verification status of this preparation

The preparation task performed only source reading, helper/document creation and Python AST/JSON parsing. It imported or executed none of the new helpers and performed no archive, large hash pass, Git stage, Java process, build or test. Final artifact counts, archive hashes, readback receipts, relocation verification and patch replay results belong to the later authorized packaging records; this design record does not claim they have passed.

### Static review findings and resolutions

The independent review found no blocking defect in private-index reconstruction, forward/reverse delta verification, archive ordering/readback, exact path relocation or no-overwrite publication. The review led to two delta hardening changes: disable implicit lazy Git fetches and repeat input/index/HEAD checks immediately before publication. It also identified missing original `validationRecords[*].testedSourceFile` references; those exact files and their recorded hashes are now included.

The source provenance review caught an older source-map file that differed from the measured map in one test fixture. Active descriptors now pin the exact measured `benchmark-source-hashes.json` digest, and the preserved correction history is packaged separately. The JFR review established that each original recording contained 105 initial environment-variable events, 16 initial system-property events, 3 system-process events and 45 initial security-property events; that concrete finding led to sanitized-only recording delivery with an explicit verification receipt.

All five prepared Python helpers pass AST parsing, and the selection policy parses as JSON. These are syntax/static checks; archive readback, relocation execution, sanitization verification and Git patch replay are later packaging gates.

### Authorized runtime preflight after timing

The post-timing [runtime preflight](delivery-preflight/runtime-preflight.json) passed: 129 distinct runtime artifacts, three ordered 127-entry classpaths, and 12,866 file identities were checked against the frozen protocol/source metadata. The original and both candidate identities matched, and inputs remained stable. This preflight ran no Java, created no archive, and read no original raw JFR. Sanitization, archive readback, relocation execution and source-patch replay remain separate packaging gates.
