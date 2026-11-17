#!/usr/bin/env python3
"""Prepare/report an isolated formatter workspace. Never run Java or write original sources."""
from __future__ import annotations

import argparse
import copy
import difflib
import hashlib
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
BASE = Path("result-coordinate-evidence/retention-followup")
PACKAGE = Path("org/eclipse/rdf4j/sail/lmdb")
SEAL = BASE / "contracts/contract-seal-v1.json"
NS = "http://maven.apache.org/POM/4.0.0"
ET.register_namespace("", NS)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def tag(name: str) -> str:
    return "{" + NS + "}" + name


def sources() -> list[tuple[Path, Path, str]]:
    result = []
    for name in ("NativeDirectValueMemo", "NativeProjectedBindingSet", "NativeResultMaterializer"):
        role = "new-change-candidate" if name == "NativeDirectValueMemo" else "unchanged-overlay-control"
        relative = PACKAGE / "evaluation" / (name + ".java")
        result.append((BASE / "implementation-src" / relative, relative, role))
    for name in ("NativeDirectValueMemoStatistics", "DirectMemoLauncher"):
        relative = PACKAGE / "evaluation" / (name + ".java")
        result.append((BASE / "benchmark/src" / relative, relative, "new-change-candidate"))
    for relative in (PACKAGE / "evaluation/NativeDirectMemoRetentionDiagnostic.java",
                     PACKAGE / "NativeMemoSourceCacheRetentionDiagnosticTest.java"):
        result.append((BASE / "diagnostics/src" / relative, relative, "new-diagnostic-candidate"))
    relative = PACKAGE / "evaluation/NativeDirectValueMemoRetentionContractTest.java"
    result.append((BASE / "contracts/src" / relative, relative, "sealed-test-diagnostic-only"))
    return result


def verify_seal() -> None:
    seal = json.loads((ROOT / SEAL).read_text())
    source = ROOT / sources()[-1][0]
    if digest(source) != seal["sha256"]:
        raise RuntimeError("The sealed contract source no longer matches contract-seal-v1.json")


def prepare(work: Path) -> None:
    verify_seal()
    root_pom = ROOT / "pom.xml"
    settings = ROOT / "eclipse-settings/eclipse-rdf4j-conventions.xml"
    document = ET.parse(root_pom)
    plugin = document.find(".//m:profile[m:id='formatting']/m:build/m:plugins/m:plugin", {"m": NS})
    if plugin is None or plugin.findtext(tag("artifactId")) != "spotless-maven-plugin":
        raise RuntimeError("Repository Spotless formatting plugin was not found")
    java = copy.deepcopy(plugin.find(tag("configuration")).find(tag("java")))
    version = plugin.findtext(tag("version"))
    eclipse = java.find(tag("eclipse"))
    if version != "3.10.4" or eclipse.findtext(tag("version")) != "4.40":
        raise RuntimeError("Expected repository Spotless 3.10.4 and Eclipse 4.40")
    work.mkdir(parents=True, exist_ok=False)
    (work / ".mvn").mkdir()  # Prevent Maven from discovering the repository's ancestor project configuration.
    shutil.copyfile(settings, work / settings.name)
    includes = java.find(tag("includes"))
    includes.clear()
    entries = []
    for source, relative, role in sources():
        target = Path("src/main/java") / relative
        original = Path("originals") / relative
        for destination in (work / target, work / original):
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / source, destination)
        ET.SubElement(includes, tag("include")).text = target.as_posix()
        entries.append({"source": source.as_posix(), "copy": target.as_posix(),
                        "originalCopy": original.as_posix(), "sha256Before": digest(ROOT / source), "role": role})
    eclipse.find(tag("file")).text = str(work / settings.name)
    project = ET.Element(tag("project"))
    for name, value in (("modelVersion", "4.0.0"), ("groupId", "org.eclipse.rdf4j.experiments"),
                        ("artifactId", "retention-java-formatting"), ("version", "1.0.0"), ("packaging", "pom")):
        ET.SubElement(project, tag(name)).text = value
    plugins = ET.SubElement(ET.SubElement(project, tag("build")), tag("plugins"))
    isolated = ET.SubElement(plugins, tag("plugin"))
    for name, value in (("groupId", "com.diffplug.spotless"), ("artifactId", "spotless-maven-plugin"),
                        ("version", version)):
        ET.SubElement(isolated, tag(name)).text = value
    configuration = ET.SubElement(isolated, tag("configuration"))
    configuration.append(java)  # Java settings only: no parent, XML format, executions, or historical sources.
    ET.SubElement(configuration, tag("skip")).text = "false"
    ET.indent(project, space="  ")
    ET.ElementTree(project).write(work / "pom.xml", encoding="utf-8", xml_declaration=True)
    manifest = {"repository": str(ROOT), "workspace": str(work), "spotless": version, "eclipse": "4.40",
                "rootPomSha256": digest(root_pom), "settingsSha256": digest(settings),
                "sealSha256": digest(ROOT / SEAL), "entries": entries}
    save_json(work / "inputs.json", manifest)
    print(json.dumps({"workspace": str(work), "sourceCopies": len(entries), "originalsModified": False}))


def report(work: Path) -> None:
    verify_seal()
    manifest = json.loads((work / "inputs.json").read_text())
    if manifest["repository"] != str(ROOT) or manifest["workspace"] != str(work):
        raise RuntimeError("Formatting workspace belongs to another repository or location")
    for path, expected in ((ROOT / "pom.xml", manifest["rootPomSha256"]),
                           (ROOT / "eclipse-settings/eclipse-rdf4j-conventions.xml", manifest["settingsSha256"]),
                           (ROOT / SEAL, manifest["sealSha256"])):
        if digest(path) != expected:
            raise RuntimeError(f"An original formatting input changed: {path}")
    rows = []
    differences = work / "diffs"
    differences.mkdir(exist_ok=False)
    for entry in manifest["entries"]:
        source, before, after = ROOT / entry["source"], work / entry["originalCopy"], work / entry["copy"]
        if digest(source) != entry["sha256Before"] or digest(before) != entry["sha256Before"]:
            raise RuntimeError(f"Original source changed during formatting; do not copy over it: {source}")
        changed = before.read_bytes() != after.read_bytes()
        row = dict(entry, sha256After=digest(after), changed=changed)
        if changed:
            difference = "".join(difflib.unified_diff(before.read_text().splitlines(keepends=True),
                                                     after.read_text().splitlines(keepends=True),
                                                     fromfile=entry["source"], tofile=entry["copy"]))
            diff_path = differences / (source.name + ".diff")
            diff_path.write_text(difference, encoding="utf-8")
            row["diff"] = str(diff_path.relative_to(work))
        rows.append(row)
    result = {"originalsUnchanged": True, "sealedContractUnchanged": True,
              "comparisonOnlyNoFormatterExitStatusClaim": True,
              "changedFiles": sum(row["changed"] for row in rows), "files": rows}
    save_json(work / "comparison.json", result)
    print(json.dumps({"comparison": str(work / "comparison.json"), "changedFiles": result["changedFiles"],
                      "originalsModified": False}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=("prepare", "report"))
    parser.add_argument("workspace", type=Path)
    args = parser.parse_args()
    workspace = args.workspace.resolve()
    if not workspace.is_relative_to(ROOT / BASE / "formatting"):
        parser.error("Workspace must be a new directory beneath retention-followup/formatting")
    {"prepare": prepare, "report": report}[args.operation](workspace)
