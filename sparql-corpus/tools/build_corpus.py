#!/usr/bin/env python3
"""Build the manifest corpus, then add reviewed native operator/helper adaptations."""
from __future__ import annotations
import collections, importlib.util, json, re
from pathlib import Path
spec=importlib.util.spec_from_file_location('corpus_importer',Path(__file__).with_name('import.py'))
base=importlib.util.module_from_spec(spec); spec.loader.exec_module(base)
original_expected=base.expected

def read_expected(uri):
    if isinstance(uri,base.Literal) and str(uri.datatype)==base.XSD+'boolean':
        return {'kind':'boolean','value':str(uri) in {'true','1'}}
    return original_expected(uri)
base.expected=read_expected

def split_args(text):
    result=[]; start=0; depth=0; quote=None; escape=False
    for i,c in enumerate(text):
        if quote:
            if escape: escape=False
            elif c=='\\': escape=True
            elif c==quote: quote=None
            continue
        if c in {'"',"'"}: quote=c
        elif c in '([{': depth+=1
        elif c in ')]}': depth-=1
        elif c==',' and depth==0: result.append(text[start:i].strip()); start=i+1
    result.append(text[start:].strip()); return result

def close_paren(text,pos):
    depth=1
    for i in range(pos,len(text)):
        if text[i]=='(': depth+=1
        elif text[i]==')':
            depth-=1
            if not depth: return i
    raise ValueError('unterminated helper invocation')

def cpp_array(text):
    text=re.sub(r'\bU\b','null',text).replace('{','[').replace('}',']')
    return json.loads(text)

def table(text):
    text=text.strip()
    if text.startswith('makeIdTableFromVector('):
        text=split_args(text[len('makeIdTableFromVector('):-1])[0]
    if text.startswith('IdTable'):
        m=re.match(r'IdTable\s*[({]\s*(\d+)',text)
        if not m: raise ValueError('unrecognized empty IdTable: '+text)
        return [],int(m.group(1))
    rows=cpp_array(text); width=len(rows[0]) if rows else 0
    if any(len(row)!=width for row in rows): raise ValueError('ragged table')
    return rows,width

def token(v): return 'UNDEF' if v is None else '<urn:qlever:vid:'+str(v)+'>'
def values(prefix,rows,width):
    return 'VALUES ('+' '.join('?'+prefix+str(i) for i in range(width))+') { '+' '.join('('+' '.join(token(v) for v in row)+')' for row in rows)+' }'

def native_case(p,name,line,query,vars,rows,assertion,notes):
    key=base.rel(p)+'#'+name
    return {'id':base.digest(key.encode())[:20],'name':name,'family':p.relative_to(base.VENDOR).parts[0]+'-native-adapted','kind':'evaluation','status':'ready','source':base.source(p,line),'base':'http://base/','query':query,'fixtures':[],'expected':{'kind':'tuple','vars':vars,'rows':rows},'sourceAssertion':assertion,'limitations':notes,'comparison':'unordered-bag'}

def exists_ports():
    p=base.VENDOR/'qlever/test/engine/ExistsJoinTest.cpp'; text=p.read_text()
    start=re.search(r'TEST\(ExistsJoin,\s*computeResult\)\s*\{',text)
    if not start: raise ValueError('ExistsJoin.computeResult not found')
    end=base.block_end(text,start.end()); original=text[start.end():end-1]
    body=re.sub(r'//[^\n]*',lambda m:' '*len(m.group()),original)
    generated=[]
    for ordinal,m in enumerate(re.finditer(r'\b(testExists|testExistsFromIdTable)\s*\(',body)):
        stop=close_paren(body,m.end()); args=split_args(body[m.end():stop])
        if len(args)!=4: raise ValueError('unsupported existence helper shape')
        left,lw=table(args[0]); right,rw=table(args[1]); flags=cpp_array(args[2]); joins=int(args[3])
        if len(left)!=len(flags) or joins>min(lw,rw): raise ValueError('bad existence assertion')
        predicates=['(!BOUND(?l'+str(i)+') || !BOUND(?r'+str(i)+') || sameTerm(?l'+str(i)+',?r'+str(i)+'))' for i in range(joins)]
        inner=values('r',right,rw)+(' FILTER('+' && '.join(predicates)+')' if predicates else '')
        query='SELECT '+' '.join('?l'+str(i) for i in range(lw))+' ?exists WHERE { '+values('l',left,lw)+' BIND(EXISTS { '+inner+' } AS ?exists) }'
        rows=[]
        for row,flag in zip(left,flags):
            bindings={'l'+str(i):{'type':'uri','value':'urn:qlever:vid:'+str(v)} for i,v in enumerate(row) if v is not None}
            bindings['exists']={'type':'literal','value':str(flag).lower(),'datatype':base.XSD+'boolean'}; rows.append(bindings)
        line=text.count('\n',0,start.end()+m.start())+1
        generated.append(native_case(p,'ExistsJoin.computeResult.'+str(ordinal+1).zfill(2),line,query,['l'+str(i) for i in range(lw)]+['exists'],rows,original[m.start():stop+1],['Native identifier tables are represented by distinct IRIs. Original expected booleans are retained.','Explicit compatibility filters preserve UNDEF wildcard matching; internal sort, lazy chunking, and optimizer selection are not asserted.']))
    if len(generated)!=12: raise ValueError('Expected twelve reviewed ExistsJoin helper calls, found '+str(len(generated)))
    return generated

