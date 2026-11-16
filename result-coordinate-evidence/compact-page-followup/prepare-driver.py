#!/usr/bin/env python3
"""Create the additive dual-fixture driver from the previously reviewed serial driver."""
from pathlib import Path

HERE = Path(__file__).resolve().parent
OLD = HERE.parent / 'namespace-followup/frame-directory-guard/run-comparison.py'
s = OLD.read_text()
s = s.replace('Freeze or execute the prospective three-arm frame-directory-guard comparison.',
              'Freeze or execute the prospective compact-page comparison on two fixtures.')
s = s.replace('ROOT = HERE.parents[2]', 'ROOT = HERE.parents[1]')
s = s.replace("IDENTITY_FILE = ROOT / 'result-coordinate-evidence/materializer-followup/runtime_identity.py'",
              "IDENTITY_FILE = HERE / 'runtime_identity.py'")
s = s.replace('original-off', 'current-off').replace('original-dense', 'current-dense').replace('candidate-dense', 'compact-dense')
s = s.replace('frame-directory-guard', 'compact-page').replace('+namespace-harness-sha256-', '+compact-page-harness-sha256-')
s = s.replace('core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java',
              'core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java')
# Runtime labels remain original/candidate internally; public arm labels identify current versus compact pages.

def function(name, body):
    global s
    start = s.index('def ' + name + '(')
    end = s.find('\ndef ', start + 1)
    if end == -1:
        end = s.index("\nif __name__ == '__main__':", start)
    s = s[:start] + body.rstrip() + '\n\n' + s[end + 1:]

