#!/usr/bin/env python3
"""Port result assertions of QLever's lazy relation helpers; physical chunk assertions remain separate.

The accepted initializer language is deliberately small. Loops, unknown mutations,
custom conversion functions and unresolved values are audited rather than guessed.
"""
from __future__ import annotations
import collections, hashlib, importlib.util, json, re
from pathlib import Path
import optional_ports as opt

base=opt.base; builder=opt.builder
SOURCES={
    'test/MinusTest.cpp':('dd73fb17f52d99beb60913bbf33383c3de9cb732','testMinus','minus'),
    'test/engine/ExistsJoinTest.cpp':('90e5c49b00d3993d11f1387a6b6c13031c80b423','testExistsJoin','exists'),
    'test/engine/OptionalJoinTest.cpp':('023a7760457c74de86b129677e09fbc8388d0c12','testLazyOptionalJoin','optional'),
}
VECTOR=re.compile(r'\bstd::vector\s*<\s*IdTable\s*>\s+(\w+)\s*(;|=|\{)')


def unwrap(text):
    text=text.strip()
    while True:
        m=re.fullmatch(r'std::move\s*\(([\s\S]*)\)',text)
        if m:text=m.group(1).strip();continue
        m=re.fullmatch(r'(\w+)\.clone\(\s*\)',text)
        if m:return m.group(1)
        return text


def array(text, integer_default=False):
    def tag(kind):return lambda m:json.dumps('@'+kind+':'+m.group(1))
    text=re.sub(r'\b(?:I|Id::makeFromInt)\s*\(\s*(-?\d+)\s*\)',tag('integer'),text)
    text=re.sub(r'\bV\s*\(\s*(-?\d+)\s*\)',tag('vocab'),text)
    text=text.replace('Id::makeUndefined()','null')
    text=re.sub(r'\bU\b','null',text);text=re.sub(r'\bT\b','true',text);text=re.sub(r'\bF\b','false',text)
    text=text.replace('{','[').replace('}',']');text=re.sub(r',\s*]',']',text)
    values=json.loads(text)
    if not isinstance(values,list):raise ValueError('Expected a literal table array')
    result=[]
    for row in values:
        if not isinstance(row,list):raise ValueError('Expected literal row array')
        parsed=[]
        for cell in row:
            if cell is None or type(cell)is bool:parsed.append(cell)
            elif type(cell)is int:parsed.append(('@integer:' if integer_default else '@vocab:')+str(cell))
            elif isinstance(cell,str) and re.fullmatch(r'@(integer|vocab):-?\d+',cell):parsed.append(cell)
            else:raise ValueError('Unknown Id constant: '+repr(cell))
        result.append(tuple(parsed))
    if result and any(len(r)!=len(result[0]) for r in result):raise ValueError('Ragged table')
    return tuple(result)


