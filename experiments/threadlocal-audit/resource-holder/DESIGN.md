# Actual-HTTP resource-context holder consolidation

Design only; no production changes or promotion. Actual Workbench profile at depth-workbench/HTTP-PROFILES.md identifies enterReadResourceContext nearest ThreadLocal caller13 base/30 depth samples and ReadResourceContext.close7/5. This is actual-path evidence for investigating this boundary, not its removable time. Clean holder-depth HTTP result remains no demonstrated improvement.

## Bounded implementation

Introduce a private **static** ReadResourceState containing only `ReadResourceContext current`. Replace per-store `ThreadLocal<ReadResourceContext>` with `ThreadLocal<ReadResourceState>` initialized by an owner-free constructor reference. No store reference, frame history, supplier, lease, Thread or connection in the empty holder.

Keep existing ReadResourceContext frames, previous links, suppliers and closed flag. Do not pool/rearm frames.

- Enter: get current thread's holder; read previous=current; construct frame after requiring ownedViews nonnull; assign holder.current=installed only after construction succeeds; return same frame API.
- Close: if already closed return as today. Get **closing thread's** holder. If holder.current!=this, throw same nesting-order error without setting closed or changing either thread's state. Then set closed=true and holder.current=previous. Do not store the entry thread's holder in the frame.
- currentReadResourceViews: get holder.current and retain null→empty / nonnull→dynamic chain behavior.
- GuardedEstimatorStatementSource.getStatements: read holder.current at same point; leave resource-presence, borrowed single-view, dynamic union and deferral conditions intact.

Source audit finds exactly these four lookup sites and the enter/close map mutations. No external access to the private key. Worker captured-read activation reaches enter normally, so receives that worker's holder. Public/escaping close is not assumed same-thread. Cross-thread simultaneous close of one frame is not newly guaranteed; preserve existing closed-flag semantics rather than adding synchronization casually.

## Static reduction and costs

Normal completed scope currently: enter get+set; first close get+remove (or set previous). Candidate: enter get+field write; close get+field write. **Two map mutations removed, two gets retained per frame**, no scope extension. This is not the inconclusive set(null) experiment, which retained map mutations.

Source-level per-scope frame allocation unchanged. One holder object is added per first touched store/thread, even if its first operation is a read-only empty-context lookup. The persistent ThreadLocal entry plus holder persists for that pair while key is live. With S stores/T workers fully cross-touched, upper live-key count is S*T entries and S*T holders, versus static initializer at most T entries. Dead weak keys may occupy capacity until cleanup. No exact byte/latency prediction.

At outermost close, holder.current becomes null. Because the holder is static/owner-free, this value cannot retain store→ThreadLocal-key indirectly. Active frames still retain owning store/suppliers/previous as baseline. Caller-retained closed frame likewise preserves baseline retention; don't clear refs in this patch. Test store/frame-owner collection with persistent idle workers after frames released, and many-store footprint separately.

## Correctness constraints

Dynamic supplier evaluation and identity-unioned parent leases remain unchanged. Equal/empty views do not justify skipping frames: estimator admission distinguishes context presence from view count. Keep source read-view, callback admission, replay worker generation, preferred view, lease retention and cleanup ordering intact.

Wrong-order or wrong-thread close must reject before marking closed, allowing legitimate owner-thread cleanup afterward as baseline permits. Calling currentReadResourceViews on foreign thread remains empty unless that thread has its own frame. Idempotent close remains a no-op after successful close regardless of caller thread. Normal nested callbacks preserve parent frame identity. No depth substitution, source-pull fusion, query-wide scope, or explicit session rewrite.

## Tests first and qualification

Baseline semantic tests before implementation: nested dynamic suppliers and parent union/dedup; empty-frame estimator behavior; exception restoration; null supplier leaves parent intact; correct idempotence; out-of-order failure with subsequent correct close; foreign-thread failure leaves creator state intact and unclosed, then creator closes. Worker captured resources/growth/replay/setup failure use existing LmdbEstimatorPublicationAdmissionTest, replay/exposure/worker and registration suites. Do not claim broad safety merely from new narrow tests.

Structural red/green: inspect static holder/no enclosing reference, current-thread lookup in close, no ThreadLocal.set/remove at resource frame boundaries. No per-row atomic instrumentation in timings.

