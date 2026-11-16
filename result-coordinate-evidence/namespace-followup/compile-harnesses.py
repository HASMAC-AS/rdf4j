#!/usr/bin/env python3
"""Compile additive common/diagnostic harnesses only after root releases a JVM slot."""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import time

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
JAVA = Path('/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8/bin')
spec = importlib.util.spec_from_file_location('preserved_runtime_identity',
        ROOT / 'result-coordinate-evidence/materializer-followup/runtime_identity.py')
identity = importlib.util.module_from_spec(spec)
spec.loader.exec_module(identity)


def compile_sources(name, sources, classpath, output):
    directory = output / name
    directory.mkdir()
    classes = directory / 'classes'
    classes.mkdir()
    argv = [str(JAVA / 'javac'), '-proc:none', '-encoding', 'UTF-8', '-cp', classpath,
            '-d', str(classes), *map(str, sources)]
    identity.write_new(directory / 'command.json', {'argv': argv, 'cwd': str(ROOT),
                                                'startedEpochNs': time.time_ns()})
    with (directory / 'compiler.log').open('xb') as log:
        result = subprocess.run(argv, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, timeout=60)
    identity.write_new(directory / 'result.json', {'exitCode': result.returncode,
                                                  'finishedEpochNs': time.time_ns()})
    if result.returncode:
        raise RuntimeError('Compilation failed: ' + str(directory / 'compiler.log'))
    return classes


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--slot-released', action='store_true', required=True)
    args = parser.parse_args()
    before = identity.sha(HERE / 'prepared-source-provenance.json')
    provenance = json.loads((HERE / 'prepared-source-provenance.json').read_text())
    for name, expected in provenance['generated'].items():
        if identity.sha(HERE / name) != expected:
            raise ValueError('Prepared source changed before compilation: ' + name)
    original_cp, original_manifest = identity.original_identity()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    common = compile_sources('common-harness', sorted((HERE / 'harness-src').rglob('*.java')), original_cp, output)
    diagnostic = compile_sources('diagnostic-production',
                                 sorted((HERE / 'diagnostic-production-src').rglob('*.java')), original_cp, output)
    diagnostic_harness = compile_sources('diagnostic-harness',
        sorted((HERE / 'diagnostic-harness-src').rglob('*.java')),
        str(diagnostic) + os.pathsep + original_cp, output)
    for name, cp in [('original', str(common) + os.pathsep + original_cp),
                     ('diagnostic-original', str(diagnostic_harness) + os.pathsep + str(diagnostic)
                      + os.pathsep + original_cp)]:
        directory = output / 'runtimes' / name
        directory.mkdir(parents=True)
        manifest = identity.classpath_manifest(cp)
        if manifest[-127:] != original_manifest:
            raise ValueError('Shared original runtime bytes changed while compiling')
        identity.write_new(directory / 'classpath-manifest.json', manifest)
        with (directory / 'classpath.txt').open('x') as stream:
            stream.write(cp + '\n')
        source = identity.ORIGINAL_SOURCE + '+namespace-harness-sha256-' + manifest[0]['sha256']
        if name.startswith('diagnostic'):
            source += '+diagnostic-shadow-sha256-' + manifest[1]['sha256']
        descriptor = {'name': name, 'sourceIdentity': source,
                      'productionSourceIdentity': identity.ORIGINAL_SOURCE,
                      'classpathFile': str(directory / 'classpath.txt'),
                      'classpathFileSha256': identity.sha(directory / 'classpath.txt'),
                      'classpathManifestFile': str(directory / 'classpath-manifest.json'),
                      'classpathManifestSha256': identity.sha(directory / 'classpath-manifest.json'),
                      'sourceProvenanceFile': str(HERE / 'prepared-source-provenance.json'),
                      'sourceProvenanceSha256': before,
                      'originalEntriesUnchanged': 127, 'prefixEntryCount': len(manifest) - 127,
                      'diagnosticOnly': name.startswith('diagnostic'),
                      'nativeEvaluation': True, 'directAdjacencyEnabled': True,
                      'harnessEntry': manifest[0], 'productionEntryIndex': 2 if name == 'original' else 3}
        identity.write_new(directory / 'runtime-descriptor.json', descriptor)
    if identity.sha(HERE / 'prepared-source-provenance.json') != before:
        raise ValueError('Prepared source provenance changed during compilation')
    identity.write_new(output / 'build-summary.json', {
        'completedEpochNs': time.time_ns(), 'originalManifest': original_manifest,
        'sourceProvenanceSha256': before, 'helperSha256': identity.sha(Path(__file__)),
        'note': 'Common adjacency-enabled harness and separate diagnostic shadow compiled; no benchmark executed.'})
    print(json.dumps({'output': str(output), 'originalRuntime': str(output / 'runtimes/original/runtime-descriptor.json'),
                      'diagnosticRuntime': str(output / 'runtimes/diagnostic-original/runtime-descriptor.json')}))


if __name__ == '__main__':
    main()
