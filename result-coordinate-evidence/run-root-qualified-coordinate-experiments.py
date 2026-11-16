#!/usr/bin/env python3
"""Prespecified root-qualified confirmation on the immutable measured runtime.

This separately named clone preserves the original measurement loop. A unique
external launcher delegates the original harness unchanged and records absolute
root-host counters only after harness.main. Any nonzero hosting counter, audit
failure, incomplete query set, or artifact change stops the protocol without
replacement; raw evidence remains and no confirmation estimate is accepted.
"""

from __future__ import annotations

import argparse
from collections import Counter
import csv
import hashlib
import json
import os
from pathlib import Path
import signal
import statistics
import subprocess
import sys
import time


PROPERTY = "rdf4j.lmdb.resultCoordinateTransport"
MAIN = "org.eclipse.rdf4j.sail.lmdb.evaluation.RootQualifiedOptionalFanoutLauncher"
MODES = {"off", "dense", "changes", "rows", "selection"}
MODE_DESCRIPTIONS = {
    "off": "ordinary last-value reuse; no result-coordinate acquisition or cache",
    "dense": "five-word coordinate copy for each eligible projected cell",
    "changes": "changed-register coordinate records with sparse output changes",
    "rows": "shared source-row selection for grouped output columns within a window",
    "selection": "per-cell primitive selection into immutable coordinate records; supplementary arm",
}
PARENT_ARM = "fresh-parent-e7c6ca"
METRICS = ["totalNs", "allThreadAllocatedBytes", "prepareNs", "evaluateNs",
           "consumeNs", "closeNs", "retainedReadNs", "ledgerPeakBytes"]
COORDINATE_COUNTERS = ["opened", "initialAdmissionRefused", "growthAdmissionRefused",
                       "rowSelectionOwners", "rowSelectionWindows", "rowSelectionRows",
                       "rowSelectionColumns", "rowSelectionGroups", "rowSelectionInvariantRefused"]


def cases(suite: str) -> list[dict]:
    default = {"name": "default", "measurements": 100, "readings": 10}
    if suite == "ablation":
        return [default]
    # Separate effects; this screening set is deliberately not a Cartesian product.
    return [
        default,
        {"name": "small", "measurements": 1, "readings": 1},
        {"name": "many-measurements", "measurements": 1000, "readings": 1},
        {"name": "narrow-1", "width": 1},
        {"name": "narrow-4", "width": 4},
        {"name": "low-fanout", "statuses": 1},
        {"name": "high-fanout", "measurements": 25, "statuses": 8},
        {"name": "payload-1", "unique-payloads": 1},
        {"name": "payload-16", "unique-payloads": 16},
        {"name": "payload-1000", "unique-payloads": 1000},
        {"name": "early-1", "consumption": "early", "limit": 1},
        {"name": "early-1024", "consumption": "early", "limit": 1024},
        {"name": "retained-small-all", "measurements": 1, "readings": 1,
         "consumption": "retained", "retain": 0},
        {"name": "retained-4096", "consumption": "retained", "retain": 4096},
        {"name": "ledger-1m", "ledger": 1 << 20},
    ]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def classpath_manifest(classpath: str, cwd: Path) -> list[dict]:
    result = []
    for entry in classpath.split(os.pathsep):
        path = Path(entry)
        if not path.is_absolute():
            path = cwd / path
        path = path.resolve()
        if path.is_dir():
            digest = hashlib.sha256()
            count = 0
            for file in sorted(p for p in path.rglob("*") if p.is_file()):
                digest.update(file.relative_to(path).as_posix().encode() + b"\0")
                digest.update(bytes.fromhex(sha256(file)))
                count += 1
            result.append({"path": str(path), "kind": "tree", "files": count,
                           "sha256": digest.hexdigest()})
        elif path.is_file():
            result.append({"path": str(path), "kind": "file", "sha256": sha256(path)})
        else:
            raise ValueError(f"Missing classpath entry: {path}")
    return result


