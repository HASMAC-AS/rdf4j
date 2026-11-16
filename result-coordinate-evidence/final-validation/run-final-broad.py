#!/usr/bin/env python3
"""Replay the saved broad selection against an already installed source snapshot.

Run from the RDF4J repository root after all builds and benchmark JVMs stop.
This runner preserves old reports and never rebuilds or changes source files.
"""

from datetime import datetime, timezone
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET


def now():
    return datetime.now(timezone.utc).isoformat()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-name", default="normal-heap-final",
                        help="New evidence subdirectory name; existing output is preserved")
    args = parser.parse_args()
    if not args.output_name or any(char not in "abcdefghijklmnopqrstuvwxyz0123456789-"
                                   for char in args.output_name):
        raise ValueError("Output name must use lowercase letters, digits, and hyphens")
    repo = Path.cwd().resolve()
    validation = repo / "result-coordinate-evidence/final-validation"
    plan = json.loads((validation / "normal-heap-broad-plan.json").read_text())
    classes = [entry["qualifiedName"] for entry in plan["classes"]]
    hashes = json.loads((validation / "delivered-source-hashes.json").read_text())
    identity = (validation / "delivered-source-identity.txt").read_text().strip()
    changed = [name for name, digest in hashes.items()
               if hashlib.sha256((repo / name).read_bytes()).hexdigest() != digest]
    if changed:
        raise RuntimeError(f"Source differs from delivery manifest: {changed}")
    missing = [name for name in classes
               if not (repo / "core/sail/lmdb/target/test-classes" /
                       (name.replace(".", "/") + ".class")).is_file()]
    if missing:
        raise RuntimeError(f"Selected compiled classes are missing: {missing}")

    output = validation / args.output_name
    output.mkdir()  # A prior result must never be overwritten.
    for name in ("delivered-source-hashes.json", "delivered-source-identity.txt",
                 "delivered-source-provenance.json", "delivered-bytecode-comparison.json"):
        shutil.copy2(validation / name, output / name)
    reports = repo / "core/sail/lmdb/target/surefire-reports"
    if reports.exists():
        reports.rename(output / "preexisting-target-reports")
    reports.mkdir()
    argv = [arg.replace(plan["evidenceDirectory"], str(output)) for arg in plan["argv"]]
    command = {
        "argv": argv, "cwd": str(repo), "started": now(),
        "sourceIdentity": identity, "sourceFilesVerified": len(hashes),
        "benchmarkSourceIdentity": plan["sourceIdentity"],
        "selectedClasses": len(classes),
        "note": "Final test-only fixture correction; production and benchmark drivers unchanged.",
    }
    write_json(output / "command.json", command)
    with (output / "tests.log").open("w", encoding="utf-8") as log:
        result = subprocess.run(argv, cwd=repo, stdout=log, stderr=subprocess.STDOUT)
    command.update(exit_code=result.returncode, finished=now())
    write_json(output / "command.json", command)
    shutil.copytree(reports, output / "surefire-reports")
    completed = []
    for report in sorted(reports.glob("TEST-*.xml")):
        root = ET.parse(report).getroot()
        completed.append({
            "name": root.get("name"),
            **{key: int(root.get(key, "0"))
               for key in ("tests", "failures", "errors", "skipped")},
            "time": root.get("time"),
        })
    completed_names = {item["name"] for item in completed}
    selected_names = set(classes)
    remaining = [name for name, digest in hashes.items()
                 if hashlib.sha256((repo / name).read_bytes()).hexdigest() != digest]
    summary = {
        "exitCode": result.returncode, "sourceIdentity": identity,
        "selectedClasses": len(selected_names), "completedClassReports": len(completed),
        **{key: sum(item[key] for item in completed)
           for key in ("tests", "failures", "errors", "skipped")},
        "missingClassReports": sorted(selected_names - completed_names),
        "unexpectedClassReports": sorted(completed_names - selected_names),
        "sourceChangesDuringRun": remaining, "reports": completed,
    }
    write_json(output / "results.json", summary)
    print(json.dumps({key: value for key, value in summary.items() if key != "reports"}))
    if result.returncode or remaining or summary["missingClassReports"] or summary["unexpectedClassReports"]:
        return result.returncode or 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
