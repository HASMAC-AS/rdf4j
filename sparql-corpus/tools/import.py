#!/usr/bin/env python3
"""Discover manifest tests and selected native assertion forms without using an engine as oracle."""
from __future__ import annotations
import argparse, collections, hashlib, html, json, re, shutil, subprocess, tarfile, tempfile, zipfile
from pathlib import Path
from urllib.parse import quote, unquote, urlsplit
import rdflib
from rdflib import Graph, URIRef, BNode, Literal, Namespace, RDF, RDFS
import yaml
rdflib.NORMALIZE_LITERALS = False
ROOT=Path(__file__).resolve().parents[1]
VENDOR=ROOT/'vendor'; OUT=ROOT/'corpus'
MF=Namespace('http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#')
QT=Namespace('http://www.w3.org/2001/sw/DataAccess/tests/test-query#')
RS=Namespace('http://www.w3.org/2001/sw/DataAccess/tests/result-set#')
XSD='http://www.w3.org/2001/XMLSchema#'
VIRTUAL='https://corpus.invalid/'
PINS=json.loads((ROOT/'sources.json').read_text())
errors=[]; archives={}; consumed=set(); cases=[]; others=[]; manifests=[]; native=[]

def digest(data): return hashlib.sha256(data).hexdigest()
def rel(p): return p.relative_to(ROOT).as_posix()
def logical(p): return VIRTUAL+quote(rel(p),safe='/')
def local(uri):
    s=str(uri)
    if not s.startswith(VIRTUAL): return None
    p=(ROOT/unquote(urlsplit(s).path.lstrip('/'))).resolve()
    return p if p.is_relative_to(ROOT) and p.is_file() else None

def source(p,line=None):
    r=p.relative_to(VENDOR); project=r.parts[0]; file='/'.join(r.parts[1:]); pin=PINS[project]
    archived=archives.get(str(p))
    url='https://github.com/'+pin['repository']+'/blob/'+pin['revision']+'/'+quote(archived['archive'] if archived else file,safe='/')
    if line and not archived: url+='#L'+str(line)
    data=p.read_bytes()
    result={'repository':pin['repository'],'revision':pin['revision'],'path':file,'url':url,'sha256':digest(data),'gitBlobSha1':hashlib.sha1(('blob '+str(len(data))+'\0').encode()+data).hexdigest()}
    if archived: result['archiveMember']=archived['member']
    if line: result['line']=line
    return result

def download():
    VENDOR.mkdir(exist_ok=True)
    for project,pin in PINS.items():
        dst=VENDOR/project
        if dst.exists(): shutil.rmtree(dst)
        with tempfile.TemporaryDirectory() as temp:
            repo=Path(temp)/'repo'; subprocess.run(['git','init','-q',str(repo)],check=True)
            subprocess.run(['git','-C',str(repo),'fetch','--depth=1','https://github.com/'+pin['repository']+'.git',pin['revision']],check=True)
            subprocess.run(['git','-C',str(repo),'checkout','--detach','-q','FETCH_HEAD'],check=True)
            actual=subprocess.check_output(['git','-C',str(repo),'rev-parse','HEAD'],text=True).strip()
            if actual!=pin['revision']: raise RuntimeError('revision mismatch: '+project)
            inventory=subprocess.check_output(['git','-C',str(repo),'ls-files','--stage'],text=True)
            (OUT/(project+'-git-files.txt')).write_text(inventory)
            archive=Path(temp)/'sources.tar'; subprocess.run(['git','-C',str(repo),'archive','-o',str(archive),'HEAD'],check=True)
            dst.mkdir()
            with tarfile.open(archive) as tf: tf.extractall(dst,filter='data')
            # Verify every regular tracked file against the immutable Git inventory.
            checked=0
            for entry in inventory.splitlines():
                meta,name=entry.split('\t',1); mode,sha,_=meta.split(); p=dst/name
                if mode not in {'100644','100755'}: continue
                data=p.read_bytes(); actual_blob=hashlib.sha1(('blob '+str(len(data))+'\0').encode()+data).hexdigest()
                if actual_blob!=sha: raise RuntimeError('Git blob mismatch: '+name)
                checked+=1
            print(project+': verified '+str(checked)+' tracked source files',flush=True)

