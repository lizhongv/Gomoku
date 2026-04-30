# Gomoku 五子棋 · Java C/S + 单机 AI

基于 Java AWT/Swing + Socket + MySQL 的五子棋。两种玩法：

- **联机对战**：服务端监听 `4801` 端口，多个客户端同时登录、邀请、对局、聊天、回放历史棋谱。
- **人机对战**：内置 Alpha-Beta 剪枝 AI（深度 3 + 启发式估值），单机即玩，可从客户端首页"人机对战"按钮启动。

---

## 产品功能（v1.0 阶段一）

阶段一重塑后客户端从工程 demo 升级为产品级体验，七大模块均已交付：

1. **现代化 UI**：FlatLaf + 自研 Theme/Tokens/Scene 体系，4 套皮肤主题（Classic 经典木纹 / DarkWood 暗夜 / Jade 玉石 / Neon 霓虹）运行时切换；HomeScene/LobbyScene/GameScene/ReviewScene/ShopScene/SettingsScene 全新视觉。
2. **联机 / 人机对战**：统一 GameSession 抽象（OnlineGameSession + AiGameSession），同一 GameScene 适配两种模式。
3. **AI 复盘**：每局结束自动生成 ReviewReport，评分曲线 + BRILLIANT / GOOD / OK / INACCURACY / MISTAKE / BLUNDER 六级失误标注。
4. **段位系统**：本地 ELO（青铜→白银→黄金→铂金→钻石→大师 6 段位）；联机用 ELO 系数，人机分难度分 0~15 分。
5. **聊天表情**：8 个快捷短语，对端在 ChatPanel 渲染为 GChip 而非纯文本。
6. **棋谱 GIF 导出**：基于 animated-gif-lib 输出 30 手 < 10 秒（CC0 共享）。
7. **商店 + 金币 + 每日签到**：联机胜利 +10 / AI 胜利 +5 / 每日签到奖励，购买皮肤扣金币 + 即时切换。

---

## 功能

**联机部分**
- 用户注册 / 登录（密码 SHA-256 加盐存储，旧明文用户首次登录自动迁移）
- 在线用户列表 + 状态广播（`ready` / `playing`）
- 邀请对局 + 倒计时确认
- 19×19 棋盘联机对局，服务器随机分配黑/白
- 实时五连判定（横/竖/双对角四方向）
- 认输 / 胜利 / 对局中掉线 三种结束方式都会写库
- 历史棋局列表 + 棋谱逐手回放
- 对局聊天（支持任意特殊字符）
- 可选 TLS（按 JSSE 标准 keystore 配置）

**单机 AI 部分**
- 15×15 棋盘，HomeScene 进入时弹难度对话框（简单 / 中等 / 困难）
- 启发式估值：连五 / 活四 / 冲四 / 活三 / 眠三 / 活二 + 中心位置加权
- Alpha-Beta 剪枝深度 3，每层取分数前 10 个候选位
- 搜索在后台线程跑，UI 不卡顿；带 1-3 秒思考延时模拟"在想"
- AI 局结束同样写入 ReviewReport，可在 ReviewScene 复盘 / 导出 GIF

---

## 目录结构

