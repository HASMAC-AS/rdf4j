# One-JVM controller review

**No blocking finding in the reviewed controller.** This was a bounded, read-only review before the fresh primary cohort, with no Java execution or timing-effect analysis.

The reviewed `supervision/run-step.py` has SHA256 `38faa745b6c9d7e13422ddb5b3956c53fc4cec216e00b4a3025e291d6f5fa145`. Its pinned protocol is `8140db1dafa30ff1121e56284837b52b7a395464f656a7f3eecf9eea8f0fcc84`, and its original driver is `2884c48422c85991930ceb0216b47aa182c5790b39f9e1fe824017db9e644c0f`.

## Command and progression checks

All **36 reconstructed commands** matched independent evaluation of the command expression extracted from the original driver's `execute()` AST. At the old cohort paths, all **eight preserved command receipts** also matched. This comparison inspected command identities only, without deriving outcomes from the interrupted cohort.

Twelve pure in-memory prefix checks passed: valid prefixes of lengths 0, 1, 7, 35, and 36, plus refusals for an unclaimed directory, a claim without its directory, a gapped first claim, an unprescribed directory, a later claim gap, an extra slot, and duplicate claim names.

Static review confirmed that a create-exclusive claim precedes launch. Every predecessor must have a normal completion receipt, all seven pinned raw files, its frozen command and working directory, a successful process result, replayed original qualification, and valid supervisor/process chronology. An incomplete claimed slot cannot be selected again. Missing, gapped, extra, or failed slots stop progression. The controller exposes no retry, replacement, skip, or import operation.

The original result, ownership, compilation, source-call, ledger, route, adjacency, and paired-query gates remain in force. Original comparison estimates require all 36 slots. Runtime-wide hashing occurs before and after the cohort; checks between forks run outside the serial JVM.

## Scope and coordinator obligations

The coordinator must observe **tool exit zero after every invocation** before starting the next one. The controller records that requirement but cannot authenticate the external tool's status. A completion receipt alone is insufficient authorization to continue when the tool outcome is unknown.

Separate invocations change administrative gaps. Those gaps are recorded without adaptive cooling, exclusions, or tuning, and the interrupted cohort contributes no data to accepted estimates. The supervision README was still being finalized when this bounded review completed; this receipt covers the pinned controller and checked commands, not an unseen final document.

The adjacent JSON records the exact checks and source identities. No benchmark file was edited.
