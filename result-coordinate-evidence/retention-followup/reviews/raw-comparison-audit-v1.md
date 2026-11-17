# Independent raw comparison audit

**The 36-JVM cohort is valid. The primary estimates were independently reproduced from raw observations, and there is no route variation across the six rounds. The fanout weak-versus-strong latency result is sensitive to the estimator: its primary median-based ratio is 0.988624, while a post hoc arithmetic-mean sensitivity gives 1.012401.**

This review reads raw query samples, memo owner records, root/JVM/claim/completion receipts, and GC logs. It imports neither the frozen runner nor its analyzer or summary values. `validation.json` was hashed only to verify its completion seal. The independent Python audit and compact JSON are `raw-comparison-audit-v1.py` and `raw-comparison-audit-v1.json`. The frozen protocol SHA-256 is `e151245f589b655d7b2f8240998552486e84836d0895147ab7b8428d7fe111af`. Runtime library/source identity verification remains the separate frozen audit scope.

## Cohort and route verification

All 36 prescribed fresh JVMs exited normally and ran exactly eight warmups followed by five measured queries: 468 queries, comprising 288 warmups and 180 measurements. Claims follow the fixed six-permutation schedule; JVM intervals do not overlap; every sealed raw output still matches its hash. Every result count and lexical checksum passes, and all 234 shared-tail queries also pass their independent exact-bag oracle. Query timing components sum exactly to recorded total time.

All 468 queries use an observed unforced AUTO native root. In every one of the 18 JVMs per fixture, iteration −8 uses `irKernelInterpreted` with `retry normal`, and all remaining 12 queries use `irKernel` with `normal`. The 180 measured queries therefore use compiled row IR. The complete route signature matches at every query ordinal across all arms and all six rounds. No generic hosted root or unhosted hard decline is recorded.

Adjacency is ACTIVE at the same applied/current revision before and after every query. Fanout records exactly 55 successful adjacency lookups per query and shared-tail exactly 10; both record one planner-statistics hit, no exact misses, and no fallback reasons. These observations establish query-time adjacency use, including preparation; they do not assign every emitted row to an adjacency lookup.

All query ledger endpoints are zero. All 312 direct owners close, including all 156 weak owners, with no recorder overflow. Per-owner reservation formulas reconcile to the ledger peaks. There are no admission refusals, semantic invalidations, unsupported admissions, rehash discards, adaptive disables, or table growth in these fixed fixtures.

## Primary estimates

The primary estimator is unchanged: each JVM contributes the median of all five measured query values; each fixture then has six same-round candidate/comparator ratios, summarized by their median. Rows below use zero-based round numbers. A ratio below one favors the numerator. These six-ratio summaries need not equal a ratio of the separately summarized absolute medians, and medians of different pairwise comparisons need not multiply transitively.

### fanout

| Round | Latency W/S | Latency S/O | Latency W/O | Allocation W/S | Allocation S/O | Allocation W/O |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 0 | 0.908446855 | 1.041044448 | 0.945733555 | 1.001075217 | 0.838239225 | 0.839140514 |
| 1 | 1.088229496 | 0.892848486 | 0.971624058 | 1.001055210 | 0.838284255 | 0.839168821 |
| 2 | 0.995688930 | 0.854842640 | 0.851157354 | 1.000959384 | 0.862669892 | 0.863497524 |
| 3 | 1.064486855 | 0.918434064 | 0.977660988 | 1.035967465 | 0.809987517 | 0.839120715 |
| 4 | 0.973469147 | 0.997330021 | 0.970870005 | 1.014707640 | 0.826922923 | 0.839085007 |
| 5 | 0.981559865 | 0.953250382 | 0.935672317 | 1.000918487 | 0.838312501 | 0.839082479 |
| Median ratio | 0.988624398 | 0.935842223 | 0.958301780 | 1.001065213 | 0.838261740 | 0.839130615 |

### shared-tail

