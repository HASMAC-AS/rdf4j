# LMDB Compaction Architecture

LMDB environments accumulate free pages and fragmentation over time. RDF4J now ships with an
on-demand compaction workflow that rewrites the store into a fresh directory, verifies the copy,
and atomically swaps it into place. The process is designed to be offline and safe: the original
data directory is preserved as a backup until the swap completes successfully.

## High-level dataflow

```mermaid
flowchart TD
    A[Maintenance trigger] --> B{Collect fragmentation metrics}
    B -->|below threshold| C[Exit]
    B -->|above threshold| D[Stage destination directory]
    D --> E[Copy LMDB environments
    (MDB_CP_COMPACT)]
    E --> F[Optional verification
    via LmdbStore.init()]
    F --> G[Atomic swap & backup]
    G --> H[Emit metrics/report]
```

## Workflow summary

1. **Validation** – Ensure the store is shut down, compute baseline metrics, and prepare staging and
   temporary directories.
2. **Copy** – Each LMDB environment (`values`, `triples`, persistent set caches, …) is copied with
   `mdb_env_copy2(..., MDB_CP_COMPACT)`. If the host does not support the compact flag the
   destination map size is shrunk to the minimum required number of pages.
3. **Verification (optional)** – A temporary `LmdbStore` instance opens the staged directory; if the
   initialization succeeds the snapshot is considered valid.
4. **Swap** – The live data directory is renamed into a timestamped backup and the staged copy is
   atomically moved into place.
5. **Reporting** – A `LmdbCompactionReport` captures timings, file sizes, free-page ratios, and backup
   details. Callers may subscribe to progress events and metric callbacks.

## Configuration properties

Add the following entries to an LMDB store configuration to enable automatic maintenance:

| Key | Default | Description |
| --- | --- | --- |
| `lmdb:compactAutoEnabled` | `false` | Enables the automatic compaction check executed by maintenance tools. |
| `lmdb:compactThreshold` | `0.35` | Fragmentation ratio (free pages / total pages) that triggers compaction. |
| `lmdb:compactMinInterval` | `7 days` | Minimum interval between automatic compaction runs (informational hint for schedulers). |

See [`LmdbStoreConfig`](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java)
for programmatic access to these properties.

## Programmatic API

```java
LmdbStore store = new LmdbStore(dataDir, config);
LmdbCompactionOptions options = LmdbCompactionOptions.builder()
        .destinationDirectory(staging)
        .temporaryDirectory(temporary)
        .verifyAfterCopy(true)
        .progressListener(progress -> LOG.info("{}", progress.getMessage().orElse("")))
        .metricsConsumer(metrics -> LOG.info("Shrank to {} bytes", metrics.getFileSizeAfterBytes()))
        .build();

// run always
LmdbCompactionReport report = store.compact(options);

// or only when fragmentation is high enough
store.compactIfNeeded(options, config.getCompactionFragmentationThreshold());
```

The `LmdbCompactionMetrics` object carried by the report exposes file-size deltas, copy duration,
per-environment statistics, and free-page ratios before/after the copy.

## Maintenance helper

`LmdbMaintenance` provides a small façade around `LmdbStore.compactIfNeeded`, respecting the
configuration flags for automatic maintenance. It is suitable for background schedulers that already
operate on RDF4J configuration objects.

## CLI usage

A helper script is available under `bin/rdf4j-lmdb-maintenance.sh`. Build the LMDB module first:

```bash
mvn -pl core/sail/lmdb -DskipTests package dependency:copy-dependencies
```

Then invoke the script:

```bash
bin/rdf4j-lmdb-maintenance.sh \
  --compact \
  --dataDir /var/lib/rdf4j/repositories/foo/data \
  --target /var/lib/rdf4j/repositories/foo/compacted \
  --threshold 0.30 \
  --verify
```

The command prints the current fragmentation ratio, performs compaction only when the supplied
threshold is met, and outputs a summary of the resulting metrics. Use `--no-backup` to discard the
backup directory after a successful swap.

## Metrics

Compaction records the following metrics, surfaced via `LmdbCompactionMetrics` and the CLI output:

- Size before/after (bytes)
- Duration of the copy stage
- Average free page ratio before/after
- Per-environment page statistics
- Backup location (if retained)

These metrics can be persisted or reported to external monitoring systems as needed.
