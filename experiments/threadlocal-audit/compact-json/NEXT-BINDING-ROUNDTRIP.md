# Deferred candidate: Workbench tuple row round-trip

Read-only investigation while the serializer factorial runs. No implementation, benchmark, claimed allocation savings or new experiment.

## Actual path

For the HTTP browser tuple batch, `QueryEvaluator.evaluateBrowserBatch` (~348–378) obtains binding names once, starts the builder, then for each emitted row calls `addResult` before incrementing emitted count, flushing the complete window, checking the materialization checkpoint and updating progress. Rows outside the output window are still consumed as required for totals; the known-total lookahead and early-stop rules are independent.

`QueryEvaluator.addResult` (~777–783) clears a reusable List<Object>, calls the source BindingSet.getValue for **all declared names in order**, and then allocates `values.toArray()`. `TupleResultBuilder.result(Object...)` (~242–253) creates a QueryBindingSet, skips nulls, converts each non-null object through outputNamedResult and adds it. For already-RDF Values conversion returns the same Value, but constructs SimpleBinding wrappers. QueryBindingSet.addBinding(Binding) then extracts name/value and stores them in its internal map. `WorkbenchPageResultWriter.handleSolution` reads all declared binding names from that new set, calls packedTerm/term, creates the packed row, and only after complete packing enters compact serialization or ordinary batching.

The source parser path is separate: `BinaryQueryResultParser` (~204–209) supplies a ListBindingSet over fixed column headers and an unmodifiable completed tuple list; ListBindingSet.getValue uses names.indexOf, so projecting W columns can involve repeated linear scans. Do not assume every Workbench row is that exact class: `BNodeOriginTransport.map` (~153–170) preserves the original row when values do not change, but creates a MapBindingSet when identity/provenance mapping changes them. The server's CompactBindingSetSnapshot lookup appears under BinaryQueryResultWriter on the other side of HTTP, not in this Workbench adapter.

## Semantics that a bypass must preserve

1. All source getValue calls happen before any Value lexical getter/packedTerm call. Passing the original BindingSet straight into the writer interleaves these phases and can change callback/lazy/error behavior. A later source getter that throws currently prevents all term packing and output.
2. All Value packing completes before row serialization, even if an earlier term is oversized. The current serializer experiment intentionally retained this boundary.
3. Null source values become unbound slots. QueryBindingSet stores declared names, so duplicate declared names are not a trivial positional identity: with assertions enabled duplicate add can fail; otherwise overwriting changes which value subsequent duplicate lookups observe. Do not silently replace this with first-wins or independent positional values. Null names and size mismatch also require existing behavior or a conservative fallback.
4. Generic builder.result accepts URL and arbitrary literal-convertible Java objects. A typed query-only fast path cannot replace those semantics globally or introduce an ambiguous result(null) overload.
5. Do not retain the reusable List or original BindingSet beyond the synchronous call. Some input iterations may reuse mutable rows; current packing captures primitive/string output before return.
6. Preserve synchronized writer row ownership relative to progress/error/flush, close on cancellation/disconnect, exact output counters and flush points. There is no authorization to increase batches, read ahead, or hold the repository iterator open longer.
7. Preserve declared-variable order, omission of extra bindings, blank-node provenance and recursive terms. No bypass of BNodeOriginTransport or repository materialization.

## Bounded proposal to evaluate later

Prefer an explicit typed **ordered-values** route, not direct BindingSet forwarding. Keep QueryEvaluator's existing complete getValue loop and reusable list. Only for the concrete Workbench compact writer, with unique/non-null declared names and exact column alignment proven once at result start, let the builder synchronously pass that list to a writer method that performs the unchanged packing loop by position. This would structurally avoid the intermediate toArray, QueryBindingSet construction, temporary SimpleBinding wrappers and the second name-lookup pass. Keep the original route for legacy/download writers, arbitrary Object results, unsupported shapes and duplicate/null-name cases.

This is a proposed change in representation, not yet a measured gain. Adding a specialized method couples the builder to the compact protocol and requires careful shared start/ensureOpen/emptyStartPending/dataStarted behavior. A safe implementation should share row packing/commit code rather than duplicate the writer state machine. Simply substituting ListBindingSet can retain O(W²) lookups and change duplicate-name behavior; it is not automatically the desired fast path.

## Existing coverage and required additions

Relevant existing tests:
- QueryBatchEvaluationTest.smallTupleBatchStreamsItsWindowAndDrainsForAnExactTotal
- QueryBatchEvaluationTest.aContinuationWithAKnownTotalStopsAfterItsWindowAndOneLookaheadRow
- QueryBatchEvaluationTest.defaultBatchStreamsOneMillionRowsAndDrainsTheSameQueryToCompletion
- QueryBatchEvaluationTest.anEmptyArbitraryTupleWindowStillReportsTheExactOriginalTotal
- QueryBatchEvaluationTest.eachRerunDeclaresItsOwnBlankNodeScope
- QueryEvaluatorTest.shouldAbortTupleMaterializationWhenBreakerTurnsCriticalMidStream
- QueryEvaluatorTest.genericTupleDownloadNegotiatesRemoteBlankNodeOriginsAndRemainsParseable
- WorkbenchPageResultWriterTest.negotiatedBlankNodeTermsKeepRawIdsAndPerValueAuthority
- WorkbenchPageResultWriterTest.aDisconnectWhileStreamingAnOversizedRowSurfacesAsAnIOFailure
- WorkbenchPageResultWriterTest.progressCannotInterruptAnOrdinaryOrOversizedRowRecord
- WorkbenchCompactTokenWriterTest.laterTermFailureStillOccursBeforeAnyRowBytes, plus exact bytes/batch/cap/depth regressions.

These do not prove the proposed two-phase adapter change by themselves. Add a recording BindingSet/Value to assert every source getValue precedes the first lexical getter; later-getter failure with an earlier oversized term; reused/mutated source rows; unbound and reordered/extra bindings; duplicate/null-name conservative fallback; generic URL/object/legacy/download fallback; zero variables; and callback/heartbeat/cancellation behavior at row and flush boundaries. Structural hit proof should establish that the qualified compact path does not construct the intermediate map while fallback still does.

## Profile support and limits

The corrected actual HTTP depth profile has QueryBindingSet.addBinding among leaf samples (5), and serializer stacks run through TupleResultBuilder.result. That confirms this adapter executes in the measured route. Broad snapshot_binding counts (113 in that depth recording; 118/121/142/136 in the later four-arm diagnostics) mix server snapshot creation, binary writer lookup and other binding work. They cannot be assigned wholesale to the Workbench round-trip. Sampled arrays/maps/entries similarly include planning and unrelated paths; no exact per-query allocation or recoverable time follows.

Current evidence is stronger for inspecting the concrete redundant representation than for predicting a speedup. First finish the dispatch-only factorial. If this candidate is authorized next, compare independently and combined under the same full HTTP latency/throughput protocol, with immutable runtime identities and width variation where appropriate. Do not infer that an improved microbenchmark guarantees end-to-end gain.
