# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/current-long_unicode-heartbeat-pid164-2026-11-17T13_29_42.411647895Z-13585777093402508918.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 984 | 981 | 99.70% |
| Allocation samples | 240 | 239 | 99.58% |
| Allocation weight, bytes | 417,106,496 | 389,653,600 | 93.42% |

Selected CPU samples with stacks: 981; marked truncated: 0. Selected allocation samples with stacks: 239; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLargeString(Ljava/lang/String;)V` | 356 | 36.29% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.utf8Length(Ljava/lang/String;)I` | 245 | 24.97% |
| `java.lang.String.checkIndex(II)V` | 146 | 14.88% |
| `java.lang.StringUTF16.charAt([BI)C` | 64 | 6.52% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 53 | 5.40% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 14 | 1.43% |
| `java.lang.Character.isSurrogate(C)Z` | 13 | 1.33% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 9 | 0.92% |
| `java.util.Arrays.copyOfRange([BII)[B` | 9 | 0.92% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 8 | 0.82% |
| `java.lang.Character.isLowSurrogate(C)Z` | 8 | 0.82% |
| `java.io.DataOutputStream.write([BII)V` | 7 | 0.71% |
| `java.lang.StringLatin1.hashCode([B)I` | 7 | 0.71% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 5 | 0.51% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 5 | 0.51% |
| `java.lang.Character.isHighSurrogate(C)Z` | 3 | 0.31% |
| `java.lang.Character.toCodePoint(CC)I` | 3 | 0.31% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.encodeString(Ljava/lang/String;II)I` | 3 | 0.31% |
| `java.lang.String.getBytes([BIB)V` | 2 | 0.20% |
| `java.lang.StringConcatHelper.doConcat(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | 2 | 0.20% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 2 | 0.20% |
| `org.eclipse.rdf4j.model.base.AbstractLiteral.equals(Ljava/lang/Object;)Z` | 2 | 0.20% |
| `org.eclipse.rdf4j.model.impl.SimpleLiteral.equals(Ljava/lang/Object;)Z` | 2 | 0.20% |
| `java.lang.StringUTF16.length([B)I` | 1 | 0.10% |
| `java.lang.invoke.DirectMethodHandle$Holder.newInvokeSpecial(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1 | 0.10% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 981 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 981 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 981 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001105d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 981 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001105d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 981 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 981 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 981 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 981 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 981 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 981 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 981 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 981 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 981 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 981 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 981 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 981 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 981 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 981 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 978 | 99.69% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 978 | 99.69% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 951 | 96.94% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 920 | 93.78% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeTypedLiteral(Ljava/lang/String;[B)V` | 903 | 92.05% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 895 | 91.23% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLargeString(Ljava/lang/String;)V` | 894 | 91.13% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `byte[]` | 346.596 | 93.27% |
| `java.lang.String` | 25.006 | 6.73% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLargeString(Ljava/lang/String;)V` | 272.485 | 73.33% |
| `java.util.Arrays.copyOfRange([BII)[B` | 68.226 | 18.36% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 15.141 | 4.07% |
| `java.lang.StringConcatHelper.doConcat(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | 9.865 | 2.65% |
| `jdk.internal.misc.Unsafe.allocateUninitializedArray(Ljava/lang/Class;I)Ljava/lang/Object;` | 5.885 | 1.58% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 371.603 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 371.603 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 371.603 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001105d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 371.603 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001105d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 371.603 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 371.603 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 371.603 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 371.603 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 371.603 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 371.603 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 371.603 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 371.603 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 371.603 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 371.603 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeValue(Lorg/eclipse/rdf4j/model/Value;)V` | 371.603 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 371.603 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 371.603 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 371.603 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 371.603 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 371.603 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 371.603 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLiteral(Lorg/eclipse/rdf4j/model/Literal;)V` | 312.817 | 84.18% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeLargeString(Ljava/lang/String;)V` | 272.485 | 73.33% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 272.485 | 73.33% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeTypedLiteral(Ljava/lang/String;[B)V` | 272.485 | 73.33% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
