# Independent benchmark methodology review

Reviewed against upstream `main` revision `5434e51194e6c2e1aa745e5d65a4c998918d6073`. This review covers the standalone `BinaryWriterBenchmark`, `bench.py`, the completed `isolated-screen` comparison, and the subsequent explicit namespace-layout parameter. It does not add production changes or run another benchmark.

## Original namespace fixture and correction

The original mixed fixture used `urn:binary-benchmark:subject:<row>:<column>` and `urn:binary-benchmark:kind:<row%17>:<column>`. [SimpleIRI.getNamespace](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/model/src/main/java/org/eclipse/rdf4j/model/impl/SimpleIRI.java#L109) delegates to [URIUtil.getLocalNameIndex](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/model/src/main/java/org/eclipse/rdf4j/model/util/URIUtil.java#L67), which splits these URNs after the last colon. Consequently, each subject row introduced a distinct namespace.

| Layout | Subject namespaces | Kind namespaces | Datatype namespaces | Total per mixed document |
| --- | ---: | ---: | ---: | ---: |
| Original, now `churn` | 4,096 | 17 | 2 | **4,115** |
| New default `stable` | 1 | 1 | 2 | **4** |

The two datatype namespaces are XSD and `urn:binary-benchmark:datatype:`. The counts apply to `ascii`, `unicode`, `wide`, and `long`; widening a row repeats local names within the same subject/kind namespaces. Stable replaces only the final colon with a hyphen, preserving the row and column identifiers in the local name. Churn preserves the original values exactly. The three synthetic LMDB literal workloads and the real query are independent of this parameter; the real query already used stable `s:<subject>` and `kind:<mod7>` IRIs.

The completed `isolated-screen` measurements and initial baseline profiles remain valid **namespace-churn** evidence. They are not evidence for a stable-namespace workload. Final canonical comparisons should select stable explicitly, with churn retained as a separately labeled stress case. The new parameter defaults to stable in both JMH and manually instantiated smoke runs; the fourth smoke CSV argument selects one or both layouts.

## Identity of the original screening artifacts

SHA-256 values captured after `isolated-screen` completed and before modifying the benchmark source:

| Artifact, relative to `binary-writer-evidence/` | SHA-256 |
| --- | --- |
| Original benchmark source: `harness/src/org/eclipse/rdf4j/query/resultio/binary/benchmark/BinaryWriterBenchmark.java` | `23aa4e56b073820baf3e1b7b43a7c0cd6e19b627034b97eb937dc9f6d0bc11b8` |
| Renamed baseline: `harness/OriginalBinaryQueryResultWriter.java` | `6af56d6ba09b334fa38bda9548c2e45f8167103e31e0191f7494a1cf3c1399bd` |
| Immutable baseline: `harness/original/BinaryQueryResultWriter.java` | `7747ecd67c429be722dcfbb9723241c864ebf49abff1b3830251beece14fa7e5` |
| Screening compilation metadata: `harness/compiled-source-identity.json` | `15e6fafc46e5be2decbd9b18bbcd04f1e44760f0ee9126d17dd1a090a9c777a7` |
| Recorded invocation: `isolated-screen-command.json` | `57f2e171fbd46895019f37d1ed26624087e08fc69ce9ba990600d1d98c0225bc` |
| Raw JMH results: `isolated-screen.json` | `200967f2a5920f6d407365155dccc96f6146981ee5679fb59210bee76f5c7d93` |

The compilation metadata path is overwritten by a later compile; the hash above identifies its screening version. Its recorded benchmark hash matched the original source, and all listed source hashes matched live files during review. Removing the `Original` class/constructor rename from the baseline yielded byte-for-byte upstream source. The runtime classpath had no duplicate jar basenames or RDF4J artifacts from another checkout.

The screening used JDK 25.0.5+8, one thread, one fork, two one-second warmup iterations, three one-second measurement iterations, and the GC profiler. Its selections were ten writer variants, `ascii,unicode,repeat_equal_lmdb`, and `sink=heartbeat`. Treat this short run as variant screening; use the separately recorded final comparisons for performance claims.

## Measurement validity and scope

- Each invocation writes a complete 4,096-row document. `@OperationsPerInvocation(4096)` normalizes time and allocation to one row. Writer construction and optional heartbeat-wrapper construction occur once per measured document; reflective constructor lookup occurs once during setup.
- Synthetic rows and values are preconstructed. A complete document is written during trial setup, growing the reusable `ByteArrayOutputStream` before measurement, including the 8,192-character literal case. The benchmark writes actual byte arrays and consumes the backing array, sampled bytes, and final size.
- Synthetic LMDB IDs are consistent with lexical values: repeated group/column pairs have the same lexical value and native ID; unique pairs have distinct IDs. An explicitly canonical shared revision and initialized literals isolate model equality without database IO. These cases must not be described as database benchmarks.
- The real query loads 512 subjects with six single-valued properties and eight `v` values each, producing exactly 4,096 rows. Native evaluation is explicitly enabled. Loading and query preparation occur during setup; query evaluation, result materialization, and serialization are measured. Every invocation checks the result count; smoke validation checks all expected subject/value pairs exactly once and compares every parsed term.
- The timed fork receives the WARN-level Logback configuration. INFO output observed during smoke validation is outside measurement. Profiling and timing comparisons are separate runs.
- The heartbeat sink includes the actual synchronized payload stream and terminal-state checks, with no scheduled heartbeat thread for binary format. The destination is a reusable memory buffer, so these measurements do not establish socket throughput, servlet latency, or behavior under slow consumers.

No other invalidating methodology issue was found. The proposed writer-only scope keeps equality optimization limited to avoiding unnecessary comparisons, and avoids exposing LMDB storage identities or native byte-buffer lifetimes through generic result IO.
