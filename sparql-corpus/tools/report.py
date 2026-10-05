#!/usr/bin/env python3
"""Record actual outcomes, never equate packaging success with test success."""
import collections, json, os, xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
r=ROOT/'reports'; r.mkdir(exist_ok=True)
cases=json.loads((ROOT/'corpus/cases.json').read_text()) if (ROOT/'corpus/cases.json').exists() else []
status=collections.Counter(c.get('status','unknown') for c in cases)
results=[]
for p in sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml')):
    raw=p.read_bytes(); (r/p.name).write_bytes(raw)
    try:
        root=ET.fromstring(raw)
        for tc in root.iter('testcase'):
            error=tc.find('error'); failure=tc.find('failure'); skip=tc.find('skipped')
            state='error' if error is not None else 'failed' if failure is not None else 'skipped' if skip is not None else 'passed'
            el=error if error is not None else failure if failure is not None else skip
            results.append({'class':tc.get('classname'),'name':tc.get('name'),'seconds':tc.get('time'),'status':state,'message':el.get('message','') if el is not None else '', 'detail':el.text if el is not None else None})
    except ET.ParseError as e: results.append({'file':p.name,'status':'report-parse-error','message':str(e)})
counts=collections.Counter(x['status'] for x in results)
summary={'catalogueRecords':len(cases),'catalogueStatus':dict(status),'junitCounts':dict(counts),'junitResultsPresent':bool(results),'importOutcome':os.getenv('IMPORT_OUTCOME','not recorded'),'javaOutcome':os.getenv('JAVA_OUTCOME','not recorded'),'coverageComplete':False,'nativeInventoryIsNotPortedCoverage':True}
(r/'verification.json').write_text(json.dumps(summary,indent=2)+'\n')
(r/'junit-results.json').write_text(json.dumps(results,indent=2)+'\n')
(r/'VERIFICATION.md').write_text('# Verification\n\n'+json.dumps(summary,indent=2)+'\n\nThe XML and JSON reports record actual execution. Missing reports mean unverified, not passed. Remaining native tests and external suites are not claimed as ported. All reported failures remain failures; no expected result has been rewritten from RDF4J output.\n')
print(json.dumps(summary))
