# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/published-long_unicode-heartbeat-pid102-2026-11-17T13_29_09.115048933Z-15283787376284177849.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 997 | 997 | 100.00% |
| Allocation samples | 3,170 | 3,168 | 99.94% |
| Allocation weight, bytes | 9,049,554,032 | 9,022,141,640 | 99.70% |

Selected CPU samples with stacks: 997; marked truncated: 0. Selected allocation samples with stacks: 3,168; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `sun.nio.cs.UTF_8$Encoder.encodeBufferLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 270 | 27.08% |
| `java.nio.Buffer.nextPutIndex()I` | 154 | 15.45% |
| `java.nio.HeapByteBuffer.put(B)Ljava/nio/ByteBuffer;` | 114 | 11.43% |
| `java.lang.StringUTF16.charAt([BI)C` | 90 | 9.03% |
| `java.nio.Buffer.hasRemaining()Z` | 89 | 8.93% |
| `java.nio.HeapByteBuffer.<init>(IILjava/lang/foreign/MemorySegment;)V` | 60 | 6.02% |
| `java.nio.Buffer.remaining()I` | 43 | 4.31% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 34 | 3.41% |
| `java.nio.Buffer.nextGetIndex()I` | 31 | 3.11% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 16 | 1.60% |
| `java.nio.HeapByteBuffer.ix(I)I` | 15 | 1.50% |
| `java.lang.Character.isSurrogate(C)Z` | 12 | 1.20% |
| `java.lang.Character.isLowSurrogate(C)Z` | 8 | 0.80% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;` | 8 | 0.80% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 5 | 0.50% |
| `sun.nio.cs.Surrogate$Parser.parse(CLjava/nio/CharBuffer;)I` | 5 | 0.50% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 4 | 0.40% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 4 | 0.40% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 3 | 0.30% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 3 | 0.30% |
| `jdk.internal.util.Preconditions.checkFromIndexSize(IIILjava/util/function/BiFunction;)I` | 3 | 0.30% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat$HeartbeatOutputStream.write(I)V` | 3 | 0.30% |
| `java.io.DataOutputStream.write([BII)V` | 2 | 0.20% |
| `java.lang.Character.isHighSurrogate(C)Z` | 2 | 0.20% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 2 | 0.20% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 997 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 997 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 997 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001705d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 997 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001705d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 997 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 997 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 997 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 997 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 997 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 997 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 997 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 997 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 997 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 997 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 997 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 997 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 996 | 99.90% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 996 | 99.90% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 995 | 99.80% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 994 | 99.70% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 985 | 98.80% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 971 | 97.39% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 930 | 93.28% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;` | 904 | 90.67% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;Z)Ljava/nio/charset/CoderResult;` | 836 | 83.85% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `byte[]` | 5,466.117 | 63.53% |
| `java.lang.String` | 3,135.122 | 36.44% |
| `java.nio.HeapByteBuffer` | 2.946 | 0.03% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.HeapByteBuffer.<init>(IILjava/lang/foreign/MemorySegment;)V` | 5,466.117 | 63.53% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 3,135.122 | 36.44% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 2.946 | 0.03% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 8,604.185 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 8,604.185 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,604.185 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001705d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,604.185 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001705d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,604.185 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,604.185 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 8,604.185 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 8,604.185 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 8,604.185 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 8,604.185 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,604.185 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,604.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 8,604.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 8,604.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 8,604.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 8,604.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 8,604.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 8,604.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 8,604.185 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 8,604.185 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 8,604.185 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 8,604.185 | 100.00% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 5,469.063 | 63.56% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;` | 5,469.063 | 63.56% |
| `org.eclipse.rdf4j.query.resultio.binary.PublishedBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 5,469.063 | 63.56% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
