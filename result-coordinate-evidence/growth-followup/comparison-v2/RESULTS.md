# Finite memo growth comparison

All 54 scheduled JVMs, 702 queries and 270 measured queries passed the frozen result, route, ownership and cleanup gates.

The primary estimator pairs the five-query median of each JVM within its round and fixture. The secondary sensitivity uses the arithmetic mean of the same five samples; it was specified before this cohort and never replaces the primary estimator. Ratios below one favor the numerator.

| Fixture | Comparison | Primary latency | Secondary mean latency | Primary allocation | Caller allocation | Direction differs |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| early-cold-hot | fixed/adaptive | 0.819682 | 0.795537 | 1.019988 | 1.019989 | False |
| early-cold-hot | adaptive/ordinary | 1.316173 | 1.276840 | 0.960127 | 0.960166 | False |
| early-cold-hot | fixed/ordinary | 0.915258 | 0.966135 | 0.966071 | 0.966154 | False |
| late-cold-hot | fixed/adaptive | 1.012979 | 1.025954 | 0.995654 | 0.995603 | False |
| late-cold-hot | adaptive/ordinary | 0.931323 | 0.849472 | 0.992061 | 0.992059 | False |
| late-cold-hot | fixed/ordinary | 0.915578 | 0.890380 | 0.984530 | 0.984518 | False |
| warm-cycle-8192 | fixed/adaptive | 0.998332 | 0.947475 | 1.014025 | 1.014026 | False |
| warm-cycle-8192 | adaptive/ordinary | 0.858811 | 0.864861 | 0.981340 | 0.981338 | False |
| warm-cycle-8192 | fixed/ordinary | 0.869390 | 0.843682 | 0.995230 | 0.995230 | False |

## Prespecified mechanism measurements

Whole-query elapsed latency remains the primary outcome. Tail elapsed time, caller CPU and caller allocation are secondary measurements of the interval after the declared prefix has been consumed. Caller CPU excludes GC, JIT and other worker threads. Ratios below one favor the numerator.

| Fixture | Comparison | Total caller CPU | Tail elapsed | Tail caller CPU | Tail caller allocation |
| --- | --- | ---: | ---: | ---: | ---: |
| early-cold-hot | fixed/adaptive | 0.804497 | 0.793423 | 0.819152 | 1.000000 |
| early-cold-hot | adaptive/ordinary | 1.344536 | 0.944555 | 0.943675 | 0.701970 |
| early-cold-hot | fixed/ordinary | 0.930004 | 0.730111 | 0.729610 | 0.701970 |
| late-cold-hot | fixed/adaptive | 1.000410 | 1.049416 | 1.045735 | 0.866081 |
| late-cold-hot | adaptive/ordinary | 0.937038 | 0.738701 | 0.755137 | 0.810984 |
| late-cold-hot | fixed/ordinary | 0.923034 | 0.755374 | 0.755861 | 0.702378 |
| warm-cycle-8192 | fixed/adaptive | 0.997241 | 1.358159 | 1.358229 | 1.139152 |
| warm-cycle-8192 | adaptive/ordinary | 0.866021 | 0.817867 | 0.846112 | 0.855082 |
| warm-cycle-8192 | fixed/ordinary | 0.866748 | 1.113872 | 1.115004 | 0.973991 |

## Every paired observation