def unpack():
    for project in PINS:
        for zpath in sorted((VENDOR/project).rglob('*.zip')):
            if '_archives' in zpath.parts or not any(x in {'testing','e2e','test','tests','src'} for x in zpath.parts): continue
            dest=zpath.parent/'_archives'/zpath.stem
            with zipfile.ZipFile(zpath) as z:
                for info in z.infolist():
                    if info.is_dir(): continue
                    out=(dest/info.filename).resolve()
                    if not out.is_relative_to(dest.resolve()): raise ValueError('unsafe zip member '+info.filename)
                    out.parent.mkdir(parents=True,exist_ok=True)
                    with z.open(info) as src, out.open('wb') as dst: shutil.copyfileobj(src,dst)
                    archives[str(out)]={'archive':zpath.relative_to(VENDOR/project).as_posix(),'member':info.filename}
    (OUT/'archive-members.json').write_text(json.dumps(archives,indent=2))

def asset(uri):
    p=local(uri)
    return {'path':rel(p),'base':str(uri),'sha256':digest(p.read_bytes())} if p else {'missing':str(uri)}

def term(v):
    if isinstance(v,URIRef): return {'type':'uri','value':str(v)}
    if isinstance(v,BNode): return {'type':'bnode','value':str(v)}
    if isinstance(v,Literal):
        t={'type':'literal','value':str(v)}
        if v.language: t['xml:lang']=v.language
        else: t['datatype']=str(v.datatype or XSD+'string')
        return t
    raise ValueError('unsupported RDF term: '+repr(v))

def expected(uri):
    p=local(uri)
    if not p: return {'kind':'missing','source':str(uri)}
    raw=p.read_bytes(); suffix=p.suffix.lower(); data=asset(uri)
    if suffix in {'.srx','.xml'} and b'<sparql' in raw[:2048]:
        import xml.etree.ElementTree as ET
        doc=ET.fromstring(raw); ns={'s':'http://www.w3.org/2005/sparql-results#'}
        b=doc.find('s:boolean',ns)
        if b is not None: return {'kind':'boolean','value':(b.text or '').strip() in {'true','1'},'asset':data}
        variables=[x.attrib['name'] for x in doc.findall('s:head/s:variable',ns)]; rows=[]
        for result in doc.findall('s:results/s:result',ns):
            row={}
            for binding in result:
                if not len(binding): continue
                node=binding[0]; kind=node.tag.split('}')[-1]
                if kind=='unbound': continue
                if kind not in {'uri','bnode','literal'}: return {'kind':'unsupported','reason':'non-scalar SPARQL XML result term','asset':data}
                t={'type':kind,'value':node.text or ''}
                if kind=='literal':
                    lang=node.get('{http://www.w3.org/XML/1998/namespace}lang')
                    if lang: t['xml:lang']=lang
                    else: t['datatype']=node.get('datatype',XSD+'string')
                row[binding.attrib['name']]=t
            rows.append(row)
        return {'kind':'tuple','vars':variables,'rows':rows,'asset':data}
    if suffix in {'.srj','.json'}:
        obj=json.loads(raw)
        if 'boolean' in obj: return {'kind':'boolean','value':obj['boolean'],'asset':data}
        if 'results' in obj: return {'kind':'tuple','vars':obj['head'].get('vars',[]),'rows':obj['results'].get('bindings',[]),'asset':data}
        return {'kind':'unsupported','reason':'unknown JSON expected format','asset':data}
    if suffix in {'.csv','.tsv','.srt','.srx'}:
        return {'kind':'tuple-file','asset':data}
    formats={'.ttl':'turtle','.n3':'turtle','.rdf':'xml','.trig':'trig','.nt':'nt','.nq':'nquads'}
    if suffix not in formats: return {'kind':'unsupported','reason':'unknown result format '+suffix,'asset':data}
    g=Graph()
    try: g.parse(data=raw,format=formats[suffix],publicID=str(uri))
    except Exception as exc:
        # Rio may support newer RDF 1.2 syntax than rdflib; preserve raw graph oracle.
        return {'kind':'graph','asset':data,'parserNote':str(exc)}
    rs=next(g.subjects(RDF.type,RS.ResultSet),None)
    if rs is None: return {'kind':'graph','asset':data}
    b=g.value(rs,RS.boolean)
    if b is not None: return {'kind':'boolean','value':str(b) in {'true','1'},'asset':data}
    variables=sorted(str(v) for v in g.objects(rs,RS.resultVariable)); indexed=[]
    for solution in g.objects(rs,RS.solution):
        row={}
        for b in g.objects(solution,RS.binding):
            name=g.value(b,RS.variable); value=g.value(b,RS.value)
            if value is not None: row[str(name)]=term(value)
        index=g.value(solution,RS.index)
        indexed.append((int(index) if index is not None else None,row))
    ordered=bool(indexed) and all(i is not None for i,_ in indexed)
    if ordered: indexed.sort(key=lambda pair:pair[0])
    else: indexed.sort(key=lambda pair:json.dumps(pair[1],sort_keys=True))
    return {'kind':'tuple','vars':variables,'rows':[r for _,r in indexed],'ordered':ordered,'asset':data}

