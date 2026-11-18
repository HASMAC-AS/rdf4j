# Read-only width-sensitive source review

Scope: the **same 100k-row, 30-slot** actual HTTP query in an upcoming base/depth/serializer/combined factorial. No edits to production, JVM/build execution or performance measurements. The candidate under review is the published typed serializer, not the later ordered-values adapter. Source basis: `compact-json/{baseline-source,candidate-source}.java.txt`, current QueryStreamCompression implementation, and the frozen fixture/query formulas. The new factorial protocol was not yet available at initial review; the earlier `range-workbench/PLAN.md` supplies qualification context, not authorization to change a new protocol.

## What actually scales with width

1. **The packed objects remain.** Both versions complete WorkbenchPageResultWriter.handleSolution/packedTerm before serialization. Six-slot rows have six bound terms. Wide rows add 24 slots; at F=10 each arm has two members with an unbound four-attribute suffix, yielding an average 19.2 additional bound language-literal terms and 4.8 unbound slots. Thus the average wide row has 25.2 bound terms, not 30 identical terms. Lists, packed Object[] allocations, language access and lexical strings are not eliminated by typed serialization.
2. **This also changes term content.** Narrow projected IRIs/labels/seeds are ASCII. Wide attributes include `é 漢字`, longer lexical strings, null datatype placeholders and `nb` language strings. UTF-8 encoding and Jackson string validation remain in both arms. This fixture does not add quote/control escaping, triple recursion or directional literals to the wide case; those are correctness-test coverage, not this workload's cost.
3. **Typed dispatch has real per-field work.** writePackedValue emits start array, kind number and each field; the field loop checks kind==3, null, Boolean and otherwise String. Generic ObjectArray/IndexedList serializers instead use their cached serializer dispatch and normal null handling. Removing generic dispatch is plausible, but a warmed generic cache/inlined path need not be uniformly slower than a hand-written branch loop. JIT hoisting, inlining and machine-code effects have not been established from source.
4. **Required JSON context checks remain.** `_verifyValueWrite` is not bypassed. Counting logical starts/scalars gives approximately 21 such value transitions per narrow row and 121.8 per average wide row (root + nested starts, tags, strings and nulls). These are schema-derived operation counts, not sampled CPU fractions or exact dynamic machine-call counts. End-array checks and UTF-8 work are additional. A top `_verifyValueWrite` leaf does not establish a removable bottleneck.
5. **Generator lifetime is still per row.** Both create/initialize and close a fresh generator for every packed row. Any fixed per-row dispatch/context cost becomes a smaller fraction of a much wider row. This does not imply all generator backing buffers are newly allocated; pooling/recycling and GC behavior need measured evidence.
6. **The binding and binary paths remain.** QueryEvaluator→Object[]→QueryBindingSet→page lookup is unchanged in all four intended arms. Wider projection increases its work, as well as server snapshot/name lookup, binary writer repeat/equality/encoding and Workbench decoding. Earlier profiles contain these costs, but cannot assign all snapshot_binding samples to a single adapter or quantify their contribution on this wide query.

## Record boundaries and first-batch versus completion

The unchanged source flushes the first compact record at 32 rows, later records at 1024 rows, **or earlier at 256 KiB including record framing**. A single oversized row uses the separate streaming path. CompactRows.flush writes a complete record and calls outputStream.flush. QueryStreamCompression.flush forwards to the codec and servlet destination; gzip/deflate constructors enable synchronous flush. Actual negotiated encoding must be observed rather than inferred.

Illustrative JSON byte sizes were calculated from the fixture formulas with compact UTF-8 encoding (not from timed output):

| Example row | Six slots | Thirty slots |
|---|---:|---:|
| Group 0, all members 0 (all suffixes unbound) |267 bytes|999 bytes|
| Group 99, all members 9 (all attributes bound) |273 bytes|1641 bytes|
| Group 99, all members 7 (all suffixes unbound) |273 bytes|1017 bytes|

These model rows have no single-row oversized case. First 32 rows occupy roughly 8.6 KiB narrow versus32–53 KiB wide, safely below the record cap. Later equal-sized illustrative rows hit the cap at about 956–978 narrow rows versus159–262 wide rows. Real records mix row sizes, so these are not predicted exact record counts.

Consequences worth testing, **not causal claims**:
- FirstRowMs in the existing Node client starts when a whole rows record has been received and JSON-parsed, normally 32 rows. It is not the first engine tuple, first socket byte, browser paint or completion of the whole request.
- Width can increase first-record serialization/transmission/parse time even when completion throughput improves. Fixed startup, plan/JIT/cache, header and flush latency can move differently from repeated full-query work.
- Completion can encounter many more cap-triggered records and codec/container flushes in wide output. Compression, socket backpressure, Node parsing/counting and GC are part of the observed pipeline, not merely engine overhead.
- No ORDER BY is present. Equal full-result bags do not prove the same first 32-row composition or record sequence. If early rows differ in bound suffixes, their byte content can differ even with 32 rows in every first record.

## Questions for protocol/qualification review

1. Are all four arms running the exact same wide query hash, 100-group fixture, budgets, transport/writer flags and application bytes, changing only the intended depth and serializer layers? In particular, exclude resource holder and ordered-values families.
2. Do all four independently pass the full-term/uniqueness oracle and dual order-independent checksums? Do no-spill/migration and ledger-release checks remain per request, with actual compiled/grouping/memo witnesses?
3. Are balanced fresh-server blocks, warmups/trials and contrasts prespecified? Report serializer−base and combined−depth separately, plus depth−base and interaction; a combined-versus-base result alone cannot identify the serializer's wide effect.
4. Can existing parsing record first rows-record count/UTF-8 bytes, total rows-record count and Content-Encoding without adding heavy validation to timed work? Distinguish decoded bytes from compressed wire bytes. Compare untimed first-record term composition without imposing an ordering contract.
5. Are first-record, complete-window, terminal and EOF latencies reported separately, with the same timestamps as prior runs? Avoid pooling a changed first-record measurement into earlier firstRowMs.
6. Do Janino cache/compilation counters and per-fork route evidence reveal obvious compilation differences? They do not prove identical generated-plan bytecode or JIT machine code. Report that limitation rather than attributing differences solely to dispatch.
7. If clean wide results differ from narrow, does a separately authorized profile show where work changed? Source-level hypotheses above are not a substitute for that evidence; no automatic extra profile/timing round is requested here.

The earlier range protocol already makes most admission and measurement boundaries explicit. Its statement that the nested OPTIONAL body is identical is correct; this is not proof that projection width leaves the optimized/generated plan identical. The plan's later route/certification caveats appropriately qualify that distinction.
