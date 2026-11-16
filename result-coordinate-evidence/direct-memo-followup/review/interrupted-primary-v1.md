# Interrupted primary comparison v1

**Cause remains undetermined.** The available files establish an incomplete primary cohort and an unrecorded termination of its control flow. They do not establish either a five-minute execution/session timeout or an out-of-memory kill. No timing effect estimate was calculated.

## Preserved facts

Seven JVMs completed successfully with exit code zero and no recorded failure. Their summary contains 91 queries, including 35 measurement-stage queries. The eighth fork, `round-01-00-shared-tail-01-direct`, contains only four warmup samples, with ordinals -8 through -5. It has no `process-result.json`, root-host audit, direct-memo statistics export, or validation receipt. There are 95 persisted query samples when those four unfinished-fork warmups are counted; they do not make the cohort qualified.

The top-level summary still says `complete: false`, records no caught errors, and explicitly accepts no comparison estimates. A post-run preservation receipt is absent. No available console contains an OOM, exception, fatal-error, signal, or deadline message matching the diagnostic search.

| Administrative event | UTC timestamp |
|---|---|
| First JVM launch record | 2026-11-16T20:36:41.172320+00:00 |
| Seventh JVM completion record | 2026-11-16T20:40:40.776131+00:00 |
| Eighth JVM launch record | 2026-11-16T20:40:40.800978+00:00 |
| Last persisted sample-file activity | 2026-11-16T20:40:53.058940+00:00 |

The last persisted progress was **251.886620457 seconds after the first JVM launch**, and **12.257962415 seconds after the eighth JVM launch**. These are diagnostic chronology intervals, not performance estimates. A last file-write time is not a process termination time.

## What the evidence can and cannot explain

The driver prescribes a 300-second deadline for each individual JVM. Its handled timeout path raises `TimeoutError` and writes `process-result.json` from `finally`. That expected receipt is absent for the eighth fork. The absence is inconsistent with a normally completed, successfully recorded managed-timeout path, but it does not identify the terminating event or rule out a failure while writing the receipt.

The coordinator reports using `exec_command`, correctly polling returned session 17470 through `write_stdin`, receiving several poll results, and later receiving “Unknown process id.” No absolute controller-invocation timestamp was retained. Controller runtime validation precedes the first JVM launch record, so the 251.887-second interval above cannot establish or refute a five-minute controller/session lifetime. No platform watchdog or timeout record is available.

A process snapshot in the namespace visible to this diagnostic tool showed no Java process or benchmark controller. It is not a host-wide process census. The memory cgroup files were not exposed, no cgroup mount was observed, and kernel-log access failed with `Operation not permitted`. No JVM crash file or heap dump was observed under the experiment directory. These observations supply **no positive evidence of OOM**, but they cannot exclude an external OOM kill. No further inaccessible OOM probing was attempted.

## Evidence and continuation rule

`interrupted-primary-v1.json` preserves the exact raw-file hashes, file modification times, all eight launch/completion records, the process-snapshot text, inaccessible telemetry results, and the distinction between independently observed and coordinator-reported facts. The frozen driver hash is `2884c48422c85991930ceb0216b47aa182c5790b39f9e1fe824017db9e644c0f`. Raw files were rehashed before writing this diagnosis. No Java, source edit, or benchmark-tool edit was performed.

Keep this cohort intact as an interrupted attempt. Do not replace the partial fork, merge it with another cohort, or resume it to obtain accepted estimates. A controller change requires a newly frozen, complete 36-JVM cohort with its stopping and no-retry rules established before launch. Its prospective execution rules will be reviewed separately.
