# Gomoku 产品级重塑（阶段一）· 设计文档

**Date**: 2026-04-30
**Status**: Draft for review
**Scope**: 阶段一 — 单机 + 质感重塑（包含视觉系统、皮肤、复盘、音效、段位、表情、GIF 分享、皮肤店、签到/金币）
**Out of scope**: 阶段二 — 联机房间制、观战、服务端真天梯（另起 spec）

---

## 1. 目标与定位

### 1.1 产品定位

桌面五子棋，**单机休闲为主 + 局域网联机为辅**。目标用户：偏成人玩家。视觉调性：**温暖怀旧（木纹/棕色/衬线/复古）**。

### 1.2 商业模型

**占位式变现**：完整保留商店 / 皮肤 / 会员 / 付费按钮 UI 流程，但点击付费跳"敬请期待"占位 + 0 元试用解锁。

未来若接真支付（Steam / 微软商店 / 自建小后端），只需替换购买 handler，前端流程不动。

### 1.3 成功标准

- **视觉**：界面看上去与"商店付费桌面游戏"水准持平，不再像"内部工具"
- **功能**：7 个新模块（皮肤/复盘/音效/段位/表情/GIF/商店）齐备
- **架构**：client 包重组后，新增功能可在不改既有代码的情况下接入
- **回归**：现有 36 个 E2E 协议层断言保持 100% 通过

---

## 2. 高层架构

### 2.1 实施路径

**Approach 3：全重写客户端 UI 层**（已确认）。保留所有非 UI 代码：
- `protocol/`、`game/`、`user/`、`server/`、`profile/`、`config/`、`tool/`
- `ai/Evaluate.java`（AI 算法纯逻辑）
- `Communication`（重新归属到 `net/`）

整个旧 `client/` 包推倒重写 + `ai/AiBoard,AiChess,AiGame` 用新组件库重建。

### 2.2 包结构

```
src/main/java/
├── core/                  保留不动（仅 model 包做轻量重组）
│   ├── protocol/          Message, Net, SafeObjectInputStream, Command
│   ├── model/             Profile（从 profile/ 移过来；Game/Manual 保留在 game/）
│   ├── config/            AppConfig
│   ├── server/            FiveServer（不动）
│   ├── game/              Game, Manual, GameDao, ManualDao（保持包名以减少改动面）
│   ├── user/              User, UserDao, PasswordHash, DBConnection
│   └── ai/                Evaluate（纯算法，AiBoard/AiChess/AiGame 已被 GameScene 吸收）
│
├── ui/                    全新设计系统
│   ├── design/            DesignTokens, Skin, BoardStyle, PieceStyle, Fonts
│   ├── component/         GButton, GCard, GAvatar, GBoard, GChip, GTab, GBadge,
│   │                      GToast, GDialog, GSlider, GTextField, GAvatarUpload
│   └── scene/             Scene 接口, Router, HomeScene, LobbyScene,
│                          GameScene, ReviewScene, ShopScene, SettingsScene
│
├── feature/               7 个独立功能模块
│   ├── skin/              SkinManager, SkinPack, ClassicSkin, DarkWoodSkin,
│   │                      JadeSkin (占位), NeonSkin (占位)
│   ├── sound/             SoundManager, SoundId
│   ├── review/            ReviewEngine, ReviewReport, ReviewMove
│   ├── rank/              RankCalculator, Rank, Tier
│   ├── emote/             EmotePack, Emote, EmoteParser
│   ├── share/             GifExporter, ExportOpts
│   └── shop/              ShopScene 内部组件 + PurchaseDialog
│
├── net/                    会话层
│   ├── Communication.java  纯网络层（无 UI 依赖）
│   ├── GameSession.java    对局会话抽象接口
│   ├── OnlineGameSession.java  走 Communication 的实现
│   └── AiGameSession.java      走本地 Evaluate 的实现
└── app/Main.java           启动入口
```

> 注：`ai/AiBoard,AiChess,AiGame` 在阶段一被 `ui/scene/GameScene` 统一吸收。
> GameScene 通过 `GameSession` 抽象支撑两种模式：
> - `OnlineGameSession`：底层走 `net/Communication` 收发协议帧
> - `AiGameSession`：本地维护对局状态，AI 由 `core/ai/Evaluate` 异步算
>
> GameScene 只调用 `session.makeMove()` / `session.giveup()` / `session.subscribe(callback)`，
> 不区分网络 vs 单机。

