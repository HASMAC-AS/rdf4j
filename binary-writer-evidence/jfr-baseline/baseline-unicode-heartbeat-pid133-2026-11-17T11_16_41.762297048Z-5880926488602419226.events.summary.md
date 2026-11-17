# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-baseline/baseline-unicode-heartbeat-pid133-2026-11-17T11_16_41.762297048Z-5880926488602419226.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 1,114 | 1,110 | 99.64% |
| Allocation samples | 3,567 | 3,563 | 99.89% |
| Allocation weight, bytes | 10,425,635,480 | 10,396,204,968 | 99.72% |

Selected CPU samples with stacks: 1,110; marked truncated: 0. Selected allocation samples with stacks: 3,563; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 426 | 38.38% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 171 | 15.41% |
| `java.nio.HeapByteBuffer.put(B)Ljava/nio/ByteBuffer;` | 80 | 7.21% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 69 | 6.22% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 44 | 3.96% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeNamespace(Ljava/lang/String;)Ljava/lang/Integer;` | 35 | 3.15% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 33 | 2.97% |
| `java.nio.ByteBuffer.limit(I)Ljava/nio/Buffer;` | 32 | 2.88% |
| `jdk.internal.util.ArraysSupport.hashCodeOfUnsigned([BIII)I` | 25 | 2.25% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 23 | 2.07% |
| `sun.nio.cs.UTF_8$Encoder.encodeBufferLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 22 | 1.98% |
| `java.nio.Buffer.limit(I)Ljava/nio/Buffer;` | 18 | 1.62% |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 18 | 1.62% |
| `java.nio.StringCharBuffer.get()C` | 14 | 1.26% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeBNode(Lorg/eclipse/rdf4j/model/BNode;)V` | 14 | 1.26% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;` | 12 | 1.08% |
| `org.eclipse.rdf4j.model.impl.SimpleLiteral.equals(Ljava/lang/Object;)Z` | 12 | 1.08% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 11 | 0.99% |
| `sun.nio.cs.Surrogate$Parser.parse(CLjava/nio/CharBuffer;)I` | 8 | 0.72% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;Z)Ljava/nio/charset/CoderResult;` | 7 | 0.63% |
| `java.lang.StringLatin1.hashCode([B)I` | 6 | 0.54% |
| `sun.nio.cs.UTF_8$Encoder.encodeLoop(Ljava/nio/CharBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/charset/CoderResult;` | 6 | 0.54% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 4 | 0.36% |
| `jdk.internal.misc.Unsafe.allocateUninitializedArray(Ljava/lang/Class;I)Ljava/lang/Object;` | 3 | 0.27% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 3 | 0.27% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 1,110 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 1,110 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,110 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001605d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,110 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001605d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,110 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,110 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 1,110 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 1,110 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 1,110 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 1,110 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,110 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,110 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,110 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,110 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 1,110 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 1,110 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,110 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 1,110 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 1,110 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,110 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 1,033 | 93.06% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 689 | 62.07% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 639 | 57.57% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeQName(Lorg/eclipse/rdf4j/model/IRI;)V` | 556 | 50.09% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;)Ljava/nio/CharBuffer;` | 426 | 38.38% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.HeapByteBuffer` | 4,302.379 | 43.39% |
| `java.nio.StringCharBuffer` | 2,396.089 | 24.17% |
| `byte[]` | 2,304.634 | 23.24% |
| `java.lang.String` | 824.059 | 8.31% |
| `java.lang.Integer` | 46.947 | 0.47% |
| `java.util.HashMap$Node` | 26.460 | 0.27% |
| `java.util.HashMap$Node[]` | 14.026 | 0.14% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 4,302.379 | 43.39% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 2,396.089 | 24.17% |
| `java.nio.HeapByteBuffer.<init>(IILjava/lang/foreign/MemorySegment;)V` | 1,729.457 | 17.44% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 546.376 | 5.51% |
| `java.util.Arrays.copyOfRange([BII)[B` | 489.789 | 4.94% |
| `java.lang.StringConcatHelper.doConcat(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | 277.683 | 2.80% |
| `jdk.internal.misc.Unsafe.allocateUninitializedArray(Ljava/lang/Class;I)Ljava/lang/Object;` | 85.388 | 0.86% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeNamespace(Ljava/lang/String;)Ljava/lang/Integer;` | 46.947 | 0.47% |
| `java.util.HashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 26.460 | 0.27% |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 14.026 | 0.14% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 9,914.594 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 9,914.594 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,914.594 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001605d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,914.594 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001605d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,914.594 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,914.594 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 9,914.594 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 9,914.594 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 9,914.594 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 9,914.594 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,914.594 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,914.594 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 9,914.594 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 9,914.594 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 9,914.594 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 9,914.594 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 9,914.594 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 9,914.594 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 9,822.269 | 99.07% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 9,822.269 | 99.07% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 9,822.269 | 99.07% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 8,427.925 | 85.01% |
| `org.eclipse.rdf4j.query.resultio.binary.OriginalBinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 7,856.016 | 79.24% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 6,031.836 | 60.84% |
| `java.nio.charset.CharsetEncoder.encode(Ljava/nio/CharBuffer;)Ljava/nio/ByteBuffer;` | 6,031.836 | 60.84% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
