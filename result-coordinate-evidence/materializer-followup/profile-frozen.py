#!/usr/bin/env python3
"""Diagnostic JFR pair; does not edit or import the earlier benchmark drivers."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
JDK = Path('/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8')
SOURCE = '15fe7d5011+worktree-sha256-34767935d60f32ba61adfb44495c607e17ca760d5d2f77a17ddb2862c4f7a8ac'
MAIN = 'org.eclipse.rdf4j.sail.lmdb.evaluation.RootQualifiedOptionalFanoutLauncher'


def write(path, value):
    with path.open('x') as out:
        json.dump(value, out, indent=2)
        out.write('\n')


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1 << 20), b''):
            digest.update(block)
    return digest.hexdigest()


def manifest(classpath):
    entries = []
    for item in classpath.split(os.pathsep):
        path = Path(item)
        if path.is_dir():
            digest = hashlib.sha256()
            files = sorted(p for p in path.rglob('*') if p.is_file())
            for file in files:
                digest.update(file.relative_to(path).as_posix().encode() + b'\0')
                digest.update(bytes.fromhex(sha(file)))
            entries.append(dict(path=str(path), kind='tree', files=len(files), sha256=digest.hexdigest()))
        else:
            entries.append(dict(path=str(path), kind='file', sha256=sha(path)))
    return entries


def configure():
    settings = {
        'jdk.ExecutionSample': {'enabled': 'true', 'period': '5 ms'},
        'jdk.NativeMethodSample': {'enabled': 'true', 'period': '10 ms'},
        'jdk.CPUTimeSample': {'enabled': 'true', 'throttle': '10 ms', 'stackTrace': 'true'},
        'jdk.CPUTimeSamplesLost': {'enabled': 'true'},
        'jdk.ObjectAllocationSample': {'enabled': 'true', 'throttle': '300/s', 'stackTrace': 'true'},
        'jdk.Compilation': {'enabled': 'true', 'threshold': '0 ms'},
        'jdk.FileWrite': {'enabled': 'true', 'threshold': '0 ms', 'throttle': 'off', 'stackTrace': 'true'},
    }
    base = JDK / 'lib/jfr/profile.jfc'
    tree = ET.parse(base)
    for event in tree.getroot().findall('event'):
        changes = settings.get(event.get('name'), {})
        for name, value in changes.items():
            element = next((s for s in event.findall('setting') if s.get('name') == name), None)
            if element is None:
                element = ET.SubElement(event, 'setting', {'name': name})
            element.attrib.pop('control', None)
            element.text = value
    target = HERE / 'materializer-profile.jfc'
    with target.open('xb') as out:
        tree.write(out, encoding='UTF-8', xml_declaration=True)
    return target, settings, sha(base)


def run(mode, classpath, config):
    folder = HERE / ('frozen-' + mode)
    folder.mkdir()
    sample_path = folder / 'samples.jsonl'
    recording = folder / 'profile.jfr'
    command = [str(JDK / 'bin/java'), '-Xms1024m', '-Xmx1024m', '-XX:+UseG1GC',
               '-XX:FlightRecorderOptions=stackdepth=256',
               f'-XX:StartFlightRecording=name=materializer,settings={config},filename={recording},dumponexit=true,disk=true',
               f'-Drdf4j.lmdb.resultCoordinateTransport={mode}', '-cp', classpath, MAIN,
               f'--arm={mode}', f'--source={SOURCE}', '--warmup=5', '--iterations=5',
               '--measurements=100', '--readings=10', '--width=12', '--statuses=2',
               '--unique-payloads=0', '--consumption=full',
               f'--output={sample_path}', f'--store={folder / "store"}']
    write(folder / 'command.json', {'argv': command, 'cwd': str(ROOT)})
    started_wall = time.time_ns()
    started_mono = time.monotonic_ns()
    deadline = started_mono + 360 * 10**9
    offset = 0
    pending = b''
    record_end = 0
    records = []
    timeout = False
    with (folder / 'console.log').open('xb') as console, (folder / 'arrival-trace.jsonl').open('x') as trace:
        process = subprocess.Popen(command, cwd=ROOT, stdout=console, stderr=subprocess.STDOUT)
        write(folder / 'process.json', {'pid': process.pid, 'startedEpochNs': started_wall,
                                      'startedMonotonicNs': started_mono,
                                      'observationPollIntervalSeconds': .01})
        print(f'{mode}: started PID {process.pid}', flush=True)
        while True:
            if sample_path.exists():
                with sample_path.open('rb') as samples:
                    samples.seek(offset)
                    new = samples.read()
                    offset += len(new)
                pending += new
                while b'\n' in pending:
                    line, pending = pending.split(b'\n', 1)
                    record_end += len(line) + 1
                    record = json.loads(line)
                    records.append(record)
                    observed = {'observedEpochNs': time.time_ns(), 'observedMonotonicNs': time.monotonic_ns(),
                                'byteEndExclusive': record_end, 'type': record.get('type'),
                                'stage': record.get('stage'), 'iteration': record.get('iteration')}
                    trace.write(json.dumps(observed) + '\n')
                    trace.flush()
                    if record.get('type') == 'sample':
                        print(f'{mode}: {record["stage"]} {record["iteration"]} valid={record["valid"]}', flush=True)
            status = process.poll()
            if status is not None:
                break
            if time.monotonic_ns() > deadline:
                timeout = True
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                break
            time.sleep(.01)
        exit_code = process.wait()
    samples = [r for r in records if r.get('type') == 'sample']
    audit_path = folder / 'root-host-audit.json'
    audit = json.loads(audit_path.read_text()) if audit_path.exists() else None
    errors = []
    if timeout or exit_code != 0:
        errors.append(f'process failed: exit={exit_code}, timeout={timeout}')
    if len(samples) != 10 or [s['iteration'] for s in samples] != list(range(-5, 5)):
        errors.append('expected all ten original query records')
    if any(not s.get('valid') or s.get('ledgerBeforeBytes') or s.get('ledgerAfterBytes') for s in samples):
        errors.append('query result or ledger validation failed')
    if any(s.get('counters', {}).get('semanticNativeCompiles') != 1 for s in samples):
        errors.append('expected one native semantic compile per query')
    if not audit or not audit.get('harnessReturned') or audit.get('hostedGenericTotal') != 0 or audit.get('semanticNativeCompilesTotal') != 10:
        errors.append('fresh-process root audit failed')
    if not recording.exists() or recording.stat().st_size == 0:
        errors.append('JFR recording missing')
    result = {'mode': mode, 'status': 'valid diagnostic' if not errors else 'blocked', 'errors': errors,
              'exitCode': exit_code, 'finishedEpochNs': time.time_ns(), 'profile': str(recording),
              'rootAudit': audit, 'sampleCount': len(samples), 'elapsedSeconds': (time.monotonic_ns()-started_mono)/1e9,
              'instrumentedTimingIsNotPerformanceEstimate': True}
    write(folder / 'validation.json', result)
    print(f'{mode}: {result["status"]}; {errors}', flush=True)
    if errors:
        raise RuntimeError('; '.join(errors))
    return result


def main():
    classpath_path = ROOT / 'result-coordinate-evidence/root-qualified-launcher/classpath.txt'
    classpath = classpath_path.read_text().strip()
    reference = json.loads((ROOT / 'result-coordinate-evidence/transport-comparison/classpath-manifest.json').read_text())
    before = manifest(classpath)
    if len(before) != 127 or before[:126] != reference or before[-1]['sha256'] != '044cf5e6cca19abbaf98ff8789ee92480fd25c04f05483be257d3e18807e867a':
        raise RuntimeError('Frozen root-qualified classpath identity mismatch')
    write(HERE / 'classpath-manifest-before.json', before)
    (HERE / 'classpath.txt').write_text(classpath + '\n')
    config, settings, base_sha = configure()
    write(HERE / 'protocol.json', {
        'name': 'Frozen materializer diagnostic JFR pair', 'routine': 'C: no production changes',
        'createdEpochNs': time.time_ns(), 'source': SOURCE, 'modeOrder': ['off', 'dense'],
        'warmup': 5, 'iterations': 5, 'fixture': 'unchanged default full-consumption fixture',
        'strategy': 'normal AUTO', 'heap': '1024m', 'gc': 'G1', 'recordingWindow': 'entire JVM from startup',
        'analysisWindow': 'measured queries only, bounded by sample JSONL FileWrite events where available; external file-arrival trace retained as a second, conservative observation',
        'eventOverrides': settings, 'stackDepth': 256, 'baseJfcSha256': base_sha,
        'profileJfcSha256': sha(config), 'runnerSha256': sha(Path(__file__)),
        'performanceClaim': 'None. Sampling and extra JFR events perturb execution; this diagnostic is not an additional timing protocol.',
        'failurePolicy': 'Preserve and stop on invalid result, ledger leak, nonzero root host, incomplete records or changed frozen classpath.',
        'phaseCaveat': 'JSONL emission follows per-query validation and counter snapshots; write boundaries bracket execution but do not mark exact preparation/consumption transitions.',
        'rootAudit': 'Existing immutable launcher; absolute HOSTED_GENERIC read only after original main. Zero substantive; UNHOSTED_HARD_DECLINES is inert.',
        'noOtherJvmLoads': 'Parent reserved quiet serial period; no build/test/other JVM while each profile runs.'})
    runs = [run(mode, classpath, config) for mode in ('off', 'dense')]
    after = manifest(classpath)
    write(HERE / 'classpath-manifest-after.json', after)
    write(HERE / 'run-result.json', {'runs': runs, 'frozenClasspathUnchanged': before == after})
    if before != after:
        raise RuntimeError('Frozen classpath changed')


if __name__ == '__main__':
    main()
