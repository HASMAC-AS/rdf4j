# Hot/cold memo split: compiler evidence

The completed `hot-split-profile.json` reports `witnessesPassed: true`, and its compilation XML has the completion marker. This is an instrumented diagnostic, not a performance comparison.

## Local inline goal succeeded, but the surrounding inline boundary moved

Final split collector C2 compile **12144** hot-inlines:
- `NativeValueMemo.value` at collector BCI116;
- memo `slot` and `findCachedValue`;
- `Reference.get`, with `_Reference_get0` intrinsic.

It does not inline `cacheMiss` (`already compiled into a medium method`) or `finishSample` (`too big`). The miss-source call is still monomorphically specialized to guarded direct `SyntheticValueSource.lazyValue`, with `too big` refusal.

However, split repository `LmdbStoreConnection$1.next` C2 compile **11841** refuses to inline the collector with **`already compiled into a big method`**, leaving a direct call. No later repository-next compilation is present in this log. In the unchanged previous-best diagnostic, repository next **11717** hot-inlined the collector but refused the memo (`hot method too big`). The split therefore does not eliminate the complete boundary chain; it changes which boundary is out-of-line.

| HotSpot metric | Previous best | Split |
| --- | ---: | ---: |
| Memo entry bytecode | 474 | 181 |
| Final standalone memo task_done nmsize | 4,144 | 2,168 |
| Final collector task_done nmsize | 2,560 | 5,408 |
| Final collector nmethod total size | 2,816 | 5,664 |
| Final collector inlined_bytes | 142 | 687 |

Final split standalone memo is compile12137. Cold/helper bodies also have their own compiled forms, so standalone entry size is not total footprint. These sizes and refusals are code-shape facts, not a causal demonstration of a timing regression. Parent's separate matched timings found no gain; this diagnosis provides a plausible reason to avoid equating a successful local inline with end-to-end improvement.

## Source-equivalence review

Compared `memo-hot-split/NativeValueMemo.before.java` with `NativeValueMemo.candidate.java`. No concrete behavior change found beyond helper stack frames/compiler shape:
- A successful old hit necessarily has a nonnull LmdbValue, so returning null for failed lookup preserves the hit/miss branch.
- Returned value remains a local strong reference across testHitObserver; observer remains after counters and before sampling, including if it clears or recursively calls the memo.
- lazyValue, semantic-version check, initial reservation, slot recalculation, occupied accounting and table writes preserve their order.
- Failed initial reservation still returns the value before sampling.
- Sampling conditions and reset order are unchanged; weak-reference and version guards are unchanged.

This is a read-only review, not a substitute for parent-run correctness tests.

## Evidence

`summarize-hot-split.py` reads the completed XML and emits `hot-split-compilation-summary.json`, including relevant C2 inline decisions across compilation roots. No production code, frozen artifacts, queries or builds were changed/run by this analysis worker.
