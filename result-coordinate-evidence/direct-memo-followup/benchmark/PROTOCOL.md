# Direct strong-value memo: prospective comparison

This is a new experiment. It tests whether the proposed adaptive direct map is a useful replacement for the existing outward-result reuse on two preserved fixtures. No result from the incomplete publication study, its diagnostic runs, or any older benchmark is pooled into this comparison. The publication-qualified work remains preserved separately.

The implementation is experimental and disabled by default. The timed variant adds an identity-witness array to the proposed four arrays. That extra array is needed because a built-in `LmdbValue` can change its internal ID or revision without changing its semantic version. The four-array sketch and the identity-witness variant must remain distinguishable in the correctness evidence. The timing runtime may contain only the identity-witness variant after its correctness and lifecycle qualification.

## What is measured

The primary outcome is total query cost under ordinary garbage collection. It includes the direct map's lookup and replacement cost, its stronger retention of values, its adaptive cutoff and growth, changed resolver work, and the absence of coordinate transport in the direct arm. This is a comparison of complete reuse strategies. It cannot separately identify the CPU cost of hashing, weak-reference collection, coordinate transport, or strong retention.

The same source ownership, accepted-row capture, source/view/exposure checks, detachment, native iteration cleanup, input graph, SPARQL query, Java runtime, heap, query-memory limit, and AUTO strategy selection are used in all arms. The prefix containing the experimental code is also identical across arms. Only the two explicit treatment properties differ.

| Arm | Coordinate transport | Direct memo | Result reuse |
|---|---|---|---|
| `ordinary` | `off` | `false` | Existing ordinary weak last-value reuse |
| `dense` | `dense` | `false` | Existing ordinary weak reuse and dense factor pages |
| `direct` | `off` | `true` | Adaptive strong direct map; ordinary and factor reuse bypassed |

The properties are `rdf4j.lmdb.resultCoordinateTransport` and `rdf4j.lmdb.nativeValueMemo.enabled`. The ordinary arm is not a cache-free baseline. The direct arm does not layer its table behind a successful ordinary lookup: doing so would alter the probe population and the proposed adaptive policy.

## Frozen algorithm

The table is local to one native result iteration and keyed by the numeric value ID and the complete position mask. A cache bank is not a substitute for the complete mask. The first eligible lookup allocates 4,096 buckets. A probe examines exactly one bucket. An occupied bucket with a different full key is a collision; an admitted miss replaces that bucket. There is no LRU list, secondary probe, or weak fallback table.

Probe, hit, and collision counts are cumulative for the iteration. After probe 8,192, fewer than 25% hits permanently disable the memo and release its arrays. That first decision does not grow the table. At probe 16,384 and each later multiple of 8,192, cumulative hits of at least 25% and cumulative collisions of at least 12.5% allow one doubling, up to 65,536 buckets. Successful growth rehashes in deterministic old-slot order and discards entries that collide in the replacement table. Counters are not reset after growth or a scope clear. Admission refusal at initial allocation or growth disables the memo and returns to ordinary resolution.

An admitted miss is published immediately after resolution. It is not postponed until the end of the row, because postponement changes the replacement order and whether another value in that row can hit. No miss eagerly initializes a lexical value. Unsupported values and unknown semantic versions are not admitted. Source identity, non-null stable result-view tokens, exposure scope, semantic version, and the captured internal ID, revision number, and revision owner govern reuse. All earlier ownership and scope-repair behavior must remain intact.

The selected implementation uses five arrays: `long[]` IDs, `long[]` semantic versions, `int[]` full masks, strong value references, and internal-identity witnesses. The experimental charge assumes a 16-byte array header and eight bytes per reference, giving `80 + 36 × capacity` bytes. The initial table is charged 147,536 bytes. At growth, both old and replacement arrays remain reserved until publication; the final 32,768-to-65,536 growth therefore reaches 3,539,104 charged array bytes. The final table alone is 2,359,376 charged bytes.

These are array charges, not a bound on retained heap. Values can retain lexical data, child values, revision or owner objects, and data initialized after admission. A passing timing screen cannot establish bounded query memory or authorize production promotion. In particular, a smaller reported query-ledger peak does not establish a smaller live heap.

## Preserved fixtures and runtime

The fanout fixture uses 100 measurements, 10 readings, width 12, two statuses, and no unique payloads. It produces exactly 1,152,000 rows with lexical checksum `3123987218479850048`. The shared-tail fixture uses 64 roots, 128 left members, 128 right members, and tuple width two. It produces exactly 1,048,576 rows with checksum `8327963502356915200`. Its independent exact-bag oracle checks complete tuple associations and multiplicities. Graph, insertion, query, projection, configuration, and expected-result identities are pinned in the driver.

