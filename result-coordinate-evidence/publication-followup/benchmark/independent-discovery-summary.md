# Independent raw discovery reconciliation

The stdlib-only `check-discovery-raw.py` independently read the eight owner JSONL files, both harness sample JSONL files, both root audits, and the frozen suite protocol. It imports no diagnostic analyzer or experiment runner. Only after calculating the result did it compare its mask decisions with `candidate-masks.json`.

All eight queries pass the independent checks. The result is preserved in `independent-discovery-original512.json`, SHA-256 `2864deb4101dca49d0cbc728bfbd5b104d3b5067be9bab87b9c1436cc0b93751`, with fourteen pinned input paths and hashes. No Java process was launched for this analysis.

## The selected targets are reproduced exactly

The prospective rule requires positive page admissions and completed publications, zero successful factor hits, zero factor weak misses, and zero factor witness misses in every one of the four queries, after canonical-bank aggregation. Both fixtures have identity alias mappings, so requested and canonical indices coincide in these runs.

| Fixture | Independently selected canonical targets | Publication calls on selected targets per query | Selected page charge | Selected / all pages |
|---|---|---:|---:|---:|
| Fanout | `measurement`, `reading` | 2,304,000 | 1,671,168 B | 102 / 124 |
| Shared-tail | `l0`, `l1` | 2,097,152 | 136,314,880 B | 8,320 / 16,640 |

These are diagnostic operation and reservation counts, not predicted CPU savings. The publication call count includes calls that find an unchanged cache tuple and therefore perform no page-cell write.

| Target | Ordinary hits | Successful factor hits | Completed publications | Actual publication writes | Pages admitted |
|---|---:|---:|---:|---:|---:|
| `measurement` | 1,151,900 | 0 | 1,152,000 | 100 | 2 |
| `reading` | 1,151,000 | 0 | 1,152,000 | 1,000 | 100 |
| `l0` | 1,040,384 | 0 | 1,048,576 | 8,192 | 128 |
| `l1` | 1,040,384 | 0 | 1,048,576 | 8,192 | 8,192 |

Every figure in these tables repeats in all four queries for its fixture. All admitted pages remain represented in the final page table, and every admitted page and granted page byte has an observed explicit release. No page admission is refused at the 512 MiB per-query limit. There are no weak or witness misses.

## Independent traffic and result checks

| Fixture | Binding reads | Ordinary hits | Factor hits | Resolver calls | Peak query ledger |
|---|---:|---:|---:|---:|---:|
| Fanout | 13,824,000 | 11,053,899 | 2,768,954 | 1,147 | 2,264,968 B |
| Shared-tail | 6,291,456 | 4,177,855 | 2,080,768 | 32,833 | 272,948,260 B |

For every query, ordinary hits plus factor hits plus resolver calls equal binding reads; materializer finds also equal binding reads, and remember calls equal resolver calls. Publication calls equal ordinary hits plus remember calls. Every local outcome partition also reconciles. These equations are qualified by the observed fully bound, nonaliased fixtures; they are not asserted as general invariants of all projections.

Every result has the prescribed row count and checksum. Shared-tail also passes the independent exact-bag oracle. Every query uses normal AUTO native IR and active query-time adjacency; each observed query is compiled native IR, and root audits record four native compiles with no hosted generic root. Query ledger endpoints are zero.

Ordinary, directory, and memo reservation amounts remain close-time snapshots. The independent report distinguishes their snapshots and any other query claims from explicit lifetime page grant/release accounting. It does not extend the page accounting claim to uninstrumented reservation classes.

## Historical budget evidence, with its limits

`compare-historical-budget-counters.py` separately reread all four historical original-page shared-tail owner records and harness samples at 256 MiB. `independent-historical-budget-counters.json` preserves that comparison and its input hashes. The old and new diagnostics use the same fixture identity and original dense representation, but different helper instrumentation; this is corroborating counter evidence, not the definitive same-overlay budget experiment.

| Shared-tail quantity | Historical 256 MiB | Fresh 512 MiB | Difference |
|---|---:|---:|---:|
| Ordinary hits | 4,177,855 | 4,177,855 | 0 |
| Factor hits | 2,048,256 | 2,080,768 | +32,512 |
| Resolver calls | 65,345 | 32,833 | −32,512 |
| Pages admitted | 16,364 | 16,640 | +276 |
| Page admission refusals | 1 | 0 | −1 |
| Page charge | 268,107,776 B | 272,629,760 B | +4,521,984 B |
| Peak ledger | 268,426,276 B | 272,948,260 B | +4,521,984 B |

All four query pairs show these same counts. The relationship supports the decision to run the publication CPU comparison at 512 MiB: at 256 MiB, page admission and resolver work can change alongside publication work. No duration or allocation measurement from either diagnostic is compared.

## What remains deliberately outside the mask

The rule selects banks that actually admitted pages. `patient` in fanout and `s,a` in shared-tail also have zero factor hits, but they admit no pages and complete no publications. Their calls stop during coordinate acquisition. They therefore fail the predeclared positive-admission/completion condition and remain enabled in this experiment. The mask has not been broadened after seeing these counters.

The observations establish only the recorded full-consumption executions. Retained results, early close, other query shapes, aliases, restored frames, changed strategies, or later replay may alter the value of an entry. The following timing comparison must show whether this specific publication removal earns a meaningful latency saving while preserving resolver counts and routes; the diagnostic alone cannot answer that question.
