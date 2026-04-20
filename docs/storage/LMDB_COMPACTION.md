# LMDB Compaction Overview

## Purpose

The LMDB compaction routine rewrites the `values` and `triples` environments into
new LMDB databases, collapses fragmented pages, and atomically swaps the
compacted copy into place. This reduces file sizes and improves cursor
locality after sustained churn.

## Dataflow

```
┌───────────────┐    copy+metrics     ┌────────────────┐
│ live data dir │ ─────────────────▶ │ staging/env(*) │
└───────────────┘                     └────────────────┘
        │                                    │
        │ swap                               │
        ▼                                    ▼
┌────────────────┐     optional backup     ┌───────────────┐
│ compacted dir  │ ◀────────────────────── │ old dir .bak  │
└────────────────┘                         └───────────────┘
```

1. The compactor creates an empty staging directory outside the active data
   directory (either user supplied or a timestamped sibling directory).
2. Non-LMDB artifacts (namespaces, configuration files) are copied as-is.
3. Each LMDB environment (`values`, `triples`) is reopened read-only, copied via
   `mdb_env_copy2(..., MDB_CP_COMPACT)`, and verified.
4. Metrics (per-environment entry counts, page totals, copy duration) are
   captured and fed to any configured consumer.
5. Once both environments are copied successfully the compactor atomically moves
   the original data directory aside (keeping or deleting the backup based on
   options) and moves the staging directory into place.

## API Surface

* `LmdbCompactionOptions` – builder-style options for destination/temporary
  directories, verification, backup retention, progress and metrics consumers.
* `LmdbStore.compact(options)` – entry point invoked while the store is shut
  down; throws if the store is still initialized or the data directory is
  missing.
* `LmdbCompactionResult` – exposes byte counts, backup location, and collected
  metrics.

## Operational Notes

* Compaction must be run with the repository offline. `LmdbStore.compact()`
  enforces this by rejecting calls when the store is still initialized.
* A `.bak` directory is kept by default so operators can roll back manually.
  Set `keepBackup(false)` in the options to automatically delete the old copy.
* Verification compares per-database entry counts before and after the copy. If
  any mismatch is detected the compactor aborts without swapping directories.