def block(c,reason):
    c['status']='blocked'; c.setdefault('limitations',[]).append(reason)

def read_manifest(p):
    raw=p.read_text(errors='replace'); g=Graph()
    try: g.parse(data=raw,format='turtle',publicID=logical(p))
    except Exception as exc:
        errors.append({'path':rel(p),'stage':'manifest-parse','error':str(exc)}); return
    entries=[]; listed=set()
    for head in g.objects(None,MF.entries):
        try:
            for entry in g.items(head):
                if entry not in listed: entries.append(entry); listed.add(entry)
        except Exception as exc: errors.append({'path':rel(p),'stage':'manifest-list','error':str(exc)})
    for s,_,t in g.triples((None,RDF.type,None)):
        if str(t).startswith(str(MF)) and 'Test' in str(t) and s not in listed: entries.append(s)
    includes=[]
    for head in g.objects(None,MF.include):
        try: includes.extend(str(x) for x in g.items(head))
        except Exception: includes.append(str(head))
    manifests.append({'path':rel(p),'entries':len(entries),'includes':includes,'source':source(p)})
    for ordinal,e in enumerate(entries):
        types=sorted(str(t) for t in g.objects(e,RDF.type)); name=str(g.value(e,MF.name) or g.value(e,RDFS.label) or str(e).split('#')[-1])
        category='evaluation' if any(t.endswith('QueryEvaluationTest') for t in types) else 'syntax-negative' if any(t.endswith(('NegativeSyntaxTest','NegativeSyntaxTest11')) for t in types) else 'syntax-positive' if any(t.endswith(('PositiveSyntaxTest','PositiveSyntaxTest11')) for t in types) else 'other'
        if category=='other':
            others.append({'name':name,'types':types,'manifest':rel(p),'ordinal':ordinal,'source':source(p)}); continue
        action=g.value(e,MF.action); query=g.value(action,QT.query) if isinstance(action,(BNode,URIRef)) else None
        if query is None and category.startswith('syntax'): query=action
        # Update syntax tests are retained separately; they are not query-syntax tests.
        if any('Update' in t for t in types): others.append({'name':name,'types':types,'manifest':rel(p),'ordinal':ordinal,'source':source(p)}); continue
        qpath=local(query) if query else None
        key=p.relative_to(VENDOR).as_posix()+'#'+str(ordinal)+':'+name
        line=next((i+1 for i,l in enumerate(raw.splitlines()) if 'mf:name' in l and name in l),1)
        c={'id':digest(key.encode())[:20],'name':name,'family':p.relative_to(VENDOR).parts[0]+'-manifest','kind':category,'status':'ready','source':source(p,line),'manifest':rel(p),'ordinal':ordinal,'listed':e in listed,'types':types,'description':str(g.value(e,RDFS.comment) or ''),'query':qpath.read_text(errors='replace') if qpath else '', 'queryAsset':asset(query) if query else {},'base':str(query or logical(p)),'fixtures':[],'limitations':[]}
        if qpath: consumed.add(str(qpath))
        else: block(c,'query file unavailable')
        if category=='evaluation':
            for d in g.objects(action,QT.data): c['fixtures'].append({**asset(d),'graph':None})
            for d in g.objects(action,QT.graphData):
                if isinstance(d,URIRef): c['fixtures'].append({**asset(d),'graph':str(d)})
                else:
                    f=g.value(d,QT.graph); label=g.value(d,RDFS.label) or g.value(d,QT.label)
                    if f is None: block(c,'unrecognized named graph descriptor'); continue
                    c['fixtures'].append({**asset(f),'graph':str(label or f)})
            result=g.value(e,MF.result)
            try: c['expected']=expected(result) if result is not None else {'kind':'missing'}
            except Exception as exc: c['expected']={'kind':'unsupported','reason':str(exc)}
            if c['expected']['kind'] in {'missing','unsupported'}: block(c,'expected result unavailable or unsupported: '+c['expected'].get('reason',''))
            c['laxCardinality']=g.value(e,MF.resultCardinality)==MF.LaxCardinality
            if c['laxCardinality']: block(c,'REDUCED cardinality interval oracle requires a separate comparator; strict equality would be incorrect')
            if any('missing' in f for f in c['fixtures']): block(c,'fixture file unavailable')
            if re.search(r'\bSERVICE\b',c['query'],re.I): block(c,'requires isolated SERVICE endpoint fixture')
            if re.search(r'\bDESCRIBE\b',c['query'],re.I): block(c,'implementation-defined DESCRIBE description policy')
            if any('entailment' in str(pred).lower() for pred in g.predicates(action,None)) or any('entailment' in str(pred).lower() for pred in g.predicates(e,None)): block(c,'requires declared entailment regime')
            if c['expected'].get('ordered'):
                c['comparison']='upstream-indexed-sequence'; c['limitations'].append('Explicit upstream result indexes impose sequence comparison; tied ORDER BY rows may admit alternative valid orders.')
            else: c['comparison']='unordered-bag'
        else: c['expected']={'kind':category}
        cases.append(c)

