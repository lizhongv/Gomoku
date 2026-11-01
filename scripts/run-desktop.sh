#!/usr/bin/env bash
# 启动 JavaFX 桌面客户端。
set -euo pipefail
cd "$(dirname "$0")/.."

bash mvnw -q -pl gomoku-core,gomoku-ai,gomoku-protocol -am install -DskipTests
bash mvnw -q -pl gomoku-desktop javafx:run