| Fixture | Comparison | Estimator / metric | Round 1 | Round 2 | Round 3 | Round 4 | Round 5 | Round 6 |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| early-cold-hot | fixed/adaptive | primary/totalNs | 1.054611 | 0.806233 | 0.492549 | 0.538328 | 0.902253 | 0.833131 |
| early-cold-hot | fixed/adaptive | secondaryMeanSensitivity/totalNs | 1.123083 | 0.850113 | 0.583297 | 0.603916 | 0.754181 | 0.836894 |
| early-cold-hot | fixed/adaptive | primary/allThreadAllocatedBytes | 1.001536 | 1.038440 | 0.978556 | 0.971508 | 1.039646 | 1.074173 |
| early-cold-hot | fixed/adaptive | primary/callerThreadAllocatedBytes | 1.001624 | 1.038353 | 0.978554 | 0.971591 | 1.039650 | 1.074274 |
| early-cold-hot | fixed/adaptive | primary/callerCpuNs | 1.132857 | 0.799909 | 0.499119 | 0.548634 | 0.887912 | 0.809085 |
| early-cold-hot | fixed/adaptive | primary/tailConsumeNs | 0.780475 | 0.806372 | 0.740763 | 0.534693 | 1.220319 | 1.121877 |
| early-cold-hot | fixed/adaptive | primary/callerCpuTailNs | 0.830686 | 0.807619 | 0.740346 | 0.534329 | 1.220308 | 0.896341 |
| early-cold-hot | fixed/adaptive | primary/callerAllocatedTailBytes | 1.000000 | 1.000003 | 1.000000 | 1.000000 | 1.000000 | 1.000003 |
| early-cold-hot | adaptive/ordinary | primary/totalNs | 0.687505 | 1.021253 | 1.853753 | 1.704264 | 1.331310 | 1.301036 |
| early-cold-hot | adaptive/ordinary | secondaryMeanSensitivity/totalNs | 0.553291 | 0.941390 | 1.814966 | 1.573676 | 1.301946 | 1.251734 |
| early-cold-hot | adaptive/ordinary | primary/allThreadAllocatedBytes | 0.979195 | 0.959671 | 0.960583 | 0.951628 | 0.980788 | 0.885745 |
| early-cold-hot | adaptive/ordinary | primary/callerThreadAllocatedBytes | 0.979195 | 0.959752 | 0.960581 | 0.951624 | 0.980783 | 0.885735 |
| early-cold-hot | adaptive/ordinary | primary/callerCpuNs | 0.652969 | 1.006228 | 1.875887 | 1.683670 | 1.357475 | 1.331597 |
| early-cold-hot | adaptive/ordinary | primary/tailConsumeNs | 0.630900 | 0.894336 | 0.858691 | 1.382206 | 0.994774 | 1.175641 |
| early-cold-hot | adaptive/ordinary | primary/callerCpuTailNs | 0.590762 | 0.892579 | 0.858819 | 1.381840 | 0.994771 | 1.175665 |
| early-cold-hot | adaptive/ordinary | primary/callerAllocatedTailBytes | 0.701970 | 0.701970 | 0.701970 | 0.701970 | 0.701970 | 0.701969 |
| early-cold-hot | fixed/ordinary | primary/totalNs | 0.725050 | 0.823367 | 0.913063 | 0.917453 | 1.201178 | 1.083934 |
| early-cold-hot | fixed/ordinary | secondaryMeanSensitivity/totalNs | 0.621392 | 0.800288 | 1.058664 | 0.950368 | 0.981903 | 1.047568 |
| early-cold-hot | fixed/ordinary | primary/allThreadAllocatedBytes | 0.980699 | 0.996560 | 0.939985 | 0.924514 | 1.019672 | 0.951443 |
| early-cold-hot | fixed/ordinary | primary/callerThreadAllocatedBytes | 0.980785 | 0.996561 | 0.939980 | 0.924589 | 1.019670 | 0.951523 |
| early-cold-hot | fixed/ordinary | primary/callerCpuNs | 0.739721 | 0.804890 | 0.936290 | 0.923718 | 1.205319 | 1.077375 |
| early-cold-hot | fixed/ordinary | primary/tailConsumeNs | 0.492402 | 0.721168 | 0.636087 | 0.739055 | 1.213942 | 1.318924 |
| early-cold-hot | fixed/ordinary | primary/callerCpuTailNs | 0.490738 | 0.720864 | 0.635824 | 0.738356 | 1.213927 | 1.053797 |
| early-cold-hot | fixed/ordinary | primary/callerAllocatedTailBytes | 0.701970 | 0.701972 | 0.701970 | 0.701970 | 0.701970 | 0.701971 |
| late-cold-hot | fixed/adaptive | primary/totalNs | 0.663444 | 1.099493 | 0.740786 | 1.021212 | 1.004745 | 1.186715 |
| late-cold-hot | fixed/adaptive | secondaryMeanSensitivity/totalNs | 0.626201 | 1.304976 | 0.841092 | 0.994578 | 1.057330 | 1.288730 |
| late-cold-hot | fixed/adaptive | primary/allThreadAllocatedBytes | 0.966741 | 0.943270 | 1.009435 | 1.004036 | 1.057067 | 0.987271 |
| late-cold-hot | fixed/adaptive | primary/callerThreadAllocatedBytes | 0.966739 | 0.943345 | 1.009437 | 1.003936 | 1.056970 | 0.987271 |
| late-cold-hot | fixed/adaptive | primary/callerCpuNs | 0.666435 | 1.097629 | 0.759056 | 1.017006 | 0.983815 | 1.195025 |
| late-cold-hot | fixed/adaptive | primary/tailConsumeNs | 0.801915 | 1.308616 | 0.934828 | 1.093083 | 1.219686 | 1.005750 |
| late-cold-hot | fixed/adaptive | primary/callerCpuTailNs | 0.802418 | 1.308638 | 0.938306 | 1.085447 | 1.246162 | 1.006024 |
| late-cold-hot | fixed/adaptive | primary/callerAllocatedTailBytes | 0.866079 | 0.866082 | 0.866098 | 0.866085 | 0.866065 | 0.866032 |
| late-cold-hot | adaptive/ordinary | primary/totalNs | 1.272791 | 0.624381 | 0.770030 | 1.031166 | 1.062109 | 0.831480 |
| late-cold-hot | adaptive/ordinary | secondaryMeanSensitivity/totalNs | 1.312016 | 0.643188 | 0.651019 | 0.946547 | 1.072987 | 0.752396 |
| late-cold-hot | adaptive/ordinary | primary/allThreadAllocatedBytes | 1.010723 | 0.992455 | 0.991666 | 1.002660 | 0.938402 | 0.973370 |
| late-cold-hot | adaptive/ordinary | primary/callerThreadAllocatedBytes | 1.010797 | 0.992455 | 0.991663 | 1.002658 | 0.938398 | 0.973367 |
| late-cold-hot | adaptive/ordinary | primary/callerCpuNs | 1.267240 | 0.650650 | 0.754737 | 1.035989 | 1.082144 | 0.838086 |
| late-cold-hot | adaptive/ordinary | primary/tailConsumeNs | 0.774297 | 0.652189 | 0.703106 | 0.808945 | 1.049093 | 0.558266 |
| late-cold-hot | adaptive/ordinary | primary/callerCpuTailNs | 0.815851 | 0.652172 | 0.701545 | 0.808730 | 1.026796 | 0.558106 |
| late-cold-hot | adaptive/ordinary | primary/callerAllocatedTailBytes | 0.810980 | 0.810983 | 0.810969 | 0.810999 | 0.810999 | 0.810985 |
| late-cold-hot | fixed/ordinary | primary/totalNs | 0.844426 | 0.686502 | 0.570427 | 1.053039 | 1.067149 | 0.986730 |
| late-cold-hot | fixed/ordinary | secondaryMeanSensitivity/totalNs | 0.821586 | 0.839345 | 0.547567 | 0.941415 | 1.134502 | 0.969636 |
| late-cold-hot | fixed/ordinary | primary/allThreadAllocatedBytes | 0.977107 | 0.936153 | 1.001023 | 1.006707 | 0.991954 | 0.960980 |
| late-cold-hot | fixed/ordinary | primary/callerThreadAllocatedBytes | 0.977177 | 0.936227 | 1.001021 | 1.006604 | 0.991858 | 0.960977 |
| late-cold-hot | fixed/ordinary | primary/callerCpuNs | 0.844533 | 0.714173 | 0.572888 | 1.053607 | 1.064629 | 1.001534 |
| late-cold-hot | fixed/ordinary | primary/tailConsumeNs | 0.620921 | 0.853465 | 0.657282 | 0.884245 | 1.279564 | 0.561475 |
| late-cold-hot | fixed/ordinary | primary/callerCpuTailNs | 0.654653 | 0.853457 | 0.658264 | 0.877833 | 1.279555 | 0.561468 |
| late-cold-hot | fixed/ordinary | primary/callerAllocatedTailBytes | 0.702373 | 0.702378 | 0.702379 | 0.702394 | 0.702378 | 0.702339 |
| warm-cycle-8192 | fixed/adaptive | primary/totalNs | 1.906999 | 0.464733 | 0.809707 | 0.970682 | 1.508553 | 1.025982 |
| warm-cycle-8192 | fixed/adaptive | secondaryMeanSensitivity/totalNs | 1.789834 | 0.484495 | 0.892540 | 1.002410 | 1.399433 | 0.843380 |
| warm-cycle-8192 | fixed/adaptive | primary/allThreadAllocatedBytes | 1.032292 | 1.027644 | 0.971202 | 0.995913 | 1.014016 | 1.014034 |
| warm-cycle-8192 | fixed/adaptive | primary/callerThreadAllocatedBytes | 1.032294 | 1.027648 | 0.971199 | 0.995913 | 1.014018 | 1.014034 |
| warm-cycle-8192 | fixed/adaptive | primary/callerCpuNs | 1.853691 | 0.458056 | 0.818815 | 0.984724 | 1.480812 | 1.009758 |
| warm-cycle-8192 | fixed/adaptive | primary/tailConsumeNs | 2.360095 | 0.570783 | 1.338711 | 1.050705 | 1.377606 | 1.431701 |
| warm-cycle-8192 | fixed/adaptive | primary/callerCpuTailNs | 2.353868 | 0.570802 | 1.338854 | 1.050609 | 1.377604 | 1.399099 |
| warm-cycle-8192 | fixed/adaptive | primary/callerAllocatedTailBytes | 1.139059 | 1.139058 | 1.139369 | 1.139990 | 1.139055 | 1.139246 |
| warm-cycle-8192 | adaptive/ordinary | primary/totalNs | 0.713648 | 1.696631 | 1.173635 | 0.696106 | 1.003974 | 0.585928 |
| warm-cycle-8192 | adaptive/ordinary | secondaryMeanSensitivity/totalNs | 0.742069 | 1.551269 | 1.048447 | 0.698158 | 0.987653 | 0.709919 |
| warm-cycle-8192 | adaptive/ordinary | primary/allThreadAllocatedBytes | 0.981340 | 0.991461 | 0.981313 | 0.999450 | 0.981340 | 0.941645 |
| warm-cycle-8192 | adaptive/ordinary | primary/callerThreadAllocatedBytes | 0.981338 | 0.991459 | 0.981312 | 0.999450 | 0.981339 | 0.941724 |
| warm-cycle-8192 | adaptive/ordinary | primary/callerCpuNs | 0.726898 | 1.691556 | 1.170800 | 0.707293 | 1.005144 | 0.595603 |
| warm-cycle-8192 | adaptive/ordinary | primary/tailConsumeNs | 0.697430 | 1.638330 | 1.005683 | 0.653047 | 0.938304 | 0.541421 |
| warm-cycle-8192 | adaptive/ordinary | primary/callerCpuTailNs | 0.752272 | 1.638266 | 1.005571 | 0.653045 | 0.939952 | 0.589735 |
| warm-cycle-8192 | adaptive/ordinary | primary/callerAllocatedTailBytes | 0.855082 | 0.855082 | 0.854854 | 0.856405 | 0.855085 | 0.854947 |
| warm-cycle-8192 | fixed/ordinary | primary/totalNs | 1.360927 | 0.788480 | 0.950301 | 0.675697 | 1.514548 | 0.601151 |
| warm-cycle-8192 | fixed/ordinary | secondaryMeanSensitivity/totalNs | 1.328180 | 0.751583 | 0.935781 | 0.699841 | 1.382154 | 0.598731 |
| warm-cycle-8192 | fixed/ordinary | primary/allThreadAllocatedBytes | 1.013029 | 1.018868 | 0.953054 | 0.995364 | 0.995095 | 0.954860 |
| warm-cycle-8192 | fixed/ordinary | primary/callerThreadAllocatedBytes | 1.013029 | 1.018871 | 0.953049 | 0.995365 | 0.995095 | 0.954941 |
| warm-cycle-8192 | fixed/ordinary | primary/callerCpuNs | 1.347444 | 0.774827 | 0.958668 | 0.696488 | 1.488429 | 0.601414 |
| warm-cycle-8192 | fixed/ordinary | primary/tailConsumeNs | 1.646002 | 0.935130 | 1.346318 | 0.686159 | 1.292613 | 0.775153 |
| warm-cycle-8192 | fixed/ordinary | primary/callerCpuTailNs | 1.770748 | 0.935126 | 1.346313 | 0.686095 | 1.294882 | 0.825097 |
| warm-cycle-8192 | fixed/ordinary | primary/callerAllocatedTailBytes | 0.973988 | 0.973988 | 0.973994 | 0.976293 | 0.973988 | 0.973995 |

