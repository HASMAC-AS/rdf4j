#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "${SCRIPT_DIR}/.." && pwd)
LMDB_MODULE="${PROJECT_ROOT}/core/sail/lmdb"
JAR=$(ls "${LMDB_MODULE}"/target/rdf4j-sail-lmdb-*.jar 2>/dev/null | head -n 1)
DEPS_DIR="${LMDB_MODULE}/target/dependency"

if [[ -z "${JAR}" ]]; then
  echo "Error: LMDB module artifacts not found." >&2
  echo "Build them with 'mvn -pl core/sail/lmdb -DskipTests package dependency:copy-dependencies'" >&2
  exit 1
fi

CLASSPATH="${JAR}"
if [[ -d "${DEPS_DIR}" ]]; then
  CLASSPATH="${CLASSPATH}:${DEPS_DIR}/*"
fi

exec java -cp "${CLASSPATH}" org.eclipse.rdf4j.sail.lmdb.LmdbMaintenanceCli "$@"
