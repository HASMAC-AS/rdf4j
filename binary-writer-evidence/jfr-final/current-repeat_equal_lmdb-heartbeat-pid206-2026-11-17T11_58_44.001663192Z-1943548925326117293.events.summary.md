# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-final/current-repeat_equal_lmdb-heartbeat-pid206-2026-11-17T11_58_44.001663192Z-1943548925326117293.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 1,157 | 1,154 | 99.74% |
| Allocation samples | 114 | 114 | 100.00% |
| Allocation weight, bytes | 76,258,560 | 76,258,560 | 100.00% |

Selected CPU samples with stacks: 1,154; marked truncated: 0. Selected allocation samples with stacks: 114; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 194 | 16.81% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 171 | 14.82% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 155 | 13.43% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.checkDatatypeVersion()V` | 71 | 6.15% |
| `java.io.DataOutputStream.write([BII)V` | 64 | 5.55% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.size()I` | 63 | 5.46% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 62 | 5.37% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 40 | 3.47% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 40 | 3.47% |
| `java.util.ImmutableCollections$ListN.indexOf(Ljava/lang/Object;)I` | 35 | 3.03% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeMarkers(I)V` | 34 | 2.95% |
| `java.util.Arrays$ArrayList.get(I)Ljava/lang/Object;` | 27 | 2.34% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 21 | 1.82% |
| `java.util.Objects.equals(Ljava/lang/Object;Ljava/lang/Object;)Z` | 18 | 1.56% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbValue$InternalIdentity.revision()Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;` | 17 | 1.47% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.stableDatatype()Z` | 15 | 1.30% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbValue$InternalIdentity.id()J` | 14 | 1.21% |
| `java.util.Arrays$ArrayItr.next()Ljava/lang/Object;` | 11 | 0.95% |
| `java.util.Objects.checkIndex(II)I` | 11 | 0.95% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.equalsNative(Ljava/lang/Object;)Ljava/lang/Boolean;` | 10 | 0.87% |
| `java.util.concurrent.ConcurrentHashMap.get(Ljava/lang/Object;)Ljava/lang/Object;` | 7 | 0.61% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.getSemanticVersion()J` | 6 | 0.52% |
| `java.io.ByteArrayOutputStream.write(I)V` | 5 | 0.43% |
| `java.util.Collections$UnmodifiableCollection.size()I` | 5 | 0.43% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 5 | 0.43% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 1,154 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 1,154 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 1,154 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 1,154 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 1,154 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 1,153 | 99.91% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 1,153 | 99.91% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,153 | 99.91% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,152 | 99.83% |
| `java.lang.invoke.LambdaForm$MH.0x000000001e05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,152 | 99.83% |
| `java.lang.invoke.LambdaForm$MH.0x000000001e05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,152 | 99.83% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,152 | 99.83% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,152 | 99.83% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,152 | 99.83% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,152 | 99.83% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 1,152 | 99.83% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 1,147 | 99.39% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 1,146 | 99.31% |
| `org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter.handleSolution(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,127 | 97.66% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 1,122 | 97.23% |
| `org.eclipse.rdf4j.query.impl.ListBindingSet.getValue(Ljava/lang/String;)Lorg/eclipse/rdf4j/model/Value;` | 313 | 27.12% |
| `java.io.DataOutputStream.write([BII)V` | 305 | 26.43% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat$HeartbeatOutputStream.write([BII)V` | 237 | 20.54% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 234 | 20.28% |
| `java.util.ImmutableCollections$ListN.indexOf(Ljava/lang/Object;)I` | 224 | 19.41% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `byte[]` | 72.726 | 100.00% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.<init>(Ljava/io/OutputStream;)V` | 72.726 | 100.00% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 72.726 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 72.726 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.newInvokeSpecial(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.lang.invoke.Invokers$Holder.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001e05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001e05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000001e129800.invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.lang.reflect.Constructor.newInstance([Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.lang.reflect.Constructor.newInstanceWithCaller([Ljava/lang/Object;ZLjava/lang/Class;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 72.726 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 72.726 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 72.726 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 72.726 | 100.00% |
| `jdk.internal.reflect.DirectConstructorHandleAccessor.invokeImpl([Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance([Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 72.726 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.<init>(Ljava/io/OutputStream;)V` | 72.726 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 72.726 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 72.726 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 72.726 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 72.726 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 72.726 | 100.00% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
