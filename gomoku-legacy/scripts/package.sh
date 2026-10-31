#!/usr/bin/env bash
# 打 2 个 fat jar：
#   gomoku-server.jar  —— 服务端
#   gomoku-client.jar  —— 客户端（联机 + 内嵌人机对战）
# 每个 jar 内含全部 .class、assets/、JDBC 驱动；用户 java -jar X.jar 即可运行
set -e
cd "$(dirname "$0")/.."

# 先编译
bash scripts/build.sh

JDBC=$(ls lib/mysql-connector-j-*.jar | head -1)
if [ -z "$JDBC" ]; then echo "找不到 lib/mysql-connector-j-*.jar"; exit 1; fi

build_fatjar() {
    local out_name=$1
    local main_class=$2
    local stage="target/stage-${out_name%.jar}"
    rm -rf "$stage"
    mkdir -p "$stage"

    # 1) 项目类 + 资源
    cp -r target/classes/. "$stage/"
    # 2) JDBC 驱动展开
    (cd "$stage" && jar xf "../../$JDBC")
    # 3) 客户端额外依赖：FlatLaf + animated-gif-lib（服务端用不上但展开也无害）
    if [ "$out_name" = "gomoku-client.jar" ]; then
        for extra in lib/flatlaf-*.jar lib/animated-gif-lib-*.jar; do
            [ -f "$extra" ] && (cd "$stage" && jar xf "../../$extra")
        done
    fi
    # 4) 干掉所有 MANIFEST + 签名（重打包必须移除签名，否则 SecurityException）
    rm -f "$stage"/META-INF/MANIFEST.MF
    rm -f "$stage"/META-INF/*.SF "$stage"/META-INF/*.RSA "$stage"/META-INF/*.DSA 2>/dev/null || true

    # 5) 重新打包，主类写到 MANIFEST
    jar --create --file "target/$out_name" --main-class "$main_class" -C "$stage" .

    rm -rf "$stage"
    echo "  → target/$out_name ($(du -h "target/$out_name" | cut -f1))"
}

echo "Packaging fat jars..."
build_fatjar gomoku-server.jar server.FiveServer
build_fatjar gomoku-client.jar app.Main

echo ""
echo "Done. 运行方式："
echo "  java -jar target/gomoku-server.jar     # 服务端"
echo "  java -jar target/gomoku-client.jar     # 客户端（点'人机对战'按钮可玩单机 AI）"
