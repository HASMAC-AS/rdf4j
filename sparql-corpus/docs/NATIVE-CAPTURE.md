# Native assertion capture and port boundaries

## Source snapshots

The corpus is derived from HASMAC-AS/jena at `df0d523eed42cd0b7c9ef11c74eeb0a63302a423` and HASMAC-AS/qlever at `a815b3b71a6c764d927332dcd5505fe6436ad214`. Each generated case retains its original source body, file and line, source hash, query text, fixture requirements and expected assertion. Original vendor files are not rewritten.

The imported catalogue is not an exhaustive proof of upstream coverage. It includes duplicate source instances in bundled historical collections, source-only inventory entries, extension-dependent cases and explicitly blocked cases. The native capture audit records every selected method invocation, including methods that produce no executable case. Capturing a helper invocation does not certify that all other assertions in the enclosing native method were translated.

## Jena: compile the helper callers rather than guessing Java expressions

`tools/capture/SourceTransformer.java` uses the JDK compiler's syntax tree to locate methods, original source lines and assertion helper calls. It emits transformed copies under `target/native-capture`, leaving the source snapshot untouched. Known terminal helper bodies are replaced by `Capture` calls. Higher-level helpers and argument expressions retain their original Java implementation, including string concatenation, Unicode escapes, arithmetic constants and repeated calls.

The resulting classes run only for extraction. They record the original expected arguments, not actual Jena or RDF4J query results. When a source expectation is itself an expression, only that explicitly designated expected expression is evaluated. Expected-exception wrappers are recorded as metadata, not reported as successful original Jena test executions. Uncaptured exceptions, unsupported predicate lambdas and incomplete invocations remain audit gaps or blocked records.

The accepted comparison modes are exact RDF term, value equality with datatype equality, the original floating-point delta check, source-specific type predicates, and expression error yielding one unbound solution mapping. The original floating-point helper accepts either sign of infinity when the expectation is infinite; the port preserves that behavior instead of silently strengthening the assertion. Exact lexical comparisons may be stricter than what a portable SPARQL implementation must guarantee. Those tests are valuable compatibility checks, but differences are not automatically conformance failures.

`jena-comparator-calibration.json` contains a synthetic 26-by-26 matrix obtained from the original Jena comparator. The independently implemented RDF4J-value comparator is checked against all 676 pairs. These are harness calibration data, not 676 additional imported query tests.

The original `LibTestExpr.testSSE` evaluates its expected expression twice rather than evaluating its actual argument. Its invocations are documented as gaps; the extractor does not advertise them as meaningful actual-expression assertions.

## QLever: preserve relation semantics without asserting physical execution

`tools/optional_ports.py` handles reviewed non-lazy OPTIONAL helper cases. `tools/lazy_ports.py` decodes literal table/chunk initializers for MINUS, EXISTS and OPTIONAL. Bare native vocabulary identifiers become distinct IRIs; integer-valued identifiers retain integer types; undefined identifiers become absent bindings. Expected rows are taken from the source's expected tables. A separate elementary relation calculation checks the decoder against those source tables; its computed rows are never substituted for the original oracle.

The lazy helper ports concatenate chunks and compare complete result multisets. They do not claim to verify C++ chunk boundaries, physical ordering, lazy-versus-materialized flags, index selection, runtime statistics, caches, allocator state or local-vocabulary propagation. Original tables and chunk boundaries remain in case metadata for inspection.

MINUS is intentionally different from EXISTS. A compatible right-side mapping removes a left mapping only when their bound domains intersect. A shared variable unbound on one side does not establish that intersection. OPTIONAL and EXISTS still use ordinary mapping compatibility, including undefined values.

Only a reviewed initializer grammar is accepted. Unsupported mutations, control-flow-dependent vector population, moved objects, ambiguous helper signatures or source-hash changes produce explicit gaps rather than guessed cases.

## Reproduction

Run the already materialized corpus with JDK 25 and Maven:

```sh
mvn test
mvn test -Dsuite.filter=jena-native-captured
mvn test -Dsuite.filter=qlever-native-lazy
```

A complete regeneration uses Python, the JDK compiler and Maven:

```sh
python3 -m pip install -r requirements.txt
python3 -m unittest discover -s tools/tests -v
python3 tools/rebuild.py
mvn test
python3 tools/report.py
python3 tools/package.py --stage final
```

`rebuild.py --download` re-fetches the immutable source pins. Without that flag it uses the packaged vendor sources. Native extraction also needs compiled classes from the matching Jena source revision. CI reuses the verified reference-build artifact when present. Outside CI, `native_capture.py` can build the reference under `target/jena-reference`; this reference build is extraction tooling, not a runtime dependency of the generated RDF4J tests. Alternatively set `JENA_REFERENCE_ROOT` and `JENA_REFERENCE_LIBS` to a matching built checkout and its test dependency directory.

`native_capture.py --import-only` regenerates the captured records from the retained source index and event JSONL without recompiling Jena. All original query-result expectations are retained in the source package.

## Inspect actual outcomes

`reports/per-case-results.jsonl` records imported outcomes by stable ID. `reports/verification.json` separates imported execution, syntax checks and harness calibration, and lists missing or duplicated execution IDs. `reports/failures.json` provides initial triage categories, not root-cause conclusions. No missing record, unsupported case, interrupted run or packaging success is a passing correctness test.

`complete-source.patch` applies to bootstrap commit `a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3`. `incremental-source.patch` applies to previously delivered commit `4c8a3e9126e4b00a1f8db01ba1ee15f447458688`. Packaging applies both patches to their stated bases and compares tracked source bytes and executable modes. Generated corpus data and vendor fixtures live in the complete ZIP, not in the tracked-source patches.
