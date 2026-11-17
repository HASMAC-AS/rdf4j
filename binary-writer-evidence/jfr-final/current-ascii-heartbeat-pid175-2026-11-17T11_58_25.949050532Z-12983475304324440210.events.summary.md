# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-final/current-ascii-heartbeat-pid175-2026-11-17T11_58_25.949050532Z-12983475304324440210.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 1,127 | 1,126 | 99.91% |
| Allocation samples | 3,371 | 3,361 | 99.70% |
| Allocation weight, bytes | 2,772,885,416 | 2,745,365,672 | 99.01% |

Selected CPU samples with stacks: 1,126; marked truncated: 0. Selected allocation samples with stacks: 3,361; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 252 | 22.38% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 138 | 12.26% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 114 | 10.12% |
| `java.io.DataOutputStream.write([BII)V` | 105 | 9.32% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 78 | 6.93% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 62 | 5.51% |
| `jdk.internal.util.ArraysSupport.hashCodeOfUnsigned([BIII)I` | 45 | 4.00% |
| `java.io.ByteArrayOutputStream.write(I)V` | 40 | 3.55% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 39 | 3.46% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 24 | 2.13% |
| `java.lang.StringLatin1.charAt([BI)C` | 19 | 1.69% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 18 | 1.60% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 17 | 1.51% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 16 | 1.42% |
| `java.lang.StringLatin1.lastIndexOf([BII)I` | 13 | 1.15% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 12 | 1.07% |
| `java.io.DataOutputStream.incCount(I)V` | 11 | 0.98% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 11 | 0.98% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 11 | 0.98% |
| `java.lang.String.length()I` | 8 | 0.71% |
| `java.lang.StringLatin1.hashCode([B)I` | 8 | 0.71% |
| `java.util.Objects.equals(Ljava/lang/Object;Ljava/lang/Object;)Z` | 8 | 0.71% |
| `java.util.ImmutableCollections$ListN.indexOf(Ljava/lang/Object;)I` | 7 | 0.62% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 6 | 0.53% |
| `java.util.Arrays.copyOfRange([BII)[B` | 6 | 0.53% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 1,126 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 1,126 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006405d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006405d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 1,126 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 1,126 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 1,126 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 1,126 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 1,126 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 1,126 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,126 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 1,126 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 1,126 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,126 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,125 | 99.91% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,125 | 99.91% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 973 | 86.41% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 617 | 54.80% |
| `java.io.DataOutputStream.write([BII)V` | 438 | 38.90% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat$HeartbeatOutputStream.write([BII)V` | 415 | 36.86% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 410 | 36.41% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `byte[]` | 1,861.039 | 71.08% |
| `java.lang.String` | 742.163 | 28.35% |
| `java.util.HashMap$Node` | 11.986 | 0.46% |
| `java.util.concurrent.ConcurrentHashMap` | 1.998 | 0.08% |
| `java.util.Collections$UnmodifiableCollection$1` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat` | 0.499 | 0.02% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.Arrays.copyOfRange([BII)[B` | 1,859.050 | 71.01% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 742.163 | 28.35% |
| `java.util.HashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 11.986 | 0.46% |
| `org.eclipse.rdf4j.rio.RioConfig.<init>()V` | 1.998 | 0.08% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.<init>(Ljava/io/OutputStream;)V` | 1.989 | 0.08% |
| `java.util.Collections$UnmodifiableCollection.iterator()Ljava/util/Iterator;` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 0.499 | 0.02% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 2,618.185 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 2,618.185 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,618.185 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006405d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,618.185 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006405d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,618.185 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,618.185 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 2,618.185 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 2,618.185 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 2,618.185 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 2,618.185 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,618.185 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,618.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 2,618.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 2,618.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,618.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 2,618.185 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 2,618.185 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,618.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,613.199 | 99.81% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,613.199 | 99.81% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 2,613.199 | 99.81% |
| `java.lang.String.substring(II)Ljava/lang/String;` | 2,601.213 | 99.35% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 2,601.213 | 99.35% |
| `org.eclipse.rdf4j.model.impl.SimpleIRI.getNamespace()Ljava/lang/String;` | 2,583.535 | 98.68% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 2,015.298 | 76.97% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
