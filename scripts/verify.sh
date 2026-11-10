#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
python3 scripts/check-agent-conventions.py
if [ -n "${MAVEN_BIN:-}" ]; then
    maven_binary=$MAVEN_BIN
elif command -v mvn >/dev/null 2>&1; then
    maven_binary=mvn
elif [ -x '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' ]; then
    maven_binary='/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn'
else
    echo '找不到 Maven，请安装 Maven 或通过 MAVEN_BIN 指定可执行文件。' >&2
    exit 127
fi
"$maven_binary" -q clean verify "$@"
