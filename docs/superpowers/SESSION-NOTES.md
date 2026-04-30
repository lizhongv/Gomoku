# Gomoku 产品级重塑阶段一 · 会话进度备忘

**Last updated**: 2026-04-30
**Branch**: `redesign-stage1`（已推送 `origin/redesign-stage1`）
**Spec**: [`docs/superpowers/specs/2026-04-30-gomoku-product-redesign-stage1-design.md`](specs/2026-04-30-gomoku-product-redesign-stage1-design.md)
**Plan**: [`docs/superpowers/plans/2026-04-30-gomoku-product-redesign-stage1.md`](plans/2026-04-30-gomoku-product-redesign-stage1.md)

---

## 当前进度

| Phase | 主题 | Tasks | 状态 |
|---|---|---|---|
| 0 | 准备：分支/依赖/测试脚本 | 3 | ✅ 完成 |
| 1 | 设计系统基础（Tokens/Fonts/Skin/Profile 扩展） | 6 | ✅ 完成 |
| **2** | **组件库 G* + Router/Scene/EventBus** | **12** | **⬜ 下一个** |
| 3 | SkinManager + 4 套皮肤 | 5 | ⬜ |
| 4 | HomeScene + LobbyScene + 重写既有视图 | 6 | ⬜ |
| 5 | GameSession + GameScene + 重做对局 | 7 | ⬜ |
| 6 | ReviewScene + Sound + Rank | 8 | ⬜ |
| 7 | ShopScene + 金币/签到 + Emote + GIF | 9 | ⬜ |
| 8 | 错误收口 + Settings + 文案 + 音效 | 4 | ⬜ |
| 9 | 测试 + 重打包 + 文档 | 4 | ⬜ |

**Phase 1 收尾的 commit 链**（最早 → 最近）：

```
71eb626  0.2: 新增 GIF 编码依赖 + 单元测试运行脚本
0541b8b  0.3: 新增包结构占位目录
efae83b  1.0: 引入既有 AppConfig / Profile 雏形 / FlatLaf / 配置示例
25353d9  1.1: 新增 Tokens 设计变量数据类
3717bd5  1.2: 新增 Fonts 字体选择器
1094c9a  1.3: 新增 BoardStyle / PieceStyle 棋盘棋子绘制规则
a7a398e  1.4: 新增 Skin 皮肤数据类
7941f59  1.5: Profile 扩展段位/皮肤/金币/签到字段
bc3168c  1.6: ProfileTest 单元测试（往返+字段升级兼容）
```

---

## 测试基线（每次 Phase 完成必须保持全过）

| 测试套件 | 数量 | 状态 |
|---|---|---|
| `src/test/java/E2eTest.java`（联机协议主流程） | 28 | ✅ |
| `src/test/java/E2eTest2.java`（GIVEUP / TALK 边界） | 8 | ✅ |
| `src/test/java/profile/ProfileTest.java` | 12 | ✅ |
| **总计** | **48** | **✅** |

---

## 下次会话起步指引

### 1. 拉取最新代码

```bash
cd C:/Users/zhongli2/Documents/code/Gomoku
git fetch origin
git checkout redesign-stage1
git pull --ff-only
```

### 2. 跑基线验证

```bash
# 杀掉残留 java 进程（避免 server 端口占用 / 数据污染）
taskkill //F //IM java.exe 2>&1 | head -1
taskkill //F //IM Gomoku-Server.exe 2>&1 | head -1
taskkill //F //IM Gomoku-Client.exe 2>&1 | head -1
sleep 2

# 编译
bash scripts/build.sh 2>&1 | iconv -f gbk -t utf-8 -c | tail -3
# 期望：Build OK.

# 单元测试
bash scripts/run-unit-tests.sh 2>&1 | grep -E "结果:|FAIL"
# 期望：=== profile.ProfileTest === ... 结果: passed=12 failed=0

# E2E（需要先起 server）
java -cp "target/classes;lib/*" server.FiveServer > /tmp/srv.log 2>&1 &
sleep 3
bash scripts/run-tests.sh 2>&1 | grep -E "结果:|All tests|FAILED"
taskkill //F //IM java.exe 2>&1 | head -1
# 期望：passed=28 + passed=8 + All tests passed.
```

### 3. 继续执行 Phase 2

打开 plan 文件 `docs/superpowers/plans/2026-04-30-gomoku-product-redesign-stage1.md` 找到 **## Phase 2: 组件库 G* + Router/Scene/EventBus 框架**。

按 `superpowers:subagent-driven-development` 工作流，**派一个 implementer subagent 一次完成 Phase 2 的全部 12 个 task**：

| Task | 内容 | 文件 |
|---|---|---|
| 2.1 | EventBus + 6 个事件类型 | `ui/Event.java`, `ui/EventBus.java`, `ui/events/*.java` |
| 2.2 | EventBusTest 单元测试 | `src/test/java/ui/EventBusTest.java` |
| 2.3 | Scene 接口 + Router | `ui/scene/Scene.java`, `ui/scene/Router.java` |
| 2.4 | Theme + ThemeAware | `ui/Theme.java`, `ui/ThemeAware.java` |
| 2.5 | GButton（PRIMARY/SECONDARY/GHOST） | `ui/component/GButton.java` |
| 2.6 | GCard 卡片容器 | `ui/component/GCard.java` |
| 2.7 | GAvatar 圆形头像 | `ui/component/GAvatar.java` |
| 2.8 | GChip 圆胶囊标签 | `ui/component/GChip.java` |
| 2.9 | GToast 浮动提示 | `ui/component/GToast.java` |
| 2.10 | GDialog 圆角对话框基类 | `ui/component/GDialog.java` |
| 2.11 | GTextField/GSlider/GTab/GBadge | `ui/component/GTextField.java` 等 4 个 |
| 2.12 | GBoard 棋盘组件（动态尺寸+悬停+呼吸光环） | `ui/component/GBoard.java` |