### 2.3 设计系统三要素

#### DesignTokens（设计变量）

集中定义所有颜色、间距、字号、圆角。组件不再硬编码任何视觉值：

```java
class Tokens {
  // 颜色
  Color bgPrimary, bgSecondary, bgCard, bgBoard, bgToolbar;
  Color fgDefault, fgMuted, fgOnAccent;
  Color accent, accentDark, danger, success, warning;
  Color boardLine, boardStar, boardShadow;
  Color borderLight, borderMedium;

  // 间距（8pt grid）
  int space1=4, space2=8, space3=12, space4=16, space5=24, space6=32, space7=48;

  // 字号
  Font fontDisplay;   // 28-34px Bold（标题）
  Font fontTitle;     // 18-22px Bold
  Font fontHeading;   // 14-16px Bold
  Font fontBody;      // 13px Regular
  Font fontCaption;   // 11px Regular
  Font fontMono;      // 13px 等宽

  // 圆角
  int radiusSm=4, radiusMd=8, radiusLg=12, radiusFull=999;

  // 阴影
  Shadow shadowSm, shadowMd, shadowLg;
}
```

#### Skin（皮肤包）

把"温暖怀旧"做成第一张皮肤；皮肤是 immutable 数据对象：

```java
class Skin {
  String id;          // "classic" / "darkwood" / "jade" / "neon"
  String displayName; // "经典木纹" / "深色木纹" / "玉石" / "霓虹"
  boolean premium;    // 是否需付费占位
  Tokens tokens;      // 该皮肤所有设计变量
  BoardStyle board;   // 棋盘绘制规则（背景渐变 / 木纹 / 星位样式）
  PieceStyle pieces;  // 棋子绘制规则（黑白渐变参数 / 阴影）
}
```

#### Components（G* 组件库）

11 个核心组件，所有视图必须由它们拼。组件读取 `SkinManager.current().tokens` 自动响应皮肤切换：

| 组件 | 职责 |
|---|---|
| `GButton` | 按钮，三种 variant：primary / secondary / ghost；带 tooltip + 快捷键提示 |
| `GCard` | 卡片容器，圆角 + 边框 + 微阴影 |
| `GAvatar` | 圆形头像（渐变球或自定义图） |
| `GBoard` | 棋盘组件（皮肤化绘制 + 悬停预览 + 落子动画 + 呼吸光环 + 鼠标坐标转换） |
| `GChip` | 标签 / 段位徽章 / 表情气泡 |
| `GTab` | 选项卡切换（用于 ShopScene 皮肤分类） |
| `GBadge` | 数字角标（未读消息、新内容） |
| `GToast` | 短暂浮动提示，3 秒淡出（替代 JOptionPane 系统消息） |
| `GDialog` | 圆角现代对话框基类 |
| `GSlider` | 滑块（用于 ReviewScene 进度、设置音量） |
| `GTextField` | 输入框，placeholder + 圆角 |

### 2.4 Scene / Router 视图层

6 个 Scene，由 Router 切换，互不直接耦合：

| Scene | 何时显示 | 内容 |
|---|---|---|
| **HomeScene** | 启动后未登录 | 大 LOGO + 联机/单机两大入口 + 玩家档案卡 + 入口（皮肤店、本地棋谱、设置） |
| **LobbyScene** | 联机登录后 | 我的卡片 + 玩家列表 + 聊天 + 邀请按钮 |
| **GameScene** | 对局中（联机 OR 人机） | 棋盘最大 + 双方信息卡 + 表情按钮 + 认输/聊天 |
| **ReviewScene** | 对局结束 | 棋盘回放 + AI 复盘评分曲线 + 妙手/失误标记 + GIF 导出 |
| **ShopScene** | 皮肤店 | 皮肤分类 tab + 卡片网格 + "购买"占位 + 金币显示 + 签到 |
| **SettingsScene** | 设置 | 主题切换、音量、字体、AI 难度默认值 |

`Router` 维护 Scene 历史栈：

```java
class Router {
  void push(Scene s);    // 进新场景
  void replace(Scene s); // 替换当前
  void pop();            // 返回
  Scene current();
}
```

