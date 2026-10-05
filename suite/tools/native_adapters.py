"""Conservative native-source adapters. Unsupported source constructs stay unported."""
from __future__ import annotations
import ast, collections, json, operator, re
from pathlib import Path


def mask_source(text: str, strings: bool = True) -> str:
    """Replace comments/string tokens with spaces, preserving source offsets and newlines."""
    out=list(text);i=0
    def blank(a,b):
        for j in range(a,b):
            if out[j] not in '\r\n':out[j]=' '
    while i<len(text):
        start=i
        if text.startswith('//',i):
            end=text.find('\n',i);end=len(text) if end<0 else end;blank(i,end);i=end;continue
        if text.startswith('/*',i):
            end=text.find('*/',i+2);end=len(text) if end<0 else end+2;blank(i,end);i=end;continue
        raw=re.match(r'R"([^ ()\\\t\r\n]{0,16})\(',text[i:])
        if raw:
            tail=')'+raw.group(1)+'"';end=text.find(tail,i+raw.end());end=len(text) if end<0 else end+len(tail)
            if strings:blank(i,end)
            i=end;continue
        if text.startswith('"""',i):
            end=text.find('"""',i+3);end=len(text) if end<0 else end+3
            if strings:blank(i,end)
            i=end;continue
        if text[i] in '\"\'':
            quote=text[i];i+=1
            while i<len(text):
                if text[i]=='\\':i+=2;continue
                if text[i]==quote:i+=1;break
                i+=1
            if strings:blank(start,min(i,len(text)))
            continue
        i+=1
    return ''.join(out)


def calls(text: str, names: set[str]):
    masked=mask_source(text)
    pattern=r'\b('+ '|'.join(re.escape(x) for x in sorted(names))+r')\s*\('
    for m in re.finditer(pattern,masked):
        depth=1;i=m.end()
        while i<len(masked) and depth:
            if masked[i]=='(':depth+=1
            elif masked[i]==')':depth-=1
            i+=1
        if depth:raise ValueError('Unbalanced helper call')
        yield m.start(),i,m.group(1),text[m.end():i-1]


def discover_native(c):
    """Discard commented-out annotations and string-embedded test declarations."""
    for project in c.SOURCES:
        for p in c.files(c.VENDOR/project):
            if p.suffix not in {'.java','.cpp','.cc'} or not any('test' in part.lower() for part in p.parts):continue
            text=p.read_text(encoding='utf8',errors='replace');masked=mask_source(text)
            if p.suffix=='.java':
                pat=r'@(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b[\s\S]{0,1200}?\b(?:public\s+|protected\s+|private\s+)?(?:static\s+)?(?:void|Stream\s*<[^>]+>|Collection\s*<[^>]+>)\s+(\w+)\s*\([^)]*\)[^{;]*\{'
            else:pat=r'\b(?:TEST|TEST_F|TEST_P|TYPED_TEST|TYPED_TEST_P)\s*\(\s*(\w+)\s*,\s*(\w+)\s*\)\s*\{'
            for m in re.finditer(pat,masked):
                start=masked.rfind('{',m.start(),m.end());end=c.end_block(text,start)
                name=p.stem+'.'+m.group(1) if p.suffix=='.java' else m.group(1)+'.'+m.group(2)
                snippet=text[m.start():end];line=text.count('\n',0,m.start())+1
                relevant=bool(re.search(r'(?i)sparql|QueryExecution|ExprUtils|SELECT\s|ASK\s|CONSTRUCT\s|testExists|testBoolean|testNumeric|eval\(',mask_source(snippet,False)))
                c.NATIVE.append({'id':project+'-native-'+c.sha((str(p.relative_to(c.VENDOR/project))+':'+str(line)+':'+name).encode())[:20], 'name':name,'project':project,'source':c.source(project,p,line),'sourceEndLine':text.count('\n',0,end)+1,'sourceCode':snippet,'queryCandidate':relevant,'portedCases':[],'parameterExpansion':'not expanded' if re.search(r'ParameterizedTest|RepeatedTest|TEST_P|TYPED_TEST',m.group()) else 'not parameterized','disposition':'manual-review-required' if relevant else 'native-unit-test-not-classified-as-query'})