```
Gomoku/
├── README.md
├── pom.xml                       # Maven 构建（可选，IDE 直接识别）
├── .gitignore
├── db.properties.example         # DB 配置模板
├── docs/
│   ├── ARCHITECTURE.md           # 模块 / 线程 / 数据流
│   └── PROTOCOL.md               # 消息帧格式与命令清单
├── scripts/                      # 一键脚本（Windows .bat + Unix .sh）
│   ├── build.{bat,sh}            # 编译
│   ├── init-db.{bat,sh}          # 建库 + 建表
│   ├── run-server.{bat,sh}       # 启动联机服务器
│   ├── run-client.{bat,sh}       # 启动联机客户端
│   ├── run-ai.{bat,sh}           # 直接启动单机人机对战
│   └── run-tests.{bat,sh}        # 跑端到端测试（需服务器先起）
├── assets/                       # 静态资源
│   ├── board.jpg
│   ├── black.jpg
│   └── white.jpg
├── manuals/                      # 运行时棋谱（.fiv 文件，已 gitignore）
├── lib/
│   └── mysql-connector-j-8.4.0.jar
└── src/
    ├── main/java/
    │   ├── app/                  # 客户端启动入口（Main：FlatLaf + Profile + SkinManager 初始化 → Router）
    │   ├── ui/                   # 重塑后的视图层
    │   │   ├── component/        # GButton / GCard / GChip / GAvatar / GToast / GBoard / GDialog / ...
    │   │   ├── design/           # Theme + Tokens + Fonts + BoardStyle + PieceStyle
    │   │   └── scene/            # HomeScene / LobbyScene / GameScene / ReviewScene / ShopScene / SettingsScene + Router
    │   ├── feature/              # 七大产品模块
    │   │   ├── skin/             # SkinManager + 4 套皮肤
    │   │   ├── sound/            # SoundManager + 6 音效（程序化 WAV）
    │   │   ├── review/           # ReviewEngine 复盘
    │   │   ├── rank/             # RankCalculator + Tier
    │   │   ├── emote/            # EmoteParser + EmotePack
    │   │   ├── share/            # GifExporter
    │   │   └── shop/             # CoinManager + CoinEarnSubscriber
    │   ├── net/                  # GameSession 抽象 + Communication（OnlineGameSession + AiGameSession）
    │   ├── server/               # FiveServer
    │   ├── protocol/             # 共享协议（Command, Message, Net, SafeObjectInputStream）
    │   ├── game/                 # 棋局 / 棋谱实体与 DAO
    │   ├── user/                 # User、UserDao、PasswordHash、DBConnection
    │   ├── profile/              # 本地玩家档案 Profile（ELO / 段位 / 皮肤 / 金币 / 签到）
    │   ├── config/               # AppConfig 全局配置
    │   ├── ai/                   # AI 估值（Evaluate）— 旧 AiGame/AiBoard/AiChess 已删，被 AiGameSession 替代
    │   └── tool/                 # CreateDatabase（建库工具）
    └── test/java/
        ├── E2eTest.java          # 端到端主流程（28 断言）
        ├── E2eTest2.java         # 边界测试（GIVEUP / TALK 特殊字符，8 断言）
        └── feature/.../*Test.java  # 单元测试（32 断言：Profile / EventBus / Skin / Review / Rank / Emote / Gif）
```

---

## 环境

| 组件 | 版本 | 备注 |
|---|---|---|
| JDK | 17+（实测 17） | AWT/Swing 即用，无外部 GUI 依赖 |
| MySQL | 5.7 / 8.x | 已用 8.0.45 验证 |
| MySQL Connector/J | 8.4.0 | 已随仓库 `lib/` 提供 |
| FlatLaf | 3.4.1 | 现代化 LookAndFeel，已随仓库 `lib/` 提供 |
| animated-gif-lib | 1.4 | 棋谱 GIF 导出，已随仓库 `lib/` 提供 |
| Maven（可选） | 3.6+ | 用 `pom.xml` 时需要；不用 Maven 可直接 `javac` |
| 操作系统 | Windows / Linux / macOS | 任何带 GUI 的环境 |

数据库默认连接：`jdbc:mysql://localhost:3306/fiveChess`，账号 `root` / `12345678`。

修改方式（按优先级）：

1. 环境变量：`FIVE_DB_URL` / `FIVE_DB_USER` / `FIVE_DB_PASSWORD`
2. 工作目录下 `db.properties`：复制 `db.properties.example` 改填
3. 内置默认值（适合本地调试）

> 单机人机对战不需要 MySQL，可以跳过下面的 0、2 步。

---

## 运行

### 0. 准备 MySQL（仅联机需要）

确保 MySQL 服务已启动，且 `root/12345678` 可登录（或按上面改成你的凭据）。

### 1. 编译

```bash
# Windows
scripts\build.bat

# Linux / macOS
./scripts/build.sh

# 或者用 Maven
mvn -q package
```

输出到 `target/classes/`（脚本方式）或 `target/`（Maven）。

### 2. 初始化数据库（仅联机需要）

```bash
# Windows
scripts\init-db.bat

# Linux / macOS
./scripts/init-db.sh
```

幂等——再跑一次也不会报错（`if not exists`）。

### 3a. 联机模式

启动服务器：

```bash
scripts\run-server.bat   # Windows
./scripts/run-server.sh  # Linux / macOS
```

会弹出"JAVA 五子棋服务器"窗口，监听 4801。

启动客户端（同一台机器可多开，分别注册两个账号对战）：

```bash
scripts\run-client.bat   # Windows
./scripts/run-client.sh  # Linux / macOS
```

跨机器对战：把客户端 GUI 上的"服务器IP"改成服务器机器的 IP。