### 2.5 事件总线

```java
class EventBus {
  static void publish(Event e);
  static <T extends Event> void subscribe(Class<T> type, Consumer<T> handler);
  static void unsubscribe(Class<?> type, Consumer<?> handler);
}

// 事件类型
SkinChangedEvent(Skin newSkin)
GameStartEvent(GameMode mode, String myColor)
GameEndEvent(boolean youWon, Manual manual)
MoveEvent(int col, int row, boolean self)
RankUpEvent(Tier from, Tier to)
CoinsChangedEvent(int newBalance, int delta, String reason)
```

视图与 feature 模块通过事件解耦——加新 feature 不改 Scene 代码。

---

## 3. 7 个 Feature 模块详细设计

### 3.1 `feature/skin` 皮肤系统

**核心类**：

```java
class SkinManager {
  static Skin current();
  static void setCurrent(String skinId);   // publish SkinChangedEvent
  static boolean isUnlocked(String skinId);
  static void unlock(String skinId);       // 写到 ~/.gomoku/skins.txt
  static List<Skin> all();
}
```

**内置 4 套**：
- `ClassicSkin`：默认免费 · B 温暖怀旧（**精修**，唯一发布质量到位的皮肤）
- `DarkWoodSkin`：占位"敬请期待"
- `JadeSkin`：占位"敬请期待"
- `NeonSkin`：占位"敬请期待"

> 决策：阶段一只精修 Classic 一套，其他 3 套作为商店"敬请期待"卡片陈列，避免质量参差。

**ShopScene 流程**：
1. 列出全部皮肤卡片（带预览图）
2. 点付费皮肤 → `PurchaseDialog`
3. PurchaseDialog 显示价格 + 两个按钮：[敬请期待] [用 0 金币试用解锁]
4. 试用解锁调 `SkinManager.unlock(id)` + `SkinManager.setCurrent(id)`

**与组件库联动**：所有 `G*` 组件构造时缓存 `SkinManager.current()`，订阅 `SkinChangedEvent` 后调 `revalidate() + repaint()`。

---

### 3.2 `feature/review` AI 复盘

**核心类**：

```java
class ReviewEngine {
  ReviewReport analyze(Manual m, int aiDepth, ProgressCallback cb);
}

class ReviewReport {
  ReviewMove[] moves;
  int blackMistakes, whiteMistakes;
  int blackBrilliant, whiteBrilliant;
  int totalMs;  // 复盘总耗时
}

class ReviewMove {
  int col, row;
  int evaluationBefore;     // 该手前局面分（己方视角）
  int evaluationAfter;      // 该手后局面分
  int delta;                // 评分变化
  Tag tag;                  // BRILLIANT / GOOD / OK / INACCURATE / MISTAKE / BLUNDER
  int[] suggestedMove;      // AI 推荐更优落点（深度=aiDepth）
}
```

**算法**：
- 复盘时对每一手，把局面回退到该手前 → 跑 `Evaluate.getTheBestPosition()` 得到 AI 推荐位 + 评分
- 比较实际落子与 AI 推荐位 → 计算 delta（己方视角）

**Tag 阈值**（基于实际手与 AI 推荐手的评分差）：
- `BRILLIANT`: 实际手 = AI 推荐手 且该手将局面分提升 > 5000
- `GOOD`: 实际手 = AI 推荐手
- `OK`: 评分差 <= 300
- `INACCURATE`: 300 < 评分差 <= 1000
- `MISTAKE`: 1000 < 评分差 <= 5000
- `BLUNDER`: 评分差 > 5000

**性能优化**：每手只跑一次 `getTheBestPosition`（深度由 `aiDepth` 控制，复盘默认 = 标准难度 = 3）。
不需要"全位置评估"，避免 O(n²) 爆炸。

**ReviewScene 布局**：
- 左：GBoard 复用 + 拖动 GSlider 跳到任意手
- 右上：评分曲线（折线图，黑红双线，X 轴=手数，Y 轴=分数）
- 右下：手列表 → 点击跳转 + Tag 颜色 + AI 建议
- 工具栏：[导出 GIF] [返回大厅]

**性能**：15×15、30-40 手对局，深度 3 跑一遍约 5-15 秒。`SwingWorker` + 进度条。

