# RDF4J Maven dependencies — two standard ZIP files

Download **both parts**. Each is an ordinary independently extractable ZIP; together they
contain one complete dependency cache. Extract both into the same destination:

```bash
unzip -o rdf4j-maven-dependencies-6.2.0-SNAPSHOT-2026-11-16-part1.zip -d /absolute/path/to/dependencies
unzip -o rdf4j-maven-dependencies-6.2.0-SNAPSHOT-2026-11-16-part2.zip -d /absolute/path/to/dependencies
```

The shared root is `/absolute/path/to/dependencies/rdf4j-maven-dependencies`. The small README,
settings, inventory and evidence files intentionally appear identically in both parts.
Repository files are distributed between the parts. Neither part alone is the complete cache.

## Pinned source revisions

| Repository | Branch checked | Commit |
| --- | --- | --- |
| `eclipse-rdf4j/rdf4j` | `main` | `8fc7b0137164ffdc1f5d080b943fd362931137dd` |
| `HASMAC-AS/rdf4j` | `main` | `ec7e48248eb20c4f7681d8ea5cbed3100718527a` |

Prepared 2026-11-16 for RDF4J 6.2.0-SNAPSHOT.
The commits define this archive's scope. Future branch changes can require other versions.

## Contents

The cache covers compile, runtime, provided and test dependencies, transitive dependencies,
build/report plugins, optional build profiles and the native classifiers declared by the
pinned RDF4J projects. Supplemental downloads include plugin tooling and dynamic test or
formatting dependencies. Existing reusable release artifacts are retained, so this is a
coverage-checked development cache rather than a minimal set.

There are 10,466 repository files, 3,977
artifact files and 1,476 JARs, totalling 835,573,602 bytes.
POM descriptors, repository origin records and published checksum files are included.

## Use the cache

From the actual RDF4J checkout at a pinned commit, use absolute paths:

```bash
mvn -o -B -ntp \
  -s /absolute/path/to/dependencies/rdf4j-maven-dependencies/repositories.xml \
  -Dmaven.repo.local=/absolute/path/to/dependencies/rdf4j-maven-dependencies/repository \
  -DexcludeReactor=true \
  org.apache.maven.plugins:maven-dependency-plugin:3.11.0:go-offline
```

Use the same `-s` and `-Dmaven.repo.local` arguments for other Maven commands. To select the
expanded profiles, add `-Passembly,benchmarks,jacoco,licence-check,ossrh,formatting,dependency-updates,spring.ide`.
The public settings preserve Maven Central and Eclipse repository IDs needed to match
cached origins. They contain no credentials, proxy or machine-specific settings.

RDF4J's own SNAPSHOT module binaries must be built and installed from the matching checkout.
Java 25 or later and Maven are required separately. JDKs, Maven distributions, Node.js, npm
packages, external services and downloads outside Maven are not included.

## Verification and limits

- `upstream-offline`: offline dependency resolution succeeded; profiles: `assembly,benchmarks,jacoco,licence-check,ossrh,formatting,dependency-updates,spring.ide`.
- `fork-offline`: offline dependency resolution succeeded; profiles: `assembly,benchmarks,jacoco,licence-check,ossrh,formatting,dependency-updates,spring.ide`.
- `supplemental-offline`: offline dependency resolution succeeded; profiles: `default`.

The expanded profile graphs cover the default dependency coordinates for these pinned
revisions. These checks verify dependency resolution; they do not claim a full compilation,
test suite, frontend build or release build. Exact successful commands, profiles and logs
are in `evidence/`. Names such as `upstream-main`, `fork-main`, `supplemental` and `auxiliary-*`
refer to temporary verification inputs. Source checkouts and synthetic input POMs are not
included; Maven artifact POMs under `repository/` are included.

Published historical Japicmp baselines are included when available. The project configures
`ignoreMissingOldVersion=true`; absent historical versions listed below are expected
comparison omissions, not unresolved required dependencies. Responses are in the receipts.

- `org.eclipse.rdf4j:rdf4j-opentelemetry:6.0.0:jar`

Every included JAR/WAR/ZIP was checked for CRC errors; available published SHA-1 and SHA-256
companions were checked, and both final ZIPs were CRC-tested. After extracting both parts,
verify the complete extracted payload with:

```bash
cd /absolute/path/to/dependencies/rdf4j-maven-dependencies
sha256sum --check manifest/SHA256SUMS
```

On macOS use `shasum -a 256 --check manifest/SHA256SUMS`. The separate
`rdf4j-maven-dependencies-6.2.0-SNAPSHOT-2026-11-16-SHA256SUMS.txt` checks the two ZIP downloads themselves. The inventory covers every
payload file except the checksum list itself. Failed-download markers and incomplete
transfers are excluded. `manifest/artifacts.tsv` supplies artifact coordinates and hashes.
