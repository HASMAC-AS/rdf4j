# Broader regression checkpoint

Combined memo/partial-ready/wide flags, single3GiB fork.421 top-level evaluation test classes selected in40-class durable shards by run-broad-regression.sh. This is not the entire LMDB module.

Completed first four shards:467,509,355 and831 tests. First three green; fourth has two failures: HotLoopCounterTest assumes5-column DISTINCT uses object path (now primitive under flag); IdAuthorityTest expects64 source lookups across aggregate cache growth but memo bypass for its unsupported source makes128. Both under investigation; not suppressed. Remaining shards running.

First three shards predate replay-context fix class rebuild at2026-11-17T07:58:12Z; subsequent shards use it. Run logs and exact selections retained locally. Copied per-shard report folders may contain stale unrelated reports; use the selection and log summary as authoritative.

Strict real map-growth test: red1/1/0, then green1/0/0 with two memo instances and two compiled irKernel attempts, exact200 output rows, and caller context identity restored. Broader lifecycle/exceptional-reopen validation pending at this checkpoint.

All branches here are WIP. No full-regression pass claim.
