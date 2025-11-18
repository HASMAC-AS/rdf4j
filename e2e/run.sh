#!/usr/bin/env bash
#
# Copyright (c) 2023 Eclipse RDF4J contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Distribution License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php.
#
# SPDX-License-Identifier: BSD-3-Clause
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

pushd "$ROOT_DIR" >/dev/null
mvn -pl tools/server-boot package -DskipTests -DskipITs
popd >/dev/null

JAR_PATH=$(ls "$ROOT_DIR"/tools/server-boot/target/rdf4j-server-boot-*.jar | grep -v '\-sources' | head -n 1)
DATA_DIR=$(mktemp -d)
LOG_FILE="$SCRIPT_DIR/server-boot.log"

cleanup() {
  if [[ -n "${BOOT_PID:-}" ]]; then
    kill "$BOOT_PID" 2>/dev/null || true
    wait "$BOOT_PID" 2>/dev/null || true
  fi
  rm -rf "$DATA_DIR"
}
trap cleanup EXIT

java -Dorg.eclipse.rdf4j.appdata.basedir="$DATA_DIR" -jar "$JAR_PATH" >"$LOG_FILE" 2>&1 &
BOOT_PID=$!

for attempt in {1..60}; do
  if curl -fsS "http://localhost:8080/rdf4j-server/repositories" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! curl -fsS "http://localhost:8080/rdf4j-server/repositories" >/dev/null 2>&1; then
  echo "Embedded server failed to start; see $LOG_FILE" >&2
  exit 1
fi

cd "$SCRIPT_DIR"

npm install

npx playwright install --with-deps
npx playwright test

