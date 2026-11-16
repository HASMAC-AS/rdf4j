# Compact factor-page experiment

This is a new experiment phase. It preserves every prior artifact, the repaired ownership implementation, and original frame handling. The only candidate production source is `NativeResultMaterializer.java`, with a 512-byte inline page shell and a separately charged 15,872-byte dense payload on promotion. The root owns production edits and correctness validation; this directory owns the common harness, prospective protocol and immutable runtime tooling.

## Approved design before data

Run current OFF, current DENSE and compact-page DENSE on both fixtures. Each fixture gets all six arm permutations; each fresh JVM runs eight warmups and all five measured queries. Alternate which fixture starts each round, keeping each fixture's three-arm block together. Total:36fresh JVMs,288warmups,180measurements,468queries. Use normal AUTO, native evaluation/direct-adjacency PREFER, Java25.0.5+8, a1GiB fixed heap/G1, a1GiB global query authority and256MiB per-query allowance. No profiling or competing builds/tests/compression/optional hashing during timing.

The fanout fixture is unchanged at M100/R10/width12/two statuses,1,152,000rows and checksum3123987218479850048. The shared-tail fixture is unchanged at64roots/128left/128right/two columns per tail,1,048,576rows and checksum8327963502356915200. Its independent exact oracle checks bound IRI types, tuple and parent relationships, legal unique row keys, count and checksum. Oracle working storage is allocated before timing; complete result consumption/checking remains inside timing.

One common128-entry runtime prefix is used across every arm and fixture. The prefix copies frozen fanout/helper and shared-tail fixture/oracle/launcher classes, replacing only the shared-tail main harness classes to add startup maintenance waiting and outside-window adjacency snapshots. The original127base entries are unchanged except candidate productionentry1 (combinedindex2). The candidate contains no page-occupancy instrumentation.

## Fixed engineering acceptance screen

Promotion requires every gate below, comparing compact DENSE with current DENSE within the same fixture and matched round:

- Every shared-tail peak-ledger ratio must be **<=0.25**.
- Every shared-tail all-thread allocation ratio must be **<1**.
- Each fixture's median of six paired total-latency ratios must be **<=1**.
- No individual total-latency ratio on either fixture may exceed **1.10**.

This is a prospective engineering screen for a memory optimization, not statistical noninferiority. Report every pair and failed gate. A separate consistent-latency-win claim requires all six latency ratios below one on that fixture. OFF remains the ordinary-reuse control; report compact/OFF and current-DENSE/OFF separately. Do not pool fixtures or older runs. No post-data tolerances, extra rounds, exclusions or favorable-tail selections are allowed. A failed candidate is preserved in the evidence; the root restores only its production source.

## Preparation and compilation

From the repository root, `prepare-harness.py` creates source copies and their exact patch/provenance without running Java. This has already been done; it refuses to overwrite its outputs.

After the root grants a compiler slot:

```bash
python3 result-coordinate-evidence/compact-page-followup/compile-harness.py \
  --slot-released \
  --output result-coordinate-evidence/compact-page-followup/compiled-v1
```

The compiler preserves exact fanout, fixture/oracle and launcher class bytes. The original oracle's independently recorded12legal-row/seven-invalid-bag checks remain tied to its unchanged classes. Runtime/freezer helpers verify all preserved dependency identities; no benchmark JVM runs during compilation.

## Candidate freezing after the matching correctness gate

The root must supply exact measured production preimages and a passing matching validation record whose source snapshot equals the current module sources:

```bash
python3 result-coordinate-evidence/compact-page-followup/freeze-candidate-runtime.py \
  --name compact-page \
  --allow-production-path core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java \
  --validation-record PASSED_GATE/results.json \
  --preimages result-coordinate-evidence/compact-page-followup/baseline/preimages
```

`PASSED_GATE` must name the real successful source-matched gate. The helper freezes complete production classes, modified source, exact diff, source maps, validation evidence and all dependency hashes. It permits only the declared source's outer/inner classes and explicitly co-declared `NativeResultMaterializerPlan.class`; all other production bytes must remain exact. Independent review then identifies the exact production patch SHA-256.

Freeze the final benchmark protocol:

```bash
python3 result-coordinate-evidence/compact-page-followup/run-comparison.py \
  --protocol result-coordinate-evidence/compact-page-followup/protocol-draft.json \
  --freeze-candidate result-coordinate-evidence/compact-page-followup/runtimes/compact-page/runtime-descriptor.json \
  --frozen-protocol result-coordinate-evidence/compact-page-followup/protocol.json \
  --reviewed-production-patch-sha256 REVIEWED_SHA256
```

Replace `REVIEWED_SHA256` only with the actual independently reviewed hash. Freezing validates the complete runtime and approved prospective design, with no JVM launched. `--validate-only` on the resulting frozen protocol checks it without execution.

## Serial execution after an exclusive slot grant

```bash
python3 result-coordinate-evidence/compact-page-followup/run-comparison.py \
  --protocol result-coordinate-evidence/compact-page-followup/protocol.json \
  --output result-coordinate-evidence/compact-page-followup/comparison-v1 \
  --slot-released
```

All output paths must be new. The runner retains exact commands, complete raw JSONL, consoles, stores, process status, root audits, per-fork validation and consumed runtime identities. Before every fork it checks the driver/helpers/common harness. After the study it checks all complete runtime identities again.

Each query must pass its result oracle, zero ledger endpoints, expected coordinate admission and one native compilation. Actual adjacency lookup hits must be positive, with ACTIVE and stable applied/current revisions before/after. All measured queries must use normal AUTO compiled IR and match the first qualified route/tier histogram within their fixture. A post-main outer-root audit must reconcile13native compilations and zero hosted-generic roots. Lookup hits establish query-time use, including possible planning/probe operations; they do not mean every output row used adjacency.

Any failed result, process, route or identity stops the protocol while preserving its raw evidence. A partial study produces no accepted final comparison or promotion estimate. When complete, use every five-query JVM median, every within-round ratio and the complete range of six ratios. Report cold preparation and all warmups separately from warmed estimates. Ledger peaks represent reservation accounting, not whole JVM heap or the separate direct-adjacency memory budget.