def scan_manifests():
    for p in sorted(VENDOR.rglob('*')):
        if not p.is_file() or p.suffix.lower() not in {'.ttl','.n3'}: continue
        if 'manifest' not in p.name.lower():
            if p.stat().st_size>1000000: continue
            data=p.read_bytes()
            if b'test-manifest#' not in data or b'entries' not in data: continue
        read_manifest(p)

def qlever_yaml():
    scientist=next((p for p in VENDOR.rglob('scientists.nt') if '_archives' in p.parts),None)
    for p in sorted((VENDOR/'qlever').rglob('*.yaml')):
        if 'e2e' not in p.parts: continue
        try: obj=yaml.safe_load(p.read_text())
        except Exception as exc: errors.append({'path':rel(p),'stage':'yaml','error':str(exc)}); continue
        if not isinstance(obj,dict) or not isinstance(obj.get('queries'),list): continue
        for i,q in enumerate(obj['queries']):
            if not isinstance(q,dict) or not q.get('sparql'): continue
            key=rel(p)+'#'+str(i); line=next((i+1 for i,l in enumerate(p.read_text().splitlines()) if str(q.get('query','')) in l),1)
            c={'id':digest(key.encode())[:20],'name':str(q.get('query',i)),'family':'qlever-yaml','kind':'evaluation','status':'ready','source':source(p,line),'base':logical(p),'query':q['sparql'],'fixtures':([{**asset(logical(scientist)),'graph':None}] if scientist else []),'expected':{'kind':'qlever-checks','checks':q.get('checks',[])},'limitations':['Original YAML oracle: null cells are wildcards, numeric tolerance is 0.1, return cap is 5000.'],'comparison':'upstream-yaml-checks'}
            if not scientist: block(c,'scientists fixture unavailable')
            if not q.get('checks'): block(c,'upstream query has no result assertions')
            if re.search(r'contains-(?:word|entity)|ql:|ql\.|TEXT\(|SCORE\(|\bSERVICE\b',q['sparql'],re.I): block(c,'requires QLever text/index/extension or remote SERVICE capability')
            supported={'num_rows','num_cols','selected','res','contains_row','order_numeric'}
            unsupported={k for check in q.get('checks',[]) for k in check if k not in supported}
            if unsupported: block(c,'unported YAML checks: '+', '.join(sorted(unsupported)))
            cases.append(c)

