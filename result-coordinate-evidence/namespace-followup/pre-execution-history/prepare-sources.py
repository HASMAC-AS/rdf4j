#!/usr/bin/env python3
"""Prepare additive benchmark/diagnostic source copies; never compile or run Java."""
import difflib
import hashlib
import json
from pathlib import Path
import tarfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
MANIFEST = ROOT / 'result-coordinate-evidence/final-validation/benchmark-source-hashes.json'
ARCHIVE = Path('/workspace/scratch/73b19a6d6f5c/deliverables/rdf4j-source.tar.gz')
COORD = 'core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java'
HARNESS = 'core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/OptionalFanoutExperiment.java'
PACKAGE = Path('org/eclipse/rdf4j/sail/lmdb')


def digest(value):
    return hashlib.sha256(value).hexdigest()


def baseline(path, hashes):
    data = (ROOT / path).read_bytes()
    origin = str(ROOT / path)
    if digest(data) != hashes[path]:
        with tarfile.open(ARCHIVE, 'r:gz') as tar:
            data = tar.extractfile('rdf4j/' + path).read()
        origin = str(ARCHIVE) + '!rdf4j/' + path
    if digest(data) != hashes[path]:
        raise ValueError('Baseline source hash mismatch: ' + path)
    return data.decode(), {'path': path, 'sha256': digest(data), 'readFrom': origin}


def replace(text, old, new, count=1):
    if text.count(old) != count:
        raise ValueError(f'Expected {count} exact source sites, got {text.count(old)}: {old[:100]}')
    return text.replace(old, new)


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x') as out:
        out.write(text)


def patch(before, after, path):
    return ''.join(difflib.unified_diff(before.splitlines(keepends=True), after.splitlines(keepends=True),
                                        fromfile='a/' + path, tofile='b/' + path))


def common_harness(text):
    text = replace(text, 'import java.util.List;\n', 'import java.util.List;\nimport java.util.concurrent.TimeUnit;\n')
    text = replace(text, 'import org.eclipse.rdf4j.sail.lmdb.LmdbStore;\n',
                   'import org.eclipse.rdf4j.sail.lmdb.LmdbStore;\n'
                   'import org.eclipse.rdf4j.sail.lmdb.LmdbTestUtil;\n'
                   'import org.eclipse.rdf4j.sail.lmdb.NamespaceAdjacencyObservation;\n')
    text = replace(text, '\t\tconfig.setNativeEvaluationEnabled(true);\n',
                   '\t\tconfig.setNativeEvaluationEnabled(true);\n\t\tconfig.setDirectAdjacencyEnabled(true);\n')
    text = replace(text, '\t\tSailRepository repository = new SailRepository(new LmdbStore(store.toFile(), config));',
                   '\t\tLmdbStore nativeStore = new LmdbStore(store.toFile(), config);\n'
                   '\t\tSailRepository repository = new SailRepository(nativeStore);')
    text = replace(text, '\t\t\t\trepository.init();\n',
                   '\t\t\t\trepository.init();\n'
                   '\t\t\t\tLmdbTestUtil.awaitBackgroundMaintenance(nativeStore, 30L, TimeUnit.SECONDS);\n')
    text = replace(text, '\t\t\t\temit(output, metadata(options, fixture, dataset, config, store));',
                   '\t\t\t\tif (!nativeStore.awaitDirectAdjacencyReady(30L, TimeUnit.SECONDS)) {\n'
                   '\t\t\t\t\tthrow new IllegalStateException("Direct adjacency not ready: "\n'
                   '\t\t\t\t\t\t\t+ nativeStore.getDirectAdjacencyReadinessDescription());\n'
                   '\t\t\t\t}\n'
                   '\t\t\t\tMap<String, Object> metadata = metadata(options, fixture, dataset, config, store);\n'
                   '\t\t\t\tmetadata.put("namespaceExperimentHarness", "native-and-direct-adjacency-v1");\n'
                   '\t\t\t\tmetadata.put("adjacencySetup", NamespaceAdjacencyObservation.snapshot(nativeStore).setup());\n'
                   '\t\t\t\temit(output, metadata);')
    text = replace(text, 'sample = run(repository, fixture, options, allocation, i);',
                   'sample = run(repository, nativeStore, fixture, options, allocation, i);')
    text = replace(text, 'run(SailRepository repository, OptionalFanoutExperimentFixture fixture,',
                   'run(SailRepository repository, LmdbStore nativeStore, OptionalFanoutExperimentFixture fixture,')
    text = replace(text, '\t\tCounters countersBefore = OptionalFanoutRouteAccess.counters();',
                   '\t\tCounters countersBefore = OptionalFanoutRouteAccess.counters();\n'
                   '\t\tvar adjacencyBefore = NamespaceAdjacencyObservation.snapshot(nativeStore);')
    text = replace(text, '\t\tCounters counters = OptionalFanoutRouteAccess.counters().minus(countersBefore);',
                   '\t\tCounters counters = OptionalFanoutRouteAccess.counters().minus(countersBefore);\n'
                   '\t\tvar adjacencyAfter = NamespaceAdjacencyObservation.snapshot(nativeStore);')
    text = replace(text, '\t\trecord.put("counters", counterFields(counters));',
                   '\t\trecord.put("counters", counterFields(counters));\n'
                   '\t\trecord.put("adjacency", adjacencyAfter.minus(adjacencyBefore));')
    return text


