# Natural collection and reuse through the real ValueStore

## Scope and verification

This is an untimed direct-memo diagnostic of ten fresh JVMs: strong/weak retention across two IRI sizes and zero/one intentional consumers, plus an ordinary source-only control for each size with no retained consumer. Each run reopens its seeded ValueStore, uses eight distinct noncolliding stored IDs, validates every lexical character, and issues 136 lookups over an initial read and 16 revisits. The retained sample is one Value at key index zero; these runs do not exercise retained query rows.

Every JVM used a fixed 128 MiB G1 heap. Each revisit followed 32 MiB of volatile-escaping byte-array payload allocation, for 512 MiB per run and 5 GiB over the cohort; array headers and all other allocation are additional. Default source-cache settings were preserved. The native map size was 128 MiB, separate from the Java heap. No explicit collection cause appears in any GC log.

The independent audit re-read the raw records and hashes, reconciled 170 rounds and 1,360 requests, and regenerated both payload-group checksums from primitive seed metadata. All ledger endpoints were zero and every source closed successfully. Corresponding seed IDs, serials, buckets, and checksums matched across the five runs at each payload size. All runs shared the captured runtime inputs and the same JVM command apart from arm, payload size, consumer, and output paths.

The machine-readable evidence is [audit.json](audit.json). No weak clearing count was required in advance.

## 1,024-character IRIs

| Arm | Consumer | Source calls | Cold completions | Same-prior returns | Memo collected misses | Original Values cleared before last revisit |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| ordinary | none | 136 | 8 | 128 | — | 0/8 |
| strong | none | 8 | 8 | 128 | 0 | 0/8 |
| strong | one | 8 | 8 | 128 | 0 | 0/8 |
| weak | none | 8 | 8 | 128 | 0 | 0/8 |
| weak | one | 8 | 8 | 128 | 0 | 0/8 |

With `none` retained consumers, weak recorded 0 additional source calls and 0 additional cold-to-initialized observations relative to strong in these two runs. Its memo recorded 0 collected same-key entries. No collision, admission cutoff, or resize participates in this 136-probe comparison.

With `one` retained consumers, weak recorded 0 additional source calls and 0 additional cold-to-initialized observations relative to strong in these two runs. Its memo recorded 0 collected same-key entries. No collision, admission cutoff, or resize participates in this 136-probe comparison.

## 1,048,577-character IRIs

| Arm | Consumer | Source calls | Cold completions | Same-prior returns | Memo collected misses | Original Values cleared before last revisit |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| ordinary | none | 136 | 136 | 0 | — | 8/8 |
| strong | none | 8 | 8 | 128 | 0 | 0/8 |
| strong | one | 8 | 8 | 128 | 0 | 0/8 |
| weak | none | 136 | 136 | 0 | 128 | 8/8 |
| weak | one | 120 | 120 | 16 | 112 | 7/8 |

With `none` retained consumers, weak recorded 128 additional source calls and 128 additional cold-to-initialized observations relative to strong in these two runs. Its memo recorded 128 collected same-key entries. No collision, admission cutoff, or resize participates in this 136-probe comparison.

With `one` retained consumers, weak recorded 112 additional source calls and 112 additional cold-to-initialized observations relative to strong in these two runs. Its memo recorded 112 collected same-key entries. No collision, admission cutoff, or resize participates in this 136-probe comparison.

## Interpretation

A cold completion means the returned native wrapper was observed uninitialized before lexical consumption and initialized afterward. It is not an instrumented count of physical LMDB decoding or storage reads. Source calls include both cold and already-initialized returns; the ordinary controls show why those counts must stay separate. Same-prior identity returns are checked against weak observers without loading earlier Values into new strong locals.

Because the eight memo buckets do not collide and the short trace never reaches an adaptation boundary, additional weak memo misses reconcile exactly to collected entries. Clearing masks observed before demand provide direct reachability evidence; more clearing can occur during later consumption. A non-cleared weak observer is not proof that an object would remain reachable under every collection schedule. An intentional retained consumer must keep its initial Value and payload alive; every one-consumer run checks that specific key throughout all revisits.

GC count/time deltas and whole-JVM heap snapshots include runtime and other roots. The GC log covers the entire JVM, including seeding, while recorded deltas begin after reopen and memo construction and extend through cleanup. They are not exclusive memo costs. The isolated memo ledger covers declared metadata; its zero endpoint does not assert that all source-cache or native memory was freed. These single-process cases are mechanism observations, not a query-latency estimate or a statistical comparison of collection schedules.

## Raw runs

| Case | GC count delta | Cleared canaries / 16 | Source returned initialized | Raw evidence |
| --- | ---: | ---: | ---: | --- |
| ordinary/1024/none | 10 | 10/16 | 128 | [records](../../natural-v1/ordinary-1024-none/stdout.jsonl) |
| strong/1024/none | 10 | 10/16 | 0 | [records](../../natural-v1/strong-1024-none/stdout.jsonl) |
| strong/1024/one | 10 | 10/16 | 0 | [records](../../natural-v1/strong-1024-one/stdout.jsonl) |
| weak/1024/none | 10 | 10/16 | 0 | [records](../../natural-v1/weak-1024-none/stdout.jsonl) |
| weak/1024/one | 10 | 10/16 | 0 | [records](../../natural-v1/weak-1024-one/stdout.jsonl) |
| ordinary/1048577/none | 31 | 0/16 | 0 | [records](../../natural-v1/ordinary-1048577-none/stdout.jsonl) |
| strong/1048577/none | 14 | 11/16 | 0 | [records](../../natural-v1/strong-1048577-none/stdout.jsonl) |
| strong/1048577/one | 14 | 11/16 | 0 | [records](../../natural-v1/strong-1048577-one/stdout.jsonl) |
| weak/1048577/none | 31 | 0/16 | 0 | [records](../../natural-v1/weak-1048577-none/stdout.jsonl) |
| weak/1048577/one | 30 | 0/16 | 0 | [records](../../natural-v1/weak-1048577-one/stdout.jsonl) |

All raw attempts are preserved. This audit launches no Java and changes no diagnostic source.
