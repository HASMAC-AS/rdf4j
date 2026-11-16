# Prospective publication experiment

## Question and controls

The primary comparison is the runtime cost of publishing factor-cache entries for selected targets that show no successful factor hit in the fixture diagnostics. Both DENSE arms use the same compiled experimental materializer, original five-word coordinate transport, original dense factor pages, repaired ownership code, harness classes, input graph, JVM settings, and normal AUTO selection. The only arm difference is an experimental system property containing an explicit set of projection names. This is a fixture-specific causal intervention, not a planner policy or a production candidate.

The third arm disables coordinate transport but retains ordinary weak value reuse. It uses the same experimental materializer classpath and an empty publication exclusion property. It is a contemporaneous reference for the remaining coordinate cost. Prior timing observations are not pooled into this study.

The fixed arms are `off`, `dense-all`, and `dense-omit`. All six permutations occur once for each fixture. The fixture that runs first alternates by round. Each fresh JVM performs eight warmup queries and five measured queries, yielding 36 JVMs, 468 total queries, and 180 measured queries. The median of all five measurements is the per-JVM statistic. Ratios pair the same round and fixture, then report the median and full range of all six ratios. No late warmup selection, extra rounds, replacements, or favorable exclusions are allowed.

## Why the budget changes

The earlier original-page shared-tail diagnostic denied a factor-page admission near its 256 MiB per-query limit. Removing left-side publication at that limit would both avoid publication work and allow more right-side entries to survive admission. The corresponding resolver reduction would confound a claim that only publication overhead changed.

Every timed arm therefore explicitly sets `rdf4j.lmdb.queryMemory.perQueryMaxBytes=536870912`. The fixed heap remains 1 GiB, the process ledger remains 1 GiB, and the collector remains G1. Diagnostics must establish zero factor-page admission refusal and equal resolver counts across the two DENSE arms at this budget before the timing protocol can be frozen. The separate 256 MiB diagnostics describe the practical admission-relief effect without labeling it an isolated CPU saving.

## Target attribution

The shared-tail projection is `s,a,l0,l1,r0,r1`. The selected exclusion is precisely `l0,l1`, the two left columns whose pages produced zero successful factor hits in the prior diagnostic. Their positions are 2 and 3 in this specific projection. Publication must be attributed to canonical cache banks, and any aliases must be reconciled before choosing exclusions; requested target names alone do not establish canonical identity.

The fanout projection is `patient,measurement,reading,diagnosis,severity,drug,route,language,procedure,author,department,status`. Its exclusion is left unresolved until the new per-target diagnostic supplies evidence. The runner requires a nonempty distinct list drawn from these names. It does not infer a rule from projection order or transfer the shared-tail mask to fanout.

## Gates fixed before timing

The mechanism screen requires a median selective/all latency ratio at most 0.95 on each fixture, with all six paired latency ratios strictly below one. Five percent is the chosen useful engineering effect; agreement across all pairs limits reliance on one favorable ordering. These are engineering criteria, not statistical significance or a general noninferiority claim.

Allocation has separate criteria: each fixture's median selective/all all-thread allocation ratio must be at most one and no individual paired ratio may exceed 1.02. Every same-round query iteration, including all eight warmups, must preserve the DENSE-arm resolver count, observed route and tier, and adjacency lookup-hit count. Count/checksum and the exact shared-tail bag oracle must pass for every query. Ledger endpoints must be zero. Every measured query must execute compiled native IR through normal AUTO, and the final root audit must report no hosted generic root.

Passing these criteria supplies evidence that avoiding the diagnosed publication work warrants a generic-policy design. It never promotes the fixture name mask into production. Failing a criterion is retained as a result; the protocol is not extended to obtain a pass.

## Runtime preservation

`common-runtime-audit.json` verifies the preserved common runtime's 128 classpath entries. Its first entry contains the 15 shared harness/launcher/helper classes with tree SHA-256 `05c88f61336fce5028bb617e5ff7f7759e4a55ae4c4dba3a3b487c5512f0d825`. The following 127 entries match the original preserved runtime exactly. The selected production materializer source remains SHA-256 `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e`; the original production class tree contains 2,780 files with tree SHA-256 `f822ad378729e2ebdd5c7e91d2420e9dbf1eb92aab382e76c35fba00989eab8c`.

The experimental runtime consists of one materializer prefix followed by those exact 128 entries. No diagnostic helper classes are allowed in the timing prefix. All three arms share this same 129-entry classpath. The runner pins the generated source, its patch, its validation receipt, the runtime descriptor and manifest, the mask-selection receipt, and the diagnostic evidence files. It saves a copy of every descriptor input before launching any timed process and checks full runtime identity after the final process.

Only the small materializer prefix and the driver/protocol are rehashed between forks. No builds, tests, profiles, compression, or optional heavy hashing may overlap the timed interval. Process start/end records are retained and must form a serial nonoverlapping sequence. A failure stops the protocol and preserves its raw data; it does not trigger a replacement JVM.

## Tooling and verification

`run-comparison.py` freezes and runs the protocol. `runtime_identity.py` copies the prior read-only identity helper with its repository path adjusted for the new directory. `validator-selfcheck.py` verifies schema compatibility against explicitly synthetic copies of old records and rejects altered resolver counts or leaked ledger bytes. Its six positive schema checks and three negative checks passed in `validator-selfcheck-v1.json`. Those inputs are temporary, altered records and are not experiment results; the self-check launches no JVM.

Once the materializer runtime and diagnostics are verified, freeze a fresh protocol file from the repository root:

    python3 result-coordinate-evidence/publication-followup/benchmark/run-comparison.py --freeze --runtime /absolute/path/runtime-descriptor.json --selection /absolute/path/selection.json --protocol /absolute/path/protocol.json

Then check the complete frozen composition without running Java:

    python3 result-coordinate-evidence/publication-followup/benchmark/run-comparison.py --protocol /absolute/path/protocol.json --validate-only

After the exclusive experiment slot is released:

    python3 result-coordinate-evidence/publication-followup/benchmark/run-comparison.py --protocol /absolute/path/protocol.json --output /absolute/path/new-comparison-directory --slot-released

The root agent owns the timing slot, mask selection, semantic validation, independent analysis, and final interpretation. No Java process was launched in preparing this runner or this design note.