def java_value(text: str):
    """Evaluate only literal arithmetic/boolean expressions; never execute arbitrary source."""
    text=re.sub(r'(?<=\d)[lL]\b','',text.strip())
    text=re.sub(r'\btrue\b','True',text);text=re.sub(r'\bfalse\b','False',text)
    text=text.replace('&&',' and ').replace('||',' or ');text=re.sub(r'!(?!=)',' not ',text).strip()
    root=ast.parse(text,mode='eval').body
    binary={ast.Add:operator.add,ast.Sub:operator.sub,ast.Mult:operator.mul,ast.LShift:operator.lshift,ast.RShift:operator.rshift}
    compare={ast.Eq:operator.eq,ast.NotEq:operator.ne,ast.Lt:operator.lt,ast.LtE:operator.le,ast.Gt:operator.gt,ast.GtE:operator.ge}
    def visit(n):
        if isinstance(n,ast.Constant) and type(n.value) in {str,int,float,bool}:return n.value
        if isinstance(n,ast.UnaryOp):
            v=visit(n.operand)
            if isinstance(n.op,ast.USub):return -v
            if isinstance(n.op,ast.UAdd):return +v
            if isinstance(n.op,ast.Not):return not v
        if isinstance(n,ast.BinOp) and type(n.op) in binary:return binary[type(n.op)](visit(n.left),visit(n.right))
        if isinstance(n,ast.BoolOp):return all(map(visit,n.values)) if isinstance(n.op,ast.And) else any(map(visit,n.values))
        if isinstance(n,ast.Compare):
            left=visit(n.left)
            for op,r in zip(n.ops,n.comparators):
                right=visit(r)
                if type(op) not in compare or not compare[type(op)](left,right):return False
                left=right
            return True
        raise ValueError('Unsupported Java constant expression: '+ast.dump(n))
    return visit(root)


def string_value(expr: str, constants: dict[str,str]) -> str:
    replacements=[];i=0
    for m in re.finditer(r'"(?:\\.|[^"\\])*"|[A-Za-z_$][\w.$]*(?:\(\))?|\+|\s+',expr):
        if m.start()!=i:raise ValueError('Unsupported string expression near '+expr[i:])
        token=m.group();i=m.end()
        if token.isspace() or token=='+':continue
        if token.startswith('"'):replacements.append(json.loads(token));continue
        datatype=re.fullmatch(r'XSDDatatype\.XSD(\w+)\.getURI\(\)',token)
        vocab=re.fullmatch(r'(RDF|XSD)\.(\w+)\.getURI\(\)',token)
        if datatype:replacements.append('http://www.w3.org/2001/XMLSchema#'+datatype.group(1))
        elif token in {'RDF.getURI()','XSD.getURI()'}:replacements.append('http://www.w3.org/1999/02/22-rdf-syntax-ns#' if token.startswith('RDF') else 'http://www.w3.org/2001/XMLSchema#')
        elif token=='XSDDatatype.XSD':replacements.append('http://www.w3.org/2001/XMLSchema')
        elif vocab:
            ns='http://www.w3.org/1999/02/22-rdf-syntax-ns#' if vocab.group(1)=='RDF' else 'http://www.w3.org/2001/XMLSchema#'
            replacements.append(ns+('string' if vocab.group(2)=='xstring' else vocab.group(2)))
        elif token in constants:replacements.append(constants[token])
        else:raise ValueError('Unresolved Java string constant '+token)
    if i!=len(expr):raise ValueError('Unsupported string expression '+expr[i:])
    return ''.join(replacements)


