# Ordered growth fixtures and phase counters

This study copies the complete existing `PhaseMemoQueryFixture` and uses a new isolated copy of `PhaseMemoQueryHarness`. The source/runtime prefix precedes the preserved common runtime, leaving every earlier study untouched. The three finite inputs are specified in `fixture-oracles-v1.json`; `PROTOCOL.md` defines the comparison and limits.

## Query and input sequence

Each output row is a unique subject `urn:phase-memo:row:` followed by an eight-digit ordinal. It has one object for each predicate `urn:phase-memo:p0` through `p3`. The exact query is:

```sparql
SELECT ?v0 ?v1 ?v2 ?v3 WHERE {
  ?row <urn:phase-memo:p0> ?v0 .
  ?row <urn:phase-memo:p1> ?v1 .
  ?row <urn:phase-memo:p2> ?v2 .
  ?row <urn:phase-memo:p3> ?v3 .
} ORDER BY ?row
```

The ordering key is hidden from the projection. Visit each projected opportunity in row-major order, `j = 4 * row + column`. For `cold-hot`, the prefix key is `j`; the tail key is `(j - prefix) mod workingSet`. For `warm-cycle`, the prefix key is `j mod bootstrapKeys`; the tail key is `bootstrapKeys + (j - prefix) mod workingSet`. Thus the bootstrap and tail key sets are disjoint in the larger-set control.

Every payload is a 64-character IRI: `urn:phase-memo:value:`, an eight-digit key, a colon and ASCII `x` padding. The generator writes statements in deterministic row/column order, checks their count, shuts the repository down and reopens it before running queries. Default source-cache settings remain intact. The insertion digest describes the ordered recipe of big-endian row/column/key/length tuples; it is not presented as a canonical graph hash.

All three fixtures contain 131,072 opportunities and 32,768 rows. Early cold-hot has a 4,096-unique prefix and a 126,976-opportunity tail cycling 64 keys. Late cold-hot has a 32,768-unique prefix and a 98,304-opportunity tail cycling 64 keys. Warm-cycle has an 8,192-opportunity prefix cycling 64 bootstrap keys and a 122,880-opportunity tail cycling 8,192 disjoint keys. The working sets exceed projection width, so these patterns do not repeat a key within the same row.

Expected checksums are respectively `-392792955594352548`, `-1128082570782671460` and `4120647300540988608`. The checksum uses Java lexical hashes, a 64-bit row fold and one-based row weights. The standalone Python oracle computes those expectations without the RDF engine; qualification independently reconstructs them with complete short strings and scalar arithmetic. Exact pilots also check every returned key and padding character. Timed consumers read every Value/string and use the same checksum while avoiding the exact expected-key/padding comparisons.

## Harness and eligibility

The accepted arms are supplied externally through the same classpath and explicit properties. The harness records admission and growth selections; all full comparison arms choose always admission, and both enabled arms use weak retention. Metadata includes JVM properties, heap, source-cache settings, source/runtime identity, exact input/query digests, actual routes and ledger limits.

The existing `full`, `early` and `retained` consumers remain available for investigation, but this finite cohort selects `full`. `trace=true` captures primitive projected IDs/masks and materialization boundaries without retaining extra Values. It is a diagnostic and cannot enter the timing cohort. `exact-oracle=true` also makes an observation ineligible for timing. All output writers create fresh paths and retain failed attempts.

The native ordered wrapper can keep consumed binding sets until close. Consequently the ordered fixture does not establish natural weak-reference collection, useful-key rematerialization, or behavior for streaming results. The memo itself does not strongly own the Values in weak mode, but the result wrapper or source cache may do so.

## Bounded phase observations

The existing elapsed timestamps report connection, preparation, evaluation, first row, consumed prefix, remaining tail, close, retained reads and total. The new harness adds exactly four caller allocation and CPU snapshots at start, consumed-prefix completion, consumption completion and final completion. It does not read those counters for each row. At each boundary the order is allocation, CPU, elapsed timestamp. Current-thread CPU support and enablement are required.

`callerCpuNs` is divided into `callerCpuToPrefixNs`, `callerCpuTailNs` and `callerCpuAfterConsumeNs`. `callerThreadAllocatedBytes` is divided into `callerAllocatedToPrefixBytes`, `callerAllocatedTailBytes` and `callerAllocatedAfterConsumeBytes`. The to-prefix counters include preparation/evaluation and denote a fully consumed prefix only in the selected FULL consumer. The tail includes the final exhaustion `hasNext()` and any automatic cleanup it triggers, and ends before explicit result/connection close. The remaining interval includes those explicit closes and deferred reads if a retained consumer is selected. Each raw metadata record states these distinctions in `callerPhaseScope`. Counter phases sum exactly for each full raw query, and the harness rejects nonmonotonic observations.

Caller CPU excludes GC, JIT and other worker threads. The all-thread allocation total retains its earlier interval ending after selection-capture cleanup, whereas aligned caller snapshots end before that cleanup. Their difference is therefore not a pure worker-allocation estimate; raw `allThreadAllocationScope` metadata records this explicitly. Separately aggregated medians can also break additive identities even when every raw query reconciles exactly.

The per-query ledger peak is the process-authority accounting peak in an otherwise serial JVM. A separate active-query ledger peak is unavailable and remains null. Memo array/reference reservations are estimates, including adaptive resize overlap; they do not bound complete heap or native memory. Source lazy-call counts are not physical decode counters.

## Reproduction

`run_pilot.py` selects a named fixture/arm with the exact lexical oracle and defaults to the full eight-warmup/five-query sequence. `qualify_runtime.py` binds fresh pilot records, test receipts and final class bytes before any timing. `run_comparison.py freeze` creates the finite immutable schedule, `next` launches only one supervised JVM, and `check_route_profile.py` checks the full qualified per-ordinal AUTO history after each completed process. Consult `PROTOCOL.md` for commands, isolation requirements, estimators and artifact gates.
