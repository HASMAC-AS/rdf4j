# Admission history changes attainable reuse

## Finding

The controlled cyclic traces expose two separate limitations. The initial cutoff can reject a stream that would soon become useful, and removing that cutoff can still leave a larger working set unable to earn the hits required for growth. An initial hot phase can change the later capacity and reuse substantially, even when retention, hashing, the cyclic tail, and all source semantics are unchanged.

These are observations of the real direct-mapped memo with synthetic sequential native IDs, a fresh uncached source, and intentionally live consumer roots. They are not query latency, allocation, or natural-GC results. The source-derived reference model below reconciles the complete observed counters and checkpoint states; it does not establish that a particular real query has the same key distribution.

Raw evidence is under [traces-v1](traces-v1). The reproducible [audit script](analysis/audit_traces.py) produced [trace-audit-v1.json](analysis/trace-audit-v1.json). The prospective design and trace definitions are in [contracts/DESIGN.md](../contracts/DESIGN.md) and [the diagnostic README](README.md).

## Verification

All three JVMs—ordinary, weak/initial, and weak/always—finished with exit code zero, without timeout. Each stdout contained exactly ten JSON trace records after non-JSON Logback startup lines were excluded. No trace or process was replaced.

The independent audit verified 30 records representing 3,932,160 checked requests. All ten corresponding input cases had matching result checksums across the three arms. Every record satisfied `sourceCalls = requests - hits`; observed initialization counts equaled source calls for this deliberately fresh and fully consumed source. All query and global ledger endpoints were zero. All 20 enabled traces recorded zero collected-entry encounters, semantic/source invalidations, unsupported values, rehash losses, entry clears, and reservation refusals. Live consumer roots explain why collection cannot account for these differences; this does not mean the JVM performed no GC.

The Python reference simulation reproduced all 510 checkpoint states and every emitted memo counter. It uses the preserved full-key hash, the documented cutoff/growth rules, and live references. All five bootstrapped initial/always pairs were identical in their complete JSON records after the arm name was removed.

The audit also verified 15 raw-file hashes and 7,295 captured input files totaling 304,537,213 bytes, including the selected Java executable, module image, and release file. The common input-manifest SHA-256 was `cf86953c5fc70555612803107aa78afb8da524f7d8eb64ef41dc4a7e22bca4c2`. Recorded commands matched apart from the arm argument and GC-log destination. Each process used the same Temurin Java 25.0.5+8 installation, a fixed 1 GiB G1 heap, eight-byte object alignment, and compressed object/class pointers. The memo's isolated test ledger was 16 MiB.

To reproduce this audit against the preserved files, use a new output path:

    python3 result-coordinate-evidence/admission-followup/diagnostics/analysis/audit_traces.py \
      --raw result-coordinate-evidence/admission-followup/diagnostics/traces-v1 \
      --output <new-audit-output.json>

The script refuses to overwrite an existing output, launches no Java process, and reports an assertion failure if any required record, hash, identity, cleanup endpoint, or reference-model state differs.

## Cold cyclic streams

Each cold stream contains 131,072 requests cycling over W distinct full `(ID, OBJECT-mask)` keys. The ordinary arm makes 131,072 source calls and has no memo reservation in every case.

| W | Initial hits | Initial source calls | Initial state at end | Always hits | Always source calls | Always final slots |
| ---: | ---: | ---: | --- | ---: | ---: | ---: |
| 64 | 131,008 | 64 | Enabled, 4,096 slots | 131,008 | 64 | 4,096 |
| 4,096 | 1,515 | 129,557 | Disabled at 8,192 | 106,410 | 24,662 | 65,536 |
| 8,192 | 0 | 131,072 | Disabled at 8,192 | 16,950 | 114,122 | 4,096 |
| 16,384 | 0 | 131,072 | Disabled at 8,192 | 2,051 | 129,021 | 4,096 |
| 65,536 | 0 | 131,072 | Disabled at 8,192 | 0 | 131,072 | 4,096 |

At W=4,096, the first logical repeat is request 4,097 and the first actual hit is request 4,098. Repetition therefore begins well before the cutoff, yet only 1,515 of the first 8,192 requests hit: 18.4937%, below the required 25%. Initial admission permanently disables the table. Always admission continues, accumulates 4,545 hits by request 16,384 (27.7405%), and becomes eligible to grow. It then reaches 65,536 slots and finishes with 106,410 hits. Removing the cutoff recovered 104,895 additional hits and avoided the same number of source calls in this finite trace.

