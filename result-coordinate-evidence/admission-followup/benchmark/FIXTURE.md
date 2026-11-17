# Ordered phase query fixture

This is an additive investigation harness. It changes no production implementation and does not replace any preserved retention experiment. The new entry point is `org.eclipse.rdf4j.sail.lmdb.evaluation.PhaseMemoQueryHarness`; its companion `PhaseMemoQueryFixture` owns input construction and the independent result oracle. `fixture_oracle.py` computes the same declared input recipe and checksum without Java or an RDF query engine. The coordinator owns compilation, JVM launches, runtime qualification and protocol freezing.

## Question and limitation

The fixture tests whether an iteration-local memo earns its admission and lookup cost when repetition begins late, disappears after a hot prefix, or occurs across a larger working set. It preserves AUTO query selection, the source's default value-cache settings and the selected weak-retention implementation. Admission treatment is supplied through the runtime and `rdf4j.lmdb.nativeValueMemo.admission`; the harness only records that property.

The query uses an unprojected `ORDER BY ?row` key to make projected value order deterministic. In the current source, `NativeOrderedRowsIteration.createIteration()` can retain the `ArrayList<BindingSet>` returned by `NativeRowsStep.evaluateAll()`. Consequently, already consumed rows can continue owning their values until the ordered iteration closes. This fixture is suitable for admission and timing experiments. It cannot establish the cost of weak-reference collection and subsequent rematerialization during a streaming query. That requires a separate diagnostic or a qualified streaming fixture. No unordered variant is supplied: removing the order would remove the declared trace guarantee, and allowing the optimizer to choose a convenient observed order after seeing results would undermine the experiment.

## Exact input and query

For projection width `C`, insert one subject per output row and exactly one object for each predicate `urn:phase-memo:p0` through `p(C-1)`. Subject IRIs are `urn:phase-memo:row:` followed by an eight-digit, zero-padded ordinal. The maximum row count fits that representation. Objects occur only in object position; the subject/order variable is never projected. For width four, the exact query is:

```sparql
SELECT ?v0 ?v1 ?v2 ?v3 WHERE {
  ?row <urn:phase-memo:p0> ?v0 .
  ?row <urn:phase-memo:p1> ?v1 .
  ?row <urn:phase-memo:p2> ?v2 .
  ?row <urn:phase-memo:p3> ?v3 .
} ORDER BY ?row
```

Let `j` be a zero-based projected opportunity, visited in row-major order: `j = row * C + column`. A projected opportunity is a place where a bound value appears in an output row. It is not automatically a counted memo probe: the table can disable itself, allocation can be refused, a row may be materialized through another path, or same-row duplicate suppression can bypass lookup.

The two phases contain `P = prefix-probes` and `T = tail-probes` opportunities. Both must be positive multiples of the projection width. `W = working-set` is at least the width, so a cycle cannot repeat a key within one row. In `warm-cycle`, `K = bootstrap-keys` is also at least the width. The patterns are defined exactly as follows:

| Pattern | Prefix, `0 <= j < P` | Tail, `P <= j < P + T` | Purpose |
| --- | --- | --- | --- |
| `cold-hot` | Key `j` | Key `(j-P) mod W` | Delay the first reuse, then repeatedly revisit keys from the original prefix. Requires `P >= W`. |
| `hot-unique` | Key `j mod W` | Key `W+j-P` | Earn admission with repetition, then replace it with a stream of fresh keys. |
| `warm-cycle` | Key `j mod K` | Key `K + (j-P) mod W` | Earn admission on disjoint bootstrap keys before introducing a larger cyclic working set. |

For `cold-hot`, the first logical reuse is opportunity `P+1`, counting from one. This means the same key has occurred earlier; it does not promise a hit in the direct-mapped table. The prefix may already have evicted that key. In `warm-cycle`, the first tail reuse is opportunity `P+W+1` when the tail is long enough. Prefix and tail key sets are disjoint.

Each object is an IRI beginning `urn:phase-memo:value:` followed by its eight-digit key and a colon, then ASCII `x` padding. Ordinary IRIs have exactly 64 characters. The first `large-keys` keys have exactly `large-payload-chars` characters. No expected `Value` objects or complete large expected payloads are retained by the oracle; it retains small key prefixes, primitive lexical hashes, and one shared padding string. That one padding string is an explicitly reported fixture cost outside the query allocation window.

