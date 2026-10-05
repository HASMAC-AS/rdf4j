"""Source-driven logical-vector ports with exact upstream boolean/UNDEF oracles."""
from __future__ import annotations
from dataclasses import dataclass
import json,math,re
import native_adapters as native

XSD='http://www.w3.org/2001/XMLSchema#'

@dataclass(frozen=True)
class Scalar:
    sparql:str
    term:dict|None


def literal(value:str,datatype:str='string')->Scalar:
    return Scalar(json.dumps(value,ensure_ascii=True)+'^^<'+XSD+datatype+'>',{'type':'literal','value':value,'datatype':XSD+datatype})

UNDEF=Scalar('UNDEF',None)

class Resolver:
    """Resolve only immutable literal/vector declarations in the lexical call scope."""
    def __init__(self,code,c):
        self.code=code;self.masked=native.mask_source(code);self.clean=native.mask_source(code,False);self.c=c

    def resolve(self,expression:str,before:int,active_scope:tuple,seen:tuple=()):
        expression=expression.strip()
        if expression=='U':return UNDEF
        if expression in {'true','false'}:return literal(expression,'boolean')
        if re.fullmatch(r'"(?:\\.|[^"\\])*"',expression):return literal(json.loads(expression))
        wrapper=re.fullmatch(r'IdOrLocalVocabEntry\s*[({]([\s\S]*)[)}]',expression)
        if wrapper:return self.resolve(wrapper.group(1),before,active_scope,seen)
        vector=re.fullmatch(r'(?:V\s*<[^>]+>|Ids|IdOrLocalVocabEntryVec)\s*[({]([\s\S]*)[)}]',expression)
        if vector:
            args=self.c.split_args(vector.group(1))
            if not args or not args[0].strip().startswith('{'):raise ValueError('Noninitializer vector constructor')
            body=args[0].strip()
            if not body.endswith('}'):raise ValueError('Malformed vector initializer')
            if len(args)>2 or len(args)==2 and args[1]!='alloc':raise ValueError('Unsupported allocator/constructor argument')
            items=self.c.split_args(body[1:-1]) if body[1:-1].strip() else []
            values=[self.resolve(x,before,active_scope,seen) for x in items]
            if any(not isinstance(x,Scalar) for x in values):raise ValueError('Nested vector value')
            return values
        call=re.fullmatch(r'(I|D|B|lit)\s*\(([\s\S]*)\)',expression)
        if call:
            name,arg=call.groups()
            if name=='lit':
                args=self.c.split_args(arg)
                if len(args)!=1:raise ValueError('Tagged literal needs a separate source adapter')
                return literal(json.loads(args[0]))
            if name=='B':
                value=native.java_value(arg)
                if type(value)!=bool:raise ValueError('Boolean constructor needs a boolean constant')
                return literal(str(value).lower(),'boolean')
            if name=='I':
                value=native.java_value(arg)
                if type(value)!=int:raise ValueError('Integer constructor needs an integer constant')
                return literal(str(value),'integer')
            # These aliases are explicit constants at the pinned source revision.
            if arg.strip() in {'naN','nan','std::numeric_limits<double>::quiet_NaN()'}:value='NaN'
            elif arg.strip() in {'inf','std::numeric_limits<double>::infinity()'}:value='INF'
            elif arg.strip()=='negInf':value='-INF'
            else:
                number=native.java_value(arg)
                if type(number) not in {int,float}:raise ValueError('Double constructor needs a numeric constant')
                value=repr(float(number))
            return literal(value,'double')
        alias=re.fullmatch(r'([A-Za-z_]\w*)(?:\.clone\(\))?',expression)
        if not alias:raise ValueError('Unsupported literal/vector expression: '+expression[:100])
        name=alias.group(1)
        if name in seen:raise ValueError('Recursive vector alias: '+name)
        pattern=r'\b(auto|V\s*<[^>]+>|Ids|IdOrLocalVocabEntryVec|IdOrLocalVocabEntry)\s+'+re.escape(name)+r'\s*(=|\{|\()'
        candidates=[]
        for m in re.finditer(pattern,self.masked[:before]):
            declaration_scope=native.scope(self.masked,m.start())
            if active_scope[:len(declaration_scope)]==declaration_scope:candidates.append(m)
        if not candidates:raise ValueError('No literal/vector declaration for '+name)
        match=candidates[-1];end=self.masked.find(';',match.end(),before)
        if end<0:raise ValueError('Missing declaration terminator')
        between=self.masked[end+1:before]
        mutation=r'\b'+re.escape(name)+r'\s*(?:=(?!=)|\.(?:push_back|emplace_back|resize|clear|erase|insert|assign|pop_back)\s*\(|\[[^\]]*\]\s*=)'
        if re.search(mutation,between):raise ValueError('Mutated vector requires invocation expansion: '+name)
        if match.group(2)=='=':rhs=self.clean[match.end():end]
        else:rhs=match.group(1)+self.clean[match.end()-1:end]
        return self.resolve(rhs,match.start(),native.scope(self.masked,match.start()),seen+(name,))