insert = '''
FIXTURE_ORDER = ['fanout', 'shared-tail']
FIXTURE_ORDERS = [FIXTURE_ORDER if i % 2 == 0 else list(reversed(FIXTURE_ORDER)) for i in range(6)]
ACCEPTANCE = {'sharedTailLedgerMaximumRatioEveryPair': 0.25,
              'sharedTailAllocationMaximumExclusiveRatioEveryPair': 1.0,
              'medianLatencyMaximumRatioEachFixture': 1.0,
              'individualLatencyMaximumRatioEitherFixture': 1.10,
              'interpretation': 'Prospective engineering acceptance screen for a memory optimization; not statistical noninferiority.',
              'consistentLatencyWinRequiresAllSixRatiosBelowOne': True}
FIXTURES = {
    'fanout': {
        'launcher': 'org.eclipse.rdf4j.sail.lmdb.evaluation.RootQualifiedOptionalFanoutLauncher',
        'harnessMain': 'org.eclipse.rdf4j.sail.lmdb.benchmark.OptionalFanoutExperiment',
        'rootReadPhase': 'after-original-harness-main',
        'arguments': ['--measurements=100', '--readings=10', '--width=12', '--statuses=2', '--unique-payloads=0'],
        'metadata': {'fixture': 'optional-fanout', 'measurements': 100, 'readings': 10, 'width': 12,
            'statuses': 2, 'uniquePayloads': 0, 'expectedRows': 1152000, 'expectedChecksum': '3123987218479850048',
            'canonicalGraphSha256': 'cf637a7e80dbd9468d7e8b04116da072f726038282dfab15380c267c8cc82e9b',
            'querySha256': '9cffa2dc37bbb1e2743442b228d5ab9a41115bf7c4a6667506231fbda3bfc301'},
        'oracle': 'Independent full-bag row count and checksum; original consumption unchanged.'},
    'shared-tail': {
        'launcher': 'org.eclipse.rdf4j.sail.lmdb.evaluation.RootQualifiedSharedSelectionLauncher',
        'harnessMain': 'org.eclipse.rdf4j.sail.lmdb.benchmark.SharedSelectionExperiment',
        'rootReadPhase': 'after-shared-selection-harness-main',
        'arguments': ['--roots=64', '--left=128', '--right=128', '--tuple-width=2'],
        'metadata': {'fixture': 'coherent-two-optional-tail-tuples', 'roots': 64, 'left': 128, 'right': 128,
            'tupleWidth': 2, 'width': 6, 'expectedRows': 1048576, 'expectedChecksum': '8327963502356915200',
            'statements': 32832, 'expectedMultiplicityPerCompleteRow': 1,
            'fixtureInsertionSha256': '6c6b154e249781a76e84cb0ccf614ee7a3248869f41e852e002639f935de6433',
            'canonicalGraphSha256': '59218d96a3c4c1cd3a2634c72174b0f44f3a7415d6cae4c944b1a54ed9506621',
            'querySha256': '570d85a6fd11ecabae385744a2137f45aad5c9e53bf74dd50b2de69f3c2a4b76',
            'compactPageSharedTailHarness': 'adjacency-observation-v1'},
        'oracle': 'Exact independent IRI types, lexical values, tuple/parent association, legal unique dense row keys, complete count/checksum.'}}
'''
s = s.replace('\ndef check(value, message):', insert + '\n\ndef check(value, message):', 1)
function('validate_design', '''def validate_design(protocol):
    check(protocol['arms'] == ARMS and protocol['armDefinitions'] == ARM_DEFINITIONS, 'Prespecified arms changed')
    check(protocol['balancedOrders'] == ORDERS and protocol['rounds'] == 6
          and protocol['warmup'] == 8 and protocol['iterations'] == 5
          and protocol['fixtureOrdersByRound'] == FIXTURE_ORDERS and protocol['fixtures'] == FIXTURES,
          'Prespecified dual-fixture six-permutation 8+5 design changed')
    check(protocol['java'] == JAVA and protocol['jvmArguments'] == JVM, 'Prespecified JVM changed')
    check(protocol['configuration'] == {
        'nativeEvaluation': True, 'directAdjacency': True, 'directAdjacencyMode': 'PREFER',
        'valueOverlay': False, 'forceSync': False, 'indexes': 'spoc,ospc,psoc,posc',
        'globalLedgerLimitBytes': 1073741824, 'perQueryLedgerLimitBytes': 268435456},
        'Prespecified store/query-memory configuration changed')
    check(protocol['engineeringAcceptance'] == ACCEPTANCE, 'Prospective engineering acceptance screen changed')
    check(protocol['statistics']['metrics'] == METRICS and protocol['timeoutSeconds'] == 300,
          'Prespecified analysis/deadline changed')
''')
s = s.replace("'Candidate is not the verified narrow frame guard'", "'Candidate is not the verified compact-page representation'")
s = s.replace("'Common uninstrumented v1 harness is not exact'", "'Common dual-fixture harness is not exact'")
s = s.replace("'name': 'compact-page-common-harness'", "'name': 'compact-page-candidate-common-harness'")
function('freeze_protocol', '''def freeze_protocol(args):
    draft = json.loads(args.protocol.read_text())
    validate_design(draft)
    check(draft['status'] == 'draft-candidate-validation-required', 'Not an unfrozen compact-page draft')
    common_path = Path(draft['commonHarnessDescriptorFile']).resolve(strict=True)
    if draft['commonHarnessDescriptorFileSha256'] is not None:
        check(identity.sha(common_path) == draft['commonHarnessDescriptorFileSha256'], 'Common descriptor changed')
    original = json.loads(common_path.read_text())
    check(original['sharedOracleAndLaunchersUnchanged'] and original['fanoutHarnessAndAdjacencyHelperUnchanged']
          and original['fixtures'] == FIXTURE_ORDER, 'Common prefix did not preserve fixtures and launchers')
    base = json.loads(args.freeze_candidate.read_text())
    check(args.reviewed_production_patch_sha256
          and args.reviewed_production_patch_sha256 == base['productionPatchSha256'],
          'Candidate patch does not match the explicitly reviewed production patch hash')
    output = args.frozen_protocol.resolve()
    check(not output.exists(), 'Refusing to overwrite a frozen protocol')
    candidate = make_candidate(args.freeze_candidate, original, output.parent / 'candidate-runtime')
    draft.update({'status': 'frozen-ready', 'frozenEpochNs': time.time_ns(),
                  'commonHarnessDescriptorFileSha256': identity.sha(common_path),
                  'driverSha256': identity.sha(Path(__file__)),
                  'identityHelperSha256': identity.sha(IDENTITY_FILE),
                  'reviewedProductionPatchSha256': args.reviewed_production_patch_sha256,
                  'freezeHelperFile': str(HERE / 'freeze-candidate-runtime.py'),
                  'freezeHelperFileSha256': identity.sha(HERE / 'freeze-candidate-runtime.py'),
                  'runtimes': {'original': original, 'candidate': candidate}})
    identity.write_new(output, draft)
    validate_protocol(output)
    print(json.dumps({'frozenProtocol': str(output), 'sha256': identity.sha(output), 'jvmsLaunched': 0}))
''')
start = s.index("    diagnostic = verified_file(protocol['diagnosticEvidenceFile']")
end = s.index('    _, base = identity.original_identity()', start)
s = s[:start] + "    verified_file(protocol['motivationEvidenceFile'], protocol['motivationEvidenceFileSha256'])\n" + s[end:]
s = s.replace("'Original descriptor differs from the common v1 runtime'", "'Original descriptor differs from the compiled dual-fixture common runtime'")
function('validate_run', '''def validate_run(folder, fixture_name, arm, runtime, mode):
    fixture = FIXTURES[fixture_name]
    expected_rows, expected_checksum = fixture['metadata']['expectedRows'], fixture['metadata']['expectedChecksum']
    records = [json.loads(line) for line in (folder / 'samples.jsonl').read_text().splitlines() if line.strip()]
    check(len(records) == 14 and records[0].get('type') == 'metadata', 'Expected metadata plus thirteen queries')
    metadata, samples = records[0], records[1:]
    check([s['iteration'] for s in samples] == list(range(-8, 5))
          and [s['stage'] for s in samples] == ['warmup'] * 8 + ['measurement'] * 5, 'Unexpected query chronology')
    check(metadata['arm'] == arm and metadata['source'] == runtime['descriptor']['sourceIdentity'],
          'Source/arm identity mismatch')
    expected = {**fixture['metadata'], 'consumption': 'full',
        'nativeEvaluation': True, 'directAdjacency': True, 'valueOverlay': False, 'forceSync': False,
        'directAdjacencyMode': 'PREFER', 'globalLedgerLimitBytes': 1073741824, 'perQueryLedgerLimitBytes': 268435456,
        'indexes': 'spoc,ospc,psoc,posc', 'maxHeapBytes': 1073741824, 'warmup': 8,
        'measurementsToRun': 5, 'javaVersion': '25.0.5+8-LTS',
        'namespaceExperimentHarness': 'native-and-direct-adjacency-v1'}
    check(all(metadata.get(k) == v for k, v in expected.items()), 'Fixture/JVM/configuration differs')
    check(metadata['jvmArguments'] == JVM + ['-D' + PROPERTY + '=' + mode]
          and metadata['systemProperties'] == {'rdf4j.lmdb.hotCounters': 'true', PROPERTY: mode},
          'Unexpected JVM arguments, properties or instrumentation')
    setup = metadata['adjacencySetup']
    check(setup['state'] == 'ACTIVE' and setup['currentRevisionReady']
          and setup['appliedRevision'] == setup['currentDataRevision'], 'Adjacency not ready before queries')
    reasons = []
    for sample in samples:
        check(sample['type'] == 'sample' and sample['arm'] == arm and sample['valid'] and sample['complete']
              and sample['rows'] == expected_rows and sample['checksum'] == expected_checksum
              and sample['ledgerBeforeBytes'] == sample['ledgerAfterBytes'] == 0
              and sample['ledgerReleased'] and all(sample[k] >= 0 for k in METRICS),
              'Invalid result, allocation, timing or unreleased ledger')
        if fixture_name == 'shared-tail':
            check(sample['exactBagValid'] and sample['rowCountValid'] and sample['checksumValid']
                  and sample['partialMembershipValid'], 'Shared-tail independent exact bag oracle failed')
        counters, adjacency = sample['counters'], sample['adjacency']
        check(counters['semanticNativeCompiles'] == 1, 'Expected exactly one native compile per query')
        check(adjacency['stateBefore'] == adjacency['stateAfter'] == 'ACTIVE'
              and adjacency['appliedRevisionBefore'] == adjacency['currentRevisionBefore']
              == adjacency['appliedRevisionAfter'] == adjacency['currentRevisionAfter']
              == setup['currentDataRevision'], 'Adjacency revision/readiness changed')
        admissions = counters['coordinateAdmissions']
        check(admissions and admissions['opened'] == (1 if mode == 'dense' else 0)
              and admissions['initialAdmissionRefused'] == admissions['growthAdmissionRefused'] == 0,
              'Coordinate admission differs from the prescribed arm')
        if not (sample['autoRequested'] and sample['auto'] and sample['successfulSelections']
                and not any('forced' in item['mode'] for item in sample['successfulSelections'])
                and counters['irOpened'] == 1 and counters['irRows'] == expected_rows
                and adjacency['lookupHits'] > 0):
            reasons.append('iteration %s: normal AUTO/native/adjacency target not observed' % sample['iteration'])
        if sample['stage'] == 'measurement' and not (counters['compiledBinds'] == 1
                and counters['interpretedBinds'] == counters['aggregateCompiledBinds']
                == counters['aggregateInterpretedBinds'] == 0):
            reasons.append('iteration %s: compiled-only measured IR not observed' % sample['iteration'])
    audit = json.loads((folder / 'root-host-audit.json').read_text())
    check(audit['format'] == 1 and audit['type'] == 'root-host-audit'
          and audit['readPhase'] == fixture['rootReadPhase'] and audit['harnessMain'] == fixture['harnessMain']
          and audit['harnessReturned'] and audit['harnessFailureClass'] is None
          and audit['semanticNativeCompilesTotal'] == sum(s['counters']['semanticNativeCompiles'] for s in samples) == 13,
          'Post-main root audit schema or compile reconciliation failed')
    if audit['hostedGenericTotal'] != 0:
        reasons.append('Generic outer root observed')
    validation = {'resultValidity': 'passed', 'targetQualified': not reasons, 'targetQualificationReasons': reasons,
                  'fixture': fixture_name, 'queryCount': len(samples), 'measuredQueryCount': 5,
                  'oracle': fixture['oracle'],
                  'adjacencyInterpretation': 'Positive lookupHits establish successful query-time direct-adjacency use; not every result row or exclusively execution-phase access.'}
    identity.write_new(folder / 'validation.json', validation)
    return metadata, samples, audit, samples[8:], validation
''')
function('summary', '''def summary(runs, complete, errors):
    result = {'complete': complete, 'errors': errors, 'completedJvms': len(runs),
        'rawQueryCount': sum(len(r['samples']) for r in runs),
        'rawMeasuredQueryCount': sum(sum(s['stage'] == 'measurement' for s in r['samples']) for r in runs),
        'comparisonStatus': 'complete prespecified dual-fixture protocol' if complete else 'incomplete; no accepted comparison estimates',
        'ratioDefinition': 'Ratio of fresh-JVM medians of all five measurements within the same round and fixture; no cross-fixture pooling.',
        'engineeringAcceptance': {'evaluated': False, 'prospectiveScreen': ACCEPTANCE}, 'comparisons': []}
    if not complete:
        return result
    indexed = {(r['fixture'], r['round'], r['arm']): r for r in runs}
    by_pair = {}
    for fixture_name in FIXTURE_ORDER:
        for candidate, comparator in PAIRS:
            pairs = []
            for round_index in range(6):
                a, b = indexed[fixture_name, round_index, candidate], indexed[fixture_name, round_index, comparator]
                pairs.append({'round': round_index,
                    'ratios': {k: a['medians'][k] / b['medians'][k] if b['medians'][k] else None for k in METRICS},
                    'routeTierHistogramsEqual': a['routeTierHistogram'] == b['routeTierHistogram']})
            values = {k: [p['ratios'][k] for p in pairs if p['ratios'][k] is not None] for k in METRICS}
            entry = {'fixture': fixture_name, 'candidate': candidate, 'comparator': comparator, 'pairs': pairs,
                'medianRatios': {k: statistics.median(v) if v else None for k, v in values.items()},
                'ratioRanges': {k: [min(v), max(v)] if v else None for k, v in values.items()},
                'allSixLatencyRatiosFavorCandidate': all(p['ratios']['totalNs'] < 1 for p in pairs),
                'allRouteTierHistogramsEqual': all(p['routeTierHistogramsEqual'] for p in pairs)}
            result['comparisons'].append(entry)
            by_pair[fixture_name, candidate, comparator] = entry
    tail = by_pair['shared-tail', 'compact-dense', 'current-dense']
    gates = [
        {'name': 'shared-tail peak ledger <=25% in every pair',
         'passed': all(p['ratios']['ledgerPeakBytes'] <= 0.25 for p in tail['pairs']),
         'ratios': [p['ratios']['ledgerPeakBytes'] for p in tail['pairs']]},
        {'name': 'shared-tail all-thread allocation lower in every pair',
         'passed': all(p['ratios']['allThreadAllocatedBytes'] < 1 for p in tail['pairs']),
         'ratios': [p['ratios']['allThreadAllocatedBytes'] for p in tail['pairs']]}]
    for fixture_name in FIXTURE_ORDER:
        entry = by_pair[fixture_name, 'compact-dense', 'current-dense']
        values = [p['ratios']['totalNs'] for p in entry['pairs']]
        gates += [
            {'name': fixture_name + ' median paired total latency <=1',
             'passed': statistics.median(values) <= 1, 'medianRatio': statistics.median(values)},
            {'name': fixture_name + ' every total latency ratio <=1.10',
             'passed': max(values) <= 1.10, 'ratios': values}]
    result['engineeringAcceptance'] = {'evaluated': True, 'prospectiveScreen': ACCEPTANCE, 'gates': gates,
        'passed': all(g['passed'] for g in gates),
        'interpretation': 'Engineering acceptance screen only; no statistical noninferiority claim. Root owns promotion or source-only restoration.'}
    return result
''')
function('execute', '''def execute(args):
    protocol_path = args.protocol.resolve(strict=True)
    protocol_sha = identity.sha(protocol_path)
    protocol, snapshots = validate_protocol(protocol_path)
    if args.validate_only:
        print('Frozen dual-fixture protocol/runtime validation passed; no JVM launched.')
        return
    check(args.slot_released and args.output is not None, 'Execution requires an exclusive slot and new output path')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    (output / 'used-protocol.json').write_bytes(protocol_path.read_bytes())
    watched = {str(protocol_path): protocol_sha, str(Path(__file__)): protocol['driverSha256'],
               str(IDENTITY_FILE): protocol['identityHelperSha256']}
    for key in ('motivationEvidenceFile', 'commonHarnessDescriptorFile', 'freezeHelperFile'):
        watched[protocol[key]] = protocol[key + 'Sha256']
    for name, runtime in snapshots.items():
        consumed = output / 'runtime-identities' / name
        consumed.mkdir(parents=True)
        identity.write_new(consumed / 'runtime-descriptor.json', runtime['descriptor'])
        for key, path in runtime['inputs'].items():
            watched[str(path)] = identity.sha(path)
            (consumed / (key + '-' + path.name)).write_bytes(path.read_bytes())
    identity.write_new(output / 'preflight.json', {'protocolSha256': protocol_sha,
        'runtimeManifests': {name: r['manifest'] for name, r in snapshots.items()}, 'watchedFiles': watched})
    runs, errors, metadata_references, route_references = [], [], {}, {}
    for round_index, order in enumerate(protocol['balancedOrders']):
        if errors:
            break
        for fixture_position, fixture_name in enumerate(protocol['fixtureOrdersByRound'][round_index]):
            if errors:
                break
            fixture = FIXTURES[fixture_name]
            for position, arm in enumerate(order):
                folder = output / f'round-{round_index:02d}-{fixture_position:02d}-{fixture_name}-{position:02d}-{arm}'
                folder.mkdir()
                arm_spec = protocol['armDefinitions'][arm]
                runtime, mode = snapshots[arm_spec['runtime']], arm_spec['transportMode']
                command = [JAVA, *JVM, '-D' + PROPERTY + '=' + mode, '-cp', runtime['classpath'], fixture['launcher'],
                    '--arm=' + arm, '--source=' + runtime['descriptor']['sourceIdentity'],
                    '--warmup=8', '--iterations=5', *fixture['arguments'], '--consumption=full',
                    '--output=' + str(folder / 'samples.jsonl'), '--store=' + str(folder / 'store')]
                print(f'Running round {round_index + 1}/6, {fixture_name}, position {position + 1}/3: {arm}', flush=True)
                try:
                    verified_file(protocol_path, protocol_sha)
                    verified_file(Path(__file__), protocol['driverSha256'])
                    verified_file(IDENTITY_FILE, protocol['identityHelperSha256'])
                    verified_file(protocol['freezeHelperFile'], protocol['freezeHelperFileSha256'])
                    check(identity.classpath_manifest(runtime['manifest'][0]['path']) == [runtime['manifest'][0]],
                          'Common uninstrumented harness changed before fork')
                    run_fork(command, folder, protocol['timeoutSeconds'])
                    metadata, samples, audit, measured, validation = validate_run(folder, fixture_name, arm, runtime, mode)
                    normalized = normalized_metadata(metadata)
                    if fixture_name not in metadata_references:
                        metadata_references[fixture_name] = normalized
                    check(normalized == metadata_references[fixture_name], 'Normalized input/JVM metadata differs within fixture')
                    measured_routes = route_histogram(measured)
                    if fixture_name not in route_references:
                        route_references[fixture_name] = measured_routes
                    same_route = measured_routes == route_references[fixture_name]
                    runs.append({'round': round_index, 'fixture': fixture_name, 'fixturePosition': fixture_position,
                        'position': position, 'arm': arm, 'runtime': arm_spec['runtime'], 'transportMode': mode,
                        'directory': str(folder), 'metadata': metadata, 'samples': samples, 'rootAudit': audit,
                        'validation': validation, 'medians': {k: statistics.median(s[k] for s in measured) for k in METRICS},
                        'counterMedians': {k: statistics.median(s['counters'][k] for s in measured)
                            for k, v in measured[0]['counters'].items() if isinstance(v, (int, float))},
                        'adjacencyLookupHits': [s['adjacency']['lookupHits'] for s in measured],
                        'routeTierHistogram': measured_routes, 'routeTierHistogramMatchesFirstFixtureFork': same_route})
                    check(validation['targetQualified'], '; '.join(validation['targetQualificationReasons']))
                    check(same_route, 'Measured AUTO route/tier histogram changed within this fixture')
                    print(f'  completed {len(runs)}/36; all 13 queries valid, target qualified', flush=True)
                except Exception as error:
                    errors.append({'round': round_index, 'fixture': fixture_name, 'position': position, 'arm': arm,
                        'directory': str(folder), 'exception': type(error).__name__, 'message': str(error),
                        'policy': 'Preserve raw records and stop; no replacement, exclusions, extra rounds or forced routes'})
                    print('  BLOCKED: ' + str(error), flush=True)
                (output / 'runs.json').write_text(json.dumps(runs, indent=2) + '\\n')
                (output / 'summary.json').write_text(json.dumps(summary(runs, False, errors), indent=2) + '\\n')
                if errors:
                    break
    post = {'identityFilesUnchanged': all(Path(p).is_file() and identity.sha(p) == h for p, h in watched.items()),
            'runtimeIdentitiesUnchanged': {name: identity.classpath_manifest(r['classpath']) == r['manifest']
                                            for name, r in snapshots.items()}}
    identity.write_new(output / 'post-run-preservation.json', post)
    if not post['identityFilesUnchanged'] or not all(post['runtimeIdentitiesUnchanged'].values()):
        errors.append({'exception': 'IdentityMismatch', 'message': 'Runtime/protocol/driver/source evidence changed'})
    complete = len(runs) == 36 and not errors
    (output / 'summary.json').write_text(json.dumps(summary(runs, complete, errors), indent=2) + '\\n')
    print(json.dumps({'output': str(output), 'complete': complete, 'completedJvms': len(runs), 'errors': errors}))
    if not complete:
        raise SystemExit(1)
''')
with (HERE / 'run-comparison.py').open('x') as out:
    out.write(s)
print('Prepared dual-fixture driver; no JVM launched.')