**变现钩子**：免费用户只看摘要（"3 个失误，1 个妙手"+ 棋盘）；付费占位才能看每手的详细评分曲线和 AI 建议。免费用户在右侧看到模糊覆盖层 + "解锁详细复盘"按钮 → 跳 ShopScene。

---

### 3.3 `feature/sound` 音效系统

**核心类**：

```java
class SoundManager {
  static void preload();
  static void play(SoundId id);
  static void setEnabled(boolean);
  static void setVolume(int 0..100);
}

enum SoundId {
  PLACE,    // 落子
  CAPTURE,  // 留作未来
  WIN,      // 获胜
  LOSE,     // 失败
  INVITE,   // 收到邀请
  EMOTE,    // 表情发送/收到
  CLICK,    // 主操作按钮（轻微）
}
```

**实现**：`javax.sound.sampled.Clip` 池化（每个 SoundId 预加载一个 Clip，play 时 setFramePosition(0) + start()）。

**资源**：6 个短音效（每个 < 50KB），用免费 CC0 来源（freesound.org / opengameart.org）。打到 jar 内 `/assets/sounds/*.wav`。

**触发点**（订阅 EventBus）：
- `MoveEvent` → `PLACE`
- `GameEndEvent.youWon` → `WIN`，否则 `LOSE`
- 收到 `JOIN` 邀请 → `INVITE`
- 表情发送/收到 → `EMOTE`
- 主 GButton 点击 → `CLICK`

**配置**：`AppConfig.uiSound()` 控制开关；`SettingsScene` 提供音量滑块。

---

### 3.4 `feature/rank` 段位系统

**核心类**：

```java
class RankCalculator {
  // 联机：标准 ELO 公式，K=32
  // 人机：AI 难度作为对手 ELO（简单=800 / 标准=1200 / 困难=1600）
  //       + K 系数减半（K=16）— 人机分量打折，避免刷分上分
  static int calcDelta(int myElo, int oppElo, boolean won, GameType type);
  static Tier tierOf(int elo);
}

enum GameType { ONLINE, AI_EASY, AI_NORMAL, AI_HARD }

class Rank {
  int elo;
  Tier tier;
  int divisionPoints;  // 段内进度 0-100
  int peakElo;         // 历史最高
}

enum Tier {
  BRONZE("青铜",   0,    1000),
  SILVER("白银",   1000, 1300),
  GOLD("黄金",     1300, 1600),
  PLATINUM("铂金", 1600, 1900),
  DIAMOND("钻石",  1900, 2200),
  MASTER("大师",   2200, INT_MAX);
}
```

**Profile 扩展字段**：`elo`、`peakElo`、`weeklyWins`、`weeklyLosses`、`weekStartMillis`（每周一自动重置）。

**视图**：
- HomeScene 我的卡片：徽章（GChip）+ 段位 + 当前 ELO
- LobbyScene 玩家列表每行显示对手段位徽章（来自本地缓存或服务端 add 命令未来扩展）
- SettingsScene 显示本机历史段位曲线（折线，本周对局走势）+ 本周战绩
- 段位提升：`RankUpEvent` → `GToast` "升至白银！"+ 音效

**联机 vs 单机区分**：服务端 `game` 表本身不存 ELO（阶段一不改服务端协议）。ELO 全本地计算 + 写 profile。意味着"联机排行榜"实际是"我的联机对局战绩"，**不是跨用户真天梯**——这是诚实的妥协。

> 阶段二：改服务端协议，加 ELO 字段到 `add` / `change` 命令，服务端维护跨用户天梯，这才是真排行榜。

---

### 3.5 `feature/emote` 聊天表情/快捷短语

**核心类**：

```java
class EmotePack {
  static List<Emote> DEFAULT = Arrays.asList(
    new Emote("good",    "妙手！"),
    new Emote("nice",    "好棋"),
    new Emote("oops",    "失误了"),
    new Emote("again",   "再来一局？"),
    new Emote("gg",      "认输，承让"),
    new Emote("hi",      "你好"),
    new Emote("waiting", "等等..."),
    new Emote("thanks",  "感谢")
  );
}

class EmoteParser {
  // 检测 [emote:xxx] 前缀
  static Optional<Emote> parse(String text);
}
```

