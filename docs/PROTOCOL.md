# 协议

## 帧格式

每条消息（`protocol.Message`）在 TCP 流上的字节排布：

```
+------------------+----------------+----------------+----------------+
| writeUTF(cmd)    | writeInt(N)    | writeUTF(arg0) | ... writeUTF(argN-1)
+------------------+----------------+----------------+----------------+
```

- `writeUTF`：2 字节大端长度 + UTF-8 字节（标准 `DataOutputStream` 行为）。
- `writeInt`：4 字节大端整数，参数个数 `N`（合法范围 `[0, 1024]`）。
- 所有参数都是字符串；数值字段（坐标、用时）用十进制字符串编码。

`Message.send(dos, cmd, args...)` 在 `dos` 上 `synchronized`，保证多线程并发广播时不会出现帧交错。

## 命令清单

### 握手期（连接建立后第一帧）

| 方向 | cmd | args | 说明 |
|---|---|---|---|
| C→S | `login` | `[userName, passWord]` | 登录请求 |
| S→C | `login` | `["true"]` / `["false"]` / `["hasLogin"]` | 登录结果（hasLogin 表示该账号已在别处登录） |
| C→S | `register` | `[]` | 紧接其后通过 `ObjectOutputStream` 写一个 `User` 对象 |
| S→C | `register` | `["true"]` / `["false"]` | 注册结果 |

握手成功后，**服务器主动**推送：

| 方向 | cmd | args | 说明 |
|---|---|---|---|
| S→C | `tellName` | `[name]` | 告诉客户端自己的最终用户名 |
| S→C | `add` | `[name, state]` | 添加在线用户，对每个其他用户都会发一条 |

### 对局期

| 方向 | cmd | args | 说明 |
|---|---|---|---|
| C→S | `join` | `[opponentName, playingTime]` | 邀请对手 |
| S→C | `join` | `[inviterName, playingTime]` | 转发到被邀请方 |
| C→S | `agree` | `[opponentName]` | 接受邀请，触发开局 |
| C→S | `refuse` | `[opponentName]` | 拒绝 |
| S→C | `refuse` | `[refuserName]` | 转发到邀请方 |
| S→C | `change` | `[name, state]` | 状态变化广播（`ready` / `playing`） |
| S→C | `guessColor` | `[color, opponentName]` | 颜色分配（`black` / `white`） |
| C→S | `go` | `[col, row]` | 落子 |
| S→C | `go` | `[col, row]` | 转发到对手 |
| C→S | `win` | `[]` | 我方判定五连 |
| C→S | `giveup` | `[]` | 认输 |
| S→C | `tellResult` | `["win"]` / `["losses"]` | 战果通知 |
| C→S | `talk` | `[content]` | 聊天 |
| S→C | `talk` | `[sender, content]` | 转发广播 |
| C→S | `quit` | `[]` | 主动退出 |
| S→C | `delete` | `[name]` | 通知其他客户端：用户离线 |

### 棋谱查询

GAME / MANUAL 是"两段式"——一帧 `Message`，紧接其后是 `ObjectOutputStream` 写出的对象。

| 方向 | cmd | args | 后续字节 |
|---|---|---|---|
| C→S | `game` | `[userName]` | 无 |
| S→C | `game` | `[]` | `ObjectOutputStream(ArrayList<game.Game>)` |
| C→S | `manual` | `[fileName]` | 无 |
| S→C | `manual` | `[]` | `ObjectOutputStream(game.Manual)` |

客户端读取这两类应答时使用 `protocol.SafeObjectInputStream` 限定白名单（`game.Game` / `game.Manual` + JDK 基础类型）。

## 时序示例

### 注册 + 登录 + 进局
```
A → S : login("alice", "p")
S → A : login("true")
S → A : tellName("alice")
B → S : login("bob", "p")
S → B : login("true")
S → B : tellName("bob")
S → B : add("alice", "ready")
S → A : add("bob", "ready")

A → S : join("bob", "300")
S → B : join("alice", "300")
B → S : agree("alice")
S → A : change("alice", "playing")
S → A : change("bob", "playing")
S → B : change("alice", "playing")
S → B : change("bob", "playing")
S → A : guessColor("black", "bob")
S → B : guessColor("white", "alice")

A → S : go("9","9")        # alice (black)
S → B : go("9","9")
B → S : go("9","10")
S → A : go("9","10")
... 多手 ...
A → S : win()
S → A : change(..., "ready") *2
S → A : tellResult("win")
S → B : change(..., "ready") *2
S → B : tellResult("losses")
```

## 用户名约束

服务端 `validUserName` + 客户端 `DialogRegister.isValidUserName` 双层校验：

- 长度 `[1, 32]`
- 不允许：控制字符（`Character.isISOControl`）、空格、冒号 `:`

## 错误处理

- I/O 异常 → 服务端 `ClientThread.cleanupOnDrop`：判对手胜（若在对局中）、广播 `delete`、从 `clients` 移除。
- 反序列化遇到白名单外的类 → 抛 `InvalidClassException`，注册 / 棋谱接收会失败但不会 RCE。
- `Message.read` 检测到 `argCount > 1024` 抛 `IOException`，防止恶意客户端造大帧消耗内存。