class Tables(opt.Constants):
    def __init__(self,text):
        super().__init__(text);self.vectors=[]
        for m in VECTOR.finditer(text):
            name,op=m.groups();start=m.end() if op in {';','='} else m.end()-1
            end=m.end()-1 if op==';' else opt.expression_end(text,start)
            expression='' if op==';' else text[start:end].strip()
            self.vectors.append(opt.Declaration(name,'vector',expression,m.start(),end,opt.scope_at(self.index,m.start())))
    def table(self,expression,before,seen=()):
        expression=unwrap(expression)
        if re.fullmatch(r'\w+',expression):
            d=self.visible(expression,before)
            if re.search(r'\bstd::move\s*\(\s*'+re.escape(expression)+r'\s*\)',self.text[d.end+1:before]):
                raise ValueError('Previously moved IdTable: '+expression)
            return super().table(expression,before,seen)
        empty=re.fullmatch(r'IdTable\s*[({]\s*(\d+)\s*,[\s\S]*[)}]',expression)
        if empty:return opt.Table((),int(empty.group(1)))
        m=re.fullmatch(r'makeIdTableFromVector\s*\(([\s\S]*)\)',expression)
        if not m:raise ValueError('Unknown table constructor: '+expression[:100])
        args=builder.split_args(m.group(1));integer_default=False
        if len(args)==2 and args[1].strip() in {'I','Id::makeFromInt'}:integer_default=True
        elif len(args)!=1:raise ValueError('Custom Id converter is not in the reviewed grammar')
        rows=array(args[0],integer_default)
        if not rows:raise ValueError('Empty literal table has unknown width')
        return opt.Table(rows,len(rows[0]))
    def sequence(self,expression,before,seen=()):
        expression=unwrap(expression)
        if expression.startswith('std::vector<IdTable>'):expression=expression[len('std::vector<IdTable>'):].strip()
        if expression.startswith('{') and expression.endswith('}'):
            body=expression[1:-1].strip()
            return [] if not body else [self.table(x,before,seen) for x in builder.split_args(body)]
        if not re.fullmatch(r'\w+',expression):raise ValueError('Unknown table sequence: '+expression[:100])
        if expression in seen:raise ValueError('Cyclic vector alias')
        scope=opt.scope_at(self.index,before)
        visible=[d for d in self.vectors if d.name==expression and d.end<before and scope[:len(d.scope)]==d.scope]
        if not visible:raise ValueError('No visible vector declaration: '+expression)
        d=max(visible,key=lambda x:x.offset)
        rows=[] if not d.expression else self.sequence(d.expression,d.offset,seen+(expression,))
        tail=self.text[d.end+1:before]
        if re.search(r'\b(?:for|while|if|switch)\s*\(',tail):raise ValueError('Control flow changes vector contents; needs a parameter expansion adapter')
        if re.search(r'\bstd::move\s*\(\s*'+re.escape(expression)+r'\s*\)',tail):raise ValueError('Previously moved vector: '+expression)
        for m in re.finditer(r'\b'+re.escape(expression)+r'\s*\.\s*(\w+)\s*\(',tail):
            absolute=d.end+1+m.start();call_scope=opt.scope_at(self.index,absolute)
            active=[x for x in self.vectors if x.name==expression and x.end<absolute and call_scope[:len(x.scope)]==x.scope]
            if not active or max(active,key=lambda x:x.offset)!=d:continue
            stop=builder.close_paren(tail,m.end());args=tail[m.end():stop].strip();method=m.group(1)
            if method=='push_back':rows.append(self.table(args,absolute,seen))
            elif method=='clear' and not args:rows=[]
            elif method=='reserve':pass
            elif method=='emplace_back':
                parts=builder.split_args(args)
                if len(parts)==2 and re.fullmatch(r'\d+',parts[0]):rows.append(opt.Table((),int(parts[0])))
                elif len(parts)==1:rows.append(self.table(args,absolute,seen))
                else:raise ValueError('Unsupported emplace_back constructor')
            else:raise ValueError('Unknown vector mutation: '+method)
        if re.search(r'\b'+re.escape(expression)+r'\s*(?:\[|=(?!=))',tail):raise ValueError('Unknown vector reassignment/index mutation')
        return rows


def cell_term(cell):
    if cell is None:return None
    if type(cell)is bool:return {'type':'literal','value':str(cell).lower(),'datatype':base.XSD+'boolean'}
    kind,value=cell[1:].split(':',1)
    return {'type':'uri','value':'urn:qlever:vid:'+value} if kind=='vocab' else {'type':'literal','value':value,'datatype':base.XSD+'integer'}


def token(cell):
    term=cell_term(cell)
    if term is None:return 'UNDEF'
    if term['type']=='uri':return '<'+term['value']+'>'
    return '"'+term['value']+'"^^<'+term['datatype']+'>'


def values(names,rows):
    return 'VALUES ('+' '.join('?'+n for n in names)+') { '+' '.join('('+' '.join(token(v) for v in row)+')' for row in rows)+' }'


