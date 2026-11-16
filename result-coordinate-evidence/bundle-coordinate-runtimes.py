#!/usr/bin/env python3
"""Preserve the two exact measured runtime snapshots in a verified, separate archive.

Run only after the final gate finishes; this script hashes and compresses data.
  python3 result-coordinate-evidence/bundle-coordinate-runtimes.py \
      --output /existing/staging/coordinate-runtimes

The new output directory is published atomically without replacing any existing
path. Source/evidence archives remain separate. No JVM, Maven, Git, or network
operation runs. Only the two frozen classpaths, their original metadata, the
recorded timing manifests, these scripts, and a generated README/manifest enter
the archive. Publication requires Linux renameat2; the rebasing helper does not.
"""

from __future__ import annotations

import argparse
import ctypes
import errno
import gzip
import hashlib
import io
import json
import os
from pathlib import Path
import re
import runpy
import stat
import tarfile
import tempfile
import xml.etree.ElementTree as ET


RUNTIMES = ("frozen-current", "fresh-parent-e7c6ca/frozen")
DRIVER_MANIFESTS = {
    "reuse-versus-dense/classpath-manifest.json": 0,
    "transport-comparison/classpath-manifest.json": 0,
    "workload-screen/classpath-manifest.json": 0,
    "short-query-confirmation/classpath-manifest.json": 0,
    "short-query-extended/classpath-manifest.json": 0,
    "fresh-parent-comparison/classpath-manifest.json": 0,
    "fresh-parent-comparison/parent-classpath-manifest.json": 1,
}
PREFIX = "rdf4j/"
EVIDENCE = "result-coordinate-evidence/"
ARCHIVE_NAME = "rdf4j-coordinate-runtimes.tar.gz"
MANIFEST_NAME = "runtime-bundle-manifest.json"
README_NAME = "RUNTIME-REPRODUCIBILITY.md"
SCRIPT_NAMES = ("bundle-coordinate-runtimes.py", "rebase-coordinate-classpath.py")


def canonical_json(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def publish_directory(temporary: Path, destination: Path) -> None:
    """Linux atomic no-replace rename also rejects existing empty directories/links."""
    libc = ctypes.CDLL(None, use_errno=True)
    rename = getattr(libc, "renameat2", None)
    if rename is None:
        raise RuntimeError("renameat2 is unavailable; no final output was replaced")
    rename.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int, ctypes.c_char_p,
                       ctypes.c_uint]
    rename.restype = ctypes.c_int
    if rename(-100, os.fsencode(temporary), -100, os.fsencode(destination), 1) != 0:
        error = ctypes.get_errno()
        if error in (errno.EEXIST, errno.ENOTEMPTY):
            raise FileExistsError(f"Refusing to replace existing output: {destination}")
        raise OSError(error, os.strerror(error), str(destination))


