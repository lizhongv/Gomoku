#!/usr/bin/env bash
# 启动联机服务端。
# 环境变量：GOMOKU_PORT（默认 4802）、GOMOKU_DATA（默认 ~/.gomoku-server）、GOMOKU_RECONNECT_SECONDS（默认 60）。
set -euo pipefail
cd "$(dirname "$0")/.."
bash mvnw -q -pl gomoku-server -am package -DskipTests
exec java -jar gomoku-server/target/gomoku-server.jar