JAVA_METHOD=re.compile(r'@Test\b[^{};]*?\b(?:public\s+)?void\s+(\w+)\s*\([^)]*\)\s*\{',re.S)
CPP_METHOD=re.compile(r'\b(TEST(?:_F|_P)?|TYPED_TEST(?:_P)?)\s*\(\s*([^,]+),\s*([^\)]+)\)\s*\{')
# Lexical skipping avoids interpreting braces inside comments, strings or C++ raw strings.
def block_end(s,pos):
    depth=1; i=pos
    while i<len(s):
        if s.startswith('//',i):
            j=s.find('\n',i); i=len(s) if j<0 else j; continue
        if s.startswith('/*',i):
            j=s.find('*/',i+2); i=len(s) if j<0 else j+2; continue
        if s.startswith('R"',i):
            j=s.find('(',i+2)
            if j>=0:
                end=')'+s[i+2:j]+'"'; k=s.find(end,j+1); i=len(s) if k<0 else k+len(end); continue
        if s[i] in {'"',"'"}:
            q=s[i]; i+=1
            while i<len(s):
                if s[i]=='\\': i+=2
                elif s[i]==q: i+=1; break
                else: i+=1
            continue
        if s[i]=='{': depth+=1
        elif s[i]=='}':
            depth-=1
            if not depth: return i+1
        i+=1
    return len(s)

