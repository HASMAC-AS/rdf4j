# LMDB subject-centered star benchmark

**Statement indexes: `spoc,posc,ospc` (SPOC, POSC, OSPC).** Both conventional and chunk-store runs use these three full indexes. Untimed physical diagnostics read their explicit databases, `spocspoc`, `poscposc`, and `ospcospc`.

This branch publishes the standalone `SubjectStarBenchmark` driver used in the original PR #6365 comparison. It adds no production changes. Its base is main commit `49349cc0dc843fbd67dc1c7e52f67b1f9c5d1349`.

## Data and queries

The standard fixture has 12,500 subjects, five predicates, 125,000 triples, and the default graph. Each subject has zero to four objects per predicate, with degree `floor(subjectOrdinal / 5^predicateOrdinal) % 5`. Subject counts must be multiples of 3,125, giving every degree combination equally often and mean degree exactly two for each predicate. Four object IRIs (`urn:star:o:0` through `urn:star:o:3`) are reused across all subjects and predicates. Subject IRIs are `urn:star:n:0` onward; predicates are `urn:star:p:0` through `urn:star:p:4`.

Insertion is subject/predicate/object order or a deterministic shuffle of the same triples (seed `60920031`), with 1,024 statements per commit. VALUES subject order uses seed `60920037`.

Four timed cases cover three- and five-predicate stars, either scan-led or preceded by a shuffled VALUES binding for every subject. The three-predicate scan case is equivalent to:

```sparql
SELECT ?s ?o0 ?o1 ?o2 WHERE {
  ?s <urn:star:p:0> ?o0 .
  ?s <urn:star:p:1> ?o1 .
  ?s <urn:star:p:2> ?o2 .
}
```

The five-predicate query adds predicates 3 and 4 and projects all five objects. The VALUES variant prepends `VALUES ?s { ... }` containing every subject in the fixed shuffled order. At 12,500 subjects, these produce 100,000 rows per three-predicate query and 400,000 per five-predicate query.

The driver constructs tuple algebra and calls production `SailConnection.evaluate`. It uses `StrictEvaluationStrategyFactory`, STRICT evaluation, an empty optimizer pipeline, and explicit left-deep joins. It verifies the optimized explanation contains `JoinIterator` nodes with statement-pattern right children. This is a controlled nested-loop benchmark, not a SPARQL parser or default-optimizer benchmark.

Every result is consumed. Exact projected bags are checked against an independent Cartesian oracle before and after timing. Additional one-object projections check duplicate multiplicities without DISTINCT. All statements, all three physical index row counts, and results after reopening are checked. Plan files and JSONL output retain the evidence.

## Configuration

- `spoc,posc,ospc`; `forceSync=false`.
- Initial-run import, direct adjacency, value overlay, native evaluation, sketch estimation, page-cardinality estimation, and experimental query optimizations are disabled.
- Readahead is enabled. Initial triple/value maps are 1 GiB each, with automatic growth.
- Load and query transactions use `IsolationLevels.NONE`.
- Other maintenance settings retain each runtime's defaults. This is the original benchmark, not the later experimental maintenance-quiescence variant. Do not mix their timing results.

## Run

Use JDK 25 and the repository's Maven setup. The class lives in test sources and the benchmark packaging profile includes it. From the repository root:

```bash
bash scripts/run-single-benchmark.sh \
  --module core/sail/lmdb \
  --main-class org.eclipse.rdf4j.sail.lmdb.SubjectStarBenchmark \
  --jvm-arg -Xms512m --jvm-arg -Xmx1536m \
  --jvm-arg -XX:+UseG1GC --jvm-arg -XX:ActiveProcessorCount=2 \
  --jvm-arg --enable-native-access=ALL-UNNAMED \
  --main-arg main --main-arg /absolute/path/to/a-new-run \
  --main-arg 0 --main-arg 12500 --main-arg 3 --main-arg 5 \
  --main-arg false --main-arg ordered
```

The eight arguments are label, **nonexistent** run directory, fork number, subject count, warmup rounds, measured rounds, chunk-format boolean, and arrival order (`ordered` or `random`). Pass `false` for conventional main and `true` for original Ken's chunk footer format. The boolean affects only the untimed occupancy diagnostic, never query execution. Other encodings need a matching diagnostic.

For Ken, cherry-pick this benchmark-only commit into a separate checkout of the intended PR head, rebuild there, and pass `true`. Use separate fresh stores and record exact source/runtime identities. Repeat matched forks in alternating branch order. Keep data, indexes, JVM options, sync settings, and repetitions identical. Use `--no-build` only if the existing benchmark jar belongs to the intended checkout. A correctness smoke can use 3,125 subjects, zero warmup rounds, and one measured round; it is not warmed performance evidence.

## Results and limits

JSONL reports elapsed and foreground-thread CPU time, process CPU time, thread allocation, GC deltas, result checksums, exact bag checks, and index occupancy/payload/live pages. Each three-predicate timing sample executes four queries; each five-predicate sample executes one. Divide totals by the reported query count. Warmup and measured samples are labeled separately.

Logical file lengths are not filesystem allocated blocks. Whole-store files include the dictionary; per-index payload/pages do not. The OS page cache is not forcibly cleared. `forceSync=false` does not establish synchronous durability. Repeatable performance requires multiple matched forks, not one smoke run.

The original measured source SHA-256 is `301f77b0607237ebc8f3633046ea51535bc07dd9af737464ab397b190268ce16`. This copy adds the repository copyright/generated-code header and formatting, with unchanged benchmark semantics. Historical runs used original PR head `9d2bd38fd2f2a74be00c434358bc03e7a7725ae5` and develop `35fa723526812cd4030304f18f24898d55195d78`; those measurements are not automatically results for the newer main base.

Publication validation: the repository Spotless apply/check passed, and the formatted class token sequence matches the historical driver. Both pinned original Ken and develop runtimes passed a 3,125-subject smoke: all four plans, all exact bag checks, three 31,250-row indexes, and reopen validation. These are correctness checks, not fresh performance measurements on main.