def read_run(path: Path, iterations: int) -> tuple[dict | None, list[dict], str | None]:
    if not path.exists():
        return None, [], "missing JSONL"
    try:
        records = [json.loads(line) for line in path.read_text().splitlines() if line.strip()]
    except (ValueError, OSError) as error:
        return None, [], f"incomplete JSONL: {error}"
    metadata = next((record for record in records if record.get("type") == "metadata"), None)
    failures = [record for record in records if record.get("type") == "failure"]
    samples = [record for record in records
               if record.get("type") == "sample" and record.get("stage") == "measurement"]
    all_samples = [record for record in records if record.get("type") == "sample"]
    if failures:
        return metadata, samples, f"failure record: {failures[-1]}"
    if not metadata or len(samples) != iterations:
        return metadata, samples, f"incomplete measurement count: {len(samples)}/{iterations}"
    if any(not record.get("valid") for record in all_samples):
        return metadata, samples, "invalid result, ledger, or route in measured/warmup execution"
    if any(record.get("ledgerAfterBytes") != 0 for record in all_samples):
        return metadata, samples, "native ledger not empty after closure"
    return metadata, samples, None


def run_fork(command: list[str], cwd: Path, destination: Path, timeout: int) -> tuple[int, str | None]:
    (destination / "command.json").write_text(json.dumps({"cwd": str(cwd), "argv": command}, indent=2) + "\n")
    with (destination / "console.log").open("wb") as log:
        process = subprocess.Popen(command, cwd=cwd, stdout=log, stderr=subprocess.STDOUT,
                                   start_new_session=True)
        try:
            return process.wait(timeout=timeout), None
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
            return process.returncode, f"timeout after {timeout}s; incomplete run excluded"
        except BaseException:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
            raise


def route_signature(samples: list[dict]) -> dict:
    tiers = ("compiledBinds", "interpretedBinds", "aggregateCompiledBinds", "aggregateInterpretedBinds")
    selection_histogram = Counter(
        tuple(sorted((route["point"], route["label"], route["strategy"], route["mode"], route["count"])
                     for route in sample["successfulSelections"])) for sample in samples)
    tier_histogram = Counter(tuple(sample["counters"][key] for key in tiers) for sample in samples)
    composition = Counter()
    for signature, count in tier_histogram.items():
        compiled = signature[0] > 0 or signature[2] > 0
        interpreted = signature[1] > 0 or signature[3] > 0
        classification = ("compiled-and-interpreted" if compiled and interpreted
                          else "compiled" if compiled else "interpreted" if interpreted
                          else "no-ir-bind-observed")
        composition[classification] += count
    return {"selectionSignatures": sorted(selection_histogram),
            "selectionSignatureHistogram": [{"signature": signature, "samples": count}
                                            for signature, count in sorted(selection_histogram.items())],
            "tierSignatures": sorted(tier_histogram),
            "tierSignatureHistogram": [{"signature": signature, "samples": count}
                                       for signature, count in sorted(tier_histogram.items())],
            "tierCompositionHistogram": dict(sorted(composition.items())),
            "mixedIrTierComposition": bool(composition["compiled-and-interpreted"]
                                           or composition["compiled"] and composition["interpreted"]),
            "evidence": sorted({sample["routeEvidence"] for sample in samples})}


def coordinate_observations(samples: list[dict]) -> dict:
    observations = [sample.get("counters", {}).get("coordinateAdmissions") for sample in samples]
    deltas = {}
    for key in COORDINATE_COUNTERS:
        values = [observation[key] for observation in observations
                  if observation is not None and observation.get(key) is not None]
        deltas[key] = ({"observedSamples": len(values), "min": min(values),
                        "median": statistics.median(values), "max": max(values), "total": sum(values)}
                       if values else None)
    groups = deltas["rowSelectionGroups"]
    columns = deltas["rowSelectionColumns"]
    return {"availableSamples": sum(observation is not None for observation in observations),
            "sampleCount": len(samples), "deltas": deltas,
            "rowSelectionGroupToColumnRatio": groups["total"] / columns["total"]
            if groups is not None and columns is not None and columns["total"] else None}


