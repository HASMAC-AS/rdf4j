# Bounded Workbench ordered-values path

## Motivation and evidence boundary
Continue the authorized throughput/latency investigation after compact serializer checkpoint18bfd812. QueryEvaluator.addResult collects declared values, copies them to Object[], TupleResultBuilder constructs QueryBindingSet and temporary bindings, then Workbench looks them up again before packing. This path is source-confirmed and present in profiles, but its narrow sample support is smaller than the broad binding category. Do not assign all binding samples or predicted time savings to this adapter. This is a non-ThreadLocal representation experiment; TLS audit findings remain distinct.

## Required semantics
Keep the complete original source getValue phase before any RDF lexical getter/packing and keep complete row packing before output. Do not pass original BindingSet directly. Only a concrete compact Workbench writer may accept an ordered list after unique/non-null/aligned declared names are proven. Preserve arbitrary Java-object builder conversion, generic/legacy/download fallback, duplicate/null-name behavior, synchronous list/row ownership, reused input rows, blank-node origin negotiation, cancellation/progress/heartbeat locking, record caps and exact flush/disconnect semantics. No serializer or scope changes folded into this patch.

## Plan
- [in_progress] Establish bounded design and baseline contracts
- [todo] Implement guarded ordered-values query path
- [todo] Independently review and qualify exact outputs
- [todo] Measure matched standalone and combined configurations
- [todo] Publish complete evidence and next recommendation

## Source and timing identities
Standalone baseline is5ac89b5. Qualified combined comparison stack isd94617b3 (depth+typed serializer), not a proven universally fastest configuration. Compare baseline, qualified stack, ordered alone, qualified stack+ordered. The second factor is the existing stack as a whole; do not attribute its interaction specifically to depth or serializer. No holder, null-initializer restoration, record cancellation or test seam in measured layers. Reconstruct exact class-family baselines before modifying; preserve old evidence. Candidate source/test commits pending qualification.

## Coordination
writer_guard_design owns first CPU/build/test window and minimal implementation. native_token_path independently reviews semantics. inventory prepares provenance and prespecified HTTP protocol read-only until handoff. Coordinator publishes and reports. Canonical benchmark lock excludes overlap. No production edits before baseline/TDD evidence; no outcome-driven timing reruns or trimming. First-row latency tradeoffs matter alongside full completion/throughput.
