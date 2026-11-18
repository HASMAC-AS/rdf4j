# Qualified stack across an in-memory HTTP workload range

Compare only original experimental base `5ac89b5` against qualified depth+typed-serializer stack `d94617b3`. No ordered adapter, holder, initializer-restoration, cancellation or memo test seam. This is not current upstream main and not a merge-readiness certification.

## Outcome by workload

The benefit is workload-dependent. Reference 100k six-slot completion improved 8.36%, all pairs favorable with a descriptive 95% interval excluding zero. Small 10k six-slot completion showed no gain. Wide 100k thirty-slot completion was 2.76% slower pooled with mostly adverse signs, but uncertainty crosses zero. Large 250k six-slot completion was 14.29% lower, all pairs favorable but one nearly flat and its interval narrowly crosses zero. No workload established a first-rows-batch latency improvement. Do not claim a universal speedup or merge readiness.

All 32 forks and 2,560 requests completed with no excluded workload, no changed cap, no spills and no failed route/cleanup check. 640 requests were measured; eight separate full-term qualifications also passed.

## Scope and fixed protocol

Actual on-disk LMDB with synthetic RDF, Server binary writer/HTTP, Workbench Java decoder and NDJSON writer, Node streaming decode/count. Browser rendering and concurrent QPS are excluded. Four paired fresh-server blocks per workload (AB/BA/AB/BA), 60 warmups + 20 measured requests; all samples retained. Each server receives a fresh clone of its frozen fixture. Same 640 MiB per-query/2.5 GiB global budget and common JDK/heap/flags.

First row means the first complete NDJSON rows record parsed by Node, normally first 32 rows, not first engine tuple or browser paint. EOF includes client parsing/backpressure. Readiness waits for overlay/adjacency; first application query is not cold storage. Per-workload paired-log t intervals use 4 independent fork pairs, are descriptive/unadjusted and not corrected for multiple comparisons. No pooled overall speedup across unlike shapes.

## Qualification

All 8 cells passed exact every-term validation, unique Cartesian-product bag proof, independently enumerated dual checksums, complete count/status, class origins, compiled route, no spill/migration and ledger cleanup. Wide30 has identical WHERE body to reference6; it projects 24 extra functionally determined attributes including null OPTIONAL suffixes. Formula oracle and full observed diagnostics are retained.

| Shape | Rows | Slots | Base/stack charged qualification peak bytes |
|---|---:|---:|---|
| small6 | 10000 | 6 | 361024 / 361024 |
| reference6 | 100000 | 6 | 361024 / 361024 |
| wide30 | 100000 | 30 | 3797344 / 3797344 |
| large6 | 250000 | 6 | 945760 / 945760 |

These are process-lifetime query-ledger peaks observed after one qualification query, not total memory/RSS. All qualified shapes retain the fixed cap; none was excluded or silently altered.

## Per-workload results

### small6

| Metric | Base ms | Stack ms | Reduction % | Paired reductions % | Descriptive 95% interval % |
|---|---:|---:|---:|---|---|
| eofMs | 93.448 | 93.323 | 0.13 | +3.62, -20.35, +6.91, +10.04 | -22.35 to +19.45 |
| firstRowMs | 35.902 | 36.783 | -2.45 | +3.53, -18.50, +2.87, +1.11 | -19.66 to +12.43 |

Derived sequential rows/s change: +0.13%. This is not concurrent capacity.

| Fork | Arm | EOF ms | First rows-batch ms |
|---|---|---:|---:|
| matched-small6-01-base | base | 92.962 | 35.506 |
| matched-small6-02-stack | stack | 89.597 | 34.253 |
| matched-small6-03-stack | stack | 111.161 | 40.329 |
| matched-small6-04-base | base | 92.368 | 34.034 |
| matched-small6-05-base | base | 95.608 | 39.417 |
| matched-small6-06-stack | stack | 89.002 | 38.284 |
| matched-small6-07-stack | stack | 83.531 | 34.267 |
| matched-small6-08-base | base | 92.854 | 34.652 |

Measured EOF segments (first 7/middle 6/last 7):
- base: 92.200 ms, 92.679 ms, 95.355 ms
- stack: 92.208 ms, 91.065 ms, 96.373 ms

Per-request Janino compilation/cache/instantiation and DISTINCT/memo counters remain in summary.json and raw outputs. A compiled route is not proof of byte-identical generated plans or JIT code across forks.

### reference6

| Metric | Base ms | Stack ms | Reduction % | Paired reductions % | Descriptive 95% interval % |
|---|---:|---:|---:|---|---|
| eofMs | 502.550 | 460.541 | 8.36 | +9.42, +8.15, +9.50, +6.36 | +6.01 to +10.66 |
| firstRowMs | 39.640 | 38.007 | 4.12 | +12.59, +10.03, -21.48, +13.26 | -23.43 to +26.25 |

Derived sequential rows/s change: +9.12%. This is not concurrent capacity.

