# Factor-page occupancy diagnostic

This is an additive investigation using the original frozen production runtime, the original shared-selection fixture and its exact bag oracle, and a copy of the original `NativeResultMaterializer` with diagnostic observations. It is not a proposed production optimization. It does not repeat or replace the preserved OFF/DENSE/ROWS pilot. No result from the instrumented JVM will support a latency, allocation, or GC comparison.

## Evidence and source-based hypothesis

The preceding pilot returned the exact 1,048,576-row bag and checksum in all twelve queries. Its measured executions used the normal AUTO compiled IR row route, with a native outer root. The ROWS arm encoded every output row but recorded 16,384 groups and 16,384 columns across 4,096 windows: no shared selection group was observed. The DENSE and ROWS process-ledger peaks were 268,426,276 and 268,424,234 bytes, close to the 268,435,456-byte per-query cap. All final ledger values were zero. These are observations from `../pilot/`.

`LmdbNativeKernelExecution.resultCoordinateColumns` retains demanded registers that come from a replay-eligible source after an earlier expansion. The initial outer expansion is not a replay source in this query; the four later tail bindings are. `LmdbNativeKernelEmitter.emitRunCoordinates` publishes an adjacency group, stable source-run token, offset, tuple column, and namespace. Two chained RDF patterns use different physical producers, so their logical pairing does not make them one source tuple. `KernelResultCoordinates.RowSelectionWindow.sameRow` requires the same group, parent, position, and namespace. This source behavior explains the pilot's four tracked columns and four groups per window; a lowered plan dump was not captured, so the exact producer layout remains an inference from code and counters.

The factor materializer has 64 cells per page. Each page reserves 16,384 bytes and is keyed by target, namespace, group, parent/run, tuple column, and `position >>> 6`. The fixture has 64 roots, 128 left members, 128 right members, and a second one-member predicate from each member to its leaf. If all four tail bindings use their direct-adjacency run coordinates, the uncapped geometry is:

| Target | Distinct parent runs | Pages per run | Pages | Cells per page |
|---|---:|---:|---:|---:|
| `l0` | 64 | 2 | 128 | 64 |
| `l1` | 8,192 | 1 | 8,192 | 1 |
| `r0` | 64 | 2 | 128 | 64 |
| `r1` | 8,192 | 1 | 8,192 | 1 |

That predicts 16,640 pages and 272,629,760 charged page bytes before the directory, ordinary memo, coordinate memo, and transport buffers. The one-member leaf runs would account for 256 MiB of page charges by themselves. This is a prospective hypothesis, not measured occupancy. The diagnostic must establish which parent keys are admitted, how full those pages actually are, and which admission fails.

## Instrumentation

The exact original source preimage is preserved in `preimage/`. Its hash must match the measured source map. `build_overlay_source.py` produces the complete copied source and `diagnostic-overlay.patch`. The new `FactorPageDiagnostics` is a separate package-private helper. The plan class and both factor-page/memo class bodies remain exactly unchanged.

During evaluation, added operations are primitive counter increments and reads of existing fields. The counters cover page admission attempts, successful pages, granted payload reservations, refusals, blocked missing-page publications, ordinary and factor hits, page removals, and directory and memo claims. Directory accounting includes the overlap between each new claim and the old claim before its original release. The diagnostic makes no reservations, calls no additional producers, changes no keys or cache eligibility, mutates no Values, and adds no per-row allocations.

Immediately before a page's existing removal, the diagnostic scans its 64 reference slots into fixed 65-bin histograms. A nonnull `WeakReference` slot is stored occupancy, even when its referent has been collected. A nonnull `WeakReference.get()` result is a transient live-referent observation. It can briefly strengthen that referent and must not be treated as an unperturbed GC measurement. Neither count means an ever-touched cell count or a count of semantically valid current Values.

Before final materializer cleanup, a primitive table captures each remaining page's exact key, current reservation bytes, stored slot count, and live referent count. The original cleanup is protected by `finally`. JSON output is written only after cleanup. An output failure fails the diagnostic after resources have been released. Earlier namespace retirements and bulk clears have their own counts and occupancy histograms. Their keys are not in the final table. If any such removals occurred, all multiplicity statements are explicitly restricted to the final retained pages.

`Reservation.bytes()` reports the remaining charge when observed. A prior `QueryLedger.close()` can release a claim before explicit materializer cleanup. The report therefore separates granted bytes, bytes observed immediately before explicit release, declared structural page charges, page-close counts, and the final process ledger. A grant-minus-observed-release difference alone is not classified as a leak.

## Runtime and execution

The runner preserves raw `javac` output in `compiled-output/` but exposes only the changed `NativeResultMaterializer.class` and the new helper in `classes/`. The unchanged plan and nested page/memo classes resolve from the original frozen runtime. This avoids replacing unchanged class bodies solely because source insertion shifts debug line tables. Every excluded compiled class hash is recorded. The complete 127-entry original runtime and the one-entry shared harness remain unchanged and are reverified before and after execution.

Run order is fixed: one fresh DENSE JVM, three warmup queries and one measured query, full consumption, 64 × 128 × 128 rows, tuple width two. The original `-Xms1024m -Xmx1024m -XX:+UseG1GC`, JDK 25, native/direct adjacency configuration, AUTO strategy selection, exact oracle, query, graph, checksum, and root audit remain in use. Only the source identity argument and the diagnostic output property identify this instrumented runtime. Existing timing and allocation fields remain raw evidence but are not analyzed as performance estimates.

The runner rejects injected `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`, and `_JAVA_OPTIONS`. It checks the recorded instrumented source identity, exact JVM arguments, Java version/vendor, heap size and ledger limits. Direct adjacency is configured as PREFER and the unchanged harness waits for readiness before running queries. This harness does not expose positive adjacency lookup-hit counts, so configuration/readiness is not described as proof of individual adjacency lookups.

Preparation executes no Java. Compilation and execution each require the coordinating agent's exclusive slot. Independent source review precedes execution. No builds, tests, other JVMs, profiling, or compression may overlap the diagnostic JVM. No substitution fork, shape rescue, or favorable sample selection is permitted.

## Validation and interpretation

All four queries must pass exact membership, parent/leaf correlation, duplicate rejection, complete bag count, checksum, and zero ledger endpoints. The measured query must use the normal compiled IR row route and the outer-root audit must show no hosted generic root. Page admissions, removals, table size, histogram totals, per-target counts, directory/memo reservation attempts and all observed charge deltas must reconcile. The runner requires one materializer owner per serial query before assigning owner records to warmup/measurement phases.

Namespace and parent multiplicity are derived offline from the raw table. A parent token is scoped by namespace and adjacency group; per-target parent identities also include the tuple column. Numeric parent values from unrelated groups are not collapsed into one source identity. Refused publication counts are calls, not counts of unique refused pages.

The first-refusal key order is target, namespace, group, parent, column, page index. Its kind is 0 for none, 1 for the initial directory, 2 for directory growth or capacity overflow, and 3 for a page payload claim.

The structural diagnostic can distinguish dense, useful pages from singleton runs that consume a full page, and can identify which charge reached the query limit. It cannot determine whether a different page layout or admission policy would improve runtime. Such a change would require a separately reviewed correctness-preserving candidate and a matched comparison after this diagnosis.