| Round | Latency W/S | Latency S/O | Latency W/O | Allocation W/S | Allocation S/O | Allocation W/O |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 0 | 0.879945492 | 1.074147262 | 0.945191040 | 1.006124374 | 0.854769807 | 0.860004737 |
| 1 | 0.969709717 | 0.932291461 | 0.904052089 | 1.006031131 | 0.854808842 | 0.859964306 |
| 2 | 1.055480970 | 0.925802763 | 0.977167198 | 1.006099763 | 0.854792475 | 0.860006506 |
| 3 | 0.812477701 | 1.188451214 | 0.965590111 | 1.006068577 | 0.854815512 | 0.860003026 |
| 4 | 0.993471789 | 0.790925996 | 0.785762665 | 1.006099855 | 0.854767671 | 0.859981629 |
| 5 | 0.995190353 | 0.936365561 | 0.931861973 | 1.006134570 | 0.854788459 | 0.860032218 |
| Median ratio | 0.981590753 | 0.934328511 | 0.938526507 | 1.006099809 | 0.854790467 | 0.860003881 |

W = weak direct memo; S = strong direct memo; O = ordinary reuse. Primary weak/ordinary latency is below one in all six pairs on both fixtures. Weak/strong is below one in four fanout pairs and five shared-tail pairs. The latter is not a consistent six-pair latency win.

### Absolute medians

These are medians of the six JVM medians, not the primary paired ratios.

| Fixture | Arm | Query seconds | All-thread allocation bytes | Caller allocation bytes | Ledger peak bytes | Resolver calls |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| fanout | ordinary | 3.523619410 | 1,630,367,272 | 1,630,294,756 | 2,432 | 2,770,101 |
| fanout | strong | 3.232625529 | 1,366,672,540 | 1,366,613,556 | 147,536 | 33,386 |
| fanout | weak | 3.282006737 | 1,368,056,908 | 1,367,995,592 | 606,448 | 33,386 |
| shared-tail | ordinary | 2.465851231 | 1,320,732,948 | 1,320,680,444 | 1,472 | 2,113,601 |
| shared-tail | strong | 2.396675490 | 1,128,937,688 | 1,128,886,568 | 147,536 | 172,090 |
| shared-tail | weak | 2.259718428 | 1,135,815,572 | 1,135,763,096 | 606,448 | 172,090 |

The fanout absolute weak median is larger than the absolute strong median even though the primary paired median ratio favors weak. These answer different summaries of the paired data; neither should be substituted for the other.

| Fixture | Arm | Prepare ms | Evaluate ms | First row after evaluate ms | Consume ms | Close ms |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| fanout | ordinary | 1.837997 | 16.380855 | 5.388386 | 3495.593548 | 0.145565 |
| fanout | strong | 2.138138 | 21.555557 | 6.274063 | 3209.586986 | 0.130954 |
| fanout | weak | 2.440059 | 20.303536 | 6.452729 | 3256.061442 | 0.171651 |
| shared-tail | ordinary | 1.259567 | 7.240921 | 4.259319 | 2458.974906 | 0.162689 |
| shared-tail | strong | 1.126407 | 5.721883 | 3.899986 | 2387.725259 | 0.170944 |
| shared-tail | weak | 1.259591 | 6.042649 | 4.009605 | 2251.985043 | 0.138955 |

The first-row interval is contained within consumption and must not be added to it. Independently summarized phase medians also need not sum to the separately summarized total median.

## Post hoc arithmetic-mean sensitivity

**This secondary analysis was requested after the cohort completed. It was not prespecified in the frozen protocol and does not replace the primary estimator.** It changes only each JVM’s inner time summary to the arithmetic mean of its same five measured queries. It retains the same six round pairs and outer median. No samples are removed, replaced, or added.

