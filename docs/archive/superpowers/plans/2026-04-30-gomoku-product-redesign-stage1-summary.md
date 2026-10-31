# Gomoku 产品重塑 阶段一 总结（2026-04-30）

## 背景

旧版 Gomoku 是工程练习导向的 C/S 五子棋 demo：协议/AI/服务端工程上扎实，但客户端 UI 是裸 AWT 风格、无产品包装，用户反馈集中为两条 ——

> **"UI 不及格"** + **"未能变现"**。

通过 brainstorming → spec → plan → 执行的标准 superpower 流程，本次会话锁定了"全重写客户端 UI、加 7 大产品功能、保持服务端协议向下兼容、阶段一不动天梯/房间/观战"的范围，分 9 个 phase 完成。

- spec：`docs/superpowers/specs/2026-04-30-gomoku-product-redesign-stage1-design.md`
- plan：`docs/superpowers/plans/2026-04-30-gomoku-product-redesign-stage1.md`

## 实施过程

总共 **10 个 phase**（Phase 0 占位准备 → Phase 9 收尾），合计 **64 个 phase commit**（不含 spec / plan / 设计自查的 4 个文档 commit）。每 phase 独立可验证，每 task 独立 commit。

| Phase | 主题 | tasks | commits |
|---|---|---|---|
| 0 | 准备工作（占位目录 + GIF 依赖 + 单元测试脚本） | 3 | 2 |
| 1 | 数据层（Tokens / Fonts / BoardStyle / PieceStyle / Skin / Profile 扩展） | 6 | 7 |
| 2 | UI 组件库（GButton/GCard/GChip/GAvatar/GBadge/GToast/GDialog/GTextField/GSlider/GTab/GBoard + EventBus + Scene/Router + Theme） | 13 | 13 |
| 3 | 皮肤系统（4 套皮肤 + SkinManager + 测试） | 5 | 5 |
| 4 | 主框架场景（Main 入口 + HomeScene + 全局组件 + LobbyScene + ChatPanel + LoginDialog） | 6 | 6 |
| 5 | GameSession 抽象 + GameScene（OnlineGameSession + AiGameSession + PlayerCard，删除旧 client/ + ai 旧三件套） | 7 | 7 |
| 6 | 复盘 + 段位 + 音效（ReviewEngine + Sound + Rank + ReviewScene + ScoreChart） | 8 | 8 |
| 7 | 商店 + 金币 + 表情 + GIF（CoinManager + ShopScene + PurchaseDialog + Emote + GifExporter） | 9 | 9 |
| 8 | 体验补丁（GToast 统一 + SettingsScene + 音效素材 + AboutDialog + OnboardingDialog） | 4 | 4 |
| 9 | 收尾（全测验证 + 冒烟清单 + 重打包 + 文档） | 4 | 3 |

## 完成清单

阶段一 spec 列出的 7 大产品功能全部交付：

| Spec 模块 | 实现 | 验证 |
|---|---|---|
| 现代化 UI（4 套皮肤主题切换） | Theme/Tokens/SkinManager + Classic/DarkWood/Jade/Neon 4 套 | SkinManagerTest passed=5 |
| 联机 / 人机对战统一 | GameSession 抽象 + OnlineGameSession + AiGameSession | E2eTest passed=28 |
| AI 复盘 | ReviewEngine + ReviewReport + ReviewScene + ScoreChart | ReviewEngineTest passed=2 |
| 段位系统（青铜→大师 6 段） | RankCalculator + Tier + RankSubscriber | RankCalculatorTest passed=5 |
| 聊天表情（8 个快捷短语） | Emote + EmotePack + EmoteParser + EmoteBar + ChatPanel 渲染 | EmoteParserTest passed=4 |
| 棋谱 GIF 导出 | GifExporter + ReviewScene 集成 | GifExporterTest passed=1 |
| 商店 + 金币 + 每日签到 | CoinManager + CoinEarnSubscriber + ShopScene + PurchaseDialog + Profile.checkin | ProfileTest passed=12 |

外加基础设施：

- EventBus 6 事件类型（EventBusTest passed=3）
- Profile 持久化扩展字段（ELO / peakElo / coins / skinId / unlockedSkins / 签到日历）
- AboutDialog + OnboardingDialog（首次启动 3 步引导）
- 音效素材（6 个 WAV 程序化生成，落子 / 胜利 / 失败 / 升段 / 表情 / 金币）

