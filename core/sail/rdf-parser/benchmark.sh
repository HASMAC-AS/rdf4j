#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
module=core/sail/rdf-parser
baseline=${BASELINE_COMMIT:-7afc4b787697dc3f6345e8d667d4083ea6aea57d}
output="$PWD/$module/target/benchmarks"
mkdir -p "$output/baseline-src" "$output/baseline-classes"
mvn -B -ntp -nsu -pl "$module" dependency:build-classpath \
  -Dmdep.includeScope=test -Dmdep.outputFile="$output/classpath.txt"
deps=$(cat "$output/classpath.txt")
git archive "$baseline" "$module/src/main/java" | tar -x -C "$output/baseline-src"
find "$output/baseline-src" -name '*.java' > "$output/sources.txt"
javac --release 25 -proc:none -cp "$deps" -d "$output/baseline-classes" @"$output/sources.txt"
# Both variants use the same fixtures, generated harness, dependencies, JVM options and machine.
# Baseline classes precede the harness; optimized production classes are excluded from its classpath.
for variant in baseline optimized; do
  if [[ "$variant" == baseline ]]; then classes="$output/baseline-classes"; else classes="$PWD/$module/target/classes"; fi
  cp="$classes:$PWD/$module/target/test-classes:$deps"
  java -cp "$cp" org.openjdk.jmh.Main 'RdfParserBenchmark.(tuple.*|service.*)' \
    -p statements=10,1000 -f 2 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -foe true \
    -rf json -rff "$output/$variant-documents.json" > "$output/$variant-documents.log" 2>&1
  java -cp "$cp" org.openjdk.jmh.Main 'RdfParserBenchmark.rewrite.*' \
    -f 2 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -foe true \
    -rf json -rff "$output/$variant-rewrites.json" > "$output/$variant-rewrites.log" 2>&1
done
python3 - "$output" <<'PYRESULT'
import json, pathlib, sys
root = pathlib.Path(sys.argv[1])
for kind in ('documents', 'rewrites'):
    baseline = {(r['benchmark'], tuple(sorted(r.get('params', {}).items()))): r
                for r in json.loads((root / ('baseline-' + kind + '.json')).read_text())}
    for row in json.loads((root / ('optimized-' + kind + '.json')).read_text()):
        before = baseline[(row['benchmark'], tuple(sorted(row.get('params', {}).items())))]
        def metric(r, key):
            return r['primaryMetric']['score'] if key == 'time' else r['secondaryMetrics']['gc.alloc.rate.norm']['score']
        print(json.dumps({'benchmark': row['benchmark'].rsplit('.', 1)[-1], 'params': row.get('params', {}),
                          'baseline_us': metric(before, 'time'), 'optimized_us': metric(row, 'time'),
                          'speedup': metric(before, 'time') / metric(row, 'time'),
                          'baseline_B_op': metric(before, 'alloc'), 'optimized_B_op': metric(row, 'alloc')}))
PYRESULT
