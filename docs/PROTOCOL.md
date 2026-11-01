# 联机协议 v2

> 适用版本：v2.0.0（开发中）
> 实现：`gomoku-protocol`（编解码与常量）、`gomoku-server`（服务端）、`gomoku-desktop` 的在线页

## 一、传输

- TCP，默认端口 4802。
- 一行一条 UTF-8 JSON：`{"type":"move","data":{"row":7,"col":7}}`。
- 单帧上限 64 KB；超过即断开。
- 客户端连接后先发送 `hello` 协商版本，版本不一致返回 `error(VERSION_MISMATCH)`。

## 二、消息

| 类型 | 方向 | 字段 | 说明 |
| --- | --- | --- | --- |
| `hello` | C→S | `version` | 协议版本，当前 2 |
| `welcome` | S→C | `version`、`server` | 握手成功 |
| `register` | C→S | `username`、`password`、`display` | 注册并登录 |
| `login` | C→S | `username`、`password` | 登录 |
| `logged_in` | S→C | `token`、`name`、`display`、`wins`、`losses`、`rating` | 登录成功；token 用于断线重连 |
| `reconnect` | C→S | `token` | 重连并恢复房间 |
| `list_rooms` | C→S | — | 列出等待中的房间 |
| `rooms` | S→C | `rooms: [{id, black, white}]` | 房间列表 |
| `create_room` | C→S | — | 创建房间，创建者为黑 |
| `quick_match` | C→S | — | 快速匹配：加入最早的等待房间，没有则新建 |
| `join_room` | C→S | `room` | 加入房间，加入者为白 |
| `room_joined` | S→C | `room`、`color`、`opponent` | 已进入房间 |
| `game_start` | S→C | `room`、`black`、`white` | 双方到齐 |
| `move` | C→S | `row`、`col` | 请求落子，由服务端裁决 |
| `state` | S→C | `room`、`moves`、`turn`、`result`、`black`、`white` | 权威局面，客户端按此渲染 |
| `chat` | 双向 | `from`、`text` | 房间内聊天，最多 200 字 |
| `resign` | C→S | — | 认输 |
| `leave` | C→S | — | 离开房间 |
| `game_over` | S→C | `result`、`reason`、`rating` | 结果与本人新积分；reason 为 `five`、`resign`、`leave`、`timeout` |
| `opponent_left` | S→C | `grace` | 对手断线，宽限秒数 |
| `ping` / `pong` | 双向 | — | 心跳 |
| `error` | S→C | `code`、`message` | 错误 |

## 三、权威性

- 服务端持有唯一的 `GameState`：校验轮次、坐标、占用、禁手与胜负。
- 客户端**没有**「宣布胜利」的消息；胜负只由服务端根据落子判定。
- 非法消息返回 `error`，不改变棋局；同一连接每秒最多 40 条消息。
- 断线保留座位 `GOMOKU_RECONNECT_SECONDS`（默认 60 秒），期间用 token 重连恢复；超时判负。
- 每手限时 `GOMOKU_TURN_SECONDS`（默认 60 秒），超时由服务端判当前行棋方负，reason 为 `timeout`。
- 结束后按 ELO（K=32，初始 1200）更新双方积分，并在 `game_over.rating` 中返回本人新积分。
- 对局结束写入 `games.jsonl`，并更新账号胜负。

## 四、错误码

| code | 含义 |
| --- | --- |
| `VERSION_MISMATCH` | 协议版本不兼容 |
| `BAD_MESSAGE` | JSON 格式错误 |
| `UNKNOWN_TYPE` | 未知消息类型 |
| `REGISTER_FAILED` / `LOGIN_FAILED` | 注册/登录失败 |
| `ALREADY_ONLINE` | 账号已在别处登录 |
| `NOT_LOGGED_IN` | 未登录 |
| `NO_ROOM` / `ROOM_FULL` / `NOT_PLAYER` | 房间不存在、已满或不在房间 |
| `NOT_YOUR_TURN` | 未轮到该方 |
| `ILLEGAL_MOVE` | 落点非法（越界、已占用或禁手） |
| `RATE_LIMIT` | 消息过于频繁 |

## 五、版本兼容

- 不兼容的字段或语义变化必须提升 `Codec.VERSION`，服务端拒绝旧版本。
- 新增可选字段不改版本，旧客户端忽略即可。
- 帧格式刻意不用 Java 序列化：可跨语言实现、可读、避免反序列化漏洞。
