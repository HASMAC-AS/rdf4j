# Independent ordered-values design review

Read-only review before implementation. Source path is QueryEvaluator.addResult -> TupleResultBuilder.result -> WorkbenchPageResultWriter.handleSolution. This is a separate representation experiment, not additional serializer or TLS work.

## Required admission constraints

1. Only concrete compact Workbench writer, exact positional width, non-null unique column names, builder/current writer headers equal. Ordinary page/legacy/download/URL/object conversions retain existing result(Object...) route.
2. Builder `variables(String...)` retains caller array via Arrays.asList; writer copies names. A boolean cached at start is NOT sufficient proof of continued alignment. Source BindingSet.getValue callbacks can mutate that array too. Check current alignment after the complete source getter loop and before packing, or introduce a proven internal ownership mechanism. Do not silently copy/freeze existing public builder variables and change its fallback semantics.
3. TupleResultBuilder is non-final and result(Object...) virtual. A new path must not bypass subclass overrides accidentally. Restrict fast path to exact builder class or explicitly preserve overridden result dispatch; default conservative subclass fallback is cheap and precise.
4. Source Values are already RDF Values: old outputNamedResult returns identical Value without invoking vf. Preserve BNodeOriginTransport before this call and writer origins usage; do not re-create Values or infer blank-node authority from name/ID.

## Snapshot and callback semantics

All source getValue calls must complete before first lexical/packed getter; all packed getters must complete before any row output. Retain exact source lookup order, ignoring extra bindings and preserving null slots. Duplicate/null columns must fall back: original map assertion/overwrite and name lookup semantics cannot be represented as unconstrained positional data.

Existing toArray+QueryBindingSet creates a structural snapshot before lexical callbacks. A new public API accepting a caller-owned mutable List is not equivalent: an earlier lexical callback can mutate a later slot, whereas old snapshot would preserve it. The evaluator reusable list is method-local and not exposed today, so an internal synchronous route with documented exclusive ownership can avoid a new snapshot. Do not retain it or store it on builder/writer, and avoid a new arbitrary public List entry point without an ownership/snapshot contract. Tests should distinguish internal exclusive ownership from unsupported external mutation; no claim of general concurrent safety.

Writer admission, state updates, packing, and row commit should remain under the same writer monitor as handleSolution. Metadata/progress remain serialized. Java monitors are reentrant: lexical callbacks may invoke writer methods, so do not accidentally move ensureOpen/start/dataStarted checks later than before. Preserve empty-start replacement/zero-variable behavior and failure cleanup. A guard returning false must do so before writer state mutation or consuming any values; fallback must not double-pack or double-output.

## Suggested test matrix

- Recording source/Values: every getValue event precedes every lexical event; later source failure with huge first term produces no row bytes or term reads.
- Later lexical failure still leaves no row bytes; original serializer disconnect/oversized/progress and constraints tests remain.
- Reused source BindingSet changes after each row; emitted packed strings remain per-row snapshots.
- Mutation of original names array after start and during source getValue triggers fallback with baseline behavior; duplicate, null, reordered, mismatched widths and empty columns.
- Builder subclass overriding result records same call; legacy and download writers, generic URL/non-Value conversion unchanged.
- Blank-node references vs transient BNodes, recursive terms, unbound/extra columns and zero-variable rows.
- Direct route hit proof plus fallback proof, without timing instrumentation in clean HTTP forks.

No source edits or builds performed by reviewer. Main potential blockers are stale cached alignment, new mutable-list exposure, and bypassed builder virtual dispatch; these can be resolved with bounded conservative admission.

## Implemented candidate independent review

Approved for measurement, no source correctness blocker found. Reviewed SHA-256:
- QueryEvaluator.java: `55fe96154e81356b4a99a7bff44b6c6c2f4ed644f39185bd1b0b4d4bc2c9a7e3`
- TupleResultBuilder.java: `f9d5b8f58a7b3ee73992acc2a1409efc9381173c3a69479827b41b90543b0c9f`
- WorkbenchPageResultWriter.java: `4a0fc006b9247ba7dd3ed78b1cafc5aef9bd16cd55543712bb88115ae8046e17`

All three evaluator scratch lists are method-local ArrayLists, passed only through private addResult, package-private final builder method and final writer. No scratch reference is stored or exposed to lexical callbacks. Recursive query evaluation gets a fresh scratch list, not a shared evaluator field. This supports the narrow exclusive-ownership contract; it is not a general mutable-list API guarantee.

Source getters still complete first. Exact builder class guard short-circuits subclasses to original virtual result(Object[]). Boolean attempt returns false unless the concrete compact writer consumes the row; default inline mocks therefore fall back, addressing the preserved mock-dispatch regression. Guarding columns occurs after getters under writer monitor, checks actual size and equality against copied writer header, with unique/non-null qualification computed from that copied header. Decline occurs before beginRow/state mutation. Matching unique columns means baseline map-to-name lookup is positionally equivalent; Values retain identity and origin handling. Ordinary/noncompact/generic conversion routes retain the old builder.result body.

Shared beginRow and commitPackedRow extraction preserves old handleSolution ordering: ensureOpen/start validation, pending empty header, dataStarted, all packing, then commit. Both entry points hold same writer monitor throughout. No serializer, RowOutput, flush/batch, checkpoint/read-ahead or cancellation change belongs to this delta. Existing reentrant callback behavior is not strengthened into a new guarantee.

Read evidence: baseline family comparison reports all 9 base and 9 stack classes identical. Preserved baseline new-test report has 8 tests, 7 semantic passes and expected structural failure; mock-dispatch-red.txt records later compatibility failures. candidate-focused-fixed.log records 124 tests, zero failures/errors/skips. Reviewer did not execute JVM tests or independently rebuild class families. Full module suite still writer-owned at review time. Positional path StackWalker test proves the optimized entry; callback test confirms later source BindingSet mutation does not replace captured Value.

Optional coverage improvement sent writer: explicit null-column and zero-width guards in addition to duplicate and mutable-column cases. No timing/throughput improvement inferred from structural allocation removal; independent actual HTTP qualification is still required.

Final writer-reported qualification update: full Workbench module 1,574 passing; isolated base 126/126 and stack 128/128 passing with exact-count/container checks. Optional null-name, zero-width, deferred-empty tests passed. Final candidate-source-hashes.json retains the three reviewed stack source hashes; base writer SHA is `37beb0afcfb4be0002ab6baad8f428047617548c653ce965770e8f1b28103d1d`. No reviewer rerun; timing ownership passed to inventory.
