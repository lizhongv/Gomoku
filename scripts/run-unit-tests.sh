#!/usr/bin/env bash
# 运行 src/test/java 下所有带 public static void main 的测试类
# 注：项目无 JUnit 依赖，约定每个测试类自带 main 入口，最后打印 "passed=N failed=0"
set -e
cd "$(dirname "$0")/.."
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
    *) SEP=':' ;;
esac
CP="target/classes${SEP}target/test-classes${SEP}lib/*"
EXIT=0
for cls in $(find src/test/java -name '*Test.java' \
     | sed 's|src/test/java/||;s|\.java$||;s|/|.|g'); do
    # 跳过 E2eTest（需服务器）
    case "$cls" in E2eTest|E2eTest2) continue ;; esac
    echo "=== $cls ==="
    java -Dfile.encoding=UTF-8 -cp "$CP" "$cls" || EXIT=1
done
exit $EXIT