## Absolute query measurements

Values below are medians of the six process medians, not pooled query medians.

| Fixture | Arm | Total ms | Allocation bytes | Prepare ms | Evaluate ms | Consume ms | Close ms | Ledger peak bytes | Source calls |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| early-cold-hot | ordinary | 630.3846 | 302286536 | 1.3808 | 6.0074 | 622.5272 | 0.1903 | 1152 | 131072 |
| early-cold-hot | adaptive | 858.0455 | 293155008 | 1.7343 | 8.6293 | 841.6423 | 0.2240 | 606448 | 4128 |
| early-cold-hot | fixed | 662.6823 | 293367376 | 1.4373 | 6.2144 | 651.8046 | 0.1694 | 606448 | 4128 |
| late-cold-hot | ordinary | 703.1233 | 326931916 | 1.3463 | 6.5496 | 696.5251 | 0.1532 | 1152 | 131072 |
| late-cold-hot | adaptive | 621.5885 | 322892488 | 1.2073 | 6.5406 | 610.9606 | 0.2115 | 14549472 | 32832 |
| late-cold-hot | fixed | 655.5779 | 323447656 | 1.0225 | 4.9584 | 638.8520 | 0.1341 | 606448 | 32832 |
| warm-cycle-8192 | ordinary | 1057.2373 | 350844464 | 1.5562 | 7.2845 | 1046.2212 | 0.1790 | 1152 | 131072 |
| warm-cycle-8192 | adaptive | 818.9063 | 344297620 | 1.3641 | 7.8647 | 809.1890 | 0.1962 | 14549472 | 32566 |
| warm-cycle-8192 | fixed | 867.0796 | 349139884 | 1.4487 | 6.7484 | 856.6212 | 0.2011 | 606448 | 107656 |

