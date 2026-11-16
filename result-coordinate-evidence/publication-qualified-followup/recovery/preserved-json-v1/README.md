# Lossless recovery JSON archive

These three gzip files preserve the complete bytes of the large recovery manifests and the first GitHub transfer payload. The original JSON files remain unchanged in the workspace. Their exact paths are excluded from Git so the repository records the compressed copies and this manifest without another 31.31 MB of redundant text.

`manifest.json` records each original path, byte count and SHA-256, its compressed path, byte count and SHA-256, and the verified round trip. Compression uses gzip level 9 with modification time zero and no original filename. The combined compressed payload is 5,345,097 bytes; its original JSON payload is 31,314,822 bytes. All source, protocols, reports, and smaller receipts remain ordinary files.

Verify compressed content and any originals already present:

```bash
python3 result-coordinate-evidence/publication-qualified-followup/recovery/preserved-json-v1/restore_preserved_json.py
```

Restore missing original JSON files after a fresh checkout:

```bash
python3 result-coordinate-evidence/publication-qualified-followup/recovery/preserved-json-v1/restore_preserved_json.py --restore
```

The restore command never overwrites an existing file. An existing mismatch stops the operation and preserves that file for inspection. The verification and restoration helper does not use Java, Git writes, or a network connection.
