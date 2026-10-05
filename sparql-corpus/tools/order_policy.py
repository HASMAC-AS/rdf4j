"""Extract observable outer ORDER BY keys; never mistake subquery ordering for output ordering."""
from __future__ import annotations
import re
IRI=re.compile(r'<[^<>\x00-\x20]*>')
KEY=re.compile(r'(?:(ASC|DESC)\s*\(\s*([?$]\w+)\s*\)|([?$]\w+))',re.I)

def top_level(query: str) -> str:
    result=[]; depth=0; i=0
    while i<len(query):
        c=query[i]
        if c=='#':
            end=query.find('\n',i);end=len(query) if end<0 else end
            result.append(' '*(end-i));i=end;continue
        if c in {'"',"'"}:
            delimiter=c*3 if query.startswith(c*3,i) else c
            start=i;i+=len(delimiter)
            while i<len(query):
                if query[i]=='\\':i+=2
                elif query.startswith(delimiter,i):i+=len(delimiter);break
                else:i+=1
            result.append(' '*(i-start));continue
        match=IRI.match(query,i) if c=='<' else None
        if match:
            result.append(' '*(match.end()-i));i=match.end();continue
        if c=='{':depth+=1;result.append(' ')
        elif c=='}':depth=max(0,depth-1);result.append(' ')
        else:result.append(c if depth==0 else ' ')
        i+=1
    return ''.join(result)

def extract(query: str):
    text=top_level(query)
    start=re.search(r'(?<![\w:?$])ORDER\s+BY\b',text,re.I)
    if not start:return None,None
    clause=re.split(r'\b(?:LIMIT|OFFSET|VALUES|BINDINGS)\b',text[start.end():],maxsplit=1,flags=re.I)[0]
    keys=[];pos=0
    while pos<len(clause):
        if clause[pos].isspace():pos+=1;continue
        match=KEY.match(clause,pos)
        if not match:return [],'ORDER BY expression or non-variable key needs an expression-specific oracle'
        keys.append({'variable':(match.group(2) or match.group(3))[1:],'ascending':(match.group(1) or 'ASC').upper()=='ASC'})
        pos=match.end()
    return (keys,None) if keys else ([], 'Empty ORDER BY clause cannot be checked')

def apply(case: dict, block) -> None:
    if case.get('kind')!='evaluation' or case.get('expected',{}).get('kind') not in {'tuple','tuple-file'}:return
    keys,error=extract(case.get('query',''))
    if keys is None:return
    if error:block(case,error);return
    variables=case['expected'].get('vars')
    if variables is not None and any(k['variable'] not in variables for k in keys):
        block(case,'ORDER BY uses a non-projected variable; its key is unavailable in the result fixture');return
    case['orderBy']=keys;case['comparison']='bag plus SPARQL partial-order verification'
    case['expected']['ordered']=False
    note='Projected-variable outer ORDER BY is checked independently. Valid ties and specification-undefined relative orders are allowed. Complex or hidden order expressions require another oracle.'
    if note not in case.setdefault('limitations',[]):case['limitations'].append(note)
