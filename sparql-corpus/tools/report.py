#!/usr/bin/env python3
"""Reconcile actual execution with stable catalogue IDs. Missing outcomes are never passes."""
from __future__ import annotations
import collections, json, os, xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REPORTS=ROOT/'reports';REPORTS.mkdir(exist_ok=True)

def read(path,default):return json.loads(path.read_text()) if path.exists() else default

def main():
    cases=read(ROOT/'corpus/cases.json',[]);coverage=read(ROOT/'corpus/coverage.json',{})
    by_id={c['id']:c for c in cases};results=[]
    for path in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
        raw=path.read_bytes();(REPORTS/path.name).write_bytes(raw)
        try:
            for tc in ET.fromstring(raw).iter('testcase'):
                error,failure,skip=tc.find('error'),tc.find('failure'),tc.find('skipped')
                state='error' if error is not None else 'failed' if failure is not None else 'skipped' if skip is not None else 'passed'
                element=error if error is not None else failure if failure is not None else skip
                results.append({'class':tc.get('classname',''),'name':tc.get('name',''),'seconds':tc.get('time'),'status':state,
                  'message':element.get('message','') if element is not None else '', 'detail':element.text if element is not None else None})
        except ET.ParseError as error:results.append({'file':path.name,'status':'report-parse-error','message':str(error)})
    per_case=[];journal=REPORTS/'per-case-results.jsonl'
    if journal.exists():
        for line in journal.read_text().splitlines():
            try:per_case.append(json.loads(line))
            except json.JSONDecodeError as error:per_case.append({'status':'report-parse-error','message':str(error)})
    ids=[x['id']for x in per_case if 'id'in x]
    families=collections.defaultdict(collections.Counter);kinds=collections.defaultdict(collections.Counter)
    for row in per_case:
        definition=by_id.get(row.get('id'),{})
        families[definition.get('family',row.get('family','unknown'))][row['status']]+=1
        kinds[definition.get('kind','unknown')][row['status']]+=1
    harness=[r for r in results if r.get('class','').startswith('org.hasmac.sparql.')and r['class'].split('.')[-1]!='CorpusTest']
    unit=[r for r in harness if r['class'].split('.')[-1]!='FixtureTest']
    fixtures=[r for r in harness if r['class'].split('.')[-1]=='FixtureTest']
    summary={'catalogueRecords':len(cases),'catalogueStatus':dict(collections.Counter(c.get('status','unknown')for c in cases)),
      'catalogueKinds':dict(collections.Counter(c.get('kind','unknown')for c in cases)),
      'junitCounts':dict(collections.Counter(r['status']for r in results)),
      'corpusExecutionCounts':dict(collections.Counter(r['status']for r in per_case)),
      'executionByFamily':{k:dict(v)for k,v in sorted(families.items())},'executionByKind':{k:dict(v)for k,v in sorted(kinds.items())},
      'harnessCounts':dict(collections.Counter(r['status']for r in harness)),
      'oracleUnitCounts':dict(collections.Counter(r['status']for r in unit)),
      'engineFacingFixtureCounts':dict(collections.Counter(r['status']for r in fixtures)),
      'junitResultsPresent':bool(results),'corpusUnexecuted':len(set(by_id)-set(ids)),
      'duplicateExecutionIds':len(ids)-len(set(ids)),'unknownExecutionIds':sorted(set(ids)-set(by_id)),
      'importOutcome':os.getenv('IMPORT_OUTCOME','not recorded'),'javaOutcome':os.getenv('JAVA_OUTCOME','not recorded'),
      'coverageComplete':False,'nativeInventoryIsNotPortedCoverage':True,'coverage':coverage}
    failures=[];categories=collections.Counter()
    for event in per_case:
        if event['status']not in {'failed','error'}:continue
        case=by_id.get(event.get('id'),{});row=dict(event);exception=event.get('exception','');message=event.get('message')or''
        if 'MalformedQueryException'in exception:category='query-parse-rejection'
        elif any(s in message.lower()for s in ['timeout','timed out','suite.maxrows','exceeds','resource limit']):category='resource-or-time-limit'
        elif any(s in exception for s in ['RDFParseException','QueryResultParseException']):category='fixture-or-result-parsing'
        elif any(s in message for s in ['Unsupported RDF syntax','Unsupported result term','expected result format','upstream result kind']):category='adapter-format-or-kind'
        elif event['status']=='failed':category='result-assertion-difference'
        else:category='evaluation-or-harness-exception'
        row.update(triageCategory=category,triageCategoryIsNotRootCause=True,queryKind=case.get('kind'),prerequisites=case.get('fixtures',[]),
          limitations=case.get('limitations',[]),caseDocumentation='../corpus/cases/'+event.get('id','')+'.md')
        categories[category]+=1;failures.append(row)
    summary['failureCategories']=dict(categories)
    documents={'verification.json':summary,'junit-results.json':results,'failures.json':failures,
      'unexecuted-case-ids.json':sorted(set(by_id)-set(ids))}
    for name,value in documents.items():(REPORTS/name).write_text(json.dumps(value,indent=2,ensure_ascii=True)+'\n')
    table='| Family | Passed | Failed assertions | Errors | Skipped |\n|---|---:|---:|---:|---:|\n'
    for family,counts in sorted(families.items()):table+='| '+family+' | '+' | '.join(str(counts.get(k,0))for k in ['passed','failed','error','skipped'])+' |\n'
    narrative='# Verification\n\n'+table+'\n## Exact reconciliation\n\n```json\n'+json.dumps(summary,indent=2)+'\n```\n\n'
    narrative+='Imported queries, syntax tests, comparator calibration and engine-facing fixture checks are distinct. The full corpus is not exhaustive. Missing journal entries, interrupted tests, blocked prerequisites and successful packaging are not passes. Original expected answers remain unchanged. Failure categories are triage labels, not findings of RDF4J defects.\n\n'
    for row in failures[:12]:narrative+='### '+row.get('id','')+' '+row.get('name','')+'\n\n'+row.get('source','')+'\n\n```text\n'+str(row.get('message',''))[:2200]+'\n```\n\n'
    (REPORTS/'VERIFICATION.md').write_text(narrative,encoding='utf-8',errors='backslashreplace')
    print('EXECUTION_VERIFICATION '+json.dumps(summary),flush=True)
    print('FAILURE_SAMPLE '+json.dumps([{k:x.get(k)for k in ['id','name','family','exception','message','triageCategory']}for x in failures[:5]]),flush=True)

if __name__=='__main__':main()
