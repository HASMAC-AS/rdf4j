# Exact toolchain recovery for admission follow-up

Recovered on 2026-11-17. **The recorded Temurin JDK and mvnd distribution were restored at their original paths; no JDK or Maven substitution is required.** This recovery did not launch Java, javac, Maven, a build, a test, or a benchmark.

## Ready paths

| Tool | Restored path |
|---|---|
| JDK root | `/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8` |
| Java | `/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8/bin/java` |
| Compiler | `/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8/bin/javac` |
| Maven launcher | `/workspace/scratch/d0a0e51b9386/toolchain/maven-mvnd-1.0.6-linux-amd64/mvn/bin/mvn` |

The release file identifies Temurin **25.0.5+8-LTS**. The Maven core JAR's embedded properties identify **3.9.16**. These identities were read from files without executing the tools.

## Library-first search and official recovery

Targeted Library searches covered the recorded OpenJDK archive, `jdk-25.0.5+8`, `25.0.5`, the mvnd distribution, `mvnd`, `toolchain`, `Temurin`, `OpenJDK`, `JDK`, `Maven`, and `jdk.tar.gz`. The first 200 listed Library files were also checked for relevant archive names. These bounded searches did not locate the exact old toolchain. They exposed an unrelated Zulu 26 archive and an installer script for SapMachine 25.0.4.1; neither was used. This is a bounded retrieval result, not proof that no other Library copy exists.

The old toolchain parent directory also contained a pre-existing Zulu 25.0.5.0.3 installation and standalone Maven 3.9.16. Those files were left intact. The standalone Maven launcher and core already matched their historical hashes, but the Zulu archive did not match the recorded Temurin archive.

The exact archives were then downloaded successfully from their official publishers:

- [Eclipse Temurin 25.0.5+8 Linux x64 archive](https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.5%2B8/OpenJDK25U-jdk_x64_linux_hotspot_25.0.5_8.tar.gz): **141,341,188 bytes**, SHA-256 `68982529869257d0ee6b824ec5aa270ff8b118bb438670da0b97ebdab178d7fb`. The hash exactly matches the prior recorded JDK archive.
- [Apache mvnd 1.0.6 Linux amd64 archive](https://downloads.apache.org/maven/mvnd/1.0.6/maven-mvnd-1.0.6-linux-amd64.tar.gz): **24,295,403 bytes**, SHA-256 `88fd474fd3f21b33ec1e6a75950f6abbe493d63a5e2b429475f5230b3ee6cb24`. This matches the official [mvnd release metadata](https://github.com/apache/maven-mvnd/releases).

Verified archives are retained outside the repository under `/workspace/scratch/d0a0e51b9386/toolchain/admission-recovered-archives-v1/`. Downloads were verified before extraction. Each archive had exactly the expected top-level directory, was extracted with Python's `tarfile.extractall(filter="data")` into a new staging directory, and was moved to its absent original destination. Existing files were not replaced.

## Identity evidence

`jdk-extraction-and-identity-v1.json` compares the restored JDK with every JDK input in the unchanged `retention-followup/comparison-v1/protocol.json`. **All 44 input records match**, including the complete 16-file configuration tree, Java launcher, module image, and all recorded shared libraries. The compiler hash separately matches the earlier runtime assembly receipt. The referenced protocol's hash is recorded in the new receipt.

`maven-extraction-and-identity-v1.json` records the complete 121-file extracted distribution and confirms these historical hashes:

- Maven launcher: `f9381d0cb98abaaf9592dae421eddc497e84ed9bfb723b84c111d1350863c3a2`.
- Maven core: `5d45c72e3dbfab8b68d15ad4f12777b7d9b5fe4d4adc99c3bd51fb9641fab009`.

The two `official-*-download-attempt-v1.json` receipts preserve exact download commands, timestamps, zero exit statuses, byte counts, and expected hashes. Their original temporary destination paths were subsequently moved to the archive directory identified above.

## Scope and handoff

No production source, dependency cache, frozen source, protocol, descriptor, or prior evidence was edited. No Git mutation was performed. The original path-based launchers can now be used by the root agent, which retains the exclusive Java execution slot. This recovery establishes matching recorded toolchain bytes; it does not pool or rerun any prior study measurements.

