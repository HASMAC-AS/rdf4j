#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
TOOLCHAIN=$(cd ../toolchain && pwd)
export JAVA_HOME="$TOOLCHAIN/jdk-25.0.5+8"
export PATH="$JAVA_HOME/bin:$TOOLCHAIN/apache-maven-3.9.11/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
# The executor proxy changes per command. Derive it at execution, never persist credentials.
SETTINGS=$(mktemp "$TOOLCHAIN/maven-settings.XXXXXX.xml")
export SETTINGS
python3 - <<'PY'
import os,urllib.parse
from xml.sax.saxutils import escape
p=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',''))
proxy=f'<proxies><proxy><id>environment</id><active>true</active><protocol>http</protocol><host>{escape(p.hostname)}</host><port>{p.port}</port><nonProxyHosts>localhost|127.0.0.1</nonProxyHosts></proxy></proxies>' if p.hostname else ''
open(os.environ['SETTINGS'],'w').write(f'<settings>{proxy}</settings>')
PY
exec mvn -s "$SETTINGS" -B -ntp -Dmaven.repo.local=.m2_repo "$@"
