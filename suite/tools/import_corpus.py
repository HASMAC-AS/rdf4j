#!/usr/bin/env python3
"""Materialize fork tests and retain exact expected results; never ask the DUT for an oracle."""
from __future__ import annotations
import argparse, collections, hashlib, html, json, os, re, subprocess, zipfile
from pathlib import Path
from urllib.parse import unquote, urlparse
import rdflib
from rdflib import Graph, URIRef, BNode, Literal, Namespace
from rdflib.namespace import RDF, RDFS, XSD
import yaml
rdflib.NORMALIZE_LITERALS=False
ROOT=Path(__file__).resolve().parents[1]
VENDOR=ROOT/'vendor'; OUT=ROOT/'corpus'
SOURCES=json.loads((ROOT/'sources.json').read_text())
MF=Namespace('http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#')
QT=Namespace('http://www.w3.org/2001/sw/DataAccess/tests/test-query#')
ASSETS={}; CASES=[]; NATIVE=[]; DIAG=[]; INCLUDES=[]; ARCHIVES={}; SKIPPED_TYPES=collections.Counter()

def sha(data):return hashlib.sha256(data).hexdigest()
def dump(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(obj,ensure_ascii=False,indent=2,sort_keys=True)+'\n',encoding='utf8')
def files(root):
    for d,dirs,names in os.walk(root):
        dirs[:]=sorted(x for x in dirs if x not in {'.git','target','node_modules','.venv','__pycache__'})
        for n in sorted(names):
            p=Path(d)/n
            if p.is_file() and not p.is_symlink():yield p

def base(project):
    s=SOURCES[project];return f"https://raw.githubusercontent.com/{s['repository']}/{s['revision']}/"
def url(project,p):return base(project)+p.relative_to(VENDOR/project).as_posix()
def local(project,uri):
    u=str(uri)
    if not u.startswith(base(project)):return None
    tail=unquote(u[len(base(project)):].split('#')[0])
    p=(VENDOR/project/tail).resolve()
    return p if p.is_relative_to((VENDOR/project).resolve()) else None

def source(project,p,line=None):
    rel=p.relative_to(VENDOR/project).as_posix();s=SOURCES[project]
    src={'repository':s['repository'],'revision':s['revision'],'path':rel,'url':f"https://github.com/{s['repository']}/blob/{s['revision']}/{rel}"}
    if line:src['line']=line;src['url']+=f'#L{line}'
    for prefix,meta in ARCHIVES.items():
        if p.is_relative_to(Path(prefix)):
            src['archive']=meta;src['member']=p.relative_to(Path(prefix)).as_posix()
            src['url']=f"https://github.com/{s['repository']}/blob/{s['revision']}/{meta}"
            break
    return src

