# Gomoku 产品级重塑阶段一 · 会话进度备忘

**Last updated**: 2026-04-30（阶段一全部完成）
**Branch**: `redesign-stage1`（55 commit ahead of `origin/redesign-stage1`，**未推送**，等控制器决定）
**Spec**: [`docs/superpowers/specs/2026-04-30-gomoku-product-redesign-stage1-design.md`](specs/2026-04-30-gomoku-product-redesign-stage1-design.md)
**Plan**: [`docs/superpowers/plans/2026-04-30-gomoku-product-redesign-stage1.md`](plans/2026-04-30-gomoku-product-redesign-stage1.md)
**Summary**: [`docs/superpowers/plans/2026-04-30-gomoku-product-redesign-stage1-summary.md`](plans/2026-04-30-gomoku-product-redesign-stage1-summary.md)

---

## 当前进度

| Phase | 主题 | Tasks | Commits | 状态 |
|---|---|---|---|---|
| 0 | 准备：分支/依赖/测试脚本 | 3 | 3 | ✅ |
| 1 | 设计系统基础（Tokens/Fonts/Skin/Profile 扩展） | 6 | 6 | ✅ |
| 2 | 组件库 G* + Router/Scene/EventBus | 12 | 13 (含 review fix) | ✅ |
| 3 | SkinManager + 4 套皮肤 | 5 | 5 | ✅ |
| 4 | HomeScene + LobbyScene + 重写既有视图 | 6 | 6 | ✅ |
| 5 | GameSession + GameScene + 重做对局 | 7 | 7 | ✅ |
| 6 | ReviewScene + Sound + Rank | 8 | 8 | ✅ |
| 7 | ShopScene + 金币/签到 + Emote + GIF | 9 | 9 | ✅ |
| 8 | 错误收口 + Settings + 文案 + 音效 | 4 | 4 | ✅ |
| 9 | 测试 + 重打包 + 文档 | 4 | 3 (9.1 无回归跳过) | ✅ |
| **总计** | | **64** | **64** | **✅ 阶段一完成** |

---

## 测试基线（最终）

| 测试套件 | 断言数 | 状态 |
|---|---|---|
| `src/test/java/E2eTest.java`（联机协议主流程） | 28 | ✅ |
| `src/test/java/E2eTest2.java`（GIVEUP / TALK 边界） | 8 | ✅ |
| `src/test/java/profile/ProfileTest.java` | 12 | ✅ |
| `src/test/java/ui/EventBusTest.java` | 3 | ✅ |
| `src/test/java/feature/skin/SkinManagerTest.java` | 5 | ✅ |
| `src/test/java/feature/review/ReviewEngineTest.java` | 2 | ✅ |
| `src/test/java/feature/rank/RankCalculatorTest.java` | 5 | ✅ |
| `src/test/java/feature/emote/EmoteParserTest.java` | 4 | ✅ |
| `src/test/java/feature/share/GifExporterTest.java` | 1 | ✅ |
| **总计** | **68** | **✅** |

---

## 交付清单

### 7 大产品功能（spec 章节 3）
- ✅ 皮肤系统（4 套：Classic 精修 + DarkWood/Jade/Neon 占位）
- ✅ AI 复盘（ReviewEngine 6 等级 + ScoreChart 评分曲线）
- ✅ 音效系统（6 SoundId 程序化生成 wav，可后期换 freesound CC0）
- ✅ 段位系统（青铜→大师 6 段 + ELO K=32/16）
- ✅ 聊天表情（8 个快捷短语 + EmoteParser）
- ✅ 棋谱 GIF 分享（GifExporter，写入 `~/.gomoku/exports/`）
- ✅ 商店 + 金币 + 签到（CoinManager + ShopScene + PurchaseDialog）

### 架构基础设施
- ✅ Theme + DesignTokens（无硬编码颜色）
- ✅ EventBus（6 种事件，EDT dispatch）
- ✅ Scene + Router（CardLayout 历史栈）
- ✅ GameSession 抽象（OnlineGameSession + AiGameSession）
- ✅ Communication 解耦（迁到 net 包，引入 GameClient 接口）
- ✅ Profile 扩展 13 字段（elo/peakElo/skinId/coins/checkin...）

### 旧代码清理
- ✅ `client/` 目录全删（10+ 文件）
- ✅ `ai/AiBoard, AiChess, AiGame` 删
- ✅ 保留 `ai/Evaluate.java`（AiGameSession 复用）

### 打包
- ✅ `scripts/package.sh` 入口切换 `client.FiveClient` → `app.Main`
- ✅ FlatLaf + animated-gif-lib 打入 fat jar
- ✅ `target/installer/Gomoku-{Client,Server}.exe` + `.zip` 产物生成

### 文档
- ✅ `README.md` 更新（产品功能 + 启动命令 + 依赖）
- ✅ `docs/ARCHITECTURE.md` 新增"阶段一重塑后架构"章节
- ✅ `docs/testing-checklist.md` 手工冒烟清单
- ✅ `docs/superpowers/plans/.../summary.md` 阶段一总结

---

## 已知未完成（故意推迟）

- **字体下拉 / AI 难度按钮**在 SettingsScene 是 UI 占位（Profile 缺字段，切换仅 GToast 提示）
- **服务端真天梯**（阶段二，本地 ELO 已实现）
- **房间制 / 观战**（阶段二）
- **真实音效素材**当前是程序化正弦波（可用但不动听，建议后续换 freesound CC0）
- **GameScene → ReviewScene 自动跳转**目前是 router.pop()（结果对话框按返回）

