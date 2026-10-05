#!/usr/bin/env python3
"""Save all recoverable implementation/corpus source even after failed execution."""
from __future__ import annotations
import hashlib, json, os, subprocess, sys, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REPO=ROOT.parent
BASELINE='a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3'

def main():
    stage=sys.argv[1] if len(sys.argv)>1 else 'checkpoint'
    outdir=REPO/'deliverables'; outdir.mkdir(exist_ok=True)
    output=outdir/'sparql-corpus-complete-source.zip'
    files={}; excluded=[]
    for p in sorted(ROOT.rglob('*')):
        if not p.is_file(): continue
        rel=p.relative_to(ROOT)
        if any(x in {'.git','target','__pycache__','.venv','.pytest_cache'} for x in rel.parts): continue
        if p.is_symlink() or p.suffix.lower() in {'.woff','.woff2','.ttf','.otf'}:
            excluded.append(str(rel)); continue
        files[str(rel)]=p
    workflow=REPO/'.github/workflows/corpus-recovery.yml'
    if workflow.exists(): files['.github/workflows/corpus-recovery.yml']=workflow
    manifest={}; tmp=output.with_suffix('.tmp')
    with zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
        for rel,p in sorted(files.items()):
            h=hashlib.sha256()
            with p.open('rb') as src, z.open('sparql-corpus/'+rel,'w',force_zip64=True) as dst:
                for chunk in iter(lambda:src.read(1024*1024),b''): h.update(chunk); dst.write(chunk)
            manifest[rel]={'bytes':p.stat().st_size,'sha256':h.hexdigest()}
        z.writestr('sparql-corpus/SOURCE-MANIFEST.json',json.dumps(manifest,indent=2,sort_keys=True)+'\n')
        z.writestr('sparql-corpus/EXCLUDED-FILES.json',json.dumps(excluded,indent=2)+'\n')
        z.writestr('sparql-corpus/SNAPSHOT-STAGE.txt',stage+'\n')
    with zipfile.ZipFile(tmp) as z:
        bad=z.testzip()
        if bad: raise RuntimeError('Corrupt ZIP member '+bad)
    os.replace(tmp,output)
    try:
        patch=subprocess.check_output(['git','diff','--binary',BASELINE,'HEAD','--','suite','.github/workflows/corpus-recovery.yml'],cwd=REPO)
        (outdir/'complete-source.patch').write_bytes(patch)
    except (OSError,subprocess.CalledProcessError) as e:
        (outdir/'PATCH-UNAVAILABLE.txt').write_text(str(e))
    h=hashlib.sha256()
    with output.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''): h.update(chunk)
    summary={'stage':stage,'sourceFiles':len(manifest),'zipBytes':output.stat().st_size,'zipSha256':h.hexdigest(),'patchBaseline':BASELINE}
    (outdir/'snapshot.json').write_text(json.dumps(summary,indent=2)+'\n')
    (outdir/'SHA256SUMS').write_text(h.hexdigest()+'  '+output.name+'\n')
    print(json.dumps(summary))
if __name__=='__main__': main()