**派遣模板**（参考本 spec 中其他 Phase 的 implementer prompt 风格）：

```
Task 工具 (general-purpose)
  description: "Implement Phase 2: 组件库 + EventBus + Router"
  prompt: |
    你是 Gomoku Phase 2 implementer。Phase 0+1 已完成（HEAD ≈ bc3168c）。
    plan 在 docs/superpowers/plans/2026-04-30-gomoku-product-redesign-stage1.md
    依次完成 Task 2.1-2.12 全部 12 个 task。
    每个 Task 一个独立 commit，message 格式 "2.X: <subject>"。
    [此处粘贴 plan 中 Phase 2 全部内容]
    完成后跑：build.sh + run-tests.sh + run-unit-tests.sh，期望 48+3=51 断言全过。
```

由于 Task 2.5-2.11 plan 给的是 "实现要点"（非完整代码），implementer 需要自行写 `paint` 等代码。**关键约束**已在 plan 中说明：
- 颜色/字体/间距全部读 `Theme.tokens()`，禁止硬编码
- 实现 `ThemeAware` 接口（如适用）
- 抗锯齿统一开启
- 文件保持 < 200 行

### 4. Phase 2 完成后

派 spec compliance reviewer + code quality reviewer 各一个。如果 reviewer 发现问题，再派 implementer 修。修完进 Phase 3。

---

## 工作约定（贯穿整个阶段一）

- **分支**：`redesign-stage1`，从不直接改 master/packaging
- **commit message 格式**：`<phase>.<task>: <subject>` 中文 OK
  - 例：`2.5: GButton 基础按钮组件（PRIMARY/SECONDARY/GHOST）`
- **每 Task 完成必须 commit**，不要批量提交
- **每 Phase 完成必须跑全套测试**：build.sh + run-tests.sh + run-unit-tests.sh
- **回归门禁**：48 断言（E2E 28+8 + ProfileTest 12）+ 后续新增的单元测试不能破
- **shell**：用 Bash 工具（项目脚本是 .sh），不要用 PowerShell
- **路径分隔符**：classpath 用 `;`（Windows），但路径写 `/`（bash 兼容）
- **中文乱码**：bash 命令中文输出 stderr 可能 GBK，加 `| iconv -f gbk -t utf-8 -c` 解
- **服务器残留**：跑 E2E 前先 `taskkill //F //IM java.exe` 清掉残留连接（避免 ADD 广播污染）
- **未跟踪文件**：每个 phase 开始前检查 `git status -uno`，应为干净

---

## 已知陷阱

1. **`git reset --hard` 被沙箱拦截** — 用 `git merge --ff-only <other>` 或 `git checkout <branch>` 替代
2. **bash heredoc 写大块内容会 ENAMETOOLONG** — 改用 Write 工具写到独立文件后 `cat >>`
3. **批处理 .bat 文件必须 CRLF + 纯 ASCII** — `sed -i 's/$/\r/' file.bat` 转换
4. **JOptionPane 的中文** 在 jpackage runtime 上偶尔乱码 — Phase 8 会统一替换为 `GToast`
5. **服务端 zhongli2 等历史用户**会污染 E2E 的"新用户应该看到自己 + 对方"断言 — 测试前确保杀干净所有客户端

---

## 资源 / 依赖清单

```
lib/
├── flatlaf-3.4.1.jar              FlatLaf 主题引擎（Phase 1 引入）
├── animated-gif-lib-1.4.jar       GIF 编码（Phase 0.2 引入，Phase 7.8 用）
└── mysql-connector-j-8.4.0.jar    JDBC 驱动（既有）

src/main/java/
├── ai/                  Evaluate（保留），AiBoard/AiChess/AiGame 待 Phase 5 删
├── client/              旧 UI（待 Phase 5.7 全删）
├── config/AppConfig     ✓ 已扩展，配置中心
├── game/                Game/Manual/DAO（保留）
├── profile/Profile      ✓ 已扩展（13 字段）
├── protocol/            Message/Command/Net/SafeObjectInputStream（保留）
├── server/FiveServer    保留（不改服务端协议）
├── tool/CreateDatabase  保留
├── ui/
│   ├── design/          ✓ Tokens/Fonts/BoardStyle/PieceStyle/Skin（Phase 1 完成）
│   ├── component/       ⬜ G* 组件（Phase 2）
│   └── scene/           ⬜ Scene + Router + 6 视图（Phase 2/4/5/6/7/8）
├── feature/
│   ├── skin/            ⬜ Phase 3
│   ├── sound/           ⬜ Phase 6
│   ├── review/          ⬜ Phase 6
│   ├── rank/            ⬜ Phase 6
│   ├── emote/           ⬜ Phase 7
│   ├── share/           ⬜ Phase 7
│   └── shop/            ⬜ Phase 7
├── net/                 ⬜ Phase 5（GameSession 抽象 + Communication 迁过来）
└── app/                 ⬜ Phase 4.1（Main.java）
```

---

## 远端

- GitHub: https://github.com/lizhongv/Gomoku
- 分支：
  - `master` — 历史
  - `packaging` — 阶段一前的最后里程碑（带打包脚本 + jpackage 验证过）
  - `redesign-stage1` — 当前活跃分支
- PR 链接（如要发 PR）：https://github.com/lizhongv/Gomoku/pull/new/redesign-stage1

---

## 整体节奏

剩余 8 个 Phase 大致每 phase 1-2 次会话，预计 **8-12 次会话**完成阶段一。每次会话开始照本指引第 2 步跑基线，结束时确认所有测试全过 + commit + 更新本文件的"当前进度"表。