The base is the preserved `compact-page-followup/compiled-v1` common128 runtime. Its fifteen harness classes and all 127 older entries remain unchanged. A single experimental directory is prepended, yielding 129 classpath entries. That directory contains exactly these six class files: `NativeResultMaterializer`, `NativeProjectedBindingSet`, `NativeDirectValueMemo`, `NativeDirectValueMemoStatistics`, `NativeDirectValueMemoStatistics$Snapshot`, and `DirectMemoLauncher`. Existing plan and nested projection/factor classes continue to come from the preserved runtime. The compiler/freezer records archived sources, the complete patch, emitted classes, selected runtime classes, tests, and lifecycle qualification.

The wrapper delegates to the original root-qualified launcher with its original arguments. It allocates only the primitive statistics buffer before delegation, then serializes statistics after the unchanged launcher returns. Original measured windows and all fifteen harness classes are preserved. The runtime descriptor must identify `identity-witness-v1` and have completed qualification before the main protocol can be frozen.

Java is the preserved 25.0.5+8 runtime. Each fork uses `-Xms1024m -Xmx1024m -XX:+UseG1GC`, original hot counters, a 512 MiB per-query ledger limit, and the unchanged 1 GiB global ledger limit. The fanout fixture retains its original AUTO adjacency budget; shared-tail retains its original explicit 1 GiB adjacency budget. Both are constant across arms within a fixture. All selected strategies are requested through normal AUTO; no strategy or compiler tier is forced.

## Schedule and stopping rule

The main comparison contains exactly 36 fresh JVMs: six rounds, two fixtures, and three arms. Each JVM runs eight warmups followed by all five measured queries, for 468 queries in total: 288 warmups and 180 measurements. The six arm orders are the complete permutation list in this order:

1. ordinary, dense, direct
2. ordinary, direct, dense
3. dense, ordinary, direct
4. dense, direct, ordinary
5. direct, ordinary, dense
6. direct, dense, ordinary

Fanout runs first in even-numbered rounds starting at zero; shared-tail runs first in odd-numbered rounds. Each three-arm fixture block is completed before the second fixture. Each process has a 300-second deadline. Processes are serial; builds, tests, profiles, compression, and heavy file hashing are excluded from the timed run sequence.

Before freezing the main protocol, a separate six-JVM lifecycle preflight runs one complete 13-query JVM for every fixture/arm combination with exactly the same full-size inputs and settings. Its raw timing fields remain preserved because they are produced by the original harness, but they are not analyzed, selected, or pooled. Full-size inputs prevent a smaller fixture from silently qualifying a different AUTO route. The preflight establishes result, lifecycle, owner/query mapping, route, and output-format behavior. It does not select a favorable runtime or warmup length.

The driver stops on any invalid result, process failure, unexpected source/classpath/configuration, wrong route or tier, unreleased ledger or direct-array charge, inconsistent statistics, unexpected mutation or unsupported value on these fixtures, admission refusal, or lifecycle mismatch. It preserves all outputs and does not replace a fork. Low-hit disabling, different resolver counts, collisions, and differing table growth are valid outcomes. There is no post-hoc count tolerance, GC trigger, extra warmup, discarded tail, or additional round.

## Qualification and statistics

Every query must match its expected count, checksum, binding reads, full-consumption status, valid allocation fields, active adjacency revision, and zero ledger endpoints. Every measured query must use compiled native IR. Root audits must show thirteen native compiles and zero hosted generic roots per JVM. The measured route histogram is constant within each fixture. For both direct/ordinary and direct/dense, all thirteen same-round query pairs must match their complete recorded route/tier and adjacency observations. There are 312 such pair checks. Raw resolver counts may differ and are retained as outcomes.

Ordinary and dense create no direct-memo owner. Direct creates exactly thirteen, aligned with semantic-compile ordinals 1 through 13. Each direct owner's cumulative lazy-value interval must match the corresponding original query counter, with no inter-owner gap. The statistics collector preallocates thirteen primitive records and must neither overflow nor omit a close.

The 22 exported scalar fields are the owner ordinal; semantic-compile ordinals at open and close; cumulative resolver counts at open and close; probes, hits, collisions, invalidations, unsupported values, scope clears, successful resizes, rehash discards, low-hit disabling, initial and growth admission refusals; peak capacity, peak charged array bytes, retained entries and capacity just before close, array bytes after close, and whether adaptation had disabled the memo before close. The final forced cleanup does not itself count as adaptive disabling. Scope clears count only when table arrays existed; initial activation and internal close/disable cleanup are excluded. Scope-clear totals are reported without a zero gate.

The `hits` counter counts validated table candidates. A standalone individual-value or collection accessor can subsequently perform ordinary resolution to preserve the source's ownership handoff for an internally owned wrapper. Outward capture passes its actual Sail exposure token. Therefore table hits are not generally equal to resolver calls avoided; the original raw resolver counters remain authoritative, and neither outcome is used to normalize timing or allocation.

