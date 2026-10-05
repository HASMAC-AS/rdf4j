#!/usr/bin/env python3
"""Reconcile all execution records; missing/aborted tests are never successes."""
import collections, json, os, xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];r=ROOT/'reports';r.mkdir(exist_ok=True)
def read(path,default):return json.loads(path.read_text()) if path.exists() else default
cases=read(ROOT/'corpus/cases.json',[]);coverage=read(ROOT/'corpus/coverage.json',{})
status=collections.Counter(c.get('status','unknown') for c in cases);results=[]
for p in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
    raw=p.read_bytes();(r/p.name).write_bytes(raw)
    try:
        root=ET.fromstring(raw)
        for tc in root.iter('testcase'):
            error=tc.find('error');failure=tc.find('failure');skip=tc.find('skipped')
            state='error' if error is not None else 'failed' if failure is not None else 'skipped' if skip is not None else 'passed'
            el=error if error is not None else failure if failure is not None else skip
            results.append({'class':tc.get('classname'),'name':tc.get('name'),'seconds':tc.get('time'),'status':state,'message':el.get('message','') if el is not None else '', 'detail':el.text if el is not None else None})
    except ET.ParseError as exc:results.append({'file':p.name,'status':'report-parse-error','message':str(exc)})
percase=[];path=r/'per-case-results.jsonl'
if path.exists():
    for line in path.read_text().splitlines():
        try:percase.append(json.loads(line))
        except json.JSONDecodeError as exc:percase.append({'status':'report-parse-error','message':str(exc)})
ids=[x['id'] for x in percase if 'id' in x];catalogue_ids={c['id'] for c in cases}
calibration=[x for x in results if x.get('class','').split('.')[-1] in {'FixtureTest','ResultOracleTest','ReducedOracleTest'}]
summary={'catalogueRecords':len(cases),'catalogueStatus':dict(status),'junitCounts':dict(collections.Counter(x['status'] for x in results)),'corpusExecutionCounts':dict(collections.Counter(x['status'] for x in percase)),'calibrationCounts':dict(collections.Counter(x['status'] for x in calibration)),'junitResultsPresent':bool(results),'corpusUnexecuted':len(catalogue_ids-set(ids)),'duplicateExecutionIds':len(ids)-len(set(ids)),'unknownExecutionIds':sorted(set(ids)-catalogue_ids),'importOutcome':os.getenv('IMPORT_OUTCOME','not recorded'),'javaOutcome':os.getenv('JAVA_OUTCOME','not recorded'),'coverageComplete':False,'nativeInventoryIsNotPortedCoverage':True,'coverage':coverage}
(r/'verification.json').write_text(json.dumps(summary,indent=2)+'\n');(r/'junit-results.json').write_text(json.dumps(results,indent=2)+'\n')
failures=[x for x in percase if x['status'] in {'failed','error'}];(r/'failures.json').write_text(json.dumps(failures,indent=2)+'\n')
(r/'VERIFICATION.md').write_text('# Verification\n\n```json\n'+json.dumps(summary,indent=2)+'\n```\n\nXML and JSON reports record actual execution. Missing reports mean unverified, not passed. Failures have not been suppressed and no expected result has been rewritten from RDF4J output. Extension incompatibilities, stricter oracle policies and harness defects must be separated from genuine RDF4J regressions during triage. Native inventory-only declarations, unregistered queries and external suites remain coverage gaps.\n\n## First failing cases\n\n'+''.join('### '+x.get('id','')+' '+x.get('name','')+'\n\n'+x.get('source','')+'\n\n```text\n'+str(x.get('message',''))[:3000]+'\n```\n\n' for x in failures[:20]))
print('EXECUTION_VERIFICATION '+json.dumps(summary),flush=True)
print('FAILURE_SAMPLE '+json.dumps([{k:x.get(k) for k in ['id','name','family','exception','message']} for x in failures[:8]]),flush=True)
