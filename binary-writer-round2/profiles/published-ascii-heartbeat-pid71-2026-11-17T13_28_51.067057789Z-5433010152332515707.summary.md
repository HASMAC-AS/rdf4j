# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/published-ascii-heartbeat-pid71-2026-11-17T13_28_51.067057789Z-5433010152332515707.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 946 | 945 | 99.89% |
| Allocation samples | 2,788 | 2,778 | 99.64% |
| Allocation weight, bytes | 2,213,500,680 | 2,185,983,952 | 98.76% |

Selected CPU samples with stacks: 945; marked truncated: 0. Selected allocation samples with stacks: 2,778; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 225 | 23.81% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 112 | 11.85% |
| `java.io.DataOutputStream.write([BII)V` | 90 | 9.52% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 84 | 8.89% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 75 | 7.94% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 59 | 6.24% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 53 | 5.61% |
| `jdk.internal.util.ArraysSupport.hashCodeOfUnsigned([BIII)I` | 34 | 3.60% |
| `java.io.ByteArrayOutputStream.write(I)V` | 29 | 3.07% |
| `java.lang.String.charAt(I)C` | 17 | 1.80% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 17 | 1.80% |
| `java.io.ByteArrayOutputStream.ensureCapacity(I)V` | 16 | 1.69% |
| `java.util.Arrays.copyOfRange([BII)[B` | 11 | 1.16% |
| `java.io.DataOutputStream.incCount(I)V` | 10 | 1.06% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 10 | 1.06% |
| `java.lang.StringLatin1.lastIndexOf([BII)I` | 9 | 0.95% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 9 | 0.95% |
| `java.lang.StringLatin1.hashCode([B)I` | 8 | 0.85% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 8 | 0.85% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 7 | 0.74% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 6 | 0.63% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 6 | 0.63% |
| `java.util.Arrays$ArrayList.get(I)Ljava/lang/Object;` | 4 | 0.42% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeBufferedString(I)V` | 4 | 0.42% |
| `java.io.DataOutputStream.writeInt(I)V` | 3 | 0.32% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 945 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 945 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 945 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005105d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 945 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005105d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 945 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 945 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 945 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 945 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 945 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 945 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 945 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 945 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 945 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 945 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 945 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 945 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 945 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 945 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 942 | 99.68% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 942 | 99.68% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 844 | 89.31% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 569 | 60.21% |
| `java.io.DataOutputStream.write([BII)V` | 394 | 41.69% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 381 | 40.32% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat$HeartbeatOutputStream.write([BII)V` | 368 | 38.94% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `byte[]` | 1,415.129 | 67.88% |
| `java.lang.String` | 665.593 | 31.93% |
| `java.util.HashMap$Node` | 1.998 | 0.10% |
| `java.util.concurrent.ConcurrentHashMap` | 0.999 | 0.05% |
| `java.util.Collections$UnmodifiableCollection$1` | 0.999 | 0.05% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.Arrays.copyOfRange([BII)[B` | 1,412.637 | 67.76% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 665.593 | 31.93% |
| `java.util.HashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 1.998 | 0.10% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.<init>(Ljava/io/OutputStream;)V` | 1.992 | 0.10% |
| `org.eclipse.rdf4j.rio.RioConfig.<init>()V` | 0.999 | 0.05% |
| `java.util.Collections$UnmodifiableCollection.iterator()Ljava/util/Iterator;` | 0.999 | 0.05% |
| `java.nio.charset.CharsetEncoder.<init>(Ljava/nio/charset/Charset;FF)V` | 0.499 | 0.02% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 2,084.717 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 2,084.717 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,084.717 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005105d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,084.717 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000005105d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,084.717 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,084.717 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 2,084.717 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 2,084.717 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 2,084.717 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 2,084.717 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,084.717 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,084.717 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 2,084.717 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 2,084.717 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,084.717 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 2,084.717 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 2,084.717 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,084.717 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,080.228 | 99.78% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,080.228 | 99.78% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 2,080.228 | 99.78% |
| `java.lang.String.substring(II)Ljava/lang/String;` | 2,078.230 | 99.69% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 2,078.230 | 99.69% |
| `org.eclipse.rdf4j.model.impl.SimpleIRI.getNamespace()Ljava/lang/String;` | 2,062.363 | 98.93% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 1,610.635 | 77.26% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
