#!/usr/bin/env python3
"""Validate an extracted frozen runtime and write a NEW, order-preserving classpath.

Example:
  python3 rebase-coordinate-classpath.py --runtime frozen-current \
      --output current-classpath.txt

No Java process runs. Original provenance, reports, and classpath.txt stay intact.
SHA-256 uses the freeze/experiment driver's algorithm: sorted relative POSIX
file names, a NUL separator, and each file's raw SHA-256 for directory entries.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import stat
import tempfile


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def regular_tree(root: Path) -> tuple[list[Path], list[Path]]:
    """Return deterministic directories/files, rejecting links and special files."""
    directories: list[Path] = []
    files: list[Path] = []
    stack = [root]
    while stack:
        path = stack.pop()
        mode = path.lstat().st_mode
        require(not stat.S_ISLNK(mode), f"Symlink is not a frozen artifact: {path}")
        if stat.S_ISDIR(mode):
            directories.append(path)
            stack.extend(path.iterdir())
        else:
            require(stat.S_ISREG(mode), f"Special file is not a frozen artifact: {path}")
            files.append(path)
    return sorted(directories), sorted(files)


def file_digest(path: Path) -> str:
    before = path.lstat()
    require(stat.S_ISREG(before.st_mode), f"Expected regular file: {path}")
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        opened = os.fstat(stream.fileno())
        require((before.st_dev, before.st_ino) == (opened.st_dev, opened.st_ino),
                f"File changed while opening: {path}")
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    after = path.lstat()
    require((before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns, before.st_ctime_ns)
            == (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns, after.st_ctime_ns),
            f"File changed while hashing: {path}")
    return digest.hexdigest()


def tree_digest(files: dict[str, str]) -> str:
    digest = hashlib.sha256()
    # pathlib compares path components; plain string order can differ at a
    # directory boundary (for example a/z versus a-foo). Match the freezer.
    for name in sorted(files, key=PurePosixPath):
        digest.update(name.encode() + b"\0")
        digest.update(bytes.fromhex(files[name]))
    return digest.hexdigest()


def validate_runtime(runtime: Path, hashes: dict[Path, str] | None = None) -> dict:
    runtime = runtime.resolve(strict=True)
    require(runtime.is_dir(), f"Runtime is not a directory: {runtime}")
    hashes = {} if hashes is None else hashes
    require(stat.S_ISDIR((runtime / "artifacts").lstat().st_mode),
            f"Artifacts must be a real directory, not a link: {runtime}")
    metadata_paths = [runtime / name for name in
                      ("provenance.json", "classpath.txt", "source-surefire-report.xml")]
    for path in metadata_paths:
        hashes[path] = file_digest(path)
    provenance_bytes = metadata_paths[0].read_bytes()
    require(hashlib.sha256(provenance_bytes).hexdigest() == hashes[metadata_paths[0]],
            f"Provenance changed during validation: {runtime}")
    provenance = json.loads(provenance_bytes)
    entries = provenance["entries"]
    require(len(entries) == 126, f"Expected all 126 classpath entries: {runtime}")
    require(hashes[metadata_paths[2]] == provenance["reportSha256"],
            f"Source Surefire report hash differs: {runtime}")
    # Both preserved runtimes were frozen on POSIX; keep their original strings.
    original_classpath = ":".join(entry["frozen"] for entry in entries) + "\n"
    require(hashes[metadata_paths[1]] == hashlib.sha256(original_classpath.encode("utf-8")).hexdigest(),
            f"Original classpath order/content differs from provenance: {runtime}")
    old_root = PurePosixPath(entries[0]["frozen"]).parent.parent
    require(old_root.is_absolute(), "Provenance frozen paths must be absolute POSIX paths")
    artifacts = []
    names = []
    for index, entry in enumerate(entries):
        old_path = PurePosixPath(entry["frozen"])
        relative = old_path.relative_to(old_root)
        require(len(relative.parts) == 2 and relative.parts[0] == "artifacts"
                and relative.parts[1].startswith(f"{index:03d}-")
                and all(part not in ("", ".", "..") for part in relative.parts),
                f"Unexpected frozen artifact path: {old_path}")
        names.append(relative.parts[1])
        path = runtime.joinpath(*relative.parts)
        directories, files = regular_tree(path)
        kind = "directory" if directories else "file"
        require(kind == entry["kind"], f"Artifact type differs: {path}")
        for file in files:
            hashes[file] = file_digest(file)
        digest = (tree_digest({file.relative_to(path).as_posix(): hashes[file] for file in files})
                  if kind == "directory" else hashes[path])
        require(digest == entry["sha256"], f"Artifact hash differs: {path}")
        require(os.pathsep not in str(path) and "\n" not in str(path),
                f"Extraction path cannot be represented in a classpath: {path}")
        artifacts.append({"relative": relative.as_posix(), "path": path, "kind": kind,
                          "sha256": digest, "files": len(files),
                          "directories": directories, "members": files})
    require(len(set(names)) == 126 and set(names) == {p.name for p in (runtime / "artifacts").iterdir()},
            f"Missing, duplicate, or extra artifact entry: {runtime}")
    return {"root": runtime, "provenance": provenance, "artifacts": artifacts,
            "metadata": metadata_paths}


def write_new(path: Path, content: bytes) -> None:
    """Publish a complete file without replacing an existing path, including links."""
    parent = path.parent.resolve(strict=True)
    destination = parent / path.name
    descriptor, temporary = tempfile.mkstemp(prefix=".classpath-", dir=parent)
    try:
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        os.link(temporary, destination)
    finally:
        os.unlink(temporary)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime", type=Path, required=True)
    parser.add_argument("--output", type=Path,
                        help="New file; default is classpath.rebased.txt within the runtime")
    parser.add_argument("--verify-only", action="store_true",
                        help="Validate all artifacts and original metadata without writing")
    args = parser.parse_args()
    info = validate_runtime(args.runtime)
    if args.verify_only:
        require(args.output is None, "--verify-only cannot be combined with --output")
        print(f"Validated 126 ordered entries: {info['provenance']['source']}")
        return
    output = args.output or info["root"] / "classpath.rebased.txt"
    output = output.parent.resolve(strict=True) / output.name
    require(not output.is_relative_to(info["root"] / "artifacts"),
            "The new classpath must be outside the immutable artifacts directory")
    write_new(output, (os.pathsep.join(str(entry["path"]) for entry in info["artifacts"]) + "\n")
              .encode("utf-8"))
    print(output.absolute())
    print(f"Recorded Java version: {info['provenance']['javaVersion']}")


if __name__ == "__main__":
    main()
