#!/bin/bash
set -eu
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
export JAVA_HOME="$ROOT/tools/jdk-25.0.5+8"
export PATH="$JAVA_HOME/bin:$ROOT/tools/apache-maven-3.9.11/bin:$PATH"
export MAVEN_OPTS='-Xmx2g -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
python3 - <<'PY'
import os,urllib.parse
p=urllib.parse.urlparse(os.environ['HTTPS_PROXY'])
open('experiments/overlay/settings.xml','w').write(f'<settings><proxies><proxy><id>workspace</id><active>true</active><protocol>http</protocol><host>{p.hostname}</host><port>{p.port}</port></proxy></proxies></settings>')
PY
exec mvn -B -s experiments/overlay/settings.xml -Dmaven.repo.local="$ROOT/m2" -Dcheckstyle.skip -Dspotless.skip=true -Dformatter.skip=true "$@"
