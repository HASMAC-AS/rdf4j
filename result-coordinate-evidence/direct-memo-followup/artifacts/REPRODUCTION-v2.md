# Reproducing the direct Value memo experiment

This document accompanies `direct-memo-complete-source-v2.tar.gz`. The archive contains the complete changed production and test sources, the original benchmark and validation code, and the finite supervision controller used for the completed comparison. It preserves every member of the first source archive byte-for-byte. `README.md` and `SOURCE-CONTENTS.json` remain the original archive's documentation and manifest; this document and `SOURCE-CONTENTS-v2.json` describe the additions.

The source archive contains no timing stores, JVM dependencies, compiled runtime, or raw timing observations. Source review and feature rebuilding require a full RDF4J checkout. Replaying the exact benchmark additionally requires the separately preserved runtime and evidence inputs described below. The supplied code is complete for the changed feature; the archive is not a standalone RDF4J distribution.

## 1. Verify the downloaded source

Extract into a new directory. Run this command from the extracted directory to verify the size and SHA-256 of every payload, including the preserved original manifest:

```bash
python3 - <<'PY'
import hashlib
import json
from pathlib import Path

manifest = json.loads(Path('SOURCE-CONTENTS-v2.json').read_text())
for name, expected in manifest['members'].items():
    data = Path(name).read_bytes()
    assert len(data) == expected['bytes'], name
    assert hashlib.sha256(data).hexdigest() == expected['sha256'], name
print('Verified', len(manifest['members']), 'payload files')
PY
```

The external `complete-source-v2-receipt.json` also records the compressed archive and every archive member, including `SOURCE-CONTENTS-v2.json` itself. The manifest excludes its own bytes to avoid a self-referential hash. The original archive and its original receipt remain separate preserved artifacts.

The production patch must have SHA-256:

```text
9109010da8d0dadde1c2fc7a8a29658ecf4d8226790e92ea86443a8a2332c797
```

It includes all four production files. `complete-production-source/` also contains their full contents and both new test classes at their intended repository paths. The patch's four reconstructed production files were compared byte-for-byte with the Java sources archived by the successful runtime compiler.

## 2. Review or build the feature in a separate checkout

The source baseline is `HASMAC-AS/rdf4j` commit `97284c5229fc826b156275b15f105aac4dd801d6`, tree `4db5b366c00c68f5e89c7301612b5a821fc0e5d0`. In a repository containing that commit, create a disposable worktree:

```bash
git worktree add --detach ../rdf4j-direct-memo-review 97284c5229fc826b156275b15f105aac4dd801d6
```

Set `direct_memo_source` to the absolute path of the extracted archive and `direct_memo_checkout` to the new checkout. Verify and apply the patch, then copy the two new tests:

```bash
direct_memo_source=/absolute/path/to/extracted-source
direct_memo_checkout=/absolute/path/to/rdf4j-direct-memo-review
git -C "$direct_memo_checkout" apply --check "$direct_memo_source/direct-memo-source.patch"
git -C "$direct_memo_checkout" apply "$direct_memo_source/direct-memo-source.patch"
cp "$direct_memo_source/complete-production-source/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemoContractTest.java" \
  "$direct_memo_checkout/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/"
cp "$direct_memo_source/complete-production-source/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemoIntegrationTest.java" \
  "$direct_memo_checkout/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/"
```

Use Java 25 or later, Maven, and the repository's `.m2_repo` dependencies. Follow the checkout's `AGENTS.md` build prerequisites. A source-level verification uses the root quick install before the targeted selection; it must not run concurrently with timing JVMs:

```bash
cd "$direct_memo_checkout"
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -Pquick clean install
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb \
  -Dtest=NativeDirectValueMemoContractTest,NativeDirectValueMemoIntegrationTest test
```

The local Maven repository must contain the required artifacts; if it does not, resolve the missing dependencies before treating this as a reproduction. An unrelated Spring/Kotlin dependency blocked the full reactor build in the original environment. The experimental candidate was instead compiled and qualified through the preserved LMDB runtime and isolated validation scripts. This archive does not claim a successful full reactor build.

