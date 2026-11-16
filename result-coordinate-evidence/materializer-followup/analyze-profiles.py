#!/usr/bin/env python3
"""Map untouched harness records to JFR writes and aggregate conservative consumption windows."""
from collections import Counter, defaultdict
import json
from pathlib import Path
import statistics

HERE = Path(__file__).resolve().parent
SAMPLERS = ('jdk.ExecutionSample', 'jdk.CPUTimeSample', 'jdk.NativeMethodSample')
PREFIX = 'org.eclipse.rdf4j.sail.lmdb.'


def top(counter, count=40):
    return [{'name': key, 'value': value} for key, value in counter.most_common(count)]


def events(path):
    with path.open() as stream:
        for line in stream:
            yield json.loads(line)


def inside(time, windows, conservative=False, stage=None):
    for window in windows:
        if stage and window['stage'] != stage:
            continue
        low = window['guaranteedConsumptionStartEpochNs'] if conservative else window['bracketStartEpochNs']
        high = window['guaranteedConsumptionEndEpochNs'] if conservative else window['bracketEndEpochNs']
        if low <= time <= high:
            return window
    return None


def aggregate(folder):
    raw_lines = (folder / 'samples.jsonl').read_bytes().splitlines(keepends=True)
    records = [json.loads(line) for line in raw_lines]
    samples = records[1:]
    writes = sorted((e for e in events(folder / 'events.jsonl') if e['type'] == 'jdk.FileWrite'
                     and e.get('path') == str(folder / 'samples.jsonl')), key=lambda e: e['startEpochNs'])
    assert len(writes) == len(records) == 11
    assert [w['bytesWritten'] for w in writes] == list(map(len, raw_lines))
    arrivals = list(events(folder / 'arrival-trace.jsonl'))
    assert len(arrivals) == len(records)
    windows = []
    for index, sample in enumerate(samples, 1):
        start = writes[index-1]['endEpochNs']
        end = writes[index]['startEpochNs']
        total = sample['totalNs']
        prefix = sample['connectionNs'] + sample['prepareNs'] + sample['evaluateNs']
        slack = end - start - total
        assert slack >= 0, (sample['iteration'], slack)
        consumption_start = end - total + prefix
        consumption_end = start + prefix + sample['consumeNs']
        assert consumption_start < consumption_end
        windows.append({
            'stage': sample['stage'], 'iteration': sample['iteration'],
            'bracketStartEpochNs': start, 'bracketEndEpochNs': end,
            'queryStartLowerBoundEpochNs': start, 'queryStartUpperBoundEpochNs': end-total,
            'guaranteedConsumptionStartEpochNs': consumption_start,
            'guaranteedConsumptionEndEpochNs': consumption_end,
            'guaranteedConsumptionNs': consumption_end-consumption_start,
            'reportedConsumptionNs': sample['consumeNs'], 'queryTotalNs': total,
            'unassignedBracketSlackNs': slack,
            'externalArrivalAfterWriteNs': arrivals[index]['observedEpochNs']-writes[index]['endEpochNs']})
    counters = defaultdict(Counter)
    inclusive = defaultdict(Counter)
    exclusive = defaultdict(Counter)
    line_exclusive = defaultdict(Counter)
    frame_kinds = defaultdict(Counter)
    app_inclusive = defaultdict(Counter)
    allocation_classes = Counter()
    allocation_threads = Counter()
    allocation_top = Counter()
    allocation_inclusive = Counter()
    groups = defaultdict(Counter)
    queries = defaultdict(lambda: defaultdict(Counter))
    compilations = []
    materializer_compilations = []
    gc_events = []
    monitor_events = []
    lost = Counter()
    for event in events(folder / 'events.jsonl'):
        stamp, kind = event['startEpochNs'], event['type']
        bracket = inside(stamp, windows)
        consumption = inside(stamp, windows, conservative=True)
        frames = event.get('frames', [])
        methods = [f['method'] for f in frames]
        thread = event.get('thread', '(unknown)')
        thread_key = f"{thread} (java={event.get('javaThreadId')}, os={event.get('osThreadId')})"
        if kind == 'jdk.CPUTimeSamplesLost':
            lost['allRecordingEvents'] += 1
            lost['allRecordingSamples'] += event.get('lostSamples', 0)
            if bracket and bracket['stage'] == 'measurement':
                lost['measuredBracketEvents'] += 1
                lost['measuredBracketSamples'] += event.get('lostSamples', 0)
                lost['measuredBracketThread:' + thread_key] += event.get('lostSamples', 0)
            if consumption and consumption['stage'] == 'measurement':
                lost['guaranteedConsumptionSamples'] += event.get('lostSamples', 0)
                lost['guaranteedConsumptionThread:' + thread_key] += event.get('lostSamples', 0)
        if kind in SAMPLERS and bracket:
            counters[kind + '/' + bracket['stage'] + '/threads'][thread_key] += 1
            counters[kind + '/' + bracket['stage'] + '/properties']['events'] += 1
            if event.get('failed'):
                counters[kind + '/' + bracket['stage'] + '/properties']['failed'] += 1
            if event.get('biased'):
                counters[kind + '/' + bracket['stage'] + '/properties']['biased'] += 1
            if event.get('truncated'):
                counters[kind + '/' + bracket['stage'] + '/properties']['truncated'] += 1
        if kind in SAMPLERS and consumption and consumption['stage'] == 'measurement':
            counters[kind + '/consumption/threads'][thread_key] += 1
            counters[kind + '/consumption/properties']['events'] += 1
            for property in ('failed', 'biased', 'truncated'):
                if event.get(property):
                    counters[kind + '/consumption/properties'][property] += 1
            if thread != 'main':
                continue
            q = queries[consumption['iteration']][kind]
            q['samples'] += 1
            counters[kind + '/consumption/main']['events'] += 1
            if kind == 'jdk.CPUTimeSample':
                counters[kind + '/consumption/main']['nominalSampledCpuNs'] += event.get('samplingPeriodNs', 0)
            for property in ('failed', 'biased', 'truncated'):
                if event.get(property):
                    counters[kind + '/consumption/main'][property] += 1
            if not methods:
                counters[kind + '/consumption/main']['emptyStack'] += 1
                continue
            exclusive[kind][methods[0]] += 1
            frame_kinds[kind][frames[0]['kind']] += 1
            line_exclusive[kind][f"{methods[0]}:{frames[0]['line']}"] += 1
            unique = set(methods)
            for method in unique:
                inclusive[kind][method] += 1
                if method.startswith(PREFIX):
                    app_inclusive[kind][method] += 1
            groups_to_count = {
                'materializerAny': any('.NativeResultMaterializer' in m for m in methods),
                'rememberFactor': any(m.endswith('.NativeResultMaterializer.rememberFactor') for m in methods),
                'findFactor': any(m.endswith('.NativeResultMaterializer.findFactor') for m in methods),
                'captureCoordinate': any(m.endswith('.NativeResultMaterializer.captureCoordinate') for m in methods),
                'factorMemoLoad': any(m.endswith('.NativeResultMaterializer$FactorMemo.load') for m in methods),
                'factorMemoRemember': any(m.endswith('.NativeResultMaterializer$FactorMemo.remember') for m in methods),
                'kernelResultCoordinatesAny': any('.KernelResultCoordinates.' in m for m in methods),
                'kernelResultCoordinatesWithoutMaterializer': any('.KernelResultCoordinates.' in m for m in methods)
                    and not any('.NativeResultMaterializer' in m for m in methods),
                'lexicalConsumer': any(m.endswith('.OptionalFanoutExperiment.lexicalHash') for m in methods),
            }
            for group, hit in groups_to_count.items():
                if hit:
                    groups[kind][group] += 1
                    q[group] += 1
                    if kind == 'jdk.CPUTimeSample':
                        groups[kind + ('/biased' if event.get('biased') else '/unbiased')][group] += 1
        elif kind == 'jdk.ObjectAllocationSample' and consumption and consumption['stage'] == 'measurement':
            weight = event.get('weight', 0)
            allocation_classes[event.get('objectClass', '(unknown)')] += weight
            allocation_threads[thread_key] += weight
            if methods:
                allocation_top[methods[0]] += weight
                for method in set(methods):
                    allocation_inclusive[method] += weight
        elif kind == 'jdk.Compilation':
            method = event.get('method', '')
            if '.NativeResultMaterializer' in method or '.KernelResultCoordinates' in method:
                materializer_compilations.append({k:v for k,v in event.items() if k != 'frames'})
            if bracket and bracket['stage'] == 'measurement':
                compilations.append({k:v for k,v in event.items() if k != 'frames'})
        elif kind in ('jdk.GarbageCollection', 'jdk.GCPhasePause') and bracket and bracket['stage'] == 'measurement':
            gc_events.append({k:v for k,v in event.items() if k != 'frames'})
        elif kind == 'jdk.JavaMonitorEnter' and bracket and bracket['stage'] == 'measurement':
            monitor_events.append(event)
    metrics = ('totalNs','prepareNs','evaluateNs','consumeNs','allThreadAllocatedBytes','callerThreadAllocatedBytes',
               'ledgerPeakBytes')
    measured = [s for s in samples if s['stage'] == 'measurement']
    result = {
        'mode': folder.name.removeprefix('frozen-'),
        'windowMethod': 'Every complete JSONL record matches one JFR FileWrite by order and byte length. Query start lies between preceding write end and current write start minus totalNs. The intersection of all possible consumption intervals is analyzed, excluding both uncertain edges.',
        'windowFormula': '[currentWriteStart-totalNs+connectionNs+prepareNs+evaluateNs, previousWriteEnd+connectionNs+prepareNs+evaluateNs+consumeNs]',
        'windows': windows,
        'instrumentedMeasuredMediansNotPerformanceEstimates': {k: statistics.median(s[k] for s in measured) for k in metrics},
        'rawMeasuredCounters': [s['counters'] for s in measured],
        'rawMeasuredSelections': [s.get('successfulSelections') for s in measured],
        'counters': {k:dict(v) for k,v in counters.items()},
        'samplerTopExclusive': {k:top(v,60) for k,v in exclusive.items()},
        'samplerTopExclusiveLines': {k:top(v,70) for k,v in line_exclusive.items()},
        'samplerTopInclusive': {k:top(v,60) for k,v in inclusive.items()},
        'samplerTopApplicationInclusive': {k:top(v,60) for k,v in app_inclusive.items()},
        'samplerMaterializerCoordinateInclusive': {k:top(Counter({m:n for m,n in v.items()
                    if '.NativeResultMaterializer' in m or '.KernelResultCoordinates' in m}),200)
                    for k,v in inclusive.items()},
        'samplerLeafFrameKinds': {k:dict(v) for k,v in frame_kinds.items()},
        'overlappingStackGroups': {k:dict(v) for k,v in groups.items()},
        'perMeasuredQueryStackGroups': {str(i):{k:dict(v) for k,v in q.items()} for i,q in queries.items()},
        'allocationSampleWeightBytes': sum(allocation_classes.values()),
        'allocationTopClasses': top(allocation_classes,30),
        'allocationThreads': dict(allocation_threads),
        'allocationTopExclusive': top(allocation_top,40),
        'allocationTopInclusive': top(allocation_inclusive,40),
        'cpuTimeSamplesLost': dict(lost),
        'measuredBracketCompilations': compilations,
        'allMaterializerCoordinateCompilations': materializer_compilations,
        'measuredBracketGcEvents': gc_events,
        'measuredBracketMonitorEnterEvents': monitor_events,
        'scopeCaveats': ['Inclusive groups overlap and cannot be summed.',
                        'Samples do not prove causal time savings; CPU-time sampler loss and safepoint bias are retained.',
                        'Allocation event weights are statistical; exact allocated bytes remain harness ThreadMXBean deltas.',
                        'Monitor events only record waits above the configured 10 ms threshold; uncontended monitor cost is not isolated.',
                        'One diagnostic JVM per arm; instrumented latency is not a balanced performance experiment.']}
    (folder / 'profile-analysis.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


def main():
    for mode in ('off','dense'):
        result = aggregate(HERE / ('frozen-'+mode))
        print(mode, json.dumps({
            'measuredSlackNs': [w['unassignedBracketSlackNs'] for w in result['windows'] if w['stage']=='measurement'],
            'counts': {k:v for k,v in result['counters'].items() if '/consumption/' in k},
            'groups': result['overlappingStackGroups'],
            'lost': result['cpuTimeSamplesLost'],
            'cpuTop': result['samplerTopExclusive'].get('jdk.CPUTimeSample',[])[:15],
            'executionTop': result['samplerTopExclusive'].get('jdk.ExecutionSample',[])[:15],
            'allocationClasses':result['allocationTopClasses'][:8],
            'measuredCompilations':len(result['measuredBracketCompilations']),
            'monitors':len(result['measuredBracketMonitorEnterEvents'])}, indent=2))


if __name__ == '__main__':
    main()
