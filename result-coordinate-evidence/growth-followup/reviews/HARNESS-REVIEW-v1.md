# Independent review of caller CPU and allocation boundaries

The closed Java harness was inspected without editing it or launching Java. The reviewed source was `benchmark/src/org/eclipse/rdf4j/sail/lmdb/evaluation/PhaseMemoQueryHarness.java`, SHA-256 `823d25dc5cb38fd5df6f14115771e8bb3bc5fd17e68a61803936e4349daa8282`. Later formatting or metadata wording changes need their own source binding; this digest records the version inspected for this review.

## Finding

No correctness blocker was found in the new caller CPU/allocation arithmetic for the proposed fully consumed cohort. The counter constructor requires the current-thread CPU capability, enables it when necessary and refuses an unavailable counter. Every sample checks nonnegative, monotonic counter observations. For a reached prefix, the three recorded differences telescope exactly to the caller total:

    to-prefix + tail + after-consume = caller total

The independent auditor checks that identity separately for CPU nanoseconds and allocated bytes, along with elapsed-time interval reconciliation, for every warmup and measured query. Its self-check changes partition values, availability, monotonicity, the validity flag and the nested allocation-window bound to prove these corruptions are rejected. No primary-analyzer arithmetic is imported.

## What each interval includes

The caller start observation precedes connection acquisition, query preparation and evaluation. Consequently, `callerCpuToPrefixNs` and `callerAllocatedToPrefixBytes` include those operations, blocking ordered-result production, and consumption through the row completing the configured prefix. They are not measurements of prefix value lookups alone.

The tail starts at the prefix counter snapshots and ends after the consumption loop and its final exhaustion `hasNext()` check. It includes any cleanup caused by that exhaustion check. The explicit `result.close()` and `connection.close()` calls occur after the tail snapshots. Therefore the most precise scope description is “through final exhaustion checking, before explicit close”; “tail excludes close” would be too broad if understood to exclude all cleanup.

The after-consume interval includes the explicit close calls, any requested retained-row reads and the final counter instrumentation. In the forthcoming `FULL` cohort, retained-row count is zero. If this harness is later used with `RETAINED`, rows arriving before the prefix boundary can have their explicit fixture consumer deferred until after close. The corresponding prefix labels then describe received rows rather than completed lexical consumption. This does not affect the proposed fully consumed comparison.

Each boundary reads caller allocation, then caller CPU, then elapsed time. The counters partition their own observations exactly, but the CPU, allocation and elapsed intervals are adjacent rather than simultaneous. Getter and timestamp overhead is included on one side of these boundaries. They should not be treated as precisely synchronized hardware-counter regions.

## Allocation scope differs between totals

`allThreadAllocatedBytes` retains the previous harness's wider interval: its first observation precedes the caller start observations, and its final observation occurs after `capture.close()` in `finally`. `callerThreadAllocatedBytes` now uses the final caller snapshot taken before that `finally`. The all-thread interval therefore includes small amounts of caller instrumentation and capture cleanup outside the caller interval, in addition to allocations by other threads.

As a result, all-thread allocation minus caller allocation is **not** a pure measurement of background-thread allocation. This difference is harmless for comparisons between arms using the identical harness and is explicitly recorded as a limitation by the independent auditor. No historical allocation observation should be pooled with this changed caller-window definition.

## CPU interpretation

Caller CPU excludes GC, JIT and other worker CPU. It can help distinguish work on the query thread from elapsed-time changes, but a decrease does not by itself establish an end-to-end latency improvement. Whole-JVM GC logs include setup and warmups, so they cannot assign a particular measured query's elapsed gap to collection.

The current early, late and warm-cycle fixtures end their prefix before any policy-driven resize can occur. Tail caller measurements therefore cover the opportunity to observe growth work, while still including lookup, materialization, checksum-consumer and iteration work. A fixed-versus-adaptive difference cannot automatically be labeled “resize CPU,” especially in the larger working-set control where useful hits and source calls can change.

## Remaining external gates

This review does not qualify the new Java runtime, full AUTO chronology, real store IDs or actual memo event counts. The coordinator must first obtain the sealed in-repository red/green contract evidence, compile and qualify the final runtime bytes, run all full-length exact pilots, and freeze the protocol before timing. The independent auditor will refuse incomplete or failed cohorts and analyze no timing data until all scheduled processes are closed.
