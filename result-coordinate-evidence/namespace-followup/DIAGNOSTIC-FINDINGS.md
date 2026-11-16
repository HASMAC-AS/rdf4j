# Namespace diagnostics and the next matched experiment

Two new original-DENSE diagnostic forks completed using native evaluation, explicit direct adjacency, normal AUTO, the preserved graph/query, Java 25.0.5+8, a 1 GiB fixed heap and G1. Each fork ran three warmups and one measured-position query. These were instrumented diagnostics: their timings are not performance estimates and are not pooled with any previous experiment.

## Directory memoization has no work to remove in this fixture

Both diagnostics found no managed namespace references, no namespace-directory lookups or probes, and no source-batch retain/release calls. IDs zero and one do not need directory ownership accounting. Reusing a previous hash-directory result cannot improve this particular execution because no such lookup occurs. The earlier namespace-memo timing draft remains unexecuted.

The frame loops nevertheless perform substantial work. Counts below are per query and per direction: retain and release have the same counts. The first query selected interpreted IR; the remaining two warmups and final query selected compiled IR.

| Observation, per direction | Interpreted first warmup | Each compiled query |
| --- | ---: | ---: |
| Frame range calls | 9,967,405 | 1,567,000 |
| Frame coordinate cells visited | 109,641,455 | 17,237,000 |
| Positioned frame retain/release calls | 95,477,400 | 17,236,968 |
| Direct retain/release calls outside ranges | 11,120,200 | 11 |
| Managed frame or direct references | 0 | 0 |
| Directory lookups/probes | 0 | 0 |
| Source-batch ranges | 0 | 0 |

The independent v2 diagnostic recorded `namespaceIds == null` at entry to every frame range. It counted actually visited cells under that entry flag, without skipping the original loop. All frame ranges in all four queries entered without a directory. Thus the proposed frame guard has 34,474,000 compiled frame coordinate visits per query to eliminate across retention and release. That is a work count, not a predicted latency gain.

## Route, ownership and result evidence

Every query returned 1,152,000 rows with independently expected checksum `3123987218479850048`. Query-ledger use was zero both before and after closure. One coordinate owner opened and closed per query. All counters reconciled and neither diagnostic observed a directory miss.

AUTO selected `irKernelInterpreted` in the initial warmup and `irKernel` for the remaining queries; no strategy/tier forcing was used. Each query recorded one semantic native compilation, and each post-main root audit recorded four in total, zero hosted-generic roots, and normal harness return. Each query observed 55 successful direct-adjacency lookup hits, one planner-stats hit, zero exact misses and no fallback reasons. Adjacency remained ACTIVE, with applied and current data revision both one before and after every query. Lookup hits establish successful query-time direct-adjacency use, which can include planning/probe work; they do not prove that every output row came from an adjacency lookup.

## Narrow candidate and its invariants

The candidate under separate correctness review skips only frame retain/release scans while the namespace directory has never been allocated. It keeps frame copying/restoration and active-frame state unchanged. Source-batch scans remain unchanged because their public array inputs can contain unknown managed tokens that must still withdraw coordinate transport.

Private registers receive coordinates through `set`, `copy` or `restore`; `set` validates a changed managed namespace through its reference transition before a later enabled frame save can occur. An unknown managed token disables the owner. Frames are private and are populated only from registers by `save`. The directory is initially null, assigned by successful capacity allocation, and nulled only after closure. Therefore an enabled owner with a null directory cannot have managed register/frame references. Once a directory has ever been allocated, the candidate keeps the original frame scans, including when all namespaces later retire. It adds no per-row allocation or managed-column bookkeeping.

This reasoning supports a small candidate; correctness tests and a new matched timing study are still required. The observed scan count alone does not establish that the runtime gap to ordinary reuse closes.

## Preserved evidence

`diagnostic-original-v1/` and `diagnostic-original-v2/` each contain exact argv, process result, raw JSONL, post-main root audit, source/runtime descriptor, classpath manifest, console and validated diagnostic summary. `compiled-diagnostic-v2/` contains the exact additive patch, source, compiler command/result and preserved-v1 hashes. The v1 harness and all original 127 runtime entries were unchanged. Independent review approved v1 and v2 diagnostic guards.

The separate `frame-directory-guard/protocol-draft.json` and `run-comparison.py` prescribe original OFF, original DENSE and candidate DENSE across all six permutations, eight warmups and all five measurements per fresh JVM. Candidate correctness and immutable runtime freezing precede the 18-fork study. All arms use the identical uninstrumented common harness with direct adjacency enabled. These future timings form a new protocol and will not be pooled with older disabled-adjacency timings.

## Completed follow-up

The separate frame guard comparison has now completed all 18 prescribed JVMs with every result and route qualified. It did not establish a consistent gain over original DENSE and remained slower than OFF in every pair. The root restored the original production file. See `frame-directory-guard/RESULTS.md` for the full six-round table, independently recomputed ratios, phase/allocation/ledger evidence and the unexplained lower-allocation fork retained without exclusion. No diagnostic timings were used in that conclusion.
