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

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
BOOT_MODULE="tools/server-workbench-boot"
MAVEN_REPO_LOCAL=".m2_repo"

echo "[run.sh] Using repository root: $ROOT_DIR"
echo "[run.sh] Packaging Spring Boot module: $BOOT_MODULE"
pushd "$ROOT_DIR" >/dev/null
mvn -Dmaven.repo.local=$MAVEN_REPO_LOCAL -pl $BOOT_MODULE package >/dev/null
popd >/dev/null
echo "[run.sh] Maven packaging finished"

BOOT_TARGET_DIR="$ROOT_DIR/$BOOT_MODULE/target"
echo "[run.sh] Searching for bootable jar under $BOOT_TARGET_DIR"
BOOT_JAR=$(find "$BOOT_TARGET_DIR" -maxdepth 1 -type f -name 'rdf4j-server-workbench-boot-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n1)
if [[ -z "$BOOT_JAR" ]]; then
  echo "Unable to locate Spring Boot executable jar in $BOOT_TARGET_DIR" >&2
  exit 1
fi
echo "[run.sh] Found jar: $BOOT_JAR"

pushd "$ROOT_DIR/e2e" >/dev/null
echo "[run.sh] Running npm install for Playwright harness"
npm install
echo "[run.sh] npm install completed"

BOOT_LOG="boot-app.log"
echo "[run.sh] Starting Spring Boot application; logging to $BOOT_LOG"
java -jar "$BOOT_JAR" >"$BOOT_LOG" 2>&1 &
BOOT_PID=$!
echo "[run.sh] Spring Boot PID: $BOOT_PID"

cleanup() {
  if ps -p $BOOT_PID >/dev/null 2>&1; then
    echo "[run.sh] Cleaning up Spring Boot process $BOOT_PID"
    kill $BOOT_PID
    wait $BOOT_PID || true
  fi
}
trap cleanup EXIT

echo "[run.sh] Waiting for Spring Boot server to be ready"
for _ in $(seq 1 60); do
  if curl --silent --fail http://localhost:8080/rdf4j-server/repositories >/dev/null; then
    printf '\n'
    echo "[run.sh] Spring Boot server responded"
    break
  fi
  printf '%s' "."
  sleep 1
done

if ! curl --silent --fail http://localhost:8080/rdf4j-server/repositories >/dev/null; then
  echo ""
  echo "Spring Boot server failed to start in time"
  tail -n 100 "$BOOT_LOG" || true
  exit 1
fi

echo "[run.sh] Installing Playwright browsers"
npx playwright install --with-deps
echo "[run.sh] Playwright browsers installed"
npx playwright test

echo "E2E tests finished successfully"

popd >/dev/null