| Fork | Arm | EOF ms | First rows-batch ms |
|---|---|---:|---:|
| matched-reference6-01-base | base | 490.670 | 40.968 |
| matched-reference6-02-stack | stack | 444.442 | 35.809 |
| matched-reference6-03-stack | stack | 471.855 | 35.800 |
| matched-reference6-04-base | base | 513.716 | 39.792 |
| matched-reference6-05-base | base | 508.070 | 37.228 |
| matched-reference6-06-stack | stack | 459.794 | 45.225 |
| matched-reference6-07-stack | stack | 466.071 | 35.194 |
| matched-reference6-08-base | base | 497.743 | 40.574 |

Measured EOF segments (first 7/middle 6/last 7):
- base: 515.984 ms, 497.260 ms, 493.650 ms
- stack: 465.013 ms, 455.947 ms, 460.005 ms

Per-request Janino compilation/cache/instantiation and DISTINCT/memo counters remain in summary.json and raw outputs. A compiled route is not proof of byte-identical generated plans or JIT code across forks.

### wide30

| Metric | Base ms | Stack ms | Reduction % | Paired reductions % | Descriptive 95% interval % |
|---|---:|---:|---:|---|---|
| eofMs | 2326.836 | 2391.053 | -2.76 | -6.55, -2.14, -2.48, +0.08 | -7.21 to +1.54 |
| firstRowMs | 37.077 | 40.144 | -8.27 | -8.14, -4.29, -25.99, +4.08 | -29.55 to +9.89 |

Derived sequential rows/s change: -2.69%. This is not concurrent capacity.

| Fork | Arm | EOF ms | First rows-batch ms |
|---|---|---:|---:|
| matched-wide30-01-base | base | 2302.453 | 37.395 |
| matched-wide30-02-stack | stack | 2453.344 | 40.438 |
| matched-wide30-03-stack | stack | 2467.939 | 38.362 |
| matched-wide30-04-base | base | 2416.224 | 36.783 |
| matched-wide30-05-base | base | 2265.458 | 35.486 |
| matched-wide30-06-stack | stack | 2321.685 | 44.709 |
| matched-wide30-07-stack | stack | 2321.243 | 37.068 |
| matched-wide30-08-base | base | 2323.210 | 38.645 |

Measured EOF segments (first 7/middle 6/last 7):
- base: 2319.640 ms, 2312.448 ms, 2346.366 ms
- stack: 2379.248 ms, 2338.516 ms, 2447.889 ms

Per-request Janino compilation/cache/instantiation and DISTINCT/memo counters remain in summary.json and raw outputs. A compiled route is not proof of byte-identical generated plans or JIT code across forks.

### large6

| Metric | Base ms | Stack ms | Reduction % | Paired reductions % | Descriptive 95% interval % |
|---|---:|---:|---:|---|---|
| eofMs | 1190.895 | 1020.766 | 14.29 | +19.87, +18.99, +16.82, +0.55 | -0.56 to +27.12 |
| firstRowMs | 40.175 | 39.768 | 1.01 | +6.62, +0.06, +18.46, -25.01 | -31.19 to +25.65 |

Derived sequential rows/s change: +16.67%. This is not concurrent capacity.

| Fork | Arm | EOF ms | First rows-batch ms |
|---|---|---:|---:|
| matched-large6-01-base | base | 1198.705 | 39.246 |
| matched-large6-02-stack | stack | 960.530 | 36.650 |
| matched-large6-03-stack | stack | 1014.132 | 41.237 |
| matched-large6-04-base | base | 1251.846 | 41.262 |
| matched-large6-05-base | base | 1179.905 | 43.849 |
| matched-large6-06-stack | stack | 981.492 | 35.754 |
| matched-large6-07-stack | stack | 1126.908 | 45.433 |
| matched-large6-08-base | base | 1133.125 | 36.345 |

Measured EOF segments (first 7/middle 6/last 7):
- base: 1179.894 ms, 1185.498 ms, 1206.523 ms
- stack: 1030.283 ms, 1030.973 ms, 1002.499 ms

Per-request Janino compilation/cache/instantiation and DISTINCT/memo counters remain in summary.json and raw outputs. A compiled route is not proof of byte-identical generated plans or JIT code across forks.

## Limitations and evidence

Fixed warmup does not guarantee convergence; preserve segment trends rather than select a favorable suffix. Four forks per arm limit precision. Timing counts/route checks run on every request; computationally expensive complete-term/oracle checks are untimed qualification. Whole-JVM GC logs cannot be attributed to individual queries; heap snapshots are not allocation. RSS was not measured. No new profiles were run in this range phase.

Raw request metrics/logs remain local and are published via lossless exact-byte pool dedup with SHA256 reconstruction verification. Source/runtime/fixture manifests, independent formula oracle, qualification diagnostics and reviewer notes define the reproducible scope.

## Measured route/cache stability

For every measured query in every arm/workload: zero Janino compilations and cache misses; one cache hit, one grouped tracker and one budgeted-wide admission; zero spill creation/migration and object rows. Primitive-row deltas equal the workload row count. Both-arm matching is observed per request, not inferred from flags. No observed route/cache drift explains the timing differences, but this is not full generated-plan/JIT identity proof.

Frozen protocol/source/class/fixture manifests and published-setup.json connect this evidence to the prior setup checkpoint. Existing safety qualification is documented in read-depth/broad-safety and compact-json reports; this range run adds workload validation, not a full repository or release certification.
