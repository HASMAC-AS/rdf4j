package org.eclipse.rdf4j.sail.lmdb.evaluation;
public class ExperimentalComparisonSixProbe {
 public static void main(String[] args) throws Exception {
  MainComparisonSixBenchmark.main(args);
  long groups = NativeKernelGrouping.TRACKERS.get();
  long hits = NativeValueMemo.HITS.get();
  System.out.printf("experimentWitness grouping=%d checks=%d memoProbes=%d memoHits=%d%n", groups, NativeKernelGrouping.CHECKS.get(), NativeValueMemo.PROBES.get(), hits);
  if (hits == 0) throw new AssertionError("Memo did not hit");
  if (!Boolean.getBoolean("comparison.narrow") && groups == 0) throw new AssertionError("Grouping did not hit");
 }
}