def jena_expressions(c):
    p=c.VENDOR/'jena/jena-arq/src/test/java/org/apache/jena/sparql/expr/TestExpressions.java'
    if not p.exists():return
    text=p.read_text();clean=mask_source(text,False);constants={}
    declarations=re.findall(r'static\s+(?:final\s+)?String\s+(\w+)\s*=\s*([^;]+);',clean)
    for _ in range(len(declarations)+1):
        changed=False
        for name,expr in declarations:
            if name in constants:continue
            try:constants[name]=string_value(expr,constants);changed=True
            except ValueError:pass
        if not changed:break
    prefix=''
    for m in re.finditer(r'query\.setPrefix\(\s*("(?:\\.|[^"\\])*")\s*,\s*([^;]+)\);',clean):
        try:prefix+='PREFIX '+json.loads(m.group(1))+': <'+string_value(m.group(2),constants)+'>\n'
        except ValueError:pass
    base=constants.get('baseNS','http://base/')
    env='VALUES (?a ?x) { ("A" <urn:ex:abcd>) } BIND(BNODE() AS ?b) '
    names={'testBoolean','testString','testNumeric','testURI','testEval','testSyntax','testVar'}
    for n in c.NATIVE:
        if n['project']!='jena' or n['source']['path']!=p.relative_to(c.VENDOR/'jena').as_posix():continue
        code=n['sourceCode'];items=list(calls(code,names))
        if len(items)!=1:continue
        start,end,helper,argtext=items[0];args=c.split_args(argtext)
        if helper=='testVar':n['disposition']='parser-token-structure-not-a-query-result';continue
        try:
            expression=string_value(args[0],constants)
            negative=bool(re.search(r'assertThrows\s*\(\s*QueryParseException\.class',code))
            error=bool(re.search(r'assertThrows\s*\(\s*ExprEvalException\.class',code))
            bindings=env if helper in {'testURI','testString'} or helper=='testBoolean' and len(args)==3 and args[2]=='env' else ''
            query=prefix+'SELECT ?result WHERE { '+bindings+'BIND(('+expression+') AS ?result) }'
            if negative:expected=None;kind='negative-syntax'
            elif helper=='testSyntax':expected=None;kind='positive-syntax'
            elif error:expected=c.inline(['result'],[[None]]);kind='query'
            else:
                kind='query';assertion='bound';value=None
                if helper=='testBoolean':assertion='boolean';value=java_value(args[1])
                elif helper=='testString':assertion='string';value=string_value(args[1],constants) if len(args)>1 else None
                elif helper=='testURI':assertion='iri';value=string_value(args[1],constants)
                elif helper=='testEval' and len(args)>1:
                    expected=c.inline(['result'],[[c.term_from_sparql(string_value(args[1],constants))]])
                    assertion=None
                elif helper=='testNumeric':
                    big=re.fullmatch(r'new\s+(BigDecimal|BigInteger)\s*\(\s*("(?:\\.|[^"\\])*")\s*\)',args[1])
                    if big:assertion='decimal-scale' if big.group(1)=='BigDecimal' else 'integer';value=json.loads(big.group(2))
                    else:
                        value=java_value(args[1]);assertion='numeric-double' if isinstance(value,float) else 'integer64' if re.search(r'\d[Ll]\b',args[1]) else 'integer32';value=str(value)
                if assertion is not None:expected={'type':'native-assertion','variable':'result','assertion':assertion,'value':value}
            result=c.add_native_case(n,query,expected,kind=kind,baseuri=base)
            result['nativeHelper']=helper;result['decodedExpression']=expression;result['resolvedConstants']=constants
            result['notes'].append('Preserves the native helper comparison: boolean/string/numeric value checks are not silently replaced by stricter lexical RDF-term equality. Numeric categories follow Jena NodeValue conversion capabilities.')
            if helper=='testURI':result['notes'].append('Jena testURI uses partial-expression parsing; the query wrapper requires a complete expression. No trailing-token case is silently trimmed.')
            if 'strictMode(' in code:result['requires'].append('strict RDF triple-term construction');result['notes'].append('Original Jena assertion enables strictSPARQL for the expression.')
        except (ValueError,KeyError,SyntaxError,IndexError) as e:
            n['adapterRejection']=str(e)


