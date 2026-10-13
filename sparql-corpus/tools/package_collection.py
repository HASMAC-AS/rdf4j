#!/usr/bin/env python3
"""Package the collected sources, source code, provenance, and verified Git diff."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import tarfile
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent
BASE = "59be16683be7c08beba32b931ce3568106dc2026"
FONTS = {".ttf", ".otf", ".woff", ".woff2", ".eot"}
EXCLUDED = {".git", "__pycache__", ".venv", "target"}


def git(*args: str) -> bytes:
    return subprocess.check_output(["git", *args], cwd=REPO)


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def verify_patch(patch: bytes, baseline: str) -> int:
    with tempfile.TemporaryDirectory() as directory:
        temp = Path(directory)
        archive, patch_file, checkout = temp / "base.tar", temp / "change.patch", temp / "base"
        archive.write_bytes(git("archive", baseline))
        patch_file.write_bytes(patch)
        checkout.mkdir()
        with tarfile.open(archive) as source:
            source.extractall(checkout, filter="data")
        subprocess.run(["git", "init", "-q"], cwd=checkout, check=True)
        subprocess.run(["git", "apply", "--check", str(patch_file)], cwd=checkout, check=True)
        subprocess.run(["git", "apply", str(patch_file)], cwd=checkout, check=True)
        changed = git("diff", "--name-only", "--diff-filter=ACMR", "-z", baseline, "HEAD", "--", ".github", "sparql-corpus").decode().split("\0")
        for name in filter(None, changed):
            if (checkout / name).read_bytes() != (REPO / name).read_bytes():
                raise RuntimeError("Patch roundtrip failed: " + name)
        for name in filter(None, git("diff", "--name-only", "--diff-filter=D", "-z", baseline, "HEAD", "--", ".github", "sparql-corpus").decode().split("\0")):
            if (checkout / name).exists():
                raise RuntimeError("Patch failed to delete: " + name)
        return len(list(filter(None, changed)))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=REPO / "deliverables-collection")
    parser.add_argument("--baseline", default=BASE)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    patch = git("diff", "--binary", args.baseline, "HEAD", "--", ".github", "sparql-corpus")
    if not patch:
        raise RuntimeError("Empty collection patch")
    verified = verify_patch(patch, args.baseline)
    patch_file = args.output / "sparql-corpus-collection.patch"
    patch_file.write_bytes(patch)
    summary = json.loads((ROOT / "collection/summary.json").read_text())
    modes = {r["path"]: int(r["mode"], 8) for r in (json.loads(line) for line in (ROOT / "collection/files.jsonl").open())}
    paths = [p for p in ROOT.rglob("*") if p.is_file() and not p.is_symlink()
             and not any(part in EXCLUDED for part in p.relative_to(ROOT).parts)
             and p.suffix.lower() not in FONTS
             and p.relative_to(ROOT).as_posix() not in {"SOURCE-MANIFEST.json", "SNAPSHOT.json"}]
    # Repository workflows live outside the module; include them explicitly.
    paths.extend(p for p in (REPO / ".github").rglob("*") if p.is_file())
    paths = sorted(set(paths))
    # Reject font-bearing upstream ZIPs rather than accidentally redistributing them.
    for p in paths:
        if p.suffix.lower() == ".zip":
            with zipfile.ZipFile(p) as upstream:
                if any(Path(name).suffix.lower() in FONTS for name in upstream.namelist()):
                    raise RuntimeError("Upstream ZIP contains a font asset: " + str(p))
    archive_file = args.output / "sparql-corpus-complete-collection.zip"
    temporary = archive_file.with_suffix(".zip.tmp")
    manifest = {}
    prefix = "sparql-corpus-collection/"
    def add(archive, name, data, mode=0o100644):
        info = zipfile.ZipInfo(prefix + name, date_time=(2026, 10, 13, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = mode << 16
        archive.writestr(info, data)
    with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as archive:
        for p in paths:
            name, data = p.relative_to(REPO).as_posix(), p.read_bytes()
            mode = modes.get(p.relative_to(ROOT).as_posix(), 0o100644) if p.is_relative_to(ROOT) else 0o100644
            manifest[name] = {"bytes": len(data), "sha256": digest(data), "mode": oct(mode)}
            add(archive, name, data, mode)
        add(archive, "sparql-corpus-collection.patch", patch)
        add(archive, "SOURCE-MANIFEST.json", (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode())
        snapshot = {"baseline": args.baseline, "commit": git("rev-parse", "HEAD").decode().strip(),
                    "patchRoundtripVerified": True, "changedFilesVerified": verified,
                    "collection": summary, "sourceFiles": len(manifest),
                    "historicalExecutionReports": "The reports inherited from the 2026-10-05 corpus are not new execution results."}
        add(archive, "SNAPSHOT.json", (json.dumps(snapshot, indent=2, sort_keys=True) + "\n").encode())
    with zipfile.ZipFile(temporary) as archive:
        if len(archive.namelist()) != len(set(archive.namelist())) or archive.testzip() is not None:
            raise RuntimeError("ZIP integrity failure")
        for name, metadata in manifest.items():
            data = archive.read(prefix + name)
            if len(data) != metadata["bytes"] or digest(data) != metadata["sha256"]:
                raise RuntimeError("ZIP content verification failed: " + name)
    os.replace(temporary, archive_file)
    metadata = {"zip": {"file": archive_file.name, "bytes": archive_file.stat().st_size,
                         "sha256": digest(archive_file.read_bytes()), "allMembersVerified": True},
                "patch": {"file": patch_file.name, "bytes": len(patch), "sha256": digest(patch),
                          "baseline": args.baseline, "roundtripVerified": True}, "sourceFiles": len(manifest)}
    (args.output / "verification.json").write_text(json.dumps(metadata, indent=2) + "\n")
    (args.output / "SHA256SUMS").write_text("".join(f'{v["sha256"]}  {v["file"]}\n' for v in metadata.values() if isinstance(v, dict) and "sha256" in v))
    print(json.dumps(metadata, indent=2), flush=True)


if __name__ == "__main__":
    main()
