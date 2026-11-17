# Actual Server → Workbench transport harness

This is a Routine C investigation: no production code changed. It is **not** an in-process binary writer sink, and **not** a browser rendering benchmark. `WorkbenchBenchmarkServer` starts the actual Spring Boot server/Workbench, plus a loopback-only readiness/diagnostic filter. Its POST runs the actual QueryServlet, remote HTTP repository client, RDF4J server tuple controller, binary writer/parser and Workbench v2 NDJSON page writer. A Node client streams, JSON-decodes and counts rows.

The 2.136M-triple synthetic LMDB fixture produces 8M distinct 30-column rows; it is not a private user query. `prepare.sh` copies that fixture to a separate benchmark repository. Server/client JVM processes must not open the original fixture.

## Isolation and reproducibility

- Pin PR #6440 production writer to `fca5653c2da8df975fac5760b457c436f3e12dc6` (GitHub metadata verified November 17, 2026).
- `prepare.sh` extracts the already built Boot runtime; the artifact SHA is retained.
- Query variants use complete frozen API/LMDB class directories from the lifetime experiment; Boot's copies of those modules are removed from the runtime classpath.
- Binary variants use separate complete copies of the binary module. PR production source is compiled into the `writer-on` copy. No source checkout/cherry-pick or shared Maven artifact replacement is needed.
- The run manifest records the actual command, flags, selected directories and variant. A metrics endpoint records loaded class origins. Timing only starts at the query POST, after repository opening and overlay/adjacency readiness, not Boot startup.
- Both scripts take `experiments/overlay/native-benchmark.lock`. Coordinate the whole experiment window with other workers too, since the per-run lock is released between forks.
- Publication remains paused. Never push these files while that remains unresolved.

## Commands

From repository root, **after the lifetime worker releases its measurement window**:

```
cp tools/server-boot/target/rdf4j-server-boot-6.2.0-SNAPSHOT.jar experiments/overlay/workbench-e2e/boot-frozen.jar
experiments/overlay/workbench-e2e/prepare.sh
python3 experiments/overlay/workbench-e2e/run.py original off smoke-off --size 100 --validate
python3 experiments/overlay/workbench-e2e/run.py original on smoke-on --size 100 --validate
```

Then matched fresh-process trials with `--size 1000000`. Query choices are `original` (prior best), `lifetime` (two safe lifetime tweaks), `compact` (compact snapshot only), and `combined` (both). `on`/`off` control the pinned writer. Do not refer to `original` as upstream/main.

## What is measured

- Headers received, first NDJSON rows record, requested window received, terminal query record, response EOF.
- Full server query completion/count, even when only a smaller window is emitted. No known-total shortcut is sent.
- Server/client sampled RSS separately. It includes initialization retained memory; no allocation/retained-heap equivalence is claimed.
- Code-source witnesses; compiled vs interpreted binds; memo hits; grouping admission; spill-file counters; actual server response MIME type.
- Separate `--validate` runs assert every emitted term (language, Unicode, null suffix, lexical/domain values), uniqueness, output width and terminal total. Their timing is not benchmark evidence.
- `--size 1000000` matches the default maximum browser fetch window. It does **not** claim all 8M rows reached the browser. Full browser result retrieval repeats continuation queries and is a separate scenario.

The streaming client does not run DOM layout, React/Lit components, the row worker, IndexedDB, or browser GC. It should be called **Workbench HTTP/NDJSON end-to-end**, not whole-browser end-to-end.

## Status

Setup and HTTP correctness smoke passed (`smoke-off-3.json`): real binary MIME, exactly 8M consumed/count, 100 emitted tuples validated, compiled binds=1, interpreted=0, grouping trackers=1, memo hits>0, spill files=0, budget returned to 0. No matched performance comparison yet. Two earlier scratch-client setup failures (incorrect route, missing `ref=text`) are retained in logs and corrected. Actual browser script uses the project’s existing Playwright dependency and system Chromium; it is prepared but not yet live-tested. The broad copyright scan reports existing unrelated header issues; no production files were changed.

### Browser limitation

Two browser smoke launches failed before page navigation: system Chromium aborts at `process_singleton_posix.cc` creating a Unix socket (`Operation not permitted`). A supported escalated retry did not change that executor restriction. No further bypass is attempted. `browser-smoke-off-client.log` and `browser-smoke-off-2-client.log` retain the evidence. All reported performance therefore remains **actual HTTP/Workbench pipeline to received NDJSON**, not rendered paint or IndexedDB work.

### Pinned query variants

- `original`: recovered prior best source `4724600b10af6b044cc28154309352416d0660cf`, same production as earlier b8104ecf51. Frozen original API + lifetime/baseline-classes.
- `lifetime`: the two validated refinements at `7d8edb55b58080173667309b7f587edff33d218b`. Frozen original API + lifetime/combined-classes.
- `combined_hash`: lifetime LMDB above + improved compact snapshot hash at `3397b8c6b3d080a62946f432a3050fc10b701c4e`, using compact-snapshots/hash-api-classes. Explicit flag true. Not the earlier slower compact prototype.
- `compact` and `combined` retain the earlier prototype only for provenance; they are not selected as a presumed winner.

`combination-matrix.sh` queues exact PR-on verification before the mirrored lifetime/writer and improved-hash combination trials. Do not run it concurrently with other performance workers.
