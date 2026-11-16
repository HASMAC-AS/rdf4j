# Cache publication needs its own cost model

Consulted on 2026-11-16. This note uses primary project sources; no external implementation or dependency was copied.

PostgreSQL's `cost_memoize_rescan` estimates distinct parameter sets, cache capacity, hit probability and eviction overhead. It also charges for creating cache entries and storing tuples. Thus saved underlying work does not by itself make caching favorable. When distinct-count estimation falls back to a default, the planner takes a conservative assumption that calls have unique parameters. Source: [PostgreSQL optimizer source, costsize.c](https://doxygen.postgresql.org/costsize_8c_source.html), function `cost_memoize_rescan`.

PostgreSQL's executor separates cache availability from query semantics. If entry creation or tuple storage cannot succeed, its Memoize node can continue the current scan without caching and reconsider on a later rescan. This is a useful comparison for an optional cache, although its hash table, LRU eviction and whole-tuple entries differ substantially from the LMDB weak-reference factor pages. Source: [PostgreSQL executor source, nodeMemoize.c](https://doxygen.postgresql.org/nodeMemoize_8c_source.html), state `MEMO_CACHE_BYPASS_MODE`.

The inference for this experiment is narrower than adopting either implementation: measure publication work separately from avoided resolution and from memory-admission changes. The original shared-tail query hit its 256 MiB ledger limit, so a target omission could improve useful admissions even if the publication CPU cost were small. The matched 512 MiB comparison plus separate 256 MiB diagnostic distinguishes those explanations. It does not establish a general target-classification algorithm.

The relevant local counterexample is `NativeResultMaterializerTest.acceptedReplayHitAdmitsItsOriginalFactorCoordinate`: an ordinary memo hit can occur at a new accepted coordinate that later supplies useful nonconsecutive reuse. Therefore omitting every ordinary-hit publication is a different experiment and cannot be justified by the present target-specific evidence.
