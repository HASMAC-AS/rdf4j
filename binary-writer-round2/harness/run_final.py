#!/usr/bin/env python3
"""Run final matched comparisons sequentially; never overlap with builds or other benchmarks."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
WORKLOADS = ("ascii", "unicode", "repeat_identity", "repeat_equal_lmdb", "unique_lmdb", "wide",
             "medium", "medium_unicode", "long", "long_unicode", "long_latin1", "long_cjk",
             "long_emoji", "long_sparse", "query")
identity = json.loads((HERE / "compiled-source-identity.json").read_text())
def check_sources():
    for relative, expected in identity["sources"].items():
        actual = hashlib.sha256((REPO / relative).read_bytes()).hexdigest()
        if actual != expected:
            raise RuntimeError("Source changed since compilation: " + relative)

mode = sys.argv[1] if len(sys.argv) > 1 else "heartbeat"
if mode not in ("heartbeat", "small"):
    raise SystemExit("Usage: run_final.py [heartbeat|small] [fresh-output-prefix]")
prefix = sys.argv[2] if len(sys.argv) > 2 else "final"
if not prefix or any(c not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-" for c in prefix):
    raise SystemExit("Output prefix must contain only letters, digits, underscores, or hyphens")
check_sources()
identity_path = HERE.parent / (prefix + "-" + mode + "-source-identity.json")
if identity_path.exists():
    raise SystemExit("Choose a fresh output prefix; already exists: " + str(identity_path))
identity_path.write_text(json.dumps(identity, indent=2) + "\n")
if mode == "heartbeat":
    for index, workload in enumerate(WORKLOADS):
        implementations = "published,current" if index % 2 == 0 else "current,published"
        name = prefix + "-" + workload
        if (HERE.parent / (name + ".json")).exists():
            raise SystemExit("Refusing to overwrite result: " + name)
        command = [sys.executable, str(HERE / "bench.py"), "jmh", name, "--",
                   "-p", "implementation=" + implementations, "-p", "workload=" + workload,
                   "-p", "sink=heartbeat", "-p", "namespaceLayout=stable",
                   "-wi", "5", "-w", "1s", "-i", "5", "-r", "1s", "-f", "2", "-prof", "gc"]
        print("Starting", name, "order", implementations, flush=True)
        subprocess.run(command, cwd=REPO, check=True)
        check_sources()
elif mode == "small":
    if (HERE.parent / (prefix + "-small.json")).exists():
        raise SystemExit("Refusing to overwrite small-document results")
    command = [sys.executable, str(HERE / "bench.py"), "small-jmh", prefix + "-small", "--",
               "-p", "implementation=current,published", "-p", "rows=0,1,8", "-p", "sink=memory",
               "-wi", "5", "-w", "1s", "-i", "5", "-r", "1s", "-f", "2", "-prof", "gc"]
    subprocess.run(command, cwd=REPO, check=True)
    check_sources()
else:
    raise SystemExit("Usage: run_final.py [heartbeat|small] [fresh-output-prefix]")
