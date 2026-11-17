# Compiled query factorial: no established policy speedup

Experimental memo production `eef1d734`; all arms use identical frozen module classes and improved-hash compact snapshots plus both lifetime changes. Defaults remain initial/adaptive; compact remains default-off. Eight-million-row 30-slot compiled IR fixture, same options and query-thread checksum consumer as compact-snapshot-benchmark. Fresh JVM, no query warmup, 640 MiB per-query budget, no writer or HTTP/browser.

## Clean results (two fresh JVMs each)

| Admission / capacity | Query times s | Mean s | Mean first-ready-row ms |
|---|---|---:|---:|
| initial / adaptive (default) |40.399,36.697|38.548|933.953|
| always / adaptive |36.905,36.307|36.606|781.316|
| initial / fixed4096 |38.461,37.144|37.803|894.215|
| always / fixed4096 |36.062,37.444|36.753|923.824|

The apparent 5.04% mean reduction for always/adaptive is **not a demonstrated improvement**: only two forks, substantial baseline variance, overlapping observed ranges, and no meaningful admission mechanism difference in this high-reuse query. No statistical significance is claimed. First-result differences are also exploratory, not a confirmed improvement.

Adaptive arms each have approximately218,753,460 memo hits out of220,800,000 active probes. Fixed arms approximately217,910,407, about843,000 fewer hits. Continued admission cannot recover a late-hot phase here because initial admission already survives. Thus these timings do not establish that continuing admission causes the apparent difference. Allocation also varies with compiler behavior (one always/fixed run19.23GB, other24.61GB); allocation is diagnostic, not the goal.

All ten runs (including contamination controls) produced8,000,000rows, checksum-3441391656945048864, compiled irKernel, grouping+memo witnesses, no spill and zero remaining memory reservation. No source ownership or weak guards were relaxed. Main-thread allocation is not total process footprint. Existing reference/semantic/mask/budget tests pass; broader nondefault callback/cancellation/replay/map-growth validation remains pending in this checkpoint.

## Contamination and complete accounting

Intended order: initial/adaptive,always/fixed,always/adaptive,initial/fixed,initial/fixed,always/adaptive,always/fixed,initial/adaptive. A separate review build ran20:29:02–20:29:11UTC. Fork4 ended20:29:04UTC and fork5 began immediately afterward, so **both** original initial/fixed forks were marked contaminated, regardless of their measurements. Both were retained in query-measurements.json/qualification.txt and replaced at the end by forks9and10. This breaks perfect counterbalancing; report exploratory evidence rather than hide it. Replacement times38.461/37.144s are used above; original38.472/36.619s excluded uniformly. No other overlap was reported.

## Decision

Keep experimental switches/defaults unchanged. Mechanism tests show continued admission can recover98,240hits in a late-hot synthetic trace; fixed4096 preserves those hits at lower metadata cost than adaptive in that trace. Neither synthetic nor this actual compiled query establishes a dependable latency/throughput advantage. A future policy recommendation needs representative phase-changing real query evidence and more repetitions, not a claim derived from shell reuse counts. Preserve the alternatives to test interactions rather than discarding them solely for inconclusive isolated latency.
