# GQL / LMDB adjacency benchmark

Measured on 2026-11-09 with JMH 1.37, Eclipse Adoptium JDK 25.0.5+8, Linux amd64, one thread, one fork, `-Xms512m -Xmx2g`. Each case has three 2-second warmup iterations and five 2-second measurement iterations. Maven compilation was paused during the measurements.

## What this measures

The benchmark evaluates and closes a prepared two-hop count query. Loading, parsing, adjacency construction, and an initial correctness query occur before timing. Runtime query optimization/evaluation follows the repository's normal prepared-query pipeline. The LMDB result cache is disabled.

The fixture contains one root, 32 intermediate nodes, and 32 shared leaf nodes: 65 nodes and 1,056 RDF statements. Every intermediate node reaches all 32 leaves, so every case must return exactly 1,024 paths. All statements occupy `urn:gql:benchmark:graph`; all endpoints are IRIs and every predicate is `urn:gql:benchmark:edge`.

Native evaluation is enabled in every case. `DISABLED` disables direct adjacency; `PREFER` builds the existing in-memory adjacency indexes over LMDB and permits their normal guarded use. `NAMED` explicitly selects the fixture context. `MERGED` uses the repository's merged default graph, including GQL's required per-edge deduplication. The fixture has no repeated triples across contexts, so GQL and SPARQL return the same result while retaining their respective language semantics.

Every setup verified the exact count, and all eight cases completed. The reported adjacency counters exclude the initial correctness query and include warmup and measurement evaluations.

## Results

The uncertainty column is JMH's 99.9% confidence margin, in the same units as the mean. These short runs have broad, overlapping intervals and visible warmup effects; they do not establish an adjacency speedup or a general comparison of language performance. Positive lookup counters establish that GQL actually used adjacency in both graph modes. The named-graph GQL case also obtained adjacency views for generated Janino kernels; the merged-graph GQL case did not.

| Language | Adjacency mode | Graph | Mean (µs/op) | 99.9% margin (µs/op) | Adjacency lookup hits | Generated-kernel views |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| GQL | DISABLED | MERGED | 1685.764 | ±809.111 | 0 | 0 |
| SPARQL | DISABLED | MERGED | 583.285 | ±276.200 | 0 | 0 |
| GQL | DISABLED | NAMED | 1423.288 | ±623.127 | 0 | 0 |
| SPARQL | DISABLED | NAMED | 809.598 | ±844.822 | 0 | 0 |
| GQL | PREFER | MERGED | 1600.881 | ±833.395 | 288,396 | 0 |
| SPARQL | PREFER | MERGED | 587.796 | ±263.431 | 289,817 | 41,420 |
| GQL | PREFER | NAMED | 1457.838 | ±694.830 | 117,545 | 16,924 |
| SPARQL | PREFER | NAMED | 618.740 | ±369.365 | 119,062 | 39,770 |

These counters distinguish direct adjacency lookups from generated-kernel specialization. A zero generated-kernel counter does not imply that the adjacency indexes were bypassed. The narrow `Distinct(StatementPattern)` / filtered-statement planner test separately verifies native row-plan support without a generic statement island. Merged-graph edge deduplication is retained for correctness.

## Queries

GQL merged graph (the named case prepends `USE GRAPH ` followed by the backtick-delimited fixture graph IRI):

```gql
MATCH (a)-[:`urn:gql:benchmark:edge`]->(middle)-[:`urn:gql:benchmark:edge`]->(leaf)
WHERE a = $start
RETURN COUNT(*) AS total
```

SPARQL merged graph (the named case places the two triple patterns inside `GRAPH <urn:gql:benchmark:graph> { ... }`):

```sparql
SELECT (COUNT(*) AS ?total) WHERE {
  ?a <urn:gql:benchmark:edge> ?middle .
  ?middle <urn:gql:benchmark:edge> ?leaf
  FILTER (?a = ?start)
}
```

Both prepared queries bind `start` to `urn:gql:benchmark:root` through `Query.setBinding`.

## Reproduction

Use the repository's supported benchmark helper with JDK 25 and the repository's Maven configuration:

```bash
scripts/run-single-benchmark.sh \
  --module core/sail/lmdb \
  --class org.eclipse.rdf4j.sail.lmdb.benchmark.GqlAdjacencyQueryBenchmark \
  --method countPaths --param fanout=32 \
  --warmup-iterations 3 --measurement-iterations 5 --forks 1 \
  --jmh-arg -w --jmh-arg 2s \
  --jmh-arg -r --jmh-arg 2s \
  --jmh-arg -rf --jmh-arg json \
  --jmh-arg -rff --jmh-arg validation-gql/gql-adjacency-jmh.json
```

This run used the same command with `--no-build`, after packaging the LMDB benchmark module and refreshing the installed parser dependency with `jar:jar assembly:single@benchmark-jar`. The executable JAR's manifest, LMDB classes, benchmark class, and byte-for-byte compiler match against the installed parser were checked before running.

The benchmark class also supplies fanout 4 and 128 parameters for future measurements; those cases were not run here. Parser source-size and `MOD` fixes made after the measured parser snapshot do not affect this query's parsing or execution path. The final correctness suite is run against the final installed snapshot separately.

## Evidence

The committed `results.json` contains all eight measurements and their iteration data. `adjacency-counters.txt` records engagement for each case. Additional local logs below were retained in the delivery archive.

- `results.json`: complete JMH measurements, uncertainty, and per-iteration raw data.
- `gql-adjacency-jmh-raw.log`: complete runtime output, including adjacency counters.
- `gql-adjacency-jmh.log`: compact output from the supported helper.
- `gql-lmdb-benchmark-parser-sha256.txt`: exact measured parser JAR hash.
- `gql-lmdb-benchmark-uberjar-sha256.txt`: exact measured executable benchmark JAR hash.
- `gql-lmdb-benchmark-refresh-executable.log`: successful final executable assembly.

Measured parser SHA-256:

```text
1fa97fb9e74dfcb4de1170dc7a4cbd0f0e5754500cb7d0ece79824213a844ba8  .m2_repo/org/eclipse/rdf4j/rdf4j-queryparser-gql/6.2.0-SNAPSHOT/rdf4j-queryparser-gql-6.2.0-SNAPSHOT.jar
```

Measured executable SHA-256:

```text
288fbd103c9766a397223f74307beb5920b5158db17b3ef0d682c77c72229e7c  core/sail/lmdb/target/jmh-benchmarks.jar
```
