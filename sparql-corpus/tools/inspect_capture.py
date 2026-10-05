#!/usr/bin/env python3
"""Print compact extraction, calibration and execution evidence from retained reports."""
import collections, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def read(path,default):return json.loads(path.read_text()) if path.exists() else default
def emit(label,value):print(label+' '+json.dumps(value,ensure_ascii=True),flush=True)
for name in ['coverage.json','jena-capture-summary.json','lazy-port-summary.json','native-adapter-gaps.json']:
    emit(name,read(ROOT/'corpus'/name,{}))
summary=read(ROOT/'reports/verification.json',{});summary.pop('coverage',None);emit('VERIFICATION',summary)
audit=ROOT/'corpus/jena-capture-audit.jsonl'
if audit.exists():
    rows=[json.loads(s) for s in audit.read_text().splitlines()]
    emit('CAPTURE_GAPS',[r for r in rows if r.get('gaps')][:8])
report=ROOT/'reports/per-case-results.jsonl'
if report.exists():
    rows=[json.loads(s) for s in report.read_text().splitlines()]
    captured=[r for r in rows if r.get('family') in {'jena-native-captured','qlever-native-lazy'}]
    counts=collections.defaultdict(collections.Counter);examples={}
    for row in captured:
        cls=row['name'].split('.')[0];counts[cls][row['status']]+=1
        if row['status'] in {'error','failed'}:examples.setdefault((cls,row.get('exception','')),row)
    emit('CAPTURE_OUTCOMES_BY_CLASS',{k:dict(v) for k,v in counts.items()})
    emit('CAPTURE_FAILURE_EXAMPLES',[{**r,'message':str(r.get('message',''))[:900]}for r in list(examples.values())[:12]])
    emit('QLEVER_FIXTURE_EXAMPLES',[{**r,'message':str(r.get('message',''))[:1600]}for r in rows if r.get('family')=='qlever-yaml' and r['status']=='error'][:2])
junit=read(ROOT/'reports/junit-results.json',[])
noncorpus=[r for r in junit if r.get('class','').split('.')[-1]!='CorpusTest']
counts=collections.defaultdict(collections.Counter)
for row in noncorpus:counts[row.get('class','')][row['status']]+=1
emit('HARNESS_BY_CLASS',{k:dict(v) for k,v in counts.items()})
emit('HARNESS_FAILURES',[{**r,'detail':str(r.get('detail',''))[:4000],'message':str(r.get('message',''))[:4000]}for r in noncorpus if r['status'] in {'error','failed'}])
emit('PATCH_VERIFICATION',read(ROOT.parent/'deliverables/PATCH-VERIFICATION.json',[]))
emit('SNAPSHOT',read(ROOT.parent/'deliverables/snapshot.json',{}))
