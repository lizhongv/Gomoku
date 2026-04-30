#!/usr/bin/env bash
# 把 fat jar 打成自带 JRE 的可分发应用
# 无 WiX 时产出 app-image（绿色目录，含 .exe）
# 装了 WiX 3.x 时产出 .msi 安装包
set -e
cd "$(dirname "$0")/.."

# 先确保 fat jar 已构建
bash scripts/package.sh

# 检测包类型
PKG_TYPE="app-image"
if command -v candle >/dev/null 2>&1 && command -v light >/dev/null 2>&1; then
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*) PKG_TYPE="msi" ;;
    esac
fi
echo ""
echo "Package type: $PKG_TYPE"
echo ""

mkdir -p target/installer
rm -rf "target/installer/Gomoku-Server" "target/installer/Gomoku-Client" \
       "target/installer/Gomoku-Server-1.0.0.msi" "target/installer/Gomoku-Client-1.0.0.msi" \
       2>/dev/null || true

build_app() {
    local jar=$1; local name=$2
    local extra=()
    if [ "$PKG_TYPE" = "msi" ]; then
        extra=(
            --win-menu --win-menu-group Gomoku
            --win-shortcut
            --win-dir-chooser
        )
    fi
    # 干净的输入目录：只放当前 jar，避免 jpackage 扫描已生成的 target/installer/
    local input_dir="target/jpackage-input"
    rm -rf "$input_dir"
    mkdir -p "$input_dir"
    cp "target/$jar" "$input_dir/"

    jpackage \
        --type "$PKG_TYPE" \
        --input "$input_dir" \
        --main-jar "$jar" \
        --name "$name" \
        --app-version 1.0.0 \
        --vendor "Gomoku Project" \
        --dest target/installer \
        --java-options "-Dfile.encoding=UTF-8" \
        "${extra[@]}"

    rm -rf "$input_dir"
    echo "  → target/installer/${name}*"
}

echo "Packaging Server ..."
build_app gomoku-server.jar Gomoku-Server

echo "Packaging Client ..."
build_app gomoku-client.jar Gomoku-Client

echo ""
echo "Done. 产物在 target/installer/"
ls -la target/installer/ 2>/dev/null

# app-image 模式：压成 zip 方便分发
if [ "$PKG_TYPE" = "app-image" ]; then
    echo ""
    echo "Zipping for distribution..."
    if command -v zip >/dev/null 2>&1; then
        (cd target/installer && \
            rm -f Gomoku-Server-1.0.0.zip Gomoku-Client-1.0.0.zip && \
            zip -qr Gomoku-Server-1.0.0.zip Gomoku-Server && \
            zip -qr Gomoku-Client-1.0.0.zip Gomoku-Client)
    elif command -v powershell >/dev/null 2>&1 || command -v powershell.exe >/dev/null 2>&1; then
        powershell -NoProfile -Command "
            cd 'target/installer';
            Compress-Archive -Force -Path Gomoku-Server -DestinationPath Gomoku-Server-1.0.0.zip;
            Compress-Archive -Force -Path Gomoku-Client -DestinationPath Gomoku-Client-1.0.0.zip
        "
    else
        echo "  跳过：未找到 zip 或 powershell"
    fi
    ls -lh target/installer/*.zip 2>/dev/null
fi
