#!/bin/bash
set -u
mkdir -p core/sail/lmdb/target/tmp/fork-1 experiments/overlay/store-regression
python3 - <<'PY'
from pathlib import Path
import re
p=Path('core/sail/lmdb/target/test-classes/org/eclipse/rdf4j/sail/lmdb')
n=sorted(x.stem for x in p.glob('*Test.class') if '$' not in x.name and re.search('Replay|MapGrowth|Lazy|Snapshot|Value|Detach|Result|Query|Mutation',x.stem))
for i in range(0,len(n),20):
 Path(f'experiments/overlay/store-regression/shard-{i//20:02d}.selection').write_text(','.join(n[i:i+20]))
print('Store boundary/replay test classes:',len(n))
PY
for selection in experiments/overlay/store-regression/shard-*.selection; do
 name=${selection%.selection}
 (
 flock 9
 experiments/overlay/mvn.sh -pl core/sail/lmdb -Dtest="$(cat "$selection")" -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=3G -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true surefire:test > "$name.log" 2>&1
 echo $? > "$name.exit"
 ) 9>../native-benchmark.lock
 echo "$name exit $(cat "$name.exit")"
done
