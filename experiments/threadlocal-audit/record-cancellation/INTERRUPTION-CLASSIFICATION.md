# Pre-execution interruption classification prerequisite

Description: direct leaf/lifecycle cancellation uses ordinary `QueryInterruptedException` rather than mutating a possibly shared RowState token. Kernel row/aggregate front doors previously only rethrew ordinary exceptions after `executionStarted`; before then `serialTerminalFailure` returned null for an ordinary interruption, permitting decline.

Evidence: `classifier-red.log`, exact original compiled classifier, 5 tests found/started, 3 successful, 2 failed. Direct and wrapped interruption both produced null instead of the original exception. Native query cancellation, probe deadline, and ordinary unsupported-bind controls passed. Tests invoke the actual private classification helper, not a reimplementation; this is classifier hit proof, not a full query-path test.

Change: recognize `QueryInterruptedException` in the cause chain before ordinary control classification and return the original exception object, preserving wrapper and suppression. Do not add it to clean-control detection. Parent native token/probe behavior remains unchanged.

Plan:
- [x] Capture narrow failing evidence before editing production.
- [x] Apply narrow classifier change.
- [x] Combined compile and all five classifier tests green in `green-focused.log`. That overall selection is not green: one record-cursor test failed and is being corrected/rechecked by the native-path worker.
- [x] Final combined selected safety: 213 passed both owned ON and OFF in the same compiled candidate. See `green-owned-final-2.log` and `green-off-final.log`.

Limitation: classifier and scanner admission are independently exercised; no full end-to-end forced pre-execution binder-failure test is claimed. Performance qualification is the current in-progress step, owned by the benchmark worker.

The production source is `LmdbNativeKernelExecution.java`; tests are `LmdbNativeKernelInterruptionClassificationTest.java`. A direct javac/JUnit launcher was used for RED while preserving the exact existing production classes. No timing ran concurrently.
