#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
    *) SEP=':' ;;
esac
# 阶段一重塑后，独立 AI 入口 ai.AiGame 已删；改为启动客户端，从首页点 [人机对战]
java -cp "target/classes${SEP}lib/*" app.Main
