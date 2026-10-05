#!/usr/bin/env python3
"""Create a complete source ZIP and independently verify full and incremental Git patches."""
from __future__ import annotations
import argparse, hashlib, json, os, subprocess, tarfile, tempfile, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent
BASE = 'a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3'
PREVIOUS = '4c8a3e9126e4b00a1f8db01ba1ee15f447458688'
FONT_EXT = {'.ttf', '.otf', '.woff', '.woff2', '.eot'}
SELECT = ['sparql-corpus', '.github/workflows/sparql-corpus-port.yml']


def sha(data):
    return hashlib.sha256(data).hexdigest()


def make_patch(destination, baseline, name):
    info = {'baseline': baseline, 'file': name, 'roundtripVerified': False}
    patch = subprocess.check_output(['git', 'diff', '--binary', baseline, 'HEAD', '--', *SELECT], cwd=REPO)
    path = destination / name
    path.write_bytes(patch)
    with tempfile.TemporaryDirectory() as directory:
        temp = Path(directory)
        archive, checkout = temp / 'base.tar', temp / 'checkout'
        checkout.mkdir()
        subprocess.run(['git', 'archive', '-o', str(archive), baseline], cwd=REPO, check=True)
        with tarfile.open(archive) as source:
            source.extractall(checkout, filter='data')
        subprocess.run(['git', 'init', '-q'], cwd=checkout, check=True)
        subprocess.run(['git', 'apply', '--check', str(path)], cwd=checkout, check=True)
        subprocess.run(['git', 'apply', str(path)], cwd=checkout, check=True)
        tracked = subprocess.check_output(['git', 'ls-files', '-z', '--', *SELECT], cwd=REPO).decode().split('\0')
        count = 0
        for name in filter(None, tracked):
            original, applied = REPO / name, checkout / name
            if applied.read_bytes() != original.read_bytes():
                raise RuntimeError('Patch roundtrip mismatch: ' + name)
            if bool(applied.stat().st_mode & 0o111) != bool(original.stat().st_mode & 0o111):
                raise RuntimeError('Patch executable-mode mismatch: ' + name)
            count += 1
        deleted = subprocess.check_output(['git', 'diff', '--name-only', '--diff-filter=D', baseline, 'HEAD', '--', *SELECT], cwd=REPO, text=True).splitlines()
        if any((checkout / name).exists() for name in deleted):
            raise RuntimeError('Patch did not remove all deleted files')
    info.update(roundtripVerified=True, trackedFilesVerified=count, bytes=len(patch), sha256=sha(patch))
    return info


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--stage', default='checkpoint')
    args = parser.parse_args()
    destination = REPO / 'deliverables'
    destination.mkdir(exist_ok=True)
    patch_results = []
    for baseline, name in [(BASE, 'complete-source.patch'), (PREVIOUS, 'incremental-source.patch')]:
        try:
            patch_results.append(make_patch(destination, baseline, name))
        except (OSError, subprocess.CalledProcessError, RuntimeError) as error:
            patch_results.append({'baseline': baseline, 'file': name, 'roundtripVerified': False, 'error': str(error)})
    (destination / 'PATCH-VERIFICATION.json').write_text(json.dumps(patch_results, indent=2) + '\n')
    for stale in ['PATCH-VERIFICATION-ERROR.txt']:
        if (destination / stale).exists():
            (destination / stale).unlink()
    paths = [p for p in sorted(ROOT.rglob('*')) if p.is_file()
             and not any(x in {'target', '.git', '.venv', '__pycache__'} for x in p.relative_to(ROOT).parts)
             and p.relative_to(ROOT).as_posix() not in {'SOURCE-MANIFEST.json', 'SNAPSHOT.json'}]
    omitted = [p.relative_to(ROOT).as_posix() for p in paths if p.suffix.lower() in FONT_EXT]
    paths = [p for p in paths if p.suffix.lower() not in FONT_EXT]
    workflows = [p for p in (REPO / '.github/workflows').glob('sparql-corpus-port.yml') if p.is_file()]
    manifest = {}
    output = destination / 'sparql-corpus-complete-source.zip'
    temporary = output.with_suffix('.zip.tmp')

    def add(archive, name, data, mode=0o100644):
        entry = zipfile.ZipInfo('sparql-corpus/' + name, date_time=(2026, 10, 5, 0, 0, 0))
        entry.compress_type = zipfile.ZIP_DEFLATED
        entry.external_attr = mode << 16
        archive.writestr(entry, data)

    with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as archive:
        for path in paths:
            name, data = path.relative_to(ROOT).as_posix(), path.read_bytes()
            manifest[name] = {'bytes': len(data), 'sha256': sha(data)}
            add(archive, name, data, 0o100755 if path.stat().st_mode & 0o111 else 0o100644)
        for path in workflows:
            name, data = '.github/workflows/' + path.name, path.read_bytes()
            if name in manifest:
                continue
            manifest[name] = {'bytes': len(data), 'sha256': sha(data)}
            add(archive, name, data)
        add(archive, 'SOURCE-MANIFEST.json', (json.dumps(manifest, indent=2, sort_keys=True) + '\n').encode())
        snapshot = {'stage': args.stage, 'patchBaseline': BASE, 'incrementalPatchBaseline': PREVIOUS,
                    'patchRoundtripVerified': all(p['roundtripVerified'] for p in patch_results),
                    'patches': patch_results, 'omittedNonTestFontAssets': omitted}
        add(archive, 'SNAPSHOT.json', (json.dumps(snapshot, indent=2) + '\n').encode())
    with zipfile.ZipFile(temporary) as archive:
        if len(archive.namelist()) != len(set(archive.namelist())):
            raise RuntimeError('Duplicate ZIP members')
        bad = archive.testzip()
        if bad:
            raise RuntimeError('ZIP CRC failure: ' + bad)
        for name, metadata in manifest.items():
            data = archive.read('sparql-corpus/' + name)
            if len(data) != metadata['bytes'] or sha(data) != metadata['sha256']:
                raise RuntimeError('ZIP content mismatch: ' + name)
    os.replace(temporary, output)
    checks = {p.name: {'bytes': p.stat().st_size, 'sha256': sha(p.read_bytes())}
              for p in sorted(destination.iterdir()) if p.is_file()
              and p.name not in {'SHA256SUMS', 'snapshot.json'}}
    (destination / 'SHA256SUMS').write_text(''.join(value['sha256'] + '  ' + name + '\n' for name, value in checks.items()))
    summary = {'stage': args.stage, 'sourceFiles': len(manifest), 'patches': patch_results,
               'patchRoundtripVerified': all(p['roundtripVerified'] for p in patch_results), 'artifacts': checks}
    (destination / 'snapshot.json').write_text(json.dumps(summary, indent=2) + '\n')
    print('SOURCE_SNAPSHOT ' + json.dumps(summary), flush=True)


if __name__ == '__main__':
    main()
