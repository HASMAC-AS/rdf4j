# Finite phase admission comparison

All 54 scheduled JVMs, 702 queries and 270 measured queries passed the frozen result, route, ownership and cleanup gates.

The primary estimator pairs the five-query median of each JVM within its round and fixture. The secondary sensitivity uses the arithmetic mean of the same five samples; it was specified before this cohort and never replaces the primary estimator. Ratios below one favor the numerator.

| Fixture | Comparison | Primary latency | Secondary mean latency | Primary allocation | Caller allocation | Direction differs |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| early-cold-hot | always/initial | 1.054183 | 1.030475 | 0.966232 | 0.966188 | False |
| early-cold-hot | initial/ordinary | 0.917221 | 0.905032 | 0.970803 | 0.970842 | False |
| early-cold-hot | always/ordinary | 0.941411 | 0.917282 | 0.956331 | 0.956370 | False |
| late-cold-hot | always/initial | 1.135265 | 1.054137 | 0.975300 | 0.975297 | False |
| late-cold-hot | initial/ordinary | 1.077202 | 0.934395 | 0.988908 | 0.988908 | True |
| late-cold-hot | always/ordinary | 1.221997 | 1.052751 | 0.974302 | 0.974300 | False |
| hot-unique | always/initial | 1.000358 | 1.061376 | 1.000000 | 1.000000 | False |
| hot-unique | initial/ordinary | 0.935621 | 0.914078 | 1.002208 | 1.002209 | False |
| hot-unique | always/ordinary | 0.918838 | 0.934844 | 1.002208 | 1.002209 | False |

## Every paired observation

| Fixture | Comparison | Estimator / metric | Round 1 | Round 2 | Round 3 | Round 4 | Round 5 | Round 6 |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| early-cold-hot | always/initial | primary/totalNs | 1.188290 | 1.131337 | 0.764674 | 1.019046 | 0.830065 | 1.089321 |
| early-cold-hot | always/initial | secondaryMeanSensitivity/totalNs | 1.122605 | 0.952593 | 0.682647 | 1.108357 | 0.788687 | 1.260768 |
| early-cold-hot | always/initial | primary/allThreadAllocatedBytes | 0.960978 | 1.018202 | 0.970597 | 0.961866 | 0.961386 | 1.000085 |
| early-cold-hot | always/initial | primary/callerThreadAllocatedBytes | 0.961059 | 1.018203 | 0.970512 | 0.961863 | 0.961466 | 0.999999 |
| early-cold-hot | initial/ordinary | primary/totalNs | 0.872254 | 0.740333 | 1.179993 | 0.962188 | 1.218790 | 0.773917 |
| early-cold-hot | initial/ordinary | secondaryMeanSensitivity/totalNs | 0.918125 | 0.891939 | 1.235359 | 0.725012 | 1.248796 | 0.806930 |
| early-cold-hot | initial/ordinary | primary/allThreadAllocatedBytes | 1.027794 | 0.970580 | 0.971027 | 0.962029 | 1.009154 | 0.942107 |
| early-cold-hot | initial/ordinary | primary/callerThreadAllocatedBytes | 1.027797 | 0.970578 | 0.971106 | 0.962026 | 1.009156 | 0.942183 |
| early-cold-hot | always/ordinary | primary/totalNs | 1.036491 | 0.837567 | 0.902309 | 0.980514 | 1.011675 | 0.843044 |
| early-cold-hot | always/ordinary | secondaryMeanSensitivity/totalNs | 1.030693 | 0.849654 | 0.843314 | 0.803572 | 0.984909 | 1.017351 |
| early-cold-hot | always/ordinary | primary/allThreadAllocatedBytes | 0.987688 | 0.988247 | 0.942475 | 0.925343 | 0.970186 | 0.942186 |
| early-cold-hot | always/ordinary | primary/callerThreadAllocatedBytes | 0.987774 | 0.988245 | 0.942471 | 0.925338 | 0.970269 | 0.942182 |
| late-cold-hot | always/initial | primary/totalNs | 1.124604 | 1.149823 | 1.145926 | 0.913311 | 1.173705 | 0.905713 |
| late-cold-hot | always/initial | secondaryMeanSensitivity/totalNs | 1.140551 | 0.930273 | 1.121858 | 0.980399 | 1.213936 | 0.986415 |
| late-cold-hot | always/initial | primary/allThreadAllocatedBytes | 0.975578 | 0.975021 | 1.013847 | 0.974950 | 0.986817 | 0.974478 |
| late-cold-hot | always/initial | primary/callerThreadAllocatedBytes | 0.975575 | 0.975019 | 1.013846 | 0.975017 | 0.986816 | 0.974476 |
| late-cold-hot | initial/ordinary | primary/totalNs | 1.162813 | 0.909199 | 0.991590 | 1.655329 | 1.201960 | 0.907505 |
| late-cold-hot | initial/ordinary | secondaryMeanSensitivity/totalNs | 0.898666 | 0.905631 | 0.963159 | 1.560433 | 1.186990 | 0.873598 |
| late-cold-hot | initial/ordinary | primary/allThreadAllocatedBytes | 0.998145 | 0.988633 | 0.988411 | 1.047964 | 0.987858 | 0.989182 |
| late-cold-hot | initial/ordinary | primary/callerThreadAllocatedBytes | 0.998145 | 0.988633 | 0.988411 | 1.047894 | 0.987859 | 0.989184 |
| late-cold-hot | always/ordinary | primary/totalNs | 1.307705 | 1.045418 | 1.136288 | 1.511830 | 1.410746 | 0.821939 |
| late-cold-hot | always/ordinary | secondaryMeanSensitivity/totalNs | 1.024974 | 0.842484 | 1.080528 | 1.529847 | 1.440929 | 0.861731 |
| late-cold-hot | always/ordinary | primary/allThreadAllocatedBytes | 0.973769 | 0.963938 | 1.002097 | 1.021712 | 0.974835 | 0.963936 |
| late-cold-hot | always/ordinary | primary/callerThreadAllocatedBytes | 0.973766 | 0.963936 | 1.002097 | 1.021714 | 0.974835 | 0.963936 |
| hot-unique | always/initial | primary/totalNs | 0.994381 | 1.128885 | 1.006335 | 0.977391 | 1.055463 | 0.986460 |
| hot-unique | always/initial | secondaryMeanSensitivity/totalNs | 1.044100 | 1.123643 | 1.109142 | 1.040983 | 1.078652 | 1.004954 |
| hot-unique | always/initial | primary/allThreadAllocatedBytes | 1.000001 | 0.975114 | 1.000000 | 0.986413 | 0.999999 | 1.000000 |
| hot-unique | always/initial | primary/callerThreadAllocatedBytes | 1.000001 | 0.975111 | 1.000000 | 0.986412 | 1.000000 | 1.000000 |
| hot-unique | initial/ordinary | primary/totalNs | 1.058555 | 0.659326 | 0.841937 | 0.907355 | 0.988159 | 0.963886 |
| hot-unique | initial/ordinary | secondaryMeanSensitivity/totalNs | 1.063954 | 0.658596 | 0.768560 | 0.901356 | 1.057928 | 0.926800 |
| hot-unique | initial/ordinary | primary/allThreadAllocatedBytes | 0.988561 | 1.034865 | 1.002210 | 0.977348 | 1.002208 | 1.002208 |
| hot-unique | initial/ordinary | primary/callerThreadAllocatedBytes | 0.988560 | 1.034869 | 1.002209 | 0.977347 | 1.002209 | 1.002209 |
| hot-unique | always/ordinary | primary/totalNs | 1.052607 | 0.744303 | 0.847270 | 0.886841 | 1.042966 | 0.950835 |
| hot-unique | always/ordinary | secondaryMeanSensitivity/totalNs | 1.110874 | 0.740027 | 0.852443 | 0.938296 | 1.141136 | 0.931392 |
| hot-unique | always/ordinary | primary/allThreadAllocatedBytes | 0.988562 | 1.009111 | 1.002210 | 0.964069 | 1.002208 | 1.002208 |
| hot-unique | always/ordinary | primary/callerThreadAllocatedBytes | 0.988561 | 1.009112 | 1.002210 | 0.964067 | 1.002209 | 1.002209 |

