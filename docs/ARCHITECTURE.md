# 架构

## 模块

| 包 | 职责 | 关键类 |
|---|---|---|
| `protocol` | 客户端 / 服务器共享的协议层 | `Command`（命令字常量）、`Message`（结构化帧）、`Net`（TCP/TLS 工厂）、`SafeObjectInputStream`（白名单反序列化） |
| `server` | 单进程服务器 | `FiveServer`（`Frame`，主线程 accept；每连接一个 `HandshakeRunner`，握手成功后一个 `ClientThread`） |
| `client` | GUI 客户端 | `FiveClient`（主窗口）、`Communication`（网络收发）、`PanelBoard`（棋盘）、`PanelTiming`/`PanelControl`/`PanelMessage`/`PanelUserList`、`Chess`、各种 `Dialog*` |
| `game` | 棋局 / 棋谱模型 | `Game`、`Manual`（继承 Game）、`GameDao`（MySQL）、`ManualDao`（文件序列化到 `manuals/`） |
| `user` | 账号 | `User`、`UserDao`（PreparedStatement）、`PasswordHash`（SHA-256+salt）、`DBConnection`（连接工厂） |
| `ai` | 单机人机对战 | `AiGame`（`JFrame` 入口）、`AiBoard`（棋盘 + 鼠标 + 调度）、`AiChess`（带手数的棋子）、`Evaluate`（启发式估值 + Alpha-Beta） |
| `tool` | 一次性工具 | `CreateDatabase` |

## 服务器线程模型

```
Main (AWT)
  └─ startServser()
       └─ accept loop  ──┬─→ HandshakeRunner #1 (短)
                         │      └─ 握手成功 → 持有 socket → ClientThread #1 (长)
                         ├─→ HandshakeRunner #2
                         │      ...
                         └─→ ...
```

- **accept 主线程**：只做 `ss.accept()` 和派 `HandshakeRunner`，不参与协议。
- **HandshakeRunner**：读首帧，分流登录 / 注册。注册一次性应答即关连接；登录成功后把 `Client` 交给一个独立的 `ClientThread`，自身退出。
- **ClientThread**：循环 `Message.read(c.dis)` → 调度到 `handle(...)`，I/O 异常时走 `cleanupOnDrop()`：通知对手判胜、广播 `DELETE`、从 `clients` 中移除。
- `clients` 是 `CopyOnWriteArrayList`，所有 ClientThread 都在写它（add/remove），所以读路径无锁、写路径开销低。
- `Message.send` 在 `dos` 上 `synchronized`，保证两个线程同时给同一连接写帧不会撕裂帧。

## 客户端线程模型

```
Main (AWT)
  └─ FiveClient (Frame)
       ├─ ActionMonitor (EDT)
       ├─ Communication
       │   ├─ login() (调用线程 EDT)
       │   ├─ ReceiveThread (后台读循环 → dispatch)
       │   └─ TimerThread (对局开始时启)
       └─ Dialogs (modal, EDT)
```

- 所有 UI 操作运行在 EDT。
- `ReceiveThread` 在后台 `Message.read(dis)`，再调用 UI 组件（这里没有严格切回 EDT，是项目原本的写法；对于这种小负载的更新一般不会出问题，但严格说应该 `SwingUtilities.invokeLater`）。
- `TimerThread` 每秒减一格，仅在游戏期间存活。

## 数据流

### 登录
```
Client.login(name, pwd)
  → send LOGIN(name, pwd)
  → server.HandshakeRunner.handleLogin
      → UserDao.getUser  (PreparedStatement, PasswordHash.verify)
      → 若旧明文 → upgradeLegacyPassword (UPDATE 为哈希)
      → send LOGIN(true)
      → 把 Client 加入 clients
      → send TELLNAME(name)
      → 双向广播 ADD（互相告知列表）
      → start ClientThread
```

### 对局
```
A 邀请 B：A → JOIN(B, time)  → server forward → B
B 接受：    B → AGREE(A) → server 分配颜色
                          → broadcast CHANGE(A, playing) / CHANGE(B, playing)
                          → send GUESSCOLOR(blackOrWhite, opponent) (双向)
落子：黑方 → GO(x, y) → server forward → 白方；server 同时把 (x,y) 写到双方 coordinates[step]
胜：    赢方 → WIN → server broadcast CHANGE(*, ready)
                  → send TELLRESULT(win) / TELLRESULT(losses)
                  → recordGame + recordManual（写 MySQL + 写 manuals/.fiv）
                  → resetMatchState
```

### 棋谱回放
```
Client → GAME(myname)
  ← 服务器先发 Message GAME（无 args）
  ← 紧接其后写一个 ObjectOutputStream(ArrayList<Game>)
Client.SafeObjectInputStream 读取 ArrayList<Game> → showGames → DialogGames
用户选中一条 → Client → MANUAL(fileName)
  ← 服务器先发 Message MANUAL
  ← 紧接其后写 ObjectOutputStream(Manual)
Client.SafeObjectInputStream 读取 Manual → playBack → DialogPlayback
```

## AI 模块（单机人机对战）

### 入口

`ai.AiGame` 是独立的 `JFrame`，两条触发路径：

1. `client.FiveClient` 的 "人机对战" 按钮 → `new ai.AiGame()`
2. 直接 `java -cp ... ai.AiGame`（`scripts/run-ai.{bat,sh}`）

`DISPOSE_ON_CLOSE`，关掉 AI 窗口不影响联机会话。

### 线程模型