The two new classes contain 20 table contracts and 12 integration cases. The qualified candidate also passed 106 existing cases with the memo disabled and eight existing real-query lifetime/bag cases with the memo enabled, including compiled and interpreted IR. Those additional sources belong to the selected full repository. Rebuilding sources against local dependencies is useful correctness verification, but it does not recreate the exact binary inputs of the timing study.

## 3. Restore the exact experimental runtime

The experiment left the selected production checkout unchanged. It compiled an isolated six-class prefix over the preserved common128 classpath. Restore the complete experiment checkpoint, its frozen dependency corpus, and its toolchain before using the sealed benchmark commands. The source archive alone cannot satisfy the runtime validators.

The required preserved inputs include:

- The qualified `result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2` directory, including its descriptor, compiled-source receipt, compile receipt, selected six classes, and validation references. The separately supplied `identity-witness-v2-prefix.tar.gz` contains the prefix, not all these dependencies.
- The unchanged common128 runtime under `result-coordinate-evidence/compact-page-followup/compiled-v1`, its fifteen harness classes, and the older dependency entries under `result-coordinate-evidence/frozen-current`.
- The prior source and validation evidence named by the descriptors, including the publication-focused baseline, direct table and integration red/green receipts, enabled lifetime selection, and six-process lifecycle evidence.
- The finite supervisor's preserved interruption audit at `result-coordinate-evidence/direct-memo-followup/review/interrupted-primary-v1.json`.
- Java `25.0.5+8` at `/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8/bin/java` and the repository at `/workspace/scratch/37853bcaea36/rdf4j`. Frozen commands and receipts include absolute paths.

Do not alter a frozen descriptor or expected digest to make a different local environment appear identical. A port to new paths, dependency bytes, or a different JVM needs a separately versioned design frozen before collecting its observations.

| Input | SHA-256 |
|---|---|
| Common128 runtime descriptor | `c6c46c4dec351666e404dddf9d374aaee619f742b6d6ba4adbde32dcbb27e815` |
| Qualified direct runtime descriptor | `cf4336e8ce853bc3f4a604f0f29b4e3c9acfcff4751ea0b028c78e71f98d8115` |
| Frozen main protocol | `8140db1dafa30ff1121e56284837b52b7a395464f656a7f3eecf9eea8f0fcc84` |
| Finite controller | `38faa745b6c9d7e13422ddb5b3956c53fc4cec216e00b4a3025e291d6f5fa145` |
| Finite supervision README | `8e656ccbee8a0555b9ebdcd44c7d46f22e5b4b47060c24722c151701e2c58b13` |

The selected compiled-attempt directory is `identity-witness-v2`. The descriptor's algorithm label remains `memoVariant: identity-witness-v1`. The failed preliminary compiled-attempt directory `identity-witness-v1` is not selected. This distinction also resolves the historical wording in the preserved protocol narrative; no frozen file was edited to produce this package.

The complete validation and compilation commands are in `result-coordinate-evidence/direct-memo-followup/contracts/VALIDATION.md`. Rebuilding a prefix requires newly named output directories, the sealed red evidence, all green selections, and the six full-fixture lifecycle runs before runtime freeze. The four-array diagnostic is intentionally invalid and cannot qualify a timing runtime.

## 4. Run the same bounded comparison

Run from the restored repository root. First validate the original protocol without launching Java:

```bash
python3 result-coordinate-evidence/direct-memo-followup/benchmark/run-comparison.py \
  --protocol result-coordinate-evidence/direct-memo-followup/benchmark/frozen-protocol-v1.json \
  --validate-only
```

Choose a fresh cohort directory, such as `comparison-reproduction-v1`. Never reuse the completed `comparison-v2` or the interrupted `comparison-v1`. Initialize the finite supervisor:

