#!/bin/bash
set -euo pipefail
RESULTS=${1:-experiments/overlay/spill-final-regression}
export RESULTS
mkdir -p core/sail/lmdb/target/tmp/fork-1 "$RESULTS"
git rev-parse HEAD > "$RESULTS/source-commit.txt"
sha256sum core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/{NativeExpressionDomain,NativeBudgetedWideDistinct,NativeSegmentedTupleTable}.java > "$RESULTS/source.sha256"
(
 flock 9
 experiments/overlay/mvn.sh -o -pl core/sail/lmdb -DskipTests test-compile > "$RESULTS/compile.log" 2>&1
) 9>../native-benchmark.lock
python3 - <<'PY'
from pathlib import Path
import os
result = Path(os.environ['RESULTS'])
classes = Path('core/sail/lmdb/target/test-classes/org/eclipse/rdf4j/sail/lmdb/evaluation')
names = sorted('org.eclipse.rdf4j.sail.lmdb.evaluation.' + x.stem
               for x in classes.glob('*Test.class') if '$' not in x.name)
(result / 'selection.txt').write_text('\n'.join(names) + '\n')
for i in range(0, len(names), 40):
    (result / f'shard-{i//40:02d}.selection').write_text(','.join(names[i:i+40]))
print('Evaluation test classes:', len(names))
PY
for selection in "$RESULTS"/shard-*.selection; do
 name=${selection%.selection}
 if [[ -f "$RESULTS/PAUSE" ]]; then
  echo "Paused before $name by request; remaining shards not run"
  exit 0
 fi
 (
 flock 9
 touch "$name.started"
 set +e
 experiments/overlay/mvn.sh -o -pl core/sail/lmdb -Dtest="$(cat "$selection")" -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=3G -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true surefire:test > "$name.log" 2>&1
 status=$?
 set -e
 echo "$status" > "$name.exit"
 mkdir -p "$name-reports"
 while IFS= read -r class; do
  for report in "core/sail/lmdb/target/surefire-reports/$class.txt" "core/sail/lmdb/target/surefire-reports/TEST-$class.xml"; do
   if [[ -f "$report" && "$report" -nt "$name.started" ]]; then cp "$report" "$name-reports/"; fi
  done
 done < <(tr ',' '\n' < "$selection"; echo)
 ) 9>../native-benchmark.lock
 echo "$name exit $(cat "$name.exit")"
 # Release the shared lock between every shard for the ordered-route worker.
 sleep 1
done
