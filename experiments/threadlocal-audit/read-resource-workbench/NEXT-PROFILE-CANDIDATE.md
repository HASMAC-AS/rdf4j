# Next actual HTTP experiment: protocol writer before broader TLS surgery

Read-only source/profile review at checkpoint7f656b80. No implementation or measurements. Clean factorial has no demonstrated HTTP gain from depth/resource holders; do not promote them because helper counts changed.

## Recommendation

Lead with a bounded **Workbench compact-row direct token writer** experiment, not another ThreadLocal map tweak. Preserve the query engine, binary Server transport, protocol and batching. Emit the existing packed RDF term arrays with Jackson's streaming generator directly from BindingSet into the existing RowOutput instead of constructing generic List/Object[] trees and serializing them with ObjectMapper for every row. Use Jackson escaping/number/null APIs; do not hand-roll JSON quoting.

This targets real sampled work with a small protocol boundary. It does not imply JSON is a larger wall-time cost than all lifetime code or that token checks disappear. A direct generator still calls _verifyValueWrite. The test is whether bypassing intermediate trees and generic serializers reduces end-to-end cost without degrading latency/correctness.

## Evidence and callable path

Latest HTTP nearest categories base/depth/resource/both:
- Workbench NDJSON131/151/183/116 samples.
- Snapshot/binding118/121/142/136.
- Lifetime/read/replay257/259/237/198.
- Binary writer65/64/55/62.
All are sampled categories, not timings. Lifetime remains a large category but comprises many correctness-sensitive boundaries, not one removable operation.

Top leaf UTF8JsonGenerator._verifyValueWrite is106/100/130/75 across arms. Full stack connects it to StringSerializer→ObjectArraySerializer→IndexedListSerializer→ObjectMapper.writeValue→WorkbenchPageResultWriter.CompactRows.add→handleSolution→TupleResultBuilder.result→QueryEvaluator.addResult→evaluateBrowserBatch→QueryServlet. This is the actual HTTP path, not an isolated JSON microbenchmark.

Source `WorkbenchPageResultWriter.handleSolution` creates a List<Object> sized to bindingNames for every row, looks up each binding, and calls packedTerm for compact mode. packedTerm creates Object[] values for IRI/bnode/literal/RDF-star triple; nested triple terms recursively build more arrays. CompactRows.add resets RowOutput, invokes JSON_MAPPER.writeValue(row,values), and folds serialized row bytes into capped records. Noncompact term-map mode is a separate path and should remain unchanged in an initial patch.

## Minimal first arm and bounds

Add compact-only direct emission accepting BindingSet plus existing bindingNames. Start/end row array; for each name preserve null/unbound slot; emit exact existing packedTerm variant and field order with the same lexical/datatype/language/direction/bnode-origin conversion. Preserve the same Value access order as far as practicable and do not alter binary parser or add term memoization yet.

Keep existing RowOutput, record ByteArrayOutputStream, MAX_ROW_RECORD_BYTES256KiB, first-record32 rows, later1024 rows, metadata ordering, flushes and oversized-row switch. Do not enlarge batching to manufacture throughput at first-row latency expense. Preserve client-disconnect/heartbeat-invalidated IO exception unwrapping and synchronized public write methods.

First isolate direct emission with ordinary per-row generator lifecycle. A second independent experiment may reuse a generator, but must flush its internal bytes before resetting RowOutput, avoid root separators between rows, reset array/root context correctly, and avoid closing the underlying response. Generator pooling/reuse is not automatically safe after an exception or oversized-row streaming transition. It may save per-row setup but should not be bundled before the simpler path is qualified.

## Correctness / workload

Existing WorkbenchPageResultWriterTest is starting coverage. Require byte- or protocol-equivalent rows for IRI, scoped/raw bnode origins, unbound values, simple/typed/lang/direction literals, embedded triples, Unicode/supplementary characters, escapes/control chars, empty results and headers/metadata. Test row sizes around cap, oversized literals/triples, first32/later1024 flush boundaries, progress/error/end records, abort/disconnect and no extra output after terminal.

Use original frozen actual HTTP harness and full100k query with same Server binary stream/Workbench output/client validation. Add wider/high-entropy/nested-term cases only as prespecified secondary cells. Check term checksums, wire/protocol validity, first-row/full-window latency, output bytes, bounded memory and cleanup. A local writer-only allocation benchmark can explain mechanics but cannot promote the change. Keep baseline and candidate engine classes identical; no bundled TLS holders or memo/cancellation changes.

## TLS alternatives ranked

1. **Explicit fused session propagation:** stronger future engine candidate than more map churn, but larger semantic reach. Actual nearest attach samples base5/depth17/resource12/both13. LmdbNativeRowStep attaches fusedSession around uncached row.advance plus initialization; Scope currently does get+set+restore and allocates a source-level frame. LmdbFusedKernelRuntime.current() consumers include SIP membership/domain telemetry, factorized batches and kernel bind/materialization reporting. Session carries query budget/shared immutable domains and worker-confined mutable batches. A viable first explicit API would pass owned session to a narrowly selected internal/generated operation while retaining dynamic attachment around callbacks. Need prove actual hot API call frequency and retain NOOP shadowing, inherited worker child, nested query restoration, cancellation/budget release and close semantics. Can't simply skip attach(null), keep a scope across public pulls, or bind worker to root mutable session.
2. **Initializer activation object instead of AtomicBoolean+lambda:** bounded scope-allocation candidate already designed, conditional on surviving allocation evidence. Preserve strong exactly-once CAS, current-closing-thread identity guard and token order. CPU TL caller visibility alone does not prove these allocations survive JIT.
3. **Resource frame allocation reduction:** current holder leaves one frame/supplier chain per activation. Removing/pooling frames is not a cheap next step: suppliers are dynamic, parent union/frame presence matters to estimator admission, wrong-order and foreign close are observable, and callers can retain closed frames. No current allocation evidence isolates surviving frame cost. Reusing an owner-free empty holder did not demonstrate HTTP benefit; don't infer that eliminating a frame must.
4. **ValueStore synchronous explicit ReadContext holder reuse:** semantically bounded design exists, but current broad HTTP memo-heavy query doesn't establish meaningful decode/transaction-setup frequency. Qualify a real lexical-heavy route first rather than claiming it addresses this profile.

Binding/snapshot category is also material, but no new exact redundant copy was established in this review. Removing detached snapshots or changing lazy-value lifetime to save allocations would cross safety boundaries; inspect concrete representation/copy source before proposing that as a patch.

## Decision

Choose compact token emission as the next isolated actual-path experiment, with no speedup expectation baked into acceptance. If parent requires continued TLS-only work, explicit session propagation needs a separate source/call-frequency audit before implementation; current evidence does not justify broad scope elision. Preserve throughput AND first-row/full latency as outcome, not removed lookups or smaller helper profiles.
