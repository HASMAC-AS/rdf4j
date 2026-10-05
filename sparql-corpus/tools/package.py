#!/usr/bin/env python3
"""Save and CRC-check full source independently of import/build/test outcomes."""
from __future__ import annotations
import argparse, hashlib, json, os, subprocess, tarfile, tempfile, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; REPO=ROOT.parent
BASE='a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3'
FONT_EXT={'.ttf','.otf','.woff','.woff2','.eot'}
def sha(data): return hashlib.sha256(data).hexdigest()
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--stage',default='checkpoint');args=ap.parse_args()
    dest=REPO/'deliverables';dest.mkdir(exist_ok=True)
    paths=[p for p in sorted(ROOT.rglob('*')) if p.is_file() and not any(x in {'target','.git','.venv','__pycache__'} for x in p.relative_to(ROOT).parts)]
    omitted=[str(p.relative_to(ROOT)) for p in paths if p.suffix.lower() in FONT_EXT]
    paths=[p for p in paths if p.suffix.lower() not in FONT_EXT]
    workflows=[p for p in (REPO/'.github/workflows').glob('sparql-corpus-port.yml') if p.is_file()]
    patch_verified=False
    try:
        patch=subprocess.check_output(['git','diff','--binary',BASE,'HEAD','--','sparql-corpus','.github/workflows/sparql-corpus-port.yml'],cwd=REPO)
        (dest/'complete-source.patch').write_bytes(patch)
        with tempfile.TemporaryDirectory() as td:
            temp=Path(td);archive=temp/'base.tar';checkout=temp/'checkout';checkout.mkdir()
            subprocess.run(['git','archive','-o',str(archive),BASE],cwd=REPO,check=True)
            with tarfile.open(archive) as tf:tf.extractall(checkout,filter='data')
            subprocess.run(['git','init','-q'],cwd=checkout,check=True)
            subprocess.run(['git','apply','--check',str(dest/'complete-source.patch')],cwd=checkout,check=True)
            subprocess.run(['git','apply',str(dest/'complete-source.patch')],cwd=checkout,check=True)
            tracked=subprocess.check_output(['git','ls-files','sparql-corpus','.github/workflows/sparql-corpus-port.yml'],cwd=REPO,text=True).splitlines()
            for path in tracked:
                if (checkout/path).read_bytes()!=(REPO/path).read_bytes(): raise RuntimeError('patch roundtrip mismatch: '+path)
            patch_verified=True
    except (OSError,subprocess.CalledProcessError) as exc:
        (dest/'PATCH-VERIFICATION-ERROR.txt').write_text(str(exc))
    manifest={};out=dest/'sparql-corpus-complete-source.zip';temp=out.with_suffix('.zip.tmp')
    def add(z,name,data):
        info=zipfile.ZipInfo('sparql-corpus/'+name,date_time=(2026,10,5,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,data)
    with zipfile.ZipFile(temp,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
        for p in paths:
            r=str(p.relative_to(ROOT));data=p.read_bytes();manifest[r]={'bytes':len(data),'sha256':sha(data)};add(z,r,data)
        for p in workflows:
            data=p.read_bytes();r='.github/workflows/'+p.name;manifest[r]={'bytes':len(data),'sha256':sha(data)};add(z,r,data)
        add(z,'SOURCE-MANIFEST.json',(json.dumps(manifest,indent=2,sort_keys=True)+'\n').encode())
        add(z,'SNAPSHOT.json',(json.dumps({'stage':args.stage,'patchBaseline':BASE,'patchRoundtripVerified':patch_verified,'omittedNonTestFontAssets':omitted},indent=2)+'\n').encode())
    with zipfile.ZipFile(temp) as z:
        bad=z.testzip()
        if bad:raise RuntimeError('ZIP CRC failure: '+bad)
        for path,metadata in manifest.items():
            data=z.read('sparql-corpus/'+path)
            if len(data)!=metadata['bytes'] or sha(data)!=metadata['sha256']:raise RuntimeError('ZIP hash mismatch: '+path)
    os.replace(temp,out)
    checks={p.name:{'bytes':p.stat().st_size,'sha256':sha(p.read_bytes())} for p in sorted(dest.iterdir()) if p.is_file() and p.name not in {'SHA256SUMS','snapshot.json'}}
    (dest/'SHA256SUMS').write_text(''.join(v['sha256']+'  '+k+'\n' for k,v in checks.items()))
    summary={'stage':args.stage,'sourceFiles':len(manifest),'patchRoundtripVerified':patch_verified,'artifacts':checks}
    (dest/'snapshot.json').write_text(json.dumps(summary,indent=2)+'\n');print('SOURCE_SNAPSHOT '+json.dumps(summary),flush=True)
if __name__=='__main__':main()
