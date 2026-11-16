# Lossless preservation of comparison JSON

The three gzip files preserve the complete independent analysis, assembled run records, and supervision history byte for byte. Their raw originals remain unchanged at the paths recorded in `manifest.json`; only those exact raw paths are ignored by Git. Individual JVM evidence and readable reports remain ordinary files.

From the repository root, verify compressed hashes, decoded hashes, lengths, and any existing originals:

```bash
python3 -B result-coordinate-evidence/direct-memo-followup/recovery/preserved-comparison-v2-json/restore.py
```

After cloning, restore missing files at their exact original audit-input paths:

```bash
python3 -B result-coordinate-evidence/direct-memo-followup/recovery/preserved-comparison-v2-json/restore.py --restore
```

The helper validates both compressed and raw SHA-256 identities. Existing equal files are verified and left unchanged; differing files cause an error and are never overwritten. Missing parent directories are created only when restoring. Paths outside the repository are rejected. No Java or network access is required.

Compression uses an empty gzip filename, timestamp zero, and level nine. Creation verified repeat compression determinism and full byte-for-byte round trips. The manifest records every raw/compressed size and hash. Restored files are exact audit inputs, including their original absolute-path strings; the separate experiment tools retain their existing workspace-path requirements.
