# Result-coordinate experiment harness

## Historical provenance

The supplied historical observations were approximately 2.79 to 1.42 GB/query
and 2.21–2.25 to 2.68 seconds. The preserved baseline and Stage-2 binaries,
commands, JVM flags, and raw results could not be recovered in this workspace
or the available file inventory. Do not label newly generated results as those
preserved measurements. The checkpoint's parent, `e7c6ca7537`, is available for
a separately labelled source reconstruction. A separately committed Stage-2
state has not been identified.

The decisive comparison uses one frozen binary containing the ownership repairs
and changes only `rdf4j.lmdb.resultCoordinateTransport`. No execution strategy is
forced.

| Mode | Transport being compared |
| --- | --- |
| `off` | Ordinary last-value reuse without coordinate acquisition or the coordinate cache |
| `dense` | Five-word coordinates copied through the output window |
| `changes` | Changed-register records and sparse output-coordinate changes |
| `rows` | Primitive source-row selections shared by groups of output columns within each window |
| `selection` | Primitive selection per output cell into immutable coordinate records; supplementary correctness coverage only, not timed |

The primary four-arm comparison is `off,dense,changes,rows`. The `rows` mode
tests sharing one row selection among related output columns. The earlier
`selection` mode retains a selection for each cell, so it is labelled separately.
All representations retain the existing five-word upstream register, frame,
and source-side coordinate state. This experiment changes output-window
transport, not every coordinate representation in the engine.

## Entrypoint and dimensions

`OptionalFanoutExperiment` is a test-side standalone main. The existing
`OptionalFanoutBenchmark`, `OptionalFanoutFixture`, and query resource are
unchanged. Its default data and query are exactly the original 100 measurements
by 10 readings, 3,278 triples, 12 projected columns, and 1,152,000 output rows.

All arguments use `--name=value`:

| Argument | Default | Meaning |
| --- | --- | --- |
| `measurements` | 100 | Measurement subjects |
| `readings` | 10 | Readings per measurement |
| `width` | 12 | Prefix of the original SELECT list, 1 through 12 |
| `statuses` | 2 | Final independent OPTIONAL fanout |
| `unique-payloads` | 0 | 0 preserves the original graph; a positive number adds one literal payload per reading and replaces projected reading with payload |
| `consumption` | full | `full`, `early`, or `retained` |
| `limit` | 1024 | Maximum rows for early close |
| `retain` | 4096 | Number of result rows held until after result and connection close; 0 retains all |
| `warmup` | 3 | Untimed-for-analysis query invocations, still recorded |
| `iterations` | 5 | Measured query invocations |
| `arm` | unspecified | Descriptive arm name; does not configure execution |
| `source` | unspecified | Source identity supplied by the caller |
| `output` | stdout | New JSONL file; existing files are never overwritten |
| `store` | new temporary directory | New store directory; kept for inspection |

Retained mode reads nonretained rows normally and reads retained rows only
after both the result and connection have closed. Every result contributes to
the final independent full-bag checksum. This exercises detached result
lifetimes while keeping the amount of application retention explicit.

Early mode records a lexical prefix checksum. SPARQL does not promise an order
for this query, so the independent fixture model validates membership and
multiplicity rather than demanding that two strategies produce the same
prefix. The full-bag checksum is checked when the early limit covers all rows.
Partial validation happens outside the timing/allocation window. Retention
reference arrays and the observation buffer are allocated before that window;
returned rows, values, and lexical strings are allocated inside it.

The unique-payload family changes the query by one mandatory payload triple
pattern and changes a projected reading IRI to its literal payload. The graph
shape and result cardinality remain constant within that family while the
number of distinct payloads varies. It is labelled separately and is not
treated as the exact default benchmark. Projection width is a prefix of the
SELECT list: width 1 projects only patient, width 4 includes the reading or
payload, and width 12 projects all columns.

## Recorded measurements

Every sample records connection, prepare, evaluate, consume, close,
post-close retained-read, and total nanoseconds. First-row delay after
`evaluate()` is also recorded. It overlaps consumption time and must not be
added to total time. Parsing/preparation in `prepareTupleQuery()` is only one
phase: optimization and compilation may occur in `evaluate()` or first pull.

Allocation uses `com.sun.management.ThreadMXBean.getTotalThreadAllocatedBytes()`
and includes every Java thread in the JVM during the sample window. Caller
allocation is also reported. The runner rejects a JVM without the total
counter instead of silently substituting caller-only accounting. Asynchronous
preparation that continues beyond a sample can appear in a later sample;
warmup and separately recorded compiled/interpreted binds make this visible.

