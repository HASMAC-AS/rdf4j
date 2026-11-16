# Namespace lifecycle: bounded follow-up hypothesis

This is read-only guidance for a later experiment. It introduces no implementation, draft patch, benchmark arm or measured-savings claim. The current 16-fork comparison and its stopping rule remain unchanged.

## What the diagnostic and source establish

The frozen DENSE diagnostic reports CPU leaf samples in `releaseNamespace` (89), `findNamespace` (55), `namespaceIndex` (52), `retainRange` (28), `releaseRange` (14) and `emit` (12). These samples support investigating namespace bookkeeping separately from coordinate copying; they do not establish its causal share of latency or a prospective speedup. See [profile-report.md](profile-report.md).

The relevant implementation is [KernelResultCoordinates.java](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java), specifically `retainRange`/`releaseRange` (lines 808–821), `retain`/`release`/`releaseNamespace`/`namespaceIndex` (831–870), and the directory maintenance methods (888–988).

Each valid coordinate cell in a source sidecar or saved frame currently performs its own namespace lookup and reference-count update. The lookup hashes the token and probes the open-addressed directory. `save` releases an already active frame's entire previous range, copies registers, then retains the new range; `releaseFrame` releases its range. `retainBatch` and `releaseBatch` use the same helpers.

The output window already has a different, coarser lifetime: `emit` pins a namespace at most once per window, and `releaseRow` releases the window's pins only after its last active row. `lastOutputNamespaces` avoids repeated pin attempts for the same output column; `namespaceWindowNext` deduplicates pins across columns. Optimizing those paths as though they still retain once per emitted cell would misidentify the existing work (lines 595–605 and 686–731).

Repeated sidecar tokens are plausible from the producer structure. [LmdbNativeKernelBindings.java](../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelBindings.java), `BoundCursor.fillRows`, captures a row-major coordinate batch and retains all `rows × requested columns` cells (around 1635). `mappedNamespace` reuses a mapped token for the same source namespace until retirement/activation change (1679–1757). This establishes a mechanism for repeated tokens, but the diagnostic did not measure adjacent-token run lengths. Single-cell batches and alternating namespace sequences may offer little benefit.

## Smallest plausible next change

Investigate a **call-local last-namespace/slot memo inside `retainRange` and `releaseRange`**. Resolve the slot for the first valid managed token and resolve again only when the token changes. For repeated tokens within that one call, keep the existing individual reference-count increment/decrement and its checks.

This targets repeated hashing/probing while leaving ownership multiplicity and publication timing intact. It introduces only local primitive variables: no retained cache, array, object, reservation, source handle or value reference. It deliberately does not coalesce counts, rewrite frame replacement, or change output-window pinning in the same candidate. The additional branch can itself cost time when tokens rarely repeat, so a later comparison must establish the tradeoff.

A cached slot is safe only within that narrow call. Neither range operation invokes an external consumer, allocates a namespace, grows the directory or removes a directory entry. `releaseNamespace` can enqueue retirement, but enqueueing changes queue links/state without relocating slots. Actual deletion occurs in `drainRetiredNamespaces`; `removeNamespace` backward-shifts collision chains, while `ensureNamespaceCapacity` rehashes the table. Both invalidate stored slot positions across calls. A persistent cached slot would therefore need additional invalidation machinery and is outside this smallest proposal.

## Invariants that must remain exact

- Each valid managed coordinate still contributes one reference, including duplicate cells. Invalid positions and namespace sentinels `<= 1` keep their current behavior.
- Missing managed tokens, reference underflow and overflow retain the existing disable behavior and per-cell checks. Do not replace a failed lookup with a previously cached slot.
- Keep the existing `enabled()` stopping points and execution-confinement assumption. No slot escapes the call or survives growth, collision deletion, disable or close.
- Retirement remains a request followed by delivery only after all register, frame, source-batch and output-window references are gone. Queueing must not become immediate page retirement.
- Output-window pins remain independent from frame/source pins. Partial consumption, out-of-order row release and retained result values keep their existing lifetimes.
- Reservation sizes, non-reclaiming admission, old-plus-new growth accounting and final ledger release remain unchanged. This proposal allocates no additional retained metadata and changes no materializer authority or mutation witness.

## Useful future verification boundary

Existing [KernelResultCoordinatesTest.java](../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinatesTest.java) cases already cover independent frame/source/window ownership, repeated namespaces and out-of-order row releases, collision-chain deletion, directory growth/refusal, thousands of namespace retirements and fatal disable cleanup. The matching materializer and interpreted/compiled transport selections should remain part of a later neutral-refactor gate.

A targeted additional characterization, if those existing cases do not hit the new local-memo branch, would retain a sidecar containing repeated tokens mixed with unbound/sentinel cells, relocate the namespace directory by collision deletion and growth between calls, then release that sidecar. Assert exact retirement membership and timing, surviving coordinates, and zero ledger use after close. An unknown-token case should still disable transport instead of reusing the last valid slot. These distinguish correctness from an optimization that merely passes a uniform repeated-token case.

For later performance work, first establish whether the hot range calls contain repeated adjacent managed tokens and whether fewer probes reduce CPU cost under the same route and workload. Do not infer a predicted saving from the sum of the sampled leaf counts. A broader frame-reference-delta or batched-count design would be a separate proposal with separate lifetime proof.
