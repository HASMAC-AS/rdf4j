# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-final/baseline-ascii-heartbeat-pid71-2026-11-17T11_57_28.358697780Z-5472984616024138580.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 1,127 | 1,126 | 99.91% |
| Allocation samples | 3,608 | 3,607 | 99.97% |
| Allocation weight, bytes | 7,997,211,096 | 7,997,146,384 | 100.00% |

Selected CPU samples with stacks: 1,126; marked truncated: 0. Selected allocation samples with stacks: 3,607; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 272 | 24.16% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 108 | 9.59% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 104 | 9.24% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 97 | 8.61% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 74 | 6.57% |
| `java.io.DataOutputStream.write([BII)V` | 59 | 5.24% |
| `sun.nio.cs.UTF_8$Encoder.encodeBufferLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 45 | 4.00% |
| `java.nio.Buffer.hasRemaining()Z` | 37 | 3.29% |
| `java.io.ByteArrayOutputStream.write(I)V` | 29 | 2.58% |
| `jdk.internal.util.ArraysSupport.hashCodeOfUnsigned([BIII)I` | 27 | 2.40% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 20 | 1.78% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 17 | 1.51% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 17 | 1.51% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 15 | 1.33% |
| `java.nio.StringCharBuffer.get()C` | 15 | 1.33% |
| `java.util.ImmutableCollections$ListN.indexOf(Ljava/lang/Object;)I` | 14 | 1.24% |
| `java.nio.HeapByteBuffer.<init>(IILjava/lang/foreign/MemorySegment;)V` | 10 | 0.89% |
| `java.lang.String.length()I` | 9 | 0.80% |
| `java.nio.Buffer.nextPutIndex()I` | 9 | 0.80% |
| `java.io.ByteArrayOutputStream.ensureCapacity(I)V` | 8 | 0.71% |
| `java.io.DataOutputStream.writeInt(I)V` | 7 | 0.62% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 7 | 0.62% |
| `jdk.internal.util.ArraysSupport.newLength(III)I` | 7 | 0.62% |
| `java.lang.invoke.VarHandleByteArrayAsInts$ArrayHandle.index([BI)I` | 6 | 0.53% |
| `org.eclipse.rdf4j.model.util.Literals.isLanguageLiteral(Lorg/eclipse/rdf4j/model/Literal;)Z` | 6 | 0.53% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 1,126 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 1,126 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006605d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006605d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,126 | 100.00% |
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
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,123 | 99.73% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,123 | 99.73% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 996 | 88.45% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 746 | 66.25% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 639 | 56.75% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 484 | 42.98% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat$HeartbeatOutputStream.write([BII)V` | 470 | 41.74% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `byte[]` | 3,872.525 | 50.78% |
| `java.nio.HeapByteBuffer` | 2,586.102 | 33.91% |
| `java.nio.StringCharBuffer` | 1,047.228 | 13.73% |
| `java.lang.String` | 120.817 | 1.58% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.HeapByteBuffer.<init>(IILjava/lang/foreign/MemorySegment;)V` | 2,602.193 | 34.12% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 2,586.102 | 33.91% |
| `java.util.Arrays.copyOfRange([BII)[B` | 1,270.332 | 16.66% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 1,047.228 | 13.73% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 120.817 | 1.58% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 7,626.673 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 7,626.673 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 7,626.673 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006605d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 7,626.673 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006605d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 7,626.673 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 7,626.673 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 7,626.673 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 7,626.673 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 7,626.673 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 7,626.673 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 7,626.673 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 7,626.673 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 7,626.673 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 7,626.673 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 7,626.673 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 7,626.673 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 7,626.673 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 7,626.673 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 7,626.174 | 99.99% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 7,626.174 | 99.99% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 7,626.174 | 99.99% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 6,235.524 | 81.76% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 5,221.869 | 68.47% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 5,188.296 | 68.03% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;` | 5,188.296 | 68.03% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
