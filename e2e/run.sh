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

pushd "$ROOT_DIR" >/dev/null
mvn -Dmaven.repo.local=$MAVEN_REPO_LOCAL -pl $BOOT_MODULE package >/dev/null
popd >/dev/null

BOOT_TARGET_DIR="$ROOT_DIR/$BOOT_MODULE/target"
BOOT_JAR=$(find "$BOOT_TARGET_DIR" -maxdepth 1 -type f -name 'rdf4j-server-workbench-boot-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n1)
if [[ -z "$BOOT_JAR" ]]; then
  echo "Unable to locate Spring Boot executable jar in $BOOT_TARGET_DIR" >&2
  exit 1
fi

pushd "$ROOT_DIR/e2e" >/dev/null
npm install

BOOT_LOG="boot-app.log"
java -jar "$BOOT_JAR" >"$BOOT_LOG" 2>&1 &
BOOT_PID=$!

cleanup() {
  if ps -p $BOOT_PID >/dev/null 2>&1; then
    kill $BOOT_PID
    wait $BOOT_PID || true
  fi
}
trap cleanup EXIT

printf '%s' "Waiting for Spring Boot server to be ready"
for _ in $(seq 1 60); do
  if curl --silent --fail http://localhost:8080/rdf4j-server/repositories >/dev/null; then
    echo ""
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

npx playwright install --with-deps
npx playwright test

echo "E2E tests finished successfully"

popd >/dev/null

