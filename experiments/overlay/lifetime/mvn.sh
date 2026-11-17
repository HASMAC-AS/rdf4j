#!/bin/bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$ROOT"
export JAVA_HOME="$ROOT/experiments/overlay/tools/jdk-25.0.5+8"
export PATH="$JAVA_HOME/bin:$ROOT/experiments/overlay/tools/apache-maven-3.9.11/bin:$PATH"
export MAVEN_OPTS='-Xmx2g -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
python3 - <<'PROXY'
import os,urllib.parse
p=urllib.parse.urlparse(os.environ['HTTPS_PROXY'])
open('experiments/overlay/settings.xml','w').write(f'<settings><proxies><proxy><id>workspace</id><active>true</active><protocol>http</protocol><host>{p.hostname}</host><port>{p.port}</port></proxy></proxies></settings>')
PROXY
exec mvn -B -ntp -s experiments/overlay/settings.xml -Dmaven.repo.local=.m2_repo "$@"
