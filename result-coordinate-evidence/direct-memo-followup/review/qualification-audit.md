# Independent lifecycle qualification audit

**Qualified:** all six lifecycle JVMs, 78 queries, and 26 direct memo owners passed independent raw-record checks. No timing summaries or comparisons were computed. This cohort is not pooled with the primary experiment.

## Checks

The audit used Python standard-library parsing and hashing. It imported no experiment driver, analyzer, builder, or identity helper. It established results from raw commands, process results, samples, root audits, and owner records before comparing them with the existing lifecycle receipt. No Java was launched and no source or tool was edited.

- Exact scheduled commands, source identity, JVM settings, full-size inputs, and full consumption.
- Six successful, non-overlapping JVMs; eight warmups and five measurement-stage queries each.
- All 78 full row counts, checksums, binding-read counts, normal AUTO routes, active adjacency revisions, and zero ledger endpoints.
- All 39 shared-tail queries passed the exact-bag oracle. All 30 measurement-stage queries used compiled native IR.
- Every JVM reported 13 semantic native compiles, zero generic roots, and zero hard declines.
- Direct owners aligned with compile ordinals 1 through 13. Resolver intervals started at zero, had no gaps, and matched every original per-query source-call counter. Controls exported no direct owners.
- All 52 direct/comparator query pairs matched full route/tier and adjacency observations, including warmups. Cross-treatment resolver equality was not required.

## Exact direct memo observations

Each row below is the identical event pattern observed in all 13 owners for that fixture. These are lifecycle/accounting observations, not performance estimates.

| Fixture | Probes | Hits | Collisions | Source calls | Entries before close | Capacity | Peak array charge |
|---|---:|---:|---:|---:|---:|---:|---:|
| fanout | 13,824,000 | 13,790,614 | 32,405 | 33,386 | 981 | 4,096 | 147,536 bytes |
| shared-tail | 6,291,456 | 6,119,366 | 167,994 | 172,090 | 4,096 | 4,096 | 147,536 bytes |

Every direct owner had zero invalidations, unsupported admissions, scope clears, resizes, rehash discards, low-hit disables, initial or growth refusals, and array bytes after close. Each table remained active at capacity 4,096 until final close. Its declared five-array charge reconciles with that capacity and remains below the query-ledger peak.

For these full-capture fixtures, probes equal binding reads, probes minus hits equal raw resolver calls, and probes minus hits minus collisions equal final occupied entries. These exact conservation checks supplement the ordinary policy invariants. They do not establish that standalone candidate hits always avoid source calls, and final totals cannot reconstruct every historical adaptive decision.

## Results and identity

| Fixture | Queries | Rows per query | Checksum |
|---|---:|---:|---|
| Fanout | 39 | 1,152,000 | `3123987218479850048` |
| Shared tail | 39 | 1,048,576 | `8327963502356915200` |

The compilation source identity was recomputed from all five archived Java inputs. The audit checked all six actual prefix class files, all fifteen unchanged common-harness class files, and five retained plan/nested classes. Exact classpath order and the 129-entry manifest matched. The complete historical dependency byte walk was not repeated; that remains a separate runtime-freezer responsibility.

- Preflight protocol SHA-256: `d4dfb2a44c4c4e8f25c44def6d439c00fbfcf949b438038ae54f381a78deb9d0`.
- Compile receipt SHA-256: `2356f8e4bdfc921dbe46f50b3203940c3d6ee53b0ff7708fe0c6aacb17d750c9`.
- Recomputed compilation-source digest: `5863fe02555484d19edb0d5f0a71c4b6fff8eabfe0d520d2d0d55d9705e412b4`.

The current protocol document differs from the previously supplied documentation hash only by changing its command example from the `identity-witness-v1` directory to `identity-witness-v2`. The executable driver and independent analyzer retained their final supplied hashes. The audit records the actual versions and leaves the earlier source-review snapshot intact.

`qualification-audit.json` pins every raw file, identity receipts, exact owner/compiler/resolver intervals, and the direct event patterns. Array accounting still excludes retained wrappers, payloads, and revision graphs. This audit supplies lifecycle qualification only; the separate 36-JVM primary run is required for a runtime conclusion.
