# Independent review of the interrupted growth cohort

**Decision:** `comparison-v1` is permanently rejected. Its records are internally consistent with an interrupted 23rd slot. A new, empty `comparison-v2` may reuse the closed qualification, runtime and experimental design, subject to a new qualification-only audit before timing.

The machine-readable check is [interruption-rejection-v1.json](interruption-rejection-v1.json), SHA-256 `cbbd71d6a4cf0ecec8658a8a4f2857580dd6fe66384577ba7706a87b9c70066c`.

## Record consistency

The 23 claims are contiguous and match exactly the first 23 scheduled run directories and commands. The first 22 runs have successful process receipts, valid claim/protocol bindings, nonoverlapping receipt chronology and intact completion seals. All 176 hashes recorded by those completion seals matched. The interrupted slot is ordinal 22: round 2, early-cold-hot, ordinary. Its five preserved raw files match the hashes in `failure.json`; its sample stream contains metadata and four warmup records, with no completion record. Process, after-input, validation and completion receipts are absent for that slot. The runner lock was free at review.

The failure receipt hashes to `55361a959bc9d02332c3b66368c9c8fc4cb447b0cd5500d16483f7274a91e10c`. It correctly leaves the terminal exit code and root cause unknown. These records support rejection; they do not establish why the process-control session disappeared.

The frozen independent auditor was invoked only through its rejection path and returned `Cohort has a preserved failure; no accepted estimates` before any sample analysis. Source inspection confirms that the runner and primary analyzer also reject a preserved failure before continuation or accepted cohort estimates. No slot may be replaced in this cohort.

This review rehashed all 188 frozen manifest entries: 7,372 file checks, including repeated inputs, covering 344,166,308 bytes. The runtime, JDK, qualification and source inputs remain identical to the sealed protocol.

## Scope of the performance exclusion

No metric values were inspected and no cohort estimates were computed or used by this review. Completed `validation.json` files were read only as opaque hash inputs. The runner automatically wrote per-process medians and means into those files while sealing the successful runs. Therefore `performanceEstimatesComputed=false` in the failure receipt means **no accepted cohort/comparison estimates**; it does not assert that the runner never calculated its automatic per-process summaries. Those summaries remain preserved and must not influence the replacement design.

## Conditions for a fresh cohort

The frozen runner constructs the schedule deterministically from the same three arms, three fixtures and six permutations. Its freeze operation requires a previously nonexistent cohort directory and verifies the qualification against the classpath, runtime, fixture and JDK hashes. The prior qualification remains applicable because those inputs are unchanged; an interrupted timing process does not invalidate the already closed ownership and exact-result evidence.

The new protocol may differ only in its cohort path, derived per-slot GC/output/store paths and freeze timestamp; its digest will consequently change. The qualification, input identities, JVM arguments, environment, properties, fixture order, 54-slot schedule, 8+5 query sequence, timeout, estimators and exclusion rules must remain identical. All 54 JVMs must be fresh. No successful or partial run from `comparison-v1` may be pooled, substituted or resumed.

Before timing, compare the new protocol under precisely those path/timestamp substitutions and run the unchanged qualification-only auditor while the new claims directory and run set are empty. This review has not yet approved a concrete `comparison-v2` protocol.

No Java was launched and no frozen file was changed. Independent review by Codex.