## 测试覆盖

合计 **68 断言**，全过：

- 联机 E2E：`E2eTest` 28（注册/登录/邀请/对局/胜负/写库/历史回放） + `E2eTest2` 8（GIVEUP / 特殊字符 TALK / 重复对局）
- 单元测试：`ProfileTest` 12 + `EventBusTest` 3 + `SkinManagerTest` 5 + `ReviewEngineTest` 2 + `RankCalculatorTest` 5 + `EmoteParserTest` 4 + `GifExporterTest` 1 = 32

E2E 跑法：`bash scripts/run-tests.sh`（需服务器在线）；单测跑法：`bash scripts/run-unit-tests.sh`（独立）。

## 未完成 / 故意推迟

写实记录，避免误导：

- **Settings 部分控件是占位**：字体下拉、AI 难度按钮在 SettingsScene 暂未绑定 Profile 字段，UI 可见但保存无效（Profile schema 未加对应字段，下个 phase 再扩）。
- **段位仅本地 ELO**：服务端未维护跨用户排行榜；ELO 写在 `~/.gomoku/profile.properties`，换设备不同步。**真天梯延后到阶段二**（spec § 9）。
- **房间制 / 观战不做**：阶段二，需要服务端协议升级（spec 显式列出）。
- **音效素材为程序化生成**：6 个 WAV 由 Java 写正弦波 + 包络合成，能用但不动听。后续替换成 freesound CC0 真音效是单独一个 task。
- **付费皮肤是 demo 占位**：PurchaseDialog 有 `[跳过支付直接试用]` 按钮一键解锁；没接真支付（spec 决策日志明确：变现方式 = demo 占位）。
- **客户端 ReceiveThread 仍未严格切 EDT**：低负载下 OK，旧 ARCHITECTURE.md 的"已知约束"还成立。
- **断线重连仍未做**：服务器关掉 → 红色 StatusBar → 自动回 HomeScene；要重新建立会话还得手动重登。

## 阶段二预告

参考 spec § 9：

1. 联机房间制（创建 / 加入 / 密码 / 围观位）
2. 观战模式（在大厅看别人对局，订阅落子流）
3. 服务端协议升级（`room_create` / `room_join` / `watch` / `elo_sync` 命令，向下兼容）
4. 服务端真天梯（跨用户 ELO 排行榜，迁移 Profile.elo → server.user.elo）
5. 真支付接入或 Steam 上架决策（如果 demo 反馈正向）
6. 真实音效素材替换（freesound CC0）
7. SettingsScene 占位字段补齐（字体 / AI 难度落库到 Profile）
8. 客户端断线自动重连 + EDT 切换严格化

预估 4-5 会话。spec 文件名 `2026-XX-XX-gomoku-online-redesign-stage2-design.md`。

## 架构亮点

- **Theme + Tokens**：DesignTokens 模式驱动全部组件取色，皮肤切换 = 替换 token 表 + 广播 SkinChangedEvent。组件层零硬编码颜色，新增皮肤只需写一个 `SkinPack`。
- **EventBus**：6 种事件、CopyOnWriteArrayList 订阅表，跨模块解耦（金币模块听对局结束、复盘模块听落子流，互不直接依赖）。
- **Scene + Router**：CardLayout 历史栈 + onEnter/onLeave 生命周期，每屏自包含。Router 单例统一切换 / back / replace 语义，使 SettingsScene → ShopScene → HomeScene 这种深跳转也可正确返回。
- **GameSession 抽象**：联机和人机两种对局模式从此走同一个 GameScene。GameSession 接口只暴露落子 / 认输 / 聊天 / 复盘报告四个动作，OnlineGameSession 包 Communication，AiGameSession 包 Evaluate；UI 完全不感知差异。

## 产物清单

- `target/gomoku-server.jar` / `target/gomoku-client.jar`：fat jar，需要 JRE 17+
- `target/installer/Gomoku-Server/Gomoku-Server.exe` + `target/installer/Gomoku-Client/Gomoku-Client.exe`：自带 JRE 的 app-image（约 50MB / 个）
- `target/installer/Gomoku-{Server,Client}-1.0.0.zip`：分发用压缩包
- `docs/testing-checklist.md`：阶段一上线前手工冒烟清单
- 本文：阶段一交付总结
