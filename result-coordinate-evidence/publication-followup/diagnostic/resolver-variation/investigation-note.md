# One additional resolver call: established facts and open causes

The fixed publication timing study stopped after its ninth JVM because the shared-tail DENSE-omit sample at iteration `-7` in `comparison-v1/round-01-00-shared-tail-01-dense-omit/samples.jsonl` reported 32,834 lazy-value calls. The same iteration in `round-01-00-shared-tail-02-dense-all/samples.jsonl` reported 32,833. Both contain 1,048,576 complete rows, 6,291,456 binding reads, checksum `8327963502356915200`, compiled native IR, one semantic native compile, and one coordinate owner without initial or growth admission refusal. The stopped summary records no accepted timing comparison. The independent audit found no other hot-counter, result, route/tier, or adjacency mismatch in that pair.

Those raw records do not contain per-target weak-reference or witness counters. They therefore establish the extra resolver call, but do not identify its cause. Earlier 512 MiB per-target diagnostics had no weak/witness misses in their four queries per arm and matched 32,833 calls. Four prior observations do not establish that the count must remain deterministic under normal GC.

## Source evidence

In the selected original `NativeResultMaterializer.java`, `find` first checks source/scope validity. For a matching ordinary-cache ID, it dereferences the existing weak reference. A null referent increments the existing weak-miss counter, clears the ordinary entry, and continues into `findFactor`; a failed semantic witness also clears the entry. An ordinary hit can publish the current coordinate before returning. The experimental publication mask adds an early guard only to `rememberFactor`, leaving these lookup paths intact.

`findFactor` first checks storage, coordinate validity, and owner/activation. After a matching page and cell, a null weak referent clears the cell and returns null; a failed witness also clears the cell and returns null. `NativeProjectedBindingSet.prepare` invokes `lazyValue` after a failed materializer lookup and later remembers the resolved value. This is a designed fallback path. The two `LmdbSailStore.lazyValue` implementations increment the shared lazy-value counter for valid IDs before asking the value store for a lazy value; synthetic-value sources can also increment or delegate that counter. It is not intrinsically a counter only for materializer weak misses.

There is also a separate correctness path in `NativeProjectedBindingSet.prepare`: if `resultValueScope()` changes after provisional hits, it abandons the cached scope, clears those provisional hits, and resolves them again. Scope/source rejection, witness failure, coordinate rejection, page/cell availability, and resolution outside the counted materializer path are therefore alternatives to weak-value collection. The timing record alone cannot rule them out. The new probe's find/hit/remember reconciliation can expose some uncounted or scope-repair activity, but even a failed equation would not itself prove a particular ownership bug.

Source anchors, relative to the repository root:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java`: `begin`, `find`, `remember`, `sameScope`, `matches`, `findFactor`, `rememberFactor`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet.java`: `prepare` and its scope-change repair after provisional hits.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: the two `lazyValue(long,int)` implementations near lines 14,120 and 15,676 in the selected source.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbSyntheticValueSource.java`: `lazyValue(long,int)`.
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializerTest.java`: `clearedWeakReferentFallsBackAndIncrementsTheWeakMissCount` describes the existing ordinary weak-cache contract. This note does not claim to have rerun that test.
- `result-coordinate-evidence/publication-followup/diagnostic/build_overlay_source.py` and its generated masked materializer: the counter insertion sites preserve existing dereference operations and distinguish ordinary/factor weak and witness branches.

## Interpretation before the new probe

A normal weak-cache fallback is a plausible explanation for one additional resolver with unchanged results. It is an inference from the source contract, not a diagnosis of the original event. The publication mask does not change strong Value ownership in the source. Nevertheless, different allocation and collection schedules can alter weak-entry availability, and source-level plausibility alone neither proves the mechanism nor excludes ownership defects elsewhere.

The smallest useful next step reuses the already compiled diagnostic runtime, changes no code or retention, and observes the longer 8+5 query sequence in four fresh JVMs with normal GC/reference logging. Its predeclared all/omit/omit/all schedule and all 52 records distinguish observed branch behavior from assumptions without accepting a new timing comparison. Direct weak counters can demonstrate a weak-entry fallback in a new instrumented query. GC logs alone cannot identify a referent, and aggregate ordinary/factor weak counters can overlap. Counter and JSON overhead can change GC pressure, especially because the all arm closes twice as many factor pages. Neither observed misses nor their absence estimate production miss rates or prove what happened in the original timing query.
