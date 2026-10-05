#!/usr/bin/env python3
"""Verify generated case identities, retained source/fixture hashes and documentation coverage."""
from __future__ import annotations
import hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]


def verify(root=ROOT):
    root=root.resolve();corpus=root/'corpus';records=json.loads((corpus/'cases.json').read_text())
    pins=json.loads((root/'sources.json').read_text());by_repo={x['repository']:x['revision']for x in pins.values()}
    ids=[c['id']for c in records];id_set=set(ids);errors=[];cache={};checked=set();assets=0
    if len(ids)!=len(id_set):errors.append('Duplicate catalogue IDs')
    def digest(path):
        if path not in cache:cache[path]=hashlib.sha256(path.read_bytes()).hexdigest()
        return cache[path]
    def asset(node,case_id):
        nonlocal assets
        if isinstance(node,list):
            for value in node:asset(value,case_id)
        elif isinstance(node,dict):
            if 'path'in node and 'sha256'in node:
                file=(root/node['path']).resolve()
                if not file.is_relative_to(root):errors.append(case_id+': asset escapes root')
                elif not file.is_file():errors.append(case_id+': missing asset '+node['path'])
                elif digest(file)!=node['sha256']:errors.append(case_id+': asset hash mismatch '+node['path'])
                else:checked.add(file);assets+=1
            for key,value in node.items():
                if key not in {'source','sourceFixture','sourceLoadingScript'}:asset(value,case_id)
    for c in records:
        identifier=c['id'];query=c.get('query','')
        if hashlib.sha256(query.encode('utf-8')).hexdigest()!=c.get('querySha256'):errors.append(identifier+': query digest mismatch')
        if c.get('status')=='ready' and(not query or not c.get('expected')):errors.append(identifier+': ready case has no query/oracle')
        source=c.get('source',{})
        if by_repo.get(source.get('repository'))!=source.get('revision'):errors.append(identifier+': source pin mismatch')
        project=next((key for key,pin in pins.items()if pin['repository']==source.get('repository')),None)
        if project and source.get('path'):
            file=(root/'vendor'/project/source['path']).resolve()
            if not file.is_relative_to(root/'vendor'/project):errors.append(identifier+': source path escapes vendor tree')
            elif not file.is_file():errors.append(identifier+': retained source missing '+source['path'])
            elif source.get('sha256')and digest(file)!=source['sha256']:errors.append(identifier+': source SHA mismatch')
        doc=corpus/'cases'/(identifier+'.md')
        if not doc.is_file():errors.append(identifier+': no case documentation')
        else:
            text=doc.read_text()
            for required in ['## Provenance','## Prerequisites','## Query','## Expected results']:
                if required not in text:errors.append(identifier+': missing documentation section '+required)
            if query not in text:errors.append(identifier+': documentation query differs')
        asset(c.get('fixtures',[]),identifier);asset(c.get('expected',{}),identifier);asset(c.get('queryAsset',{}),identifier)
    extra=sorted(p.stem for p in(corpus/'cases').glob('*.md')if p.stem not in id_set)
    if extra:errors.append('Stale case documents: '+str(len(extra)))
    report={'catalogueRecords':len(records),'uniqueIds':len(id_set),'documentsChecked':len(records),
      'assetReferencesChecked':assets,'distinctFilesHashed':len(cache),'distinctReferencedAssets':len(checked),
      'errors':errors,'passed':not errors,'coverageCompletenessProven':False}
    (root/'reports').mkdir(exist_ok=True)
    (root/'reports/corpus-integrity.json').write_text(json.dumps(report,indent=2)+'\n')
    print('CORPUS_INTEGRITY '+json.dumps(report),flush=True)
    if errors:raise RuntimeError('Corpus integrity errors; see reports/corpus-integrity.json')
    return report

if __name__=='__main__':verify()
