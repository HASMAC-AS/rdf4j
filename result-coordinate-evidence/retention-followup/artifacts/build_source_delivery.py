#!/usr/bin/env python3
"""Build source-only delivery artifacts without changing study inputs or executing Java."""
from __future__ import annotations

import difflib
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import tarfile

ROOT = Path(__file__).resolve().parents[3]
PHASE = Path("result-coordinate-evidence/retention-followup")
OLD = Path("result-coordinate-evidence/direct-memo-followup")
ARTIFACTS = ROOT / PHASE / "artifacts"
PACKAGE = Path("org/eclipse/rdf4j/sail/lmdb/evaluation")
PRODUCTION = Path("core/sail/lmdb/src/main/java") / PACKAGE
NAMES = ("NativeResultMaterializer", "NativeProjectedBindingSet", "NativeDirectValueMemo",
         "NativeDirectValueMemoStatistics")
PREFIX = "complete-source-v1"


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def encoded(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode()


def write_new(path: Path, data: bytes) -> None:
    with path.open("xb") as stream:
        stream.write(data)


def git(*args: str) -> bytes:
    return subprocess.run(["git", *args], cwd=ROOT, check=True, capture_output=True).stdout


def apply_in_memory(patch: str, bases: dict[str, bytes]) -> dict[str, bytes]:
    """Strict unified-hunk application independent of the filesystem/git applicability check."""
    result = {}
    lines = patch.splitlines(keepends=True)
    at = 0
    while at < len(lines):
        if not lines[at].startswith("diff --git "):
            raise ValueError("Unexpected patch section")
        path = lines[at].strip().split(" b/", 1)[1]
        if path not in bases or path in result:
            raise ValueError("Unexpected or duplicate patch path: " + path)
        original = bases[path].decode().splitlines(keepends=True)
        output, cursor = [], 0
        at += 1
        while at < len(lines) and not lines[at].startswith("@@ "):
            at += 1
        while at < len(lines) and lines[at].startswith("@@ "):
            match = re.fullmatch(r"@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@.*\n", lines[at])
            if not match:
                raise ValueError("Unsupported hunk header")
            old_start, old_count, new_start, new_count = (int(match[1]), int(match[2] or 1),
                                                        int(match[3]), int(match[4] or 1))
            start = old_start - 1 if old_count else old_start
            if start < cursor:
                raise ValueError("Overlapping patch hunks")
            output.extend(original[cursor:start])
            cursor = start
            if len(output) != (new_start - 1 if new_count else new_start):
                raise ValueError("New hunk offset does not match reconstructed content")
            removed = added = 0
            at += 1
            while at < len(lines) and not lines[at].startswith(("@@ ", "diff --git ")):
                line = lines[at]
                if line[0] in " -":
                    if cursor >= len(original) or original[cursor] != line[1:]:
                        raise ValueError("Patch context/removal mismatch: " + path)
                    cursor += 1
                    removed += 1
                if line[0] in " +":
                    output.append(line[1:])
                    added += 1
                if line[0] not in " +-":
                    raise ValueError("Unsupported patch content")
                at += 1
            if (removed, added) != (old_count, new_count):
                raise ValueError("Hunk line counts differ")
        output.extend(original[cursor:])
        result[path] = "".join(output).encode()
    if set(result) != set(bases):
        raise ValueError("Patch does not cover exactly the four production files")
    return result


def main() -> None:
    patch_path = ARTIFACTS / "complete-native-value-memo.patch"
    archive_path = ARTIFACTS / "complete-source-v1.tar.gz"
    manifest_path = ARTIFACTS / "complete-source-v1.manifest.json"
    receipt_path = ARTIFACTS / "source-delivery-verification-v1.json"
    readback = ARTIFACTS / "source-readback-v1"
    for path in (patch_path, archive_path, manifest_path, receipt_path, readback):
        if path.exists():
            raise FileExistsError("Preserve prior delivery artifact: " + str(path))
    commit = git("rev-parse", "HEAD").decode().strip()
    branch = git("branch", "--show-current").decode().strip()
    payloads: dict[str, bytes] = {}
    origins: dict[str, str] = {}
    identities: dict[str, str] = {}

    def add(source: Path, destination: Path | None = None) -> None:
        absolute = ROOT / source
        data = absolute.read_bytes()
        name = (destination or source).as_posix()
        if name in payloads and payloads[name] != data:
            raise ValueError("Conflicting archive payload: " + name)
        payloads[name] = data
        origins[name] = source.as_posix()
        identities[source.as_posix()] = sha(data)

    bases, targets, production_records, sections = {}, {}, {}, []
    frozen_hashes = json.loads((ROOT / PHASE / "runtime-v2/sources.json").read_text())
    for name in NAMES:
        relative = PRODUCTION / (name + ".java")
        frozen = PHASE / "runtime-v2/src" / PACKAGE / (name + ".java")
        canonical = PHASE / ("benchmark/src" if name.endswith("Statistics") else "implementation-src")
        canonical = canonical / PACKAGE / (name + ".java")
        target = (ROOT / frozen).read_bytes()
        if target != (ROOT / canonical).read_bytes() or sha(target) != frozen_hashes[str(ROOT / canonical)]:
            raise ValueError("Final runtime and current experiment sources differ: " + name)
        current = ROOT / relative
        if current.exists():
            base = current.read_bytes()
            if base != git("show", commit + ":" + relative.as_posix()):
                raise ValueError("Production source has uncommitted changes: " + str(relative))
        else:
            base = b""
        bases[relative.as_posix()], targets[relative.as_posix()] = base, target
        header = "diff --git a/" + relative.as_posix() + " b/" + relative.as_posix() + "\n"
        if not current.exists():
            header += "new file mode 100644\n"
        section = "".join(difflib.unified_diff(base.decode().splitlines(keepends=True),
                                               target.decode().splitlines(keepends=True),
                                               fromfile="a/" + relative.as_posix() if current.exists() else "/dev/null",
                                               tofile="b/" + relative.as_posix(), n=3))
        if not section:
            raise ValueError("Expected a changed or added production file: " + name)
        sections.append(header + section)
        add(frozen, relative)
        production_records[relative.as_posix()] = {"runtimeSource": frozen.as_posix(),
                                                   "canonicalSource": canonical.as_posix(),
                                                   "beforeSha256": sha(base), "afterSha256": sha(target),
                                                   "newFile": not current.exists()}

    for folder in ("implementation-src", "benchmark/src", "diagnostics/src", "contracts/src", "runtime-v2/src"):
        for source in sorted((ROOT / PHASE / folder).rglob("*.java")):
            add(source.relative_to(ROOT))
    for folder in ("benchmark", "contracts", "diagnostics", "formatting", "recovery"):
        for source in sorted((ROOT / PHASE / folder).glob("*.py")):
            add(source.relative_to(ROOT))
    for folder in (Path("result-coordinate-evidence/compact-page-followup/harness-src"),
                   Path("result-coordinate-evidence/root-qualified-launcher/src"),
                   OLD / "runtimes/identity-witness-v2/generated/src",
                   OLD / "contracts/test-src", OLD / "contracts/integration-src"):
        for source in sorted((ROOT / folder).rglob("*.java")):
            add(source.relative_to(ROOT))
    metadata = [PHASE / "contracts/contract-seal-v1.json", PHASE / "contracts/TEST-DESIGN.md",
                PHASE / "contracts/validation/red-all-v1/receipt.json",
                PHASE / "contracts/validation/red-all-v1/inputs.json",
                PHASE / "benchmark/README.md", PHASE / "runtime-v2/sources.json",
                PHASE / "comparison-v1/protocol.json", PHASE / "comparison-v1/protocol.sha256",
                OLD / "contracts/contract-seal.json", OLD / "contracts/integration-contract-seal.json",
                OLD / "runtimes/identity-witness-v2/generated/compiled-sources.json",
                OLD / "comparison-v2/runtime-identity/baseInputs-sourceProvenanceFile-prepared-harness-provenance.json",
                Path("eclipse-settings/eclipse-rdf4j-conventions.xml"), Path(__file__).resolve().relative_to(ROOT)]
    for source in metadata:
        add(source)
    sealed = PHASE / "contracts/src" / PACKAGE / "NativeDirectValueMemoRetentionContractTest.java"
    formatted = PHASE / "formatting/copies-v1/src/main/java" / PACKAGE / sealed.name
    review = Path("review/core/sail/lmdb/src/test/java") / PACKAGE / sealed.name
    add(formatted, review)
    seal = json.loads((ROOT / PHASE / "contracts/contract-seal-v1.json").read_text())
    original, reviewed = (ROOT / sealed).read_bytes(), (ROOT / formatted).read_bytes()
    token_pattern = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[\w$]+|[^\s]', re.S)
    if sha(original) != seal["sha256"] or token_pattern.findall(original.decode()) != token_pattern.findall(reviewed.decode()):
        raise ValueError("Review test is not a whitespace-only copy of the sealed original")
    patch = "".join(sections).encode()
    reconstructed = apply_in_memory(patch.decode(), bases)
    if reconstructed != targets:
        raise ValueError("Patch does not reconstruct exact final production sources")
    write_new(patch_path, patch)
    checked = subprocess.run(["git", "apply", "--check", str(patch_path)], cwd=ROOT,
                             capture_output=True, text=True)
    if checked.returncode:
        raise RuntimeError("git apply --check failed: " + checked.stderr)
    payloads[patch_path.name] = patch
    origins[patch_path.name] = "generated four-file production diff"

    readme = f"""# Complete native Value memo source package

This source package was assembled from branch `{branch}` at checkout commit
`{commit}`, plus the explicitly hashed retention experiment sources in this package.
Its four production files are byte-identical to the final formatted `retention-followup/runtime-v2/src` files.

## Scope and prerequisites

This is a complete source overlay for this experiment, not a standalone RDF4J distribution. Use a full
RDF4J checkout at the commit above, JDK 25 (the study uses Temurin 25.0.5+8), Maven, the repository's
workspace `.m2_repo` dependencies, and a platform supported by the LMDB native library. Python 3.11+
is required for the standard-library runners. No dependency JARs, compiled classes, LMDB databases,
runtime classpaths, or comparative timing measurements are included. The separately supplied binary
prefix, if any, is not part of this source archive and is insufficient without its matched base classpath.

Extract into an empty directory for review. The archive root is `{PREFIX}/`. `MANIFEST.json` gives
the byte length and SHA-256 of every payload except itself; the external verification receipt also
hashes the manifest and the archive. Identical source contents at production and experiment paths
are intentional: the original runner paths are preserved. No files are hidden behind archive links.

## Production review and patch

The full four production files appear under `core/sail/lmdb/src/main/java/`:
`NativeResultMaterializer.java`, `NativeProjectedBindingSet.java`, `NativeDirectValueMemo.java`,
and `NativeDirectValueMemoStatistics.java`. The patch modifies the first two and adds the last two.
It contains exactly those four files. From an unmodified matching checkout, first run:

```bash
git apply --check /path/to/complete-native-value-memo.patch
```

Applying the patch is a separate user action. The archive build only performed the check and an
independent in-memory reconstruction; it did not modify production files or execute Java.

The direct memo remains experimental and disabled by default. Its enable property is
`rdf4j.lmdb.nativeValueMemo.enabled`; retention is selected with
`rdf4j.lmdb.nativeValueMemo.retention=strong|weak`. The strong reservation covers arrays. The weak
reservation also covers a declared conservative metadata estimate. Neither reservation is a
measurement of total process heap. The experiment preserves source/exposure ownership fences.

## Test identity

The original eight-case retention contract remains at its exact evidence-relative path:
`{sealed.as_posix()}`. It matches the supplied `contract-seal-v1.json`.
`{review.as_posix()}` is the formatted production-review copy. It differs only in whitespace;
Java tokens, strings and comments compare identically. Use the original for replay of sealed evidence.
The historical 20-case memo table test and 12-case integration test are also included at their
original `direct-memo-followup/contracts/` paths. Other repository tests require the full checkout.

## Rebuilding and replaying the study

For study replay, retain the checkout's unmodified production sources while building the ordinary
baseline. Copy only the supplied experimental `result-coordinate-evidence/` sources into that
checkout initially; applying the production patch before baseline recovery changes the comparator.
Perform the repository-required root build, then use the supplied recovery assembler to create a
fresh matched baseline and preserved strong-direct overlay. The package includes all six common
harness sources, the root optional-fanout launcher, the five preserved strong-direct sources, and
their source-provenance inputs. The final weak-capable overlay uses the five new implementation/
benchmark source files; the production statistics file and its benchmark copy are identical.

The canonical runners are `recovery/prepare_runtime.py`, `contracts/run_contracts.py`,
`contracts/compile_runtime.py`, `contracts/qualify_runtime.py`, `diagnostics/run_memory.py`, and
the three scripts under `benchmark/`, relative to `retention-followup/`. The source-generation
helper, isolated formatter helper, evidence-preservation helper, and delivery builder are included.
Read `benchmark/README.md` and `contracts/TEST-DESIGN.md` for their intended sequence and invariants.

These are the original study runners, not a rewritten portable build system: some toolchain paths,
receipt paths and output-version defaults refer to the study workspace. On another machine, configure
a fresh replay copy for the actual checkout and toolchain, record those changes, and use fresh output
directories. Do not silently alter archived sources, seals or protocols, reuse old paths as evidence
of a new execution, or relax qualification gates. The archived pre-implementation red receipt is
provenance for the original eight tests with five expected failures; it is not verification of a new
checkout. Recreate appropriate red/green qualification evidence before claiming reproduction.
The supplied frozen comparison protocol records the prospective schedule and machine-specific
identities, not measured results; generate a new protocol after qualifying newly built runtime inputs.

The comparison uses normal AUTO execution, matched ownership and source caches, coordinate transport
off, fresh JVMs in balanced arm order, eight warmups and five measurements. Explicit collection is
confined to the separate memory diagnostic. Never run builds, tests, compression or diagnostics
alongside a timed JVM. Do not pool diagnostics, prior-host timings, or qualification-pilot timings
with the primary cohort. Compilation and archive verification do not establish a latency improvement.
""".encode()
    payloads["README.md"] = readme
    origins["README.md"] = "generated source package scope and rebuild guidance"
    write_new(ARTIFACTS / "complete-source-v1.README.md", readme)
    manifest = {"format": 1, "archiveRoot": PREFIX, "baseCommit": commit, "branch": branch,
                "sourceOnly": True, "standaloneDistribution": False, "measurementResultsIncluded": False,
                "productionFiles": production_records,
                "sealedTest": {"original": sealed.as_posix(), "originalSha256": sha(original),
                               "formattedReview": review.as_posix(), "formattedSha256": sha(reviewed),
                               "whitespaceOnlyJavaTokenComparison": True},
                "manifestSelfEntryOmitted": "The external receipt hashes this manifest; all other payloads are listed.",
                "files": [{"path": name, "bytes": len(data), "sha256": sha(data), "origin": origins[name]}
                          for name, data in sorted(payloads.items())]}
    manifest_bytes = encoded(manifest)
    write_new(manifest_path, manifest_bytes)
    members = dict(payloads, **{"MANIFEST.json": manifest_bytes})
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w", format=tarfile.PAX_FORMAT) as archive:
        for name, data in sorted(members.items()):
            info = tarfile.TarInfo(PREFIX + "/" + name)
            info.size, info.mode, info.mtime = len(data), 0o644, 0
            info.uid = info.gid = 0
            archive.addfile(info, io.BytesIO(data))
    write_new(archive_path, gzip.compress(buffer.getvalue(), compresslevel=6, mtime=0))
    readback.mkdir()
    extracted = {}
    with tarfile.open(archive_path, mode="r:gz") as archive:
        for member in archive.getmembers():
            relative = Path(member.name)
            if not member.isfile() or relative.is_absolute() or ".." in relative.parts or relative.parts[0] != PREFIX:
                raise ValueError("Unexpected archive member")
            name = Path(*relative.parts[1:]).as_posix()
            if name in extracted:
                raise ValueError("Duplicate archive member")
            data = archive.extractfile(member).read()
            destination = readback / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            write_new(destination, data)
            extracted[name] = destination.read_bytes()
    if extracted != members:
        raise ValueError("Archive read-back is incomplete or differs from source payloads")
    for item in json.loads(extracted["MANIFEST.json"])["files"]:
        if len(extracted[item["path"]]) != item["bytes"] or sha(extracted[item["path"]]) != item["sha256"]:
            raise ValueError("Manifest verification failed")
    if apply_in_memory(extracted[patch_path.name].decode(), bases) != targets:
        raise ValueError("Archived patch reconstruction differs")
    for source, expected in identities.items():
        if sha((ROOT / source).read_bytes()) != expected:
            raise ValueError("Frozen source changed while packaging: " + source)
    receipt = {"verified": True, "baseCommit": commit, "gitApplyCheckExitCode": checked.returncode,
               "gitApplyCheckStdout": checked.stdout, "gitApplyCheckStderr": checked.stderr,
               "productionFilesReconstructedInMemory": 4, "productionFilesMatchFinalRuntimeV2": True,
               "sealedTestUnchanged": True, "formattedTestWhitespaceOnly": True,
               "originalInputsUnchanged": True, "javaCommandsRun": 0, "productionPatchApplied": False,
               "archiveRegularFiles": len(members), "manifestPayloadFiles": len(payloads),
               "uncompressedPayloadBytes": sum(map(len, members.values())),
               "archive": {"file": archive_path.name, "bytes": archive_path.stat().st_size,
                           "sha256": sha(archive_path.read_bytes())},
               "patch": {"file": patch_path.name, "bytes": len(patch), "sha256": sha(patch)},
               "manifest": {"file": manifest_path.name, "bytes": len(manifest_bytes), "sha256": sha(manifest_bytes)},
               "readbackDirectory": str(readback)}
    write_new(receipt_path, encoded(receipt))
    print(json.dumps(receipt, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
