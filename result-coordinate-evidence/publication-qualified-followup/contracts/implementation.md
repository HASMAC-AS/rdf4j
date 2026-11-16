# Rare accounting implementation and evidence

## Purpose and current status

The experiment now has a generated materializer observer that distinguishes real weak fallback outcomes without counting every successful binding, walking factor pages, or writing JSON during query execution. The source is ready for compilation and the unchanged fifteen contracts. Java execution remains owned by the coordinator's exclusive slot. Nothing in `core/` has been edited by this implementation.

The accepted preimplementation run is `validation/red-v1`. Its preserved Surefire report states:

    Tests run: 15, Failures: 14, Errors: 0, Skipped: 0, Time elapsed: 0.380 s

All fourteen failures are the expected `qualified-observer:` assertions. Result identity, independent resolver-call lists, required weak collection, and ledger-release assertions precede them and passed. The disabled-observer case already passed. The exact test source remains SHA-256 `a4c8bef7fc5455e2c04eab3cc21aca021f9df9caf62555e6443b87f391f528f6`.

The next step is to compile the generated source with the agreed wrapper, then run the same fifteen tests and the existing materializer/publication selection. No successful postimplementation test result is claimed here.

## Source generation

The generation command is:

    python3 result-coordinate-evidence/publication-qualified-followup/contracts/build_accounting_overlay.py --red-validation result-coordinate-evidence/publication-qualified-followup/contracts/validation/red-v1 --output result-coordinate-evidence/publication-qualified-followup/contracts/generated/rare-accounting-v1

The generator checks the raw red XML, all fifteen case identities, failure descriptions, the archived sealed test source, the failing process receipt, and the class map showing that red used the original publication overlay. It accepts only the exact prior materializer SHA-256 `25a321a3f9cb590492dc9d4efe0a2022ae4f731b6d90db38c5137a4ee1c21fb1`. Outputs are additive and confined to a new directory under `contracts/`.

Generation produced materializer SHA-256 `038d604694758374cb66422e8bbdf79853862ab400db7f3962a34187f84749c7` and helper SHA-256 `741df48cf276991e66f08a3157751e2119505948eb7685b374ca0d600ae29320`. The generated directory contains complete source, exact preimage, an isolated-source patch, and `source-receipt.json` with all file identities and evidence links.

Ten uniquely matched transformations reverse exactly to the preimage. The generator separately verifies that the Plan/import prefix, ordinary publication method, factor publication method, coordinate capture method, and complete nested FactorMemo/FactorPage source bodies are unchanged. Materializer `.get()` sites remain fourteen and weak-reference construction sites remain seven. It adds no imports, page traversal, coordinate sampling, weak dereference, or strong reference field.

The compiler should retain the existing Plan and nested class files in the timing runtime. The emitted Plan should match its prior bytes because its complete source prefix and line positions are unchanged. Freshly emitted nested classes can differ in debug line numbers because inserted observer lines shift their source locations; those emitted copies are not the retained timing classes. Source-region identity and exact retained-runtime byte identity are the applicable checks.

## Recorded events

The materializer adds eleven primitive event counters and one primitive owner ordinal. On an ordinary weak failure it clears the ordinary entry as before, saves the per-instance factor-weak count, performs exactly one factor lookup, classifies the returned value, and returns that same value. The existing materializer monitor serializes this work. An ordinary weak failure can recover, end without a factor weak failure, or overlap a factor weak failure. The factor weak counter increments only in the existing dereference-null branch that immediately clears the factor cell and returns null.

The exact weak partition is `ordinaryWeak = ordinaryWeakRecovered + ordinaryWeakTerminalOther + jointWeakTerminal`. The terminal weak union is `factorWeakTerminal + ordinaryWeakTerminalOther`, with `jointWeakTerminal <= factorWeakTerminal`. These are descriptions of observed lookup paths. They do not establish a counterfactual resolver cost, and no latency correction or resolver-minus-weak acceptance gate is justified by them.

Other counters record the existing ordinary and factor witness-rejection branches, calls to `abandon`, and actual ordinary, factor-page, and factor-memo admission refusals. Factor-page refusal counts once at each failed `admitFactorPage` return, including failure of its directory reservation. It does not count ordinary page absence. Ordinary allocation and memo failures retain their existing refusal latches, so repeated rows do not invent repeated admission attempts. Normal setup, successful hits, nonweak lookup misses, and publication success receive no new event writes.

`abandon` exposes mid-capture scope repair or failure rollback without editing `NativeProjectedBindingSet`. Normal scope setup and coordinate activation changes occur elsewhere. As with the per-instance ordinary/factor classification, export is scoped to the serial, non-reentrant native benchmark fixtures. It is not an API for classifying arbitrary late callbacks or reentrant custom source implementations.

## Export and memory

`PublicationQualifiedAccounting.initialize(13)` preallocates primitive storage before the unchanged launcher begins. Each owner constructor records only an ordinal and the existing cumulative semantic-compile and lazy-value counters. The materializer exports its scalar counters after its original successful cleanup, within the existing idempotent-close guard. The helper records the corresponding closing cumulative counters. It does not receive a Value, source, projection plan, row, page, or coordinate owner.

Normal owner open and close perform no allocation or I/O. Snapshot objects are constructed only when the wrapper asks for them after the original launcher returns. The original harness timing and allocation windows remain untouched. Four existing atomic-counter reads occur per observed owner: two at open and two at close. There are no new per-binding atomic operations.

The materializer has twelve additional `long` fields: 96 bytes of field payload before object layout and alignment. The normal buffer stores sixteen long words for each of thirteen owners: 1,664 bytes of long-array payload, plus thirteen boolean completion slots, array headers, and helper state. That bounded primitive buffer is allocated before timing. The extra object fields participate in the measured materializer allocation. Both additions are common to every experiment arm and sit outside the unchanged query-ledger claims. These payload figures are source-layout accounting, not a measured total heap-size claim.

Overflow increments once per owner that has no buffer slot. The owner still executes and closes; opened and closed totals remain visible. The wrapper/analyzer must reject any overflow, missing owner, incomplete export, or invalid lifecycle alignment. A full buffer never silently manufactures a valid result. `initialize(0)` disables recording for the existing tests, and initialization refuses to replace an active observed run.

## Validation still required

Static generation and Python syntax checks passed. The generated Java line widths and required headers passed inspection. The source receipt confirms the original production source and prior evidence were not modified. Postimplementation Java contracts, existing focused coverage, exact compiled-prefix checks, lifecycle preflight, and the prospective timing study are still required before drawing performance conclusions.
