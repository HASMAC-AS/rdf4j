# Root-qualified external launcher

This directory adds one uniquely named launcher class after the original 126 frozen classpath entries. It delegates the measured `OptionalFanoutExperiment.main` unchanged, then writes absolute root-host counters beside its raw samples. It performs no engine-counter reads or resets before the original main.

The separately named [driver](../run-root-qualified-coordinate-experiments.py) enforces the [prespecified protocol](../root-qualified-confirmation-protocol.json). A nonzero hosting counter or failed audit stops the confirmation without replacing a fork. Original benchmark sources, drivers, manifests, and runtime artifacts stay unchanged.

The [overlay manifest](overlay-manifest.json) identifies the unique 3,481-byte class and its source. The [build command](build-command.json) records compilation against the original frozen runtime with annotation processing disabled; [build log](build.log) records its result. `classpath.txt` is a new 127-entry classpath, leaving the original classpath intact.

The absolute counters cover all warmups and measured queries in each fresh process. A zero hosted-root count qualifies this new confirmation, and does not retrospectively classify older timed forks. The wrapper is outside the 2,057-file measured source identity and is preserved as a separate runtime add-on.
