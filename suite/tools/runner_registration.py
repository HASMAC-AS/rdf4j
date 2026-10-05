#!/usr/bin/env python3
"""Prove reachability from statically resolvable Jena runner roots; do not guess others."""
from __future__ import annotations
import collections,json,re
from pathlib import Path
import import_corpus as c
import native_adapters as native

def main():
    root=c.VENDOR/'jena';constants={};roots=[];unresolved=[]
    const_file=root/'jena-arq/src/test/java/org/apache/jena/arq/TestConsts.java'
    text=native.mask_source(const_file.read_text(),False)
    defs=re.findall(r'static\s+final\s+String\s+(\w+)\s*=\s*([^;]+);',text)
    for _ in range(len(defs)+1):
        changed=False
        for name,expr in defs:
            if name in constants:continue
            try:constants[name]=native.string_value(expr,constants);constants['TestConsts.'+name]=constants[name];changed=True
            except ValueError:pass
        if not changed:break
    for file in c.files(root):
        if file.suffix!='.java' or '/src/test/' not in file.as_posix():continue
        text=file.read_text(errors='replace')
        if 'SparqlTests' not in text and 'org.apache.jena.arq.junit.Scripts' not in text:continue
        clean=native.mask_source(text,False)
        module=Path(str(file).split('/src/',1)[0])
        for start,end,name,args in native.calls(text,{'all'}):
            for argument in c.split_args(args):
                if '.ttl' not in argument and 'TestConsts.' not in argument:continue
                argument=native.mask_source(argument,False).strip()
                try:value=native.string_value(argument,constants)
                except ValueError as e:
                    unresolved.append({'source':c.source('jena',file,text.count('\n',0,start)+1),'argument':argument,'reason':str(e)});continue
                target=(module/value).resolve()
                if not target.is_relative_to(root.resolve()):
                    unresolved.append({'source':c.source('jena',file),'argument':argument,'reason':'root outside pinned source tree'});continue
                roots.append({'source':c.source('jena',file,text.count('\n',0,start)+1),'manifest':c.url('jena',target),'present':target.is_file()})
    discovery=json.loads((c.OUT/'discovery.json').read_text());edges=collections.defaultdict(set)
    for include in discovery.get('manifestIncludes',[]):edges[include['from']].add(include['to'])
    reached=set();stack=[r['manifest'] for r in roots if r['present']]
    while stack:
        item=stack.pop()
        if item in reached:continue
        reached.add(item);stack.extend(sorted(edges[item]-reached))
    cases=json.loads((c.OUT/'cases.json').read_text());groups=collections.Counter();querygroups=collections.Counter()
    for case in cases:
        path=case['source']['path'];project=case['project']
        if project=='qlever':state='qlever-source-case'
        elif case.get('sourceAssertion'):state='native-assertion-adapter'
        elif '.zip.expanded/' in path:state='historical-archive-copy'
        elif c.base('jena')+path in reached and case.get('listedInManifestEntries'):state='reachable-from-resolved-runner-root'
        else:state='not-proven-registered-by-static-audit'
        groups[state]+=1
        if case['kind']=='query':querygroups[state]+=1
    result={'resolvedRunnerRoots':roots,'unresolvedRunnerExpressions':unresolved,'reachableManifests':sorted(reached),'catalogueRegistrationGroups':dict(groups),'queryRegistrationGroups':dict(querygroups),'fullyReconciledWithRuntimeRunner':False,'note':'Positive reachability evidence only. No claim that unresolved/unregistered entries are absent from other upstream runners. Archived copies are not additional unique semantic scenarios.'}
    (c.OUT/'runner-registration.json').write_text(json.dumps(result,indent=2)+'\n')
    (c.OUT/'registration-summary.json').write_text(json.dumps({k:v for k,v in result.items() if k not in {'resolvedRunnerRoots','unresolvedRunnerExpressions','reachableManifests'}},indent=2)+'\n')
    print(json.dumps({'resolvedRunnerRoots':len(roots),'unresolvedExpressions':len(unresolved),'reachableManifests':len(reached),'catalogueRegistrationGroups':dict(groups)}))

if __name__=='__main__':main()
