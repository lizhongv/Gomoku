#!/usr/bin/env bash
# 打包桌面客户端。
# 用法：bash scripts/package-desktop.sh [app-image|msi|deb|dmg]
# 默认生成 app-image（免安装目录，直接运行 Gomoku/Gomoku.exe）。
set -euo pipefail
cd "$(dirname "$0")/.."

TYPE="${1:-app-image}"
VERSION="${VERSION:-2.0.0}"
OUT="target/package"
INPUT="$OUT/input"

./mvnw -q -pl gomoku-desktop -am package -DskipTests
rm -rf "$INPUT" "$OUT/Gomoku"
mkdir -p "$INPUT/lib"
cp gomoku-desktop/target/gomoku-desktop-*.jar "$INPUT/"
./mvnw -q -pl gomoku-desktop dependency:copy-dependencies \
  -DincludeScope=runtime -DoutputDirectory="$(cd "$OUT/input/lib" && pwd)"

JAR=$(basename "$(ls gomoku-desktop/target/gomoku-desktop-*.jar | head -1)")
ARGS=(--type "$TYPE" --name Gomoku --app-version "$VERSION"
      --input "$INPUT" --dest "$OUT"
      --main-jar "$JAR" --main-class io.github.lizhongv.gomoku.desktop.Launcher
      --java-options "-Dfile.encoding=UTF-8")

case "$TYPE" in
  msi) ARGS+=(--win-shortcut --win-menu) ;;
  deb|dmg) ;;
esac

jpackage "${ARGS[@]}"
echo "已生成：$OUT"