def summarize(runs: list[dict], modes: list[str]) -> dict:
    successful = [run for run in runs if run["status"] == "complete"]
    forks = []
    for run in successful:
        forks.append({"case": run["case"], "fork": run["fork"], "mode": run["mode"],
                      "medians": {metric: statistics.median(s[metric] for s in run["samples"])
                                  for metric in METRICS},
                      "routes": route_signature(run["samples"]),
                      "coordinateObservations": coordinate_observations(run["samples"]),
                      "input": [run["metadata"][key] for key in
                                ("canonicalGraphSha256", "querySha256", "expectedRows", "expectedChecksum")],
                      "ledgerLimit": run["metadata"]["perQueryLedgerLimitBytes"]})
    pairs = []
    base = modes[0]
    grouped = {(fork["case"], fork["fork"], fork["mode"]): fork for fork in forks}
    for reference in forks:
        if reference["mode"] != base:
            continue
        for mode in modes[1:]:
            candidate = grouped.get((reference["case"], reference["fork"], mode))
            if candidate is None:
                continue
            if candidate["input"] != reference["input"] or candidate["ledgerLimit"] != reference["ledgerLimit"]:
                raise ValueError("Matched input/ledger identity mismatch; no comparison is valid")
            pairs.append({"case": reference["case"], "fork": reference["fork"], "baseline": base,
                          "candidate": mode, "routeChanged": reference["routes"] != candidate["routes"],
                          "candidateOverBaseline": {
                              metric: candidate["medians"][metric] / reference["medians"][metric]
                              if reference["medians"][metric] else None for metric in METRICS}})
    aggregates = []
    for case in sorted({pair["case"] for pair in pairs}):
        for mode in modes[1:]:
            matched = [pair for pair in pairs if pair["case"] == case and pair["candidate"] == mode]
            if not matched:
                continue
            aggregates.append({"case": case, "baseline": base, "candidate": mode,
                               "completePairs": len(matched),
                               "pairsWithDifferentRoutes": sum(pair["routeChanged"] for pair in matched),
                               "medianPairedRatio": {
                                   metric: statistics.median(values) if values else None
                                   for metric in METRICS
                                   for values in [[pair["candidateOverBaseline"][metric] for pair in matched
                                                   if pair["candidateOverBaseline"][metric] is not None]]}})
    return {"method": "median within each JVM fork, then paired ratios across matching forks",
            "historicalArtifacts": "unavailable; neither output nor labels assert preserved Stage-2",
            "completeForks": len(successful), "excludedForks": len(runs) - len(successful),
            "forks": forks, "pairs": pairs, "aggregates": aggregates}



def read_root_audit(folder: Path, warmup: int, iterations: int) -> tuple[dict | None, str | None]:
    """A zero absolute total covers warmups and measurements in this fresh JVM."""
    try:
        audit = json.loads((folder / "root-host-audit.json").read_text())
        records = [json.loads(line) for line in (folder / "samples.jsonl").read_text().splitlines()
                   if line.strip()]
        samples = [record for record in records if record.get("type") == "sample"]
        metadata = [record for record in records if record.get("type") == "metadata"]
        if len(metadata) != 1 or len(samples) != warmup + iterations:
            return audit, "root audit requires the complete prespecified query count"
        if [sample["iteration"] for sample in samples] != list(range(-warmup, iterations)):
            return audit, "root audit sample chronology differs from prespecified warmups/measurements"
        if [sample["stage"] for sample in samples] != ["warmup"] * warmup + ["measurement"] * iterations:
            return audit, "root audit stage chronology differs"
        if (audit.get("format") != 1 or audit.get("type") != "root-host-audit"
                or audit.get("readPhase") != "after-original-harness-main"
                or audit.get("harnessMain") != "org.eclipse.rdf4j.sail.lmdb.benchmark.OptionalFanoutExperiment"
                or audit.get("harnessReturned") is not True
                or audit.get("harnessFailureClass") is not None):
            return audit, "root audit schema/delegated-main completion is invalid"
        if audit.get("hostedGenericTotal") != 0:
            return audit, "NONZERO HOSTED_GENERIC: stop; outer-root equivalence not established"
        if audit.get("unhostedHardDeclinesTotal") != 0:
            return audit, "NONZERO UNHOSTED_HARD_DECLINES: stop and diagnose"
        per_query_compiles = [sample["counters"]["semanticNativeCompiles"] for sample in samples]
        if any(count != 1 for count in per_query_compiles):
            return audit, "root audit requires exactly one recorded semantic compile per query"
        native_compiles = sum(per_query_compiles)
        if audit.get("semanticNativeCompilesTotal") != native_compiles or native_compiles != warmup + iterations:
            return audit, "absolute semantic compile count does not match one recorded compile per query"
        return audit, None
    except (KeyError, TypeError, ValueError, OSError) as error:
        return None, f"missing/invalid post-main root audit: {error}"


