#!/usr/bin/env python3
from __future__ import annotations
import collections,json,os,shutil,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'reports';R.mkdir(exist_ok=True)
cases=json.loads((ROOT/'corpus/cases.json').read_text()) if (ROOT/'corpus/cases.json').exists() else []
results=[]
for p in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
    shutil.copy2(p,R/p.name)
    try:
        for t in ET.parse(p).getroot().iter('testcase'):
            e=next((t.find(n) for n in ['error','failure','skipped'] if t.find(n) is not None),None)
            state={'failure':'failed','error':'error','skipped':'skipped'}.get(e.tag,'unknown') if e is not None else 'passed'
            results.append({'class':t.get('classname'),'name':t.get('name'),'seconds':t.get('time'),'status':state,'message':e.get('message','') if e is not None else '', 'detail':e.text if e is not None else None})
    except ET.ParseError as e:results.append({'file':p.name,'status':'report-error','message':str(e)})
summary={'catalogueRecords':len(cases),'catalogueStatus':dict(collections.Counter(c['status'] for c in cases)),'junit':dict(collections.Counter(c['status'] for c in results)),'reportsPresent':bool(results),'importOutcome':os.getenv('IMPORT_OUTCOME','not recorded'),'javaOutcome':os.getenv('JAVA_OUTCOME','not recorded'),'exhaustivePortComplete':False}
(R/'verification.json').write_text(json.dumps(summary,indent=2)+'\n')
(R/'junit-results.json').write_text(json.dumps(results,indent=2)+'\n')
(R/'VERIFICATION.md').write_text('# Recorded verification\n\n```json\n'+json.dumps(summary,indent=2)+'\n```\n\nInventory-only native tests are not counted as ported. Missing reports mean unverified, not passed. Failures remain failures; expected results are never regenerated from RDF4J. See the JSON/XML files for exact cases and errors.\n')
print(json.dumps(summary))