def asset(project,p,uri=None):
    if p is None or not p.is_file():return None
    rel=p.relative_to(ROOT).as_posix()
    if rel not in ASSETS:
        ASSETS[rel]={'path':rel,'uri':uri or url(project,p),'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size}
    return dict(ASSETS[rel],uri=uri or ASSETS[rel]['uri'])

def issue(where,message):DIAG.append({'source':str(where),'message':str(message)})
def newcase(project,p,name,identity,query='',kind='query'):
    return {'id':project+'-'+sha(identity.encode())[:20], 'name':name,'project':project,'source':source(project,p), 'kind':kind,'query':query,'base':url(project,p),'defaults':[],'named':[],'requires':[],'notes':[],'status':'ready','blockedReason':'','expected':None,'ordered':None,'lax':False}
def block(c,reason):
    c['status']='blocked';c['blockedReason']='; '.join(filter(None,[c.get('blockedReason'),reason]))
def literal_node(value):
    if isinstance(value,URIRef):return {'type':'iri','value':str(value)}
    if isinstance(value,BNode):return {'type':'bnode','value':str(value)}
    if isinstance(value,Literal):
        t={'type':'literal','value':str(value),'datatype':str(value.datatype or (RDF.langString if value.language else XSD.string))}
        if value.language:t['language']=value.language
        return t
    raise ValueError('Unsupported RDF term '+repr(value))
def inline(vars,rows):return {'type':'tuple','vars':vars,'rows':rows}
def boolterm(b):return {'type':'literal','value':str(b).lower(),'datatype':str(XSD.boolean)}

def download():
    VENDOR.mkdir(exist_ok=True)
    for project,s in SOURCES.items():
        dest=VENDOR/project
        if not (dest/'.git').exists():
            dest.mkdir(exist_ok=True)
            subprocess.run(['git','init','-q',str(dest)],check=True)
            subprocess.run(['git','-C',str(dest),'fetch','--depth=1','https://github.com/'+s['repository']+'.git',s['revision']],check=True)
            subprocess.run(['git','-C',str(dest),'checkout','--detach','-q','FETCH_HEAD'],check=True)
        actual=subprocess.check_output(['git','-C',str(dest),'rev-parse','HEAD'],text=True).strip()
        if actual!=s['revision']:raise RuntimeError(f'{project}: expected {s["revision"]}, found {actual}')
        tracked=subprocess.check_output(['git','-C',str(dest),'ls-files','--stage','-z']).split(b'\0')
        entries=[]
        for entry in tracked:
            if not entry:continue
            head,path=entry.split(b'\t',1); mode,digest,stage=head.decode().split(); name=path.decode('utf8')
            p=dest/name
            if p.is_file() and not p.is_symlink():
                data=p.read_bytes();gitsha=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
                if gitsha!=digest:raise RuntimeError('Git blob integrity failure: '+str(p))
                entries.append({'path':name,'gitBlob':digest,'sha256':sha(data),'bytes':len(data),'mode':mode})
        dump(OUT/(project+'-source-inventory.json'),entries)

def unpack():
    # Archived manifests are real upstream fixtures, not remotely reconstructed copies.
    for project in SOURCES:
        for p in list(files(VENDOR/project)):
            if p.suffix.lower()!='.zip':continue
            try:
                with zipfile.ZipFile(p) as z:
                    useful=any('manifest' in Path(n).name.lower() and n.endswith(('.ttl','.n3')) for n in z.namelist())
                    if project=='qlever' and p.name=='scientist-collection.zip':useful=True
                    if not useful:continue
                    dest=p.parent/(p.name+'.expanded');dest.mkdir(exist_ok=True)
                    ARCHIVES[str(dest.resolve())]=p.relative_to(VENDOR/project).as_posix()
                    for info in z.infolist():
                        target=(dest/info.filename).resolve()
                        if not target.is_relative_to(dest.resolve()):raise ValueError('Unsafe archive path '+info.filename)
                        if info.is_dir():target.mkdir(parents=True,exist_ok=True);continue
                        if info.file_size>1024**3:raise ValueError('Oversized archive entry '+info.filename)
                        target.parent.mkdir(parents=True,exist_ok=True)
                        if not target.exists():target.write_bytes(z.read(info))
            except Exception as e:issue(p,e)

def mask(text):
    return re.sub(r'"""[\s\S]*?"""|\'\'\'[\s\S]*?\'\'\'|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|<[^>\n]*>|#[^\n]*', ' ', text)
def feature_gates(c):
    q=mask(c['query'])
    if re.search(r'\bSERVICE\b',q,re.I):
        c['requires'].append('controlled SERVICE endpoint');block(c,'SERVICE endpoint fixture not configured; no external network execution')
    if c['kind']=='query' and re.search(r'\bDESCRIBE\b',q,re.I):
        c['requires'].append('upstream DESCRIBE policy');block(c,'DESCRIBE graph selection is implementation-dependent')
    if c['kind']=='query' and (re.search(r'\b(?:CALL|EBV|LATERAL|UNNEST)\s*\(',q,re.I) or 'ql:' in q):
        c['requires'].append('engine extension');block(c,'Engine-specific expression/index extension needs a repository capability adapter')

def read_manifest(project,p):
    text=p.read_text(encoding='utf8',errors='replace')
    if not any(t in text for t in ['test-manifest#','mf:entries','mf:Manifest','mfx:TestQuery']):return
    g=Graph()
    try:g.parse(data=text,publicID=url(project,p),format='turtle')
    except Exception as e:issue(p,'Manifest parse failure: '+str(e));return
    entries=[];seen=set()
    for subject,head in sorted(g.subject_objects(MF.entries),key=lambda t:str(t[0])):
        try:
            for node in g.items(head):
                if node not in seen:entries.append(node);seen.add(node)
        except Exception as e:issue(p,'Malformed entries list: '+str(e))
    for subj,head in g.subject_objects(MF.include):
        try:
            for child in g.items(head):INCLUDES.append({'from':url(project,p),'to':str(child),'present':bool(local(project,child) and local(project,child).exists())})
        except Exception as e:issue(p,'Malformed include list: '+str(e))
    # Some native manifests contain typed but unlisted entries. Retain registration status.
    fallback=sorted(set(g.subjects(MF.action,None))-seen,key=lambda n:(str(g.value(n,MF.name)),str(g.value(n,MF.action))))
    for ordinal,node in enumerate(entries+fallback):
        types=sorted(str(t) for t in g.objects(node,RDF.type)); joined=' '.join(types)
        action=g.value(node,MF.action)
        querynode=g.value(action,QT.query) if isinstance(action,(BNode,URIRef)) else None
        name=str(g.value(node,MF.name) or g.value(node,RDFS.label) or str(node).split('#')[-1])
        syntax=('Syntax' in joined or 'TestSyntax' in joined)
        query_candidate=querynode is not None or 'QueryEvaluationTest' in joined or ('Syntax' in joined and 'Update' not in joined and str(action).lower().endswith(('.rq','.sparql')))
        if not query_candidate:
            SKIPPED_TYPES[joined or 'untyped']+=1;continue
        if querynode is None:querynode=action
        kind=('negative-syntax' if 'Negative' in joined else 'positive-syntax') if syntax else 'query'
        identity=f'{project}/{p.relative_to(VENDOR/project)}::{ordinal}::{name}'
        c=newcase(project,p,name,identity,kind=kind)
        c['manifestEntry']=str(node) if isinstance(node,URIRef) else f'blank-entry-{ordinal}'
        c['listedInManifestEntries']=node in seen;c['types']=types
        c['comment']=str(g.value(node,RDFS.comment) or '')
        qp=local(project,querynode)
        qa=asset(project,qp,str(querynode))
        if qa:
            c['query']=qp.read_text(encoding='utf8',errors='strict');c['queryAsset']=qa;c['base']=qa['uri']
        else:block(c,'Missing/nonlocal query source: '+str(querynode))
        for data in sorted(g.objects(action,QT.data),key=str):
            a=asset(project,local(project,data),str(data))
            if a:c['defaults'].append(a)
            else:block(c,'Missing/nonlocal default graph: '+str(data))
        for graphdata in sorted(g.objects(action,QT.graphData),key=str):
            if isinstance(graphdata,URIRef):data=graphdata; graphname=graphdata
            else:
                data=g.value(graphdata,QT.data) or g.value(graphdata,QT.graph)
                graphname=g.value(graphdata,RDFS.label) or g.value(graphdata,QT.graphName) or g.value(graphdata,QT.graph) or data
            a=asset(project,local(project,data),str(data)) if data else None
            if a and graphname:c['named'].append({'graph':str(graphname),'asset':a})
            else:block(c,'Missing/unsupported named graph fixture: '+str(graphdata))
        result=g.value(node,MF.result)
        if isinstance(result,Literal) and result.datatype==XSD.boolean:c['expected']={'type':'boolean','value':bool(result.toPython())}
        elif result is not None:
            a=asset(project,local(project,result),str(result))
            if a:
                c['expected']=a
                if Path(a['path']).suffix.lower() in {'.csv','.srj.gz','.srx.gz'}:block(c,'Lossy/compressed result format needs an explicit comparator')
            else:block(c,'Missing/nonlocal expected result: '+str(result))
        if kind=='query' and c['expected'] is None:block(c,'No materialized upstream expected-result oracle')
        c['lax']=g.value(node,MF.resultCardinality)==MF.LaxCardinality
        for pred in [MF.requires,MF.feature]:c['requires']+=sorted(str(v) for v in g.objects(node,pred))
        regimes=[str(o) for s,pred,o in g.triples((None,None,None)) if s in {node,action} and 'entailment' in str(pred).lower()]
        if regimes:c['requires']+=regimes;block(c,'Entailment regime requires an explicit inference repository')
        feature_gates(c);CASES.append(c)

def end_block(text,start):
    # Lexical balancing ignores strings/comments, including C++ raw string literals.
    depth=0;i=start
    while i<len(text):
        raw=re.match(r'R"([^ ()\\\t\r\n]{0,16})\(',text[i:])
        if raw:
            close=')'+raw.group(1)+'"';j=text.find(close,i+raw.end());i=len(text) if j<0 else j+len(close);continue
        if text.startswith('//',i):
            j=text.find('\n',i);i=len(text) if j<0 else j+1;continue
        if text.startswith('/*',i):
            j=text.find('*/',i+2);i=len(text) if j<0 else j+2;continue
        if text.startswith('"""',i):
            j=text.find('"""',i+3);i=len(text) if j<0 else j+3;continue
        if text[i] in '\"\'':
            quote=text[i];i+=1
            while i<len(text):
                if text[i]=='\\':i+=2;continue
                if text[i]==quote:i+=1;break
                i+=1
            continue
        if text[i]=='{':depth+=1
        elif text[i]=='}':
            depth-=1
            if depth==0:return i+1
        i+=1
    return len(text)

def native_inventory():
    for project in SOURCES:
        for p in files(VENDOR/project):
            if p.suffix not in {'.java','.cpp','.cc'} or not any('test' in part.lower() for part in p.parts):continue
            text=p.read_text(encoding='utf8',errors='replace')
            if p.suffix=='.java':
                pat=r'@(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b[\s\S]{0,1200}?\b(?:public\s+|protected\s+|private\s+)?(?:static\s+)?(?:void|Stream\s*<[^>]+>|Collection\s*<[^>]+>)\s+(\w+)\s*\([^)]*\)[^{;]*\{'
            else:pat=r'\b(?:TEST|TEST_F|TEST_P|TYPED_TEST|TYPED_TEST_P)\s*\(\s*(\w+)\s*,\s*(\w+)\s*\)\s*\{'
            for m in re.finditer(pat,text):
                start=text.rfind('{',m.start(),m.end());end=end_block(text,start)
                name=p.stem+'.'+m.group(1) if p.suffix=='.java' else m.group(1)+'.'+m.group(2)
                snippet=text[m.start():end];line=text.count('\n',0,m.start())+1
                relevant=bool(re.search(r'(?i)sparql|QueryExecution|ExprUtils|SELECT\s|ASK\s|CONSTRUCT\s|testExists|testBoolean|testNumeric|eval\(',snippet))
                NATIVE.append({'id':project+'-native-'+sha((str(p.relative_to(VENDOR/project))+':'+str(line)+':'+name).encode())[:20], 'name':name,'project':project,'source':source(project,p,line),'sourceEndLine':text.count('\n',0,end)+1,'sourceCode':snippet,'queryCandidate':relevant,'portedCases':[],'parameterExpansion':'not expanded' if re.search(r'ParameterizedTest|RepeatedTest|TEST_P|TYPED_TEST',m.group()) else 'not parameterized','disposition':'manual-review-required' if relevant else 'native-unit-test-not-classified-as-query'})

STRING=r'"(?:\\.|[^"\\])*"'
def jstring(s):return json.loads(s)
def term_from_sparql(s):
    g=Graph();g.parse(data='@prefix xsd: <'+str(XSD)+'> . @prefix rdf: <'+str(RDF)+'> . <urn:s> <urn:p> '+s+' .',format='turtle')
    return literal_node(next(g.objects(URIRef('urn:s'),URIRef('urn:p'))))

def add_native_case(n,query,expected,suffix='',kind='query',baseuri='https://example.org/native/'):
    s=n['source'];p=VENDOR/n['project']/s['path']
    c=newcase(n['project'],p,n['name']+suffix,n['id']+suffix,query,kind)
    c['source']=s;c['sourceEndLine']=n['sourceEndLine'];c['sourceAssertion']=n['sourceCode'];c['expected']=expected;c['base']=baseuri
    c['notes'].append('Query-level adaptation of the original native assertion; engine-internal representation and execution strategy are not asserted.')
    feature_gates(c);CASES.append(c);n['portedCases'].append(c['id']);n['disposition']='query-assertion-adapted'
    return c

def jena_native():
    prefix='PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPREFIX fn: <http://www.w3.org/2005/xpath-functions#>\n'
    for n in NATIVE:
        if n['project']!='jena':continue
        code=n['sourceCode'];filename=Path(n['source']['path']).name
        try:
            if filename=='TestExpressions2.java':
                calls=list(re.finditer(r'\beval\(\s*('+STRING+r')\s*(?:,\s*(true|false))?\s*\)',code))
                if len(calls)==1:
                    expr=jstring(calls[0].group(1));value=calls[0].group(2)!='false'
                    error='assertThrows(ExprEvalException.class' in re.sub(r'\s+','',code)
                    # Match whitespace-insensitively without assuming an expected boolean on error paths.
                    error=bool(re.search(r'assertThrows\s*\(\s*ExprEvalException\.class',code))
                    negative=bool(re.search(r'assertThrows\s*\(\s*QueryParseException\.class',code))
                    query=prefix+'SELECT ?result WHERE { BIND(IF(('+expr+'), true, false) AS ?result) }'
                    c=add_native_case(n,query,inline(['result'],[[None if error else boolterm(value)]]),kind='negative-syntax' if negative else 'query')
                    c['notes'].append('Jena eval helper asserts effective boolean value, retained via IF. Expression exceptions are one unbound output cell, not zero rows.')
                else:
                    m=re.search(r'\bevalTest\(\s*('+STRING+r')\s*,\s*('+STRING+r')\s*,\s*('+STRING+r')\s*\)',code)
                    if m:
                        bu,expr,result=map(jstring,m.groups())
                        add_native_case(n,prefix+'SELECT ?result WHERE { BIND(('+expr+') AS ?result) }',inline(['result'],[[term_from_sparql(result)]]),baseuri=bu)
            elif filename=='TestExpressions3.java':
                m=re.search(r'\b(eval|evalExpr)\(\s*('+STRING+r')\s*,\s*('+STRING+r')\s*,\s*(true|false)\s*\)',code)
                if not m:continue
                helper,exprs,binds,b=m.groups();expr=jstring(exprs);bindings=jstring(binds)
                pairs=re.findall(r'\(\s*(\?\w+)\s+([^()]+)\)',bindings)
                values=' '.join('VALUES '+var+' { '+value.strip()+' }' for var,value in pairs)
                if helper=='evalExpr':
                    inner=re.fullmatch(r'\(bound\s+(.+)\)',expr).group(1)
                    plus=re.fullmatch(r'\(\+\s+(\?\w+)\s+(\d+)\)',inner)
                    if plus:inner=plus.group(1)+' + '+plus.group(2)
                    expr='BOUND(?__native_bound)';values+=' BIND(('+inner+') AS ?__native_bound)'
                add_native_case(n,prefix+'SELECT ?result WHERE { '+values+' BIND(('+expr+') AS ?result) }',inline(['result'],[[boolterm(b=='true')]]))
        except Exception as e:issue(n['source']['url'],'Native adapter rejected expression: '+str(e))

def split_args(s):
    result=[];depth=0;start=0;quote=None;i=0
    while i<len(s):
        ch=s[i]
        if quote:
            if ch=='\\':i+=2;continue
            if ch==quote:quote=None
        elif ch in '\"\'':quote=ch
        elif ch in '({[':depth+=1
        elif ch in ')}]':depth-=1
        elif ch==',' and depth==0:result.append(s[start:i].strip());start=i+1
        i+=1
    result.append(s[start:].strip());return result

def cpp_calls(body,name):
    for m in re.finditer(r'\b'+name+r'\s*\(',body):
        start=m.end();depth=1;i=start
        while i<len(body) and depth:
            if body[i]=='(':depth+=1
            elif body[i]==')':depth-=1
            i+=1
        yield split_args(body[start:i-1])
def cpp_table(s):
    s=s.strip()
    if s.startswith('IdTable'):return []
    if s.startswith('makeIdTableFromVector('):s=s[len('makeIdTableFromVector('):-1]
    s=s.replace('{','[').replace('}',']');s=re.sub(r'\bU\b','null',s)
    return json.loads(s)
def qid(x):return None if x is None else {'type':'iri','value':'urn:qlever:native-id:'+str(x)}
def values_clause(vars,rows):
    return 'VALUES ('+' '.join('?'+v for v in vars)+') { '+' '.join('('+' '.join('UNDEF' if x is None else '<urn:qlever:native-id:'+str(x)+'>' for x in row)+')' for row in rows)+' }'
def qlever_native():
    for n in NATIVE:
        if n['project']!='qlever' or n['name']!='ExistsJoin.computeResult':continue
        calls=[]
        # Preserve source order when expanding individual helper invocations.
        for name in ['testExists','testExistsFromIdTable']:
            for args in cpp_calls(n['sourceCode'],name):calls.append((name,args))
        for index,(helper,args) in enumerate(calls,1):
            try:
                left,right=map(cpp_table,args[:2]);expected=json.loads(args[2].replace('{','[').replace('}',']'));joins=int(args[3])
                if len(left)!=len(expected):raise ValueError('Source result cardinality mismatch')
                lwidth=len(left[0]) if left else 2;rwidth=len(right[0]) if right else 2
                lv=[('join'+str(i)) if i<joins else 'left'+str(i) for i in range(lwidth)]
                rv=[('join'+str(i)) if i<joins else 'right'+str(i) for i in range(rwidth)]
                query='SELECT '+' '.join('?'+v for v in lv)+' ?exists WHERE { '+values_clause(lv,left)+' BIND(EXISTS { '+values_clause(rv,right)+' } AS ?exists) }'
                c=add_native_case(n,query,inline(lv+['exists'],[[qid(x) for x in row]+[boolterm(b)] for row,b in zip(left,expected)]),suffix=f'/{helper}-{index:02}')
                c['nativeArguments']=args;c['notes'].append('Native IDs map injectively to IRIs; UNDEF remains unbound. Left input multiplicity is retained.')
            except Exception as e:issue(n['source']['url'],'QLever helper adapter rejected '+repr(args)+': '+str(e))

def qlever_yaml():
    project='qlever';root=VENDOR/project
    for p in files(root):
        if p.suffix.lower() not in {'.yaml','.yml'} or not any('e2e' in x for x in p.parts):continue
        try:doc=yaml.safe_load(p.read_text(encoding='utf8'))
        except Exception as e:issue(p,e);continue
        if not isinstance(doc,dict) or not isinstance(doc.get('queries'),list):continue
        fixtures=list(root.glob('e2e/scientist-collection.zip.expanded/**/scientists.nt'))
        for i,q in enumerate(doc['queries']):
            if not isinstance(q,dict) or 'sparql' not in q:continue
            c=newcase(project,p,str(q.get('query',i)),str(p.relative_to(root))+':'+str(i),q['sparql'])
            c['base']='https://example.org/qlever-scientists/';c['qleverChecks']=q.get('checks',[]);c['expected']={'type':'qlever-checks','checks':q.get('checks',[])}
            c['notes']+=['Original QLever YAML checks, not exact result-set equivalence.','Null expected cells are wildcards; floating-point tolerance is 0.1; result cap is 5,000.','One synthetic base resolves both relative fixture IRIs and query IRIs.']
            if fixtures:
                a=asset(project,fixtures[0]);a['uri']=c['base'];a['format']='Turtle';c['defaults']=[a]
            else:block(c,'scientists.nt not materialized from upstream archive')
            if q.get('type')=='text':block(c,'QLever text index prerequisite is not supplied by RepositoryConnection')
            supported={'num_rows','num_cols','selected','res','contains_row','order_numeric','order_string'}
            unknown={key for check in q.get('checks',[]) for key in check if key not in supported}
            if unknown:block(c,'Unimplemented native YAML checks: '+', '.join(sorted(unknown)))
            if not q.get('checks'):block(c,'No upstream correctness assertions')
            feature_gates(c);CASES.append(c)

def documentation():
    docs=OUT/'cases';docs.mkdir(exist_ok=True)
    for old in docs.glob('*.md'):old.unlink()
    for c in CASES:
        exp=c['expected'];expected_text=json.dumps(exp,ensure_ascii=False,indent=2)
        if isinstance(exp,dict) and 'path' in exp:
            ep=ROOT/exp['path']
            if ep.stat().st_size<250000:expected_text=ep.read_text(encoding='utf8',errors='replace')
            else:expected_text='Full expected result is retained in '+exp['path']+' ('+str(ep.stat().st_size)+' bytes).'
        prereq={'defaultGraphs':c['defaults'],'namedGraphs':c['named'],'requirements':c['requires'],'base':c['base'],'status':c['status'],'blockedReason':c['blockedReason']}
        content='# '+c['name']+'\n\nID: `'+c['id']+'`\n\n## Provenance\n\n'+c['source']['url']+'\n\n```json\n'+json.dumps(c['source'],ensure_ascii=False,indent=2)+'\n```\n\n## Prerequisites\n\n```json\n'+json.dumps(prereq,ensure_ascii=False,indent=2)+'\n```\n\n## Query\n\n```sparql\n'+c['query']+'\n```\n\n## Expected results / assertions\n\n```text\n'+expected_text+'\n```\n\n## Adaptation notes\n\n'+'\n\n'.join(c['notes'])+'\n'
        if c.get('sourceAssertion'):content+='\n## Original native assertion\n\n```text\n'+c['sourceAssertion']+'\n```\n'
        (docs/(c['id']+'.md')).write_text(content,encoding='utf8')
    compact=[{'id':c['id'],'name':c['name'],'project':c['project'],'status':c['status'],'query':c['query'],'reason':c['blockedReason'],'source':c['source']['url'],'expected':c['expected'],'defaults':c['defaults'],'named':c['named'],'base':c['base'],'notes':c['notes']} for c in CASES]
    payload=json.dumps(compact,ensure_ascii=False).replace('<','\\u003c').replace('&','\\u0026')
    page='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>SPARQL correctness catalogue</title><style>body{font:16px system-ui;max-width:1100px;margin:2rem auto;padding:0 1rem}input{width:100%;font:inherit;padding:.6rem;box-sizing:border-box}article{border-top:1px solid #bbb;padding:1rem 0}pre{overflow:auto;background:#f4f4f4;padding:1rem;white-space:pre-wrap}small{display:block}button{padding:.7rem}a{overflow-wrap:anywhere}</style><h1>SPARQL correctness catalogue</h1><p>Every entry retains its original oracle and prerequisites. Ready means specified, not passed. See reports for executed outcomes. Native inventory is separate and is not counted as a port.</p><input id="q" aria-label="Search cases" placeholder="Search names, queries, sources, or status"><p id="count"></p><main id="cases"></main><button id="more">Show more</button><script>const data='''+payload+''';let limit=50;const $=id=>document.getElementById(id);function el(tag,text){let e=document.createElement(tag);e.textContent=text;return e}function render(){let q=$('q').value.toLowerCase(),a=data.filter(x=>JSON.stringify(x).toLowerCase().includes(q));$('count').textContent=a.length+' matching records';$('cases').replaceChildren();for(let x of a.slice(0,limit)){let article=el('article','');let h=el('h2',x.name);article.append(h,el('small',x.id+' · '+x.status));let link=el('a','Full case documentation');link.href='cases/'+x.id+'.md';article.append(link);let d=el('details','');d.append(el('summary','Query, prerequisites and expected results'),el('pre',x.query),el('pre',JSON.stringify({base:x.base,defaults:x.defaults,named:x.named,expected:x.expected,reason:x.reason,notes:x.notes},null,2)));let s=el('a','Pinned source');s.href=x.source;d.append(s);article.append(d);$('cases').append(article)}$('more').hidden=limit>=a.length}$('q').oninput=()=>{limit=50;render()};$('more').onclick=()=>{limit+=50;render()};render();</script></html>'''
    (OUT/'catalog.html').write_text(page,encoding='utf8')

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--download',action='store_true');args=ap.parse_args();OUT.mkdir(exist_ok=True)
    if args.download:download()
    for project in SOURCES:
        if not (VENDOR/project).exists():raise FileNotFoundError('Missing vendor tree; run --download: '+project)
    unpack()
    for project in SOURCES:
        for p in files(VENDOR/project):
            if p.suffix.lower() in {'.ttl','.n3'}:read_manifest(project,p)
    native_inventory();jena_native();qlever_native();qlever_yaml()
    ids=[c['id'] for c in CASES]
    if len(ids)!=len(set(ids)):raise RuntimeError('Duplicate stable case IDs')
    CASES.sort(key=lambda c:(c['project'],c['source']['path'],c['name'],c['id']))
    dump(OUT/'cases.json',CASES);dump(OUT/'assets.json',ASSETS);dump(OUT/'native-inventory.json',NATIVE)
    summary={'records':len(CASES),'status':dict(collections.Counter(c['status'] for c in CASES)),'kinds':dict(collections.Counter(c['kind'] for c in CASES)),'projects':dict(collections.Counter(c['project'] for c in CASES)),'nativeDeclarations':len(NATIVE),'nativeQueryCandidates':sum(n['queryCandidate'] for n in NATIVE),'nativeDeclarationsWithAdapters':sum(bool(n['portedCases']) for n in NATIVE),'manifestIncludes':INCLUDES,'diagnostics':DIAG,'archives':ARCHIVES,'nonQueryManifestKinds':dict(SKIPPED_TYPES),'exhaustivePortComplete':False,'upstreamRunnerRegistrationReconciled':False}
    dump(OUT/'discovery.json',summary);documentation()
    print(json.dumps({k:v for k,v in summary.items() if k not in {'diagnostics','manifestIncludes','nonQueryManifestKinds','archives'}},indent=2))
    print('Manifest/import diagnostics:',len(DIAG));print('Unresolved manifest includes:',sum(not x['present'] for x in INCLUDES))
if __name__=='__main__':main()
