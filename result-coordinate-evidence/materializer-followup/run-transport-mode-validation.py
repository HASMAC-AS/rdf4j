#!/usr/bin/env python3
"""Repeat the preserved IR/transport selections on the unchanged validated candidate."""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
MODULE = ROOT / "core/sail/lmdb"


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n")


def source_hashes():
    return {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted((MODULE / "src").rglob("*")) if p.is_file()}


def run(argv, directory, label):
    record = {"argv": argv, "cwd": str(ROOT),
              "started": datetime.datetime.now(datetime.timezone.utc).isoformat()}
    write_json(directory / (label + "-command.json"), record)
    started_ns = time.time_ns()
    logfile = directory / ("maven-build.log" if label == "build" else "tests.log")
    print(directory.name + " " + label + " started", flush=True)
    with logfile.open("wb") as log:
        proc = subprocess.run(argv, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    record.update(exit_code=proc.returncode,
                  finished=datetime.datetime.now(datetime.timezone.utc).isoformat())
    write_json(directory / (label + "-command.json"), record)
    if label == "build":
        shutil.copyfile(logfile, ROOT / "maven-build.log")
    print(directory.name + " " + label + " exited " + str(proc.returncode), flush=True)
    return proc.returncode, started_ns


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=HERE / "validation/candidate1-and-2-modes")
    parser.add_argument("--validated-stage", type=Path, default=HERE / "validation/candidate1-and-2")
    args = parser.parse_args()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    before = source_hashes()
    prior = json.loads((args.validated_stage / "sources-after.json").read_text())
    if before != prior:
        raise ValueError("Source differs from the passing Candidate 1+2 selection")
    write_json(out / "sources-before.json", before)
    aggregate = {}
    for mode in ["off", "changes", "selection", "rows"]:
        directory = out / mode
        directory.mkdir()
        original = ROOT / "result-coordinate-evidence/final-validation/modes" / mode / "command.json"
        argv = json.loads(original.read_text())["argv"]
        if "-am" in argv or "-q" in argv:
            raise ValueError("Invalid preserved test command")
        code, _ = run(["../mvn", "-o", "-Pquick", "-DskipFrontend=true", "-pl",
                       "core/sail/lmdb", "install"], directory, "build")
        if code:
            raise SystemExit(code)
        code, started_ns = run(argv, directory, "tests")
        reports = directory / "surefire-reports"
        reports.mkdir()
        totals = dict(classes=0, tests=0, failures=0, errors=0, skipped=0)
        for p in sorted((MODULE / "target/surefire-reports").glob("*")):
            if p.is_file() and p.stat().st_mtime_ns >= started_ns:
                shutil.copyfile(p, reports / p.name)
                if p.name.startswith("TEST-") and p.suffix == ".xml":
                    suite = ET.parse(p).getroot()
                    totals["classes"] += 1
                    for key in ("tests", "failures", "errors", "skipped"):
                        totals[key] += int(suite.attrib.get(key, 0))
        write_json(directory / "results.json", totals)
        aggregate[mode] = totals
        write_json(out / "results.json", aggregate)
        print(mode + " " + json.dumps(totals), flush=True)
        if code or totals["tests"] != (84 if mode == "off" else 90) or any(
                totals[k] for k in ("failures", "errors", "skipped")):
            raise RuntimeError("Transport mode gate failed; preserve output for investigation")
        if source_hashes() != before:
            raise RuntimeError("Candidate source changed during the mode gate")
    write_json(out / "sources-after.json", source_hashes())
    print("all transport mode gates passed", flush=True)


if __name__ == "__main__":
    main()
