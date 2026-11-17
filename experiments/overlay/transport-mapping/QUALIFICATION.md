# Experimental compact identity-preserving transport mapping

Default-off flag: `rdf4j.http.compactIdentityMapping=true`.

## Change

An internal query-module capability lets compact snapshots map values without exposing a binding iterator on the identity path. BNodeOriginTransport uses it only with the flag enabled and only for implementing rows. Generic rows use the original loop. The compact implementation visits all actual bindings, not a projected-name list; preserves Value identity; allocates an independent shallow map only for changed values; and falls back to ordinary map traversal for already-materialized rows.

Callback-triggered materialization does not restart mapping. Later bindings come from the current map, while the current pre-callback Binding is preserved for comparison. Structural modifications are checked before the next original binding, matching LinkedHashMap iteration, including no post-last-callback check. Public iterator, mutable name-view and serialization behavior are unchanged.

## Verification

- Maven reactor selection: core/sail/api and core/http/client with dependencies; Java25. Focused classes CompactIdentityMappingTest, CompactBindingSetSnapshotTest, BNodeOriginTransportTest, BNodeOriginTransportStreamingTest.
- 44 tests passed:13 new callback/mutation parity tests,19 existing compact snapshot tests,12 existing blank-node transport/streaming tests. Both experiment flags enabled.
- Standalone invocation of all13 new tests with assertions disabled passed, including null-binding cases.
- Actual compact snapshot plus BNodeOriginTransport integration passed with the dispatch flag OFF and ON: identity/callback-order, stored blank-node and nested RDF-star triple roundtrip, retained values, independent changed rows. Reflection verified OFF materializes the compact map and ON leaves it compact for identity mapping.
- Frozen query-classes, api-classes and http-client-classes are isolated under this directory for paired HTTP benchmarking. Both arms must use the same frozen outputs and differ only in the dispatch property.
- No full-repository or full HTTP module test claim. These tests do not establish throughput improvement; matched Workbench timings are separate.

The first test compile caught a missing test-only Resource cast; it was corrected before the successful run. No production correctness failure was observed in this focused qualification.

## Exact reproduction commands

Run from the repository root at source checkpoint `6e6bc3eaeb` (or a descendant with the same candidate sources). These are the invocations used for qualification, with all paths made repository-relative. They are recorded here without rerunning them during the timed HTTP matrix. Prerequisites: the existing Java25/Maven tools used by the overlay experiments, dependency cache/network configuration accepted by `lifetime/mvn.sh`, and the prepared Workbench runtime jars under `workbench-e2e/runtime/BOOT-INF/lib`. See the Workbench harness preparation script for that runtime. These commands do not start a Workbench timing trial.

### Focused build and tests

```bash
bash experiments/overlay/lifetime/mvn.sh \
  -pl core/sail/api,core/http/client -am \
  -Dtest=CompactIdentityMappingTest,CompactBindingSetSnapshotTest,BNodeOriginTransportTest,BNodeOriginTransportStreamingTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Drdf4j.sail.compactResultSnapshots=true \
  -Drdf4j.http.compactIdentityMapping=true \
  test > experiments/overlay/transport-mapping-tests.log 2>&1
```

Verify `BUILD SUCCESS` and the four per-class test reports. The selected suite ran 44 tests, zero failures/errors/skips. The null-binding method returns early when JVM assertions are enabled; the separate `-da` run below exercises that case.

### Freeze the tested outputs

The following `cp -a` invocations were run once when the three destination class directories did not exist. Use fresh destinations when reproducing, rather than copying into an existing directory and accidentally nesting `classes/` or mixing outputs. Never replace frozen outputs during a timing matrix.

```bash
mkdir -p experiments/overlay/transport-mapping
cp -a core/query/target/classes experiments/overlay/transport-mapping/query-classes
cp -a core/sail/api/target/classes experiments/overlay/transport-mapping/api-classes
cp -a core/http/client/target/classes experiments/overlay/transport-mapping/http-client-classes
```

### Actual compact snapshot plus HTTP transport integration

This compiles the standalone qualification against the frozen three modules, ahead of the prepared runtime jars. It runs OFF and ON in separate JVMs because the property is read into a static final field.

```bash
python - <<'PY'
import pathlib, subprocess
p = pathlib.Path('experiments/overlay/transport-mapping')
libs = sorted(pathlib.Path('experiments/overlay/workbench-e2e/runtime/BOOT-INF/lib').glob('*.jar'))
cp = ':'.join(map(str, [p/'query-classes', p/'api-classes', p/'http-client-classes', *libs]))
java = 'experiments/overlay/tools/jdk-25.0.5+8/bin/'
subprocess.run([java+'javac', '-cp', cp, '-d', str(p/'harness'),
                str(p/'TransportMappingQualification.java')], check=True)
with (p/'integration-qualification.txt').open('w') as out:
    for flag in ['false', 'true']:
        result = subprocess.check_output([
            java+'java', '-ea', '-Drdf4j.http.compactIdentityMapping='+flag,
            '-cp', str(p/'harness')+':'+cp,
            'org.eclipse.rdf4j.sail.features.TransportMappingQualification'], text=True)
        out.write(result)
        print(result.strip())
PY
```

### Assertions-disabled callback parity

Use the test classpath recorded by the successful Maven run above. This launches a separate JVM with `-da` and invokes all 13 new JUnit test methods through the checked-in standalone driver; JUnit assertions remain active even though Java language assertions are disabled.

```bash
python - <<'PY'
import xml.etree.ElementTree as E
import pathlib, subprocess
report = 'core/sail/api/target/surefire-reports/TEST-org.eclipse.rdf4j.sail.features.CompactIdentityMappingTest.xml'
r = E.parse(report).getroot()
cp = next(x.attrib['value'] for x in r.find('properties') if x.attrib['name'] == 'java.class.path')
p = pathlib.Path('experiments/overlay/transport-mapping')
java = 'experiments/overlay/tools/jdk-25.0.5+8/bin/'
subprocess.run([java+'javac', '-cp', cp, '-d', str(p/'harness'),
                str(p/'AssertionsDisabledQualification.java')], check=True)
result = subprocess.check_output([
    java+'java', '-da', '-Drdf4j.sail.compactResultSnapshots=true',
    '-cp', str(p/'harness')+':'+cp,
    'org.eclipse.rdf4j.sail.features.AssertionsDisabledQualification'], text=True)
(p/'assertions-disabled-qualification.txt').write_text(result)
print(result)
PY
```

The Surefire XML is local build output and contains machine-specific paths; it is read locally rather than copied into published evidence. Published qualification receipts contain only PASS descriptions.
