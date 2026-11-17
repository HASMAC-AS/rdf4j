# Compact snapshots: first query comparison

Local source candidate `22b9bbd0c444a95d0398140c932add78695710d2`, lifetime changes `7d8edb55b58080173667309b7f587edff33d218b`, prior best recovered from `4724600b10af6b044cc28154309352416d0660cf`. Default-off prototype, not a recommendation.

## Primary outcome

The first compact snapshot implementation **does not improve throughput or latency**. Lower allocation is useful diagnostic evidence, not success. Two fresh-JVM forks per variant, balanced A D C B B C D A ordering:

| Variant | Mean query seconds | Mean thread allocation GB |
| --- | ---: | ---: |
| A prior best, no new lifetime changes | 39.719 | 32.279 |
| B lifetime changes only | 39.038 | 31.404 |
| C compact snapshots only | 44.900 | 25.748 |
| D both | 45.305 | 24.713 |

Combined vs lifetime-only is 16.05% higher latency (13.83% lower throughput at equal rows), despite 21.30% less allocation. Combined vs prior best is 14.06% higher latency, with 23.44% less allocation. These are small-sample exploratory measurements, not statistical significance claims. Lifetime-only small gain remains inconclusive.

## Workload and controls

Eight million rows, 30 projected slots, existing 417 MB fixture; 640 MiB query budget, 2.5 GiB global ledger, 2 GiB initial/5 GiB maximum JVM heap. All earlier best compiled-IR/memo/grouping improvements enabled. Query warmup zero, one query per fresh JVM; query measurement excludes startup and fixture opening. Serial machine lock, immutable class-directory snapshots for each API and LMDB variant. Every timed run returned 8,000,000 rows, checksum -3441391656945048864, compiled irKernel route, grouping and memo witnesses; zero spill and zero retained ledger reservation. Candidate runtime class witnessed explicitly, avoiding accidentally measuring fallback. This is direct repository consumption by name/getValue, not HTTP, serialization, Workbench, or browser rendering. Consumer/first-result timings were not separately captured in this pass.

Additional untimed-for-comparison exact-domain/uniqueness checks passed for B and D across all eight million rows. Focused integration: 183 tests passed, plus three explicitly enabled compiled replay/map-growth/context cases. Candidate API worker separately documented 137 module tests with flag on and off.

## Profile and next experiment

Separate diagnostic JFR runs: B 38.949 s /31.473 GB, D 43.451 s /24.842 GB. Candidate CompactBindingSetSnapshot.slot is the largest CPU leaf: 833/4059 main-thread CPU samples (20.52%). Sample share is not wall-time share or an independently removable cost. The current xor-spread + linear-probing table clusters the actual fixture's 30 names: mean 5.03 probes, worst 16, with 64 slots. Test a better integer hash spread before deciding whether compact representation can improve latency. Preserve this version and comparison independently.

Cold Binding iteration/name-view access deliberately materializes a conventional MapBindingSet delegate; that consumer mode can pay compact construction plus map conversion, and is not covered by the getValue workload. No general result-consumer speedup is claimed.

Raw JFR and frozen class directories remain local. Checked-in summary text normalizes generated lambda addresses. `run.sh` reproduces variant selection; dependency classpath, fixture and compiler setup are described by the sibling lifetime experiment. Use SNAPSHOT_FLAG=rdf4j.sail.compactResultSnapshots, including for original API variants (ignored there).
