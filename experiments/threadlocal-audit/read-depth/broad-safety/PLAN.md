# Broader depth-only correctness qualification

1. completed first batch; pending remaining56: frozen LMDB scope/read lifecycle batches using exact prior A vs candidate AD runtime families and historical compiled tests; capture origins/results and baseline-triage failures.
2. completed: affected HTTP context tests, then release CPU to inventory before remaining broad native-evaluation batches.
3. in_progress (waiting inventory CPU handback): broader evaluation coverage as CPU windows permit; report exact included/excluded tests and unresolved failures. No production changes.

Candidate identity0750336e; baseline5ac89b5. Current git HEAD/source tree is not the runtime identity. Existing cp_a.txt/cp_ad.txt are canonical runtime classpaths; no current target/classes, resource null-restoration, slower cancellation or grouped test seam added. Scope selection is79-ish top-level source classes matching read/replay/lifecycle/cancellation/transaction/repository/value-store themes and present in the frozen historical test bundle. Test family SHA256 recorded; selection excludes classes absent from frozen bundle. This is broad selected coverage, not full module/repository coverage.

Each20-class batch runs in a fresh JVM, sequential JUnit execution and common existing Mockito javaagent,4GiB heap. Class completion prints a durable result/summary; failure class list written immediately. External12-minute batch ceiling bounds unresponsive tests and will be reported as incomplete, not silently skipped. Existing per-test deadlines remain untouched. Canonical native-benchmark.lock is held during each run; no build/profile overlap. Failed classes receive the identical frozen test/runtime flags on A. Benchmark-only/statistical loops are not being added.

First window completed399passed/3skipped/0failed across23classes; detailed coverage in PARTIAL-RESULTS.md. CPU released to inventory per root instruction. No additional JVM while inventory builds/times.

Second window update: all remaining56scope classes completed green; aggregate1393passed/3gatedskip/0failure across76LMDB+3HTTPclasses. Step1 is fully complete for this selection. Step3 waiting inventory profile window; prepared28native-evaluation scoped classes in native-01/02.txt. No failures requiring baseline triage so far.

Final phase complete:1705passed/3gatedskip/0fail across107classes, including prepared28native classes. Step3 now in_progress only for parent review/publication acceptance; no further suites requested thisphase. CPU handed to inventory for final JFR export; no correctness JVM active. Final report RESULTS.md and publication whitelist PUBLICATION-FILES.txt.
