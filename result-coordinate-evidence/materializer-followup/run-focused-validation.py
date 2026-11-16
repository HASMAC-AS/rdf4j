#!/usr/bin/env python3
"""Preserve matching build/test evidence for the localized materializer optimization."""
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
MODULE = ROOT / "core/sail/lmdb"
SELECTION = ",".join([
    "NativeResultMaterializerTest",
    "LmdbNativeProjectionValueReuseTest",
    "LmdbNativeIrFactorCoordinateTransportTest",
    "LmdbNativeIrOptionalFanoutCoordinateTest",
    "LmdbNativePackedFtreeTest#delayedPackedRowsFallBackAfterAdvanceAndIterationClose",
    "KernelResultCoordinatesTest",
    "KernelResultCoordinateRepresentationsTest",
    "KernelResultRowSelectionsTest",
])


def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n")


def sources():
    return {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted((MODULE / "src").rglob("*")) if p.is_file()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("stage")
    args = parser.parse_args()
    if not args.stage or Path(args.stage).name != args.stage:
        parser.error("stage must be a single new directory name")
    out = Path(__file__).resolve().parent / "validation" / args.stage
    out.mkdir(parents=True, exist_ok=False)
    write_json(out / "sources-before.json", sources())
    commands = [
        ("copyright", ["./checkCopyrightPresent.sh"], ROOT / "scripts"),
        ("format", ["../mvn", "-o", "-Pquick", "-Dformatter.skip=false", "-pl",
                    "core/sail/lmdb", "spotless:apply", "spotless:check"], ROOT),
        ("build", ["../mvn", "-o", "-Dmaven.compiler.showWarnings=false", "-T", "1C",
                   "-Pquick", "-Dformatter.skip=false", "-DskipFrontend=true", "-pl",
                   "core/sail/lmdb", "-am", "install"], ROOT),
        ("tests", ["../mvn", "-o", "-Pquick", "-DskipTests=false", "-pl", "core/sail/lmdb",
                   "-Dtest=" + SELECTION, "-DforkCount=1", "-Drdf4j.lmdb.test.maxHeap=4G",
                   "-Dsurefire.runOrder=alphabetical", "surefire:test"], ROOT),
    ]
    for label, argv, cwd in commands:
        record = {"argv": argv, "cwd": str(cwd), "started": now()}
        write_json(out / (label + "-command.json"), record)
        started_ns = time.time_ns()
        print(label + " started " + record["started"], flush=True)
        log_name = "maven-build.log" if label == "build" else label + ".log"
        with (out / log_name).open("wb") as log:
            proc = subprocess.run(argv, cwd=cwd, stdout=log, stderr=subprocess.STDOUT)
        record.update(exit_code=proc.returncode, finished=now())
        write_json(out / (label + "-command.json"), record)
        if label == "build":
            shutil.copyfile(out / log_name, ROOT / "maven-build.log")
        print(label + " exited " + str(proc.returncode), flush=True)
        if label == "tests":
            reports = out / "surefire-reports"
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
            write_json(out / "results.json", totals)
            print(json.dumps(totals), flush=True)
            if totals["classes"] != 8:
                raise RuntimeError("Expected eight fresh report classes")
        if proc.returncode:
            raise SystemExit(proc.returncode)
    write_json(out / "sources-after.json", sources())
    print("completed " + str(out), flush=True)


if __name__ == "__main__":
    main()