def root_qualified_summary(runs: list[dict], modes: list[str]) -> dict:
    result = summarize(runs, modes)
    result["method"] = "median within each JVM, then candidate/reference ratios within matched balanced rounds"
    result["rootQualification"] = {
        "scope": "All warmups and measurements in each dedicated JVM; counters read only after original main",
        "requiredHostedGenericTotal": 0,
        "prespecifiedForks": 16,
        "completeRootQualifiedForks": sum(run["status"] == "complete" and run.get("rootHostAudit", {}).get("hostedGenericTotal") == 0 for run in runs),
        "status": "blocked" if any(run["status"] != "complete" for run in runs) else "complete" if len(runs) == 16 else "in-progress",
        "perFork": [{"case": run["case"], "fork": run["fork"], "mode": run["mode"],
                     "status": run["status"], "audit": run.get("rootHostAudit")} for run in runs],
        "failurePolicy": "Stop without replacement or exclusion-based confirmation on any failed audit/run",
    }
    if result["rootQualification"]["status"] == "blocked":
        result["aggregates"] = []
        result["inferenceBlocked"] = True
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classpath-file", type=Path, required=True)
    parser.add_argument("--protocol-file", type=Path, required=True)
    parser.add_argument("--parent-classpath-file", type=Path,
                        help="Separate frozen classpath for the optional fresh-parent-e7c6ca arm")
    parser.add_argument("--parent-source", default=PARENT_ARM,
                        help="Exact fresh parent source/overlay identity; never a preserved Stage-2 label")
    parser.add_argument("--java", default="java")
    parser.add_argument("--heap", default="1024m")
    parser.add_argument("--jvm-arg", action="append", default=[])
    parser.add_argument("--modes", default="off,dense,changes,rows")
    parser.add_argument("--suite", choices=["ablation", "screen"], default="ablation")
    parser.add_argument("--cases", help="Comma-separated case subset of the selected suite")
    parser.add_argument("--forks", type=int)
    parser.add_argument("--warmup", type=int)
    parser.add_argument("--iterations", type=int)
    parser.add_argument("--timeout", type=int, default=240)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--source", default="working-tree")
    parser.add_argument("--cwd", type=Path, default=Path.cwd())
    parser.add_argument("--keep-going", action="store_true")
    args = parser.parse_args()
    modes = args.modes.split(",")
    if len(set(modes)) != len(modes) or any(mode not in MODES | {PARENT_ARM} for mode in modes):
        parser.error("Modes must be distinct members of off,dense,changes,rows,selection,fresh-parent-e7c6ca")
    if PARENT_ARM in modes and args.parent_classpath_file is None:
        parser.error("The fresh-parent-e7c6ca arm requires --parent-classpath-file")
    if any(argument.startswith(f"-D{PROPERTY}=") for argument in args.jvm_arg):
        parser.error("Specify transport only with --modes; all other JVM settings are shared")
    forks = args.forks if args.forks is not None else 4
    warmup = args.warmup if args.warmup is not None else 5
    iterations = args.iterations if args.iterations is not None else (5 if args.suite == "ablation" else 2)
    if forks < 1 or warmup < 0 or iterations < 1:
        parser.error("Invalid fork/warmup/iteration counts")
    selected_cases = cases(args.suite)
    if args.cases:
        wanted = set(args.cases.split(","))
        selected_cases = [case for case in selected_cases if case["name"] in wanted]
        if wanted != {case["name"] for case in selected_cases}:
            parser.error("Unknown case in --cases for this suite")
    if (modes != ["off", "dense", "changes", "rows"] or args.suite != "ablation"
            or selected_cases != [{"name": "default", "measurements": 100, "readings": 10}]
            or forks != 4 or warmup != 5 or iterations != 5 or args.heap != "1024m"
            or args.jvm_arg or args.keep_going or args.parent_classpath_file is not None):
        parser.error("This confirmation is fixed: default fixture, off/dense/changes/rows, 4 rounds, 5+5 queries, 1024m/G1/AUTO; no overrides or keep-going")
    cwd = args.cwd.resolve()
    protocol_file = args.protocol_file.resolve()
    protocol_bytes = protocol_file.read_bytes()
    protocol = json.loads(protocol_bytes)
    if (protocol["modes"] != modes or protocol["forks"] != forks or protocol["warmup"] != warmup
            or protocol["iterations"] != iterations or protocol["source"] != args.source
            or protocol["driverSha256"] != sha256(Path(__file__).resolve())
            or protocol["originalDriverSha256"] != sha256(cwd / "result-coordinate-evidence/run-coordinate-experiments.py")
            or protocol["launcherSourceSha256"] != sha256(cwd / protocol["launcherSource"])
            or protocol["originalFreezeDriverSha256"] != sha256(cwd / "result-coordinate-evidence/freeze-experiment-classpath.py")
            or protocol["frozenClasspathManifestSha256"] != sha256(cwd / protocol["frozenClasspathManifest"])
            or Path(args.java).resolve() != Path(protocol["jvm"]["java"]).resolve()):
        raise ValueError("Prespecified protocol/source/driver identity mismatch")
    classpath = args.classpath_file.read_text().strip()
    parent_classpath = (args.parent_classpath_file.read_text().strip()
                        if PARENT_ARM in modes else None)
    destination = args.output.resolve()
    destination.mkdir(parents=True, exist_ok=False)
    binary_manifest = classpath_manifest(classpath, cwd)
    reference_manifest = json.loads((cwd / protocol["frozenClasspathManifest"]).read_text())
    launcher_directory = (cwd / protocol["launcherClassDirectory"]).resolve()
    launcher_files = sorted(path.relative_to(launcher_directory).as_posix()
                            for path in launcher_directory.rglob("*") if path.is_file())
    expected_launcher = "org/eclipse/rdf4j/sail/lmdb/evaluation/RootQualifiedOptionalFanoutLauncher.class"
    if (len(binary_manifest) != 127 or binary_manifest[:-1] != reference_manifest
            or len(reference_manifest) != 126
            or binary_manifest[-1]["path"] != str(launcher_directory)
            or binary_manifest[-1].get("files") != 1
            or binary_manifest[-1]["sha256"] != protocol["launcherClassDirectorySha256"]
            or launcher_files != [expected_launcher]):
        raise ValueError("Classpath must be exact frozen 126 entries followed by one unique launcher class directory")
    (destination / "protocol.json").write_bytes(protocol_bytes)
    (destination / "launcher.java").write_bytes((cwd / protocol["launcherSource"]).read_bytes())
    (destination / "driver.py").write_bytes(Path(__file__).read_bytes())
    (destination / "classpath-manifest.json").write_text(json.dumps(binary_manifest, indent=2) + "\n")
    parent_manifest = classpath_manifest(parent_classpath, cwd) if parent_classpath is not None else None
    if parent_manifest is not None:
        (destination / "parent-classpath-manifest.json").write_text(
            json.dumps(parent_manifest, indent=2) + "\n")
    configuration = {"modes": modes, "cases": selected_cases, "forks": forks, "warmup": warmup,
                     "iterations": iterations, "heap": args.heap, "jvmArguments": args.jvm_arg,
                     "selection": "AUTO; no strategy override", "source": args.source,
                     "transportDescriptions": {mode: MODE_DESCRIPTIONS[mode] for mode in modes if mode in MODES},
                     "order": "rotate mode order each fork; each arm occupies each ordinal position once",
                     "rootQualification": "post-main absolute HOSTED_GENERIC must be zero for every fresh JVM",
                     "protocolSha256": hashlib.sha256(protocol_bytes).hexdigest(),
                     "originalFrozenClasspathEntries": 126, "actualClasspathEntries": 127,
                     "launcherMain": MAIN}
    if parent_classpath is not None:
        configuration["parentSource"] = args.parent_source
        configuration["comparisonScope"] = "cross-source reconstruction; ownership code differs for parent arm"
    (destination / "configuration.json").write_text(json.dumps(configuration, indent=2) + "\n")
    runs = []
    failed = False
    stop_requested = False
    for case in selected_cases:
        for fork in range(forks):
            order = modes[fork % len(modes):] + modes[:fork % len(modes)]
            for position, mode in enumerate(order):
                folder = destination / f"{case['name']}-f{fork:02d}-{position:02d}-{mode}"
                folder.mkdir()
                command = [args.java, f"-Xms{args.heap}", f"-Xmx{args.heap}", "-XX:+UseG1GC"]
                command += args.jvm_arg
                if "ledger" in case:
                    command += [f"-Drdf4j.lmdb.queryMemory.maxBytes={case['ledger']}",
                                f"-Drdf4j.lmdb.queryMemory.perQueryMaxBytes={case['ledger']}"]
                fork_classpath = parent_classpath if mode == PARENT_ARM else classpath
                fork_source = args.parent_source if mode == PARENT_ARM else args.source
                if mode != PARENT_ARM:
                    command.append(f"-D{PROPERTY}={mode}")
                command += ["-cp", fork_classpath, MAIN,
                            f"--arm={mode}", f"--source={fork_source}", f"--warmup={warmup}",
                            f"--iterations={iterations}", f"--output={folder / 'samples.jsonl'}",
                            f"--store={folder / 'store'}"]
                command += [f"--{key}={value}" for key, value in case.items() if key not in {"name", "ledger"}]
                print(f"Running {case['name']} fork {fork + 1}/{forks} {mode}", flush=True)
                started = time.time()
                exit_code, reason = run_fork(command, cwd, folder, args.timeout)
                metadata, samples, validation_error = read_run(folder / "samples.jsonl", iterations)
                root_audit, root_audit_error = read_root_audit(folder, warmup, iterations)
                if reason is None:
                    reason = validation_error
                if reason is None:
                    reason = root_audit_error
                if exit_code != 0 and reason is None:
                    reason = f"exit code {exit_code}"
                console = (folder / "console.log").read_text(errors="replace")
                if exit_code in (-9, 137):
                    reason = f"killed by SIGKILL; memory cause unproven; {reason or ''}"
                if "OutOfMemoryError" in console:
                    reason = f"JVM OutOfMemoryError; {reason or ''}"
                record = {"case": case["name"], "fork": fork, "position": position, "mode": mode,
                          "exitCode": exit_code, "elapsedSeconds": time.time() - started,
                          "directory": str(folder), "status": "complete" if reason is None else "failed",
                          "reason": reason, "metadata": metadata, "samples": samples, "rootHostAudit": root_audit, "rootHostAuditError": root_audit_error}
                runs.append(record)
                failed |= reason is not None
                (destination / "runs.json").write_text(json.dumps(runs, indent=2) + "\n")
                summary = root_qualified_summary(runs, modes)
                (destination / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
                print(f"  {record['status']} in {record['elapsedSeconds']:.1f}s"
                      + (f": {reason}" if reason else ""), flush=True)
                if reason and not args.keep_going:
                    stop_requested = True
                    break
            if stop_requested:
                break
        if stop_requested:
            break
    if (protocol_file.read_bytes() != protocol_bytes
            or sha256(Path(__file__).resolve()) != protocol["driverSha256"]
            or sha256(cwd / protocol["launcherSource"]) != protocol["launcherSourceSha256"]
            or sha256(cwd / protocol["originalDriver"]) != protocol["originalDriverSha256"]
            or sha256(cwd / "result-coordinate-evidence/freeze-experiment-classpath.py") != protocol["originalFreezeDriverSha256"]
            or sha256(cwd / protocol["frozenClasspathManifest"]) != protocol["frozenClasspathManifestSha256"]
            or classpath_manifest(classpath, cwd) != binary_manifest
            or (parent_classpath is not None and classpath_manifest(parent_classpath, cwd) != parent_manifest)):
        for run in runs:
            run["status"] = "failed"
            run["reason"] = "classpath content changed while the experiment was running"
        (destination / "runs.json").write_text(json.dumps(runs, indent=2) + "\n")
        (destination / "summary.json").write_text(json.dumps(root_qualified_summary(runs, modes), indent=2) + "\n")
        print("ERROR: protocol, launcher, driver, or classpath changed; confirmation blocked", flush=True)
        return 1
    with (destination / "fork-medians.csv").open("w", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["case", "fork", "mode", *METRICS])
        writer.writeheader()
        for fork in summary["forks"]:
            writer.writerow({key: fork[key] for key in ("case", "fork", "mode")} | fork["medians"])
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
