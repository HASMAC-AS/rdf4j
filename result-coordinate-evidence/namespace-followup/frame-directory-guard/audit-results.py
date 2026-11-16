#!/usr/bin/env python3
"""Independently recalculate completed frame-guard results from preserved raw JSONL."""
import hashlib
import json
from pathlib import Path
import statistics

HERE = Path(__file__).resolve().parent
SOURCE = HERE / 'comparison-v1'
OUTPUT = HERE / 'audit-v1'
ARMS = ['original-off', 'original-dense', 'candidate-dense']
PAIRS = [('candidate-dense', 'original-dense'), ('candidate-dense', 'original-off'),
         ('original-dense', 'original-off')]


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def range_median(values):
    return {'median': statistics.median(values), 'range': [min(values), max(values)], 'values': values}


def main():
    OUTPUT.mkdir(exist_ok=False)
    protocol = json.loads((SOURCE / 'used-protocol.json').read_text())
    assert (SOURCE / 'used-protocol.json').read_bytes() == (HERE / 'protocol.json').read_bytes()
    assert sha(HERE / 'run-comparison.py') == protocol['driverSha256']
    collected = json.loads((SOURCE / 'runs.json').read_text())
    reported = json.loads((SOURCE / 'summary.json').read_text())
    preservation = json.loads((SOURCE / 'post-run-preservation.json').read_text())
    assert reported['complete'] and not reported['errors'] and len(collected) == 18
    assert preservation['identityFilesUnchanged'] and all(preservation['runtimeIdentitiesUnchanged'].values())
    collected_by_key = {(r['round'], r['arm']): r for r in collected}
    raw, evidence, all_samples, measured_samples = {}, [], [], []
    metrics = protocol['statistics']['metrics']
    expected_route = [{'point': 'ROW', 'label': 'row/join dispatch', 'strategy': 'irKernel',
                       'mode': 'normal', 'count': 1}]
    for round_index, order in enumerate(protocol['balancedOrders']):
        for position, arm in enumerate(order):
            folder = SOURCE / f'round-{round_index:02d}-{position:02d}-{arm}'
            samples_path = folder / 'samples.jsonl'
            records = [json.loads(line) for line in samples_path.read_text().splitlines()]
            metadata, samples = records[0], records[1:]
            assert len(records) == 14 and [s['iteration'] for s in samples] == list(range(-8, 5))
            assert [s['stage'] for s in samples] == ['warmup'] * 8 + ['measurement'] * 5
            saved = collected_by_key[round_index, arm]
            assert metadata == saved['metadata'] and samples == saved['samples']
            assert saved['position'] == position and saved['routeTierHistogramMatchesFirstFork']
            validation = json.loads((folder / 'validation.json').read_text())
            audit = json.loads((folder / 'root-host-audit.json').read_text())
            process = json.loads((folder / 'process-result.json').read_text())
            assert validation['resultValidity'] == 'passed' and validation['targetQualified']
            assert audit == saved['rootAudit'] and audit['hostedGenericTotal'] == 0
            assert audit['semanticNativeCompilesTotal'] == 13 and audit['harnessReturned']
            assert audit['harnessFailureClass'] is None and process['exitCode'] == 0
            mode = protocol['armDefinitions'][arm]['transportMode']
            assert metadata['systemProperties']['rdf4j.lmdb.resultCoordinateTransport'] == mode
            assert metadata['nativeEvaluation'] and metadata['directAdjacency']
            assert metadata['directAdjacencyMode'] == 'PREFER'
            assert metadata['globalLedgerLimitBytes'] == 1073741824 and metadata['perQueryLedgerLimitBytes'] == 268435456
            for sample in samples:
                assert sample['valid'] and sample['complete'] and sample['rows'] == 1152000
                assert sample['checksum'] == '3123987218479850048'
                assert sample['ledgerBeforeBytes'] == sample['ledgerAfterBytes'] == 0
                assert sample['counters']['semanticNativeCompiles'] == 1
                assert sample['counters']['coordinateAdmissions']['opened'] == (0 if mode == 'off' else 1)
                assert sample['adjacency']['lookupHits'] == 55
                assert sample['adjacency']['stateBefore'] == sample['adjacency']['stateAfter'] == 'ACTIVE'
                assert sample['adjacency']['appliedRevisionBefore'] == sample['adjacency']['currentRevisionBefore'] == 1
                assert sample['adjacency']['appliedRevisionAfter'] == sample['adjacency']['currentRevisionAfter'] == 1
            measured = samples[8:]
            assert len(measured) == 5
            for sample in measured:
                assert sample['counters']['compiledBinds'] == 1 and sample['counters']['interpretedBinds'] == 0
                assert sample['successfulSelections'] == expected_route
            medians = {metric: statistics.median(sample[metric] for sample in measured) for metric in metrics}
            assert medians == saved['medians']
            raw[round_index, arm] = {'round': round_index, 'arm': arm, 'position': position, 'medians': medians,
                'measured': measured, 'cold': samples[0], 'samples': samples}
            evidence.append({'round': round_index, 'arm': arm, 'position': position,
                'rawFile': str(samples_path), 'rawSha256': sha(samples_path),
                'rawMatchesCollectedRecords': True, 'allFiveMediansMatch': True,
                'rootAuditSha256': sha(folder / 'root-host-audit.json')})
            all_samples.extend(samples)
            measured_samples.extend(measured)
    comparisons = []
    for a, b in PAIRS:
        ratios = {metric: [raw[i, a]['medians'][metric] / raw[i, b]['medians'][metric]
                           for i in range(6)] for metric in metrics}
        reference = next(r for r in reported['comparisons'] if r['candidate'] == a and r['comparator'] == b)
        for metric, values in ratios.items():
            assert statistics.median(values) == reference['medianRatios'][metric]
            assert [min(values), max(values)] == reference['ratioRanges'][metric]
            assert values == [p['ratios'][metric] for p in reference['pairs']]
        comparisons.append({'candidate': a, 'comparator': b,
            'metrics': {metric: range_median(values) for metric, values in ratios.items()},
            'latencyPairsBelowOne': sum(v < 1 for v in ratios['totalNs']),
            'latencyPairsAboveOne': sum(v > 1 for v in ratios['totalNs']),
            'conclusion': ('Consistently slower in all six pairs' if min(ratios['totalNs']) > 1 else
                           'Consistently faster in all six pairs' if max(ratios['totalNs']) < 1 else
                           'Inconclusive: paired range crosses one')})
    arms = {}
    for arm in ARMS:
        forks = [raw[i, arm] for i in range(6)]
        arms[arm] = {'forkMedians': {m: range_median([r['medians'][m] for r in forks]) for m in metrics},
            'firstWarmupDescriptive': {m: range_median([r['cold'][m] for r in forks]) for m in metrics},
            'firstWarmupRoutes': [r['cold']['successfulSelections'] for r in forks],
            'lazyValueCallsAcrossAllQueries': sorted({s['counters']['lazyValueCalls'] for r in forks for s in r['samples']}),
            'ledgerPeaksAcrossAllQueries': sorted({s['ledgerPeakBytes'] for r in forks for s in r['samples']}),
            'measuredAllocationByRound': [[s['allThreadAllocatedBytes'] for s in r['measured']] for r in forks],
            'measuredLatencyByRound': [[s['totalNs'] for s in r['measured']] for r in forks]}
    result = {'status': 'passed', 'protocolSha256': sha(HERE / 'protocol.json'),
        'driverSummarySha256': sha(SOURCE / 'summary.json'), 'rawMatchesCollected': True,
        'allRatiosAndMediansRecalculatedExactly': True, 'forks': len(raw), 'queries': len(all_samples),
        'measuredQueries': len(measured_samples), 'warmupQueries': len(all_samples) - len(measured_samples),
        'allMeasuredRouteSignaturesIdentical': True, 'adjacencyLookupHitsPerQuery': [55, 55],
        'allQueryLedgerEndpointsZero': True, 'semanticNativeCompilesTotal': sum(s['counters']['semanticNativeCompiles'] for s in all_samples),
        'runtimePreservation': preservation, 'arms': arms, 'comparisons': comparisons, 'rawEvidence': evidence,
        'note': 'Independent recalculation from raw JSONL, not copied estimates. All measurements retained, including round 4 candidate allocation variation. First-warmup descriptions are secondary; no new execution or tail selection.'}
    (OUTPUT / 'raw-recalculation.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'audit': str(OUTPUT / 'raw-recalculation.json'), 'status': result['status'],
        'forks': result['forks'], 'queries': result['queries'], 'measuredQueries': result['measuredQueries'],
        'comparisons': [{k: c[k] for k in ('candidate', 'comparator', 'latencyPairsBelowOne', 'latencyPairsAboveOne', 'conclusion')} for c in comparisons]}, indent=2))


if __name__ == '__main__':
    main()