ADJACENCY_HELPER = '''/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
// Some portions generated by Codex
package org.eclipse.rdf4j.sail.lmdb;

import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only package-local access to existing adjacency counters, outside query measurement windows. */
public final class NamespaceAdjacencyObservation {
    private NamespaceAdjacencyObservation() { }

    public static Snapshot snapshot(LmdbStore store) {
        var adjacency = store.getBackingStore().directAdjacencyStore();
        if (adjacency == null) {
            throw new IllegalStateException("Direct adjacency is disabled in the new experiment");
        }
        var metrics = adjacency.snapshotMetrics();
        Map<String, Long> fallbacks = new LinkedHashMap<>();
        metrics.fallbacksByReason.forEach((reason, count) -> fallbacks.put(reason.name(), count));
        return new Snapshot(metrics.state, metrics.baseRevision, metrics.appliedRevision,
                metrics.currentDataRevision, metrics.lookupHits, metrics.plannerStatsHits,
                metrics.exactMisses, Map.copyOf(fallbacks));
    }

    public record Snapshot(String state, long baseRevision, long appliedRevision, long currentDataRevision,
            long lookupHits, long plannerStatsHits, long exactMisses, Map<String, Long> fallbacks) {
        public Map<String, Object> setup() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("state", state);
            result.put("baseRevision", baseRevision);
            result.put("appliedRevision", appliedRevision);
            result.put("currentDataRevision", currentDataRevision);
            result.put("currentRevisionReady", appliedRevision == currentDataRevision);
            return result;
        }

        public Map<String, Object> minus(Snapshot before) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("stateBefore", before.state);
            result.put("stateAfter", state);
            result.put("currentRevisionBefore", before.currentDataRevision);
            result.put("currentRevisionAfter", currentDataRevision);
            result.put("appliedRevisionBefore", before.appliedRevision);
            result.put("appliedRevisionAfter", appliedRevision);
            result.put("lookupHits", lookupHits - before.lookupHits);
            result.put("plannerStatsHits", plannerStatsHits - before.plannerStatsHits);
            result.put("exactMisses", exactMisses - before.exactMisses);
            Map<String, Long> delta = new LinkedHashMap<>();
            fallbacks.forEach((reason, count) -> delta.put(reason, count - before.fallbacks.getOrDefault(reason, 0L)));
            result.put("fallbacks", delta);
            return result;
        }
    }
}
'''


DIAGNOSTIC_FIELDS = '''
    // Diagnostic shadow binary only: no instrumented class enters the timing protocol.
    private static final String[] NS_DIAG_SCOPES = { "frameRetain", "frameRelease", "batchRetain", "batchRelease",
            "directRetain", "directRelease", "otherLookup", "retirementLink" };
    private static final String[] NS_DIAG_METRICS = { "rangeCalls", "visitedCells", "positionedCells", "managedCells",
            "managedTransitions", "managedRuns", "singletonManagedRanges", "singletonPhysicalRanges", "emptyManagedRanges",
            "referenceCalls", "managedReferenceCalls", "directoryLookups", "directoryProbes", "directoryMisses" };
    private static final int NS_DIAG_WIDTH = 14;
    private static final int NS_DIAG_SIZE = NS_DIAG_SCOPES.length * NS_DIAG_WIDTH;
    private static final java.util.concurrent.atomic.AtomicLongArray NS_DIAG_TOTALS =
            new java.util.concurrent.atomic.AtomicLongArray(NS_DIAG_SIZE + 1);
    private final long[] namespaceDiagnosticCounters = new long[NS_DIAG_SIZE];
    private int namespaceDiagnosticScope = 6;

    /** Monotonic counters published once per closed owner; this API exists only in the diagnostic shadow class. */
    public static java.util.Map<String, Long> namespaceDiagnostics() {
        java.util.Map<String, Long> result = new java.util.LinkedHashMap<>();
        for (int scope = 0; scope < NS_DIAG_SCOPES.length; scope++) {
            for (int metric = 0; metric < NS_DIAG_WIDTH; metric++) {
                result.put(NS_DIAG_SCOPES[scope] + "." + NS_DIAG_METRICS[metric],
                        NS_DIAG_TOTALS.get(scope * NS_DIAG_WIDTH + metric));
            }
        }
        result.put("closedOwners", NS_DIAG_TOTALS.get(NS_DIAG_SIZE));
        return result;
    }
'''