Memory is measured with the existing process authority. The internal
`resetPeakUsedBytesForTesting()` resets only its high-water counter between
serial queries. The admission authority and its limits are unchanged. The
record contains used bytes before, peak bytes, and used bytes after closure;
nonzero starting or final usage invalidates the sample. This is the aggregate
reservation peak of query ledgers in this otherwise idle JVM. It is not an
RSS, LMDB-map, untracked-heap, or application-retention measurement, and it
cannot distinguish individual nested ledger peaks.

A bounded successful-open observer records successful native AUTO operator choices,
including observed retry selections. It retains only labels and counts, not
expressions, sources, plans, or contexts. Existing native counters independently
record IR opens/rows, compiled/interpreted binds, semantic compilation, replay,
factorized engagement, and resolver calls. Observation is identical for every
transport arm. EXPLAIN is not executed between measured samples.

This observer does not classify the outer query root or every generic fallback.
A generic root containing one native kernel can report the same selection and
compilation counts as a native root. The original six protocols therefore establish
matching observed native operator/tier frequencies, not complete outer-plan
equivalence. Their raw records contain no hosted-root counter. The separately
prespecified root-qualified confirmation adds post-main absolute counters in a
fresh JVM without changing the original timed harness; its classification applies
only to its own forks. Even a zero hosted-root count does not prove that every
interior operator was native.

Empty successful-selection observations are explicitly classified as
`generic-or-unobserved` (or native counter evidence without a selection event).
The `auto` observation is null in that case; requesting AUTO is recorded
separately and does not establish an actual native execution route. Pair
summaries compare frequency histograms of selection labels/counts and
compiled/interpreted tier bind signatures. They also show the number of
compiled, interpreted, mixed-within-query, and unobserved-IR samples and flag
mixed tier composition. Two arms that contain the same route or tier set with
different sample frequencies are reported as route-changed, rather than
treated as equivalent because both requested AUTO.

When the public coordinate diagnostics API is available, counter deltas also
report coordinate opens and initial/growth admission refusals. This distinguishes
the configured representation from optional transport that a constrained ledger
declined. An older source reconstruction without that API reports null and
`unavailable`. The test harness uses only cached public reflection outside the
measurement window; it does not inspect or manipulate private execution state.

The additive `rows` diagnostics are `rowSelectionOwners`,
`rowSelectionWindows`, `rowSelectionRows`, `rowSelectionColumns`,
`rowSelectionGroups`, and `rowSelectionInvariantRefused`. They are process
totals published once when each admitted row-selection owner closes; each
query reports the change in those totals. Windows, rows, columns, and groups
count only valid encoded output windows. Columns and groups are each summed
once per window, while rows count encoded rows in those windows, including
buffered rows that an early-closing consumer never pulled. A window
withdrawn after an invariant or growth failure does not contribute to those
participation totals. Invariant refusals count owners that withdrew because
their proposed column grouping diverged. A refusal preserves ordinary result
production and is reported separately from result validation failures.

Fork summaries retain the range, median, and total of each available diagnostic
and report total groups divided by total columns over valid windows. Nonzero
window and row counts establish actual use of `rows`; merely configuring the
mode or opening a coordinate owner does not. A group-to-column ratio near one
indicates little sharing across columns. Missing additive accessors produce
null fields, not zero counts. JSONL format 2 adds these fields; the frozen fresh
parent retains its original format-1 harness and unavailable diagnostics.

Metadata records exact data/query digests, fixture dimensions, projected names,
JVM arguments, Java runtime, GC, max heap, CPU count, LMDB properties, and ledger
limits. Canonical graph SHA-256 hashes sorted N-Triples lines; the original
fixture's insertion digest is retained separately.

## Using the preserved runtime archive

The separate `rdf4j-coordinate-runtimes.tar.gz` preserves the two exact measured
classpath snapshots. Extract it into the same parent directory as the source and
evidence archives; all three use the `rdf4j/` prefix. The original absolute
classpath strings remain untouched as provenance. From the extracted repository
root, create portable classpaths that validate all copied artifacts first:

```bash
python3 result-coordinate-evidence/rebase-coordinate-classpath.py \
  --runtime result-coordinate-evidence/frozen-current \
  --output current-classpath.txt
python3 result-coordinate-evidence/rebase-coordinate-classpath.py \
  --runtime result-coordinate-evidence/fresh-parent-e7c6ca/frozen \
  --output parent-classpath.txt
```

Use those new files for `--classpath-file` and `--parent-classpath-file` in the
commands below, and use each preserved provenance file's `source` field for its
source label. A compatible JDK/platform is still required; the measured JVM was
25.0.5+8-LTS. The archive preserves measured bytes and does not promise identical
performance on another machine. [Archive verification](runtime-delivery/runtime-verification.json)
and [fresh-extraction helper verification](runtime-delivery/helper-verification.json)
record complete file, artifact, ordering, and unchanged-metadata checks.