| Fixture | Comparison | Primary median-based ratio | Secondary mean-based ratio | Secondary six-pair range | Primary/secondary pairs below one |
| --- | --- | ---: | ---: | --- | --- |
| fanout | weak/strong | 0.988624398 | 1.012401353 | 0.847980139–1.118875961 | 4/6; 2/6 |
| fanout | strong/ordinary | 0.935842223 | 0.945604177 | 0.874493459–1.109432980 | 5/6; 4/6 |
| fanout | weak/ordinary | 0.958301780 | 0.976230934 | 0.868938779–1.060929087 | 6/6; 5/6 |
| shared-tail | weak/strong | 0.981590753 | 0.980153287 | 0.862967245–1.002541049 | 5/6; 5/6 |
| shared-tail | strong/ordinary | 0.934328511 | 0.937626210 | 0.777993590–1.055785689 | 4/6; 4/6 |
| shared-tail | weak/ordinary | 0.938526507 | 0.902512591 | 0.779970510–0.939033830 | 6/6; 6/6 |

The fanout weak/strong aggregate changes direction: approximately 1.14% lower typical latency under the primary estimator versus 1.24% higher under this secondary summary. Rounds 4 and 5 change from below one to above one. Fanout weak/ordinary remains below one in aggregate but loses the all-six-pair pattern: its round 4 mean-based ratio is 1.060929087. Shared-tail weak/strong remains below one in aggregate, and its pair directions do not change.

For reproducibility, all six mean-based ratios are:

| Fixture | Comparison | Ratios for rounds 0–5 |
| --- | --- | --- |
| fanout | weak/strong | 0.847980139, 1.118875961, 0.993648117, 1.055368254, 1.008837044, 1.015965661 |
| fanout | strong/ordinary | 1.109432980, 0.882872421, 0.874493459, 0.941730261, 1.051635736, 0.949478093 |
| fanout | weak/ordinary | 0.940777133, 0.987824729, 0.868938779, 0.993872222, 1.060929087, 0.964637139 |
| shared-tail | weak/strong | 0.865746089, 0.977968132, 1.002541049, 0.862967245, 0.984451289, 0.982338443 |
| shared-tail | strong/ordinary | 1.055785689, 0.919335609, 0.777993590, 1.049801438, 0.811521149, 0.955916810 |
| shared-tail | weak/ordinary | 0.914042331, 0.899080928, 0.779970510, 0.905944255, 0.798903041, 0.939033830 |

## Allocation spread and caller-thread sensitivity

Weak/strong allocation is higher in every paired round on both fixtures. The fanout median all-thread increase is only 0.1065%, but the full range is 0.0918%–3.5967%. Caller-thread measurements show essentially the same spread, so asynchronous worker allocation is not a sufficient explanation for the large paired differences.

| Fanout round | All-thread W/S | Caller-thread W/S | Strong all-thread bytes | Weak all-thread bytes | Difference bytes |
| --- | ---: | ---: | ---: | ---: | ---: |
| 0 | 1.001075217 | 1.001065328 | 1,366,660,304 | 1,368,129,760 | 1,469,456 |
| 1 | 1.001055210 | 1.001054993 | 1,366,689,560 | 1,368,131,704 | 1,442,144 |
| 2 | 1.000959384 | 1.000959632 | 1,366,684,776 | 1,367,995,952 | 1,311,176 |
| 3 | 1.035967465 | 1.035950185 | 1,320,554,336 | 1,368,051,328 | 47,496,992 |
| 4 | 1.014707640 | 1.014708816 | 1,348,233,160 | 1,368,062,488 | 19,829,328 |
| 5 | 1.000918487 | 1.000928611 | 1,366,778,920 | 1,368,034,288 | 1,255,368 |

Rounds 3 and 4 have lower strong-control allocation, while weak allocation remains near 1.368 billion bytes. Their differences are 47,496,992 and 19,829,328 bytes. Source calls, probes, hits, table capacity, and weak-entry counts are unchanged. The full differences therefore should not be attributed solely to newly allocated weak-reference entries; the source of the wider allocation spread is unresolved in this evidence. Both observations remain included.

Shared-tail allocation is more stable: W/S all-thread ratios span 1.006031131–1.006134570, while caller-thread ratios span 1.006031412–1.006134861. All six caller ratios, allocation medians, and measured samples are retained in the independent JSON.

