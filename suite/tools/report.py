#!/usr/bin/env python3
"""Separate corpus outcomes, oracle calibration and discovery coverage."""
from __future__ import annotations
import collections,json,os,re,shutil,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'reports';R.mkdir(exist_ok=True)
def read(p,default):return json.loads(p.read_text()) if p.exists() else default
def dump(p,v):p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n')
cases=read(ROOT/'corpus/cases.json',[]);byid={c['id']:c for c in cases}
discovery=read(ROOT/'corpus/discovery.json',{})
results=[]
for p in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
    shutil.copy2(p,R/p.name)
    try:
        for t in ET.parse(p).getroot().iter('testcase'):
            e=next((t.find(n) for n in ['error','failure','skipped'] if t.find(n) is not None),None)
            state={'failure':'failed','error':'error','skipped':'skipped'}.get(e.tag,'unknown') if e is not None else 'passed'
            results.append({'class':t.get('classname'),'name':t.get('name'),'seconds':t.get('time'),'status':state,'message':e.get('message','') if e is not None else '', 'detail':e.text if e is not None else None})
    except ET.ParseError as e:results.append({'file':p.name,'status':'report-error','message':str(e)})
ledger=[]
if (R/'case-results.ndjson').exists():
    for line in (R/'case-results.ndjson').read_text().splitlines():
        try:
            entry=json.loads(line);source=byid.get(entry.get('id'),{});entry['project']=source.get('project');entry['kind']=source.get('kind');entry['source']=source.get('source');ledger.append(entry)
        except json.JSONDecodeError:ledger.append({'status':'incomplete-record','detail':line})
calibration=[x for x in results if x.get('class','').endswith('ResultOracleTest')]
counts=lambda rows:dict(collections.Counter(x['status'] for x in rows))
summary={'catalogueRecords':len(cases),'catalogueStatus':dict(collections.Counter(c['status'] for c in cases)),'catalogueKinds':dict(collections.Counter(c['kind'] for c in cases)), 'corpusOutcomes':counts(ledger),'corpusByProject':{p:counts([x for x in ledger if x.get('project')==p]) for p in ['jena','qlever']},'corpusByKind':{p:counts([x for x in ledger if x.get('kind')==p]) for p in ['query','positive-syntax','negative-syntax']},'calibrationOutcomes':counts(calibration),'allJunitOutcomes':counts(results),'corpusRecordsWithOutcome':len(ledger),'reportsPresent':bool(results),'importOutcome':os.getenv('IMPORT_OUTCOME','not recorded'),'javaOutcome':os.getenv('JAVA_OUTCOME','not recorded'),'exhaustivePortComplete':False,'nativeDeclarations':discovery.get('nativeDeclarations'),'nativeQueryCandidates':discovery.get('nativeQueryCandidates'),'nativeDeclarationsWithAdapters':discovery.get('nativeDeclarationsWithAdapters'),'manifestDiagnostics':len(discovery.get('diagnostics',[])),'unresolvedIncludes':sum(not x.get('present') for x in discovery.get('manifestIncludes',[]))}
pylog=(R/'python-tests.log').read_text() if (R/'python-tests.log').exists() else ''
match=re.search(r'Ran (\d+) tests?',pylog);summary['pythonTests']={'count':int(match.group(1)) if match else None,'passed':bool(re.search(r'\nOK\s*$',pylog))}
sample=[];groups=collections.Counter()
for x in ledger:
    if x['status'] not in {'failed','error'}:continue
    text=x.get('detail','');group=text.split(':',1)[0];groups[group]+=1
    if sum(y.get('exceptionType')==group for y in sample)<2 and len(sample)<12:sample.append({'id':x.get('id'),'name':x.get('name'),'status':x['status'],'exceptionType':group,'detail':text[:1800],'source':x.get('source')})
summary['errorClasses']=dict(groups)
dump(R/'verification.json',summary);dump(R/'junit-results.json',results);dump(R/'corpus-results.json',ledger);dump(R/'failure-sample.json',sample)
dump(R/'import-gaps.json',{'diagnostics':discovery.get('diagnostics',[]),'unresolvedIncludes':[x for x in discovery.get('manifestIncludes',[]) if not x.get('present')]})
log=(R/'maven.log').read_text(errors='replace') if (R/'maven.log').exists() else ''
(R/'diagnostic-tail.txt').write_text('\n'.join(log.splitlines()[-100:])[-18000:]+'\n')
(R/'VERIFICATION.md').write_text('# Recorded verification\n\n```json\n'+json.dumps(summary,indent=2)+'\n```\n\nCorpus results are recorded separately from oracle calibration tests. Inventory-only native tests are not counted as ported. Missing reports mean unverified, not passed. Failures remain failures; expected results are never regenerated from RDF4J. A mismatch may be a porting/comparator or engine-extension difference, not necessarily an RDF4J defect. See the JSON/XML files for exact cases and errors.\n')
print(json.dumps(summary))
