# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-baseline/baseline-ascii-heartbeat-pid71-2026-11-17T11_16_07.789782879Z-3644738514043871861.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 1,136 | 1,134 | 99.82% |
| Allocation samples | 3,610 | 3,605 | 99.86% |
| Allocation weight, bytes | 9,135,731,656 | 9,105,580,472 | 99.67% |

Selected CPU samples with stacks: 1,134; marked truncated: 0. Selected allocation samples with stacks: 3,605; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.io.ByteArrayOutputStream.ensureCapacity(I)V` | 470 | 41.45% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 163 | 14.37% |
| `sun.nio.cs.UTF_8$Encoder.encodeBufferLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 107 | 9.44% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 61 | 5.38% |
| `sun.nio.cs.UTF_8$Encoder.encodeLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 57 | 5.03% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 48 | 4.23% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 42 | 3.70% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 35 | 3.09% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeNamespace(Ljava/lang/String;)Ljava/lang/Integer;` | 28 | 2.47% |
| `jdk.internal.util.ArraysSupport.hashCodeOfUnsigned([BIII)I` | 21 | 1.85% |
| `org.eclipse.rdf4j.model.impl.SimpleLiteral.equals(Ljava/lang/Object;)Z` | 17 | 1.50% |
| `java.lang.String.checkIndex(II)V` | 15 | 1.32% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 14 | 1.23% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeBNode(Lorg/eclipse/rdf4j/model/BNode;)V` | 14 | 1.23% |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 12 | 1.06% |
| `java.lang.StringLatin1.hashCode([B)I` | 10 | 0.88% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 7 | 0.62% |
| `org.eclipse.rdf4j.model.base.AbstractLiteral.equals(Ljava/lang/Object;)Z` | 5 | 0.44% |
| `java.util.HashMap.putVal(ILjava/lang/Object;Ljava/lang/Object;ZZ)Ljava/lang/Object;` | 2 | 0.18% |
| `java.lang.Class.getName()Ljava/lang/String;` | 1 | 0.09% |
| `java.util.Arrays.copyOfRange([BII)[B` | 1 | 0.09% |
| `java.util.Collections.unmodifiableList(Ljava/util/List;)Ljava/util/List;` | 1 | 0.09% |
| `java.util.HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1 | 0.09% |
| `org.eclipse.rdf4j.model.base.AbstractBNode.equals(Ljava/lang/Object;)Z` | 1 | 0.09% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.startQueryResult(Ljava/util/List;)V` | 1 | 0.09% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 1,134 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 1,134 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,134 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000004305d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,134 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000004305d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,134 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,134 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 1,134 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 1,134 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 1,134 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 1,134 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,134 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,134 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 1,134 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 1,134 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,134 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 1,134 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 1,134 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,134 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,131 | 99.74% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,131 | 99.74% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 1,021 | 90.04% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 675 | 59.52% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 649 | 57.23% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 596 | 52.56% |
| `java.io.ByteArrayOutputStream.ensureCapacity(I)V` | 470 | 41.45% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.StringCharBuffer` | 3,530.528 | 40.66% |
| `java.nio.HeapByteBuffer` | 2,652.047 | 30.54% |
| `byte[]` | 1,263.149 | 14.55% |
| `java.lang.String` | 1,098.618 | 12.65% |
| `java.util.HashMap$Node` | 71.919 | 0.83% |
| `java.lang.Integer` | 51.942 | 0.60% |
| `java.util.HashMap$Node[]` | 15.555 | 0.18% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 3,530.528 | 40.66% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 2,652.047 | 30.54% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 1,098.618 | 12.65% |
| `java.nio.HeapByteBuffer.<init>(IILjava/lang/foreign/MemorySegment;)V` | 914.069 | 10.53% |
| `java.util.Arrays.copyOfRange([BII)[B` | 349.080 | 4.02% |
| `java.util.HashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 71.919 | 0.83% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeNamespace(Ljava/lang/String;)Ljava/lang/Integer;` | 51.942 | 0.60% |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 15.555 | 0.18% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 8,683.758 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 8,683.758 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,683.758 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000004305d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,683.758 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000004305d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,683.758 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,683.758 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 8,683.758 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 8,683.758 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 8,683.758 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 8,683.758 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,683.758 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,683.758 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 8,683.758 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 8,683.758 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 8,683.758 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 8,683.758 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 8,683.758 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 8,683.758 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 8,683.758 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 8,683.758 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 8,683.758 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 7,096.645 | 81.72% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 6,352.619 | 73.16% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 3,951.696 | 45.51% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 3,566.116 | 41.07% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
