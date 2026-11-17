# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/current-ascii-heartbeat-pid133-2026-11-17T13_29_25.680751254Z-14748853798896164186.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 938 | 937 | 99.89% |
| Allocation samples | 2,988 | 2,986 | 99.93% |
| Allocation weight, bytes | 4,307,813,648 | 4,280,299,032 | 99.36% |

Selected CPU samples with stacks: 937; marked truncated: 0. Selected allocation samples with stacks: 2,986; marked truncated: 0.

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
| `byte[]` | 2,769.557 | 67.85% |
| `java.lang.String` | 1,310.956 | 32.12% |
| `java.util.HashMap` | 0.499 | 0.01% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter` | 0.499 | 0.01% |
| `java.util.HashMap$Node[]` | 0.499 | 0.01% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.Arrays.copyOfRange([BII)[B` | 2,769.557 | 67.85% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 1,310.956 | 32.12% |
| `jdk.internal.misc.Unsafe.allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;` | 0.499 | 0.01% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.<init>(Ljava/io/OutputStream;)V` | 0.499 | 0.01% |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 0.499 | 0.01% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 4,082.011 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 4,082.011 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 4,082.011 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005c05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 4,082.011 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005c05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 4,082.011 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 4,082.011 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 4,082.011 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 4,082.011 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 4,082.011 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 4,082.011 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 4,082.011 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 4,082.011 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 4,082.011 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 4,082.011 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 4,082.011 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 4,082.011 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 4,082.011 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 4,082.011 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 4,081.012 | 99.98% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 4,081.012 | 99.98% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 4,081.012 | 99.98% |
| `java.lang.String.substring(II)Ljava/lang/String;` | 4,080.513 | 99.96% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 4,080.513 | 99.96% |
| `org.eclipse.rdf4j.model.impl.SimpleIRI.getNamespace()Ljava/lang/String;` | 4,076.179 | 99.86% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 3,125.401 | 76.57% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
