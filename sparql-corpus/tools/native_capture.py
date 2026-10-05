#!/usr/bin/env python3
"""Compile original Jena helper callers with argument-recording terminals, then import the source oracles.

No actual RDF4J/Jena result is used to invent a golden answer. Java executes original
argument construction and source-specified expected expressions only. The unmodified
vendor tree remains the provenance source; transformed compilation files live in target/.
"""
from __future__ import annotations
import argparse, collections, hashlib, importlib.util, json, os, re, shutil, subprocess, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
TOOLS=ROOT/'tools'; OUT=ROOT/'corpus'; WORK=ROOT/'target/native-capture'
PIN=json.loads((ROOT/'sources.json').read_text())['jena']


def run(command, **kwargs):
    print('CAPTURE_COMMAND '+json.dumps([str(s) for s in command]),flush=True)
    subprocess.run([str(s) for s in command],check=True,**kwargs)


def read_lines(path):
    return [json.loads(line) for line in path.read_text().splitlines() if line.strip()]


def reference():
    configured=os.getenv('JENA_REFERENCE_ROOT')
    if configured:
        root=Path(configured).resolve(); libs=Path(os.environ['JENA_REFERENCE_LIBS']).resolve()
    else:
        root=ROOT/'target/jena-reference'; libs=ROOT/'target/jena-reference-libs'
        if not (root/'jena-arq/target/test-classes').is_dir():
            shutil.copytree(ROOT/'vendor/jena',root,dirs_exist_ok=True,ignore=shutil.ignore_patterns('.git','target','_archives','*.expanded'))
            run(['mvn','-B','-ntp','-f',root/'pom.xml','-pl','jena-arq','-am','-DskipTests','-Dmaven.javadoc.skip=true','install'])
        if not libs.is_dir():
            run(['mvn','-B','-ntp','-f',root/'jena-arq/pom.xml','org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies','-DincludeScope=test','-DoutputDirectory='+str(libs)])
    if not (root/'jena-arq/target/test-classes').is_dir():
        raise RuntimeError('Pinned Jena reference build has no ARQ test classes: '+str(root))
    # Compiling against another reference version could change source-derived constants.
    probes=['pom.xml','jena-arq/src/main/java/org/apache/jena/sparql/expr/NodeValue.java','jena-core/src/main/java/org/apache/jena/graph/Node.java']
    for p in probes:
        if (root/p).read_bytes()!=(ROOT/'vendor/jena'/p).read_bytes():
            raise RuntimeError('Reference source does not match the pinned fork: '+p)
    directories=sorted(p for p in root.glob('*/target/*') if p.name in {'classes','test-classes'} and p.is_dir())
    jars=sorted(libs.glob('*.jar'))
    if not jars:raise RuntimeError('Reference dependency jars missing: '+str(libs))
    return os.pathsep.join(map(str,[*directories,*jars]))


def extract():
    WORK.mkdir(parents=True,exist_ok=True)
    bootstrap=WORK/'bootstrap'; transformed=WORK/'transformed'; classes=WORK/'classes'
    for p in (bootstrap,transformed,classes):p.mkdir(exist_ok=True)
    cp=reference()
    run(['javac','-encoding','UTF-8','-cp',cp,'-d',bootstrap,*sorted((TOOLS/'capture').glob('*.java'))])
    metadata=OUT/'jena-capture-source-index.jsonl'
    run(['java','-cp',str(bootstrap)+os.pathsep+cp,'org.hasmac.capture.SourceTransformer',ROOT/'vendor/jena/jena-arq/src/test/java',transformed,metadata])
    source=read_lines(metadata)
    files=sorted(transformed.rglob('*.java'))
    args=WORK/'javac-sources.txt';args.write_text('\n'.join('"'+str(p)+'"' for p in files)+'\n')
    run(['javac','-encoding','UTF-8','-cp',str(bootstrap)+os.pathsep+cp,'-d',classes,'@'+str(args)])
    class_file=WORK/'classes.txt';class_file.write_text('\n'.join(x['class'] for x in source)+'\n')
    run(['java','-Xmx2g','-Duser.timezone=UTC','-Duser.language=en','-Duser.country=US','-cp',os.pathsep.join([str(classes),str(bootstrap),cp]),'org.hasmac.capture.Capture',OUT/'jena-capture-events.jsonl',OUT/'jena-capture-audit.jsonl',class_file])
    return source


def predicate_descriptor(event, method):
    calls=[c for c in method.get('invocations',[]) if c.get('line')==event.get('callLine') and c.get('name','').split('.')[-1]=='test' and len(c.get('arguments',[]))==2]
    if len(calls)!=1:return None
    text=calls[0]['arguments'][1].strip()
    match=re.fullmatch(r'NodeValue::(is[A-Za-z]+)',text)
    if not match:
        match=re.fullmatch(r'\(?\s*([A-Za-z_$][\w$]*)\s*\)?\s*->\s*\1\.(is[A-Za-z]+)\(\s*\)',text)
        name=match.group(2) if match else None
    else:name=match.group(1)
    supported={'isIRI','isBlank','isLiteral','isString','isBoolean','isInteger','isDecimal','isDouble','isFloat','isNumber','isDateTime','isDayTimeDuration','isYearMonthDuration','isDuration'}
    return {'mode':'predicate','predicate':name,'sourcePredicate':text} if name in supported else None


