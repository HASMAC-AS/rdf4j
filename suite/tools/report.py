#!/usr/bin/env python3
"""Reconcile every corpus record with actual Jupiter outcomes, including malformed Unicode."""
from __future__ import annotations
import collections,html,json,os,re,shutil,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'reports';R.mkdir(exist_ok=True)
def read(p,default):return json.loads(p.read_text()) if p.exists() else default
def dump(p,v):p.write_text(json.dumps(v,indent=2,ensure_ascii=True)+'\n')
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
            entry=json.loads(line);source=byid.get(entry.get('id'),{})
            for key in ['project','kind','source','scope','requires']:entry[key]=source.get(key)
            ledger.append(entry)
        except json.JSONDecodeError:ledger.append({'status':'incomplete-record','detail':line})
calibration=[x for x in results if x.get('class','').rsplit('.',1)[-1] in {'ResultOracleTest','NativeAssertionOracleTest'}]
xmlcorpus=[x for x in results if x.get('class','').endswith('.CorpusTest')]
counts=lambda rows:dict(collections.Counter(x['status'] for x in rows))
ids=[x.get('id') for x in ledger];missing=sorted(set(byid)-set(ids));duplicates=[x for x,n in collections.Counter(ids).items() if n>1]
summary={'catalogueRecords':len(cases),'catalogueStatus':dict(collections.Counter(c['status'] for c in cases)),'catalogueKinds':dict(collections.Counter(c['kind'] for c in cases)), 'corpusOutcomes':counts(ledger),'corpusByProject':{p:counts([x for x in ledger if x.get('project')==p]) for p in ['jena','qlever']},'corpusByKind':{p:counts([x for x in ledger if x.get('kind')==p]) for p in ['query','positive-syntax','negative-syntax']},'corpusByScope':{p:counts([x for x in ledger if x.get('scope')==p]) for p in sorted({x.get('scope') for x in ledger if x.get('scope')})},'calibrationOutcomes':counts(calibration),'allJunitOutcomes':counts(results),'corpusXmlOutcomes':counts(xmlcorpus),'corpusRecordsWithOutcome':len(ledger),'missingOutcomeIds':missing,'duplicateOutcomeIds':duplicates,'outcomesReconciled':bool(cases) and not missing and not duplicates and counts(ledger)==counts(xmlcorpus),'reportsPresent':bool(results),'importOutcome':os.getenv('IMPORT_OUTCOME','not recorded'),'javaOutcome':os.getenv('JAVA_OUTCOME','not recorded'),'exhaustivePortComplete':False,'nativeDeclarations':discovery.get('nativeDeclarations'),'nativeQueryCandidates':discovery.get('nativeQueryCandidates'),'nativeDeclarationsWithAdapters':discovery.get('nativeDeclarationsWithAdapters'),'manifestDiagnostics':len(discovery.get('diagnostics',[])),'unresolvedIncludes':sum(not x.get('present') for x in discovery.get('manifestIncludes',[]))}
pylog=(R/'python-tests.log').read_text() if (R/'python-tests.log').exists() else ''
match=re.search(r'Ran (\d+) tests?',pylog);summary['pythonTests']={'count':int(match.group(1)) if match else None,'passed':bool(re.search(r'\nOK\s*$',pylog))}
sample=[];groups=collections.Counter();details=collections.Counter()
for x in ledger:
    if x['status'] not in {'failed','error'}:continue
    text=x.get('detail','');group=text.split(':',1)[0];groups[group]+=1
    message=re.sub(r'line \d+, column \d+','line N, column N',text.split('\n',1)[0]);details[message[:240]]+=1
    if sum(y.get('exceptionType')==group for y in sample)<2 and len(sample)<12:
        c=byid.get(x.get('id'),{})
        sample.append({'id':x.get('id'),'name':x.get('name'),'status':x['status'],'exceptionType':group,'detail':text[:1800],'source':x.get('source'),'requires':c.get('requires'),'query':c.get('query','')[:2500]})
summary['errorClasses']=dict(groups);summary['topFailureMessages']=[{'message':m,'count':n} for m,n in details.most_common(15)]
dump(R/'verification.json',summary);dump(R/'junit-results.json',results);dump(R/'corpus-results.json',ledger);dump(R/'failure-sample.json',sample)
dump(R/'import-gaps.json',{'diagnostics':discovery.get('diagnostics',[]),'unresolvedIncludes':[x for x in discovery.get('manifestIncludes',[]) if not x.get('present')]})
log=(R/'maven.log').read_text(errors='replace') if (R/'maven.log').exists() else ''
(R/'diagnostic-tail.txt').write_text('\n'.join(log.splitlines()[-100:])[-18000:]+'\n',errors='backslashreplace')
(R/'VERIFICATION.md').write_text('# Recorded verification\n\n```json\n'+json.dumps(summary,indent=2,ensure_ascii=True)+'\n```\n\nCorpus results are recorded separately from harness calibration tests. Missing records and discrepancies are reported explicitly. Inventory-only native tests are not counted as ported. Missing reports mean unverified, not passed. Failures remain failures; expected results are never regenerated from RDF4J. A mismatch may be a porting/comparator or engine-extension difference, not necessarily an RDF4J defect. The source archive contains each prerequisite, query and expected-result source.\n')
# The report remains searchable offline without a server or fetch permissions.
payload=json.dumps(ledger,ensure_ascii=True).replace('<','\\u003c')
page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>SPARQL corpus execution report</title><style>body{font:16px system-ui;max-width:1100px;margin:2rem auto;padding:1rem}input{font:inherit;padding:.6rem;width:100%;box-sizing:border-box}article{border-top:1px solid #aaa;margin-top:1rem;padding-top:1rem}pre{white-space:pre-wrap;overflow-wrap:anywhere}button{padding:.6rem}</style><h1>Recorded corpus outcomes</h1><p>Execution failures are retained. Capability-dependent cases and strict comparison differences are not automatically RDF4J defects.</p><input id="q" aria-label="Search outcomes" placeholder="Filter by query name, status, source, or error"><p id="n"></p><main id="rows"></main><button id="more">More results</button><script>const data='''+payload+''';let limit=50;const $=s=>document.getElementById(s);function node(t,s){let e=document.createElement(t);e.textContent=s;return e}function show(){let q=$('q').value.toLowerCase(),v=data.filter(x=>JSON.stringify(x).toLowerCase().includes(q));$('n').textContent=v.length+' matches';$('rows').replaceChildren();for(let x of v.slice(0,limit)){let a=node('article','');a.append(node('h2',x.status+' — '+x.name),node('small',x.id+' · '+x.seconds+' seconds'));let l=node('a','Full query, prerequisites and oracle');l.href='../corpus/cases/'+x.id+'.md';a.append(l,node('pre',x.detail));$('rows').append(a)}$('more').hidden=limit>=v.length}$('q').oninput=()=>{limit=50;show()};$('more').onclick=()=>{limit+=50;show()};show();</script></html>'''
(R/'results.html').write_text(page)
print(json.dumps(summary,ensure_ascii=True))
