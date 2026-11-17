# Default-off compact sequential lookup hint

Property: `rdf4j.sail.compactSequentialLookup=true` (default false).

## Scope and semantics

A private per-row integer predicts the next compact binding index in `getValue`. An exact `String.equals` match is required before the predicted value is used. A miss uses the existing hash-table lookup. A successful lookup advances the prediction, wrapping at the final binding; null or absent names leave it unchanged. No query-order certificate or projection assumption is used for correctness. `hasBinding`, snapshot construction and other operations are unchanged.

Once a row materializes, `getValue` delegates directly to the existing map and does not use the prediction. The hint holds no Value reference and changes no ownership/materialization boundary. The new integer field exists in both flag states, so the HTTP OFF/ON comparison must use the same candidate API classes. Object-layout/alignment effects are not assumed to be zero; actual latency and allocation measurements decide whether this experiment is useful.

This only targets `getValue` hash lookups, not the hash work while appending/building snapshots. Performance benefit is not established by the safety tests.

## Verified safety

Final focused Maven suite: **42 tests passed with the hint OFF and 42 with it ON**, zero failures/errors/skips. Classes: CompactBindingSetSnapshotTest (19), CompactIdentityMappingTest (13), CompactSequentialLookupTest (10). The two assertions-disabled-specific methods return early in assertion-enabled runs; the additional driver below executes the relevant methods with language assertions disabled.

Additional assertions-disabled qualification: **23 tests passed OFF and 23 ON** (all new sequential and identity-mapping tests), including null names/values allowed by the existing assertion-disabled contract.

Coverage includes repeated/out-of-order names, new String objects equal to the stored names, 10,000 deterministic random lookups, missing/unbound/null names, empty rows, colliding String hashes and negative hashes, hint wrap and miss behavior, public binding/name/iterator materialization transitions, value replacement/removal/addition, callback mutation, nested mapping/reentrant lookup and retained Value identity. These are focused tests, not a full-module/repository pass or a proof of arbitrary concurrent mutation safety.

The frozen `sequential-api-classes` was copied after the initial successful suites. A final null-value test was then added and both suites rerun. No production source changed; a byte-for-byte recursive comparison of the final production classes against the frozen classes was clean. The earlier transport API/query/HTTP-client frozen directories were not modified.

## Reproduce from the repository root

Prerequisites are the same Java25/Maven tools and dependency cache described in QUALIFICATION.md. These commands were actually run (the shell loop below combines the two otherwise identical invocations).

```bash
for mode in false true; do
  if [ "$mode" = false ]; then label=off; else label=on; fi
  bash experiments/overlay/lifetime/mvn.sh \
    -pl core/sail/api -am \
    -Dtest=CompactIdentityMappingTest,CompactBindingSetSnapshotTest,CompactSequentialLookupTest \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Drdf4j.sail.compactResultSnapshots=true \
    -Drdf4j.sail.compactSequentialLookup="$mode" \
    test > "experiments/overlay/transport-mapping/sequential-$label-tests.log" 2>&1 || exit 1
done
```

Freeze into a previously absent destination; never replace it during a timing matrix:

```bash
test ! -e experiments/overlay/transport-mapping/sequential-api-classes &&
  cp -a core/sail/api/target/classes experiments/overlay/transport-mapping/sequential-api-classes
```

Assertions-disabled driver, using the successful Maven test classpath:

```bash
python - <<'PY'
import pathlib, xml.etree.ElementTree as E, subprocess
p = pathlib.Path('experiments/overlay/transport-mapping')
r = E.parse('core/sail/api/target/surefire-reports/TEST-org.eclipse.rdf4j.sail.features.CompactSequentialLookupTest.xml').getroot()
cp = next(x.attrib['value'] for x in r.find('properties') if x.attrib['name'] == 'java.class.path')
java = 'experiments/overlay/tools/jdk-25.0.5+8/bin/'
subprocess.run([java+'javac', '-cp', cp, '-d', str(p/'harness'),
                str(p/'SequentialAssertionsDisabledQualification.java')], check=True)
with (p/'sequential-assertions-disabled.txt').open('w') as out:
    for flag in ['false', 'true']:
        result = subprocess.check_output([
            java+'java', '-da', '-Drdf4j.sail.compactResultSnapshots=true',
            '-Drdf4j.sail.compactSequentialLookup='+flag,
            '-cp', str(p/'harness')+':'+cp,
            'org.eclipse.rdf4j.sail.features.SequentialAssertionsDisabledQualification'], text=True)
        out.write(result)
        print(result.strip())
PY
```

Surefire XML and full Maven logs remain local because they contain environment paths. Sanitized test counts and standalone PASS receipts are checked in. The parent benchmark harness owns actual HTTP exactness and matched first-row/window/completion measurements; do not pool these safety-run timings with those measurements.
