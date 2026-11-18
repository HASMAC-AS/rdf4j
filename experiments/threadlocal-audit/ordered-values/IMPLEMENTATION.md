# Ordered Workbench query values

1. completed: exact baseline families and test-first adapter ordering/fallback/hit proof.
2. completed: bounded query-only ordered-values bypass with complete getter and packing phases preserved; independent review.
3. completed: qualified source families independently over base and depth+serializer, integration tests and benchmark handoff.

## Design before production edits

Keep QueryEvaluator.addResult's complete ordered source getValue loop and privately owned reusable scratch list. Type that list as Value. A distinctly named package-private query-only TupleResultBuilder method receives it synchronously; generic public result(Object...) is unchanged. Try a package-private synchronized compact-writer positional route; if ineligible, call the exact existing result(values.toArray()) fallback. The borrowed list belongs exclusively to the evaluator invocation, is never exposed to callbacks or retained by writer/builder, and must not be accepted as a general public mutable-list API.

Eligibility: compact writer, valid unique/non-null header columns, list length matches header, and current builder columns match writer's copied columns in order. Builder.variables aliases the supplied names array; therefore recheck alignment each call rather than trust a cached flag. Uniqueness can be computed from the writer's owned header at start. This adds an O(width) comparison; performance remains empirical. Duplicate/null/misaligned columns, old page format, downloads and legacy writers retain the old map/array path.

Extract shared begin-row and commit-packed-row helpers without changing order: ensureOpen/started/empty header handling/dataStarted, then pack all values, then existing compactRows.add or rowBatch logic. New positional path executes under the same writer monitor. No direct BindingSet forwarding, no getter/packing interleaving, no lexical emission until packing completes, no retention of caller list. No batch/cap/flush/read-ahead or engine changes. Existing serializer methods must remain byte/source equivalent other than shifted line tables.

Baseline test matrix: recording source getter vs Value lexical callback order; late getter failure prevents all lexical work/output; source row mutation/reuse after synchronous return; unbound/reordered/extra bindings; duplicate and caller-mutated header fallback; legacy CSV and generic Object conversions; exact compact bytes; structural addResult-toArray expected red and generic fallback proof. Existing progress/disconnect/oversized/circuit-breaker/batch tests remain required. Baseline artifacts precede edits; unchanged actual HTTP families verified before timed overlay.

## Baseline and implementation evidence

Both independent pristine families (nine classes each) compile byte-for-byte identical to the actual base and qualified stack runtime, baseline-family-comparison.json. Eight initial new tests: seven semantic passes and one expected typed-route structural red, captured before production edits. Initial candidate passed those and 71 batch tests, but five existing evaluator tests caught inline mocks that retain the exact runtime class. The route now returns boolean success and the evaluator falls back unless true, preserving mocked and subclass result(...) dispatch. Corrected focused selection passed 124/124.

The final test expansion adds true positional-route stack hit proof, lexical-callback mutation of the original BindingSet, zero-width/deferred-real-header transitions, and null-name fallback. Its first StackWalker assert failed test compilation due BooleanSupplier overload inference; a formatting-sensitive replacement missed it once. Both failed compilation logs are preserved and count as no test execution. Primitive boolean local fixes that fixture. Final full module run completed: 1,574 passed, zero failures/errors/skips. Independent base+ordered126 and stack+ordered128 checks passed with hardened container and expected-count checks. See RESULTS.md for retained harness failures and their correction.

## Scratch-list ownership proof

There are three production scratch allocations: browser tuple batch, collected/paged tuple output, and evaluateTupleQuery. Each is a method-local ArrayList created by QueryEvaluator and only read/cleared/populated by its private addResult. After the complete getValue loop it passes the reference only through the concrete builder's package-private final attempt into the final page writer. No method stores the list or exposes it to Value/BindingSet callbacks. The writer constructs a separate packed output row synchronously and retains only its existing representation. A reentrant query creates its own scratch list. Subclasses never receive this route; they retain the original copied Object[] virtual result call. The new package-private borrowing contract forbids future callers from exposing/mutating the scratch during packing, rather than weakening a public mutable-list API.

The independent reviewer verified all these paths, alignment after source callbacks, and shared begin/commit ordering without a blocker. Existing typed serializer helper source block is unchanged byte-for-byte in stack candidate; independent base candidate has no typed serializer helper. Full source-family separation is retained for measurements.

Final sources/classes frozen and CPU released to benchmark worker. No subsequent production edits.
