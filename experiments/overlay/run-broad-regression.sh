#!/bin/bash
set -u
RESULTS=${1:-experiments/overlay/broad-regression}
export RESULTS
mkdir -p core/sail/lmdb/target/tmp/fork-1 "$RESULTS"
python3 - <<'PY'
from pathlib import Path
import os
result=Path(os.environ["RESULTS"])
p=Path('core/sail/lmdb/target/test-classes/org/eclipse/rdf4j/sail/lmdb/evaluation')
n=sorted(x.stem for x in p.glob('*Test.class') if '$' not in x.name)
for i in range(0,len(n),40):
 (result / f'shard-{i//40:02d}.selection').write_text(','.join(n[i:i+40]))
print('Evaluation test classes:',len(n))
PY
for selection in "$RESULTS"/shard-*.selection; do
  name=${selection%.selection}
  (
  flock 9
  experiments/overlay/mvn.sh -pl core/sail/lmdb -Dtest="$(cat "$selection")" -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=3G -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true surefire:test > "$name.log" 2>&1
  echo $? > "$name.exit"
  mkdir -p "$name-reports"
  cp core/sail/lmdb/target/surefire-reports/*.txt "$name-reports/"
  ) 9>../native-benchmark.lock
  echo "$name exit $(cat "$name.exit")"
done
