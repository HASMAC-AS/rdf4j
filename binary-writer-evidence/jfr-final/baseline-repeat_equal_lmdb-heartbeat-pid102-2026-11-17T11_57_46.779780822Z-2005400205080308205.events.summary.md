# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-final/baseline-repeat_equal_lmdb-heartbeat-pid102-2026-11-17T11_57_46.779780822Z-2005400205080308205.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 1,136 | 1,136 | 100.00% |
| Allocation samples | 3,375 | 3,366 | 99.73% |
| Allocation weight, bytes | 2,351,162,504 | 2,323,567,968 | 98.83% |

Selected CPU samples with stacks: 1,136; marked truncated: 0. Selected allocation samples with stacks: 3,366; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 326 | 28.70% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 176 | 15.49% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 114 | 10.04% |
| `java.io.ByteArrayOutputStream.write(I)V` | 96 | 8.45% |
| `java.util.ImmutableCollections$ListN.indexOf(Ljava/lang/Object;)I` | 57 | 5.02% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 47 | 4.14% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 39 | 3.43% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.checkDatatypeVersion()V` | 32 | 2.82% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 30 | 2.64% |
| `java.util.Objects.equals(Ljava/lang/Object;Ljava/lang/Object;)Z` | 20 | 1.76% |
| `java.io.DataOutputStream.write([BII)V` | 17 | 1.50% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 15 | 1.32% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 14 | 1.23% |
| `java.io.ByteArrayOutputStream.ensureCapacity(I)V` | 13 | 1.14% |
| `java.util.Collections$UnmodifiableCollection$1.hasNext()Z` | 12 | 1.06% |
| `java.nio.Buffer.hasRemaining()Z` | 9 | 0.79% |
| `sun.nio.cs.UTF_8$Encoder.encodeBufferLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 9 | 0.79% |
| `java.io.DataOutputStream.incCount(I)V` | 6 | 0.53% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 6 | 0.53% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 6 | 0.53% |
| `java.nio.StringCharBuffer.get()C` | 6 | 0.53% |
| `java.util.ArrayList$Itr.next()Ljava/lang/Object;` | 6 | 0.53% |
| `java.io.DataOutputStream.writeByte(I)V` | 5 | 0.44% |
| `java.io.DataOutputStream.writeInt(I)V` | 5 | 0.44% |
| `java.util.ArrayList.iterator()Ljava/util/Iterator;` | 5 | 0.44% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 1,136 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 1,136 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,136 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 1,136 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 1,136 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 1,136 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 1,136 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 1,136 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,136 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,135 | 99.91% |
| `java.lang.invoke.LambdaForm$MH.0x000000006205d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,135 | 99.91% |
| `java.lang.invoke.LambdaForm$MH.0x000000006205d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,135 | 99.91% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,135 | 99.91% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,135 | 99.91% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,135 | 99.91% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 1,135 | 99.91% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 1,134 | 99.82% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 1,134 | 99.82% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,129 | 99.38% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,127 | 99.21% |
| `java.io.DataOutputStream.writeByte(I)V` | 548 | 48.24% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat$HeartbeatOutputStream.write(I)V` | 539 | 47.45% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 537 | 47.27% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeRepeat()V` | 516 | 45.42% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 302 | 26.58% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.HeapByteBuffer` | 1,000.266 | 45.14% |
| `java.util.ArrayList$Itr` | 513.627 | 23.18% |
| `java.nio.StringCharBuffer` | 324.521 | 14.64% |
| `java.util.Collections$UnmodifiableCollection$1` | 203.708 | 9.19% |
| `byte[]` | 171.308 | 7.73% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter` | 0.999 | 0.05% |
| `java.lang.Object[]` | 0.499 | 0.02% |
| `java.util.concurrent.ConcurrentHashMap` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat` | 0.499 | 0.02% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 1,000.266 | 45.14% |
| `java.util.ArrayList.iterator()Ljava/util/Iterator;` | 513.627 | 23.18% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 324.521 | 14.64% |
| `java.util.Collections$UnmodifiableCollection.iterator()Ljava/util/Iterator;` | 203.708 | 9.19% |
| `java.nio.HeapByteBuffer.<init>(IILjava/lang/foreign/MemorySegment;)V` | 170.809 | 7.71% |
| `jdk.internal.misc.Unsafe.allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;` | 0.999 | 0.05% |
| `java.io.DataOutputStream.<init>(Ljava/io/OutputStream;)V` | 0.499 | 0.02% |
| `java.util.ArrayList.<init>(Ljava/util/Collection;)V` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.rio.RioConfig.<init>()V` | 0.499 | 0.02% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 0.499 | 0.02% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 2,215.927 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 2,215.927 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,215.927 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006205d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,215.927 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006205d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 2,215.927 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,215.927 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 2,215.927 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 2,215.927 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 2,215.927 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 2,215.927 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,215.927 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 2,215.927 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 2,215.927 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 2,215.927 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,215.927 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 2,215.927 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 2,215.927 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 2,215.927 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,208.935 | 99.68% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 2,208.935 | 99.68% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 1,495.596 | 67.49% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 1,491.600 | 67.31% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 1,491.600 | 67.31% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 1,171.075 | 52.85% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;` | 1,171.075 | 52.85% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