def readme() -> bytes:
    return """# Frozen coordinate benchmark runtimes

This archive preserves both measured runtime snapshots, with all 126 ordered
classpath entries each, original provenance/classpath/source Surefire report,
and the seven original timing classpath manifests. The fresh parent remains a
new source reconstruction, not the unavailable historical baseline or Stage-2.
No benchmark stores, JDK, Maven cache tree, or credential configuration is bundled.

Extract into an empty directory. The archive starts with rdf4j/. Preserve the
original metadata: its absolute classpath strings describe the original host.
Create separate validated classpaths from the extracted contents:

    python3 rdf4j/result-coordinate-evidence/rebase-coordinate-classpath.py \\
      --runtime rdf4j/result-coordinate-evidence/frozen-current \\
      --output current-classpath.txt
    python3 rdf4j/result-coordinate-evidence/rebase-coordinate-classpath.py \\
      --runtime rdf4j/result-coordinate-evidence/fresh-parent-e7c6ca/frozen \\
      --output parent-classpath.txt

The helper validates all artifact/report hashes and preserves classpath order.
Output files must not already exist. It never edits original classpath.txt or
provenance.json. --verify-only validates without creating a new classpath.

Use a compatible Java/runtime platform; the measured JVM was 25.0.5+8-LTS with
a fixed 1 GiB heap and G1. Original JVM/platform details remain in each source
Surefire report and in the separate experiment evidence. This archive does not
promise the same performance on different hardware or a different JVM.

For example, after validating the current runtime, one fresh JVM invocation is:

    java -Xms1024m -Xmx1024m -XX:+UseG1GC \\
      -Drdf4j.lmdb.resultCoordinateTransport=off \\
      -cp "$(cat current-classpath.txt)" \\
      org.eclipse.rdf4j.sail.lmdb.benchmark.OptionalFanoutExperiment \\
      --arm=off --source=extracted-frozen-current \\
      --warmup=5 --iterations=5 --output=new-off-samples.jsonl

Use a fresh process for each current arm: off, dense, changes, rows, or the
supplementary selection mode. The parent has no coordinate transport property;
use parent-classpath.txt and omit that property. This example is one fork,
not a substitute for the balanced paired protocol. The source/evidence package
contains the original runner, configurations, input digests, and raw results.

runtime-bundle-manifest.json lists every source member and its SHA-256, plus
both artifact manifests and recorded source identities. Its own hash and the
archive hash are in the delivery directory's runtime-verification.json and
SHA256SUMS. The archive is deterministic for identical inputs: sorted members,
fixed gzip time, zero tar timestamps/ownership, and normalized directory modes.
Publication verifies every archived entry by reading it back, then recomputes
both aggregate classpath manifests from those verified archived file hashes.
""".encode("utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path, required=True,
                        help="New output directory under an existing staging parent")
    args = parser.parse_args()
    repo = args.repo.resolve(strict=True)
    evidence = repo / EVIDENCE
    output = args.output.parent.resolve(strict=True) / args.output.name
    if os.path.lexists(output):
        raise FileExistsError(f"Refusing to replace existing output: {output}")
    helper = evidence / SCRIPT_NAMES[1]
    api = runpy.run_path(str(helper))
    require = api["require"]
    digest_file = api["file_digest"]
    require(not output.is_relative_to(repo),
            "Use a staging directory outside the checkout to preserve immutable inputs")
    hashes: dict[Path, str] = {}
    runtimes = [api["validate_runtime"](evidence / name, hashes) for name in RUNTIMES]
    records: dict[str, dict] = {}
    payloads: dict[str, Path | bytes] = {}

    def directory(name: str) -> None:
        records.setdefault(name, {"kind": "directory", "mode": 0o755, "size": 0})

    def parents(name: str) -> None:
        parts = name.split("/")
        for count in range(1, len(parts)):
            directory("/".join(parts[:count]))

    def add_file(path: Path) -> None:
        relative = path.relative_to(repo).as_posix()
        name = PREFIX + relative
        require(name not in records, f"Duplicate archive member: {name}")
        parents(name)
        before = path.lstat()
        require(stat.S_ISREG(before.st_mode), f"Expected regular archive input: {path}")
        digest = hashes.get(path) or digest_file(path)
        records[name] = {"kind": "file", "mode": stat.S_IMODE(before.st_mode) & 0o777,
                         "size": before.st_size, "sha256": digest}
        payloads[name] = path

    def add_bytes(name: str, content: bytes) -> None:
        require(name not in records, f"Duplicate generated member: {name}")
        parents(name)
        records[name] = {"kind": "file", "mode": 0o644, "size": len(content),
                         "sha256": hashlib.sha256(content).hexdigest()}
        payloads[name] = content

    runtime_summaries = []
    for index, info in enumerate(runtimes):
        root = info["root"]
        for path in info["metadata"]:
            add_file(path)
        # Exact original report is retained; refuse credential-like nonempty JVM
        # properties rather than silently redacting an allegedly identical report.
        report = root / "source-surefire-report.xml"
        report_bytes = report.read_bytes()
        require(hashlib.sha256(report_bytes).hexdigest() == hashes[report],
                f"Source report changed during validation: {root}")
        xml = ET.fromstring(report_bytes)
        sensitive = re.compile(r"password|passwd|secret|credential|access[._-]?key|"
                               r"private[._-]?key|(?:^|[._-])token(?:$|[._-])", re.I)
        offending = [p.get("name") for p in xml.findall(".//property")
                     if p.get("value") and sensitive.search(p.get("name", ""))]
        require(not offending, f"Credential-like report properties require review: {offending}")
        for artifact in info["artifacts"]:
            for path in artifact["directories"]:
                name = PREFIX + path.relative_to(repo).as_posix()
                parents(name)
                directory(name)
            for path in artifact["members"]:
                add_file(path)
        runtime_summaries.append({
            "root": PREFIX + root.relative_to(repo).as_posix(),
            "source": info["provenance"]["source"],
            "entryCount": len(info["artifacts"]),
            "reportSha256": info["provenance"]["reportSha256"],
            "artifacts": [{key: artifact[key] for key in ("relative", "kind", "sha256", "files")}
                          for artifact in info["artifacts"]],
        })
        require(len(info["artifacts"]) == 126, f"Incomplete runtime {index}")
    for relative, index in DRIVER_MANIFESTS.items():
        path = evidence / relative
        recorded_bytes = path.read_bytes()
        hashes[path] = hashlib.sha256(recorded_bytes).hexdigest()
        recorded = json.loads(recorded_bytes)
        info = runtimes[index]
        require(len(recorded) == 126, f"Incomplete timing manifest: {path}")
        for expected, entry, actual in zip(recorded, info["provenance"]["entries"], info["artifacts"]):
            require(expected["path"] == entry["frozen"]
                    and expected["kind"] == ("tree" if actual["kind"] == "directory" else "file")
                    and expected["sha256"] == actual["sha256"]
                    and ("files" not in expected or expected["files"] == actual["files"]),
                    f"Timing manifest differs from frozen runtime: {path}")
        add_file(path)
    for name in SCRIPT_NAMES:
        add_file(evidence / name)
    add_bytes(PREFIX + EVIDENCE + README_NAME, readme())
    manifest = canonical_json({
        "format": 1,
        "scope": "Two frozen measured classpaths; source/evidence delivered separately",
        "directoryHashAlgorithm": "SHA256(pathlib-sorted POSIX relative file name + NUL + raw file SHA256)",
        "runtimes": runtime_summaries,
        "timingManifests": {PREFIX + EVIDENCE + path: index
                            for path, index in DRIVER_MANIFESTS.items()},
        "members": {name: records[name] for name in sorted(records)},
        "note": "Member list excludes this generated manifest itself. No original metadata is rewritten.",
    })
    add_bytes(PREFIX + EVIDENCE + MANIFEST_NAME, manifest)
    temporary = Path(tempfile.mkdtemp(prefix=".coordinate-runtimes-", dir=output.parent))
    try:
        archive_path = temporary / ARCHIVE_NAME
        print(f"Validated 126 entries per runtime; archiving {len(records)} members.", flush=True)
        with archive_path.open("xb") as raw:
            with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0, compresslevel=6) as compressed:
                with tarfile.open(mode="w|", fileobj=compressed, format=tarfile.PAX_FORMAT) as archive:
                    for name in sorted(records):
                        record = records[name]
                        member = tarfile.TarInfo(name)
                        member.mode = record["mode"]
                        member.mtime = member.uid = member.gid = 0
                        member.uname = member.gname = ""
                        if record["kind"] == "directory":
                            member.type = tarfile.DIRTYPE
                            archive.addfile(member)
                        else:
                            member.size = record["size"]
                            payload = payloads[name]
                            if isinstance(payload, bytes):
                                archive.addfile(member, io.BytesIO(payload))
                            else:
                                with payload.open("rb") as stream:
                                    require(os.fstat(stream.fileno()).st_size == member.size,
                                            f"Input size changed during archive creation: {payload}")
                                    archive.addfile(member, stream)
            raw.flush()
            os.fsync(raw.fileno())
        # Read every byte back; the verified archive must match the original
        # measured hashes even if a live input changed during archive creation.
        seen = set()
        actual_hashes: dict[str, str] = {}
        with tarfile.open(archive_path, "r|gz") as archive:
            for member in archive:
                name = member.name.rstrip("/") if member.isdir() else member.name
                require(name in records and name not in seen, f"Unexpected/duplicate archive member: {name}")
                seen.add(name)
                expected = records[name]
                require(member.mode == expected["mode"] and member.uid == member.gid == member.mtime == 0
                        and not member.uname and not member.gname, f"Archive metadata differs: {name}")
                if expected["kind"] == "directory":
                    require(member.isdir(), f"Expected archived directory: {name}")
                    continue
                require(member.isfile() and member.size == expected["size"], f"Archive type/size differs: {name}")
                stream = archive.extractfile(member)
                require(stream is not None, f"Unreadable archived file: {name}")
                digest = hashlib.sha256()
                with stream:
                    for block in iter(lambda: stream.read(1 << 20), b""):
                        digest.update(block)
                actual_hashes[name] = digest.hexdigest()
                require(actual_hashes[name] == expected["sha256"], f"Archived content differs: {name}")
        require(seen == set(records), "Archive is missing declared members")
        for info in runtime_summaries:
            for artifact in info["artifacts"]:
                name = info["root"] + "/" + artifact["relative"]
                if artifact["kind"] == "file":
                    digest = actual_hashes[name]
                else:
                    prefix = name + "/"
                    children = {key[len(prefix):]: value for key, value in actual_hashes.items()
                                if key.startswith(prefix)}
                    require(len(children) == artifact["files"], f"Archived tree count differs: {name}")
                    digest = api["tree_digest"](children)
                require(digest == artifact["sha256"], f"Archived runtime digest differs: {name}")
        (temporary / MANIFEST_NAME).write_bytes(manifest)
        verification = canonical_json({
            "verified": True, "runtimeEntryCounts": [126, 126],
            "membersReadBack": len(seen), "filesHashedAfterReadBack": len(actual_hashes),
            "aggregateArtifactHashesRevalidated": 252,
            "archive": {"name": ARCHIVE_NAME, "bytes": archive_path.stat().st_size,
                        "sha256": digest_file(archive_path)},
            "manifestSha256": hashlib.sha256(manifest).hexdigest(),
            "originalMetadataPreserved": True,
        })
        (temporary / "runtime-verification.json").write_bytes(verification)
        sums = "".join(f"{digest_file(temporary / name)}  {name}\n" for name in
                       (ARCHIVE_NAME, MANIFEST_NAME, "runtime-verification.json"))
        (temporary / "SHA256SUMS").write_text(sums, encoding="utf-8")
        publish_directory(temporary, output)
        print(f"Published verified runtime archive: {output / ARCHIVE_NAME}", flush=True)
    except BaseException:
        print(f"Unpublished staging retained for inspection: {temporary}", flush=True)
        raise


if __name__ == "__main__":
    main()
