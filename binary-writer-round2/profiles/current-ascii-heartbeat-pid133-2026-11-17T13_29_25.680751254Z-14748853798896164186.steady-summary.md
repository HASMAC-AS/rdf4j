# JFR allocation first-sample sensitivity analysis

Approximate corrected distribution: the first allocation sample per recorded thread ID is omitted. This also drops some valid post-start allocation weight. CPU events are unchanged; JMH remains the authority for measured B/row. Keep the raw summary alongside this report.

Omitted selected allocation weight: 1,344,378,400 bytes (31.41% of the raw selected weight).

## Every omitted allocation sample

| Thread ID | Timestamp | Weight, bytes | Selected | Allocated class | Top recorded frame |
| --- | --- | ---: | --- | --- | --- |
| javaThreadId=23 | 2026-11-17T14:29:25.870657013+01:00 | 1,344,378,400 | True | `java.lang.String` | `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` |
| javaThreadId=3 | 2026-11-17T14:29:29.876557002+01:00 | 27,449,712 | False | `java.util.concurrent.locks.AbstractQueuedSynchronizer$ExclusiveNode` | `java.util.concurrent.locks.AbstractQueuedSynchronizer.tryInitializeHead()Ljava/util/concurrent/locks/AbstractQueuedSynchronizer$Node;` |
| javaThreadId=26 | 2026-11-17T14:29:35.867367940+01:00 | 64,904 | False | `java.util.ArrayList$Itr` | `java.util.ArrayList.iterator()Ljava/util/Iterator;` |

## Retained-event summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/current-ascii-heartbeat-pid133-2026-11-17T13_29_25.680751254Z-14748853798896164186.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 938 | 937 | 99.89% |
| Allocation samples | 2,985 | 2,985 | 100.00% |
| Allocation weight, bytes | 2,935,920,632 | 2,935,920,632 | 100.00% |

Selected CPU samples with stacks: 937; marked truncated: 0. Selected allocation samples with stacks: 2,985; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 170 | 18.14% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.encodeString(Ljava/lang/String;II)I` | 103 | 10.99% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 86 | 9.18% |
| `java.io.DataOutputStream.write([BII)V` | 62 | 6.62% |
| `jdk.internal.util.ArraysSupport.hashCodeOfUnsigned([BIII)I` | 62 | 6.62% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 53 | 5.66% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 48 | 5.12% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 36 | 3.84% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 29 | 3.10% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeNamedRecord(IILjava/lang/String;)V` | 24 | 2.56% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 21 | 2.24% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 17 | 1.81% |
| `java.lang.String.charAt(I)C` | 15 | 1.60% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 15 | 1.60% |
| `java.lang.String.length()I` | 14 | 1.49% |
| `java.lang.StringLatin1.hashCode([B)I` | 13 | 1.39% |
| `java.util.Arrays.copyOfRange([BII)[B` | 13 | 1.39% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeTypedLiteral(Ljava/lang/String;[B)V` | 13 | 1.39% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeTypedLiteral(Ljava/lang/String;ILjava/lang/String;)V` | 11 | 1.17% |
| `java.lang.String.substring(II)Ljava/lang/String;` | 10 | 1.07% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 10 | 1.07% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 10 | 1.07% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 10 | 1.07% |
| `java.util.ImmutableCollections$ListN.indexOf(Ljava/lang/Object;)I` | 8 | 0.85% |
| `org.eclipse.rdf4j.model.impl.SimpleLiteral.equals(Ljava/lang/Object;)Z` | 8 | 0.85% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 937 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 937 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 937 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005c05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 937 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005c05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 937 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 937 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 937 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 937 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 937 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 937 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 937 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 937 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 937 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 937 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 937 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 937 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 936 | 99.89% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 935 | 99.79% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 932 | 99.47% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 932 | 99.47% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 762 | 81.32% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 457 | 48.77% |
| `java.io.DataOutputStream.write([BII)V` | 351 | 37.46% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat$HeartbeatOutputStream.write([BII)V` | 283 | 30.20% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 279 | 29.78% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `byte[]` | 2,769.557 | 98.92% |
| `java.lang.String` | 28.857 | 1.03% |
| `java.util.HashMap` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter` | 0.499 | 0.02% |
| `java.util.HashMap$Node[]` | 0.499 | 0.02% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.Arrays.copyOfRange([BII)[B` | 2,769.557 | 98.92% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 28.857 | 1.03% |
| `jdk.internal.misc.Unsafe.allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.<init>(Ljava/io/OutputStream;)V` | 0.499 | 0.02% |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 0.499 | 0.02% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 2,799.912 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 2,799.912 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,799.912 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005c05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,799.912 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005c05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,799.912 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,799.912 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 2,799.912 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 2,799.912 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 2,799.912 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 2,799.912 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,799.912 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,799.912 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 2,799.912 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 2,799.912 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,799.912 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 2,799.912 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 2,799.912 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,799.912 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,798.913 | 99.96% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,798.913 | 99.96% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 2,798.913 | 99.96% |
| `java.lang.String.substring(II)Ljava/lang/String;` | 2,798.414 | 99.95% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 2,798.414 | 99.95% |
| `org.eclipse.rdf4j.model.impl.SimpleIRI.getNamespace()Ljava/lang/String;` | 2,794.079 | 99.79% |
| `java.util.Arrays.copyOfRange([BII)[B` | 2,769.557 | 98.92% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
- This separate sensitivity report omits each thread's first allocation sample; raw recordings and summaries remain authoritative evidence.
- OpenJDK's first allocation sample can carry cumulative pre-recording weight; omission also discards some valid post-start allocations.
- Remaining allocation attribution is sampled and throttled, not exact; totals are not normalized by rows or iterations.
- CPU events and JMH results are unchanged; use JMH gc.alloc.rate.norm for measured absolute B/row.
- The same first-sample caveat applies to the previous round's allocation percentages from this profiler, without changing its CPU or JMH results.
