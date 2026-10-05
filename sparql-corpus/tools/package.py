#!/usr/bin/env python3
"""Always save full source, independently of import/build/test outcomes."""
from __future__ import annotations
import argparse, hashlib, json, os, subprocess, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent

def main():
    ap = argparse.ArgumentParser(); ap.add_argument('--stage', default='checkpoint'); args = ap.parse_args()
    dest = REPO / 'deliverables'; dest.mkdir(exist_ok=True)
    paths = [p for p in sorted(ROOT.rglob('*')) if p.is_file() and not any(x in {'target','.git','.venv','__pycache__'} for x in p.relative_to(ROOT).parts)]
    # Include the workflow itself so the delivered project has all recovery/build code.
    workflows = [p for p in (REPO / '.github/workflows').glob('sparql-corpus-port.yml') if p.is_file()]
    manifest = {}; out = dest / 'sparql-corpus-complete-source.zip'
    temp = out.with_suffix('.zip.tmp')
    with zipfile.ZipFile(temp, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as z:
        for p in paths:
            rel = str(p.relative_to(ROOT)); data = p.read_bytes(); manifest[rel] = {'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}
            z.writestr('sparql-corpus/' + rel, data)
        for p in workflows:
            data=p.read_bytes(); rel='.github/workflows/'+p.name; manifest[rel]={'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}; z.writestr('sparql-corpus/'+rel,data)
        z.writestr('sparql-corpus/SOURCE-MANIFEST.json', json.dumps(manifest, indent=2, sort_keys=True)+'\n')
        z.writestr('sparql-corpus/SNAPSHOT-STAGE.txt',args.stage+'\n')
    with zipfile.ZipFile(temp) as z:
        bad=z.testzip()
        if bad: raise RuntimeError('ZIP CRC failure: '+bad)
    os.replace(temp,out)
    # A patch has a defined baseline even when the prior downloaded v2 is inaccessible.
    try:
        patch=subprocess.check_output(['git','diff','--binary','a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3','HEAD','--','sparql-corpus','.github/workflows/sparql-corpus-port.yml'],cwd=REPO)
        (dest/'complete-source.patch').write_bytes(patch)
    except (OSError,subprocess.CalledProcessError) as e:
        (dest/'PATCH-UNAVAILABLE.txt').write_text(str(e))
    checks={p.name:{'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(dest.iterdir()) if p.is_file() and p.name not in {'SHA256SUMS','snapshot.json'}}
    (dest/'SHA256SUMS').write_text(''.join(v['sha256']+'  '+k+'\n' for k,v in checks.items()))
    (dest/'snapshot.json').write_text(json.dumps({'stage':args.stage,'sourceFiles':len(manifest),'artifacts':checks},indent=2)+'\n')
    print(json.dumps({'stage':args.stage,'sourceFiles':len(manifest),'zipBytes':out.stat().st_size,'zipSha256':checks[out.name]['sha256']}))

if __name__ == '__main__': main()
