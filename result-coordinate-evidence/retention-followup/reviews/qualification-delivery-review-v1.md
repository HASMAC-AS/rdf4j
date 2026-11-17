# Qualification and delivery review

Reviewed after all 36 primary JVMs completed on 2026-11-17. This review ran no Java, changed no Git state, and modified no frozen source, protocol, or existing archive. Timing estimates are outside this review's scope.

## Qualification totals

The final runtime has **193 passing test executions**, with zero failures, errors, or skips: 138 with the memo disabled and normal coordinate behavior; 47 with weak retention enabled and transport off; and eight with strong retention enabled and transport off. The weak selection comprises 19 applicable historical table contracts, 12 integration contracts, eight new retention contracts, and eight real-Sail lifetime cases spanning interpreted and compiled IR. The strong-only ownership assertion remains in the strong/default selection. These are execution counts, not 193 distinct behaviors. All three receipt hashes still match [runtime-v2/qualification.json](../runtime-v2/qualification.json).

Additional evidence is separate:

| Evidence | Verified scope |
| --- | --- |
| Fresh baseline | 106 passing tests, zero failures/errors/skips |
| Test-first reproduction | Smallest selection: one expected failure; full eight-test selection: five expected failures, zero errors/skips |
| Initial retention green | Eight passing tests on runtime-v1, before the final formatting-only runtime-v2 change |
| Actual ValueStore cache diagnostic | One passing test, independent of the memo overlay |
| Controlled memory diagnostic | Four fresh JVMs against runtime-v2, 24 passing checkpoints |
| Full weak-fixture qualification | Two fresh JVMs, 26 queries; excluded from comparative timing |
| Primary cohort | 36 fresh JVMs, 468 queries: 288 warmups and 180 measured observations |

The initially misconfigured `qualified-default-v1` attempt forced coordinate transport off and failed 38 of 138 assertions. It remains preserved. The accepted `qualified-default-v2` restores the default selection's appropriate coordinate behavior and passes all 138 cases; it is the receipt used by final qualification. The failed attempt is not an accepted gate. Sources: [baseline totals](../recovery/baseline-focused-v1/results.json), [validation receipts](../contracts/validation/qualified-default-v2/receipt.json), [weak receipt](../contracts/validation/qualified-weak-v1/receipt.json), [strong lifetime receipt](../contracts/validation/qualified-strong-lifetime-v1/receipt.json), [memory receipt](../diagnostics/memory-v1/receipt.json), and [pilot receipt](../qualification-v1/qualification.json).

**The full root reactor did not pass.** Both offline and online root attempts successfully built and installed LMDB. The reactor failed in unrelated Spring modules because `org.jetbrains.kotlin:kotlin-stdlib:2.3.21` was unavailable; the online retry encountered DNS resolution failure. Report successful LMDB/source-runtime compilation and focused qualification, not a successful complete repository build. Both outcomes remain in [offline build log](../recovery/root-build-offline-v1.log) and [online build log](../recovery/root-build-online-v1.log).

## Memory and lifecycle claims

The controlled diagnostic shows 64 initialized values and payloads retained by an open strong memo after consumer release, versus zero for weak/none and one for weak/one. Closing releases the memo roots; an intentionally retained value remains readable until its consumer releases it. The 64 payloads contain 128 MiB of distinct logical ASCII content. The observed process heap is a separate whole-process measurement, not a memo-exclusive retained-size estimate.

The retained consumer in that diagnostic is **one initialized `Value`, not a retained binding-set workload**. Detached rows, early close, cancellation, ownership handoff, and both IR tiers have correctness coverage in the integration/lifetime tests. This does not establish retained-row latency or heap behavior across arbitrary row widths, nested value graphs, consumption modes, or constrained heaps. The primary comparison fully consumes two fixed fixtures; it is not the requested broad repetition/width/uniqueness/consumption sweep.

Weak mode removes published strong memo paths to values, revisions, and stores. Its 606,448-byte initial reservation is a conservative metadata estimate under the pinned JVM layout assumptions, not a total query-heap bound. Strong mode still charges only arrays, initially 147,536 bytes, while retaining wrappers, initialized payloads, and revision graphs. Allocated garbage can also occupy heap until collected. Consumer roots, source caches, native memory, and other query state remain outside these memo estimates. Zero ledger endpoints establish reservation cleanup, not immediate disappearance of every result object.

