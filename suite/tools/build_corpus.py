#!/usr/bin/env python3
"""Complete import pipeline with reviewed native-source adapters and source audit."""
from __future__ import annotations
import argparse,collections,json,re
import import_corpus as c
import native_adapters as native
import reviewed_cases

def normalize_registered_prefixes(case):
    expr=case.get('decodedExpression')
    if expr is None:return
    used=c.mask(expr);removed=[];lines=[]
    for line in case['query'].splitlines(keepends=True):
        match=re.match(r'PREFIX\s+([^\s:]*):\s*<',line,re.I)
        if match and match.group(1).endswith('.') and match.group(1)+':' not in used:
            removed.append(match.group(1));continue
        lines.append(line)
    if removed:
        case['query']=''.join(lines)
        case['notes'].append('Unused programmatic Jena prefix labels omitted from the textual wrapper because they are not legal SPARQL prefix declarations: '+', '.join(removed))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--download',action='store_true');args=parser.parse_args()
    c.OUT.mkdir(exist_ok=True)
    if args.download:c.download()
    for project in c.SOURCES:
        if not (c.VENDOR/project).exists():raise FileNotFoundError('Missing source tree; use --download: '+project)
    c.unpack()
    for project in c.SOURCES:
        for p in c.files(c.VENDOR/project):
            if p.suffix.lower() in {'.ttl','.n3'}:c.read_manifest(project,p)
    native.discover_native(c)
    c.jena_native();c.qlever_native();c.qlever_yaml()
    native.jena_expressions(c);native.optional_joins(c);native.fix_exists_queries(c);reviewed_cases.add(c);native.annotate_requirements(c)
    for case in c.CASES:normalize_registered_prefixes(case)
    c.CASES.sort(key=lambda x:(x['project'],x['source']['path'],x['name'],x['id']))
    ids=[x['id'] for x in c.CASES]
    if len(ids)!=len(set(ids)):raise RuntimeError('Duplicate stable case identifiers')
    for name,obj in [('cases',c.CASES),('assets',c.ASSETS),('native-inventory',c.NATIVE)]:c.dump(c.OUT/(name+'.json'),obj)
    summary={'records':len(c.CASES),'status':dict(collections.Counter(x['status'] for x in c.CASES)),'kinds':dict(collections.Counter(x['kind'] for x in c.CASES)),'projects':dict(collections.Counter(x['project'] for x in c.CASES)),'nativeDeclarations':len(c.NATIVE),'nativeQueryCandidates':sum(x['queryCandidate'] for x in c.NATIVE),'nativeDeclarationsWithAdapters':sum(bool(x['portedCases']) for x in c.NATIVE),'manifestIncludes':c.INCLUDES,'diagnostics':c.DIAG,'archives':c.ARCHIVES,'nonQueryManifestKinds':dict(c.SKIPPED_TYPES),'exhaustivePortComplete':False,'upstreamRunnerRegistrationReconciled':False}
    c.dump(c.OUT/'discovery.json',summary);c.dump(c.OUT/'native-audit.json',native.audit(c));c.documentation()
    print(json.dumps({k:v for k,v in summary.items() if k not in {'manifestIncludes','diagnostics','archives','nonQueryManifestKinds'}},indent=2))

if __name__=='__main__':main()