def expression_case(p,name,body,line):
    # Only parse completely understood, direct assertion helpers. No arbitrary execution.
    if p.name not in {'TestExpressions.java','TestExpressions2.java'}: return None
    call=re.search(r'\b(testBoolean|testString|testURI|testNumeric|testEval|eval)\(\s*("(?:\\.|[^"\\])*")\s*(?:,\s*(true|false|"(?:\\.|[^"\\])*"|[-+]?\d+(?:\.\d+)?[LD]?))?\s*\)',body,re.S)
    if not call: return None
    # Do not misport helpers embedded in a contextual wrapper or multiple calls.
    if 'strictMode(' in body or len(re.findall(r'\b(?:testBoolean|testString|testURI|testNumeric|testEval|eval)\(',body))!=1: return None
    helper,lit,val=call.groups()
    try: expr=json.loads(lit)
    except Exception: return None
    negative='QueryParseException.class' in body; evalerror='ExprEvalException.class' in body
    if 'assertThrows' in body and not (negative or evalerror): return None
    if val is None and not evalerror and not negative and helper!='eval': return None
    ebv=helper=='eval'; projected='IF(('+expr+'),true,false)' if ebv else '('+expr+')'
    query='PREFIX xsd: <'+XSD+'>\nPREFIX fn: <http://www.w3.org/2005/xpath-functions#>\nPREFIX math: <http://www.w3.org/2005/xpath-functions/math#>\nSELECT ?result WHERE { BIND('+projected+' AS ?result) }'
    row={}
    if not (evalerror or negative):
        if val is None: val='true'
        if val in {'true','false'}: t={'type':'literal','value':val,'datatype':XSD+'boolean'}
        elif val.startswith('"'):
            v=json.loads(val); t={'type':'uri','value':v} if helper=='testURI' else {'type':'literal','value':v,'datatype':XSD+'string'}
        else: t={'type':'literal','value':val.rstrip('LD'),'datatype':XSD+('double' if val.endswith('D') else 'decimal' if '.' in val else 'integer')}
        row['result']=t
    key=rel(p)+'#'+name
    c={'id':digest(key.encode())[:20],'name':p.stem+'.'+name,'family':'jena-native-adapted','kind':'syntax-negative' if negative else 'evaluation','status':'ready','source':source(p,line),'base':'http://base/','query':query,'fixtures':[],'expected':{'kind':'syntax-negative'} if negative else {'kind':'tuple','vars':['result'],'rows':[row]},'sourceAssertion':body,'limitations':['Expression helper lowered to a one-row SELECT/BIND. Expression errors leave result unbound, not an empty solution sequence.'],'comparison':'exact-term-bag'}
    if ebv: c['limitations'].append('The eval helper asserts effective boolean value; IF preserves that assertion.')
    if re.search(r'\b(?:EBV|CALL|hasLANG|hasLANGDIR|LANGDIR|STRLANGDIR|TRIPLE|SUBJECT|PREDICATE|OBJECT)\s*\(|<<|xsd:g(?:Year|Month|Day)',expr,re.I): block(c,'ARQ extension, RDF 1.2 or extended datatype behavior requires capability review')
    if helper=='testNumeric': c['limitations'].append('Numeric helper port uses exact typed literal equality; inspect lexical/numeric comparison divergences.')
    return c

def scan_native():
    for project in PINS:
        for p in sorted((VENDOR/project).rglob('*')):
            if not p.is_file() or p.suffix not in {'.java','.cpp','.cc','.h'} or not any(x in {'test','tests','testing','e2e'} for x in p.parts): continue
            text=p.read_text(errors='replace'); pattern=JAVA_METHOD if p.suffix=='.java' else CPP_METHOD
            for m in pattern.finditer(text):
                end=block_end(text,m.end()); body=text[m.end():end-1]; line=text.count('\n',0,m.start())+1
                name=m.group(1) if p.suffix=='.java' else m.group(2).strip()+'.'+m.group(3).strip()
                c=expression_case(p,name,body,line) if p.suffix=='.java' else None
                if c: cases.append(c)
                native.append({'project':project,'path':rel(p),'name':name,'line':line,'endLine':text.count('\n',0,end)+1,'declarationKind':'@Test' if p.suffix=='.java' else m.group(1),'disposition':'adapted-query' if c else 'inventory-only','caseId':c['id'] if c else None,'source':source(p,line)})