### Reproducing the root-qualified confirmation

The separate `rdf4j-root-qualified-runtime-addon.tar.gz` preserves the exact
external launcher directory used in the confirmation. It appends one unique
3,481-byte class as classpath entry 127, after the original 126 entries in their
original order. It does not replace any original harness or engine class. The
launcher calls the original main and reads root counters only after it returns
or unwinds; it does not reset counters or instrument the measured loop.

Extract the base runtime archive and this add-on into the same empty parent
directory. To prepare a new run at that extraction path:

```bash
python3 rdf4j/result-coordinate-evidence/rebase-root-qualified-experiment.py \
  --java /path/to/jdk-25.0.5+8/bin/java \
  --output /absolute/path/to/new-reproduction-plan
```

The helper validates all 127 entries and writes a new classpath, reference
manifest, derivative protocol, and exact command. It prints the shell-quoted
command and saves its argv in `command.json`; it executes no Java. The original
protocol, commands, and absolute-path metadata remain unchanged. Run the printed
command to execute the unchanged confirmation driver. Use the recorded Java and
JVM settings when comparing results, and treat a run on another host as new
evidence rather than a reproduction of the original wall-clock times.

The [locked protocol](root-qualified-confirmation-protocol.json) specifies four
balanced rotations of OFF, DENSE, CHANGES, and ROWS, five warmups and five
measurements per fresh JVM, normal AUTO, a fixed 1 GiB G1 heap, and a stop rule
that blocks the whole control on any invalid fork or failed root audit. A zero
fresh-process `HOSTED_GENERIC` total covers all ten queries in that fork. It
does not classify the earlier protocols or prove the absence of every generic
interior operator. The `UNHOSTED_HARD_DECLINES` counter has no production mutation
in this source; its observed zero is a consistency diagnostic only.

The final add-on's [archive readback](runtime-delivery/runtime-addon-verification.json)
and [fresh-extraction helper check](runtime-delivery/addon-helper-verification.json)
verified all 127 entries and preserved the 37 original metadata files. See the
archived [runtime guide](runtime-delivery/ROOT-QUALIFIED-RUNTIME.md) for the exact
reproduction boundary. The helper check executes no benchmark or Java process.

The next section also describes how to freeze a new source build. Choose a new
freeze/output directory for a rebuild so the preserved snapshots remain intact.

## Running matched forks

Build/install the branch and create a test-runtime classpath file first. The
classpath must begin with the LMDB test/classes and production classes as
appropriate and contain its actual dependency jars. Keep builds and tests
stopped while measuring; the driver hashes every classpath file/tree and
rejects results if the classpath changes during the run.

The final successful Surefire XML report contains the actual runtime classpath.
Freeze its class directories and jars without another Maven invocation:

```bash
python3 result-coordinate-evidence/freeze-experiment-classpath.py \
  --report core/sail/lmdb/target/surefire-reports/TEST-org.eclipse.rdf4j.sail.lmdb.benchmark.OptionalFanoutExperimentFixtureTest.xml \
  --source OWNERSHIP_FIX_COMMIT_AND_PATCH_ID \
  --output result-coordinate-evidence/frozen-current
```

Use `result-coordinate-evidence/frozen-current/classpath.txt` below. The helper
prints the matching Java executable and preserves the source report and original
classpath order. It copies artifacts rather than creating links into build
directories. The driver independently hashes each copied artifact. Stop other
JVM builds/tests during timings even when classpath contents are frozen.

The commands below use the completed experiments' protocol settings. Substitute
the Java executable and actual source identity for the classpath you are using.
The `-rerun` output names keep the preserved observations separate; each output
directory must be new. Exact original commands and absolute paths remain in each
experiment's `command.json` records. From the repository root:

```bash
python3 result-coordinate-evidence/run-coordinate-experiments.py \
  --classpath-file result-coordinate-evidence/frozen-current/classpath.txt \
  --java /path/to/jdk-25/bin/java \
  --source OWNERSHIP_FIX_COMMIT \
  --modes off,dense --suite ablation \
  --heap 1024m --forks 4 --warmup 3 --iterations 5 \
  --output result-coordinate-evidence/reuse-versus-dense-rerun
```

The completed default comparison has four independent JVM forks per arm and alternates
AB/BA order. Four-arm runs rotate mode order for each fork. Common JVM flags
can be repeated as `--jvm-arg=-Dname=value`; the transport property is controlled
only by `--modes`. The specified heap and G1 collector are new matched settings,
not a claim to recover the historical JVM configuration.

After the ordinary-reuse comparison is valid, run the compact alternatives:

