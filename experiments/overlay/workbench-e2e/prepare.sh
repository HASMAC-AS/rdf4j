#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$ROOT"
D=experiments/overlay/workbench-e2e
JAVA=experiments/overlay/tools/jdk-25.0.5+8/bin
exec 9>experiments/overlay/native-benchmark.lock; flock 9
python3 - <<'PY'
import zipfile,pathlib,shutil,hashlib,json
root=pathlib.Path('experiments/overlay/workbench-e2e')
jar=root/'boot-frozen.jar'
if not jar.exists():raise SystemExit('Freeze the Boot artifact before concurrent source builds')
with zipfile.ZipFile(jar) as z:
 for name in z.namelist():
  if name.startswith(('BOOT-INF/classes/','BOOT-INF/lib/')):z.extract(name,root/'runtime')
for version in ['writer-off','writer-on']:
 dest=root/version
 if dest.exists():raise SystemExit(f'Refusing to overwrite frozen {dest}')
 dest.mkdir()
 with zipfile.ZipFile(next((root/'runtime/BOOT-INF/lib').glob('rdf4j-queryresultio-binary-*.jar'))) as binary:
  binary.extractall(dest)
(root/'boot-jar.sha256').write_text(hashlib.sha256(jar.read_bytes()).hexdigest()+' '+str(jar)+'\n')
PY
CP="$D/runtime/BOOT-INF/classes:$D/runtime/BOOT-INF/lib/*"
mkdir -p "$D/harness"
"$JAVA/javac" -cp "$CP" -d "$D/harness" "$D/WorkbenchBenchmarkServer.java"
"$JAVA/javac" -cp "$CP" -d "$D/writer-on" "$D/pr-src/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java"
"$JAVA/java" -Dorg.eclipse.rdf4j.appdata.basedir="$ROOT/$D/appdata" -cp "$D/harness:$CP" bench.WorkbenchBenchmarkServer prepare > "$D/prepare.log" 2>&1
python3 - <<'PY'
import pathlib,shutil,hashlib,json
root=pathlib.Path('experiments/overlay/workbench-e2e')
text=(root/'prepare.log').read_text();dest=pathlib.Path(next(x.split('=',1)[1] for x in text.splitlines() if x.startswith('FIXTURE_DEST=')))
source=pathlib.Path('experiments/overlay/lifetime/fixture-8000')
# Separate writable fixture; source is never opened by this harness.
for p in source.iterdir():
 if p.is_dir():shutil.copytree(p,dest/p.name)
 else:shutil.copy2(p,dest/p.name)
manifest={}
for directory in ['writer-off','writer-on','harness']:
 manifest[directory]={str(p.relative_to(root/directory)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((root/directory).rglob('*')) if p.is_file()}
(root/'artifact-manifest.json').write_text(json.dumps(manifest,indent=2))
PY