def document():
    case_dir=OUT/'cases'; case_dir.mkdir(exist_ok=True)
    for old in case_dir.glob('*.md'): old.unlink()
    cards=[]
    for c in cases:
        fixture='\n'.join('- '+json.dumps(f,ensure_ascii=False) for f in c['fixtures']) or 'Empty repository. No fixture triples.'
        expected_text=json.dumps(c['expected'],ensure_ascii=False,indent=2)
        text='# '+c['name']+'\n\nID: `'+c['id']+'`\n\nStatus: **'+c['status']+'**. Kind: '+c['kind']+'.\n\n## Provenance\n\n'+c['source']['url']+'\n\nSource SHA-256: `'+c['source']['sha256']+'`\n\n## Prerequisites\n\n'+fixture+'\n\nBase IRI: `'+c['base']+'`\n\n## Query\n\n```sparql\n'+c['query']+'\n```\n\n## Expected results\n\n```json\n'+expected_text+'\n```\n\n## Limitations and adaptations\n\n'+'\n'.join('- '+x for x in c.get('limitations',[]))+'\n'
        if c.get('sourceAssertion'): text+='\n## Original assertion\n\n```java\n'+c['sourceAssertion']+'\n```\n'
        (case_dir/(c['id']+'.md')).write_text(text)
        cards.append('<details data-search="'+html.escape(c['name']+' '+c['family']+' '+c['status'],quote=True)+'"><summary>'+html.escape(c['family']+' / '+c['name']+' ['+c['status']+']')+'</summary><p><a href="'+html.escape(c['source']['url'],quote=True)+'">Pinned source</a> · <a href="cases/'+c['id']+'.md">Full case documentation</a></p><h3>Prerequisites</h3><pre>'+html.escape(fixture)+'</pre><h3>Query</h3><pre>'+html.escape(c['query'])+'</pre><h3>Expected results</h3><pre>'+html.escape(expected_text)+'</pre><p>'+html.escape('; '.join(c.get('limitations',[])))+'</p></details>')
    page='<!doctype html><meta charset="utf-8"><title>SPARQL correctness corpus</title><style>body{font:16px system-ui;margin:2rem auto;max-width:1100px;padding:0 1rem}input{font:inherit;padding:.7rem;width:90%}details{border-bottom:1px solid #ccc;padding:1rem 0}summary{cursor:pointer}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#f5f5f5;padding:1rem;font-size:13px}h1{font-size:28px}</style><h1>SPARQL correctness corpus</h1><p>'+str(len(cases))+' catalogue records. Ready is not a passing result. Native inventory is separate and not counted as ported tests.</p><input id="search" placeholder="Filter by name, project or status">'+''.join(cards)+'<script>document.querySelector("#search").addEventListener("input",e=>{let q=e.target.value.toLowerCase();document.querySelectorAll("details").forEach(d=>d.hidden=!d.dataset.search.toLowerCase().includes(q))})</script>'
    (OUT/'catalog.html').write_text(page)

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--download',action='store_true'); args=ap.parse_args(); OUT.mkdir(exist_ok=True)
    if args.download: download()
    if not all((VENDOR/x).exists() for x in PINS): raise SystemExit('Run with --download first')
    unpack(); scan_manifests(); qlever_yaml(); scan_native()
    cases.sort(key=lambda c:(c['family'],c['source']['path'],c['name'],c['id']))
    ids=[c['id'] for c in cases]
    if len(ids)!=len(set(ids)): raise ValueError('duplicate case IDs')
    for c in cases:
        c['querySha256']=digest(c['query'].encode()); (OUT/'cases.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n') if False else None
    (OUT/'cases.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n')
    (OUT/'native-inventory.json').write_text(json.dumps(native,indent=2)+'\n')
    (OUT/'other-manifest-records.json').write_text(json.dumps(others,indent=2)+'\n')
    (OUT/'manifest-inventory.json').write_text(json.dumps(manifests,indent=2)+'\n')
    (OUT/'import-errors.json').write_text(json.dumps(errors,indent=2)+'\n')
    # Orphan query files are explicit audit gaps, never silently reported as tests.
    orphan=[rel(p) for p in sorted(VENDOR.rglob('*')) if p.is_file() and p.suffix in {'.rq','.sparql'} and str(p) not in consumed]
    (OUT/'unregistered-query-files.json').write_text(json.dumps(orphan,indent=2)+'\n')
    stats={'cases':len(cases),'status':dict(collections.Counter(c['status'] for c in cases)),'families':dict(collections.Counter(c['family'] for c in cases)),'nativeDeclarations':len(native),'nativeAdapted':sum(n['disposition']=='adapted-query' for n in native),'manifestFiles':len(manifests),'otherManifestRecords':len(others),'unregisteredQueries':len(orphan),'importErrors':len(errors),'exhaustivePortComplete':False}
    (OUT/'coverage.json').write_text(json.dumps(stats,indent=2)+'\n'); document(); print(json.dumps(stats),flush=True)

if __name__=='__main__': main()
