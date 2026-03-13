# JMH benchmark runner

Use the Linux Java 25 wrapper when you want JFR CPU-time sampling (`jdk.CPUTimeSample`) from macOS or another non-Linux host.

Quick start:

```bash
scripts/run-single-benchmark-docker.sh \
  org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.executeQuery
```

Equivalent explicit form:

```bash
scripts/run-single-benchmark-docker.sh \
  --module core/sail/lmdb \
  --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark \
  --method executeQuery
```

What it does:

- runs inside `maven:3.9.11-eclipse-temurin-25`
- mounts the repo at `/workspace`
- keeps a separate Linux Java 25 Maven cache at `/workspace/.m2_repo_linux_j25`
- forces `--enable-jfr --enable-jfr-cpu-times`
- writes the `.jfr` file back into the benchmark module `target/` directory on the host

Useful overrides:

```bash
RDF4J_JMH_DOCKER_PLATFORM=linux/amd64 scripts/run-single-benchmark-docker.sh ...
RDF4J_JMH_DOCKER_IMAGE=maven:3.9.11-eclipse-temurin-25 scripts/run-single-benchmark-docker.sh ...
RDF4J_JMH_DOCKER_M2_REPO=/workspace/.m2_repo_linux_j25 scripts/run-single-benchmark-docker.sh ...
```
