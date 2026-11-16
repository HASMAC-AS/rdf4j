# Independent static review of the resolver-variation probe

## Decision

No blocking defect found in the reviewed driver and protocol. This is a separate bounded diagnostic after the primary timing study failed. Its completion cannot rescue the primary study or yield accepted latency, allocation, GC-cost, or miss-rate comparisons.

## Reviewed inputs

| File, relative to the publication-followup directory | SHA-256 |
| --- | --- |
| `diagnostic/resolver-variation/run_resolver_variation.py` | `ac1f446f5e9a37d52140bccc0a949d41ca9fbb98cee258965d27efd153b2d5e4` |
| `diagnostic/resolver-variation/protocol.md` | `1667f6b7d788308ebdcf5c09f0225c5643cd6e7e3619890f115c65af2411c5c7` |
| `diagnostic/resolver-variation/investigation-note.md` | `9ea2c4db46c24223c0464f487138bcc90a311fc3d6a796e99205f1ce005c5a5f` |
| `diagnostic/run_diagnostic_v2.py` | `d037db6bd17adc8f428fbd3136fc07b318dba4745c31e49370e5951c95dae4c5` |

Review was static: no Java, compilation, test, benchmark, or probe process was started by this reviewer. The existing diagnostic instrumentation and its weak-reference behavior were reviewed previously in `diagnostic-review.md`; this driver introduces no new Java source or class.

## Scope and invariants

The schedule is fixed to four fresh JVMs in all/omit/omit/all order. Each keeps eight warmup queries and five subsequent observations, for exactly 52 records. Every command uses the same frozen 130-entry diagnostic classpath, DENSE coordinates, the 64 by 128 by 128 shared-tail fixture, original pages, full consumption, normal AUTO, a fixed 1 GiB G1 heap/global ledger, and a 512 MiB query ledger. The arm-specific property is empty or `l0,l1`; all commands add the same unrotated GC/reference logging configuration with distinct fresh output paths.

Preparation writes a new plan and full command arrays without launching Java. Runtime/source hashes, generated helper/preimage hashes, classpath content, imported Python sources, and all four prior owner records per arm are checked and pinned. Execution checks the frozen input set before each command and after the final command, rejects injected JVM option environment variables, requires a coordinator slot flag, and creates the run tree exclusively. Root remains responsible for the actual exclusive slot and external protocol-hash freeze; a command-line flag is not an operating-system lock.

All 13 queries per JVM must preserve expected rows/checksum, independent exact-bag and membership checks, projected names, identity aliases, six bound reads per row, native IR and normal AUTO selection, current adjacency revision, zero hosted generic roots, and zero harness query-ledger endpoints. All five later observations require the compiled tier. Resolver counts, weak/witness events, admission counts, and peak claims are observed rather than forced to the earlier four-query reference. The existing page grant/release and lookup/publication/admission partitions remain required.

Under the checked no-alias and full-read conditions, the driver requires both aggregate hit-plus-resolver reconciliation and each target's terminal-factor-miss sum to equal its remember count. Ordinary outcomes are retained separately from terminal factor outcomes, so an ordinary weak miss recovered by a factor hit is not labeled an extra resolver. Refusal events are not conflated with missing pages. Initial factor rejection remains a combined category because the unchanged instrumentation cannot distinguish its subcauses.

## Preservation and interpretation limits

Process commands and start/end records, raw samples, owner JSONL files, root audits, console output, and GC logs remain on disk. A timeout or process/validation failure stops the fixed schedule and is captured in a partial summary without replacement. Validated-prefix counts are labeled as such; a failing run can retain additional raw records outside that count. The independent post-run audit will inventory the raw directories rather than treating the summary as an input oracle.

GC output is JVM-wide and cannot be aligned to a particular query or referent. The helper itself changes allocation and close-time work differently between all and omit. The protocol explicitly forbids deriving natural miss rates, GC-frequency/cost rankings, or latency/allocation comparisons. A terminal weak branch in the probe identifies that branch in the new instrumented observation, not the cause of the earlier uninstrumented timing event. Non-reproduction leaves that earlier event unresolved.

The primary timing protocol remains failed and incomplete at nine JVMs/117 queries, with no accepted ratios or performance gate evaluation. This probe has no replacement or extension path.