### 3b. 单机人机对战

启动客户端（同 3a）后，在 HomeScene 点 **"人机对战"** 按钮，会弹出难度选择对话框，确认后进入 GameScene 与本地 AI 对局。无需先登录、无需服务器。

```bash
scripts\run-ai.bat   # Windows  (= 启动 app.Main，人机对战是首页里的一种模式)
./scripts/run-ai.sh  # Linux / macOS
```

> 阶段一重塑后，独立的 `ai.AiGame` 窗口已被删除，统一为 GameScene + AiGameSession 组合。原来的 "先手 / 白棋" 复选框迁移为难度选择对话框。

### 4. 跑端到端测试（仅联机功能，可选）

在联机服务器已经启动的前提下：

```bash
scripts\run-tests.bat   # Windows
./scripts/run-tests.sh  # Linux / macOS
```

期望输出 `passed=28 failed=0` 和 `passed=8 failed=0`（联机 E2E 合计 36 个断言）。

加上 `scripts/run-unit-tests.{bat,sh}` 的 32 个单元断言（Profile / EventBus / Skin / Review / Rank / Emote / Gif），全套 **68 断言**。

---

## 打包成可分发软件

把代码 + 资源 + JDBC 驱动 + JRE 全打到自带运行时的目录，用户**无需安装 Java** 就能双击 .exe 启动。

```bash
# 1) 先打 fat jar（target/gomoku-server.jar 和 gomoku-client.jar，含 JDBC，可直接 java -jar 跑）
bash scripts/package.sh

# 2) 再用 jpackage 打 app-image（自带 JRE，目录大小 ~128MB；脚本自动压成 .zip ~48MB）
bash scripts/package-installer.sh
```

产物：

- `target/gomoku-server.jar` / `target/gomoku-client.jar`：fat jar，需要目标机器有 JRE 17+
- `target/installer/Gomoku-Server/`：自带 JRE 的服务端目录，运行 `Gomoku-Server.exe`
- `target/installer/Gomoku-Client/`：自带 JRE 的客户端目录（联机 + 内嵌人机对战），运行 `Gomoku-Client.exe`
- `target/installer/Gomoku-{Server,Client}-1.0.0.zip`：上面两个目录的压缩包，分发用

> 当系统装了 [WiX 3.x](https://wixtoolset.org/) 时，`package-installer.sh` 会自动改打 `.msi` 真正的 Windows 安装包（带卸载器、开始菜单快捷方式）。

**客户端运行时的可写目录**：`~/.gomoku/manuals/`（棋谱）、`~/.gomoku/db.properties`（DB 配置，可选）。安装包不会写到自身目录，可以装在 Program Files。

---

## 启用 TLS（联机模式可选）

```bash
# 生成自签证书
keytool -genkeypair -alias five -keyalg RSA -keysize 2048 -storepass changeit \
        -dname "CN=localhost" -keystore server.jks
keytool -export -alias five -storepass changeit -keystore server.jks -file five.cer
keytool -import -alias five -storepass changeit -keystore client.jks -file five.cer

# 启动服务端
java -Dfive.tls=true \
     -Djavax.net.ssl.keyStore=server.jks -Djavax.net.ssl.keyStorePassword=changeit \
     -cp "target/classes;lib/*" server.FiveServer

# 启动客户端
java -Dfive.tls=true \
     -Djavax.net.ssl.trustStore=client.jks -Djavax.net.ssl.trustStorePassword=changeit \
     -cp "target/classes;lib/*" app.Main
```

---

## AI 调参

`src/main/java/ai/Evaluate.java` 顶部常量：

| 常量 | 默认 | 含义 |
|---|---|---|
| `SEARCH_DEPTH` | 3 | Alpha-Beta 搜索深度。+1 → 棋力强，单步耗时 ~×5 |
| `SAMPLE_NUMBER` | 10 | 每层只展开估值前 N 个候选位。+10 → 棋力略强，单步耗时 ~×3 |
| `FIVE / HUO_FOUR / ...` | 50000 / 5000 / ... | 棋型权重表，可自行调优 |

`AiBoard.ROWS / COLS = 14` 控制棋盘大小（实际 `(ROWS+1) × (COLS+1) = 15×15`）。

---

## 进一步阅读

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)：模块、线程、数据流
- [docs/PROTOCOL.md](docs/PROTOCOL.md)：消息帧格式 + 全部命令字段
