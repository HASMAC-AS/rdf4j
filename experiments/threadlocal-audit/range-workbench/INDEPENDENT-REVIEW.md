# Independent review: wider in-memory HTTP validation

Read-only protocol review; no JVM, build or new optimization. Conditional approval of PLAN.md's small6 (10k), reference6 (100k), wide30 (100k), large6 (250k), base5ac versus depth+serializer d94617b3, four fresh paired blocks per shape, fixed 60 warmups/20 measurements. Admission checks must succeed before clean timing. This is validation of workload range, not merge-readiness.

## Fairness and admission

- Same frozen runtime/dependencies/binary writer and flags; only established depth+serializer families differ. Exclude ordered-values, resource holder, cancellation and test hooks even if archival source contains them. Class-origin witnesses plus family hashes must prove exact arms.
- Fixed 640 MiB per-query cap and same global cap, heap, GC, worker settings. Do not increase budget, drop DISTINCT or force a different execution route to admit wide/large shapes. If either arm fails qualification, preserve failure and report that shape as unqualified instead of selecting only favorable outcomes.
- Require actual compiled-bind increase, no interpreted fallback. Record memo probes/hits, grouped tracker/certificate use where available, primitive/object DISTINCT row deltas, charged peak and spill counters. Flags alone do not prove route. Different shapes may naturally choose different DISTINCT representations; compare arms within each shape and disclose any cross-arm route difference rather than assigning it silently to scope/serializer savings.
- Zero spill files CREATED per request is the key no-spill witness; zero files remaining alone could hide successful spill cleanup. Require ledger usedBytes restored (ideally zero idle both before and after), no unexplained deletion delta, and disk cleanup. Peak is accounting, not RSS or universal physical memory, and a global high-watermark may include earlier warmups rather than one request.
- Independent expected rows/terms must come from fixture generation, not the other arm. Width30 projects all24 attributes from the same WHERE body, including absent optional values. Verify they are functionally determined by original six columns so DISTINCT cardinality remains100k. Check declared order, all30 slots, tuple uniqueness and full-term dual checksum. Matching counts/checksums across two wrong arms alone is insufficient.
- 10/100/250-group fixtures need deterministic source/schema, triple counts, immutable identity and separate state. No concurrent LMDB writers or reused mutated query fixtures. Existing batch maximum1million accommodates proposed full250k delivery; no LIMIT, known-total shortcut or window clipping.

## Measurement meaning

Current readiness waits for value overlay and direct adjacency. This intentionally excludes rebuild latency. Fresh JVM is not cold filesystem/OS-cache; first application query includes different JIT state after readiness but not cold-machine performance. Retain it separately, do not blend with steady measurements. Fixed60 warmups may not converge; keep all measured samples and report predefined thirds and drift.

Current client starts clock just before POST after session/CSRF and pre-request metrics. firstRowMs is set when the first COMPLETE NDJSON rows record is parsed, normally first32-row batch, not first internal tuple or browser paint. EOF includes network, Workbench decode/serialization, Node decode/JSON parse/count/backpressure and final response checks. It is full delivery-pipeline latency, not server-only query time. Full-term hashing/uniqueness validation stays outside clean timing. Use identical per-request row-count/status/protocol/route checks in both arms.

AB/BA/AB/BA pairing balances order, but four forks per arm remain the independent replication units, not80 requests. Report per-pair directions/ranges and cautious intervals; multiple workloads/metrics are not independent confirmations of a universal speedup. Width changes output bytes and client work, so report rows AND bytes and compare speed within each shape. Rows/s derived from sequential full-response latency is not concurrent throughput capacity.

## Coverage limits

This matrix covers width/cardinality of one synthetic nested OPTIONAL/DISTINCT family with repeated terms. It does not cover arbitrary SPARQL, alternate engines, concurrent clients, real datasets, long/oversized literals, RDF term distribution variation, high distinct-value memo miss behavior, spill-heavy workloads, cold restart, browser rendering or release safety. Existing correctness suites supply separate semantic coverage. No merge-ready conclusion follows solely from clean results here.

## Status

Review constraints sent to inventory. Harness implementation and per-shape qualification remain to be reviewed as generated. No timings or results claimed in this note.

## Final harness and qualification review

Approved frozen 32-fork sequence to start. Read run.py, run-matched.sh, client.mjs, query.mjs, oracle.mjs and qualification-summary.json; independently compared saved expected-checksums.json to all eight qualification row counts and dual hashes using a small JSON-only script. No server/JVM/benchmark executed by reviewer.

G/F mapping is10/100/100/250 groups with three fanout10 arms, hence G*1000 rows. Requested batch equals full expected count and offset0; no known-total shortcut. Width30 projection order is s,label,seed followed by each arm node and its eight attributes, and oracle uses that exact order. Query WHERE is identical across widths. Missing suffix is attributes4..7 for member0 or7, verified as null; present attributes include exact lexical text and nb language. Cartesian index=((group*10+m0)*10+m1)*10+m2 spans exactly expected positions. Valid bounds plus no repeated bit and exact expected row count prove full Cartesian coverage; full terms are separately checked. Oracle enumerates tuples independently of server results (hash formulas intentionally agree).

Both arms' saved peaks match by shape:361024,361024,3797344,945760 charged bytes. Qualification has one grouped tracker, one wide admission, one compilation, no interpreted binds/spill/migration; checked expected hash equality for all eight. These are observed witnesses, not complete query-plan identity. Runner preserves deltas for each measured request and asserts compiled activity, no interpreted fallback, no spill creation/migration, memo hits, no compile failure/pending compile, restored ledger use and intended runtime origins. Requested inventory analyze measured grouping/primitive/object/cache deltas for drift too; no timing protocol change needed.

Runner copies immutable shape fixture into each fork directory, awaits identical readiness, fixes640MiB query/2.5GiB shared cap and same heap/flags, and rejects unintended class origins (including ordered builder/evaluator). Paired order is precisely AB/BA/AB/BA within each shape. Full-term validation stays untimed. Broader reporting and measurement caveats above remain in force.
