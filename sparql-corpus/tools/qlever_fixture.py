#!/usr/bin/env python3
"""Preserve QLever's explicit Turtle fixture syntax despite its .nt source extension."""
from __future__ import annotations
import copy, hashlib, importlib.util, json, shutil
from pathlib import Path
from urllib.parse import urljoin, urlsplit

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'corpus'
BASE='https://qlever-corpus.invalid/'
RUNNER='e2e/e2e.sh'
RUNNER_BLOB='fdb8b8863ee923fbf0bb4150643e512e11b71b6d'


def map_iri_cell(value):
    if isinstance(value,str) and value.startswith('<') and value.endswith('>'):
        iri=value[1:-1]
        return '<'+(iri if urlsplit(iri).scheme else urljoin(BASE,iri))+'>'
    return value


def adapt_checks(checks):
    result=copy.deepcopy(checks)
    for check in result:
        if 'contains_row'in check:check['contains_row']=[map_iri_cell(v)for v in check['contains_row']]
        if 'res'in check:check['res']=[[map_iri_cell(v)for v in row]for row in check['res']]
    return result


def main():
    runner=ROOT/'vendor/qlever'/RUNNER;data=runner.read_bytes()
    blob=hashlib.sha1(('blob '+str(len(data))+'\0').encode()+data).hexdigest()
    if blob!=RUNNER_BLOB or b'-F ttl'not in data:raise RuntimeError('QLever source loading contract changed; review the fixture adapter')
    cases=json.loads((OUT/'cases.json').read_text());target=OUT/'assets/qlever-scientists.ttl'
    target.parent.mkdir(parents=True,exist_ok=True);converted=0;original=None
    for case in cases:
        if case.get('family')!='qlever-yaml':continue
        for fixture in case['fixtures']:
            source=fixture.get('sourceFixture',fixture)
            if not str(source.get('path','')).endswith('/scientists.nt'):continue
            path=ROOT/source['path'];raw=path.read_bytes()
            if hashlib.sha256(raw).hexdigest()!=source['sha256']:raise RuntimeError('Scientists fixture changed')
            if original is None:
                shutil.copyfile(path,target);original=copy.deepcopy(source)
            elif original['sha256']!=source['sha256']:raise RuntimeError('Ambiguous scientists dataset source')
            fixture['sourceFixture']=copy.deepcopy(source)
            fixture.update(path=target.relative_to(ROOT).as_posix(),base=BASE,sha256=source['sha256'])
            fixture['sourceSyntax']='Turtle, selected by upstream qlever-index -F ttl'
            fixture['sourceLoadingScript']={'path':RUNNER,'gitBlobSha1':RUNNER_BLOB,'repository':'HASMAC-AS/qlever','revision':'a815b3b71a6c764d927332dcd5505fe6436ad214'}
        case['base']=BASE
        expected=case['expected']
        expected.setdefault('sourceChecks',copy.deepcopy(expected.get('checks',[])))
        expected['checks']=adapt_checks(expected['sourceChecks'])
        notes=[
            'The scientists.nt source is loaded as Turtle by upstream e2e.sh (-F ttl). Its unchanged bytes are copied to an explicit .ttl asset for the RDF4J Rio loader.',
            'QLever accepts relative IRIs in this fixture. The repository-level adaptation resolves fixture, query and expected IRI cells against the same fixed https://qlever-corpus.invalid/ base. Original checks remain in expected.sourceChecks.',
            'Relative-IRI spelling observed via STR or other string functions may differ after rebasing; such differences are adaptation-policy issues rather than automatically RDF4J defects.'
        ]
        case['limitations']=[n for n in case.get('limitations',[])if n not in notes]+notes
        converted+=1
    (OUT/'cases.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n')
    report={'casesAdapted':converted,'sourceFixture':original,'derivedAsset':target.relative_to(ROOT).as_posix(),
            'derivedBytesUnchanged':original is not None and hashlib.sha256(target.read_bytes()).hexdigest()==original['sha256'],
            'sourceRunnerBlob':RUNNER_BLOB,'base':BASE}
    (OUT/'qlever-fixture-summary.json').write_text(json.dumps(report,indent=2)+'\n')
    spec=importlib.util.spec_from_file_location('base_import',ROOT/'tools/import.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
    base.cases=cases;base.document()
    print('QLEVER_FIXTURE_ADAPTATION '+json.dumps(report),flush=True)

if __name__=='__main__':main()