def expand(expected,left,right):
    vectors=[x for x in (left,right) if isinstance(x,list)]
    size=max(map(len,vectors),default=1)
    if any(len(x)!=size for x in vectors):raise ValueError('Source operand vector lengths differ')
    if isinstance(expected,list):
        if len(expected)!=size:raise ValueError('Expected vector length differs from source result size')
        target=expected
    else:target=[expected]*size
    for item in target:
        if item.term is not None and item.term.get('datatype')!=XSD+'boolean':raise ValueError('Logical helper expected nonboolean ID')
    a=left if isinstance(left,list) else [left]*size
    b=right if isinstance(right,list) else [right]*size
    return target,a,b


def add(c):
    for n in c.NATIVE:
        if n['project']!='qlever' or n['name']!='SparqlExpression.logicalOperators':continue
        resolver=Resolver(n['sourceCode'],c);helpers=list(native.calls(n['sourceCode'],{'testOr','testAnd'}));rejections=[];adapted=0
        for ordinal,(start,end,name,argtext) in enumerate(helpers,1):
            try:
                args=c.split_args(argtext)
                if len(args)!=3:raise ValueError('Expected result and two operands')
                current_scope=native.scope(resolver.masked,start)
                expected,left,right=[resolver.resolve(x,start,current_scope) for x in args]
                target,a,b=expand(expected,left,right)
                op='||' if name=='testOr' else '&&'
                for order,lhs,rhs in [('forward',a,b),('reversed',b,a)]:
                    rows=' '.join('('+str(i)+' '+x.sparql+' '+y.sparql+')' for i,(x,y) in enumerate(zip(lhs,rhs)))
                    query='SELECT ?row ?result WHERE { VALUES (?row ?left ?right) { '+rows+' } BIND((?left '+op+' ?right) AS ?result) } ORDER BY ?row'
                    expected_result=c.inline(['row','result'],[[literal(str(i),'integer').term,x.term] for i,x in enumerate(target)])
                    result=c.add_native_case(n,query,expected_result,suffix=f'/{name}-{ordinal:02}-{order}')
                    result['ordered']=True;result['nativeArguments']=args;result['operandOrder']=order
                    result['source']=dict(result['source'],helperLine=n['source']['line']+n['sourceCode'].count('\n',0,start))
                    result['notes'].append('The upstream commutative helper invokes both operand orders, represented separately. Vector lanes are explicit VALUES rows and scalar operands are broadcast; expected booleans/UNDEF come only from the native expected vector. Native scalar/vector representation and interval-optimization variants are not asserted.')
                    result['notes'].append('Undefined expression results are unbound output cells. Native double inputs remain xsd:double, including NaN; they are not silently converted to decimal or boolean.')
                adapted+=1
            except (ValueError,TypeError,IndexError,KeyError,json.JSONDecodeError) as e:rejections.append({'invocation':ordinal,'helper':name,'arguments':argtext,'reason':str(e)})
        n['helperInvocations']=len(helpers);n['adaptedHelperInvocations']=adapted;n['adapterRejections']=rejections
        if rejections and n['portedCases']:n['disposition']='partially-adapted'
        elif not rejections and n['portedCases']:n['disposition']='logical-value-assertions-adapted-internal-metadata-unported'
