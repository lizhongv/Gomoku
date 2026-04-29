#!/usr/bin/env bash
# 编译 main + test 源码到 target/
set -e
cd "$(dirname "$0")/.."

# Windows (Git Bash / MSYS / Cygwin) classpath 分隔符是 ;，Unix 是 :
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
    *) SEP=':' ;;
esac

mkdir -p target/classes target/test-classes

find src/main/java -name '*.java' > target/sources.txt
javac -d target/classes -cp "lib/*" -encoding UTF-8 @target/sources.txt

find src/test/java -name '*.java' > target/test-sources.txt
javac -d target/test-classes -cp "target/classes${SEP}lib/*" -encoding UTF-8 @target/test-sources.txt

echo "Build OK."