The consumer checks necessary accounting invariants, including hits plus collisions not exceeding probes, at most four successful resizes, exact power-of-two capacity and array-reservation peaks, and no retained entries beyond the remaining capacity. A low-hit disable must occur at probe 8,192 with fewer than 2,048 hits, no growth, and no retained table. The earliest possible successful growth is at probe 16,384; a total of `r` successful resizes requires at least `8,192 × (r + 1)` probes. With the fixed hash and exact power-of-two doubling, occupied old buckets rehash injectively: an entry moves to its old bucket number or that number plus the old capacity. Two distinct occupied old buckets therefore cannot collide during rehash. The defensive `rehashDiscards` counter must be zero. These aggregate checks supplement the independently tested algorithm; they cannot reconstruct every historical probe-window decision. Invalidations, unsupported values, direct admission refusals, and final charged array bytes must be zero on the selected fixtures. Original coordinate-owner initial/growth refusal counters must also be zero. This new observer does not separately count dense factor-page refusal and does not claim to do so.

## Analysis fixed before timing

Each fresh JVM contributes the median of all five measurements. A comparison ratio divides candidate and comparator JVM medians from the same fixture and round. The report shows the median and full range of all six ratios, every individual latency ratio, and absolute observations. Fixtures are never pooled. The comparisons are direct/ordinary and direct/dense, with dense/ordinary as a contemporaneous reference.

Latency, all-thread and caller-thread allocation, preparation, evaluation, first-row delay, consumption, close, and query-ledger peak are reported. Raw resolver work and direct-map behavior are reported alongside them. Warm preparation is separate from the first query. The first query starts after fixture construction; it excludes JVM launch and fixture construction and is descriptive only. All eight warmups remain in the raw evidence.

The same engineering screen is applied separately to direct/ordinary and direct/dense. To pass against a comparator, each fixture must have a median paired total-latency ratio at most 0.95, all six latency ratios strictly below one, a median all-thread allocation ratio at most one, and no individual allocation ratio above 1.02. Descriptive findings are reported whether either screen passes or fails. This is neither a significance test nor a production-promotion rule. No timing or allocation is normalized by resolver counts, hits, or retained values.

`analyze-timing-independent.py` is pinned before timing. It rereads raw commands, process results, samples, root audits, and primitive statistics. It imports no experiment driver and does not consume the driver's `runs.json` or `summary.json`. A separate runtime verifier checks complete compiled-artifact preservation; the independent analyzer checks exact commands, source identities, metadata, and all raw result/accounting invariants.

## Commands and durable evidence

The working directory for commands is the repository root. Compilation, correctness tests, lifecycle preflight, and runtime freezing are handled by `contracts/compile_freeze_direct_runtime.py` and the associated contract runner. They produce the qualified descriptor; the timing driver cannot waive that prerequisite.

Freeze only after the runtime has qualified:

    python3 result-coordinate-evidence/direct-memo-followup/benchmark/run-comparison.py \
      --freeze \
      --runtime result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v1/runtime-descriptor.json \
      --protocol result-coordinate-evidence/direct-memo-followup/benchmark/frozen-protocol-v1.json

Validate the frozen design without launching Java:

    python3 result-coordinate-evidence/direct-memo-followup/benchmark/run-comparison.py \
      --protocol result-coordinate-evidence/direct-memo-followup/benchmark/frozen-protocol-v1.json \
      --validate-only

The coordinator releases the exclusive Java slot before executing the fixed comparison:

    python3 result-coordinate-evidence/direct-memo-followup/benchmark/run-comparison.py \
      --protocol result-coordinate-evidence/direct-memo-followup/benchmark/frozen-protocol-v1.json \
      --output result-coordinate-evidence/direct-memo-followup/comparison-v1 \
      --slot-released

Independent analysis uses the exact SHA-256 printed by the pre-timing freeze command, supplied through `--expected-protocol-sha256`; it must not derive a new expected hash from potentially changed inputs. Its output is created in a new directory and retains hashes of all raw files read. Interrupted or rejected attempts remain preserved under their original paths. Any corrected protocol needs a new version and a complete new comparison.

## Interpretation limits

The experiment covers two fixed, repetitive, fully consumed workloads at a 512 MiB query limit. It does not establish behavior for delayed repetition after the first-window cutoff, wide projections, many unique values, retained rows, early close, long lexical payloads, or constrained memory. Correctness contracts can expose those failure modes without converting them into unmeasured performance claims.

Strong retention and table organization change together in the direct arm. A future matched weak direct map could separate part of that mechanism, but it is not a substitute for testing the requested strong proposal. Allocation reductions alone do not establish lower live memory. Resolver reductions alone do not establish a net CPU win. The bounded, balanced comparison tests total cost while preserving the actual routes and result checksums.