def binding_ports():
    p=base.VENDOR/'jena/jena-arq/src/test/java/org/apache/jena/sparql/expr/TestExpressions3.java'; text=p.read_text(); result=[]
    for m in base.JAVA_METHOD.finditer(text):
        end=base.block_end(text,m.end()); body=text[m.end():end-1]
        call=re.search(r'\b(evalExpr|eval)\("([^"\\]*)",\s*"([^"\\]*)",\s*(true|false)\)',body)
        if not call: continue
        helper,expr,binding,boolean=call.groups(); match=re.fullmatch(r'\(\?(\w+) (\d+)\)',binding)
        bindings='VALUES ?'+match.group(1)+' { '+match.group(2)+' } ' if match else ''
        if not match and binding!='()': raise ValueError('unsupported source binding')
        if helper=='eval': expression=expr; setup=''
        else:
            atom=re.fullmatch(r'\(bound (\?\w+|\d+)\)',expr)
            addition=re.fullmatch(r'\(bound \(\+ (\?\w+) (\d+)\)\)',expr)
            if atom: target=atom.group(1)
            elif addition: target=addition.group(1)+' + '+addition.group(2)
            else: raise ValueError('unsupported BOUND algebra')
            setup='BIND(('+target+') AS ?temporary) '; expression='BOUND(?temporary)'
        query='SELECT ?result WHERE { '+bindings+setup+'BIND(('+expression+') AS ?result) }'
        row={'result':{'type':'literal','value':boolean,'datatype':base.XSD+'boolean'}}
        result.append(native_case(p,p.stem+'.'+m.group(1),text.count('\n',0,m.start())+1,query,['result'],[row],body,['Source bindings are supplied by VALUES. Generalized algebra BOUND expressions are lowered through a fresh BIND variable.']))
    if len(result)!=8: raise ValueError('Expected eight reviewed binding cases')
    return result

def enrich():
    additions=exists_ports()+binding_ports(); base.cases.extend(additions)
    for c in base.cases:
        if c.get('laxCardinality'):
            c['limitations']=[x for x in c['limitations'] if not x.startswith('REDUCED cardinality interval')]
            if not c['limitations']: c['status']='ready'
            elif c['status']=='blocked' and all(x.startswith('Explicit upstream result') for x in c['limitations']): c['status']='ready'
        if c.get('expected',{}).get('kind')=='tuple-file':
            suffix=Path(c['expected']['asset']['path']).suffix.lower()
            if suffix in {'.csv','.srt'}: base.block(c,'CSV needs lossy-format comparison, and SRT needs the Jena table parser; neither is treated as an exact RDF-term oracle')
        if c['family']=='jena-native-adapted' and c['source']['path'].endswith('/TestExpressions.java'):
            c['query']='PREFIX ex: <http://example.org/>\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPREFIX x.: <http://example.org/dot#>\nPREFIX : <http://default/>\nPREFIX select: <http://select/>\n'+c['query']
            if re.search(r'\btest(?:String|URI)\(',c.get('sourceAssertion','')):
                c['query']=c['query'].replace('WHERE { BIND(','WHERE { VALUES (?a ?x) { ("A" <urn:ex:abcd>) } BIND(BNODE() AS ?b) BIND(',1)
            if 'testNumeric(' in c.get('sourceAssertion','') or ('testEval(' in c.get('sourceAssertion','') and 'ExprEvalException.class' not in c.get('sourceAssertion','')):
                base.block(c,'Native helper asserts a specific numeric category or SSE value; exact typed-literal adaptation is retained for review, not enabled as an equivalent assertion')
        c['querySha256']=base.digest(c['query'].encode())
    for n in base.native:
        if n['project']=='qlever' and n['name']=='ExistsJoin.computeResult':
            n['disposition']='adapted-query'; n['caseIds']=[c['id'] for c in additions if c['family']=='qlever-native-adapted']
        for c in additions:
            if c['family']=='jena-native-adapted' and c['name'].endswith('.'+n['name']) and n['path'].endswith(c['source']['path']): n['disposition']='adapted-query'; n['caseId']=c['id']
    base.cases.sort(key=lambda c:(c['family'],c['source']['path'],c['name'],c['id']))
    ids=[c['id'] for c in base.cases]
    if len(ids)!=len(set(ids)): raise ValueError('duplicate IDs after native expansion')
    (base.OUT/'cases.json').write_text(json.dumps(base.cases,ensure_ascii=False,indent=2)+'\n')
    (base.OUT/'native-inventory.json').write_text(json.dumps(base.native,indent=2)+'\n')
    stats=json.loads((base.OUT/'coverage.json').read_text()); stats.update({'cases':len(base.cases),'status':dict(collections.Counter(c['status'] for c in base.cases)),'families':dict(collections.Counter(c['family'] for c in base.cases)),'reviewedNativeAdditions':len(additions),'nativeAdapted':sum(n['disposition']=='adapted-query' for n in base.native)})
    (base.OUT/'coverage.json').write_text(json.dumps(stats,indent=2)+'\n'); base.document(); print('ENRICHED_COVERAGE '+json.dumps(stats),flush=True)

if __name__=='__main__': base.main(); enrich()