```bash
python3 result-coordinate-evidence/direct-memo-followup/supervision/run-step.py init \
  --cohort result-coordinate-evidence/direct-memo-followup/comparison-reproduction-v1
```

With the exclusive Java slot available, invoke this command once per coordinator/tool process:

```bash
python3 result-coordinate-evidence/direct-memo-followup/supervision/run-step.py next \
  --cohort result-coordinate-evidence/direct-memo-followup/comparison-reproduction-v1 \
  --slot-released
```

Observe that process's terminal exit code zero before issuing the next invocation. If the execution interface returns a live process/session identifier, poll that same process until it terminates; an outer orchestration response alone does not establish completion. Repeat through the fixed 36 slots. Do not put the invocations into another long-running controller. There is no retry, replacement, skipped slot, additional warmup, cooling delay, forced GC, or timing-based exclusion. A missing normal completion receipt or a failed gate invalidates the cohort. Keep builds, tests, profiling, compression, and heavy audits outside the timing sequence. All inter-process gaps remain recorded.

The schedule uses all six arm permutations, two fixtures, and eight warmups plus five observations per JVM: 468 queries, including 180 measured observations. The arms are ordinary weak reuse with transport off; ordinary plus dense factor reuse; and the direct strong memo with transport off. All arms share ownership code, the same compiled prefix, a fixed 1 GiB G1 heap, a 512 MiB query ledger, and normal AUTO selection. Fully consumed counts, checksums, routes, source identities, and cleanup qualify every query. Resolver differences remain outcomes and never normalize latency or allocation.

After all 36 finite invocations have completed successfully, audit the cohort, then run the independently frozen analyzer into a fresh output directory:

```bash
python3 result-coordinate-evidence/direct-memo-followup/supervision/run-step.py audit \
  --cohort result-coordinate-evidence/direct-memo-followup/comparison-reproduction-v1
python3 result-coordinate-evidence/direct-memo-followup/benchmark/analyze-timing-independent.py \
  --protocol result-coordinate-evidence/direct-memo-followup/benchmark/frozen-protocol-v1.json \
  --comparison result-coordinate-evidence/direct-memo-followup/comparison-reproduction-v1 \
  --expected-protocol-sha256 8140db1dafa30ff1121e56284837b52b7a395464f656a7f3eecf9eea8f0fcc84 \
  --output result-coordinate-evidence/direct-memo-followup/analysis-reproduction-v1 \
  --post-timing-slot-released
```

The supervisor verifies existing summaries; it does not repair a missing or interrupted result. The independent analyzer rereads raw outputs without importing the driver's estimates. Each JVM contributes the median of its five observations; each fixture has six same-round paired ratios. The prespecified engineering screen is separate for direct/ordinary and direct/dense, and is not a significance test. Historical measurements and the interrupted first cohort are never pooled with a new cohort.

## 5. Interpret the implementation's scope

The timed table adds a fifth identity-witness array to the original four-array proposal. The witness preserves invalidation when ID, revision number, or revision owner changes without a semantic-version change. The version-only diagnostic fails the three corresponding contracts. The timing variant retains the original direct-mapped replacement, cumulative adaptation, 4,096-to-65,536 capacity range, and permanent low-hit cutoff at the first 8,192 probes.

The memo is disabled by default. The direct arm explicitly sets `rdf4j.lmdb.nativeValueMemo.enabled=true` and `rdf4j.lmdb.resultCoordinateTransport=off`. Statistics count validated memo candidates; a standalone accessor can still call the source to preserve ownership handoff. Raw resolver counts therefore remain the measure of resolver work actually performed.

The ledger accounts for the five arrays and wide-projection scratch. It does not bound the wrappers, decoded lexical data, or revision capabilities held by strong entries. Lower allocation or a lower recorded ledger peak does not establish a lower retained-heap bound. The two repetitive, fully consumed fixtures do not establish performance for delayed reuse, long payloads, early close, retained rows, or constrained memory.
