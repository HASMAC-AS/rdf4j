# Fixed-width spill I/O experiment

## Baseline profile

The frozen budgeted-wide implementation ran the 8-million-row, 30-column DISTINCT fixture with the default 640 MiB query cap. JFR recorded Java samples every 4 ms and native-method samples every 20 ms. The profiled query took 209.071 seconds, versus 175.682 seconds in the earlier unprofiled run. Cardinality and checksum matched; all 20 spill files were deleted and ledger usage returned to zero.

Of 4,247 main-thread native-method samples, 1,941 were `RandomAccessFile.writeBytes0`, 1,727 were `readBytes0`, 287 were `seek0`, 109 were `getFilePointer`, and 63 were `length0`. These are sample counts, not percentages of query wall time. Sampling periods differ, and many native stacks omit application callers. Captured seek/length stacks identify `appendDisk`, `findDisk`, `freeSlot`, and `growDisk`. Installed JDK 25 bytecode confirms each `writeLong` performs an eight-byte write. A 30-column record previously required 32 such writes.

## Isolated change

The default-off flag is `rdf4j.lmdb.expressionDomain.bulkIdRecordIo`.

For `Kind.ID`, a reusable big-endian buffer holds the hash, tuple and outcome. A whole record is written in one call. An exact hash match reads the hash and remaining record in two calls while still comparing every tuple component. There is no append-offset cache, index pre-sizing, index-layout change or probe reuse in this stage. The on-disk format and `Kind.VALUE` codec are unchanged.

Memory ownership decreases: ID domains previously allocated a value codec with an unused 1,024-byte buffer. The replacement buffer is at most 528 bytes for 64 keys plus hash and outcome, and no value-codec object is allocated for this kind. The existing conservative 4,096-byte workspace reservation is retained. There are no per-row arrays. Close releases the reservation and clears the buffer.

## Validation

The initial 28-test selection passed before and after the algorithm change. The first test fixture was then replaced: it now uses a typed package-private spill-file factory, rather than reflection or private-state manipulation. Its real 4,096-byte ledger leaves no room for an arena, so the first ordinary `Membership.add` naturally triggers spill. The default factory still opens normal `RandomAccessFile` instances. This seam changes file creation only, not hot-loop I/O.

The corrected fixture and existing tests pass with the flag both off and on: **28 tests, zero failures/errors** in each run (`spill-bulk-typed-off.log`, `spill-bulk-typed-on.log`). Tests cover widths 0, 1, 30 and 64; exact bytes and outcomes; full-key comparison; unchanged workspace; cancellation; truncated records; partial writes; and cleanup. The 30-column witness measures 32 payload writes becoming one and 32 exact-match reads becoming two. Existing expression-domain tests exercise the unchanged value codec and its failures.

## Actual-query measurements

Both runs use the same frozen 2,136,000-triple fixture, 5 GiB heap, default 640 MiB query cap, zero query warmups, compiled IR, memo and partial-ready optimization. They are single cold runs, not ABBA measurements.

| Layout | Previous spill I/O | Bulk record I/O | Difference |
| --- | ---: | ---: | ---: |
| Flat tuple storage | 175.682 s | 126.486 s | About 28% faster |
| Segmented tuple storage | 179.817 s | 118.630 s | About 34% faster |

Every run returned exactly 8,000,000 rows with checksum `-3441391656945048864`. The benchmark wrapper asserts exactly 8,000,000 successful bulk record writes, counted locally and aggregated once per domain close, not atomically per row. All 20 files were deleted and ledger usage returned to zero. Peak ledger usage stayed unchanged at 393,746,272 bytes for flat storage and 671,067,808 bytes for segmented storage, below the 671,088,640-byte cap.

The timing snapshots remain frozen. `bulk-flat-classes` is the original budgeted snapshot plus only the bulk domain classes; `bulk-segmented-classes` is the segmented baseline plus those same domain classes. They predate the typed test seam, which does not alter the measured algorithm. Raw JFR and JSON artifacts remain private and must not be published.

## Next isolated experiment

After this checkpoint, reuse the empty index slot discovered by a synchronized membership miss for immediate insertion, but only if the disk capacity has not changed. Generic expression callbacks must not receive this optimization. Test empty probes, collisions, growth invalidation, exact duplicates and cleanup through legitimate interfaces. Keep append-offset caching and index pre-sizing separate.
