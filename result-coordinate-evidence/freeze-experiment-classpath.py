#!/usr/bin/env python3
"""Freeze a successful LMDB Surefire report's tested classpath without Maven.

Run after the final build/tests and before the first timed fork. Copying rather
than linking isolates the experiment from later builds, while the report and
manifest preserve the identity and order of its original classpath entries.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET


def file_digest(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def artifact_digest(path: Path) -> str:
    if path.is_file():
        return file_digest(path)
    digest = hashlib.sha256()
    for file in sorted(candidate for candidate in path.rglob("*") if candidate.is_file()):
        digest.update(file.relative_to(path).as_posix().encode() + b"\0")
        digest.update(bytes.fromhex(file_digest(file)))
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, required=True,
                        help="An LMDB TEST-*.xml report from the final successful test run")
    parser.add_argument("--output", type=Path, required=True,
                        help="New directory containing immutable copied classes/dependency jars")
    parser.add_argument("--source", required=True,
                        help="Source commit plus working-tree patch identity, or clean commit")
    args = parser.parse_args()
    report = args.report.resolve()
    root = ET.parse(report).getroot()
    if root.tag != "testsuite" or int(root.get("errors", "0")) or int(root.get("failures", "0")):
        parser.error("The selected report must be a successful Surefire testsuite")
    properties = {item.attrib["name"]: item.attrib["value"]
                  for item in root.findall("./properties/property")}
    classpath = properties.get("surefire.test.class.path") or properties.get("java.class.path")
    if not classpath:
        parser.error("The report has no tested runtime classpath property")
    original = [Path(entry) for entry in classpath.split(os.pathsep)]
    if not all(path.is_absolute() and path.exists() for path in original):
        parser.error("All report classpath entries must be absolute existing paths")
    if not any((path / "org/eclipse/rdf4j/sail/lmdb/benchmark/OptionalFanoutExperiment.class").is_file()
               for path in original if path.is_dir()):
        parser.error("The report classpath does not contain the compiled experiment; rebuild it first")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    copied = []
    entries = []
    for index, path in enumerate(original):
        target = output / "artifacts" / f"{index:03d}-{path.name}"
        target.parent.mkdir(exist_ok=True)
        if path.is_dir():
            shutil.copytree(path, target)
        else:
            shutil.copy2(path, target)
        copied.append(str(target))
        entries.append({"original": str(path), "frozen": str(target),
                        "kind": "directory" if path.is_dir() else "file",
                        "sha256": artifact_digest(target)})
    (output / "classpath.txt").write_text(os.pathsep.join(copied) + "\n")
    shutil.copy2(report, output / "source-surefire-report.xml")
    manifest = {"source": args.source, "originalReport": str(report),
                "reportSha256": hashlib.sha256(report.read_bytes()).hexdigest(),
                "testName": root.get("name"), "javaHome": properties.get("java.home"),
                "javaVersion": properties.get("java.version"), "entries": entries,
                "note": "Fresh source build; historical baseline/Stage-2 artifacts unavailable. "
                        "Hashes cover every copied artifact; driver checks them again before and after timing."}
    (output / "provenance.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(output / "classpath.txt")
    print("Matching Java:", Path(properties.get("java.home", "JAVA_HOME")) / "bin/java")


if __name__ == "__main__":
    main()