## Absolute query measurements

Values below are medians of the six process medians, not pooled query medians.

| Fixture | Arm | Total ms | Allocation bytes | Prepare ms | Evaluate ms | Consume ms | Close ms | Ledger peak bytes | Source calls |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| early-cold-hot | ordinary | 520.5104 | 302203516 | 1.0864 | 4.7198 | 511.2598 | 0.1442 | 1152 | 131072 |
| early-cold-hot | initial | 471.8212 | 298720408 | 0.9727 | 4.5225 | 466.1981 | 0.1319 | 606448 | 4128 |
| early-cold-hot | always | 484.6317 | 291807128 | 1.0279 | 4.9799 | 475.2346 | 0.1455 | 606448 | 4128 |
| late-cold-hot | ordinary | 481.4577 | 325475984 | 1.0394 | 4.6734 | 475.7120 | 0.1483 | 1152 | 131072 |
| late-cold-hot | initial | 536.3767 | 326351732 | 1.1519 | 5.2761 | 529.3513 | 0.1412 | 606448 | 131072 |
| late-cold-hot | always | 587.8802 | 318199612 | 1.0385 | 5.1248 | 581.3160 | 0.1451 | 14549472 | 32832 |
| hot-unique | ordinary | 771.6414 | 455724636 | 1.1870 | 5.4048 | 763.7414 | 0.1729 | 1152 | 131072 |
| hot-unique | initial | 705.8734 | 456731004 | 0.9759 | 4.9472 | 700.3456 | 0.1583 | 3637728 | 122944 |
| hot-unique | always | 711.4885 | 456731088 | 1.0111 | 4.9851 | 705.1199 | 0.1461 | 3637728 | 122944 |

The JSON also contains first-row and phase timing, retained-read time, cold first-query measurements, all owner counter ranges and totals, and the natural-GC summary of each JVM.

## Interpretation limits

Descriptive cutoff-only ablation; no significance claim or production-promotion gate.

Ledger peak is the process-authority accounting peak, not a separate query-ledger peak or total retained heap. Weak metadata reservations are reported independently.

Whole-JVM natural GC log, including setup and warmups. Ordered wrappers retain Values; this cohort does not isolate weak collection or useful-key rematerialization.

collectedMisses counts cleared occupied slots before key comparison, not necessarily additional same-key source calls. Occupancy at close is not a count of live weak referents. Always mode may grow under unchanged cumulative hit/collision rules; this is not a fixed-capacity ablation.

Fresh controls only. No historical, diagnostic, warmup or incomplete-cohort pooling.
