# Broader regression checkpoint

Combined memo/partial-ready/wide flags, single3GiB fork.421 top-level evaluation test classes selected in40-class durable shards by run-broad-regression.sh. This is not the entire LMDB module.

Completed first four shards:467,509,355 and831 tests. First three green; fourth has two failures: HotLoopCounterTest assumes5-column DISTINCT uses object path (now primitive under flag); IdAuthorityTest expects64 source lookups across aggregate cache growth but memo bypass for its unsupported source makes128. Both under investigation; not suppressed. Remaining shards running.

First three shards predate replay-context fix class rebuild at2026-11-17T07:58:12Z; subsequent shards use it. Run logs and exact selections retained locally. Copied per-shard report folders may contain stale unrelated reports; use the selection and log summary as authoritative.

Strict real map-growth test: red1/1/0, then green1/0/0 with two memo instances and two compiled irKernel attempts, exact200 output rows, and caller context identity restored. Broader lifecycle/exceptional-reopen validation pending at this checkpoint.

All branches here are WIP. No full-regression pass claim.

## Completed evaluation sweep and isolated rerun
All421 selected classes completed:5018tests,initial2failures,0errors,5skips. The memo source-eligibility regression is fixed (36focused tests green). The counter fixture now explicitly injects keyArrayLimit0 to exercise object counters rather than assuming width5 always chooses objects; its exact once-publication assertions remain unchanged. The entire failing40-class shard03 then passed831/0/0 (broad-shard03-fixed.log). No unexplained evaluation failures remain. This is not a single final-tree rerun of all5018tests: fixes landed during the sweep; budgetdelegate classes were present but production callsites still legacy during shard03rerun.

Broader store/boundary/replay/value selection96top-level classes is underway via run-store-regression.sh, first759tests green with2diagnostic skips. Its results and new bounded-wide implementation must be reported separately.

Store sweep completed1771tests across96classes,0failures,2diagnostic skips. Source changes occurred between shards: earlyshards used pre-budget callers; lastshards had budget wiring. A new423-class evaluation sweep on bounded-wide integration is underway separately; final completion not yet claimed.

Bounded-flat integration evaluation sweep completed5030tests,0failures,0errors,5skips across423classes. The two earliest shards predated the finalscope/close compile; both were rerun afterward:467/0/0 and509/0/0, completing finalbounded-flat coverage for this selection. Logs budget-final-shard-00/01. This remains scoped evaluation coverage, not fullmodule verification. Default-off focused run:26tests,0failures/errors,11expected feature-gatedskips (15executed). Subsequent segmented/spill experiments require their own verification.
