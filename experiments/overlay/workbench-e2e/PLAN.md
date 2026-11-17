# Actual Workbench transport comparison

Routine C: investigation, no production changes. Publication paused.

- [done] Pin PR and inspect transport route
- [done] Build isolated transport correctness harness
- [done] Verify fixture and compiled execution witnesses
- [in_progress] Measure matched writer and query combinations
- [todo] Report timings and explicit scope limits

PR #6440 pinned head: fca5653c2da8df975fac5760b457c436f3e12dc6.
Base: be0c4c724f582e22d1a2936b87bb24969cc9154a.
Read production writer from authenticated GitHub connector, untouched source saved in pr-src.
Shared machine: no heavy operations until lifetime worker releases native-benchmark.lock.

Pipeline: native LMDB → actual server controller/BinaryQueryResultWriter → HTTP loopback → actual Workbench Java binary reader → page batch NDJSON → streaming JS client. This is NOT browser rendering/IndexedDB timing. First visible row, first requested batch and full-query count/completion are distinct. Unknown-total request consumes all query rows even when the page window is smaller. Complete 8M-row browser retrieval requires repeated queries/continuations and is a separate workload.

Hold native-benchmark.lock throughout server lifecycle and client measurements. Copy fixture, never open shared query fixture. Freeze runtime jar dependencies and override each relevant module with a complete frozen class directory (not individual classes). Record code-source and SHA256 manifests. Keep both query and binary writer variants explicit; do not silently compare current main with old main or mix whole-package classes from incompatible snapshots. Baseline correctness precedes adding memo-phase commit b8d2015.
