#!/usr/bin/env bash
# 需要服务器已经启动；脚本会顺序跑 E2eTest 和 E2eTest2
set -e
cd "$(dirname "$0")/.."
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
    *) SEP=':' ;;
esac
java -Dfile.encoding=UTF-8 -cp "target/classes${SEP}target/test-classes${SEP}lib/*" E2eTest
java -Dfile.encoding=UTF-8 -cp "target/classes${SEP}target/test-classes${SEP}lib/*" E2eTest2
echo
echo "All tests passed."
