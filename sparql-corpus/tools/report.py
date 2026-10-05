#!/usr/bin/env python3
"""Reconcile execution by case ID; report gaps and failures without changing outcomes."""
from __future__ import annotations
import collections, json, os, xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REPORTS=ROOT/'reports'; REPORTS.mkdir(exist_ok=True)

def read(path,default):
    return json.loads(path.read_text()) if path.exists() else default

def main():
    cases=read(ROOT/'corpus/cases.json',[])
    coverage=read(ROOT/'corpus/coverage.json',{})
    by_id={c['id']:c for c in cases}
    results=[]
    for path in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
        raw=path.read_bytes(); (REPORTS/path.name).write_bytes(raw)
        try:
            document=ET.fromstring(raw)
            for tc in document.iter('testcase'):
                error=tc.find('error'); failure=tc.find('failure'); skip=tc.find('skipped')
                state='error' if error is not None else 'failed' if failure is not None else 'skipped' if skip is not None else 'passed'
                element=error if error is not None else failure if failure is not None else skip
                results.append({'class':tc.get('classname',''),'name':tc.get('name',''),'seconds':tc.get('time'),
                                'status':state,'message':element.get('message','') if element is not None else '',
                                'detail':element.text if element is not None else None})
        except ET.ParseError as error:
            results.append({'file':path.name,'status':'report-parse-error','message':str(error)})
    per_case=[]; journal=REPORTS/'per-case-results.jsonl'
    if journal.exists():
        for line in journal.read_text().splitlines():
            try: per_case.append(json.loads(line))
            except json.JSONDecodeError as error: per_case.append({'status':'report-parse-error','message':str(error)})
    ids=[x['id'] for x in per_case if 'id' in x]
    families=collections.defaultdict(collections.Counter)
    kinds=collections.defaultdict(collections.Counter)
    for row in per_case:
        definition=by_id.get(row.get('id'),{})
        families[definition.get('family',row.get('family','unknown'))][row['status']]+=1
        kinds[definition.get('kind','unknown')][row['status']]+=1
    # Harness tests are deliberately not counted as imported upstream cases.
    calibration=[r for r in results if r.get('class','').startswith('org.hasmac.sparql.') and r['class'].split('.')[-1]!='CorpusTest']
    summary={'catalogueRecords':len(cases),
             'catalogueStatus':dict(collections.Counter(c.get('status','unknown') for c in cases)),
             'catalogueKinds':dict(collections.Counter(c.get('kind','unknown') for c in cases)),
             'junitCounts':dict(collections.Counter(r['status'] for r in results)),
             'corpusExecutionCounts':dict(collections.Counter(r['status'] for r in per_case)),
             'executionByFamily':{k:dict(v) for k,v in sorted(families.items())},
             'executionByKind':{k:dict(v) for k,v in sorted(kinds.items())},
             'calibrationCounts':dict(collections.Counter(r['status'] for r in calibration)),
             'junitResultsPresent':bool(results),'corpusUnexecuted':len(set(by_id)-set(ids)),
             'duplicateExecutionIds':len(ids)-len(set(ids)),
             'unknownExecutionIds':sorted(set(ids)-set(by_id)),
             'importOutcome':os.getenv('IMPORT_OUTCOME','not recorded'),
             'javaOutcome':os.getenv('JAVA_OUTCOME','not recorded'),
             'coverageComplete':False,'nativeInventoryIsNotPortedCoverage':True,'coverage':coverage}
    failures=[]; categories=collections.Counter()
    for event in per_case:
        if event['status'] not in {'failed','error'}: continue
        case=by_id.get(event.get('id'),{}); row=dict(event)
        exception=event.get('exception',''); message=event.get('message') or ''
        if 'MalformedQueryException' in exception: category='query-parse-rejection'
        elif any(s in message.lower() for s in ['timeout','timed out','suite.maxrows','exceeds','resource limit']): category='resource-or-time-limit'
        elif any(s in exception for s in ['RDFParseException','QueryResultParseException']): category='fixture-or-result-parsing'
        elif any(s in message for s in ['Unsupported RDF syntax','Unsupported result term','expected result format','upstream result kind']): category='adapter-format-or-kind'
        elif event['status']=='failed': category='result-assertion-difference'
        else: category='evaluation-or-harness-exception'
        row['triageCategory']=category
        row['triageCategoryIsNotRootCause']=True
        row['queryKind']=case.get('kind'); row['prerequisites']=case.get('fixtures',[])
        row['limitations']=case.get('limitations',[])
        row['caseDocumentation']='../corpus/cases/'+event.get('id','')+'.md'
        categories[category]+=1; failures.append(row)
    summary['failureCategories']=dict(categories)
    (REPORTS/'verification.json').write_text(json.dumps(summary,indent=2)+'\n')
    (REPORTS/'junit-results.json').write_text(json.dumps(results,indent=2)+'\n')
    (REPORTS/'failures.json').write_text(json.dumps(failures,indent=2)+'\n')
    (REPORTS/'unexecuted-case-ids.json').write_text(json.dumps(sorted(set(by_id)-set(ids)),indent=2)+'\n')
    table='| Family | Passed | Failed assertions | Errors | Skipped |\n|---|---:|---:|---:|---:|\n'
    for family,counts in sorted(families.items()):
        table+='| '+family+' | '+' | '.join(str(counts.get(k,0)) for k in ['passed','failed','error','skipped'])+' |\n'
    narrative=('# Verification\n\n'+table+'\n## Exact reconciliation\n\n```json\n'+json.dumps(summary,indent=2)+'\n```\n\n'
               'A catalogue entry is not a passing test. Query evaluation, syntax checks and harness calibration are reported separately. '
               'The XML and append-only per-case journal record actual execution. Missing reports are unverified, not passed. '
               'All failures remain failures and no expected answer was generated from RDF4J output. '
               'The failure categories are initial triage labels, not findings of defects in RDF4J. Extensions, adapter bugs, unsupported result formats and oracle policy differences require separate investigation. '
               'Native inventory-only declarations, unregistered queries and external suites remain coverage gaps.\n\n'
               '## Failure investigation\n\nThe complete list is in `failures.json`; use the case ID to locate its catalogue record, exact query, prerequisites and upstream expected result.\n\n')
    for row in failures[:20]:
        narrative+='### '+row.get('id','')+' '+row.get('name','')+'\n\n'+row.get('source','')+'\n\n```text\n'+str(row.get('message',''))[:3000]+'\n```\n\n'
    (REPORTS/'VERIFICATION.md').write_text(narrative)
    print('EXECUTION_VERIFICATION '+json.dumps(summary),flush=True)
    print('FAILURE_SAMPLE '+json.dumps([{k:x.get(k) for k in ['id','name','family','exception','message','triageCategory']} for x in failures[:8]]),flush=True)

if __name__=='__main__':main()
