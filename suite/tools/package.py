#!/usr/bin/env python3
"""Save all source and verify the diff can reconstruct every tracked implementation file."""
from __future__ import annotations
import hashlib,io,json,os,subprocess,sys,tarfile,tempfile,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REPO=ROOT.parent
BASELINE='a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3'

def filehash(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''):h.update(chunk)
    return h.hexdigest()

def make_patch(dest):
    patch=subprocess.check_output(['git','diff','--binary',BASELINE,'HEAD','--','suite','.github/workflows/corpus-recovery.yml'],cwd=REPO)
    (dest/'complete-source.patch').write_bytes(patch)
    with tempfile.TemporaryDirectory(prefix='verify-corpus-patch-') as d:
        tree=Path(d)
        archive=subprocess.check_output(['git','archive',BASELINE],cwd=REPO)
        with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
            for member in tar.getmembers():
                if not (tree/member.name).resolve().is_relative_to(tree):raise RuntimeError('Unsafe Git archive member')
                if member.issym() or member.islnk():raise RuntimeError('Unexpected symlink in patch baseline')
            tar.extractall(tree)
        subprocess.run(['git','init','-q',str(tree)],check=True)
        subprocess.run(['git','apply','--check','-'],input=patch,cwd=tree,check=True)
        subprocess.run(['git','apply','-'],input=patch,cwd=tree,check=True)
        tracked=subprocess.check_output(['git','ls-tree','-r','--name-only','HEAD','--','suite','.github/workflows/corpus-recovery.yml'],cwd=REPO,text=True).splitlines()
        for rel in tracked:
            expected=subprocess.check_output(['git','show','HEAD:'+rel],cwd=REPO)
            actual=(tree/rel).read_bytes()
            if actual!=expected:raise RuntimeError('Patch reconstruction mismatch: '+rel)
        return {'baseline':BASELINE,'appliesCleanly':True,'reconstructedTrackedFiles':len(tracked),'sha256':hashlib.sha256(patch).hexdigest()}

def main():
    stage=sys.argv[1] if len(sys.argv)>1 else 'checkpoint'
    outdir=REPO/'deliverables';outdir.mkdir(exist_ok=True)
    # Save verification in the project before packaging so it travels inside the ZIP.
    try:
        patch_report=make_patch(outdir)
    except (OSError,subprocess.CalledProcessError) as e:
        patch_report={'baseline':BASELINE,'appliesCleanly':False,'error':str(e)}
        (outdir/'PATCH-UNAVAILABLE.txt').write_text(str(e))
    (ROOT/'reports').mkdir(exist_ok=True)
    (ROOT/'reports/patch-verification.json').write_text(json.dumps(patch_report,indent=2)+'\n')
    output=outdir/'sparql-corpus-complete-source.zip';files={};excluded=[]
    for p in sorted(ROOT.rglob('*')):
        if not p.is_file():continue
        rel=p.relative_to(ROOT)
        if any(x in {'.git','target','__pycache__','.venv','.pytest_cache'} for x in rel.parts):continue
        if p.is_symlink() or p.suffix.lower() in {'.woff','.woff2','.ttf','.otf'}:
            excluded.append(str(rel));continue
        files[str(rel)]=p
    workflow=REPO/'.github/workflows/corpus-recovery.yml'
    if workflow.exists():files['.github/workflows/corpus-recovery.yml']=workflow
    manifest={};tmp=output.with_suffix('.tmp')
    with zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
        for rel,p in sorted(files.items()):
            h=hashlib.sha256()
            with p.open('rb') as src,z.open('sparql-corpus/'+rel,'w',force_zip64=True) as dst:
                for chunk in iter(lambda:src.read(1024*1024),b''):h.update(chunk);dst.write(chunk)
            manifest[rel]={'bytes':p.stat().st_size,'sha256':h.hexdigest()}
        z.writestr('sparql-corpus/SOURCE-MANIFEST.json',json.dumps(manifest,indent=2,sort_keys=True)+'\n')
        z.writestr('sparql-corpus/EXCLUDED-FILES.json',json.dumps(excluded,indent=2)+'\n')
        z.writestr('sparql-corpus/SNAPSHOT-STAGE.txt',stage+'\n')
    with zipfile.ZipFile(tmp) as z:
        bad=z.testzip()
        if bad:raise RuntimeError('Corrupt ZIP member '+bad)
    os.replace(tmp,output)
    digest=filehash(output)
    summary={'stage':stage,'sourceFiles':len(manifest),'zipBytes':output.stat().st_size,'zipSha256':digest,'zipCRCVerified':True,'patchVerification':patch_report}
    (outdir/'snapshot.json').write_text(json.dumps(summary,indent=2)+'\n')
    (outdir/'SHA256SUMS').write_text(digest+'  '+output.name+'\n'+(filehash(outdir/'complete-source.patch')+'  complete-source.patch\n' if (outdir/'complete-source.patch').exists() else ''))
    print(json.dumps(summary))

if __name__=='__main__':main()
