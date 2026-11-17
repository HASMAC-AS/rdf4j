import json,collections,re
for flag in ['false','true']:
 es=json.load(open(f'experiments/overlay/profile-{flag}-samples.json'))['recording']['events']; inc=collections.Counter();top=collections.Counter();n=0
 for e in es:
  ns=[f['method']['type']['name'].replace('/','.')+'.'+f['method']['name'] for f in e['values'].get('stackTrace',{}).get('frames',[])]
  if not any(any(x in s for x in ['ValueStore.resolveValue','ValueStore$OverlayMaterializer','ValueStore.data2','CompressedValueOverlay.recordAt','CompressedValueOverlay.openRecordAt','ValueOverlayRegistry$SnapshotLease.get','ValueOverlayRegistry$SnapshotLease.visitRecord']) for s in ns):continue
  n+=1;top.update(ns[:1]);inc.update(set(ns))
 print(flag,'scoped samples',n,'top frames',top.most_common(25))
 vals=[float(re.search('resolveMillis=([0-9.]+)',l)[1]) for l in open(f'experiments/overlay/profile-{flag}.log') if 'resolveMillis=' in l];print('last8 mean',sum(vals[-8:])/8)
