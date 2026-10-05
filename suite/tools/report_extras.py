#!/usr/bin/env python3
"""Compact, independently inspectable cohort/failure details for build logs."""
from __future__ import annotations
import collections,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'reports';R.mkdir(exist_ok=True)
def read(path,default):return json.loads(path.read_text()) if path.exists() else default
def dump(name,value):(R/name).write_text(json.dumps(value,indent=2,ensure_ascii=True)+'\n')
cases=read(ROOT/'corpus/cases.json',[]);byid={x['id']:x for x in cases};outcomes=read(R/'corpus-results.json',[])
cohorts=collections.defaultdict(collections.Counter);native_failures=[];qlever_failures=[]
for outcome in outcomes:
    c=byid.get(outcome.get('id'),{})
    if c.get('sourceAssertion'):
        cohorts[c['source']['path']][outcome['status']]+=1
        if outcome['status'] in {'failed','error'}:
            native_failures.append({'id':outcome['id'],'name':outcome['name'],'status':outcome['status'],'detail':outcome.get('detail','')[:1100],'source':c['source'],'query':c['query'],'expected':c['expected']})
    if c.get('project')=='qlever' and outcome['status'] in {'failed','error'}:qlever_failures.append({'id':outcome['id'],'name':outcome['name'],'status':outcome['status'],'detail':outcome.get('detail','')[:1100]})
summary={'nativeCohorts':{k:dict(v) for k,v in cohorts.items()},'nativeFailures':len(native_failures),'qleverFailures':len(qlever_failures),'upstreamOracleAudit':read(R/'upstream-oracle-summary.json',None),'referenceAuditExecution':read(R/'reference-audit-execution.json',None),'registration':read(ROOT/'corpus/registration-summary.json',None)}
dump('cohort-summary.json',summary);dump('native-failures.json',native_failures);dump('qlever-failures.json',qlever_failures)
# Console output stays bounded; full definitions remain in the source archive.
print(json.dumps(summary,ensure_ascii=True))
print('NATIVE FAILURE SAMPLE '+json.dumps([{k:v for k,v in x.items() if k not in {'query','expected','source'}} for x in native_failures[:20]],ensure_ascii=True))
print('QLEVER FAILURES '+json.dumps(qlever_failures,ensure_ascii=True))
