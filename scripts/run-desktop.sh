#!/usr/bin/env bash
# 启动 JavaFX 桌面客户端。
set -euo pipefail
cd "$(dirname "$0")/.."

./mvnw -q -pl gomoku-core,gomoku-ai -am install -DskipTests
./mvnw -q -pl gomoku-desktop javafx:run
