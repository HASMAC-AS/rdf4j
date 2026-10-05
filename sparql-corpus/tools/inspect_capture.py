#!/usr/bin/env python3
"""Print compact, reproducible evidence for review without dumping the full corpus."""
import collections,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def read(path,default):return json.loads(path.read_text()) if path.exists() else default
for name in ['coverage.json','jena-capture-summary.json','native-adapter-gaps.json']:
    value=read(ROOT/'corpus'/name,{})
    print(name+' '+json.dumps(value),flush=True)
summary=read(ROOT/'reports/verification.json',{})
summary.pop('coverage',None);print('VERIFICATION '+json.dumps(summary),flush=True)
audit=ROOT/'corpus/jena-capture-audit.jsonl'
if audit.exists():
    rows=[json.loads(s) for s in audit.read_text().splitlines()]
    print('CAPTURE_GAPS '+json.dumps([r for r in rows if r.get('gaps')][:30]),flush=True)
report=ROOT/'reports/per-case-results.jsonl'
if report.exists():
    rows=[json.loads(s) for s in report.read_text().splitlines()]
    captured=[r for r in rows if r.get('family')=='jena-native-captured']
    counts=collections.defaultdict(collections.Counter)
    examples={}
    for r in captured:
        cls=r['name'].split('.')[0];counts[cls][r['status']]+=1
        if r['status'] in {'error','failed'}:examples.setdefault((cls,r.get('exception','')),r)
    print('CAPTURE_OUTCOMES_BY_CLASS '+json.dumps({k:dict(v) for k,v in counts.items()}),flush=True)
    print('CAPTURE_FAILURE_EXAMPLES '+json.dumps([{**r,'message':str(r.get('message',''))[:1400]} for r in list(examples.values())[:25]]),flush=True)
