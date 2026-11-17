#!/usr/bin/env python3
"""Compile and run the checked-in writer experiment with JAVA_HOME pointing at JDK 25+."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
JAVA_HOME = Path(os.environ["JAVA_HOME"])
JAVA = JAVA_HOME / "bin/java"
CLASSES = HERE / "classes"
MAIN = "org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark"


def classpath():
    paths = (HERE / "dependency-classpath.txt").read_text().strip().split(os.pathsep)
    for artifact in ["rdf4j-queryresultio-binary", "rdf4j-http-client", "rdf4j-sail-lmdb"]:
        paths.append(str(REPO / ".m2_repo/org/eclipse/rdf4j" / artifact / "6.2.0-SNAPSHOT" / (artifact + "-6.2.0-SNAPSHOT.jar")))
    for path in paths:
        if not Path(path).is_file():
            raise FileNotFoundError(path)
    return os.pathsep.join(dict.fromkeys(paths))


def run(command, log=None):
    shown = list(map(str, command))
    for flag in ["-cp", "-processorpath"]:
        if flag in shown:
            shown[shown.index(flag) + 1] = "<recorded classpath>"
    print(" ".join(shown), flush=True)
    if log:
        log.parent.mkdir(parents=True, exist_ok=True)
        with log.open("w") as output:
            result = subprocess.run(list(map(str, command)), cwd=REPO, stdout=output, stderr=subprocess.STDOUT)
        print("exit", result.returncode, "log", log, flush=True)
        print("\n".join(log.read_text().splitlines()[-28:]), flush=True)
        if result.returncode:
            raise SystemExit(result.returncode)
    else:
        subprocess.run(list(map(str, command)), cwd=REPO, check=True)


mode = sys.argv[1]
cp = classpath()
runtime = str(CLASSES) + os.pathsep + cp
if mode == "compile":
    CLASSES.mkdir(exist_ok=True)
    generated = HERE / "generated"
    generated.mkdir(exist_ok=True)
    processor = os.pathsep.join(str(p) for p in [
        REPO / ".m2_repo/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar",
        REPO / ".m2_repo/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar",
    ])
    sources = sorted(HERE.glob("*BinaryQueryResultWriter.java")) + sorted((HERE / "src").rglob("*.java"))
    sources.append(REPO / "core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java")
    command = [JAVA_HOME / "bin/javac", "-cp", cp, "-processorpath", processor,
        "-processor", "org.openjdk.jmh.generators.BenchmarkProcessor", "-d", CLASSES, "-s", generated, *sources]
    run(command, HERE / "compile.log")
    metadata = {
        "base": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip(),
        "java": subprocess.check_output([str(JAVA), "-version"], stderr=subprocess.STDOUT, text=True),
        "sources": {str(p.relative_to(REPO)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
        "classpath": cp.split(os.pathsep),
    }
    (HERE / "compiled-source-identity.json").write_text(json.dumps(metadata, indent=2) + "\n")
elif mode == "smoke":
    run([JAVA, "-ea", "--enable-native-access=ALL-UNNAMED", "-Xms256m", "-Xmx1g", "-XX:ActiveProcessorCount=2",
         "-Dlogback.configurationFile=" + str(HERE / "logback.xml"), "-cp", runtime, MAIN, *sys.argv[2:]], HERE.parent / "smoke.log")
elif mode == "jmh":
    name = sys.argv[2]
    args = sys.argv[3:]
    if args and args[0] == "--":
        args = args[1:]
    command = [JAVA, "--enable-native-access=ALL-UNNAMED", "-XX:ActiveProcessorCount=2", "-cp", runtime,
        "org.openjdk.jmh.Main", MAIN + ".serialize", "-t", "1", "-foe", "true", "-jvmArgsAppend",
        "--enable-native-access=ALL-UNNAMED -Xms512m -Xmx1g -XX:ActiveProcessorCount=2 -XX:FlightRecorderOptions=stackdepth=128 "
        "-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dlogback.configurationFile=" + str(HERE / "logback.xml"),
        "-rf", "json", "-rff", HERE.parent / (name + ".json"), *args]
    (HERE.parent / (name + "-command.json")).write_text(json.dumps(list(map(str, command)), indent=2) + "\n")
    run(command, HERE.parent / (name + ".log"))
else:
    raise SystemExit("Usage: bench.py compile | smoke [implementations workloads sinks] | jmh NAME [JMH options]")
