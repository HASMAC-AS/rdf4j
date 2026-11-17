# Untimed admission and natural-collection diagnostics

These launchers are mechanism diagnostics. They do not produce query latency estimates. They compile against the qualified experiment classes, require the same supporting RDF4J classpath as the study, and must run in the root agent's exclusive Java slot. Neither launcher calls `System.gc`, sleeps, or records wall-clock latency. No Java invocation was performed while preparing these sources.

## Cyclic working-set trace

Main class: `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeDirectMemoAdmissionTraceDiagnostic`.

Its single argument is `ordinary`, `initial`, or `always`. Each invocation emits ten JSON records: W=64, 4,096, 8,192, 16,384, and 65,536, each with zero bootstrap requests or an 8,192-request bootstrap that repeatedly requests a disjoint key. Every trace has exactly 131,072 requests and a fresh memo/ledger. The bootstrapped W=65,536 tail therefore contains one complete cycle and 57,344 requests of the next cycle; the output states both counts.

The ordinary arm uses the same fresh lazy source without creating a memo. Enabled arms use weak retention and the specified admission property. Returned objects remain in intentional consumer roots until the trace closes, so any encountered collected entry is a qualification failure. Every native ID and lexical value is checked. Source calls must equal total requests minus memo hits, and this controlled source must initialize exactly once per source call.

Checkpoints at request 1 and every 8,192 requests report source calls, memo probes, hits, collisions, capacity, disabled state, and reservation. Close records include the existing memo counters; query/global ledger endpoints must be zero. This is a direct-table trace, not an AUTO query, and does not establish attainable working-set capacity independently of the adaptive history. Compare ordinary/initial/always checksums within the same W/bootstrap pair.

## Real ValueStore with natural collection

Main class: `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeDirectMemoNaturalGcDiagnostic`.

Use a fixed 128 MiB G1 Java heap. The launcher verifies initial and maximum heap sizes and the presence of G1. Its arguments are:

    ordinary|strong|weak 1024|1048577 none|one FRESH_VALUES_DIRECTORY

For example, after the root agent compiles the diagnostic into its qualified classpath:

    java -Xms128m -Xmx128m -XX:+UseG1GC -cp <qualified-classpath> \
      org.eclipse.rdf4j.sail.lmdb.evaluation.NativeDirectMemoNaturalGcDiagnostic \
      weak 1048577 none <new-attempt-directory>/values

The finite core comparison is strong/weak × 1,024/1,048,577 characters × no retained consumer/one retained consumer: eight JVMs. Ordinary supports four additional source-only controls if those are declared before launching. Each run requires a new values directory, seeds eight IRIs with distinct memo buckets, closes the seeding store, and reopens it before collecting observations. Only primitive IDs, serial numbers, bucket numbers, and a checksum escape seeding. At most 32 candidates may be stored to find the eight buckets. Default source-cache settings remain unchanged. The native value-database map is explicitly 128 MiB in every arm; that native mapping is separate from the 128 MiB Java heap.

After the first full read, each of 16 rounds allocates 32 MiB in 256 KiB arrays. Every array escapes through a volatile field and has each page touched; the field is cleared at the end of the round. This supplies exactly 512 MiB of specified array payload over the run; array headers, source activity, verification, reporting, and runtime allocations are additional. No artificial collection is requested. A fresh weak canary records whether a collection affected a newly unreachable object during that round.

The first read and every revisit validate every lexical character and the requested native ID. Before each revisit, weak observers report which earlier Values and payload Strings have cleared. Identity masks compare returned Values with weak observers directly, without loading an earlier Value into a strong local. All arrays of observations contain weak references only. In `one` mode, an explicit holder retains the first initial Value through memo/source close; the holder is fenced and then released. No other result holder persists between reads.

JSON records report source calls, whether each returned source object was already initialized, observed cold-to-initialized transitions, same-object returns, weak-observer clearing masks, canary state, GC count/time deltas, metadata reservations, and whole-JVM heap usage. A cold-to-initialized transition is a consumer-visible completion observation; it is not an instrumented physical LMDB decode count. GC and heap observations include other JVM roots and activity and cannot be assigned exclusively to the memo. Lack of observed clearing under this bounded schedule is inconclusive about eventual collection.

There are only 136 memo probes, so the initial cutoff and resizing do not participate. Collisions, semantic/source invalidations, unsupported values, reservation refusals, and cleanup leaks are qualification failures. This isolates useful same-key loss from the different-key clearing ambiguity in the aggregate `collectedMisses` counter. The diagnostic uses public ValueStore lazy lookup and the real memo directly; it does not establish full-query performance or replace native result lifetime qualification.