## Collected slots, source calls, and reservations

Every measured strong/weak pair has identical source-call, probe, and hit counts. Fanout uses 33,386 source calls from 13,824,000 probes, with 13,790,614 hits. Shared-tail uses 172,090 source calls from 6,291,456 probes, with 6,119,366 hits. Those source-call counts are substantially lower than ordinary reuse’s 2,770,101 and 2,113,601 calls respectively.

Fanout records zero cleared-slot encounters in all warmups and measurements. Shared-tail records 633 over all 78 weak queries, of which 236 occur in the 30 measured queries. Its measured per-query `collectedMisses` range is 3–14, with median 7.5. In every measured query, the reduction in weak collision counts equals the collected-slot count exactly, while hits and source calls match strong mode. This is consistent with cleared conflicting buckets being classified before key comparison; `collectedMisses` is not a count of additional same-key rematerializations.

There is one warmup exception to all-query equality: **shared-tail round 3, iteration −7**. Strong makes 172,090 source calls and 6,119,366 hits; weak makes 172,091 calls and 6,119,365 hits. Weak records 14 collected slots and 13 fewer collisions (167,981 versus 167,994). This warmup has one extra source call. It is retained in raw evidence and excluded from the primary latency estimator under the original eight-warmup rule. The other 155 paired queries reconcile source calls, hits, and collision reclassification exactly.

Weak mode allocates 33,386 entry wrappers per measured fanout query and 172,090 per measured shared-tail query, plus one owner-reference wrapper per query. All direct owners keep capacity 4,096 with no resize or adaptive cutoff. Both modes report an array estimate of 147,536 bytes; weak mode reserves 606,448 bytes including its declared reference allowance. Close occupancy is 981 metadata slots for fanout and 4,096 for shared-tail; it is not a count of live payloads. All reservations are released at query close.

## Natural GC observations

Every fork identifies G1, and none contains a recorded explicit or diagnostic GC cause. The following values are medians over each arm’s six whole-JVM logs. They include fixture setup, warmups, and measurement. Concurrent-cycle duration overlaps application work and must not be added to pause duration.

| Fixture | Arm | Pauses | Pause ms | Pause-ms range | Concurrent cycles | Concurrent-cycle ms |
| --- | --- | ---: | ---: | --- | ---: | ---: |
| fanout | ordinary | 37.5 | 267.9585 | 227.790–325.145 | 1 | 33.9185 |
| fanout | strong | 32 | 254.3575 | 224.650–462.735 | 1 | 50.5700 |
| fanout | weak | 33.5 | 247.3175 | 221.051–434.137 | 1.5 | 65.2750 |
| shared-tail | ordinary | 31.5 | 319.1180 | 266.419–341.559 | 1 | 61.1680 |
| shared-tail | strong | 28 | 286.6560 | 253.742–595.163 | 1 | 36.1755 |
| shared-tail | weak | 29 | 297.9115 | 265.553–356.103 | 1 | 55.5350 |

These logs do not isolate measured-query GC cost or establish lower retained heap. Allocation, ledger charges, cleared-slot counters, and occupied-slot counts remain distinct from whole-heap reachability. The separate memory diagnostics and ownership contracts address the lifetime question.

## Conclusion and limits

No raw-result validity mismatch, route mismatch, ownership-cleanup failure, or reservation discrepancy was found. The direct weak arm preserves the measured resolver reduction with modest typical additional allocation on these two fixtures. A stable latency advantage over strong retention is not established: fanout changes sign under the post hoc mean sensitivity, and both fixtures contain primary weak/strong ratios above one. Weak versus ordinary remains favorable in the primary six-pair comparison, with the stated fanout mean-sensitivity exception.

These results apply to the fixed full-consumption workloads, their unchanged source caches, and the frozen 1 GiB heap/configuration. They do not establish behavior for retained rows, early close, large uncached payloads, constrained-memory sweeps, or other repetition patterns. No further JVM, profiler, benchmark, or sample selection was used by this review.