def scope(masked: str, position: int):
    stack=[]
    for i,ch in enumerate(masked[:position]):
        if ch=='{':stack.append(i)
        elif ch=='}' and stack:stack.pop()
    return tuple(stack)


def optional_joins(c):
    for n in c.NATIVE:
        if n['project']!='qlever' or not n['source']['path'].endswith('/OptionalJoinTest.cpp'):continue
        code=n['sourceCode'];masked=mask_source(code);clean=mask_source(code,False)
        targets=list(calls(code,{'testOptionalJoin'}))
        if not targets or re.search(r'\b(?:for|while)\s*\(',masked):continue
        def matrix(expression, before, current_scope, seen=()):
            expression=expression.strip()
            if re.fullmatch(r'[A-Za-z_]\w*(?:\.clone\(\))?',expression):
                name=expression.split('.')[0]
                if name in seen:raise ValueError('Recursive table alias')
                matches=[]
                pat=r'\b(?:auto|IdTable)\s+'+re.escape(name)+r'\s*(=|\(|\{)'
                for m in re.finditer(pat,masked[:before]):
                    decl_scope=scope(masked,m.start())
                    if current_scope[:len(decl_scope)]==decl_scope:matches.append(m)
                if not matches:raise ValueError('Unresolved table '+name)
                m=matches[-1]
                # Statements in this reviewed subset have no semicolons inside constructor arguments.
                end=masked.find(';',m.end(),before)
                if end<0:raise ValueError('Missing declaration terminator')
                tail=masked[end+1:before]
                if re.search(r'\b'+re.escape(name)+r'\s*(?:\.(?!clone\b)\w+\s*\(|=(?!=)|\[)',tail):raise ValueError('Intervening table mutation/use needs review: '+name)
                if m.group(1)=='=':return matrix(clean[m.end():end],m.start(),scope(masked,m.start()),seen+(name,))
                args=c.split_args(clean[m.end():end].rstrip(')} '))
                width=int(args[0]);return width,[]
            m=re.fullmatch(r'(?:\w+::)*makeIdTableFromVector\s*\(([\s\S]*)\)',expression)
            if m:
                args=c.split_args(m.group(1))
                if len(args)!=1:raise ValueError('Nondefault ID conversion needs a typed adapter')
                rows=c.cpp_table(args[0]);width=len(rows[0]) if rows else None
                if width is None:raise ValueError('Empty initializer has no declared width')
                if any(len(r)!=width or any(x is not None and type(x)!=int for x in r) for r in rows):raise ValueError('Nonrectangular or nonconstant table')
                return width,rows
            m=re.fullmatch(r'IdTable\s*[({]\s*(\d+)\s*,[\s\S]*[)}]',expression)
            if m:return int(m.group(1)),[]
            raise ValueError('Unsupported table expression '+expression[:120])
        failures=[]
        for index,(pos,end,name,argtext) in enumerate(targets,1):
            try:
                args=c.split_args(argtext)
                if len(args)!=4:raise ValueError('Expected four helper arguments')
                s=scope(masked,pos);lw,left=matrix(args[0],pos,s);rw,right=matrix(args[1],pos,s);ew,expected=matrix(args[3],pos,s)
                joins=c.cpp_table(args[2])
                if any(len(pair)!=2 for pair in joins):raise ValueError('Join column pair malformed')
                lv=['left'+str(i) for i in range(lw)];rv=['right'+str(i) for i in range(rw)]
                if len({a for a,b in joins})!=len(joins) or len({b for a,b in joins})!=len(joins):raise ValueError('Repeated join column requires an extra equality adapter')
                for j,(a,b) in enumerate(joins):lv[a]=rv[b]='join'+str(j)
                projected=lv+[v for v in rv if v not in lv]
                if len(projected)!=ew:raise ValueError('Expected output-column layout differs from helper contract')
                query='SELECT '+' '.join('?'+v for v in projected)+' WHERE { '+c.values_clause(lv,left)+' OPTIONAL { '+c.values_clause(rv,right)+' } }'
                result=c.add_native_case(n,query,c.inline(projected,[[c.qid(x) for x in row] for row in expected]),suffix=f'/vector-{index:02}')
                result['nativeArguments']=args;result['source']['helperLine']=n['source']['line']+code.count('\n',0,pos)
                result['notes'].append('Native sorted-output assertions become unordered SPARQL bags; join columns and left-then-unshared-right output order are preserved. Table mutations and parameter loops are rejected rather than guessed.')
            except (ValueError,IndexError,TypeError,json.JSONDecodeError) as e:failures.append({'invocation':index,'reason':str(e)})
        n['helperInvocations']=len(targets);n['adapterRejections']=failures
        if failures and n['portedCases']:n['disposition']='partially-adapted'