def flatten(tables,width):
    if any(t.width!=width for t in tables):raise ValueError('Helper table width does not match its variable mapping')
    return tuple(row for table in tables for row in table.rows)


def reference_rows(left,right,operation,width):
    """Independent decoder calibration. These outputs are never serialized as gold."""
    out=[]
    for a in left:
        compatible=[b for b in right if a[0] is None or b[0] is None or a[0]==b[0]]
        if operation=='minus':
            if not any(a[0] is not None and b[0] is not None and a[0]==b[0] for b in right):out.append(a)
        elif operation=='exists':out.append(a+(bool(compatible),))
        else:
            if not compatible:out.append(a+((None,) if width==2 else ()))
            for b in compatible:out.append((a[0] if a[0] is not None else b[0],)+a[1:]+b[1:])
    return collections.Counter(out)


def extract(path,helper,operation):
    text=path.read_text();masked=opt.mask_comments(text);records=[];audit=[]
    for method in base.CPP_METHOD.finditer(masked):
        end=base.block_end(masked,method.end());body=masked[method.end():end-1]
        matches=list(re.finditer(r'\b'+re.escape(helper)+r'\s*\(',body))
        if not matches:continue
        method_name=method.group(2).strip()+'.'+method.group(3).strip()
        try:constants=Tables(body)
        except ValueError as exc:
            audit.append({'method':method_name,'status':'gap','reason':str(exc),'helperCalls':len(matches)});continue
        for ordinal,call in enumerate(matches,1):
            absolute=method.end()+call.start();stop=builder.close_paren(body,call.end());invocation=text[absolute:method.end()+stop+1]
            entry={'method':method_name,'ordinal':ordinal,'line':text.count('\n',0,absolute)+1,'helper':helper,'assertion':invocation}
            try:
                args=builder.split_args(body[call.end():stop])
                if len(args)not in {3,4}:raise ValueError('Unreviewed helper signature')
                if len(args)==4 and args[3]not in {'true','false'}:raise ValueError('Nonconstant singleVar requires parameter expansion')
                width=1 if len(args)==4 and args[3]=='true' else 2
                left_tables=constants.sequence(args[0],call.start());right_tables=constants.sequence(args[1],call.start());expected_tables=constants.sequence(args[2],call.start())
                left,right=flatten(left_tables,width),flatten(right_tables,width)
                out_width=width if operation=='minus' else width+1 if operation=='exists' else 2*width-1
                gold=flatten(expected_tables,out_width)
                if reference_rows(left,right,operation,width)!=collections.Counter(gold):raise ValueError('Source expected table disagrees with decoded observable relation; no oracle substituted')
                left_names=['x'] if width==1 else ['x','y'];right_names=['x'] if width==1 else ['x','z']
                projected=left_names if operation=='minus' else left_names+['exists'] if operation=='exists' else left_names+right_names[1:]
                lhs=values(left_names,left)
                if operation=='minus':pattern=lhs+' MINUS { '+values(right_names,right)+' }'
                elif operation=='optional':pattern=lhs+' OPTIONAL { '+values(right_names,right)+' }'
                else:
                    rnames=['r0'] if width==1 else ['r0','r1']
                    pattern=lhs+' BIND(EXISTS { '+values(rnames,right)+' FILTER(!BOUND(?x) || !BOUND(?r0) || sameTerm(?x,?r0)) } AS ?exists)'
                query='SELECT '+' '.join('?'+x for x in projected)+' WHERE { '+pattern+' }'
                expected=[{name:cell_term(v) for name,v in zip(projected,row) if v is not None} for row in gold]
                case=builder.native_case(path,method_name+'.lazy.'+str(ordinal).zfill(2),entry['line'],query,projected,expected,invocation,[
                    'The source chunk sequences are concatenated into VALUES input relations. The source expected chunk sequence supplies the complete expected result multiset.',
                    'Only query-observable results are ported: native chunk boundaries, lazy/materialized flags, physical order, runtime statistics and cache behavior are not asserted.',
                    'MINUS retains mappings with no jointly bound shared variable; EXISTS and OPTIONAL use compatibility including UNDEF. This distinction is independently calibrated against the source assertions.'
                ])
                case['family']='qlever-native-lazy';case['nativeMethod']=method_name;case['querySha256']=base.digest(query.encode())
                case['nativeInputs']={'leftChunks':[{'width':t.width,'rows':t.rows}for t in left_tables],'rightChunks':[{'width':t.width,'rows':t.rows}for t in right_tables],'expectedChunks':[{'width':t.width,'rows':t.rows}for t in expected_tables],'operation':operation}
                records.append(case);entry.update(status='ported',caseId=case['id'])
            except (ValueError,TypeError,IndexError) as exc:entry.update(status='gap',reason=str(exc))
            audit.append(entry)
    return records,audit


