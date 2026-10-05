#!/usr/bin/env python3
"""Apply final oracle policies to every imported cohort and regenerate documentation."""
from __future__ import annotations
import collections, importlib.util, json
from pathlib import Path
from order_policy import apply
ROOT=Path(__file__).resolve().parents[1]

def main():
    spec=importlib.util.spec_from_file_location('corpus_importer',Path(__file__).with_name('import.py'))
    importer=importlib.util.module_from_spec(spec);spec.loader.exec_module(importer)
    cases=json.loads((ROOT/'corpus/cases.json').read_text())
    for case in cases:apply(case,importer.block)
    (ROOT/'corpus/cases.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n')
    stats=json.loads((ROOT/'corpus/coverage.json').read_text())
    stats.update({'cases':len(cases),'status':dict(collections.Counter(c['status'] for c in cases)),
                  'families':dict(collections.Counter(c['family'] for c in cases)),
                  'orderCheckedCases':sum('orderBy' in c for c in cases),
                  'orderBlockedCases':sum(any('ORDER BY' in x and ('requires' in x or 'needs' in x or 'unavailable' in x) for x in c.get('limitations',[])) and c['status']=='blocked' for c in cases)})
    (ROOT/'corpus/coverage.json').write_text(json.dumps(stats,indent=2)+'\n')
    importer.cases=cases;importer.document()
    print('FINAL_CORPUS_POLICY '+json.dumps(stats),flush=True)
if __name__=='__main__':main()
