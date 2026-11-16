# Finite supervision for the fresh comparison-v2 cohort

The first comparison lost its long-running controller during its eighth JVM. Its cause is undetermined. Seven completed JVMs and the partial eighth attempt remain preserved and are excluded from the new cohort. No timing estimate from that attempt selects or modifies the new experiment.

This controller changes process supervision only. It uses the unchanged frozen protocol SHA-256 `8140db1dafa30ff1121e56284837b52b7a395464f656a7f3eecf9eea8f0fcc84`, its original Java arguments, all 36 scheduled slots, eight warmups and five measurements per JVM, the 300-second per-process deadline, qualification gates, summary implementation, and independent analyzer.

The selected compiled runtime is **identity-witness-v2**, whose descriptor SHA-256 is `cf4336e8ce853bc3f4a604f0f29b4e3c9acfcff4751ea0b028c78e71f98d8115`. The failed preliminary runtime directory `identity-witness-v1` is not selected. The descriptor's separate algorithm label remains `memoVariant: identity-witness-v1`, as required by the frozen protocol. The compiled-attempt directory and algorithm label are distinct. Frozen files remain unchanged.

## Operation

`init` requires a fresh output directory, fully validates the original runtime, copies the original protocol and identity evidence, and creates a separate prospective `supervision.json`. This file pins the finite controller, this document, the exact 36 commands, and the interrupted-cohort audit. Initialization launches no JVM.

Each `next` invocation acquires a nonblocking cohort lock and launches exactly one next JVM. Before launch it verifies small pinned files, the six-class prefix, and every prior slot's original command, successful terminal process receipt, all thirteen query records, root audit, direct-memo records, raw hashes, and normal supervisor completion receipt. It replays the original result, route, metadata, ownership, and pair gates. It does not trust a mutable cursor to select a slot.

A durable slot claim is created before launching its JVM. Every claim must belong to the exact contiguous scheduled prefix and have its matching directory. A missing normal completion receipt, partial or failed output, missing predecessor, extra directory, extra claim, or failed gate blocks the cohort. A completed JVM without its controller's final receipt is still an interrupted attempt. No command retries or replaces it. There is no skip, resume, repair, or replacement option.

After a successful JVM, the controller writes the ordinary validation and derived `runs.json` and `summary.json`, then durably writes `supervisor-completion.json` and exits. On the final slot it also performs the full immutable-runtime preservation check, after Java has exited, before accepting the original complete summary. Earlier summaries contain no accepted comparison estimates.

The coordinator must separately observe the tool's **terminal exit code zero** before calling `next` again. The script cannot authenticate that external observation itself. If `exec_command` returns a live `session_id`, continue polling that session with `write_stdin` until its terminal `exit_code` is returned. Completion of a surrounding orchestration cell is not the same event. Loss of the session or a nonzero tool exit stops the cohort for investigation; do not infer successful completion from a progress line.

`audit` launches no JVM. It replays all normal completion receipts and raw evidence and verifies the derived files against the original summary implementation. It does not reconstruct or rewrite missing or damaged summaries. A complete audit also repeats the final runtime-preservation check. The original independent analyzer remains unchanged and runs after the full cohort and supervision audit complete.

## Timing and hashing

No cooling, sleep, pause, or altered warmup policy is introduced. The coordinator invokes the next finite process after observing success and performing normal supervision. Step timestamps and the exact interval from one JVM's end to the next JVM's start are recorded. Tool boundaries can change the intervals between JVMs; those intervals remain visible and never determine exclusions, extensions, arm order, or favorable sampling. The two cohorts are never pooled.

Full classpath jars and production class trees are hashed at initialization and final completion only. Small source/descriptor files, the experimental prefix, and preceding raw records are checked between JVMs. No source review, build, test, profiling, compression, or heavy audit runs concurrently with a timing JVM.

## Commands

Run from the repository root. All paths below are new supervision or comparison-v2 artifacts; the original frozen files remain untouched.

    python3 result-coordinate-evidence/direct-memo-followup/supervision/run-step.py init \
      --cohort result-coordinate-evidence/direct-memo-followup/comparison-v2

After initialization, run this command once for each next slot, observing its terminal exit code before issuing it again:

    python3 result-coordinate-evidence/direct-memo-followup/supervision/run-step.py next \
      --cohort result-coordinate-evidence/direct-memo-followup/comparison-v2 \
      --slot-released

Do not put the 36 invocations into another long-running shell or Python controller. The coordinator owns the sequence across tool calls.

After all 36 invocations have succeeded:

    python3 result-coordinate-evidence/direct-memo-followup/supervision/run-step.py audit \
      --cohort result-coordinate-evidence/direct-memo-followup/comparison-v2

The original independent analysis command then uses the original protocol and its original hash, pointing `--comparison` at `comparison-v2`. The additional supervision manifest and terminal receipts qualify the changed process supervision separately from the unchanged measurement and analysis protocol.
