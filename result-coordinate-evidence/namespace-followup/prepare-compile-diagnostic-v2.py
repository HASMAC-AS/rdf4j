#!/usr/bin/env python3
"""Create an additive directory-absence diagnostic, preserving the completed v1 study."""
import argparse
import difflib
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
SOURCE = Path('org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java')


def replace(text, before, after, count=1):
    if text.count(before) != count:
        raise ValueError(f'Expected {count} occurrences: {before}')
    return text.replace(before, after)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--slot-released', action='store_true', required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    v1_descriptor_path = HERE / 'compiled-v1/runtimes/diagnostic-original/runtime-descriptor.json'
    v1 = json.loads(v1_descriptor_path.read_text())
    v1_provenance = json.loads(Path(v1['sourceProvenanceFile']).read_text())
    if identity.sha(v1['sourceProvenanceFile']) != v1['sourceProvenanceSha256']:
        raise ValueError('V1 source provenance changed')
    v1_source = HERE / 'diagnostic-production-src' / SOURCE
    if identity.sha(v1_source) != v1_provenance['generated'][v1_source.relative_to(HERE).as_posix()]:
        raise ValueError('V1 diagnostic source changed')
    v1_cp = Path(v1['classpathFile']).read_text().strip()
    v1_manifest = identity.classpath_manifest(v1_cp)
    if v1_manifest != json.loads(Path(v1['classpathManifestFile']).read_text()):
        raise ValueError('V1 diagnostic runtime changed')
    preserved_files = [HERE / 'run-diagnostic.py', HERE / 'prepared-source-provenance.json', v1_source,
                       *sorted((HERE / 'diagnostic-original-v1').glob('*.json')),
                       HERE / 'diagnostic-original-v1/samples.jsonl']
    preserved = {str(path): identity.sha(path) for path in preserved_files}
    original, original_manifest = identity.original_identity()
    before = v1_source.read_text()
    after = replace(before,
        '"referenceCalls", "managedReferenceCalls", "directoryLookups", "directoryProbes", "directoryMisses" };',
        '"referenceCalls", "managedReferenceCalls", "directoryLookups", "directoryProbes", "directoryMisses",\n'
        '            "directoryAbsentRangeCalls", "directoryAbsentVisitedCells" };')
    after = replace(after, 'private static final int NS_DIAG_WIDTH = 14;',
                    'private static final int NS_DIAG_WIDTH = 16;')
    after = replace(after,
        '        namespaceDiagnosticCounters[metric]++;\n        if (length == WIDTH)',
        '        namespaceDiagnosticCounters[metric]++;\n'
        '        boolean directoryAbsentAtEntry = namespaceIds == null;\n'
        '        if (directoryAbsentAtEntry) namespaceDiagnosticCounters[metric + 14]++;\n'
        '        if (length == WIDTH)', count=2)
    after = replace(after,
        '                namespaceDiagnosticCounters[metric + 1]++;\n',
        '                namespaceDiagnosticCounters[metric + 1]++;\n'
        '                if (directoryAbsentAtEntry) namespaceDiagnosticCounters[metric + 15]++;\n', count=2)
    source = output / 'src' / SOURCE
    source.parent.mkdir(parents=True)
    source.write_text(after)
    patch = ''.join(difflib.unified_diff(before.splitlines(True), after.splitlines(True),
                                       fromfile='v1/' + SOURCE.as_posix(), tofile='v2/' + SOURCE.as_posix()))
    (output / 'diagnostic-v2.patch').write_text(patch)
    provenance = {'v1Source': str(v1_source), 'v1SourceSha256': identity.sha(v1_source),
                  'v1RuntimeDescriptor': str(v1_descriptor_path),
                  'v1RuntimeDescriptorSha256': identity.sha(v1_descriptor_path),
                  'source': str(source), 'sourceSha256': identity.sha(source),
                  'driver': str(Path(__file__)), 'driverSha256': identity.sha(Path(__file__)),
                  'purpose': 'Count exact directory-null range entries and actually visited cells; no performance inference.',
                  'preservedV1Files': preserved}
    identity.write_new(output / 'source-provenance.json', provenance)
    classes = output / 'classes'
    classes.mkdir()
    argv = [str(JAVA / 'javac'), '-proc:none', '-encoding', 'UTF-8', '-cp', original,
            '-d', str(classes), str(source)]
    identity.write_new(output / 'compiler-command.json', {'argv': argv, 'cwd': str(ROOT),
                                                         'startedEpochNs': time.time_ns()})
    with (output / 'compiler.log').open('xb') as log:
        process = subprocess.run(argv, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, timeout=60)
    identity.write_new(output / 'compiler-result.json', {'exitCode': process.returncode,
                                                       'finishedEpochNs': time.time_ns()})
    if process.returncode:
        raise RuntimeError('V2 compilation failed; original artifacts preserved')
    cp = v1_manifest[0]['path'] + os.pathsep + str(classes) + os.pathsep + original
    manifest = identity.classpath_manifest(cp)
    if manifest[0] != v1_manifest[0] or manifest[-127:] != original_manifest:
        raise ValueError('Shared diagnostic harness or original runtime changed')
    identity.write_new(output / 'classpath-manifest.json', manifest)
    (output / 'classpath.txt').write_text(cp + '\n')
    descriptor = dict(v1)
    descriptor.update({'name': 'diagnostic-original-directory-absence-v2',
        'sourceIdentity': identity.ORIGINAL_SOURCE + '+namespace-harness-sha256-' + manifest[0]['sha256']
                          + '+directory-absence-diagnostic-v2-sha256-' + manifest[1]['sha256'],
        'classpathFile': str(output / 'classpath.txt'),
        'classpathFileSha256': identity.sha(output / 'classpath.txt'),
        'classpathManifestFile': str(output / 'classpath-manifest.json'),
        'classpathManifestSha256': identity.sha(output / 'classpath-manifest.json'),
        'sourceProvenanceFile': str(output / 'source-provenance.json'),
        'sourceProvenanceSha256': identity.sha(output / 'source-provenance.json'),
        'diagnosticVersion': 2, 'additionalCounters': ['directoryAbsentRangeCalls', 'directoryAbsentVisitedCells']})
    identity.write_new(output / 'runtime-descriptor.json', descriptor)
    if any(identity.sha(path) != digest for path, digest in preserved.items()):
        raise ValueError('Preserved v1 file changed during v2 compilation')
    identity.write_new(output / 'build-summary.json', {'preservedV1FilesUnchanged': True,
        'sharedHarnessUnchanged': True, 'original127EntriesUnchanged': True,
        'runtimeDescriptorSha256': identity.sha(output / 'runtime-descriptor.json')})
    print(json.dumps({'output': str(output), 'runtimeDescriptor': str(output / 'runtime-descriptor.json')}))


if __name__ == '__main__':
    main()