---

## 关键 commit 链（54 commits 概览）

```
67cd4d7  9.4: 文档更新（README + ARCHITECTURE + summary）
2b058d5  9.3: 重打包脚本切换入口为 app.Main + 验证 .exe 生成
8d98e27  9.2: 手工冒烟测试清单
ed5b044  8.4: AboutDialog + OnboardingDialog + 文案润色
352c564  8.3: 音效素材落地（程序化生成 6 个 WAV）
33ba10c  8.2: SettingsScene 设置面板
ac83bd1  8.1: 非阻塞提示统一为 GToast（无需替换）
a160cc1  7.9: GifExporterTest + ReviewScene 集成导出 GIF
2302bbe  7.8: GifExporter 棋谱 GIF 导出
7b61012  7.7: EmoteBar + 集成 GameScene/ChatPanel/GameSession.sendChat
de681ef  7.6: EmoteParserTest（4 用例）
aab2719  7.5: Emote + EmotePack + EmoteParser
a385476  7.4: PurchaseDialog 购买对话框
e17bb94  7.3: ShopScene 商店视图（4 皮肤 + GTab）
e76e708  7.2: CoinEarnSubscriber 胜利赚金币 + 启动签到
a5ac934  7.1: CoinManager 金币 + 签到
418d62d  6.7: ReviewScene 复盘视图
322b996  6.8: ScoreChart 评分曲线组件
e61d072  6.6: RankSubscriber
afe3a3c  6.5: RankCalculatorTest（5 用例）
c4637fc  6.4: RankCalculator + Tier + Rank
b9fc9c0  6.3: SoundManager + SoundSubscriber
a93cab7  6.2: ReviewEngineTest（2 用例）
aa36c38  6.1: ReviewEngine + ReviewMove/Report + Evaluate.snapshotScore
b767298  5.7: 删除旧 client UI + 旧 AI 类
d5b283d  5.5: GameScene 对局视图
a58ea26  5.6: PlayerCard
bd636f1  5.4: AiGameSession
13e567e  5.3: OnlineGameSession
8b9ac30  5.2: Communication 迁移 net + GameClient 接口
e33bc31  5.1: GameSession 抽象
6d2e235  4.6: LoginDialog 用 GDialog 重写
8b84747  4.5: ChatPanel 彩色聊天面板
65c6bbe  4.4: LobbyScene 大厅
0d6a1a5  4.3: BrandHeader + StatusBar
ad7f6f9  4.2: HomeScene 欢迎屏 + Main 接入
719ab16  4.1: app/Main.java 启动入口
b26faac  3.5: SkinManagerTest（5 用例）
d260755  3.4: SkinManager 切换 + 持久化
3a896da  3.3: SkinPack 注册表
0f44dba  3.2: 3 套占位皮肤（DarkWood/Jade/Neon）
2fc39fb  3.1: ClassicSkin 经典木纹皮肤（精修）
4f4f387  2.13: code review 修复（GDialog/GBoard/GTextField）
2765ba4  2.12: GBoard 棋盘组件
0433899  2.11: GTextField/GSlider/GTab/GBadge
2d6c958  2.10: GDialog
bd7e189  2.9: GToast
8a2fa23  2.8: GChip
800431a  2.7: GAvatar
8ef9a2c  2.6: GCard
4b9ed0d  2.5: GButton
d82535c  2.4: Theme + ThemeAware
58935b8  2.3: Scene + Router
c8b46c6  2.2: EventBusTest（3 用例）
2868cca  2.1: EventBus + 6 事件
58ec4c4  session: 阶段一进度备忘 + 下次会话起步指引
```

---

## 工作约定（保留供阶段二参考）

- 分支：`redesign-stage1`（never 改 master/packaging）
- commit 格式：`<phase>.<task>: <subject>`
- 每 task 单独 commit
- 测试门禁：68 断言全过
- shell：bash + iconv -f gbk -t utf-8 -c
- classpath：`;` Windows / `/` 路径
- 杀残留 java：`taskkill //F //IM java.exe` 在 E2E 前
- 不要 push 除非用户明确

---

## 已知陷阱（仍有效）

1. `git reset --hard` 被沙箱拦截 — 用 merge --ff-only / checkout
2. bash heredoc ENAMETOOLONG — 用 Write 工具写文件后 cat
3. `.bat` 必须 CRLF + 纯 ASCII REM
4. JOptionPane 中文 jpackage 上偶尔乱码 — 已统一改 GToast（保留 showConfirmDialog 用于强确认）
5. server 残留连接污染 ADD 广播 — 跑 E2E 前杀全部
6. jpackage cfg 默认 ANSI codepage — 不要加 `-Dfile.encoding=UTF-8`

---

## 下一步建议

1. **验证 + 推送**：手工跑 `app.Main` 一遍 → 验证关键流程 → `git push origin redesign-stage1`
2. **冒烟测试**：跑 `docs/testing-checklist.md` 中的清单
3. **合并到 master**（可选）：分支验证无问题后合并 + 标 v1.0.0
4. **阶段二启动**：参考 spec 章节 8 的 Out of Scope 项 + summary 中的"阶段二预告"
