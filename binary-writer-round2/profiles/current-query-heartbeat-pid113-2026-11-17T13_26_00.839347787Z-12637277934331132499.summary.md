# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/current-query-heartbeat-pid113-2026-11-17T13_26_00.839347787Z-12637277934331132499.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 846 | 846 | 100.00% |
| Allocation samples | 3,047 | 3,039 | 99.74% |
| Allocation weight, bytes | 11,874,115,200 | 11,850,040,232 | 99.80% |

Selected CPU samples with stacks: 846; marked truncated: 0. Selected allocation samples with stacks: 3,039; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.childLanes(IID)D` | 95 | 11.23% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.estimateDistinct([J)D` | 49 | 5.79% |
| `java.lang.invoke.VarHandleLongs$FieldInstanceReadWrite.weakCompareAndSetRelease(Ljava/lang/invoke/VarHandle;Ljava/lang/Object;JJ)Z` | 32 | 3.78% |
| `java.util.IdentityHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 31 | 3.66% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.getLazyValue(Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ReadSnapshot;JI)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 24 | 2.84% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 23 | 2.72% |
| `java.lang.ref.Reference.<init>(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V` | 17 | 2.01% |
| `java.lang.ThreadLocal.get()Ljava/lang/Object;` | 15 | 1.77% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.meanFanOut(JZ)Ljava/util/OptionalDouble;` | 14 | 1.65% |
| `java.lang.ThreadLocal$ThreadLocalMap.cleanSomeSlots(II)Z` | 13 | 1.54% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.captureReadyOwners(Lorg/eclipse/rdf4j/model/Value;Ljava/util/Map;)V` | 12 | 1.42% |
| `java.lang.ThreadLocal$ThreadLocalMap.getEntry(Ljava/lang/ThreadLocal;)Ljava/lang/ThreadLocal$ThreadLocalMap$Entry;` | 11 | 1.30% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 11 | 1.30% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 10 | 1.18% |
| `java.util.HashMap.putVal(ILjava/lang/Object;Ljava/lang/Object;ZZ)Ljava/lang/Object;` | 10 | 1.18% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 10 | 1.18% |
| `java.util.concurrent.atomic.AtomicLong.incrementAndGet()J` | 9 | 1.06% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 9 | 1.06% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.drainIfIdle()V` | 9 | 1.06% |
| `java.io.DataOutputStream.write([BII)V` | 8 | 0.95% |
| `java.util.IdentityHashMap.hash(Ljava/lang/Object;I)I` | 8 | 0.95% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore$CanonicalValueEntry.isCurrent()Z` | 8 | 0.95% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 7 | 0.83% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 6 | 0.71% |
| `java.lang.ThreadLocal.setInitialValue(Ljava/lang/Thread;)Ljava/lang/Object;` | 6 | 0.71% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 846 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 846 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000007305d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000007305d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 846 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 846 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 846 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 846 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 846 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 846 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 846 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 844 | 99.76% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 844 | 99.76% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 844 | 99.76% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 731 | 86.41% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 731 | 86.41% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 731 | 86.41% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 729 | 86.17% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 714 | 84.40% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 503 | 59.46% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 493 | 58.27% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Object[]` | 4,876.021 | 43.15% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision$Lazy` | 642.024 | 5.68% |
| `byte[]` | 418.501 | 3.70% |
| `java.lang.Runnable[]` | 388.000 | 3.43% |
| `boolean[]` | 385.055 | 3.41% |
| `double[]` | 375.619 | 3.32% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope$RevisionKey` | 360.828 | 3.19% |
| `java.util.LinkedHashMap$Entry` | 327.929 | 2.90% |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext$$Lambda.0x0000000073248680` | 309.630 | 2.74% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x0000000073366568` | 300.114 | 2.66% |
| `java.lang.ThreadLocal$ThreadLocalMap$Entry` | 255.516 | 2.26% |
| `org.eclipse.rdf4j.model.base.AbstractLiteral$DecimalLiteral` | 234.721 | 2.08% |
| `java.math.BigDecimal` | 226.238 | 2.00% |
| `java.util.ArrayDeque` | 224.728 | 1.99% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeProjectedBindingSet` | 192.274 | 1.70% |
| `java.util.LinkedHashMap` | 183.769 | 1.63% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration$$Lambda.0x00000000732d5c50` | 164.786 | 1.46% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$$Lambda.0x000000007335b4a0` | 153.324 | 1.36% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope$$Lambda.0x0000000073247528` | 147.830 | 1.31% |
| `org.eclipse.rdf4j.query.impl.SimpleBinding` | 142.335 | 1.26% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$TreeChoice` | 105.836 | 0.94% |
| `java.lang.String` | 83.331 | 0.74% |
| `int[]` | 81.404 | 0.72% |
| `org.eclipse.rdf4j.sail.features.SailResultValueExtractor$1$$Lambda.0x000000007335ba98` | 76.890 | 0.68% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$EdgePlan` | 69.402 | 0.61% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.ImmutableCollections$SetN.<init>([Ljava/lang/Object;)V` | 3,015.918 | 26.69% |
| `java.lang.invoke.DirectMethodHandle.allocateInstance(Ljava/lang/Object;)Ljava/lang/Object;` | 1,153.097 | 10.20% |
| `java.util.IdentityHashMap.init(I)V` | 787.877 | 6.97% |
| `java.util.ArrayDeque.<init>()V` | 731.638 | 6.47% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.positionedRevision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;I)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;` | 642.024 | 5.68% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 388.000 | 3.43% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.isPrepared(Lorg/eclipse/rdf4j/model/Value;)Z` | 379.561 | 3.36% |
| `java.util.Arrays.copyOfRange([BII)[B` | 373.560 | 3.31% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.revision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision$Lazy;)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision$Lazy;` | 360.828 | 3.19% |
| `java.util.ArrayList.grow(I)[Ljava/lang/Object;` | 339.091 | 3.00% |
| `java.util.LinkedHashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 327.929 | 2.90% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 255.516 | 2.26% |
| `org.eclipse.rdf4j.model.base.AbstractValueFactory.createLiteral(Ljava/math/BigDecimal;)Lorg/eclipse/rdf4j/model/Literal;` | 234.721 | 2.08% |
| `org.eclipse.rdf4j.sail.lmdb.inlined.Decimals.unpackDecimal(JLorg/eclipse/rdf4j/model/ValueFactory;)Lorg/eclipse/rdf4j/model/Literal;` | 226.238 | 2.00% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.captureReadyOwners(Lorg/eclipse/rdf4j/model/Value;Ljava/util/Map;)V` | 224.728 | 1.99% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.recurse(IJ[I[I[I[DDI)V` | 220.245 | 1.95% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 215.733 | 1.91% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeRowsStep.project([JLorg/eclipse/rdf4j/sail/lmdb/evaluation/AggContext;Lorg/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet$ProjectionLayout;)Lorg/eclipse/rdf4j/query/BindingSet;` | 192.274 | 1.70% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeCostVector.<init>([D[D[DZ)V` | 164.833 | 1.46% |
| `org.eclipse.rdf4j.query.MutableBindingSet.addBinding(Ljava/lang/String;Lorg/eclipse/rdf4j/model/Value;)V` | 142.335 | 1.26% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.consider([I[I[DDI)V` | 141.791 | 1.25% |
| `jdk.internal.misc.Unsafe.allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;` | 78.388 | 0.69% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 76.339 | 0.68% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner.orient(Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;II)Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;` | 69.402 | 0.61% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.setInternalID(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;)V` | 65.925 | 0.58% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 11,301.079 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 11,301.079 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 11,301.079 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000007305d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 11,301.079 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000007305d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 11,301.079 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 11,301.079 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 11,301.079 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 11,301.079 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 11,301.079 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 11,301.079 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 11,301.079 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 11,301.079 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 11,301.079 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 11,301.079 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 11,301.079 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 11,301.079 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 11,301.079 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 11,301.079 | 100.00% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 7,793.311 | 68.96% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 7,793.311 | 68.96% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 7,793.311 | 68.96% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 7,793.311 | 68.96% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 7,793.311 | 68.96% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 3,306.856 | 29.26% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 3,306.856 | 29.26% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
