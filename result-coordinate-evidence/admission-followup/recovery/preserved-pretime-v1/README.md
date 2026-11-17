# Preserved pre-timing evidence

This directory preserves 43 large raw evidence files from the admission follow-up, including the frozen comparison protocol. The originals remain in place and are excluded from ordinary Git staging by 43 exact paths. Code, smaller reports, runtime compile manifests and the closed natural audit remain ordinary repository files.

The archive contains **49,450,194 raw bytes** in **43 gzip objects totaling 13,822,550 bytes**. The largest object is **418,794 bytes**, below the 512 KiB per-object bound. Each file, compressed object and decoded part has its size and SHA-256 recorded in `manifest.json`. `MANIFEST.sha256` seals that manifest.

From the repository root, verify existing raw evidence:

```bash
python3 result-coordinate-evidence/admission-followup/recovery/preserve_evidence.py verify --archive result-coordinate-evidence/admission-followup/recovery/preserved-pretime-v1
```

After a fresh checkout or workspace recovery, recreate missing originals:

```bash
python3 result-coordinate-evidence/admission-followup/recovery/preserve_evidence.py restore --archive result-coordinate-evidence/admission-followup/recovery/preserved-pretime-v1
```

Restoration refuses different existing bytes and never replaces an existing file. An optional `--destination` selects a separate directory for an independent readback. Restore the raw protocol before invoking the comparison runner or analyzer.

`verification.json` records full reconstruction against all originals and the append-only ignore-path change. `roundtrip-verification.json` records a fresh 43-file restore, an idempotent second restore, rejection of a deliberately different existing scratch copy, and renewed verification that every original is unchanged.

The frozen protocol retains SHA-256 `8a2c2fd92799ae667965cf3bb1af86b12d47d4e6af6ce83f3ad13134318b6949`. No primary JVM had run when this preservation was created. The natural audit directory and publication transfers were outside the selection. The natural audit subsequently closed and remains directly readable as an ordinary repository file.

