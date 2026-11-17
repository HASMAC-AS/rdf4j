# Restore the complete source archives

This directory preserves both complete source archives byte for byte in seven binary parts, each at most 524,288 bytes. The original archives remain available in the working directory; only their exact paths are ignored by Git. Their source manifests, patches, README files and original delivery-verification receipts remain ordinary checkpoint files.

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| `artifacts/complete-source-v1.tar.gz` | 1,218,864 | `069d38c235bcf2a044383b823bf1ad92e15644aae1cbac07b7bbe8b7f43cfb4b` |
| `artifacts/complete-source-v2.tar.gz` | 1,691,800 | `831feacaf27d260da3c8b0c53bd2503b1a4d9000b7f5662490a940d024d70bb0` |

From the repository root, restore only missing originals:

```bash
python3 result-coordinate-evidence/admission-followup/recovery/preserve_binary_artifacts.py restore --archive result-coordinate-evidence/admission-followup/recovery/preserved-source-archives-v1
```

Add `--destination /absolute/path/to/new-study-directory` to reconstruct both archive paths elsewhere. The helper validates every part, contiguous offsets, the complete-file hashes, and any existing destination before writing. Matching originals are left untouched. Different existing bytes cause refusal, and an atomic hardlink prevents concurrent overwrites.

`MANIFEST.sha256` seals the immutable part manifest. `verification.json` records original-file readback and the two exact ignore additions. `roundtrip-verification.json` records a fresh two-file restoration, a zero-write repeated restoration, agreement with both original source-packager hashes, and refusal to replace deliberately changed bytes in a separate scratch copy. No original archive, source, frozen protocol or runtime was modified.

The companion `preserve_evidence.py` supplies the shared path-validation and hashing helpers and must remain beside `preserve_binary_artifacts.py`. Both helper identities are recorded in the manifest.