```bash
python3 result-coordinate-evidence/run-coordinate-experiments.py \
  --classpath-file result-coordinate-evidence/frozen-current/classpath.txt \
  --java /path/to/jdk-25/bin/java \
  --source OWNERSHIP_FIX_COMMIT \
  --modes off,dense,changes,rows --suite ablation \
  --heap 1024m --forks 4 --warmup 5 --iterations 5 \
  --output result-coordinate-evidence/transport-comparison-rerun
```

To compare the two kinds of primitive selection directly, use
`--modes selection,rows --forks 3` with the same default case and remaining
settings, in a separate output directory. This supplementary timing comparison
was not run in the completed experiments: SELECTION received correctness gates
only. It would not replace the ordinary-reuse ablation.

`--suite screen` selects a deliberately small set of workload variations and
defaults to one fork, one warmup, and two measured samples. It includes default,
small, many-measurement, projection-width, low/high-fanout, payload-cardinality,
early-close, retained-row, and 1 MiB ledger cases. Use `--cases=name,name` to
select a subset. Confirmation uses the same cases with three to five forks.
Some cases change multiple linked parameters: high fanout also changes M to
hold output cardinality constant, and payload cases use a modified query family.
Screening identifies cases deserving confirmation; it is not a confidence
interval or a production performance claim. The completed protocols explicitly
overrode the driver defaults:

| Completed protocol | Cases | Matched rounds | Warmups/JVM | Measurements/JVM |
| --- | --- | ---: | ---: | ---: |
| Workload screen | All 14 nondefault cases in `workload-screen/configuration.json` | 2 | 3 | 3 |
| Short confirmation | `small,early-1024,retained-small-all` | 4 | 32 | 7 |
| Final short extension | Same three cases | 4 | 512 | 101 |

Each round contains one separate JVM per arm. Pass the exact case names with
`--cases`, the table's counts with `--forks`, `--warmup`, and `--iterations`, and
a new output directory. At the time, the 512/101 extension was prespecified as
the final timing run. A later, separately prespecified default-workload control
was added to address missing outer-root telemetry. It does not replace or pool
with the earlier protocols, and no further short-query extension was run.

### Optional fresh parent comparison

The reconstructed parent uses its separate frozen classpath. It has no coordinate
transport property, so the driver does not supply that property for this arm.
This compares source versions with different ownership code and is supplemental
to the same-source coordinate ablation. It must not be labelled a preserved
baseline or Stage-2 measurement.

```bash
python3 result-coordinate-evidence/run-coordinate-experiments.py \
  --classpath-file result-coordinate-evidence/frozen-current/classpath.txt \
  --parent-classpath-file result-coordinate-evidence/fresh-parent-e7c6ca/frozen/classpath.txt \
  --java /path/to/jdk-25/bin/java \
  --source OWNERSHIP_FIX_COMMIT_AND_PATCH_ID \
  --parent-source fresh-parent-e7c6ca+overlay-845db9492e0524849b575e46825b39fee8d6805925f0200131caca942ef9b22c \
  --modes fresh-parent-e7c6ca,off --suite ablation \
  --heap 1024m --forks 4 --warmup 5 --iterations 5 \
  --output result-coordinate-evidence/fresh-parent-versus-reuse-rerun
```

Each source has its own binary manifest, verified before and after the serial
forks. JVM settings, graph/query digests, expected results, ledger limits, and
consumption pattern still have to match. Both sources retain the successful
selection observer and the diagnostic peak reset; absent coordinate admission
diagnostics are recorded as unavailable on the parent.

## Failure handling and analysis

Each fork keeps `command.json`, `console.log`, `samples.jsonl`, and its store.
The top-level directory contains configuration, a classpath digest manifest,
raw collected records, fork medians, and paired ratios. Repeated observations
inside one JVM are reduced to a fork median before calculating each matched
pair's candidate/baseline ratio. The first `--modes` entry is the baseline.
Ratios below one favor the candidate for time/bytes. Changes in observed native
operator/tier signatures are counted explicitly; their effects must be separated
from representation overhead.

Failed, invalid, timed-out, and incomplete forks are excluded, not converted
into zeroes or silently omitted. A SIGKILL is labelled a kill of unknown cause
unless an explicit JVM OutOfMemoryError was observed. Pair summaries require
both matching arms to complete with identical input/query/expected-result and
ledger settings. The driver stops after the first failed fork unless
`--keep-going` is explicitly supplied. Its default per-fork timeout is 240s.

These failure-handling rules apply to `run-coordinate-experiments.py`. The
root-qualified confirmation uses a stricter prespecified rule: any failed fork
or root audit stops the control without replacement and blocks its aggregates.
All raw records remain preserved. A successful original protocol with no
exclusions is not retrospectively classified by the later root audit.

The process ledger includes only reservations claimed by the implementation.
A low peak does not prove complete accounting. Ownership/accounting regression
tests remain a prerequisite for interpreting allocation and latency results.
