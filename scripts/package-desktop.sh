#!/usr/bin/env bash
# 打包桌面客户端。
# 用法：bash scripts/package-desktop.sh [app-image|msi|deb|dmg]
# 默认生成 app-image（免安装目录，直接运行 Gomoku/Gomoku.exe）。
# 需要各平台原生工具：Windows MSI 需要 WiX Toolset 3，Linux DEB 需要 fakeroot，macOS DMG 需要 macOS。
set -euo pipefail
cd "$(dirname "$0")/.."

TYPE="${1:-app-image}"
VERSION="${VERSION:-2.0.0}"
OUT="target/package"
INPUT="$OUT/input"

bash mvnw -q -pl gomoku-desktop -am package -DskipTests
rm -rf "$INPUT" "$OUT/Gomoku"
mkdir -p "$INPUT/lib"
cp gomoku-desktop/target/gomoku-desktop-*.jar "$INPUT/"
bash mvnw -q -pl gomoku-desktop dependency:copy-dependencies \
  -DincludeScope=runtime -DoutputDirectory="$(cd "$OUT/input/lib" && pwd)"

JAR=$(basename "$(ls gomoku-desktop/target/gomoku-desktop-*.jar | head -1)")
ARGS=(--type "$TYPE" --name Gomoku --app-version "$VERSION"
      --input "$INPUT" --dest "$OUT"
      --main-jar "$JAR" --main-class io.github.lizhongv.gomoku.desktop.Launcher
      --java-options "-Dfile.encoding=UTF-8")

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    if [ -f packaging/icon.ico ]; then
      ARGS+=(--icon packaging/icon.ico)
    fi
    ;;
  Darwin)
    if [ -f packaging/icon.icns ]; then
      ARGS+=(--icon packaging/icon.icns)
    fi
    ;;
  *)
    if [ -f packaging/icon.png ]; then
      ARGS+=(--icon packaging/icon.png)
    fi
    ;;
esac

case "$TYPE" in
  msi)
    if ! command -v candle >/dev/null 2>&1 && [ -z "$(ls -d /c/Program\ Files\ \(x86\)/WiX\ Toolset\ v3*/bin 2>/dev/null)" ]; then
      echo "生成 MSI 需要 WiX Toolset 3（candle.exe / light.exe）。" >&2
      echo "安装后重试，或在 CI 的 windows-latest 上构建。" >&2
      exit 1
    fi
    ARGS+=(--win-shortcut --win-menu --win-dir-chooser)
    ;;
  deb)
    ARGS+=(--linux-shortcut --linux-menu-group Game)
    ;;
  dmg)
    ARGS+=(--mac-package-name Gomoku)
    ;;
esac

jpackage "${ARGS[@]}"
echo "已生成：$OUT"