The larger cold streams show why removing the cutoff is insufficient by itself. At W=8,192, always admission gets 16,950 hits, or 12.9318% of requests, and remains at 4,096 slots. At W=16,384 it gets 2,051 hits, or 1.5648%, and also never grows. W=65,536 produces no hits at all: always admission continues lookup and replacement work without reducing source calls in this trace. Collision pressure is high, but the cumulative hit gate prevents a larger table.

## The same cyclic tails after a hot bootstrap

The bootstrapped cases first request one disjoint key 8,192 times, producing 8,191 hits. The remaining 122,880 requests cycle over the same IDs 1 through W as the cold cases. Initial and always then behave identically because both passed the sole disabling check. Tail hit rates below exclude all bootstrap hits.

| W | Tail hits / 122,880 | Tail hit rate | Total source calls | Final slots | Peak metadata reservation |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 64 | 122,816 | 99.9479% | 65 | 4,096 | 606,448 B |
| 4,096 | 103,380 | 84.1309% | 19,501 | 65,536 | 14,549,472 B |
| 8,192 | 90,001 | 73.2430% | 32,880 | 65,536 | 14,549,472 B |
| 16,384 | 71,173 | 57.9207% | 51,708 | 65,536 | 14,549,472 B |
| 65,536 | 893 | 0.7267% | 121,988 | 16,384 | 3,637,728 B |

W=8,192 illustrates the history dependence. With no bootstrap, always admission finishes at 4,096 slots and 12.9318% hits. With the hot bootstrap, it grows to 65,536 and achieves 73.2430% in the tail. Those percentages cover 131,072 and 122,880 cyclic requests respectively. At an equal 122,880-request cyclic horizon, the cold always trace has 15,820 hits (12.8743%), versus 90,001 after the bootstrap. This is a conditional policy-history comparison; the bootstrap itself remains an additional input phase with its own work and hits.

The W=65,536 bootstrap has a particularly sharp growth constraint. It grows twice, to 16,384 slots, before any cyclic-tail repeat. At request 32,768, it still has only the bootstrap's 8,191 hits—one short of the 8,192 required for another growth. It never recovers the cumulative hit threshold within this run. Its cyclic tail has one complete cycle and 57,344 requests of the next cycle, so its reported hit rate is a finite startup observation rather than a steady state.

## Capacity and accounting checkpoints

The following are actual observed transitions. A request number here is a logical diagnostic request; after cutoff, the memo's own probe counter remains at 8,192.

| Trace family | Capacity transitions |
| --- | --- |
| Cold initial, W=4,096/8,192/16,384/65,536 | 4,096 slots at request 1; disabled and reservation released at 8,192. |
| Cold always W=4,096; bootstrapped initial/always W=4,096/8,192/16,384 | 4,096 at request 1; 8,192 at 16,384; 16,384 at 24,576; 32,768 at 32,768; 65,536 at 40,960. |
| Bootstrapped initial/always W=65,536 | 4,096 at request 1; 8,192 at 16,384; 16,384 at 24,576; no later growth. |
| All remaining enabled cases | Stay at 4,096 slots. |

The weak table reserves 606,448 bytes at 4,096 slots, 1,212,656 at 8,192, 2,425,072 at 16,384, 4,849,904 at 32,768, and 9,699,568 at 65,536. Resize admission holds the old and replacement reservations together, so a run reaching 65,536 has a 14,549,472-byte peak. The smaller two-growth peak is 3,637,728 bytes. Every reservation was released on cutoff or close.

These charges are table metadata estimates under the pinned layout assumptions. The diagnostic deliberately holds returned Values as consumers, outside this reservation. Neither metadata charge nor source-call reduction is a measurement of live query heap, total allocation, or CPU cost.

## Implication for the next comparison

This finite trace study gives a concrete reason to measure the initial-cutoff ablation end to end: a repeating 4,096-key stream was rejected even though continued admission later produced substantial reuse. It also supplies a negative case: a 65,536-key cold stream gained no hits while always admission kept doing table work.

Capacity cannot be inferred from working-set size. Its adaptation history must be reported, and a favorable bootstrapped case cannot stand in for the corresponding cold query. Keep the existing growth rule fixed during the first full-query admission comparison, record its transitions, and use separate natural-GC/source-cache diagnostics for collection effects. These count results do not yet select a production admission policy or demonstrate a latency improvement.