def calibration():
    def v(n):return '@vocab:'+str(n)
    assert reference_rows(((None,v(1)),(v(2),v(2))),((None,v(3)),(v(2),v(4))),'minus',2)==collections.Counter({(None,v(1)):1})
    assert reference_rows(((None,v(1)),),((v(2),v(3)),),'exists',2)==collections.Counter({(None,v(1),True):1})
    assert reference_rows(((None,v(1)),),((v(2),v(3)),),'optional',2)==collections.Counter({(v(2),v(1),v(3)):1})
    assert array('{{U,V(1),I(-2),T,F}}')==((None,v(1),'@integer:-2',True,False),)
    text='std::vector<IdTable> a; a.push_back(makeIdTableFromVector({{1,2}})); test(a);'
    c=Tables(text);assert flatten(c.sequence('std::move(a)',text.index('test(')),2)==((v(1),v(2)),)
    bad='std::vector<IdTable> a; a.push_back(makeIdTableFromVector({{1}})); a.pop_back(); test(a);'
    try:Tables(bad).sequence('a',bad.index('test('))
    except ValueError:pass
    else:raise AssertionError('Mutation must be rejected')


def main():
    calibration();records=[];audit=[]
    for file,(sha,helper,operation)in SOURCES.items():
        path=base.VENDOR/'qlever'/file;raw=path.read_bytes()
        actual=hashlib.sha1(('blob '+str(len(raw))+'\0').encode()+raw).hexdigest()
        if actual!=sha:raise RuntimeError('Source blob changed: '+file)
        found,entries=extract(path,helper,operation);records.extend(found);audit.extend({'source':file,**e}for e in entries)
    cases=[c for c in json.loads((base.OUT/'cases.json').read_text())if c.get('family')!='qlever-native-lazy']+records
    cases.sort(key=lambda c:(c['family'],c['source']['path'],c['name'],c['id']))
    if len({c['id']for c in cases})!=len(cases):raise RuntimeError('Duplicate lazy case IDs')
    (base.OUT/'cases.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n')
    summary={'portedCases':len(records),'helperInvocations':len(audit),'status':dict(collections.Counter(e['status']for e in audit)),'operations':dict(collections.Counter(c['nativeInputs']['operation']for c in records)),'sourceHashes':{p:x[0]for p,x in SOURCES.items()}}
    (base.OUT/'lazy-port-audit.json').write_text(json.dumps(audit,indent=2)+'\n');(base.OUT/'lazy-port-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    coverage=json.loads((base.OUT/'coverage.json').read_text());coverage.update({'cases':len(cases),'status':dict(collections.Counter(c['status']for c in cases)),'families':dict(collections.Counter(c['family']for c in cases)),'qleverLazy':summary})
    (base.OUT/'coverage.json').write_text(json.dumps(coverage,indent=2)+'\n');base.cases=cases;base.document()
    print('QLEVER_LAZY_PORTS '+json.dumps(summary),flush=True)
    print('QLEVER_LAZY_GAPS '+json.dumps([e for e in audit if e['status']=='gap'][:8]),flush=True)

if __name__=='__main__':main()