**协议复用**：用现有 `TALK` 命令发送，前缀 `[emote:good]妙手！`。**不需要服务端协议改动**。

**视图**：GameScene 右下角"表情"GButton，点击弹出 8 个快捷短语网格（GCard 包），选中即发出。聊天显示侧 `EmoteParser` 识别前缀 → 渲染为 GChip 而非普通文字气泡。

---

### 3.6 `feature/share` 棋谱 GIF 分享

**核心类**：

```java
class GifExporter {
  File export(Manual m, ExportOpts opts, ProgressCallback cb);
}

class ExportOpts {
  String skinId;       // 用哪套皮肤渲染
  int frameDelayMs;    // 每帧延时（默认 800）
  int scale;           // 像素 scale（默认 600x600）
  boolean includeMoveLabels; // 是否在棋子上画手数
}
```

**实现**：
1. `GBoard` 渲染到 `BufferedImage` 数组（每手一帧）
2. 用 [animated-gif-lib-for-java](https://github.com/rtyley/animated-gif-lib) 编码（30KB，Apache 2.0，纯 Java）
3. 输出到 `~/.gomoku/exports/<black>_vs_<white>_<时间>.gif`
4. 完成后弹 `GToast` "已导出 → 打开文件夹"

**触发**：ReviewScene 工具栏"导出 GIF"按钮 / DialogLocalGames 表格右键菜单"导出 GIF"。

**风险缓解**：限制最大帧数 100（即使是超长对局也不溢出）。

---

### 3.7 `feature/shop` 皮肤店 + 金币 + 签到

**金币系统**（纯本地 int，写 profile）：
- 每日签到：连续天数算法（lastCheckin = 昨天 → 连续天数 +1，否则重置 1）
- 签到奖励：每日 +10 金币 / 连续 7 天 +50 / 连续 14 天 +100 / 连续 30 天 弹"皮肤碎片"占位 GToast
- 联机胜利 +10 金币 / AI 胜利 +5 金币（区分"含金量"，鼓励联机）
- 失败不扣分（避免负反馈）

**示意价格表**（皮肤）：
- ClassicSkin：免费（默认）
- DarkWoodSkin：800 金币（约 80 局）
- JadeSkin：1500 金币（高级）
- NeonSkin：2500 金币（限定）

让用户能"努力一下就买得起"DarkWood，"短期内攒不起"Jade/Neon — 制造稀缺感。

**ShopScene 内容**：
- 顶部金币条：当前金币 + 签到状态（"今日已签到，连续 5 天"）
- 主体：3 个 tab（GTab）"皮肤"、"道具"、"会员"
- 皮肤 tab：4 张皮肤大卡（GCard，含预览图 + 名字 + 价格 + [购买] / [已拥有] / [使用中] 按钮）
- 道具 tab：占位（"复盘加速券" / "悔棋券" 等"敬请期待"）
- 会员 tab：占位

**PurchaseDialog**：
- 显示皮肤大图 + 描述
- 价格 + [使用金币购买] [跳过支付直接试用]
- 试用解锁 → SkinManager.unlock + SkinManager.setCurrent + EventBus.publish(CoinsChangedEvent)

---

### 3.8 模块依赖

```
ui/scene ──┬──> ui/component (G*) ──> ui/design (Tokens)
           │                              ↑
           └──> EventBus                  │
                  ↑                       │
feature/skin ─────┴──── SkinChangedEvent
                                          │
feature/sound ←── EventBus 订阅           │
feature/review ── core/ai/Evaluate        │
feature/rank ──── core/model/Profile      │
feature/emote ─── core/protocol (TALK 复用)│
feature/share ─── ui/component/GBoard ────┘
feature/shop ──── feature/skin + Profile
```

---

## 4. 关键数据流

### A. 启动流
```
Main.main → UiTheme.install
         → Profile.get()
         → SkinManager.init(从 profile 读 skinId)
         → SoundManager.preload()
         → Router.push(HomeScene)
         → if (!profile.onboardingDone) showOnboarding()
         → 检查每日签到 → 自动签到 + GToast
```

### B. 切换皮肤
```
ShopScene 选皮肤 → SkinManager.setCurrent(id)
                → publish SkinChangedEvent
                → 所有 G* 组件读新 tokens 重绘
                → Profile.skinId = id; Profile.save()
```

### C. 一手棋（联机）
```
GBoard 落子 → publish MoveEvent
           → SoundManager.play(PLACE)
           → Communication.sendGo(col, row)
           → 等对方 GO → GBoard.addOpponentChess
           → 胜负判定 → Communication.sendWin / 对方 GIVEUP
           → 收到 TELLRESULT → publish GameEndEvent
```

### D. 对局结束 → 复盘
```
GameEndEvent → SoundManager.play(WIN/LOSE)
            → Profile.update(elo, weeklyWins, ...)
            → RankCalculator 计算 delta
            → if (升段) publish RankUpEvent → GToast 动画
            → ManualDao.addManual（联机棋谱本地副本）
            → Router.replace(ReviewScene(manual))
                → ReviewEngine.analyze 后台跑 (SwingWorker)
                → 进度条 → 完成 → 渲染评分曲线 + 手列表
                → "返回大厅" → Router.replace(LobbyScene)
```

### E. 表情发送
```
GameScene 表情按钮 → 选 emote → Communication.sendTalk("[emote:good]妙手！")
                  → 收到 TALK 时 EmoteParser 识别前缀
                  → 是 emote → ChatBubble.renderAsChip
                  → 否则 → ChatBubble.renderAsText
                  → SoundManager.play(EMOTE)
```

### F. GIF 导出
```
ReviewScene 导出按钮 → 选当前皮肤 + 帧速度 → SwingWorker.execute
                    → GifExporter.export → 每手一帧 BufferedImage
                    → AnimatedGifEncoder 编码 → 写文件
                    → done → GToast "已导出 → 打开文件夹"
```

### G. 金币与签到
```
启动 → 检查 Profile.lastCheckinDate != 今天
    → 自动签到 → Profile.coins += 10
    → 连续天数算法（lastCheckin = 昨天 → +1，否则重置 1）
    → 7/14/30 天弹"奖励皮肤碎片"占位 GToast
皮肤购买 → 校验 coins >= price → 扣除 → SkinManager.unlock
       → publish CoinsChangedEvent
```

---

## 5. 错误处理

**统一原则**：catch 处不再用 `JOptionPane.showMessageDialog`（阻塞），改用 `GToast`（3 秒淡出）。Modal 只用于强确认（认输、清空棋谱、购买）。

| 场景 | 处理 | 用户感知 |
|---|---|---|
| 皮肤资源加载失败 | 回退 ClassicSkin | GToast "皮肤损坏，已恢复默认" |
| 音效文件缺失 | 静默跳过该次 play | 无（不阻塞游戏） |
| 复盘超时（>30s） | 取消 + 显示部分结果 | "复盘超时，可手动重试" |
| GIF 导出失败（磁盘满 / 写权限） | catch + GToast | "导出失败：<原因>" |
| 服务器断开 | StatusBar 红 + Router.replace(HomeScene) | 已实现，保持 |
| 旧棋谱坐标越界 | 跳过非法手 | ReviewScene 角标 ⚠ "已跳过 N 步" |
| 协议反序列化白名单拒绝 | 已实现 SafeObjectInputStream | 服务端日志 |
| Profile 文件损坏 | catch IOException → 用空档案启动 + 备份原文件 | GToast "档案已重置" |

---

## 6. 测试策略

### 6.1 保留现有 36 个 E2E 断言
- `E2eTest`（28）/ `E2eTest2`（8）继续保护协议层
- 阶段一不改服务端协议，旧测试 100% 兼容

### 6.2 新增单元测试
- `SkinManagerTest`：皮肤切换 + persist + EventBus 通知
- `ReviewEngineTest`：构造已知棋局 → 验证标记的妙手/失误正确
- `RankCalculatorTest`：ELO 公式 + 段位边界
- `EmoteParserTest`：识别 [emote:xxx] 前缀
- `GifExporterTest`：小棋谱 → 检查文件存在 + GIF 头
- `ProfileTest`：save/load 往返 + 字段升级兼容（旧档案缺新字段时取默认值）

### 6.3 手工冒烟脚本
写在 `docs/testing-checklist.md`：
- 联机：登录 → 邀请 → 对局 → 复盘 → 导出 GIF
- 单机：开新局 → 不同难度 → 战绩卡 → 段位提升
- 皮肤：4 套切换 → 每套都能下棋
- 签到 → 商店 → 占位购买 → 解锁
- 主题切换、字体回退（无中文字体环境）

### 6.4 回归门禁
- 每个 feature 模块完成必须跑 36 E2E + 该模块单元测试
- 提交前必须 `bash scripts/build.sh` + `scripts/run-tests.sh` 全绿

---

## 7. 实施时间预算

| 步 | 内容 | 预估会话量 |
|---|---|---|
| 1 | 包重组 + design system + 组件库 (G*) + Router/Scene 框架 | 2 |
| 2 | 4 个内置皮肤（Classic 精修，其他占位）+ SkinManager + ShopScene 占位 | 1 |
| 3 | 重写 5 个 Scene（Home/Lobby/Game/Review/Shop）+ Settings | 2 |
| 4 | feature: review + sound + rank | 1 |
| 5 | feature: emote + share(GIF) + 金币签到 | 1 |
| 6 | 文案 / 音效素材 / 默认皮肤打磨 / 错误处理收口 | 1 |
| 7 | 测试 / 重打包 / 文档 | 1 |
| | **总计** | **9** |

每个会话约 30-45 分钟有效工作 + 编译/测试/打包。

---

## 8. 风险登记

| 风险 | 严重度 | 缓解 |
|---|---|---|
| 皮肤切换导致 jar 内 4 张棋盘纹理图增加 1-2 MB | 低 | 用 Java2D 程序化绘制木纹，零图片资源 |
| 复盘耗时让用户等急了 | 中 | 进度条 + 后台跑 + 可中途看摘要 |
| GIF 导出在大对局（>50 手）内存溢出 | 中 | 流式编码 + 限制最大帧数 100 |
| 4 张皮肤设计审美参差 | 已规避 | 阶段一只精修 Classic，其他 3 套作为占位 |
| jpackage 包变大（animated-gif-lib + 音效）| 低 | 当前 ~50MB，加完仍 < 60MB |
| 重写期间 36 E2E 被打破 | 中 | 每个步骤完成立即跑全套测试，不留累计回归 |
| 用户在阶段一中途希望改方向 | 中 | spec 是基线；任何调整记录在 spec 修订日志 |

---

## 9. 阶段二预告（不在本 spec 范围）

阶段二将做：
- 联机房间制（创建/加入房间、密码、围观位）
- 观战模式（在大厅看别人对局）
- 服务端协议升级（加 `room_create` / `room_join` / `watch` / `elo_sync` 命令，向下兼容旧客户端）
- 真天梯（服务端维护跨用户 ELO 排行榜）
- 单独 spec：`2026-XX-XX-gomoku-online-redesign-stage2-design.md`

预估 4-5 会话。

---

## 10. 决策日志

| 决策 | 选项 | 拍板 | 理由 |
|---|---|---|---|
| 产品定位 | A 单机休闲 / B 局域网 / C 严肃训练 / D 作品集 | **A+B** | 单机为主联机为辅，目标成人玩家 |
| 视觉风格 | 简约现代 / 温暖怀旧 / 暗夜竞技 / 禅意东方 | **温暖怀旧** | 贴合棋类传统 + 成人受众 |
| 变现方式 | demo 占位 / 真支付 / Steam / 完全免费 | **demo 占位** | 工程量可控 + 看起来产品级 |
| 核心功能 | 10 候选 | **皮肤+复盘+音效+段位+表情+GIF+观战+房间制** | 阶段一/阶段二分别承接 |
| 实施粒度 | 一次到位 / 两期 / 三期 | **两期** | 每期完结都能用 |
| 实施路径 | 增量 / 重构 / 全重写 | **全重写 client UI** | 彻底产品化 + 无历史包袱 |
| GIF 依赖 | animated-gif-lib | **接受** | 30KB Apache 2.0，性价比最高 |
| 段位实现 | 本地 ELO / 服务端真天梯 | **阶段一本地，阶段二服务端** | 不改服务端协议 |
| 金币签到 | 要 / 不要 | **要** | 让商店"看起来真"的关键 |
| 皮肤打磨 | 4 套均精修 / 只精修 Classic | **只精修 Classic** | 避免质量参差 |
| 错误展示 | JOptionPane / GToast | **GToast** | 不阻塞 + 产品感强 |