def diagnostic_source(text):
    text = replace(text, 'public final class KernelResultCoordinates implements AutoCloseable {\n',
                   'public final class KernelResultCoordinates implements AutoCloseable {\n' + DIAGNOSTIC_FIELDS)
    text = replace(text, 'releaseRange(frames, base, registerWidth);', 'releaseRange(frames, base, registerWidth, 1);')
    text = replace(text, 'retainRange(frames, base, registerWidth);', 'retainRange(frames, base, registerWidth, 0);')
    text = replace(text, 'releaseRange(frames, frame * registerWidth, registerWidth);',
                   'releaseRange(frames, frame * registerWidth, registerWidth, 1);')
    text = replace(text, 'retainRange(coordinates, 0, Math.multiplyExact(count, WIDTH));',
                   'retainRange(coordinates, 0, Math.multiplyExact(count, WIDTH), 2);')
    text = replace(text, 'releaseRange(coordinates, 0, Math.multiplyExact(count, WIDTH));',
                   'releaseRange(coordinates, 0, Math.multiplyExact(count, WIDTH), 3);')
    for action in ('retain', 'release'):
        old = f'''\tprivate void {action}Range(long[] array, int start, int length) {{
\t\tfor (int at = start; enabled() && at < start + length; at += WIDTH) {{
\t\t\tif (array[at + POSITION] >= 0L) {{
\t\t\t\t{action}(array[at + NAMESPACE]);
\t\t\t}}
\t\t}}
\t}}
'''
        new = f'''\tprivate void {action}Range(long[] array, int start, int length, int scope) {{
        int previousScope = namespaceDiagnosticScope;
        namespaceDiagnosticScope = scope;
        int metric = scope * NS_DIAG_WIDTH;
        namespaceDiagnosticCounters[metric]++;
        if (length == WIDTH) namespaceDiagnosticCounters[metric + 7]++;
        long managed = 0L;
        long previousManagedNamespace = 0L;
        try {{
            for (int at = start; enabled() && at < start + length; at += WIDTH) {{
                namespaceDiagnosticCounters[metric + 1]++;
                if (array[at + POSITION] >= 0L) {{
                    namespaceDiagnosticCounters[metric + 2]++;
                    long namespace = array[at + NAMESPACE];
                    if (namespace > 1L) {{
                        namespaceDiagnosticCounters[metric + 3]++;
                        managed++;
                        if (namespace != previousManagedNamespace) {{
                            namespaceDiagnosticCounters[metric + 5]++;
                            if (previousManagedNamespace != 0L) namespaceDiagnosticCounters[metric + 4]++;
                            previousManagedNamespace = namespace;
                        }}
                    }}
                    {action}(namespace);
                }}
            }}
        }} finally {{
            if (managed == 1L) namespaceDiagnosticCounters[metric + 6]++;
            if (managed == 0L) namespaceDiagnosticCounters[metric + 8]++;
            namespaceDiagnosticScope = previousScope;
        }}
\t}}
'''
        text = replace(text, old, new)
    for action, category in [('retain', 4), ('release', 5)]:
        begin = f'\tprivate void {action}(long namespace) {{\n'
        start = text.index(begin)
        end = text.index('\n\t}\n', start) + len('\n\t}\n')
        old = text[start:end]
        body = old[len(begin):-len('\n\t}\n')]
        new = begin + f'''        int previousScope = namespaceDiagnosticScope;
        if (previousScope >= 4) namespaceDiagnosticScope = {category};
        int metric = namespaceDiagnosticScope * NS_DIAG_WIDTH;
        namespaceDiagnosticCounters[metric + 9]++;
        if (namespace > 1L) namespaceDiagnosticCounters[metric + 10]++;
        try {{
''' + body + '''
        } finally {
            namespaceDiagnosticScope = previousScope;
        }
\t}
'''
        text = replace(text, old, new)
    start = text.index('\tprivate static int findNamespace(long[] ids, long id) {')
    end = text.index('\n\tprivate static int namespaceInsertionSlot', start)
    old = text[start:end]
    new = '''\tprivate int findNamespace(long[] ids, long id) {
        int metric = namespaceDiagnosticScope * NS_DIAG_WIDTH;
        namespaceDiagnosticCounters[metric + 11]++;
        if (ids == null || id <= 1L) {
            namespaceDiagnosticCounters[metric + 13]++;
            return -1;
        }
        int mask = ids.length - 1;
        int slot = namespaceHash(id) & mask;
        while (true) {
            namespaceDiagnosticCounters[metric + 12]++;
            long candidate = ids[slot];
            if (candidate == 0L) {
                namespaceDiagnosticCounters[metric + 13]++;
                return -1;
            }
            if (candidate == id) return slot;
            slot = (slot + 1) & mask;
        }
\t}

    private int findRetirementNamespace(long[] ids, long id) {
        int previousScope = namespaceDiagnosticScope;
        namespaceDiagnosticScope = 7;
        try {
            return findNamespace(ids, id);
        } finally {
            namespaceDiagnosticScope = previousScope;
        }
    }
'''
    text = replace(text, old, new)
    text = replace(text, 'int tailSlot = findNamespace(namespaceIds, retiredTail);',
                   'int tailSlot = findRetirementNamespace(namespaceIds, retiredTail);')
    text = replace(text, '\t\t\tclosed = true;\n',
                   '\t\t\tclosed = true;\n'
                   '            for (int metric = 0; metric < NS_DIAG_SIZE; metric++) {\n'
                   '                NS_DIAG_TOTALS.addAndGet(metric, namespaceDiagnosticCounters[metric]);\n'
                   '            }\n'
                   '            NS_DIAG_TOTALS.incrementAndGet(NS_DIAG_SIZE);\n')
    return text


