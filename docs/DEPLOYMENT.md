# 部署

> 适用版本：v2.0.0（开发中）

## 一、联机服务端

### 启动

```bash
./scripts/run-server.sh          # Windows 用 scripts\run-server.bat
```

或先打包再运行：

```bash
./mvnw -pl gomoku-server -am package -DskipTests
java -jar gomoku-server/target/gomoku-server.jar
```

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `GOMOKU_PORT` | `4802` | 监听端口；`0` 由系统分配（测试用） |
| `GOMOKU_DATA` | `~/.gomoku-server` | 账号、对局记录目录 |
| `GOMOKU_RECONNECT_SECONDS` | `60` | 断线重连宽限秒数，超时判负 |
| `GOMOKU_TURN_SECONDS` | `60` | 每手限时秒数，超时判当前行棋方负 |

数据文件：

- `users.json`：账号与胜负，密码 PBKDF2-SHA256 加盐，不存明文。
- `games.jsonl`：每局一行，含双方、结果、着法与时间。

备份时停服后复制该目录即可；写入采用临时文件替换，避免半写损坏。

### 运维建议

- 默认无 TLS；对公网开放时置于 Nginx/HAProxy 的 TLS 之后，并限制来源。
- 服务端无状态管理后台，账号由客户端注册；生产环境应在注册处加验证码或邀请码（待补）。
- 客户端断线会保留座位，服务端重启后房间丢失，需要重新开局。

### 系统服务（systemd 示例）

```ini
[Unit]
Description=Gomoku server
After=network.target

[Service]
WorkingDirectory=/opt/gomoku
ExecStart=/usr/bin/java -jar /opt/gomoku/gomoku-server.jar
Environment=GOMOKU_PORT=4802
Environment=GOMOKU_DATA=/var/lib/gomoku
Restart=on-failure
User=gomoku

[Install]
WantedBy=multi-user.target
```

## 二、桌面客户端

### 直接运行

```bash
./scripts/run-desktop.sh
```

### 打包

```bash
bash scripts/package-desktop.sh app-image   # 免安装目录，target/package/Gomoku/Gomoku.exe
bash scripts/package-desktop.sh msi         # Windows 安装包，需要 WiX Toolset 3
bash scripts/package-desktop.sh deb         # Linux 安装包，需要 fakeroot
bash scripts/package-desktop.sh dmg         # macOS 安装包，需要在 macOS 上构建
```

- 图标在 `packaging/icon.ico` / `icon.png`，可用 `python packaging/make-icon.py` 重新生成；macOS 的 `.icns` 由 CI 用 `iconutil` 生成。
- `.github/workflows/package.yml` 在推送 `v*` 标签时于 Windows / Linux / macOS 三个平台构建 MSI、DEB、DMG 并创建 GitHub Release；手动触发（workflow_dispatch）只产出构件，不发版。
- **代码签名尚未接入**。正式售卖前需要：
  - Windows：`signtool sign /fd SHA256 /tr <时间戳服务> /td SHA256 /f cert.pfx <安装包>`；
  - macOS：`codesign --sign "Developer ID Application: ..." --deep --options runtime` 并做公证（notarization）。
- 自动更新尚未实现；当前版本需要用户手动下载新安装包。

## 三、端口

| 组件 | 默认端口 |
| --- | --- |
| 联机服务端 | 4802 |
| 桌面客户端 | 无需监听 |
