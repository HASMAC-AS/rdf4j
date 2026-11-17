#!/usr/bin/env python3
"""Package and verify an immutable RDF4J source commit using Python's standard library and Git.

Usage: python3 package_source.py SOURCE_COMMIT [--output-dir DIRECTORY]

Only the selected output directory and temporary directories are written. Patch
verification uses a fresh GIT_INDEX_FILE for each base, without checkout, staging
in the repository index, or ref changes. Source files come from git show, never
from the working tree. Existing deliverables are not overwritten.
"""

import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import tempfile
import zipfile


PUBLISHED_BASE = "741b9389538e80214075692439a8cd25a8a9ae84"
ORIGINAL_BASE = "5434e51194e6c2e1aa745e5d65a4c998918d6073"
INCREMENTAL_NAME = "binary-writer-incremental.patch"
CUMULATIVE_NAME = "binary-writer-cumulative.patch"
SOURCE_NAME = "binary-writer-complete-source.zip"


def git(repo, *args, data=None, env=None):
    result = subprocess.run(
        ["git", "--no-pager", "-C", str(repo), *args],
        input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env,
    )
    if result.returncode:
        message = result.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"git {args[0]} failed: {message}")
    return result.stdout


def resolve_commit(repo, revision):
    return git(repo, "rev-parse", "--verify", "--end-of-options",
               revision + "^{commit}").decode("ascii").strip()


def digest(data):
    return hashlib.sha256(data).hexdigest()


def make_patch(repo, base, source):
    return git(
        repo, "diff", "--binary", "--full-index", "--no-renames",
        "--no-ext-diff", "--no-textconv", "--no-color", "--unified=3",
        "--src-prefix=a/", "--dst-prefix=b/", "--ignore-submodules=none",
        base, source, "--",
    )


def verify_patch(repo, base, patch, expected_tree):
    with tempfile.TemporaryDirectory(prefix="rdf4j-patch-index-") as temporary:
        # The index path must initially be absent; an empty file is not a Git index.
        env = os.environ.copy()
        env["GIT_INDEX_FILE"] = str(Path(temporary) / "index")
        env["GIT_OPTIONAL_LOCKS"] = "0"
        git(repo, "read-tree", base, env=env)
        git(repo, "apply", "--cached", "--check", "--allow-empty",
            "--whitespace=nowarn", data=patch, env=env)
        git(repo, "apply", "--cached", "--allow-empty", "--whitespace=nowarn",
            data=patch, env=env)
        tree = git(repo, "write-tree", env=env).decode("ascii").strip()
        if tree != expected_tree:
            raise RuntimeError(f"Patch from {base} produced {tree}, expected {expected_tree}")
        return tree


def complete_java_sources(repo, source):
    changes = git(
        repo, "diff", "--name-status", "-z", "--no-renames",
        "--no-ext-diff", "--no-textconv", ORIGINAL_BASE, source, "--", "*.java",
    ).split(b"\0")
    if changes[-1] == b"":
        changes.pop()
    if len(changes) % 2:
        raise RuntimeError("Unexpected name-status output from git diff")
    files, records, deleted = {}, [], []
    for status_bytes, path_bytes in zip(changes[::2], changes[1::2]):
        status = status_bytes.decode("ascii")
        path = path_bytes.decode("utf-8")
        if PurePosixPath(path).is_absolute() or ".." in PurePosixPath(path).parts:
            raise RuntimeError(f"Unsafe archive path: {path!r}")
        if status == "D":
            deleted.append(path)
            continue
        content = git(repo, "show", f"{source}:{path}")
        files[path] = content
        records.append({"path": path, "change": status,
                        "size_bytes": len(content), "sha256": digest(content)})
    return files, sorted(records, key=lambda item: item["path"]), sorted(deleted)