def import_events(source):
    original=json.loads((OUT/'cases.json').read_text())
    index={(c['class'],m['name']):(c,m) for c in source for m in c['methods']}
    events=read_lines(OUT/'jena-capture-events.jsonl'); records=[]
    for event in events:
        key=(event['class'],event['method'])
        if key not in index:
            # Inherited methods are attributed to their declaring source, never guessed.
            raise RuntimeError('Captured method lacks source metadata: '+str(key))
        cls,method=index[key];path='jena-arq/src/test/java/'+cls['file'];raw=(ROOT/'vendor/jena'/path).read_bytes()
        query=''.join('PREFIX '+p+': <'+iri+'>\n' for p,iri in sorted(event['prefixes'].items()))
        query+='SELECT ?result WHERE { BIND(('+event['expression']+') AS ?result) }'
        name=event['class'].split('.')[-1]+'.'+event['method']+'.'+event['invocation']+'.'+str(event['ordinal'])
        identifier='jena-capture-'+hashlib.sha256((path+'#'+name).encode()).hexdigest()[:20]
        gold=event['expected'].copy();mode=gold['mode'];status='ready';limitations=[]
        if mode=='predicate-source':
            descriptor=predicate_descriptor(event,method)
            if descriptor:gold=descriptor;mode=gold['mode']
            else:status='blocked';limitations.append('Original predicate assertion requires an additional observable-value predicate adapter.')
        if mode=='unsupported-exception':status='blocked';limitations.append('Original expected exception has no established query-level mapping: '+gold['exception'])
        if not event.get('methodCompleted') or event.get('captureGaps'):
            status='blocked';limitations.extend(event.get('captureGaps') or ['Original method capture did not complete.'])
        expected={'kind':'expression',**gold}
        kind='evaluation'
        if mode=='syntax-negative':kind='syntax-negative';expected={'kind':'syntax-negative','sourceException':gold.get('exception')}
        elif mode=='unbound':expected={'kind':'tuple','vars':['result'],'rows':[{}],'sourceHelper':event['helper'],'sourceException':gold.get('exception')}
        limitations.extend([
            'The original Java compiler and original test method construct helper arguments. The instrumented helper records the source expectation instead of evaluating the actual expression.',
            'Expression lowered to a one-solution SELECT/BIND; evaluation errors leave result unbound. Native expression versus complete-query parsing differences remain adaptation boundaries.',
            'The source comparison mode is preserved. Exact-term expectations are implementation-specific where SPARQL permits other value-equivalent lexical forms.',
            'Capture locale is Locale.ROOT and timezone UTC. Runtime values or unsupported predicates are not converted into hard-coded expected output.'
        ])
        c={'id':identifier,'name':name,'family':'jena-native-captured','kind':kind,'status':status,
           'source':{'repository':PIN['repository'],'revision':PIN['revision'],'path':path,'line':method['line'],
             'url':'https://github.com/'+PIN['repository']+'/blob/'+PIN['revision']+'/'+path+'#L'+str(method['line']),
             'sha256':hashlib.sha256(raw).hexdigest(),'gitBlobSha1':hashlib.sha1(('blob '+str(len(raw))+'\0').encode()+raw).hexdigest()},
           'base':'http://base/','query':query,'querySha256':hashlib.sha256(query.encode()).hexdigest(),'fixtures':[],
           'expected':expected,'sourceAssertion':method['sourceAssertion'],'sourceExpression':event['expression'],'sourceHelper':event['helper'],
           'sourceInvocation':{'class':event['class'],'method':event['method'],'invocation':event['invocation'],'ordinal':event['ordinal'],'line':event.get('callLine')},
           'comparison':mode,'limitations':limitations}
        records.append(c)
    # This cohort is idempotent. Older hand-reviewed adapters are retained, not silently replaced.
    cases=[c for c in original if c.get('family')!='jena-native-captured']+records
    cases.sort(key=lambda c:(c['family'],c['source']['path'],c['name'],c['id']))
    if len({c['id'] for c in cases})!=len(cases):raise RuntimeError('Duplicate IDs after native capture')
    (OUT/'cases.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n')
    audit=read_lines(OUT/'jena-capture-audit.jsonl')
    summary={'capturedCases':len(records),'status':dict(collections.Counter(c['status'] for c in records)),
             'classes':len(source),'sourceMethods':sum(len(c['methods']) for c in source),
             'methodInvocations':len(audit),'invocationsWithCapture':sum(x.get('captured',0)>0 for x in audit),
             'comparisonModes':dict(collections.Counter(c['comparison'] for c in records)),
             'byClass':dict(collections.Counter(c['sourceInvocation']['class'] for c in records)),
             'referenceRevision':PIN['revision'],'expectedAnswersFromActualEngineExecution':False}
    (OUT/'jena-capture-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    native_path=OUT/'native-inventory.json'
    if native_path.exists():
        native=json.loads(native_path.read_text());mapped=collections.defaultdict(list)
        for c in records:mapped[(c['source']['path'],c['sourceInvocation']['method'])].append(c['id'])
        for row in native:
            ids=mapped.get((row.get('path','').removeprefix('vendor/jena/'),row.get('name')))
            if ids:row['capturedCaseIds']=ids;row['captureScope']='helper-invocations; other native assertions may remain';row['disposition']='adapted-query'
        native_path.write_text(json.dumps(native,indent=2)+'\n')
    coverage=json.loads((OUT/'coverage.json').read_text());coverage.update({'cases':len(cases),'status':dict(collections.Counter(c['status'] for c in cases)),
      'families':dict(collections.Counter(c['family'] for c in cases)),'jenaNativeCapture':summary})
    (OUT/'coverage.json').write_text(json.dumps(coverage,indent=2)+'\n')
    spec=importlib.util.spec_from_file_location('corpus_base',TOOLS/'import.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
    base.cases=cases;base.document()
    print('NATIVE_CAPTURE_SUMMARY '+json.dumps(summary),flush=True)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--import-only',action='store_true');args=parser.parse_args()
    source=read_lines(OUT/'jena-capture-source-index.jsonl') if args.import_only else extract()
    import_events(source)

if __name__=='__main__':main()
