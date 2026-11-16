#!/usr/bin/env python3
"""V2 directory-absence diagnostic; its timings are deliberately not analyzed."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import signal
import subprocess
import time

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
JAVA = '/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8/bin/java'
MAIN = 'org.eclipse.rdf4j.sail.lmdb.evaluation.RootQualifiedOptionalFanoutLauncher'
spec = importlib.util.spec_from_file_location('preserved_runtime_identity',
        ROOT / 'result-coordinate-evidence/materializer-followup/runtime_identity.py')
identity = importlib.util.module_from_spec(spec)
spec.loader.exec_module(identity)
SCOPES = ['frameRetain', 'frameRelease', 'batchRetain', 'batchRelease',
          'directRetain', 'directRelease', 'otherLookup', 'retirementLink']


def assert_check(condition, message):
    if not condition:
        raise ValueError(message)


def validate_result(records, root_audit, descriptor):
    assert_check(len(records) == 5 and records[0].get('type') == 'metadata', 'Expected metadata plus four queries')
    metadata, samples = records[0], records[1:]
    assert_check(metadata['source'] == descriptor['sourceIdentity'], 'Wrong diagnostic source identity')
    expected = {'nativeEvaluation': True, 'directAdjacency': True, 'maxHeapBytes': 1073741824,
                'javaVersion': '25.0.5+8-LTS', 'warmup': 3, 'measurementsToRun': 1,
                'canonicalGraphSha256': 'cf637a7e80dbd9468d7e8b04116da072f726038282dfab15380c267c8cc82e9b',
                'querySha256': '9cffa2dc37bbb1e2743442b228d5ab9a41115bf7c4a6667506231fbda3bfc301',
                'expectedRows': 1152000, 'expectedChecksum': '3123987218479850048',
                'namespaceExperimentHarness': 'native-and-direct-adjacency-v1'}
    assert_check(all(metadata.get(k) == v for k, v in expected.items()), 'Fixture/JVM/configuration differs')
    setup = metadata['adjacencySetup']
    assert_check(setup['state'] == 'ACTIVE' and setup['currentRevisionReady']
                 and setup['appliedRevision'] == setup['currentDataRevision'], 'Adjacency not exact and ACTIVE before queries')
    assert_check([s['iteration'] for s in samples] == [-3, -2, -1, 0], 'Unexpected query sequence')
    assert_check([s['stage'] for s in samples] == ['warmup', 'warmup', 'warmup', 'measurement'],
                 'Unexpected warmup/measurement stages')
    target_reasons, queries = [], []
    for sample in samples:
        assert_check(sample['type'] == 'sample' and sample['valid'] and sample['complete']
                     and sample['rows'] == 1152000 and sample['checksum'] == '3123987218479850048'
                     and sample['ledgerBeforeBytes'] == 0 and sample['ledgerAfterBytes'] == 0,
                     'Invalid result or unreleased query ledger')
        adjacency = sample['adjacency']
        assert_check(adjacency['stateBefore'] == adjacency['stateAfter'] == 'ACTIVE'
                     and adjacency['appliedRevisionBefore'] == adjacency['currentRevisionBefore']
                     == adjacency['appliedRevisionAfter'] == adjacency['currentRevisionAfter']
                     == setup['currentDataRevision'], 'Adjacency readiness/revision changed during read-only query')
        counters = sample['counters']
        assert_check(counters['semanticNativeCompiles'] == 1, 'Expected one semantic native compilation per query')
        ns = sample['namespaceDiagnostics']
        admissions = counters['coordinateAdmissions']
        assert_check(ns['closedOwners'] == admissions['opened'], 'Closed diagnostic owner count does not match admissions')
        assert_check(all(value >= 0 for value in ns.values()), 'Negative diagnostic counter delta')
        assert_check(sample['autoRequested'] and sample['successfulSelections']
                     and not any('forced' in item['mode'] for item in sample['successfulSelections']),
                     'AUTO selection was absent or forced')
        if adjacency['lookupHits'] <= 0:
            target_reasons.append(f"iteration {sample['iteration']}: no query-time direct-adjacency lookup hits")
        if counters['irOpened'] <= 0 or counters['irRows'] <= 0 or ns['closedOwners'] <= 0:
            target_reasons.append(f"iteration {sample['iteration']}: target IR coordinate carrier did not execute")
        if sample['stage'] == 'measurement' and (counters['compiledBinds'] <= 0 or counters['interpretedBinds'] != 0):
            target_reasons.append(f"iteration {sample['iteration']}: measured compiled-IR-only tier not established")
        misses = sum(ns[scope + '.directoryMisses'] for scope in SCOPES)
        if misses:
            target_reasons.append(f"iteration {sample['iteration']}: {misses} directory misses require explanation")
        ranges = {}
        for scope in SCOPES[:4]:
            values = {metric: ns[scope + '.' + metric] for metric in [
                'rangeCalls', 'visitedCells', 'positionedCells', 'managedCells', 'managedTransitions',
                'managedRuns', 'singletonManagedRanges', 'singletonPhysicalRanges', 'emptyManagedRanges',
                'referenceCalls', 'managedReferenceCalls', 'directoryLookups', 'directoryProbes',
                'directoryAbsentRangeCalls', 'directoryAbsentVisitedCells']}
            assert_check(0 <= values['directoryAbsentRangeCalls'] <= values['rangeCalls']
                         and 0 <= values['directoryAbsentVisitedCells'] <= values['visitedCells'],
                         'Invalid directory-absence counters for ' + scope)
            values['directoryAbsentRangeFraction'] = (values['directoryAbsentRangeCalls'] / values['rangeCalls']
                                                      if values['rangeCalls'] else None)
            values['predictedMemoHits'] = values['managedCells'] - values['managedRuns']
            assert_check(values['referenceCalls'] == values['positionedCells']
                         and values['managedReferenceCalls'] == values['managedCells']
                         and values['managedRuns'] == values['managedTransitions']
                         + values['rangeCalls'] - values['emptyManagedRanges']
                         and values['predictedMemoHits'] >= 0,
                         'Range diagnostic counters do not reconcile for ' + scope)
            if ns[scope + '.directoryMisses'] == 0:
                assert_check(values['managedCells'] == values['directoryLookups'],
                             'Original range directory lookups do not match managed cells for ' + scope)
            values['predictedMemoHitFraction'] = (values['predictedMemoHits'] / values['managedCells']
                                                if values['managedCells'] else None)
            ranges[scope] = values
        queries.append({'iteration': sample['iteration'], 'stage': sample['stage'],
                        'closedOwners': ns['closedOwners'], 'compiledBinds': counters['compiledBinds'],
                        'interpretedBinds': counters['interpretedBinds'],
                        'successfulSelections': sample['successfulSelections'], 'adjacency': adjacency,
                        'ranges': ranges, 'namespaceDiagnostics': ns})
    assert_check(root_audit['format'] == 1 and root_audit['type'] == 'root-host-audit'
                 and root_audit['readPhase'] == 'after-original-harness-main'
                 and root_audit['harnessReturned'] and root_audit['hostedGenericTotal'] == 0
                 and root_audit['semanticNativeCompilesTotal'] == 4
                 == sum(s['counters']['semanticNativeCompiles'] for s in samples),
                 'Post-main root audit did not establish four native roots')
    return {'resultValidity': 'passed', 'targetQualified': not target_reasons,
            'targetQualificationReasons': target_reasons, 'queries': queries,
            'rootAudit': root_audit,
            'timingInterpretation': 'None. Instrumented diagnostic fork; no timings used as performance evidence.',
            'counterInterpretation': 'Managed transitions ignore sentinel and unbound gaps, matching a one-entry per-range memo. Directory probes count actually examined table slots. Retain/release imbalances are reported, not classified as leaks; owner close can discard live bookkeeping.',
            'adjacencyInterpretation': 'Positive existing lookupHits establish query-time successful direct-adjacency use. They do not prove every result row or only execution-phase operations used adjacency; plannerStatsHits alone never qualify the target.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runtime-descriptor', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--slot-released', action='store_true', required=True)
    args = parser.parse_args()
    for name in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'):
        assert_check(not os.environ.get(name), 'Unprescribed JVM environment variable ' + name)
    descriptor_path = args.runtime_descriptor.resolve(strict=True)
    descriptor = json.loads(descriptor_path.read_text())
    assert_check(descriptor['diagnosticOnly'] and descriptor['diagnosticVersion'] == 2,
                 'A v2 directory-absence diagnostic shadow runtime is required')
    for path_key, hash_key in [('classpathFile','classpathFileSha256'),
                              ('classpathManifestFile','classpathManifestSha256'),
                              ('sourceProvenanceFile','sourceProvenanceSha256')]:
        assert_check(identity.sha(descriptor[path_key]) == descriptor[hash_key], 'Changed descriptor input ' + path_key)
    cp = Path(descriptor['classpathFile']).read_text().strip()
    before_manifest = identity.classpath_manifest(cp)
    assert_check(before_manifest == json.loads(Path(descriptor['classpathManifestFile']).read_text()), 'Changed diagnostic runtime')
    original_cp, original_manifest = identity.original_identity()
    assert_check(len(before_manifest) == 129 and before_manifest[-127:] == original_manifest, 'Original shared classpath changed')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    identity.write_new(output / 'runtime-descriptor.json', descriptor)
    identity.write_new(output / 'classpath-manifest-before.json', before_manifest)
    driver_hash = identity.sha(Path(__file__))
    descriptor_hash = identity.sha(descriptor_path)
    argv = [JAVA, '-Xms1024m', '-Xmx1024m', '-XX:+UseG1GC',
            '-Drdf4j.lmdb.hotCounters=true', '-Drdf4j.lmdb.resultCoordinateTransport=dense',
            '-cp', cp, MAIN, '--arm=diagnostic-original-directory-absence-v2', '--source=' + descriptor['sourceIdentity'],
            '--warmup=3', '--iterations=1', '--measurements=100', '--readings=10', '--width=12',
            '--statuses=2', '--unique-payloads=0', '--consumption=full',
            '--output=' + str(output / 'samples.jsonl'), '--store=' + str(output / 'store')]
    identity.write_new(output / 'command.json', {'argv': argv, 'cwd': str(ROOT),
        'driverSha256': driver_hash, 'descriptorSha256': descriptor_hash, 'timingUse': 'diagnostic-only'})
    start = time.time_ns()
    with (output / 'console.log').open('xb') as log:
        process = subprocess.Popen(argv, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=180)
        except BaseException:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
            raise
    identity.write_new(output / 'process-result.json', {'exitCode': code, 'startedEpochNs': start,
                                                      'finishedEpochNs': time.time_ns()})
    assert_check(code == 0, 'Diagnostic JVM failed; see preserved console and JSONL')
    records = [json.loads(line) for line in (output / 'samples.jsonl').read_text().splitlines() if line.strip()]
    audit = json.loads((output / 'root-host-audit.json').read_text())
    result = validate_result(records, audit, descriptor)
    result['frameGuardEvidence'] = [{
        'iteration': q['iteration'], 'stage': q['stage'],
        'allFrameRangesEnteredWithoutDirectory': all(
            q['ranges'][scope]['rangeCalls'] > 0
            and q['ranges'][scope]['directoryAbsentRangeCalls'] == q['ranges'][scope]['rangeCalls']
            and q['ranges'][scope]['directoryAbsentVisitedCells'] == q['ranges'][scope]['visitedCells']
            for scope in ('frameRetain', 'frameRelease'))
    } for q in result['queries']]
    result['preservation'] = {'runtimeUnchanged': before_manifest == identity.classpath_manifest(cp),
                              'driverUnchanged': driver_hash == identity.sha(Path(__file__)),
                              'descriptorUnchanged': descriptor_hash == identity.sha(descriptor_path)}
    assert_check(all(result['preservation'].values()), 'Diagnostic runtime or driver changed during execution')
    identity.write_new(output / 'diagnostic-summary.json', result)
    print(json.dumps({'output': str(output), 'resultValidity': result['resultValidity'],
                      'targetQualified': result['targetQualified'],
                      'targetQualificationReasons': result['targetQualificationReasons'],
                      'queries': [{'iteration': q['iteration'], 'compiledBinds': q['compiledBinds'],
                                   'interpretedBinds': q['interpretedBinds'], 'ranges': q['ranges']}
                                  for q in result['queries']]}, indent=2))


if __name__ == '__main__':
    main()