Population creates and inserts the real RDF statements, verifies the statement count and records a deterministic insertion-recipe digest. The population repository is shut down. A new `LmdbStore` and repository reopen the same files before querying, releasing generator-owned Values and insertion-cache roots without modifying default cache settings. The recipe digest hashes a version tag followed by big-endian `(row, column, key, lexical-length)` integer tuples. It is deliberately labeled a recipe digest, not a canonical RDF graph hash. Qualification can check every returned lexical value independently, including every padding character; the timing consumer uses the checksum path described below.

## Admission cases worth qualifying

A robust early-reuse control is `cold-hot`, `P=4096`, `W=64`. A late-reuse control is the same fixture with `P=8192`. Keep the total number of opportunities fixed when comparing those cases if the objective is the cutoff rather than output volume, by reducing `T` as `P` increases. Widths 1, 4 and 16 allow exact phase boundaries at the common power-of-two prefix lengths. Other widths are supported when both phase lengths divide evenly.

The first adaptation boundary is 8,192 actual probes, and survival requires at least 2,048 hits. A trace whose first hit arrives at probe `F` can therefore survive only if `F <= 6145`, even if every later probe hits. First logical reuse at 8,191, 8,192 and 8,193 is uniformly too late under the current rule. With an overlapping cold prefix, `P=6144` is only a theoretical all-hit boundary: any eviction or collision-induced miss in the tail makes it fail. These fixtures must not be labeled as guaranteed admitted cases before the observed counters confirm them.

For admitted-state reuse distance, use `warm-cycle` with `P=8192`, `K=64` and separately chosen `W` values up to 65,536. The table's subsequent growth still depends on cumulative hit and collision totals; the fixture does not promise a particular capacity. `hot-unique`, with a repeated prefix and long unique tail, is the negative control for ongoing lookup, wrapper and metadata costs after the workload stops benefiting from reuse.

## CLI and output

All options are unique `--name=value` arguments. The fixture options and defaults are `--pattern=cold-hot`, `--prefix-probes=4096`, `--tail-probes=65536`, `--working-set=64`, `--bootstrap-keys=64`, `--width=4`, `--large-keys=0` and `--large-payload-chars=1048577`. Common options are `--warmup=8`, `--iterations=5`, `--consumption=full`, `--limit=1024`, `--retain=256`, `--trace=false`, `--exact-oracle=false`, `--arm=...`, `--source=...`, `--output=/absolute/new/samples.jsonl` and optional `--store=/absolute/new/store-directory`. If `exact-oracle` is omitted, it defaults to the value of `trace`, so a trace pilot also receives the exact lexical oracle unless explicitly disabled.

`full` consumes all rows and checks their values immediately. `early` consumes the first `min(limit, expectedRows)` rows, closes the result and connection, and validates the exact prefix checksum. If it reaches the full row count, it also checks that no extra row exists. `retained` consumes the full result, keeps the first `min(retain, expectedRows)` binding sets, and reads those rows only after closing the result and connection. This deferred consumer check remains useful for ownership correctness even though the ordered producer itself may retain consumed rows.

The main output is one JSONL file: one `metadata` record, one `sample` for each warmup or measured query, and one `completion` record. A failure emits a `failure` record and terminates the run. Every output is created exclusively; an existing file is never overwritten. The dataset directory is preserved.

Each sample records connection, preparation, evaluation, first-row, consumption, close, retained-read and total durations; all-thread and caller-thread allocation; process-ledger endpoints and peak; observed AUTO selections; native/root counters; adjacency observations; native projected-row counts; projected binding-set creation/materialization counts; and all completed memo-owner snapshots. The expected and observed checksums are decimal strings to preserve signed 64-bit values. Arithmetic deliberately wraps as Java `long`; the checksum is the sum of each row's lexical-hash fold multiplied by its one-based ordinal.

With `--exact-oracle=true`, each lexical value is compared to the expected key/prefix and every padding character before its checksum contribution is accumulated. This is the qualification path and does not rely only on absence of hash collisions. The primary timing consumer still checks bound IRI type and projection width, reads every `Value.stringValue()`, and computes the same checksum; it avoids expected-key lookup and prefix/padding comparisons inside the binding loop. Every primary query must match the independent expected checksum. This retains the normal limits of a checksum as evidence; the separately run exact oracle provides stronger correctness qualification. Metadata and samples record `oracleMode` and `exactOracle`. Either trace or exact oracle makes `timingEligible=false`. The harness always reports `timingAccepted=false`: external runtime qualification and the frozen protocol, rather than the harness itself, decide whether a complete cohort is acceptable.

