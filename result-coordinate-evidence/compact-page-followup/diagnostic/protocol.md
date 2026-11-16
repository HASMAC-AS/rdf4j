# Compact-page representation and charge diagnostic

This additive experiment observes representation, occupancy, cache participation and reservations. It does not rank latency, allocation, GC or heap use. Original production, candidate production, fixture/oracle, previous instrumentation and earlier raw files remain unchanged. Compilation and each JVM require the coordinator's exclusive slot.

## Main diagnostic

Run separate original and compact DENSE JVMs with this phase's identical common shared-tail harness. Each uses three warmups followed by one observed measurement query, 64 roots, 128 left rows, 128 right rows, two tail columns, full consumption, a fixed 1 GiB heap, G1, a 1 GiB global ledger limit and a 256 MiB query limit. Normal AUTO/native evaluation and direct adjacency PREFER remain configured. There are no route or tier overrides.

Each query must return 1,048,576 rows, checksum 8327963502356915200 and pass the independent exact bag oracle. Validate zero ledger before and after every query, native outer root, normal AUTO IR selection, measured compiled IR, ACTIVE adjacency with current/applied revision agreement, and positive query-time adjacency lookup hits. Adjacency hits can include preparation/probes and do not prove every row used adjacency.

The original control has a single 16,384-byte base claim per dense page and no promotion claim. The candidate has a 512-byte base shell and an optional separate 15,872-byte payload. Diagnostic records preserve that difference. They must not manufacture two physical claims for the original control or equate a charge to measured heap size.

At source generation, copy the exact production source selected by each frozen runtime into a new variant directory. Apply only counted diagnostic additions. Preserve the original and instrumented source, unified patch, helper, complete javac output and hashes. Runtime selection includes only changed diagnostic classes and the helper; unchanged plan/memo classes resolve from the selected frozen runtime. If promotion hooks require the nested page class to change, include and declare that class explicitly. Compare unchanged source regions rather than requiring unrelated generated LineNumberTables to remain byte-identical.

## Observations

Fixed primitive counters and per-target arrays are created once per materializer. They record page admission attempts, base grants/refusals, promotion attempts/grants/successes/refusals, ordinary hits, factor hits, blocked new-page publications, directory grants/refusals/resize overlap and memo grants/refusals. Successful promotion changes representation counts without changing page identity or directory membership.

Immediately before original final cleanup, allocate one primitive page table. For each retained page record the complete target/namespace/group/parent/column/page-index key, representation, inline offset when relevant, occupied-offset mask, nonnull stored weak-reference count, live-referent observation and remaining base/payload reservation bytes. A nonnull weak-reference object is an occupied slot even if its referent is already dead. Weak-reference observation can temporarily strengthen a referent. This is a transient live-reference snapshot, not a stable heap census.

Before namespace retirement or another nonfinal page clear, update histograms and release counters without allocating per-row records. Report those removed pages separately: the final key table contains only surviving pages. Keep declared structural charge and `Reservation.bytes()` observed immediately before explicit close separate. Ledger closure can release a claim before the materializer closes; a grant-minus-observed-release difference alone does not establish a leak.

This fixed full-consumption diagnostic uses a stricter attribution gate: every granted byte must be observed immediately before an explicit instrumented release. An earlier ledger close that prevents that attribution disqualifies this diagnostic's complete release accounting even if the query ends at zero; it does not by itself constitute a production leak. Preserve and report that limitation without substituting another run.

Original owner cleanup remains in a `finally` block around the final snapshot. JSON output occurs after cleanup; output failure fails the diagnostic after resources are released. No diagnostic modifies reservations, admission policy, Values, weak witnesses or ownership logic. No per-row diagnostic allocations are introduced.

## Reconciliation and prospective hypotheses

For each owner, base grants equal admitted pages plus failed page-construction releases. Promotion is observed around the existing outer call: a false result records refusal, and a successful result exposes the new payload claim and its bytes. An unexpected exception may hide an internal transient grant/release; count that exceptional call explicitly and fail diagnostic validation rather than claiming complete grant accounting. The frozen FactorPage implementation remains unchanged. In an accepted run, attempts equal grants plus refusals and successful payload grants equal promotions. Every admitted page closes exactly once. Every successful payload closes with its page. Reconcile cumulative base/payload grants with observed explicit releases, declared releases, endpoint ledger and any earlier ledger release. Directory old/new overlap is separately recorded and all directory/memo grants/releases reconcile.

The source-based uncapped compact hypothesis is 16,640 pages: 16,384 singleton compact pages and 256 full dense pages, totaling 12,582,912 declared page bytes. This is a prediction, not a result. Record actual values even if the hypothesis fails. There may be more admitted pages and factor hits than the capped original control; distinguish improved admission from lower lookup cost. Check final occupancy masks against counts and ensure compact pages hold at most one reference.

Runtime/source/JVM identity is exact. Reject `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS` and `_JAVA_OPTIONS`; require the selected instrumented source identity, prescribed JVM arguments, Java version/vendor, maximum heap, exact fixture digest and common harness identity. Preserve every failed command and all raw records; stop rather than replacing a failed fork.

## Separate constrained retry probe

A deterministic synthetic probe may be added after the production tests settle, with its own source/protocol/result identity and no production API change. It should alternate two distinct offsets in one page while a non-reclaimable competing claim prevents promotion, verify the first inline member remains a factor hit, then release the competing claim and show successful promotion on a later insertion. Count each eligible failed attempt and final recovery. It is correctness/diagnostic evidence only and is not pooled with the full shared-tail query or timing study. No such probe is assumed to have run by this plan.
