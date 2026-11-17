# Experimental transport identity mapping

Hypothesis: an identity-preserving row mapping capability allows BNode-origin transport to scan compact values without forcing a mutable map representation. The full HTTP profile showed this specific materialization caller, rather than assuming allocation savings translate to latency.

## Safety gate

Keep generic BindingSet mapping unchanged. New capability is internal and default-off (`rdf4j.http.compactIdentityMapping`). Preserve one callback per visited binding, iteration order, reference identity, changed-value shallow-copy behavior, mutable custom Binding behavior, exceptions, structural modification timing, reentrancy, RDF-star recursion and blank-node origin negotiation. The row remains fully prepared at the repository boundary; this is not a lazy-value escape relaxation.

## Matched test protocol

1. Freeze query/API/HTTP-client modules; compare flag off/on using exactly these same frozen modules. Keep improved-hash compact API, lifetime LMDB and pinned writer fca5653 constant.
2. Exact-value actual HTTP smoke for both arms (100 emitted, all 8M counted). Explicit route/no-spill/budget witnesses.
3. Fresh-server ABBA with 1M emitted/8M counted. No heavy build/test work concurrently; shared benchmark flock. Compare terminal completion, throughput, first received rows and window latency separately. No browser-paint claim.
4. Separate diagnostic JFR for candidate; do not mix its elapsed time into unprofiled means. Check the expected materialization caller is eliminated and identify remaining costs.
5. Report any negative result; no allocation-only performance claim, no main-comparison claim, no release/merge qualification.

## Status

Completed: source safety qualification, exact HTTP smokes, matched ABBA and diagnostic ON profile. See QUALIFICATION.md and RESULTS.md. Candidate remains default-off.
