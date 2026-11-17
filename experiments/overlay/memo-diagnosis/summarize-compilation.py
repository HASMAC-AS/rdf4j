# Copyright (c) 2026 Eclipse RDF4J contributors.
# SPDX-License-Identifier: BSD-3-Clause
# Written by Codex: read-only reduction of completed HotSpot diagnostic logs.
import json, pathlib, xml.etree.ElementTree as E
base=pathlib.Path(__file__).resolve().parents[1]/'workbench-e2e'
output={}
for label in ('baseline','entry'):
 p=base/f'memo-jit-{label}'
 assert json.load(open(str(p)+'.json'))['witnessesPassed'] is True
 root=E.parse(str(p)+'-compilation.xml').getroot()
 assert root.find('hotspot_log_done') is not None
 ns=[n for n in root.iter('nmethod') if any(x in n.get('method','') for x in ('NativeValueMemo value','NativeProjectedBindingSet collectMaterializationValues'))]
 tasks={t.get('compile_id'):t for t in root.iter('task')}
 result={'nmethods':[{k:n.get(k) for k in ('compile_id','method','compiler','level','size','bytes','stamp')} for n in ns], 'final':[],'memo_call_sites':[]}
 finals={n.get('method'):n for n in ns if n.get('compiler')=='c2'}
 def describe(t):
  ids={e.get('id'):e for e in t.iter() if e.tag in ('method','klass','type')}
  def name(i):
   e=ids.get(i)
   if e is None:return i
   return (name(e.get('holder'))+'.' if e.tag=='method' else '')+e.get('name','')
  calls=[]
  for parse in t.iter('parse'):
   bci=None;call=None
   for e in parse:
    if e.tag=='bc':bci=e.get('bci');call=None
    elif e.tag=='call':
     call={'parent':name(parse.get('method')),'bci':bci,'callee':name(e.get('method')),'profile':dict(e.attrib),'outcome':[]};calls.append(call)
    elif e.tag in ('inline_success','inline_fail','intrinsic','direct_call','virtual_call') and call is not None:call['outcome'].append({'kind':e.tag,**e.attrib})
  return calls
 for method,n in finals.items():
  t=tasks[n.get('compile_id')]
  result['final'].append({'method':method,'compile_id':n.get('compile_id'),'done':t.find('task_done').attrib,'failures':[x.attrib for x in t.iter('failure')],'calls':describe(t)})
 for t in tasks.values():
  for c in describe(t):
   if c['callee'].endswith('NativeValueMemo.value') or c['callee'].endswith('NativeProjectedBindingSet.collectMaterializationValues'):
    result['memo_call_sites'].append({'root':t.get('method'),'compile_id':t.get('compile_id'),**c})
 output[label]=result
path=pathlib.Path(__file__).with_name('compilation-summary.json');path.write_text(json.dumps(output,indent=2)+'\n')
for label,r in output.items():
 print('\n',label)
 for f in r['final']:
  print(f['method'],f['compile_id'],f['done'])
  for c in f['calls']:
   if any(k in c['callee'] for k in ('Reference.get','lazyValue','NativeValueMemo.value')):print(c)
 print('memo callsite outcomes',[(c['compile_id'],c['root'].split(' ')[1],c['callee'].split('.')[-1],c['outcome']) for c in r['memo_call_sites']])