def main():
    hashes = json.loads(MANIFEST.read_text())
    original_harness, hp = baseline(HARNESS, hashes)
    original_coordinates, cp = baseline(COORD, hashes)
    common = common_harness(original_harness)
    diagnostic = diagnostic_source(original_coordinates)
    diagnostic_harness = replace(common, '\t\tvar adjacencyBefore = NamespaceAdjacencyObservation.snapshot(nativeStore);',
        '\t\tvar adjacencyBefore = NamespaceAdjacencyObservation.snapshot(nativeStore);\n'
        '\t\tvar namespacesBefore = org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelResultCoordinates.namespaceDiagnostics();')
    diagnostic_harness = replace(diagnostic_harness, '\t\trecord.put("omittedSelections", capture.omittedSelections());',
        '\t\trecord.put("omittedSelections", capture.omittedSelections());\n'
        '\t\tMap<String, Long> namespaces = new java.util.LinkedHashMap<>();\n'
        '\t\torg.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelResultCoordinates.namespaceDiagnostics()\n'
        '\t\t\t\t.forEach((name, count) -> namespaces.put(name, count - namespacesBefore.get(name)));\n'
        '\t\trecord.put("namespaceDiagnostics", namespaces);')
    write(HERE / 'baseline-src' / COORD, original_coordinates)
    write(HERE / 'baseline-src' / HARNESS, original_harness)
    write(HERE / 'harness-src' / PACKAGE / 'benchmark/OptionalFanoutExperiment.java', common)
    write(HERE / 'harness-src' / PACKAGE / 'NamespaceAdjacencyObservation.java', ADJACENCY_HELPER)
    write(HERE / 'diagnostic-harness-src' / PACKAGE / 'benchmark/OptionalFanoutExperiment.java', diagnostic_harness)
    write(HERE / 'diagnostic-harness-src' / PACKAGE / 'NamespaceAdjacencyObservation.java', ADJACENCY_HELPER)
    write(HERE / 'diagnostic-production-src' / PACKAGE / 'evaluation/codegen/KernelResultCoordinates.java', diagnostic)
    write(HERE / 'common-harness.patch', patch(original_harness, common, HARNESS))
    write(HERE / 'diagnostic-production.patch', patch(original_coordinates, diagnostic, COORD))
    write(HERE / 'diagnostic-harness.patch', patch(common, diagnostic_harness, HARNESS))
    generated = {p.relative_to(HERE).as_posix(): digest(p.read_bytes()) for directory in
                 ['baseline-src', 'harness-src', 'diagnostic-harness-src', 'diagnostic-production-src']
                 for p in sorted((HERE / directory).rglob('*.java'))}
    write(HERE / 'prepared-source-provenance.json', json.dumps({
        'baselines': [hp, cp], 'sourceManifestSha256': digest(MANIFEST.read_bytes()),
        'generated': generated,
        'note': 'All files are additive copies. No production source, preserved runtime, compiler, test or JVM was changed/executed.'
    }, indent=2) + '\n')
    print('Prepared common adjacency harness and separate namespace diagnostic sources; no Java executed.')


if __name__ == '__main__':
    main()