def fix_exists_queries(c):
    """Use explicit compatibility predicates instead of ambiguous substitution into VALUES."""
    for result in c.CASES:
        if result['project']!='qlever' or not result['name'].startswith('ExistsJoin.computeResult/'):continue
        args=result['nativeArguments'];left,right=map(c.cpp_table,args[:2]);joins=int(args[3])
        lw=len(left[0]) if left else 2;rw=len(right[0]) if right else 2
        lv=[('join'+str(i)) if i<joins else 'left'+str(i) for i in range(lw)];rv=['rhs'+str(i) for i in range(rw)]
        compatibility=' && '.join('(!BOUND(?'+lv[i]+') || !BOUND(?'+rv[i]+') || sameTerm(?'+lv[i]+', ?'+rv[i]+'))' for i in range(joins))
        inner=c.values_clause(rv,right)+(' FILTER('+compatibility+')' if compatibility else '')
        result['query']='SELECT '+' '.join('?'+v for v in lv)+' ?exists WHERE { '+c.values_clause(lv,left)+' BIND(EXISTS { '+inner+' } AS ?exists) }'
        result['notes'].append('RHS variables are renamed and join compatibility is explicit in FILTER. This avoids relying on engine-specific substitution of outer variables into VALUES column declarations.')


def annotate_requirements(c):
    for case in c.CASES:
        q=case['query'];path=case['source']['path'];requirements=[]
        if 'SPARQL-CDTs' in q or 'SPARQL-CDTs' in path:requirements.append('SPARQL-CDTs extension functions/datatypes')
        if re.search(r'http://(?:jena\.apache\.org/ARQ|jena\.hpl\.hp\.com/ARQ)',q):requirements.append('Jena ARQ extension functions/property functions')
        if '/geosparql' in q.lower() or '/geosparql' in path.lower():requirements.append('GeoSPARQL functions/index configuration')
        if re.search(r'\b(?:LANGDIR|STRLANGDIR|HASLANG|HASLANGDIR|ISTRIPLE|SUBJECT|PREDICATE|OBJECT|TRIPLE)\s*\(',q,re.I) or '<<(' in q:requirements.append('RDF/SPARQL triple terms or SPARQL 1.2 feature')
        case['requires']=sorted(set(case['requires']+requirements))
        case['scope']='extension-or-capability' if requirements else 'standard-or-unclassified'
        case['notes']+=['Capability annotation does not alter the upstream expected result or turn an observed failure into a pass.'] if requirements else []


def audit(c):
    counts=collections.Counter(n['source']['path'] for n in c.NATIVE if n['queryCandidate'] and not n['portedCases'])
    return {'unportedCandidateFiles':[{'path':p,'declarations':n} for p,n in counts.most_common(40)],'nativeAdapters':dict(collections.Counter(n['disposition'] for n in c.NATIVE)),'nativeParameterDeclarations':sum(n['parameterExpansion']=='not expanded' for n in c.NATIVE),'commentedDeclarationsExcluded':True}