The per-query process-ledger peak covers all reservations in this otherwise serial JVM. The public API does not expose the active query ledger's separate peak, so `queryLedgerPeakBytes` is null. The memo's own reservation peak remains available in its owner snapshot, and both global and per-query limits are in metadata. The file does not claim these reservations bound complete Java heap or native memory.

At the prefix's final returned row, the harness records source lazy-call and projected materialization counters and a timestamp. The source counter can include ordering work. The projected counter has a narrower scope, but an output wrapper could prefetch across a boundary. These are observations at a consumer boundary, not unqualified per-phase memo-hit counters. Qualification must reconcile them with the optional trace and owner totals before any analyzer derives phase-specific savings.

## Source-path qualification

The native planner has an explicit path for an order key below projection, and `NativeSortLayout` keeps projection slots separate from order slots. `NativeOrderedRowsIteration` attaches its iteration-local materializer when it returns a native projected row. Those source observations motivate this fixture; they are not a substitute for running it.

Before freezing timings, require the independent oracle and insertion/query digests to match; every consumed row to be a `NativeProjectedBindingSet`; no hosted-generic or unhosted-hard-decline increment; successful non-forced AUTO selections; no omitted selections; clean ledger endpoints; and exactly one opened/closed memo owner for each enabled query, versus none for ordinary reuse. The harness enforces those basic route/lifecycle conditions and reports all counters. Qualification must additionally establish whether counted probes and projected opportunities agree for an admitted run. A disabled initial-policy run should stop counted probes at its observed cutoff while output materialization continues. Do not force a strategy or weaken assertions to make this happen.

`--trace=true` records primitive projected IDs and full position masks, plus source/materialization counter snapshots before demand and after `next()`, into `projected-trace.jsonl`. The expected keys and one-based opportunity position accompany each row. The trace can confirm that masks are object-only (`4`), repeated keys keep their IDs, no hidden order key enters the projection, and consumer demand does not move materialization across the intended boundary. It does not report the table's individual find decisions or a live-value census. Primitive arrays hold no Values or sources. Trace allocation/writes affect the query window, so metadata and samples explicitly mark trace runs `timingEligible=false`; raw-file I/O happens after the timed window. Observed result order and per-row counter deltas must still be reconciled with owner probe/cutoff totals to establish the table's effective sequence.

No GC request or synthetic allocation pressure is introduced. GC logging belongs in the externally frozen JVM command. The source's cache threshold does not rescue the ordered-retention limitation: even payloads outside that cache can remain owned by consumed output rows.

## Bounds and independent oracle

The fixture caps projection width at 32, working set at 65,536 keys, total opportunities at 1,048,576, and key count at 262,144. Large keys contain at most 64 Mi characters in total, and one large key contains at most 2 Mi characters. Because the ordered ordinary arm can retain a separately decoded payload for every occurrence, the sum of projected lexical characters across the full result is capped at 128 Mi. These are input bounds, not whole-query heap guarantees. A bounded long-payload example is `P=8192`, `T=512`, `W=64`, width 4, eight large keys and 1,048,577 characters per large IRI.

Run the independent oracle from the repository root, without launching Java:

```bash
python3 -B result-coordinate-evidence/admission-followup/benchmark/fixture_oracle.py \
  --pattern=cold-hot --prefix-probes=4096 --tail-probes=65536 --working-set=64 --width=4
```

For those defaults, the expected row count is 17,408 and the expected checksum is `-7829599956630130340`. The query SHA-256 is `6a88dfdf0537a3d8204a120dab1f1d3193c9f88c95254f05cc83dd1b9cdbd9cd`; the insertion-recipe SHA-256 is `35f7d79a9016fb08d8a2516c19f33e2c9fce127680b182a5024df7ae93fae47b`. Supply `--consumed-rows=N` to compute an early-close prefix checksum. The Python implementation uses scalar affine exponentiation for repeated padding characters rather than constructing large expected payloads.

The initial source handoff includes only static/Python verification. Compilation, both IR tiers, actual query routes, real counter reconciliation and full 8+5 pilots remain coordinator-run qualification gates. Pilot latencies must not be used to choose fixtures or admission thresholds.