## Caller phase decomposition

Up to four bounded snapshots: query start, prefix boundary, consumption end, query end; to-prefix includes prepare/evaluate; FULL consumes prefix, RETAINED may defer reads; tail includes final exhaustion hasNext and automatic cleanup, ending before explicit result/connection close; after-consume includes explicit close and retained reads; allocation then CPU then elapsed timestamp

In the selected full consumer, the prefix is fully consumed. Tail includes the final exhaustion hasNext and any automatic cleanup it triggers; it ends before explicit result/connection close. Retained mode would defer some consumption.

All-thread allocation ends after selection-capture cleanup; aligned caller allocation ends before that finally block. Their difference is not a pure worker-allocation estimate.

Each raw query reconciles its caller CPU and allocation phases exactly. Medians of independently aggregated phases need not sum to the independently aggregated total.

| Fixture | Arm | Caller CPU ms | CPU to prefix ms | CPU tail ms | CPU after consume ms | Allocation to prefix bytes | Allocation tail bytes | Allocation after consume bytes |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| early-cold-hot | ordinary | 609.6952 | 510.7314 | 82.2151 | 0.1792 | 261369252 | 40889672 | 968 |
| early-cold-hot | adaptive | 845.0349 | 761.1100 | 80.5528 | 0.2281 | 264424608 | 28703304 | 968 |
| early-cold-hot | fixed | 649.3904 | 571.4064 | 64.0998 | 0.1729 | 264662436 | 28703304 | 968 |
| late-cold-hot | ordinary | 697.5811 | 633.1593 | 63.7912 | 0.1640 | 295218456 | 31686248 | 968 |
| late-cold-hot | adaptive | 619.3326 | 576.8877 | 46.0564 | 0.2153 | 297168016 | 25697000 | 968 |
| late-cold-hot | fixed | 653.4050 | 591.0943 | 52.6813 | 0.1420 | 301164668 | 22255720 | 968 |
| warm-cycle-8192 | ordinary | 1036.7448 | 919.1908 | 116.4577 | 0.1929 | 310229556 | 40587876 | 968 |
| warm-cycle-8192 | adaptive | 812.3149 | 735.2470 | 83.9108 | 0.1999 | 309564624 | 34705968 | 968 |
| warm-cycle-8192 | fixed | 858.8760 | 750.9296 | 107.8637 | 0.1594 | 309564284 | 39532224 | 968 |

The JSON also contains first-row and phase timing, retained-read time, cold first-query measurements, all owner counter ranges and totals, and the natural-GC summary of each JVM.

## Interpretation limits

Descriptive fixed-capacity growth ablation; no significance claim or production-promotion gate. Preserved late reuse is a hypothesis, not a performance acceptance gate.

Ledger peak is the process-authority accounting peak, not a separate query-ledger peak or total retained heap. Weak metadata reservations are reported independently.

Whole-JVM natural GC log, including setup and warmups. Ordered wrappers retain Values; this cohort does not isolate weak collection or useful-key rematerialization.

collectedMisses counts cleared occupied slots before key comparison, not necessarily additional same-key source calls. Occupancy at close is not a count of live weak referents. Both enabled arms use always admission. Fixed mode skips growth; adaptive retains cumulative hit/collision rules. Equal late-case reuse is an outcome to report, never a sample acceptance gate.

Fresh controls only. No historical, diagnostic, warmup or incomplete-cohort pooling.