def make_archive(files, manifest):
    readme = f"""# RDF4J binary query result writer: complete changed Java sources

Source commit: `{manifest['source_commit']}`
Source tree: `{manifest['source_tree']}`
Incremental patch base: `{PUBLISHED_BASE}`
Cumulative patch base: `{ORIGINAL_BASE}`

This archive contains the complete source-commit version of every Java file
changed relative to the cumulative base, at its original repository path.
It is a changed-source package, not a complete RDF4J checkout. Any deleted Java
paths are recorded in manifest.json and have no source-commit file to include.
Every source byte was read through git show; local uncommitted files are excluded.

manifest.json records each complete source's SHA-256 and size, both patch hashes,
the exact commits, and the source tree reproduced by each patch verification.
The incremental and cumulative patches contain only git diff output, including
all changed paths, and are alternatives for their respective starting commits.
Each was checked and applied in an isolated temporary index; its resulting tree
matched the source commit. The repository working tree, index, and refs were
not changed by verification. ZIP CRCs and every archived source byte were checked.

Archive timestamps are fixed for reproducibility. Benchmark evidence is not
included in this package.
""".encode("utf-8")
    entries = dict(files)
    entries["README.md"] = readme
    entries["manifest.json"] = (
        json.dumps(manifest, indent=2, sort_keys=True) + "\n"
    ).encode("utf-8")
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        for path, content in sorted(entries.items()):
            info = zipfile.ZipInfo(path, date_time=(1980, 1, 1, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, content, compress_type=zipfile.ZIP_DEFLATED,
                             compresslevel=9)
    return buffer.getvalue(), entries


def verify_archive(path, expected):
    with zipfile.ZipFile(path) as archive:
        if sorted(archive.namelist()) != sorted(expected):
            raise RuntimeError("ZIP member list does not match the complete source manifest")
        bad_member = archive.testzip()
        if bad_member is not None:
            raise RuntimeError(f"ZIP CRC verification failed: {bad_member}")
        for name, content in expected.items():
            if archive.read(name) != content:
                raise RuntimeError(f"Archived bytes differ from expected source: {name}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_commit", help="Commit containing the final source to package")
    parser.add_argument(
        "--output-dir", type=Path,
        default=Path(__file__).resolve().parent.parent / "source-package",
        help="Deliverable directory (default: binary-writer-round2/source-package)",
    )
    args = parser.parse_args()
    repo = Path(git(Path(__file__).resolve().parent, "rev-parse", "--show-toplevel")
                .decode("utf-8").strip())
    source = resolve_commit(repo, args.source_commit)
    for base in (PUBLISHED_BASE, ORIGINAL_BASE):
        resolve_commit(repo, base)
    source_tree = git(repo, "rev-parse", source + "^{tree}").decode("ascii").strip()

    payloads, patch_records = {}, []
    for name, base in ((INCREMENTAL_NAME, PUBLISHED_BASE),
                       (CUMULATIVE_NAME, ORIGINAL_BASE)):
        patch = make_patch(repo, base, source)
        verified_tree = verify_patch(repo, base, patch, source_tree)
        payloads[name] = patch
        patch_records.append({"filename": name, "base_commit": base,
                              "size_bytes": len(patch), "sha256": digest(patch),
                              "verified_tree": verified_tree})

    files, source_records, deleted = complete_java_sources(repo, source)
    manifest = {
        "format_version": 1, "source_commit": source, "source_tree": source_tree,
        "incremental_base": PUBLISHED_BASE, "cumulative_base": ORIGINAL_BASE,
        "java_sources": source_records, "deleted_java_sources": deleted,
        "patches": patch_records,
    }
    archive_bytes, archive_entries = make_archive(files, manifest)
    payloads[SOURCE_NAME] = archive_bytes

    output = args.output_dir.resolve()
    output.mkdir(parents=True, exist_ok=True)
    for name in payloads:
        if (output / name).exists():
            raise FileExistsError(f"Deliverable already exists; use a new output directory: {output / name}")
    with tempfile.TemporaryDirectory(prefix="source-package-", dir=output) as temporary:
        staging = Path(temporary)
        for name, content in payloads.items():
            path = staging / name
            path.write_bytes(content)
            if path.read_bytes() != content:
                raise RuntimeError(f"Written deliverable differs from verified bytes: {name}")
        verify_archive(staging / SOURCE_NAME, archive_entries)
        for name in payloads:
            os.replace(staging / name, output / name)

    metadata = {
        "source_commit": source, "source_tree": source_tree,
        "incremental_base": PUBLISHED_BASE, "cumulative_base": ORIGINAL_BASE,
        "complete_java_files": len(source_records),
        "verification": "Both patch trees, ZIP CRCs, and every archived source byte passed",
        "deliverables": [
            {"path": str(output / name), "size_bytes": (output / name).stat().st_size,
             "sha256": digest((output / name).read_bytes())}
            for name in payloads
        ],
    }
    print(json.dumps(metadata, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
