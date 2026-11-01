#!/usr/bin/env bash
# 生成桌面客户端截图到 docs/images/desktop/。
set -euo pipefail
cd "$(dirname "$0")/.."
TARGET="$PWD/docs/images/desktop"
mkdir -p "$TARGET"
./mvnw -q -pl gomoku-core,gomoku-ai -am install -DskipTests
./mvnw -q -pl gomoku-desktop javafx:run "-Djavafx.args=--screenshot=$TARGET"
echo "截图已输出到 $TARGET"
