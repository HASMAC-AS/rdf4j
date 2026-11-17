# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-baseline/baseline-repeat_equal_lmdb-heartbeat-pid102-2026-11-17T11_16_24.763681415Z-1828846636001329382.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 1,142 | 1,139 | 99.74% |
| Allocation samples | 3,468 | 3,454 | 99.60% |
| Allocation weight, bytes | 2,423,955,304 | 2,393,608,336 | 98.75% |

Selected CPU samples with stacks: 1,139; marked truncated: 0. Selected allocation samples with stacks: 3,454; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 733 | 64.35% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 183 | 16.07% |
| `java.nio.ByteBuffer.limit(I)Ljava/nio/Buffer;` | 74 | 6.50% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 38 | 3.34% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 29 | 2.55% |
| `sun.nio.cs.UTF_8$Encoder.encodeBufferLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 24 | 2.11% |
| `java.io.ByteArrayOutputStream.ensureCapacity(I)V` | 18 | 1.58% |
| `java.util.Collections$UnmodifiableCollection$1.hasNext()Z` | 17 | 1.49% |
| `java.nio.Buffer.limit(I)Ljava/nio/Buffer;` | 6 | 0.53% |
| `java.lang.String.checkIndex(II)V` | 4 | 0.35% |
| `sun.nio.cs.UTF_8$Encoder.encodeLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 3 | 0.26% |
| `java.nio.Buffer.position(I)Ljava/nio/Buffer;` | 2 | 0.18% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2 | 0.18% |
| `java.lang.StringBuilder.<init>()V` | 1 | 0.09% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 1 | 0.09% |
| `java.util.ArrayList.iterator()Ljava/util/Iterator;` | 1 | 0.09% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.complete()V` | 1 | 0.09% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.flushOutput()V` | 1 | 0.09% |
| `org.eclipse.rdf4j.rio.WriterConfig.<init>()V` | 1 | 0.09% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 1,139 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 1,139 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,139 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000004505d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,139 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000004505d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,139 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,139 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 1,139 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 1,139 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 1,139 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 1,139 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,139 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,139 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 1,139 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 1,139 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,139 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 1,139 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 1,139 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,139 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,133 | 99.47% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,131 | 99.30% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 342 | 30.03% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 342 | 30.03% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 183 | 16.07% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 161 | 14.14% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 147 | 12.91% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.HeapByteBuffer` | 1,011.017 | 44.29% |
| `java.util.ArrayList$Itr` | 536.145 | 23.49% |
| `java.nio.StringCharBuffer` | 328.516 | 14.39% |
| `java.util.Collections$UnmodifiableCollection$1` | 212.263 | 9.30% |
| `byte[]` | 190.786 | 8.36% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter` | 1.498 | 0.07% |
| `java.util.HashMap` | 0.999 | 0.04% |
| `java.lang.Object[]` | 0.499 | 0.02% |
| `java.util.concurrent.ConcurrentHashMap` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat` | 0.499 | 0.02% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 1,011.017 | 44.29% |
| `java.util.ArrayList.iterator()Ljava/util/Iterator;` | 536.145 | 23.49% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 328.516 | 14.39% |
| `java.util.Collections$UnmodifiableCollection.iterator()Ljava/util/Iterator;` | 212.263 | 9.30% |
| `java.nio.HeapByteBuffer.<init>(IILjava/lang/foreign/MemorySegment;)V` | 190.287 | 8.34% |
| `jdk.internal.misc.Unsafe.allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;` | 1.498 | 0.07% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.<init>(Ljava/io/OutputStream;)V` | 0.999 | 0.04% |
| `java.nio.charset.CharsetEncoder.<init>(Ljava/nio/charset/Charset;FF)V` | 0.499 | 0.02% |
| `java.util.ArrayList.<init>(Ljava/util/Collection;)V` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.rio.RioConfig.<init>()V` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 0.499 | 0.02% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 2,282.723 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 2,282.723 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,282.723 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000004505d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,282.723 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000004505d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,282.723 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,282.723 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 2,282.723 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 2,282.723 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 2,282.723 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 2,282.723 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,282.723 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,282.723 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 2,282.723 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 2,282.723 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,282.723 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 2,282.723 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 2,282.723 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,282.723 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,274.232 | 99.63% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,274.232 | 99.63% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 1,529.820 | 67.02% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 1,525.824 | 66.84% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 1,525.824 | 66.84% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 1,201.304 | 52.63% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;` | 1,201.304 | 52.63% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
