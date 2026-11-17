# Experimental compact identity-preserving transport mapping

Default-off flag: `rdf4j.http.compactIdentityMapping=true`.

## Change

An internal query-module capability lets compact snapshots map values without exposing a binding iterator on the identity path. BNodeOriginTransport uses it only with the flag enabled and only for implementing rows. Generic rows use the original loop. The compact implementation visits all actual bindings, not a projected-name list; preserves Value identity; allocates an independent shallow map only for changed values; and falls back to ordinary map traversal for already-materialized rows.

Callback-triggered materialization does not restart mapping. Later bindings come from the current map, while the current pre-callback Binding is preserved for comparison. Structural modifications are checked before the next original binding, matching LinkedHashMap iteration, including no post-last-callback check. Public iterator, mutable name-view and serialization behavior are unchanged.

## Verification

- Maven reactor selection: core/sail/api and core/http/client with dependencies; Java25. Focused classes CompactIdentityMappingTest, CompactBindingSetSnapshotTest, BNodeOriginTransportTest, BNodeOriginTransportStreamingTest.
- 44 tests passed:13 new callback/mutation parity tests,19 existing compact snapshot tests,12 existing blank-node transport/streaming tests. Both experiment flags enabled.
- Standalone invocation of all13 new tests with assertions disabled passed, including null-binding cases.
- Actual compact snapshot plus BNodeOriginTransport integration passed with the dispatch flag OFF and ON: identity/callback-order, stored blank-node and nested RDF-star triple roundtrip, retained values, independent changed rows. Reflection verified OFF materializes the compact map and ON leaves it compact for identity mapping.
- Frozen query-classes, api-classes and http-client-classes are isolated under this directory for paired HTTP benchmarking. Both arms must use the same frozen outputs and differ only in the dispatch property.
- No full-repository or full HTTP module test claim. These tests do not establish throughput improvement; matched Workbench timings are separate.

The first test compile caught a missing test-only Resource cast; it was corrected before the successful run. No production correctness failure was observed in this focused qualification.
