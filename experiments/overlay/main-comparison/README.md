# Current-main comparison: work in progress

Investigation-only benchmark. No production changes.

Upstream `eclipse-rdf4j/rdf4j` main was fetched and pinned at `be0c4c724f582e22d1a2936b87bb24969cc9154a`. Experimental classes are frozen from local `b8104ecf51`, the final validated grouping code; byte-for-byte comparison with its class directory passed. The parent maps local checkpoints to GitHub commits with identical trees.

The main dependency reactor was built with its own Maven repository. The experimental classes and all dependencies were copied before execution; no run depends on later installs by other workers. Fixture stores are separate byte copies of the same 2,136,000-triple 8,000-group fixture. Every query fully consumes and string-hashes its selected RDF Values and asserts 8,000,000 rows, compiled IR and the forced irKernel route. The WHERE body and dataset remain identical between projection variants and revisions. The 4-column projection is `?s ?n0 ?n1 ?n2` and retains all 8 million distinct combinations. The 6-column projection additionally includes `?label ?seed`, two repeated literals. The wide projection contains all 30 bindings.

Memory parameters are identical, `-Xms2g -Xmx5g`, global query budget 2.5 GiB, per-query budget 640 MiB. These limits do **not** make memory enforcement identical: current-main DISTINCT stores >4-column keys in an uncharged HashMap and <=4-column keys in an uncharged primitive table. Experimental grouping uses bounded per-group state. We report spill counters, ledger usage, GC logs, actual heap samples and process maximum RSS, and do not equate ledger peak with all process memory.

The narrow comparisons use ABBA forks, one warmup followed by two measurements each. The wide comparison uses ABBA cold-query forks, zero warmups and one measurement each to match the preceding wide experiment. Startup overlay and direct-adjacency readiness are awaited before the query timer. Process elapsed/RSS cover startup too. Heap pool peaks are per-pool, not necessarily simultaneous; their sum is not a measured live-heap maximum. Allocation counts are only the query thread.

Narrow exact-validation runs are separate from timing comparisons. They check every tuple's expected group/member domain and mark its unique combination in an 8-million-bit bitmap, rejecting duplicates/missing combinations; the six-column validator also compares expected RDF literal values. The ordinary timed hash is not itself a proof of exact equality. Experimental-only witnesses verify memo hits and report producer grouping counts outside the timed region.

This is a synthetic workload. The result measures the combined experimental branch versus a newer main snapshot, not a clean attribution of each optimization. Four-column results primarily exercise IRI exposure rather than the full literal-heavy workload. No production merge recommendation or full-repository correctness claim follows.

## Completed four-column in-memory result

Two forks per revision, one warmup + two measurements each (ABBA):
- Main: 17.488, 18.048, 18.464, 18.400 s; mean **18.100 s**.
- Experimental: 16.310, 14.974, 15.464, 15.917 s; mean **15.666 s**.
- **13.45% lower elapsed time**, **22.09% less query-thread allocation** (16.789 GB to 13.081 GB).
- Every run produced 8,000,000 rows, checksum -7911808221325257696, compiled IR, and zero spill files. Experimental grouping fired once per query and memo hits were positive.
- Separate exact bitmap checks passed for both revisions: all expected group/member combinations exactly once. Their timing is excluded.
- Main process maximum RSS in measured forks: 2,670,288 / 2,625,492 KiB; experimental: 2,065,724 / 2,058,520 KiB. Main charged query ledger peak was zero, experimental 7,078,016 bytes. Main is not memory-bounded by that ledger.

The six-column and wide comparisons remain in progress. An initial pilot is excluded, and one brief compiler run occurred during the first experimental fork's warmup; measured samples ran after compilation completed. Repeat forks show the same direction.