Leak tests: persistent worker with released activations/suppliers/store references; ensure null head plus holder cannot retain lease/store. Multiple live/discarded stores on worker pool to document empty holder/slot scaling; this is more than a Consumer-only weak-reference test. Keep GC probes outside timed phase.

Freeze an exact LmdbSailStore family layer against immutable baseline or selected depth base. Beware this file currently contains other experimental cancellation/formatter changes: compile from the exact desired source plus holder patch, or prove every unrelated method matches the chosen baseline. Do not smuggle archived changes through a whole-family overlay.

Matched actual Workbench HTTP comparison must use same full-result query, binary Server writer/parser, Workbench NDJSON mapper, counts/checksums, route/memo/no-spill/cleanup proof and first-row/full-window measures. Prespecify base vs holder and depth vs depth+holder if both are desired; no promotion based only on embedded drain. Keep external initializer setter, direct cancellation and test-hook families fixed/absent as appropriate. Follow clean timings with separate CPU/allocation profiles, never pool diagnostics into clean results.

## Why not explicit fused session first

Fused attach also appears in HTTP profiles9/12, but ambient sessions carry SIP domains, budget/telemetry and worker-local mutable batches; null attachment intentionally shadows ambient parent with NOOP. Explicit propagation into generated/interpreted APIs needs a larger callgraph and callback audit. Resource-holder mapping is four private sites with unchanged ownership frames and boundaries, hence a more bounded next implementation. This ranking is engineering-risk judgment, not a predicted performance percentage.

## Independent review checklist for implementing worker

### Holder shape / every head access
A static nested class may legally declare a field whose type is the nonstatic inner ReadResourceContext. Merely declaring that reference does not synthesize an enclosing-store field; constructing the nonstatic *frame* requires an owner, constructing the static *holder* does not. Verify compiled holder fields contain only current, with no this$0 or captured supplier/Thread. `ThreadLocal.withInitial(ReadResourceState::new)` must be an owner-free constructor reference, not a lambda closing over LmdbSailStore.this. Store collection requires both holder.current null and no other live frame retaining the store.

Audit old-key references: one enter read, one close identity read, one currentReadResourceViews read, one estimator current-frame read; two mutation sites become fields. No generic holder helper may turn nonnull-empty-frame into absent. Retain the current getter's dynamic supplier traversal, not a cached list.

### Evaluation / exception order
Baseline enter looks up previous before `Objects.requireNonNull(ownedViews)` executes as constructor argument. Candidate may create a previously absent empty holder before null-supplier rejection, but must leave head unchanged. Frame construction/validation failure must not partially install a frame. Existing parent remains usable and correctly closable.

Baseline close first checks closed without any ThreadLocal lookup. Successful closed token stays idempotent even on another thread. For an open token, wrong-thread/order identity failure must leave closed=false and creator/closer heads unchanged; a fresh empty foreign holder allocation is acceptable bookkeeping but not restoration. Do not capture activating holder into the frame. No promise of concurrent same-token close safety beyond existing semantics.

### Memory test design
- Distinct stores on one live executor must have distinct holders; one store across two live executors must have distinct holders. Read-only reflection can compare identity without mutating private state.
- Complete nested scopes, drop all frame/token/supplier/view references, then weakly observe store and supplier-owned sentinel while worker threads remain alive. Keeping store alive in a test lambda or Future result can invalidate the test; return only WeakReferences from a helper and allow the helper stack to exit before collection attempts.
- Inspect empty holder via a retained weak reference only or a read-only field check on the worker; don't accidentally retain the store through a closed frame in the test. In a controlled test, a strong empty-holder reference is useful to prove it has no backreference to the collectible store, but do not keep the ThreadLocal key/store itself strong.
- Keep distinct-store correctness tests separate from reachability tests. Multiple live stores are expected to retain empty holder objects; that is cost, not leak. Collected weak keys may leave stale map capacity until cleanup; don't equate store collection with zero retained slots or exact reclaimed bytes.
- GC-based collection checks can be sensitive to environment/JIT lifetime. Compare baseline and candidate and preserve bounds/evidence; no microsecond deadline or throughput claim from GC loops. A static-field/bytecode owner-shape check supplies deterministic complementary proof.
