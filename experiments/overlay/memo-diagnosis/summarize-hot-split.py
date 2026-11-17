# Copyright (c) 2026 Eclipse RDF4J contributors.
# SPDX-License-Identifier: BSD-3-Clause
# Written by Codex: completed-log analysis only.
import json,pathlib,xml.etree.ElementTree as E
base=pathlib.Path(__file__).resolve().parents[1]/'workbench-e2e'
p=base/'hot-split-profile'
assert json.load(open(str(p)+'.json'))['witnessesPassed']
r=E.parse(str(p)+'-compilation.xml').getroot()
assert r.find('hotspot_log_done') is not None
nm={n.get('compile_id'):n.attrib for n in r.iter('nmethod')}
out=[]
for t in r.iter('task'):
 if nm.get(t.get('compile_id'),{}).get('compiler')!='c2':continue
 ids={e.get('id'):e for e in t.iter() if e.tag in ('method','klass','type')}
 def name(i):
  e=ids.get(i)
  return i if e is None else (name(e.get('holder'))+'.' if e.tag=='method' else '')+e.get('name','')
 calls=[]
 for parse in t.iter('parse'):
  bci=None;c=None
  for e in parse:
   if e.tag=='bc':bci=e.get('bci');c=None
   elif e.tag=='call':
    c={'parent':name(parse.get('method')),'bci':bci,'callee':name(e.get('method')),'decisions':[]};calls.append(c)
   elif e.tag in ('inline_success','inline_fail','intrinsic','direct_call','virtual_call') and c is not None:c['decisions'].append({'kind':e.tag,**e.attrib})
 calls=[c for c in calls if any(s in c['callee'] for s in ('NativeValueMemo.','collectMaterializationValues','Reference.get','lazyValue'))]
 if calls:
  out.append({'root':t.attrib,'nmethod':nm[t.get('compile_id')],'done':t.find('task_done').attrib,'calls':calls})
pathlib.Path(__file__).with_name('hot-split-compilation-summary.json').write_text(json.dumps(out,indent=2)+'\n')
for x in out:
 if x['root']['compile_id'] in ('12137','12144') or ('LmdbStoreConnection$1 next ' in x['root']['method']):
  print(x['root'],x['done'])
  for c in x['calls']:print(c)