The actual source-cache diagnostic confirms retention for decoded IRIs of 1,024 and 1,048,576 characters and exclusion/collection at 1,048,577 characters with default cache settings. Do not generalize this to a bound on every model or nested value graph. Source-owned retention can keep weak memo entries live independently of the memo. Source calls, actual dictionary decodes, allocation bytes, and retained heap are distinct measures.

`retainedEntriesAtClose` counts occupied metadata slots, including weak wrappers whose referents may have cleared. Cleared wrappers can survive resize as metadata. `collectedMisses` counts collection observed at probed slots; it is not a complete collection census. Neither counter should be presented as the number of live retained values.

The original admission problem remains: fewer than 25% cumulative hits at the first 8,192 probes permanently disables that iteration's memo. Repetition beginning afterward cannot revive it. Later growth uses cumulative history, so this is not a sliding-window or phase-adaptive policy. Weak collection can create additional misses. No mode guarantees one decode per distinct ID, and a new iteration starts a new memo. Source: [final memo implementation](../implementation-src/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemo.java).

## Delivery integrity and preservation

The source archive, patch, and external manifest remain available and match their original receipt hashes. All 59 payloads were re-read from the archive and verified; the four production files remain byte-identical to runtime-v2 and the canonical experiment sources. `git apply --check` again returned zero against current unmodified production files. The sealed original test remains unchanged; its separately included review copy differs only in whitespace. The source bundle requires the complete checkout/dependencies and does not claim to be a standalone distribution.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `complete-source-v1.tar.gz` | 592,009 | `9b324c4b55c68e45eefc60a899c957e672491d015dcf633ad4c49b933bd1aae7` |
| `complete-native-value-memo.patch` | 31,773 | `3e17b818d384dcacf8c36d507cc670b8ef8609b9e4f18e1ce59596929b217c9b` |
| `complete-source-v1.manifest.json` | 25,434 | `0af3bb256e4fd75dedbf5f308f01fdd8f68c3d0b83a0d4d2198336370de337bf` |
| `retention-v2-prefix.tar.gz` | 28,461 | `57fad979f4ec26e8e837d84a43ba356467801f9faf9dc6bcf2451d09d9c6fdea` |

All seven binary prefix members also match their receipt and the final runtime-v2 bytes. See [delivery verification](../artifacts/source-delivery-verification-v1.json), [source manifest](../artifacts/complete-source-v1.manifest.json), and [prefix receipt](../artifacts/prefix-receipt.json).

At review time there were no newly untracked eligible text-evidence files over the preservation helper's 128 KiB threshold. The 2,568,038-byte primary protocol is already listed in `recovery/preserved-large-v1/manifest.json`. The newly generated primary audit is 99,326 bytes; individual raw sample files are below 28 KiB. No new lossless evidence archive is required for these files, and no existing archive was modified.

## Read-only check of the final report

The qualification, memory, and delivery claims in [RESULTS.md](../RESULTS.md) agree with the inspected receipts and raw memory observations. In particular, the report correctly separates logical payload size from whole-process heap, describes one consumer-held value in the memory diagnostic, distinguishes source-cache ownership, retains the failed default-qualification attempt, discloses the full-reactor limitation, and avoids a standalone-runtime claim for the seven-class prefix. Its statement that all seven selected class files are byte-identical before and after the Javadoc formatting change is verified by the runtime-v1/v2 class maps.

One wording refinement was sent to the coordinator: the sentence “The current lifetime contracts cover correctness for these cases,” following early close, retained rows and constrained budgets, can overstate realistic constrained-memory query coverage. Lifetime tests establish selected early-close/retained-row/cancellation cases; budget-refusal coverage comes from controlled memo/ledger contracts. Suggested wording: “Existing lifetime and reservation-refusal contracts cover selected correctness cases; constrained-memory query behavior and performance still need a dedicated sweep.” No report source was edited by this review.
