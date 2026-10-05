# Audit of upstream result-comparison policy

Run from the suite root after the RDF4J tests:

```sh
python3 tools/run_reference_audit.py
```

The command builds Jena ARQ from the exact HASMAC-AS/jena source pin included in the archive, then uses that build's `ResultsCompare` on saved failed manifest SELECT results. It does **not** execute SPARQL queries in Jena and does not overwrite upstream expected results or change the recorded Jupiter failures.

The pinned `QueryEvalTest.java` defaults `compareResultSetsByValue` to true. It uses `ResultsCompare.equalsByValue` for unordered results and `equalsByValueAndOrder` for ordered results, after de-duplicating REDUCED results. The independent RDF4J harness intentionally retains strict-term diagnostics. This audit identifies failures that disappear under Jena's own default value-comparison policy, without incorrectly labelling them RDF4J engine defects.

`reports/upstream-oracle-audit.json` contains per-case assessments; `reports/upstream-oracle-summary.json` contains counts. `reports/upstream-comparison-policy-sites.json` records all occurrences of the policy flag in the pinned Java source, including suite-level overrides. The audit is explicitly of the default policy, not a claim that every historical/unregistered manifest ran under the same setting.

The main harness does not depend on this auxiliary module. A failure to compile or run the auxiliary audit is separately recorded in `reports/reference-audit-execution.json`. It does not prevent packaging complete source or erase the primary test results.