```
EDT
 └─ AiGame
     ├─ 工具栏按钮 → AiBoard.{restartGame,goback,giveUp,about}
     └─ AiBoard
         ├─ MouseMonitor (EDT)
         │   └─ manGo → putChess → computerGo  ← 关键：仅触发，立即返回
         │
         └─ computerGo                                  ┐
              ├─ 拷贝 boardStatus → snapshot           │ EDT
              ├─ pendingGen++ → myGen                  │
              └─ new SwingWorker.execute()             ┘
                  │
                  ├─ doInBackground (后台线程池)
                  │   └─ new Evaluate(snapshot, color).getTheBestPosition()
                  │        Alpha-Beta 深度 SEARCH_DEPTH (3)
                  │        每层取分数前 SAMPLE_NUMBER (10) 候选位
                  │
                  └─ done() (回到 EDT)
                      ├─ 若 myGen != pendingGen → 丢弃 (用户已重开)
                      ├─ 若 !isGamming         → 丢弃 (人已经赢了 / 退出)
                      └─ putChess(pos, color)  → repaint + isWin 判断
```

### 数据流：一手棋

```
鼠标点击 (col,row)
  → manGo (EDT)
      └─ putChess (EDT)
          ├─ 写 chessList[chessCount++], boardStatus[col][row] = manColor
          ├─ repaint()
          └─ isWin? → 是: JOptionPane + isGamming=false; 否: 翻转 isBlack/isComputerGo
  → computerGo (EDT)
      ├─ snapshot = 深拷贝 boardStatus
      ├─ myGen = ++pendingGen
      └─ SwingWorker.execute() → 立即返回，UI 不阻塞
                                       ↓ (后台 ~ms 到 s 级)
                                  Evaluate 在 snapshot 上推演
                                       ↓
                                  done() 回 EDT
                                       ↓
                                  putChess(...) 同人下棋路径
```

### 并发隔离

`Evaluate` 在自己的快照（构造时深拷贝）上推演 + 撤销，**完全不触碰 EDT 线程的 `AiBoard.boardStatus`**。配合两层失效检查：

| 失效信号 | 触发场景 | 行为 |
|---|---|---|
| `myGen != pendingGen` | 用户在 AI 思考中点了"开始" | done() 直接 return，不落子 |
| `!isGamming` | 用户在 AI 思考中点了"放弃" / 关窗 | done() 直接 return，不落子 |

### 估值要点（`Evaluate.java`）

- **静态位置权重 `staticValue[i][j]`**：对中心格加成，鼓励 AI 不要随便往角落塞
- **空位棋型估值 `evaluateValue(color, col, row, dir)`**：模拟该色棋子放在 `(col,row)` 的某方向上能形成的连子结构（连五 / 活四 / 冲四 / 活三 / 眠三 / 活二）
- **整盘估值 `evaluateGame()`**：把棋盘按 4 个方向（横、竖、左下右上斜、左上右下斜）拆成所有 ≥5 长度的"行"，对每行用 `evaluateLine` 求和；最终 `黑势 - 白势`，再按己方颜色取符号
- **样本剪枝**：每个搜索节点只展开估值前 `SAMPLE_NUMBER=10` 个空位，避免 15×15=225 候选位组合爆炸
- **快剪枝**：找到自己能"连五"的位置直接返回，不再深搜

### 性能

| 棋盘 | 深度 | 样本 | 单步耗时 (Opus 4.7 工作机) |
|---|---|---|---|
| 15×15 | 3 | 10 | < 1 秒 |
| 15×15 | 4 | 10 | 3-5 秒 |
| 15×15 | 3 | 20 | 1-2 秒 |

调高深度棋力提升明显但耗时近指数增长；调高样本数收益较小。

## 持久化

- **MySQL** `fiveChess`：
  - `user(id, name UNIQUE, password [hash], email, level, regDate)`
  - `game(id, gameDate, playerBlack, playerWhite, winner, manualFileName)`
- **文件** `manuals/<black>_<white>_<yyyy_mm_dd_hh_mm_ss>_.fiv`：序列化的 `Manual` 对象（含完整落子序列）。

## 安全边界

- **SQL 注入**：所有 DAO 用 `PreparedStatement`。
- **反序列化**：`SafeObjectInputStream` 白名单只放行 `User` / `Game` / `Manual` 及必要 JDK 基础类型，阻断任意 gadget 链。
- **密码**：SHA-256 + 16 字节随机 salt，存储格式 `sha256$<saltHex>$<hashHex>`，验证常量时间。
- **协议字段**：`Message` 用 `writeUTF + writeInt + writeUTF*N` 长度前缀编码，任何字段（聊天内容等）都可包含任意字符不会撕裂帧。
- **用户名**：客户端 + 服务端双层校验，禁止控制字符 / 空格 / 冒号，长度 ≤ 32。
- **传输**：可选 TLS（系统属性 `-Dfive.tls=true`）；不启用时是明文 TCP，仅适合可信局域网。

## 已知约束 / 改进方向

- 客户端 `ReceiveThread` 直接更新 Swing 组件未切 EDT。低负载下 OK，严格遵循应改 `SwingUtilities.invokeLater`。
- `ObjectOutputStream` 在 GAME / MANUAL 路径每次 new，依赖 TCP 流容错。一次性升级为长寿命 OOS 工作量较大。
- 没有重连机制；ClientThread 一旦读出异常就结束，对手已结算判胜，但当前用户必须重启客户端再登录。
- AI 模块仅做单机；下出的对局不会写入联机 `game` 表 / `manuals/`。要复用棋谱体系需要把 `AiBoard.chessList` 转换成 `game.Manual` 再走 `ManualDao.addManual`。
- AI 启发式仅识别 6 种棋型（连五/活四/冲四/活三/眠三/活二），对"双活三"、"VCF/VCT"等组合威胁的识别要靠 Alpha-Beta 搜索深度间接判断，深度 3 时偶尔会被组合杀漏防。
