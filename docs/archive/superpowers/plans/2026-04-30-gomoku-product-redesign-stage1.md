# Gomoku 产品级重塑阶段一 · 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把现有 Gomoku 桌面五子棋从"功能完备的内部工具"重塑为"看上去能上线变现的产品"——重写 client UI 层，新增 7 个 feature 模块（皮肤 / 复盘 / 音效 / 段位 / 表情 / GIF / 商店）。

**Architecture:** Approach 3（全重写 client UI 层）。保留所有非 UI 代码（protocol/game/user/server/ai 算法/profile/config）。新引入 ui (design + component + scene + EventBus) + feature/* 模块结构。GameScene 通过 GameSession 抽象统一联机 / 人机模式。

**Tech Stack:** Java 17 + Swing + FlatLaf 3.4.1 + animated-gif-lib 1.4 + 既有 MySQL Connector/J 8.4.0。jpackage 出 Windows app-image。

**Spec:** [docs/superpowers/specs/2026-04-30-gomoku-product-redesign-stage1-design.md](../specs/2026-04-30-gomoku-product-redesign-stage1-design.md)

---

## 阶段划分（对应 9 个会话）

| Phase | 主题 | 任务数 |
|---|---|---|
| **0** | 准备：分支、依赖、回归基线 | 3 |
| **1** | 包重组 + DesignTokens + Skin 数据模型 | 6 |
| **2** | 组件库 G* + Router/Scene 框架 + EventBus | 12 |
| **3** | SkinManager + 4 套皮肤（Classic 精修） | 5 |
| **4** | HomeScene + LobbyScene + 重写既有视图 | 6 |
| **5** | GameSession 抽象 + GameScene + 重做 PanelBoard | 7 |
| **6** | ReviewScene + ReviewEngine + Sound + Rank | 8 |
| **7** | ShopScene + 金币 / 签到 + Emote + GIF | 9 |
| **8** | 错误收口（GToast 替代 JOptionPane）+ 文案 + 音效素材 | 4 |
| **9** | 全套手工冒烟 + 重打包 + 文档 | 4 |

---

## 全局约定

- **每完成一个 Task 后立即跑：**
  ```bash
  bash scripts/build.sh && bash scripts/run-tests.sh
  ```
  期望：`Build OK.` + `passed=28 failed=0` + `passed=8 failed=0`
- **commit 信息格式**：`<phase>.<task>: <subject>`（中文 OK）
- **单元测试位置**：`src/test/java/<package>/<Class>Test.java`，约定使用纯断言（已有 E2eTest 风格，无 JUnit 依赖）
- **测试在没有 JUnit 时的执行方式**：每个测试类带 `public static void main`，从 `scripts/run-unit-tests.{sh,bat}` 调用（Phase 0 创建）
- **包路径分隔符**：项目目前没有顶级包前缀（`client.X` / `ai.X`），新结构保持无前缀（`ui.design.Tokens` / `feature.skin.SkinManager`）

---

## Phase 0: 准备

### Task 0.1: 创建新分支 + 同步 packaging 分支基础

**Files:**
- Verify: `git status` 干净

- [ ] **Step 1: 检查当前分支与状态**

  Run: `cd C:/Users/zhongli2/Documents/code/Gomoku && git status && git branch`
  Expected: 在 `packaging` 分支，工作区干净

- [ ] **Step 2: 创建新分支 `redesign-stage1`**

  Run: `git checkout -b redesign-stage1`
  Expected: `Switched to a new branch 'redesign-stage1'`

- [ ] **Step 3: 验证基线编译 + 测试通过**

  Run:
  ```bash
  taskkill //F //IM java.exe 2>&1 | head -1
  bash scripts/build.sh && \
    java -cp "target/classes;lib/*" server.FiveServer > /tmp/srv.log 2>&1 &
  sleep 3
  bash scripts/run-tests.sh | grep -E "结果:|All tests|FAILED"
  taskkill //F //IM java.exe 2>&1 | head -1
  ```
  Expected: `Build OK.` + `passed=28 failed=0` + `passed=8 failed=0` + `All tests passed.`

---

### Task 0.2: 加入 animated-gif-lib 依赖 + 创建测试运行脚本

**Files:**
- Create: `lib/animated-gif-lib-1.4.jar` (download)
- Create: `scripts/run-unit-tests.sh`
- Create: `scripts/run-unit-tests.bat`

- [ ] **Step 1: 下载 animated-gif-lib**

  Run:
  ```bash
  cd C:/Users/zhongli2/Documents/code/Gomoku
  curl -sSL -o lib/animated-gif-lib-1.4.jar \
    https://repo1.maven.org/maven2/com/madgag/animated-gif-lib/1.4/animated-gif-lib-1.4.jar
  ls -lh lib/animated-gif-lib-1.4.jar
  ```
  Expected: 文件存在，约 35-40 KB

- [ ] **Step 2: 创建 run-unit-tests.sh**

  Write `scripts/run-unit-tests.sh`:
  ```bash
  #!/usr/bin/env bash
  # 运行 src/test/java 下所有带 public static void main 的测试类
  # 注：项目无 JUnit 依赖，约定每个测试类自带 main 入口，最后打印 "passed=N failed=0"
  set -e
  cd "$(dirname "$0")/.."
  case "$(uname -s)" in
      MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
      *) SEP=':' ;;
  esac
  CP="target/classes${SEP}target/test-classes${SEP}lib/*"
  EXIT=0
  for cls in $(find src/test/java -name '*Test.java' \
       | sed 's|src/test/java/||;s|\.java$||;s|/|.|g'); do
      # 跳过 E2eTest（需服务器）
      case "$cls" in E2eTest|E2eTest2) continue ;; esac
      echo "=== $cls ==="
      java -Dfile.encoding=UTF-8 -cp "$CP" "$cls" || EXIT=1
  done
  exit $EXIT
  ```
  `chmod +x scripts/run-unit-tests.sh`

- [ ] **Step 3: 创建 run-unit-tests.bat（CRLF 行尾，纯 ASCII REM）**

  Write `scripts/run-unit-tests.bat`:
  ```bat
  @echo off
  REM Run unit tests under src/test/java (skip E2eTest which needs server)
  setlocal
  pushd "%~dp0\.."
  set CP=target\classes;target\test-classes;lib\*
  set EXIT=0
  for /r src\test\java %%f in (*Test.java) do (
      set "REL=%%~ff"
      call :runone "%%~nf"
  )
  popd
  exit /b %EXIT%

  :runone
  if "%~1"=="E2eTest" goto :eof
  if "%~1"=="E2eTest2" goto :eof
  echo === %~1 ===
  java -Dfile.encoding=UTF-8 -cp "%CP%" %~1
  if errorlevel 1 set EXIT=1
  goto :eof
  ```
  Convert to CRLF: `sed -i 's/$/\r/' scripts/run-unit-tests.bat`

- [ ] **Step 4: Commit**

  ```bash
  git add lib/animated-gif-lib-1.4.jar scripts/run-unit-tests.sh scripts/run-unit-tests.bat
  git commit -m "0.2: 新增 GIF 编码依赖 + 单元测试运行脚本"
  ```

---

### Task 0.3: 包结构占位（创建空目录）

**Files:**
- Create dirs: `src/main/java/{ui/design,ui/component,ui/scene,feature/skin,feature/sound,feature/review,feature/rank,feature/emote,feature/share,feature/shop,net,app}`

- [ ] **Step 1: 创建所有新包目录 + .gitkeep 占位**

  Run:
  ```bash
  cd C:/Users/zhongli2/Documents/code/Gomoku
  for d in ui/design ui/component ui/scene \
           feature/skin feature/sound feature/review feature/rank \
           feature/emote feature/share feature/shop \
           net app; do
    mkdir -p "src/main/java/$d"
    touch "src/main/java/$d/.gitkeep"
  done
  ls -la src/main/java/ui/ src/main/java/feature/
  ```
  Expected: 看到 design / component / scene 三个子目录 + 7 个 feature 子目录

- [ ] **Step 2: Commit**

  ```bash
  git add src/main/java/ui src/main/java/feature src/main/java/net src/main/java/app
  git commit -m "0.3: 新增包结构占位目录"
  ```

---

## Phase 1: 包重组 + 设计系统基础

### Task 1.1: 创建 `ui.design.Tokens` 颜色 / 字体 / 间距数据类

**Files:**
- Create: `src/main/java/ui/design/Tokens.java`

- [ ] **Step 1: 写 Tokens 类（不可变数据类，所有视觉变量）**

  Write `src/main/java/ui/design/Tokens.java`:
  ```java
  // 设计变量集合：所有颜色 / 字体 / 间距 / 圆角集中在此
  // 不直接 new，由 Skin 提供
  package ui.design;

  import java.awt.Color;
  import java.awt.Font;

  public final class Tokens {
      // 背景色
      public final Color bgPrimary, bgSecondary, bgCard, bgBoard, bgToolbar;
      // 文字色
      public final Color fgDefault, fgMuted, fgOnAccent;
      // 强调色
      public final Color accent, accentDark, danger, success, warning;
      // 棋盘
      public final Color boardLine, boardStar, boardShadow, lastMoveRing;
      // 边框
      public final Color borderLight, borderMedium;

      // 间距（8pt grid）
      public final int space1 = 4, space2 = 8, space3 = 12, space4 = 16,
                       space5 = 24, space6 = 32, space7 = 48;

      // 字体
      public final Font fontDisplay, fontTitle, fontHeading, fontBody, fontCaption, fontMono;

      // 圆角
      public final int radiusSm = 4, radiusMd = 8, radiusLg = 12, radiusFull = 999;

      private Tokens(Builder b) {
          bgPrimary = b.bgPrimary; bgSecondary = b.bgSecondary; bgCard = b.bgCard;
          bgBoard = b.bgBoard; bgToolbar = b.bgToolbar;
          fgDefault = b.fgDefault; fgMuted = b.fgMuted; fgOnAccent = b.fgOnAccent;
          accent = b.accent; accentDark = b.accentDark; danger = b.danger;
          success = b.success; warning = b.warning;
          boardLine = b.boardLine; boardStar = b.boardStar;
          boardShadow = b.boardShadow; lastMoveRing = b.lastMoveRing;
          borderLight = b.borderLight; borderMedium = b.borderMedium;
          fontDisplay = b.fontDisplay; fontTitle = b.fontTitle; fontHeading = b.fontHeading;
          fontBody = b.fontBody; fontCaption = b.fontCaption; fontMono = b.fontMono;
      }

      public static Builder builder() { return new Builder(); }

      public static class Builder {
          public Color bgPrimary, bgSecondary, bgCard, bgBoard, bgToolbar;
          public Color fgDefault, fgMuted, fgOnAccent;
          public Color accent, accentDark, danger, success, warning;
          public Color boardLine, boardStar, boardShadow, lastMoveRing;
          public Color borderLight, borderMedium;
          public Font fontDisplay, fontTitle, fontHeading, fontBody, fontCaption, fontMono;
          public Tokens build() { return new Tokens(this); }
      }
  }
  ```

- [ ] **Step 2: Compile check**

  Run: `bash scripts/build.sh 2>&1 | iconv -f gbk -t utf-8 -c | tail -5`
  Expected: `Build OK.`

- [ ] **Step 3: Commit**

  ```bash
  git add src/main/java/ui/design/Tokens.java
  git commit -m "1.1: 新增 Tokens 设计变量数据类"
  ```

---

### Task 1.2: 创建 `ui.design.Fonts` 字体选择器

**Files:**
- Create: `src/main/java/ui/design/Fonts.java`

- [ ] **Step 1: 写 Fonts 工具类（中文字体回退 + 等宽字体）**

  Write `src/main/java/ui/design/Fonts.java`:
  ```java
  // 字体选择：自动选择系统中最优的中文字体 / 等宽字体
  package ui.design;

  import java.awt.Font;
  import java.awt.GraphicsEnvironment;
  import java.util.Arrays;
  import java.util.HashSet;
  import java.util.Set;

  public final class Fonts {
      private static final Set<String> AVAILABLE;
      static {
          AVAILABLE = new HashSet<>(Arrays.asList(
              GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
      }

      private Fonts() {}

      public static Font sans(int size, int style) {
          String[] candidates = {
              "Microsoft YaHei UI", "Microsoft YaHei",
              "PingFang SC", "Hiragino Sans GB",
              "Noto Sans CJK SC", "WenQuanYi Micro Hei",
              "SimHei", "SimSun", "Dialog"
          };
          return pick(candidates, size, style);
      }

      public static Font serif(int size, int style) {
          // B 温暖怀旧风格用衬线（如可用）
          String[] candidates = {
              "STSong", "SimSun", "FangSong", "STKaiti", "KaiTi",
              "Georgia", "Times New Roman", "Serif"
          };
          return pick(candidates, size, style);
      }

      public static Font mono(int size, int style) {
          String[] candidates = {
              "Consolas", "Cascadia Mono", "Menlo",
              "DejaVu Sans Mono", Font.MONOSPACED
          };
          return pick(candidates, size, style);
      }

      private static Font pick(String[] candidates, int size, int style) {
          for (String c : candidates) {
              if (AVAILABLE.contains(c)) return new Font(c, style, size);
          }
          return new Font(Font.DIALOG, style, size);
      }
  }
  ```

- [ ] **Step 2: Compile check**

  Run: `bash scripts/build.sh | tail -3`
  Expected: `Build OK.`

- [ ] **Step 3: Commit**

  ```bash
  git add src/main/java/ui/design/Fonts.java
  git commit -m "1.2: 新增 Fonts 字体选择器"
  ```

---

### Task 1.3: 创建 `ui.design.BoardStyle` + `ui.design.PieceStyle` 棋盘 / 棋子绘制规则

**Files:**
- Create: `src/main/java/ui/design/BoardStyle.java`
- Create: `src/main/java/ui/design/PieceStyle.java`

- [ ] **Step 1: 写 BoardStyle**

  Write `src/main/java/ui/design/BoardStyle.java`:
  ```java
  // 棋盘绘制规则（背景渐变 / 木纹 / 星位样式）
  package ui.design;

  import java.awt.Color;

  public final class BoardStyle {
      public final Color bgTop, bgBottom;     // 渐变两端
      public final boolean drawWoodGrain;     // 是否程序化绘制木纹
      public final int starDotSize;           // 星位圆点直径（像素）
      public final float lineStrokeMul;       // 线宽倍率（相对 span）

      public BoardStyle(Color bgTop, Color bgBottom, boolean drawWoodGrain,
                        int starDotSize, float lineStrokeMul) {
          this.bgTop = bgTop;
          this.bgBottom = bgBottom;
          this.drawWoodGrain = drawWoodGrain;
          this.starDotSize = starDotSize;
          this.lineStrokeMul = lineStrokeMul;
      }
  }
  ```

- [ ] **Step 2: 写 PieceStyle**

  Write `src/main/java/ui/design/PieceStyle.java`:
  ```java
  // 棋子绘制规则（黑/白渐变参数）
  package ui.design;

  import java.awt.Color;

  public final class PieceStyle {
      // 黑棋
      public final Color blackHighlight;  // 渐变高光颜色
      public final Color blackBase;       // 渐变基色
      // 白棋
      public final Color whiteHighlight;
      public final Color whiteBase;
      public final boolean dropShadow;    // 是否画落地阴影

      public PieceStyle(Color blackHighlight, Color blackBase,
                        Color whiteHighlight, Color whiteBase,
                        boolean dropShadow) {
          this.blackHighlight = blackHighlight;
          this.blackBase = blackBase;
          this.whiteHighlight = whiteHighlight;
          this.whiteBase = whiteBase;
          this.dropShadow = dropShadow;
      }
  }
  ```

- [ ] **Step 3: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/design/BoardStyle.java src/main/java/ui/design/PieceStyle.java
  git commit -m "1.3: 新增 BoardStyle / PieceStyle 棋盘棋子绘制规则"
  ```

---

### Task 1.4: 创建 `ui.design.Skin` 皮肤数据类

**Files:**
- Create: `src/main/java/ui/design/Skin.java`

- [ ] **Step 1: 写 Skin（不可变数据类）**

  Write `src/main/java/ui/design/Skin.java`:
  ```java
  // 皮肤包：一个完整的视觉规则集（含 Tokens + BoardStyle + PieceStyle）
  package ui.design;

  public final class Skin {
      public final String id;
      public final String displayName;
      public final boolean premium;       // 是否需付费占位
      public final int priceCoins;        // 用金币购买的价格（free=0）
      public final Tokens tokens;
      public final BoardStyle board;
      public final PieceStyle pieces;

      public Skin(String id, String displayName, boolean premium, int priceCoins,
                  Tokens tokens, BoardStyle board, PieceStyle pieces) {
          this.id = id;
          this.displayName = displayName;
          this.premium = premium;
          this.priceCoins = priceCoins;
          this.tokens = tokens;
          this.board = board;
          this.pieces = pieces;
      }
  }
  ```

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/design/Skin.java
  git commit -m "1.4: 新增 Skin 皮肤数据类"
  ```

---

### Task 1.5: 重组 Profile 到 `core/model`（保留旧路径兼容）

**Files:**
- Modify: `src/main/java/profile/Profile.java`（新增字段：elo / peakElo / weeklyWins / weeklyLosses / weekStartMillis / lastCheckinDate / consecutiveCheckinDays / coins / skinId / unlockedSkinIds）
- 注：spec 提到 model 包，但为减少改动，profile 包保持不动，只扩展字段

- [ ] **Step 1: 读现有 Profile**

  Run: `cat src/main/java/profile/Profile.java`

- [ ] **Step 2: 在 Profile 类中新增字段（视觉与变现相关）**

  Edit `src/main/java/profile/Profile.java`，在 `public boolean onboardingDone = false;` 之后新增：
  ```java
      // === 段位（阶段一本地 ELO）===
      public int  elo = 1200;                 // 初始 ELO
      public int  peakElo = 1200;
      public int  weeklyWins = 0;
      public int  weeklyLosses = 0;
      public long weekStartMillis = 0;        // 本周一 0 点；切周自动重置

      // === 皮肤 / 商店 ===
      public String skinId = "classic";       // 当前皮肤
      public String unlockedSkinIds = "classic";  // 逗号分隔的已解锁列表

      // === 金币 / 签到 ===
      public int  coins = 0;
      public String lastCheckinDate = "";     // yyyy-MM-dd 格式
      public int  consecutiveCheckinDays = 0;
  ```

- [ ] **Step 3: 在 `load()` 方法中追加新字段读取**

  在 `p.onboardingDone = ...` 行之后新增：
  ```java
      p.elo                    = parseInt(props, "elo", 1200);
      p.peakElo                = parseInt(props, "peakElo", 1200);
      p.weeklyWins             = parseInt(props, "weeklyWins", 0);
      p.weeklyLosses           = parseInt(props, "weeklyLosses", 0);
      p.weekStartMillis        = parseLong(props, "weekStartMillis", 0);
      p.skinId                 = props.getProperty("skinId", "classic");
      p.unlockedSkinIds        = props.getProperty("unlockedSkinIds", "classic");
      p.coins                  = parseInt(props, "coins", 0);
      p.lastCheckinDate        = props.getProperty("lastCheckinDate", "");
      p.consecutiveCheckinDays = parseInt(props, "consecutiveCheckinDays", 0);
  ```

- [ ] **Step 4: 新增 `parseLong` 私有方法**

  在 `parseInt` 方法之后新增：
  ```java
      private static long parseLong(Properties p, String k, long dft) {
          try { return Long.parseLong(p.getProperty(k, String.valueOf(dft))); }
          catch (NumberFormatException e) { return dft; }
      }
  ```

- [ ] **Step 5: 在 `save()` 中追加新字段写入**

  在 `props.setProperty("onboardingDone", ...)` 之后新增：
  ```java
      props.setProperty("elo", String.valueOf(elo));
      props.setProperty("peakElo", String.valueOf(peakElo));
      props.setProperty("weeklyWins", String.valueOf(weeklyWins));
      props.setProperty("weeklyLosses", String.valueOf(weeklyLosses));
      props.setProperty("weekStartMillis", String.valueOf(weekStartMillis));
      props.setProperty("skinId", skinId == null ? "classic" : skinId);
      props.setProperty("unlockedSkinIds", unlockedSkinIds == null ? "classic" : unlockedSkinIds);
      props.setProperty("coins", String.valueOf(coins));
      props.setProperty("lastCheckinDate", lastCheckinDate == null ? "" : lastCheckinDate);
      props.setProperty("consecutiveCheckinDays", String.valueOf(consecutiveCheckinDays));
  ```

- [ ] **Step 6: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/profile/Profile.java
  git commit -m "1.5: Profile 扩展段位/皮肤/金币/签到字段"
  ```

---

### Task 1.6: 写 ProfileTest 单元测试（save/load 往返 + 字段升级兼容）

**Files:**
- Create: `src/test/java/profile/ProfileTest.java`

- [ ] **Step 1: 写测试类（无 JUnit，自带 main）**

  Write `src/test/java/profile/ProfileTest.java`:
  ```java
  // ProfileTest：save/load 往返 + 字段升级兼容
  package profile;

  import java.io.File;
  import java.io.FileOutputStream;
  import java.util.Properties;

  public class ProfileTest {
      static int passed = 0, failed = 0;

      public static void main(String[] args) throws Exception {
          // 用临时目录隔离
          File tmp = new File(System.getProperty("java.io.tmpdir"),
                              "gomoku-test-" + System.currentTimeMillis());
          tmp.mkdirs();
          System.setProperty("user.home", tmp.getAbsolutePath());
          // 强制 AppConfig 重新读取 user.home（清掉旧档案）
          File old = new File(tmp, ".gomoku/profile.properties");
          if (old.exists()) old.delete();

          testRoundTrip();
          testLoadOldProfileFillsDefaults();

          System.out.println("结果: passed=" + passed + " failed=" + failed);
          System.exit(failed == 0 ? 0 : 1);
      }

      static void testRoundTrip() throws Exception {
          // 写入档案
          Profile p = new Profile();
          p.nickname = "测试玩家";
          p.elo = 1450;
          p.coins = 99;
          p.skinId = "darkwood";
          p.unlockedSkinIds = "classic,darkwood";
          p.consecutiveCheckinDays = 5;
          p.save();

          // 重置缓存并重新加载
          resetSingleton();
          Profile loaded = Profile.load();
          eq("nickname round-trip", "测试玩家", loaded.nickname);
          eq("elo round-trip", "1450", String.valueOf(loaded.elo));
          eq("coins round-trip", "99", String.valueOf(loaded.coins));
          eq("skinId round-trip", "darkwood", loaded.skinId);
          eq("unlockedSkins round-trip", "classic,darkwood", loaded.unlockedSkinIds);
          eq("checkin days", "5", String.valueOf(loaded.consecutiveCheckinDays));
      }

      static void testLoadOldProfileFillsDefaults() throws Exception {
          // 模拟"旧档案"——只含老字段
          File f = new File(System.getProperty("user.home"), ".gomoku/profile.properties");
          f.getParentFile().mkdirs();
          Properties oldProps = new Properties();
          oldProps.setProperty("nickname", "老用户");
          oldProps.setProperty("aiWins", "10");
          try (FileOutputStream out = new FileOutputStream(f)) {
              oldProps.store(out, "old test profile");
          }
          resetSingleton();
          Profile loaded = Profile.load();
          eq("旧档案 nickname 保留", "老用户", loaded.nickname);
          eq("旧档案 aiWins 保留", "10", String.valueOf(loaded.aiWins));
          eq("新字段默认 elo=1200", "1200", String.valueOf(loaded.elo));
          eq("新字段默认 coins=0", "0", String.valueOf(loaded.coins));
          eq("新字段默认 skinId=classic", "classic", loaded.skinId);
      }

      static void resetSingleton() throws Exception {
          java.lang.reflect.Field f = Profile.class.getDeclaredField("instance");
          f.setAccessible(true);
          f.set(null, null);
      }

      static void eq(String name, String expected, String actual) {
          if (expected.equals(actual)) {
              passed++;
              System.out.println("  ✓ " + name);
          } else {
              failed++;
              System.out.println("  ✗ " + name + " expected=[" + expected + "] actual=[" + actual + "]");
          }
      }
  }
  ```

- [ ] **Step 2: 编译测试**

  Run: `bash scripts/build.sh | tail -3`
  Expected: `Build OK.`

- [ ] **Step 3: 跑测试看通过**

  Run: `bash scripts/run-unit-tests.sh 2>&1 | tail -20`
  Expected: `=== profile.ProfileTest ===` ... `结果: passed=11 failed=0`

- [ ] **Step 4: Commit**

  ```bash
  git add src/test/java/profile/ProfileTest.java
  git commit -m "1.6: ProfileTest 单元测试（往返+升级兼容）"
  ```

---

> **Phase 1 检查点**
>
> 跑 `bash scripts/build.sh && bash scripts/run-tests.sh && bash scripts/run-unit-tests.sh`
> 期望：Build OK / E2E 36 全过 / ProfileTest 全过
>
> **Phase 2 见后续**（待续：组件库 G* + Router/Scene/EventBus 框架）。

---

## Phase 2: 组件库 G* + Router/Scene/EventBus 框架

### Task 2.1: EventBus 事件总线 + 6 种事件类型

**Files:**
- Create: `src/main/java/ui/EventBus.java`
- Create: `src/main/java/ui/Event.java` (interface)
- Create: `src/main/java/ui/events/{SkinChangedEvent, GameStartEvent, GameEndEvent, MoveEvent, RankUpEvent, CoinsChangedEvent}.java`

- [ ] **Step 1: 写 Event 接口**

  Write `src/main/java/ui/Event.java`:
  ```java
  package ui;
  public interface Event {}
  ```

- [ ] **Step 2: 写 EventBus**

  Write `src/main/java/ui/EventBus.java`:
  ```java
  // 简单进程内事件总线：发布 / 订阅 / 取消订阅
  // 所有回调在 Swing EDT 触发
  package ui;

  import java.util.*;
  import java.util.concurrent.CopyOnWriteArrayList;
  import java.util.function.Consumer;
  import javax.swing.SwingUtilities;

  public final class EventBus {
      private static final Map<Class<? extends Event>, CopyOnWriteArrayList<Consumer<Event>>> SUBS = new HashMap<>();
      private EventBus() {}

      @SuppressWarnings({"rawtypes", "unchecked"})
      public static synchronized <T extends Event> void subscribe(Class<T> type, Consumer<T> handler) {
          SUBS.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add((Consumer) handler);
      }

      @SuppressWarnings({"rawtypes", "unchecked"})
      public static synchronized <T extends Event> void unsubscribe(Class<T> type, Consumer<T> handler) {
          List<Consumer<Event>> list = SUBS.get(type);
          if (list != null) list.remove((Consumer) handler);
      }

      public static void publish(Event e) {
          List<Consumer<Event>> list;
          synchronized (EventBus.class) { list = SUBS.get(e.getClass()); }
          if (list == null || list.isEmpty()) return;
          List<Consumer<Event>> snapshot = new ArrayList<>(list);
          Runnable r = () -> {
              for (Consumer<Event> h : snapshot) {
                  try { h.accept(e); } catch (Exception ex) { ex.printStackTrace(); }
              }
          };
          if (SwingUtilities.isEventDispatchThread()) r.run();
          else SwingUtilities.invokeLater(r);
      }

      public static synchronized void reset() { SUBS.clear(); }
  }
  ```

- [ ] **Step 3: 写 6 个 Event 类**

  Write `src/main/java/ui/events/SkinChangedEvent.java`:
  ```java
  package ui.events;
  import ui.Event;
  import ui.design.Skin;
  public class SkinChangedEvent implements Event {
      public final Skin newSkin;
      public SkinChangedEvent(Skin newSkin) { this.newSkin = newSkin; }
  }
  ```

  Write `src/main/java/ui/events/GameStartEvent.java`:
  ```java
  package ui.events;
  import ui.Event;
  public class GameStartEvent implements Event {
      public enum Mode { ONLINE, AI }
      public final Mode mode;
      public final String myColor;        // "black" / "white"
      public final String opponentName;
      public GameStartEvent(Mode m, String myColor, String opp) {
          this.mode = m; this.myColor = myColor; this.opponentName = opp;
      }
  }
  ```

  Write `src/main/java/ui/events/GameEndEvent.java`:
  ```java
  package ui.events;
  import ui.Event;
  import game.Manual;
  public class GameEndEvent implements Event {
      public final boolean youWon;
      public final Manual manual;
      public final GameStartEvent.Mode mode;
      public final int aiDifficultyElo;
      public GameEndEvent(boolean youWon, Manual m, GameStartEvent.Mode mode, int aiDifficultyElo) {
          this.youWon = youWon; this.manual = m; this.mode = mode; this.aiDifficultyElo = aiDifficultyElo;
      }
  }
  ```

  Write `src/main/java/ui/events/MoveEvent.java`:
  ```java
  package ui.events;
  import ui.Event;
  public class MoveEvent implements Event {
      public final int col, row;
      public final boolean self;
      public MoveEvent(int col, int row, boolean self) { this.col = col; this.row = row; this.self = self; }
  }
  ```

  Write `src/main/java/ui/events/RankUpEvent.java`:
  ```java
  package ui.events;
  import ui.Event;
  public class RankUpEvent implements Event {
      public final String fromTier, toTier;
      public RankUpEvent(String fromTier, String toTier) {
          this.fromTier = fromTier; this.toTier = toTier;
      }
  }
  ```

  Write `src/main/java/ui/events/CoinsChangedEvent.java`:
  ```java
  package ui.events;
  import ui.Event;
  public class CoinsChangedEvent implements Event {
      public final int newBalance, delta;
      public final String reason;
      public CoinsChangedEvent(int newBalance, int delta, String reason) {
          this.newBalance = newBalance; this.delta = delta; this.reason = reason;
      }
  }
  ```

- [ ] **Step 4: Compile + Commit**

  ```bash
  mkdir -p src/main/java/ui/events
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/Event.java src/main/java/ui/EventBus.java src/main/java/ui/events
  git commit -m "2.1: EventBus + 6 种事件类型"
  ```

---

### Task 2.2: EventBusTest 单元测试

**Files:**
- Create: `src/test/java/ui/EventBusTest.java`

- [ ] **Step 1: 写测试 + Step 2: 编译跑测试**

  Write `src/test/java/ui/EventBusTest.java`:
  ```java
  package ui;
  import java.util.concurrent.atomic.AtomicInteger;
  import javax.swing.SwingUtilities;

  public class EventBusTest {
      static int passed = 0, failed = 0;
      static class FooEvent implements Event { public final int v; public FooEvent(int v){this.v=v;} }
      static class BarEvent implements Event {}

      public static void main(String[] args) throws Exception {
          EventBus.reset(); testSubscribePublish();
          EventBus.reset(); testUnsubscribe();
          EventBus.reset(); testWrongTypeNoTrigger();
          System.out.println("结果: passed=" + passed + " failed=" + failed);
          System.exit(failed == 0 ? 0 : 1);
      }

      static void testSubscribePublish() throws Exception {
          AtomicInteger c = new AtomicInteger(0);
          EventBus.subscribe(FooEvent.class, e -> c.addAndGet(e.v));
          EventBus.publish(new FooEvent(7));
          SwingUtilities.invokeAndWait(() -> {});
          ok("收到 FooEvent v=7", c.get() == 7);
      }

      static void testUnsubscribe() throws Exception {
          AtomicInteger c = new AtomicInteger(0);
          java.util.function.Consumer<FooEvent> h = e -> c.incrementAndGet();
          EventBus.subscribe(FooEvent.class, h);
          EventBus.unsubscribe(FooEvent.class, h);
          EventBus.publish(new FooEvent(1));
          SwingUtilities.invokeAndWait(() -> {});
          ok("取消订阅后不再触发", c.get() == 0);
      }

      static void testWrongTypeNoTrigger() throws Exception {
          AtomicInteger c = new AtomicInteger(0);
          EventBus.subscribe(FooEvent.class, e -> c.incrementAndGet());
          EventBus.publish(new BarEvent());
          SwingUtilities.invokeAndWait(() -> {});
          ok("Bar 事件不触发 Foo 订阅", c.get() == 0);
      }

      static void ok(String name, boolean cond) {
          if (cond) { passed++; System.out.println("  ✓ " + name); }
          else      { failed++; System.out.println("  ✗ " + name); }
      }
  }
  ```

  Run:
  ```bash
  bash scripts/build.sh | tail -3
  bash scripts/run-unit-tests.sh 2>&1 | tail -10
  ```
  Expected: `结果: passed=3 failed=0`

- [ ] **Step 3: Commit**

  ```bash
  git add src/test/java/ui/EventBusTest.java
  git commit -m "2.2: EventBusTest 单元测试"
  ```

---

### Task 2.3: Scene 接口 + Router

**Files:**
- Create: `src/main/java/ui/scene/Scene.java`
- Create: `src/main/java/ui/scene/Router.java`

- [ ] **Step 1: 写 Scene + Router**

  Write `src/main/java/ui/scene/Scene.java`:
  ```java
  package ui.scene;
  import javax.swing.JComponent;
  public interface Scene {
      JComponent root();
      String title();
      default void onEnter() {}
      default void onLeave() {}
  }
  ```

  Write `src/main/java/ui/scene/Router.java`:
  ```java
  // 场景路由：维护历史栈
  package ui.scene;

  import java.awt.CardLayout;
  import java.util.Deque;
  import java.util.ArrayDeque;
  import javax.swing.JFrame;
  import javax.swing.JPanel;

  public class Router {
      private final JFrame frame;
      private final JPanel cardHolder;
      private final CardLayout cards;
      private final Deque<Scene> stack = new ArrayDeque<>();

      public Router(JFrame frame) {
          this.frame = frame;
          this.cards = new CardLayout();
          this.cardHolder = new JPanel(cards);
      }

      public JPanel cardHolder() { return cardHolder; }
      public Scene current() { return stack.peek(); }

      public void push(Scene s) { stack.push(s); showScene(s); }

      public void replace(Scene s) {
          if (!stack.isEmpty()) {
              Scene old = stack.pop();
              old.onLeave();
              cardHolder.remove(old.root());
          }
          stack.push(s);
          showScene(s);
      }

      public void pop() {
          if (stack.size() <= 1) return;
          Scene old = stack.pop();
          old.onLeave();
          cardHolder.remove(old.root());
          showScene(stack.peek());
      }

      private void showScene(Scene s) {
          String key = s.getClass().getName() + "@" + System.identityHashCode(s);
          boolean exists = false;
          for (java.awt.Component c : cardHolder.getComponents()) {
              if (c == s.root()) { exists = true; break; }
          }
          if (!exists) cardHolder.add(s.root(), key);
          cards.show(cardHolder, key);
          if (frame != null) frame.setTitle(s.title());
          s.onEnter();
      }
  }
  ```

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/scene/Scene.java src/main/java/ui/scene/Router.java
  git commit -m "2.3: Scene 接口 + Router 路由"
  ```

---

### Task 2.4: Theme + ThemeAware

**Files:**
- Create: `src/main/java/ui/Theme.java`
- Create: `src/main/java/ui/ThemeAware.java`

- [ ] **Step 1: 写 Theme**

  Write `src/main/java/ui/Theme.java`:
  ```java
  // 当前皮肤的 Tokens / BoardStyle / PieceStyle 快捷访问
  package ui;
  import ui.design.Skin;
  import ui.design.Tokens;
  import ui.design.BoardStyle;
  import ui.design.PieceStyle;

  public final class Theme {
      private static volatile Skin current;
      private Theme() {}
      public static void set(Skin s) { current = s; }
      public static Skin currentSkin() {
          if (current == null) throw new IllegalStateException("Theme.set 必须先调用");
          return current;
      }
      public static Tokens tokens() { return currentSkin().tokens; }
      public static BoardStyle board() { return currentSkin().board; }
      public static PieceStyle pieces() { return currentSkin().pieces; }
  }
  ```

  Write `src/main/java/ui/ThemeAware.java`:
  ```java
  // 响应皮肤切换的可选接口
  package ui;
  public interface ThemeAware { void onSkinChanged(); }
  ```

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/Theme.java src/main/java/ui/ThemeAware.java
  git commit -m "2.4: Theme 当前皮肤访问器 + ThemeAware 接口"
  ```

---

### Task 2.5-2.11: 组件库 G* (10 个组件)

由于 G* 组件每个都不长但数量多，按"创建 + 编译 + commit" 的批次合并。完整代码见 spec 第 2.3 节描述与各组件 paint 逻辑。

每个组件遵守 **统一约束**：
- 实现 `ThemeAware` 接口（如适用）
- 颜色 / 字体 / 间距 / 圆角全部读 `Theme.tokens()`，**禁止硬编码**
- 抗锯齿在 paint 时统一开启
- 文件保持 < 200 行；超过即拆

#### Task 2.5: GButton

**Files:** Create `src/main/java/ui/component/GButton.java`

- [ ] **Step 1: 写 GButton**（completion 指南：圆角填充按钮，三种 variant：PRIMARY/SECONDARY/GHOST。PRIMARY 用 `tokens.accent` 填充 + `tokens.fgOnAccent` 文字；SECONDARY 用 `tokens.bgCard` + `tokens.borderMedium` 描边 + `tokens.fgDefault` 文字；GHOST 透明 + `tokens.fgDefault`。Hover 状态 PRIMARY/SECONDARY 颜色 `darker()`。提供 `static GButton primary(String) / secondary(String) / ghost(String)` 工厂方法。）

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/GButton.java
  git commit -m "2.5: GButton 基础按钮组件（PRIMARY/SECONDARY/GHOST）"
  ```

#### Task 2.6: GCard

**Files:** Create `src/main/java/ui/component/GCard.java`

- [ ] **Step 1: 写 GCard**（继承 JPanel；圆角 `tokens.radiusLg`；填充 `tokens.bgCard`；描边 `tokens.borderLight`；可选 `elevated=true` 时画 3 层半透明 black 形成微阴影；BorderLayout + 内边距 `tokens.space4`。）

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/GCard.java
  git commit -m "2.6: GCard 卡片容器"
  ```

#### Task 2.7: GAvatar

**Files:** Create `src/main/java/ui/component/GAvatar.java`

- [ ] **Step 1: 写 GAvatar**（继承 JComponent；构造接受 `size, isBlack`，可调 `setImage(BufferedImage)` 切换为自定义图片；paint 用 `Ellipse2D` 做剪辑画圆；渐变球用 `Theme.pieces().blackHighlight/Base` 或 `whiteHighlight/Base`；最外圈 `Color(0,0,0,90)` 描边。）

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/GAvatar.java
  git commit -m "2.7: GAvatar 圆形头像组件"
  ```

#### Task 2.8: GChip

**Files:** Create `src/main/java/ui/component/GChip.java`

- [ ] **Step 1: 写 GChip**（继承 JLabel，圆胶囊背景，构造接受 `text, bg, fg`；padding `space1/space2`；用于段位徽章 / 价格标签 / 状态。）

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/GChip.java
  git commit -m "2.8: GChip 圆胶囊标签"
  ```

#### Task 2.9: GToast

**Files:** Create `src/main/java/ui/component/GToast.java`

- [ ] **Step 1: 写 GToast**（顶层 `JWindow` 透明背景 + 圆角填充矩形 + 文字标签；提供 `static info / success / error(parent, msg)` 三个静态入口；定位在 owner 顶部居中下方 64px；`Timer 2500ms` 后启动渐隐 `Timer 40ms` 步进降 alpha 0.08；alpha<=0 dispose。）

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/GToast.java
  git commit -m "2.9: GToast 浮动提示组件"
  ```

#### Task 2.10: GDialog

**Files:** Create `src/main/java/ui/component/GDialog.java`

- [ ] **Step 1: 写 GDialog**（继承 JDialog；构造接受 `Window owner, String title, boolean modal`；提供 `protected JPanel content`，自带 `space5` 边距 + `bgPrimary` 背景；`getRootPane().registerKeyboardAction` 绑 Esc 关闭。）

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/GDialog.java
  git commit -m "2.10: GDialog 圆角对话框基类"
  ```

#### Task 2.11: GTextField + GSlider + GTab + GBadge

**Files:**
- Create: `src/main/java/ui/component/GTextField.java`
- Create: `src/main/java/ui/component/GSlider.java`
- Create: `src/main/java/ui/component/GTab.java`
- Create: `src/main/java/ui/component/GBadge.java`

- [ ] **Step 1: GTextField**（继承 JTextField，圆角背景 + 描边 + placeholder 灰色文本；`setPlaceholder(String)` 控制提示文字。）

- [ ] **Step 2: GSlider**（继承 JSlider，仅设主题色与字号。）

- [ ] **Step 3: GTab**（横向 JLabel 列表，选中项底部 3px `tokens.accent` 高亮条；`onSelect(IntConsumer)` 注册回调；`select(int)` 程序切换。）

- [ ] **Step 4: GBadge**（圆形小红点 JLabel；`tokens.danger` 背景 + 白色数字。）

- [ ] **Step 5: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/GTextField.java \
          src/main/java/ui/component/GSlider.java \
          src/main/java/ui/component/GTab.java \
          src/main/java/ui/component/GBadge.java
  git commit -m "2.11: GTextField/GSlider/GTab/GBadge 四个基础组件"
  ```

---

### Task 2.12: GBoard 棋盘组件（核心）

**Files:** Create `src/main/java/ui/component/GBoard.java`

实现要点（详细约束）：
1. **动态尺寸**：每次 paint 调用 `computeLayout()` 按当前 `getWidth/getHeight` 算 `span/margin/originX/Y`，棋盘永远正方形居中。
2. **皮肤化**：背景渐变用 `Theme.board().bgTop/bgBottom`；线 `Theme.tokens().boardLine`；星位 `Theme.tokens().boardStar`；棋子用 `Theme.pieces()` 中的 4 色 + 渐变球。
3. **悬停预览**：鼠标移到空位 → 半透明己色棋（黑 alpha=90 / 白 alpha=130）；离开 / 落子后清除。
4. **最后一手呼吸圆环**：30 FPS Timer 改 `pulsePhase`，环 alpha 在 180-240 之间用正弦波动；半径 `r = diameter/2 + 3 + 3*sin(phase)`。
5. **木纹**：`Theme.board().drawWoodGrain == true` 时程序化画 7 像素一条横向半透明 `0x3E2723` 线，y 偏移按 sin 波动。
6. **接口**：`onClick(BiConsumer<Integer,Integer>)`、`placeChess(col, row, isBlack)`、`clear()`、`setStones(List<Stone>)`、`setInteractive(boolean)`、`setHoverIsBlack(boolean)`。
7. **数据**：内部维护 `List<Stone>`（包含 col/row/black 三字段静态内部类）。

- [ ] **Step 1: 写 GBoard**（按上述实现要点；约 200 行）

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/GBoard.java
  git commit -m "2.12: GBoard 棋盘组件（动态尺寸+悬停预览+呼吸光环+木纹）"
  ```

---

> **Phase 2 检查点**
>
> ```bash
> bash scripts/build.sh && bash scripts/run-tests.sh && bash scripts/run-unit-tests.sh
> ```
> Expected: Build OK / 36 E2E 全过 / EventBusTest 3/3 + ProfileTest 11/11


---

## Phase 3: SkinManager + 4 套皮肤

### Task 3.1: ClassicSkin（精修，B 温暖怀旧）

**Files:** Create `src/main/java/feature/skin/ClassicSkin.java`

实现要点：
- ID `"classic"`，displayName `"经典木纹"`，premium=false，priceCoins=0
- **颜色（B 温暖怀旧风格）**：
  - bgPrimary `#F4E4C1`（米黄）/ bgSecondary `#EDD9B0` / bgCard `#FAF6EE` / bgBoard `#D4A373` / bgToolbar `#E8D5B7`
  - fgDefault `#3A2418`（深棕） / fgMuted `#8A6D4F` / fgOnAccent `#FAF6EE`
  - accent `#5D4037`（棕） / accentDark `#3E2723` / danger `#A52A2A` / success `#558B2F` / warning `#E65100`
  - boardLine `#3E2723` / boardStar `#3E2723` / boardShadow `Color(0,0,0,80)` / lastMoveRing `#A52A2A`
  - borderLight `#D7C9B0` / borderMedium `#A88B6A`
- **字体（衬线）**：用 `Fonts.serif()` 选 STSong/SimSun/Georgia
  - fontDisplay 30 BOLD / fontTitle 20 BOLD / fontHeading 14 BOLD / fontBody 13 / fontCaption 11 / fontMono 13（用 Fonts.mono）
- **BoardStyle**：`bgTop=#E9C892, bgBottom=#C9A06A, drawWoodGrain=true, starDotSize=6, lineStrokeMul=1f/24f`
- **PieceStyle**：blackHighlight=`#888888`, blackBase=`#000000`, whiteHighlight=`#FFFFFF`, whiteBase=`#AAAAAA`, dropShadow=true
- 提供 `public static final Skin INSTANCE = build();`，build 函数用 Tokens.builder()

- [ ] **Step 1: 写 ClassicSkin** 按上述参数

- [ ] **Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/skin/ClassicSkin.java
  git commit -m "3.1: ClassicSkin 经典木纹皮肤（B 温暖怀旧·精修）"
  ```

### Task 3.2: 占位 3 套皮肤（DarkWood / Jade / Neon）

**Files:**
- Create: `src/main/java/feature/skin/DarkWoodSkin.java`
- Create: `src/main/java/feature/skin/JadeSkin.java`
- Create: `src/main/java/feature/skin/NeonSkin.java`

每套占位皮肤：复制 ClassicSkin 改 ID/displayName/priceCoins + 微调 1-2 个色板字段。**不追求精修**，主要是商店里有东西可点。

| 皮肤 | id | displayName | priceCoins | 关键差异 |
|---|---|---|---|---|
| DarkWoodSkin | darkwood | 深色木纹 | 800 | bgPrimary=#3E2A1B, bgBoard=#4A3520（暗木）, fgDefault=#F4E4C1 |
| JadeSkin | jade | 翡翠玉石 | 1500 | accent=#0F7B6C, blackBase=#1A4D3D（墨绿）, whiteBase=#E8F5E9 |
| NeonSkin | neon | 霓虹竞技 | 2500 | accent=#00FFAA, bgPrimary=#0F0F1E, boardLine=#E0D2B8 |

- [ ] **Step 1-3: 分别写 DarkWoodSkin/JadeSkin/NeonSkin**（参考 ClassicSkin 复制改）

- [ ] **Step 4: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/skin/DarkWoodSkin.java \
          src/main/java/feature/skin/JadeSkin.java \
          src/main/java/feature/skin/NeonSkin.java
  git commit -m "3.2: 3 套占位皮肤（DarkWood/Jade/Neon）"
  ```

### Task 3.3: SkinPack 皮肤集合

**Files:** Create `src/main/java/feature/skin/SkinPack.java`

```java
// 内置皮肤注册表
package feature.skin;

import java.util.List;
import ui.design.Skin;

public final class SkinPack {
    public static final List<Skin> ALL = List.of(
        ClassicSkin.INSTANCE,
        DarkWoodSkin.INSTANCE,
        JadeSkin.INSTANCE,
        NeonSkin.INSTANCE
    );
    public static Skin byId(String id) {
        for (Skin s : ALL) if (s.id.equals(id)) return s;
        return ClassicSkin.INSTANCE;
    }
    private SkinPack() {}
}
```

- [ ] **Step 1: 写 SkinPack + Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/skin/SkinPack.java
  git commit -m "3.3: SkinPack 皮肤注册表"
  ```

### Task 3.4: SkinManager（切换 + 持久化）

**Files:** Create `src/main/java/feature/skin/SkinManager.java`

```java
package feature.skin;

import java.util.HashSet;
import java.util.Set;
import profile.Profile;
import ui.EventBus;
import ui.Theme;
import ui.design.Skin;
import ui.events.SkinChangedEvent;

public final class SkinManager {
    private SkinManager() {}

    /** 启动时调用一次：从 profile 读 skinId 应用。 */
    public static void init() {
        Profile p = Profile.get();
        Skin s = SkinPack.byId(p.skinId);
        Theme.set(s);
    }

    public static Skin current() { return Theme.currentSkin(); }

    public static void setCurrent(String skinId) {
        Skin s = SkinPack.byId(skinId);
        Theme.set(s);
        Profile p = Profile.get();
        p.skinId = skinId;
        p.save();
        EventBus.publish(new SkinChangedEvent(s));
    }

    public static boolean isUnlocked(String skinId) {
        Profile p = Profile.get();
        Set<String> unlocked = parse(p.unlockedSkinIds);
        // 免费皮肤永远解锁
        Skin s = SkinPack.byId(skinId);
        if (!s.premium) return true;
        return unlocked.contains(skinId);
    }

    public static void unlock(String skinId) {
        Profile p = Profile.get();
        Set<String> set = parse(p.unlockedSkinIds);
        set.add(skinId);
        p.unlockedSkinIds = String.join(",", set);
        p.save();
    }

    private static Set<String> parse(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null) return out;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
```

- [ ] **Step 1: 写 SkinManager + Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/skin/SkinManager.java
  git commit -m "3.4: SkinManager 皮肤切换 + 持久化"
  ```

### Task 3.5: SkinManagerTest 单元测试

**Files:** Create `src/test/java/feature/skin/SkinManagerTest.java`

测试用例：
- `testDefaultSkinIsClassic` 初始化后 current().id == "classic"
- `testSetCurrentPersistsToProfile` setCurrent("darkwood") 后 Profile.get().skinId == "darkwood"
- `testSetCurrentPublishesEvent` 订阅 SkinChangedEvent → setCurrent → 收到 event
- `testFreeSkinAlwaysUnlocked` isUnlocked("classic") == true 即使 unlockedSkinIds 为空
- `testPremiumSkinUnlockFlow` 默认 isUnlocked("jade") == false → unlock("jade") → 再查 == true

注意：测试隔离用 `System.setProperty("user.home", tmp)` + `Profile.resetSingleton()`。

- [ ] **Step 1: 写测试 + Step 2: 跑测试**

  ```bash
  bash scripts/build.sh | tail -3
  bash scripts/run-unit-tests.sh 2>&1 | tail -15
  ```
  Expected: `结果: passed=5 failed=0`

- [ ] **Step 3: Commit**

  ```bash
  git add src/test/java/feature/skin/SkinManagerTest.java
  git commit -m "3.5: SkinManagerTest 单元测试（5 个用例）"
  ```

---

> **Phase 3 检查点**
>
> ```bash
> bash scripts/build.sh && bash scripts/run-tests.sh && bash scripts/run-unit-tests.sh
> ```
> Expected: 36 E2E + ProfileTest 11/11 + EventBusTest 3/3 + SkinManagerTest 5/5

---

## Phase 4: HomeScene + LobbyScene + 重写既有视图

### Task 4.1: app/Main.java 启动入口（替代 client.FiveClient.main）

**Files:** Create `src/main/java/app/Main.java`

```java
// 应用启动入口
package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatLightLaf;
import feature.skin.SkinManager;
import profile.Profile;
import ui.scene.Router;
import ui.scene.HomeScene;

public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();           // 给 JOptionPane / JMenu 等系统组件用
        Profile.get();                  // 加载档案
        SkinManager.init();             // 应用当前皮肤到 Theme

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Gomoku 五子棋");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Router router = new Router(frame);
            frame.setContentPane(router.cardHolder());
            router.push(new HomeScene(router));
            frame.setSize(1100, 760);
            frame.setMinimumSize(new java.awt.Dimension(820, 600));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
```

- [ ] **Step 1: 写 Main + Step 2: Commit**

  ```bash
  git add src/main/java/app/Main.java
  git commit -m "4.1: app/Main.java 启动入口"
  ```

  注：此时还未有 HomeScene，编译会报错。下一 task 创建 HomeScene 后即可。

### Task 4.2: HomeScene（欢迎屏）

**Files:** Create `src/main/java/ui/scene/HomeScene.java`

实现要点：
- 顶部品牌区：大 GAvatar(96, true) + "Gomoku 五子棋" Display 字号 + "经典策略对战 · 局域网联机 · 人机对战" 副标
- 主区两张大卡（GCard 自定义渐变背景）：
  - 左卡 "联机对战" `Theme.tokens().accent` 渐变 → 点击 push(LobbyEntryScene 或弹 DialogLogin)
  - 右卡 "人机对战" `Color(0x1976D2)` 渐变 → 点击 push(GameScene with AI)
- 底部 GhostButton 三个：[本地棋谱] [设置] [关于]
- 玩家档案行：`"欢迎回来！您已对局 N 次，<等级名>(Lv.X)"`，从 `Profile.get()` 读
- 实现 Scene 接口

- [ ] **Step 1: 写 HomeScene + Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/scene/HomeScene.java
  git commit -m "4.2: HomeScene 欢迎屏"
  ```

### Task 4.3: BrandHeader + StatusBar 全局组件

**Files:**
- Create: `src/main/java/ui/component/BrandHeader.java`
- Create: `src/main/java/ui/component/StatusBar.java`

**BrandHeader**：左 LOGO + Title，右 当前用户头像 + 名字。`setCurrentUser(name, BufferedImage)` 切换。

**StatusBar**：底部一条，左 ●连接状态 + 文字，右辅助信息。`setOnline/setOffline/setError`。

- [ ] **Step 1-2: 写 BrandHeader + StatusBar**

- [ ] **Step 3: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/BrandHeader.java src/main/java/ui/component/StatusBar.java
  git commit -m "4.3: BrandHeader + StatusBar 全局组件"
  ```

### Task 4.4: LobbyScene（大厅）

**Files:** Create `src/main/java/ui/scene/LobbyScene.java`

布局（BorderLayout）：
- NORTH: 我的资料 GCard（GAvatar(56) + 昵称 Display + 段位 GChip + 战绩文字 + [更换头像] GButton）
- CENTER: 玩家列表（JList 包 ListAdapter，Cell renderer 用 GAvatar + 昵称 + 段位）
- EAST: 快捷操作 GCard（GButton 列表：邀请选中 / 聊天 / 人机对战 / 云端棋谱 / 本地棋谱 / 退出登录）
- SOUTH: 消息区（沿用旧 PanelMessage 改名 ChatPanel，用 JTextPane + 时间戳 + 颜色）

- [ ] **Step 1: 写 LobbyScene + Step 2: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/scene/LobbyScene.java
  git commit -m "4.4: LobbyScene 大厅"
  ```

### Task 4.5: ChatPanel 重写消息聊天面板

**Files:** Create `src/main/java/ui/component/ChatPanel.java`

接口：
- `appendInfo(String)` 系统消息（灰）
- `appendOk(String)` 重要提示（绿加粗 + ✓ 前缀）
- `appendError(String)` 错误（红 + ⚠ 前缀）
- `appendChat(String name, String content, boolean self)` 聊天，自己绿加粗，他人蓝加粗
- `clear()`

实现：JTextPane + StyledDocument + SimpleDateFormat 时间戳 `[HH:mm:ss]` 前缀

- [ ] **Step 1-2: 写 ChatPanel + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/ChatPanel.java
  git commit -m "4.5: ChatPanel 彩色聊天面板（中文化+时间戳）"
  ```

### Task 4.6: 重写 DialogLogin（用 GDialog + GTextField + GButton）

**Files:** Modify `src/main/java/client/DialogLogin.java` → 迁移到 `src/main/java/ui/scene/dialog/LoginDialog.java`

- 用 GDialog 基类，GTextField 输入用户名/密码，GButton 登录/注册/取消
- 登录调 Communication.login，成功后 Router.replace(LobbyScene)
- 失败 GToast.error

- [ ] **Step 1-2: 写 LoginDialog + Compile + Commit**

  ```bash
  mkdir -p src/main/java/ui/scene/dialog
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/scene/dialog/LoginDialog.java
  git commit -m "4.6: LoginDialog 用 GDialog 重写"
  ```

---

> **Phase 4 检查点**：能从 Main 启动 → 看到 HomeScene → 点联机 → 弹 LoginDialog → 输入登录 → 进入 LobbyScene

---

## Phase 5: GameSession 抽象 + GameScene + 重做对局逻辑

### Task 5.1: GameSession 抽象接口

**Files:** Create `src/main/java/net/GameSession.java`

```java
// 对局会话抽象：联机和人机的统一接口
package net;

import java.util.function.Consumer;

public interface GameSession {
    /** 我是黑还是白：调用 start() 后由 server / 难度决定 */
    String myColor();          // "black" / "white"
    String opponentName();
    int    initialTimeSeconds();

    /** 启动会话（联机邀请 / AI 开局） */
    void start();

    /** 我方落子 */
    void makeMove(int col, int row);

    /** 我方认输 */
    void giveup();

    /** 我方判定五连，宣告胜利 */
    void claimWin();

    /** 离开会话（联机：发送 QUIT / AI：清理） */
    void leave();

    /** 注册回调：接收所有事件 */
    void subscribe(Consumer<GameSessionEvent> handler);

    enum EventType { MY_TURN, OPP_MOVE, RESULT_WIN, RESULT_LOSE, OPP_LEFT, CHAT }

    class GameSessionEvent {
        public final EventType type;
        public final int col, row;          // OPP_MOVE 用
        public final String chatSender, chatText; // CHAT 用
        public GameSessionEvent(EventType t) {
            this(t, 0, 0, null, null);
        }
        public GameSessionEvent(EventType t, int col, int row, String s, String tx) {
            this.type = t; this.col = col; this.row = row;
            this.chatSender = s; this.chatText = tx;
        }
    }
}
```

- [ ] **Step 1-2: 写接口 + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/net/GameSession.java
  git commit -m "5.1: GameSession 对局会话抽象"
  ```

### Task 5.2: 迁移 Communication.java 到 net 包

**Files:**
- Move: `src/main/java/client/Communication.java` → `src/main/java/net/Communication.java`
- 改 package 声明 `package net;`
- 删除对 FiveClient 的引用，改成依赖一个 `GameClient` 接口（接收消息回调）

```java
package net;
public interface GameClient {
    void onLoggedIn(String userName);
    void onDisconnected(String reason);
    void onUserList(String name, String state);   // ADD
    void onUserStateChange(String name, String state);   // CHANGE
    void onUserLeft(String name);                  // DELETE
    void onInvite(String fromName, int seconds);   // JOIN
    void onInviteRefused(String byName);           // REFUSE
    void onColor(String color, String oppName);    // GUESSCOLOR
    void onOpponentMove(int col, int row);         // GO
    void onResult(boolean youWon);                 // TELLRESULT
    void onChat(String sender, String content);    // TALK
    void onGameList(java.util.List<game.Game> games);  // GAME
    void onManual(game.Manual manual);             // MANUAL
}
```

Communication 改造：
- 构造接受 `GameClient client`，所有 dispatch 方法转为调 client.onXxx
- 不再依赖任何 ui / scene 类

- [ ] **Step 1: 移动文件**

  ```bash
  git mv src/main/java/client/Communication.java src/main/java/net/Communication.java
  ```

- [ ] **Step 2: 改 package + 引入 GameClient 接口（写 GameClient.java + 改造 Communication）**

- [ ] **Step 3: Compile + Commit**（client 包暂时引用 net.Communication）

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/net/Communication.java src/main/java/net/GameClient.java
  git commit -m "5.2: Communication 迁移 net 包 + 引入 GameClient 接口"
  ```

### Task 5.3: OnlineGameSession 实现

**Files:** Create `src/main/java/net/OnlineGameSession.java`

实现 GameSession，内部委托 Communication 发送命令：
- `makeMove(col, row)` → `comm.go(col, row)`
- `giveup()` → `comm.giveup()`
- `claimWin()` → `comm.wins()`
- 注册 GameClient 回调，把 onColor/onOpponentMove/onResult/onChat 转译为 GameSessionEvent 发给 subscriber

注意：`onColor` 触发 `MY_TURN` 或者 `OPP_MOVE`（取决于 myColor）

- [ ] **Step 1-2: 写 OnlineGameSession + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/net/OnlineGameSession.java
  git commit -m "5.3: OnlineGameSession 联机会话实现"
  ```

### Task 5.4: AiGameSession 实现

**Files:** Create `src/main/java/net/AiGameSession.java`

实现 GameSession，本地维护对局：
- 构造接受 `String myColor, int aiDepth, int aiSamples`
- `makeMove(col, row)` → 校验 → 内部记录 → 触发 OPP_MOVE 前先做 AI 思考（SwingWorker）→ 完成后回 OPP_MOVE
- `giveup()` → 直接发 RESULT_LOSE
- 内部状态：`int[][] board, int chessCount, List<int[]> moves`
- 复用 `ai.Evaluate`：每次 AI 思考时 `new Evaluate(snapshot, computerColor, depth, samples).getTheBestPosition()`
- 实现胜负检测（参考旧 AiBoard.isWin）

- [ ] **Step 1-2: 写 AiGameSession + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/net/AiGameSession.java
  git commit -m "5.4: AiGameSession 人机会话实现"
  ```

### Task 5.5: GameScene 视图

**Files:** Create `src/main/java/ui/scene/GameScene.java`

布局：
- CENTER: GBoard（主角）
- EAST GCard 列：
  - PlayerCard（对手）：GAvatar + 昵称 + 段位 + 计时器（大字）
  - PlayerCard（我方）：同上
  - 当前回合提示 GChip（"您的回合" / "对手回合"）
  - GButton: [认输] [发送消息] [表情] [返回]
- 构造接受 `GameSession session`
- 订阅 session 事件 → 更新 GBoard / 计时 / 弹结果
- session.RESULT_WIN/LOSE → 写 Profile 战绩 + 发 GameEndEvent → router.replace(ReviewScene)

- [ ] **Step 1-2: 写 GameScene + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/scene/GameScene.java
  git commit -m "5.5: GameScene 对局视图"
  ```

### Task 5.6: PlayerCard 组件

**Files:** Create `src/main/java/ui/component/PlayerCard.java`

GCard 子类：左 GAvatar(48) + 右垂直 (昵称, 段位 GChip, 计时器 mono 字体)，可设 `setActive(boolean)` 高亮当前回合方。

- [ ] **Step 1-2: 写 PlayerCard + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/PlayerCard.java
  git commit -m "5.6: PlayerCard 玩家信息卡组件"
  ```

### Task 5.7: 删除旧 client.PanelBoard / Chess / AiBoard / AiChess / AiGame

**Files:**
- Delete: `src/main/java/client/PanelBoard.java`, `Chess.java`, `PanelTiming.java`, `PanelControl.java`, `PanelMessage.java`, `PanelUserList.java`
- Delete: `src/main/java/ai/AiBoard.java`, `AiChess.java`, `AiGame.java`
- Delete: `src/main/java/client/FiveClient.java`（被 app.Main 替代）
- Delete: 其余旧 client/* dialog 等已被 ui.scene.dialog.* 替代的

注意：保留 `ai/Evaluate.java`（纯算法，被 AiGameSession 复用）。

- [ ] **Step 1: 删文件 + Step 2: Compile（确保没有编译错误，所有依赖已迁移）**

  ```bash
  git rm src/main/java/client/PanelBoard.java src/main/java/client/Chess.java \
         src/main/java/client/PanelTiming.java src/main/java/client/PanelControl.java \
         src/main/java/client/PanelMessage.java src/main/java/client/PanelUserList.java \
         src/main/java/ai/AiBoard.java src/main/java/ai/AiChess.java src/main/java/ai/AiGame.java \
         src/main/java/client/FiveClient.java
  bash scripts/build.sh | tail -3
  ```

  若编译失败，根据错误回退或修复引用。

- [ ] **Step 3: Commit**

  ```bash
  git commit -m "5.7: 删除旧 client UI 组件（被 ui.scene 全替代）"
  ```

---

> **Phase 5 检查点**：从 HomeScene 选"人机对战" → 进 GameScene → 落子 → AI 应招 → 胜负 → 进 ReviewScene（待 Phase 6）
> 此时 ReviewScene 还没写，可临时回退到 LobbyScene。

---

## Phase 6: ReviewScene + ReviewEngine + Sound + Rank

### Task 6.1: ReviewEngine 复盘算法

**Files:** Create `src/main/java/feature/review/ReviewEngine.java`

```java
package feature.review;

import ai.Evaluate;
import game.Manual;

public final class ReviewEngine {
    public interface ProgressCallback { void onProgress(int current, int total); }

    public static ReviewReport analyze(Manual m, int aiDepth, int aiSamples,
                                       int boardSize, ProgressCallback cb) {
        int total = m.getTotalStep();
        ReviewMove[] moves = new ReviewMove[total];
        int[][] board = new int[boardSize][boardSize];

        // 黑=1 白=2，模拟整局
        int blackMistakes = 0, whiteMistakes = 0;
        int blackBrilliant = 0, whiteBrilliant = 0;

        for (int i = 0; i < total; i++) {
            if (cb != null) cb.onProgress(i, total);
            int col = m.getChessList()[i][0];
            int row = m.getChessList()[i][1];
            int color = (i % 2 == 0) ? 1 : 2;  // 黑先

            // 评估"该手前"AI 推荐位
            int[] bestPos = new Evaluate(board, color, aiDepth, aiSamples).getTheBestPosition();
            // 实际落子分（模拟落下后局面分）
            board[col][row] = color;
            int evalAfter = quickScore(board, color, boardSize);
            // 推荐落子分（仅参考，不真落）
            int[][] sim = copy(board);
            sim[col][row] = 0;  // 撤回
            sim[bestPos[0]][bestPos[1]] = color;
            int evalBest = quickScore(sim, color, boardSize);

            int delta = Math.abs(evalBest - evalAfter);
            ReviewMove.Tag tag;
            boolean isBest = (col == bestPos[0] && row == bestPos[1]);
            if (isBest && (evalAfter > 5000)) tag = ReviewMove.Tag.BRILLIANT;
            else if (isBest)                  tag = ReviewMove.Tag.GOOD;
            else if (delta <= 300)            tag = ReviewMove.Tag.OK;
            else if (delta <= 1000)           tag = ReviewMove.Tag.INACCURATE;
            else if (delta <= 5000)           tag = ReviewMove.Tag.MISTAKE;
            else                              tag = ReviewMove.Tag.BLUNDER;

            if (color == 1 && tag == ReviewMove.Tag.MISTAKE || tag == ReviewMove.Tag.BLUNDER) blackMistakes++;
            if (color == 2 && (tag == ReviewMove.Tag.MISTAKE || tag == ReviewMove.Tag.BLUNDER)) whiteMistakes++;
            if (color == 1 && tag == ReviewMove.Tag.BRILLIANT) blackBrilliant++;
            if (color == 2 && tag == ReviewMove.Tag.BRILLIANT) whiteBrilliant++;

            moves[i] = new ReviewMove(col, row, evalBest, evalAfter, delta, tag, bestPos);
        }
        if (cb != null) cb.onProgress(total, total);
        return new ReviewReport(moves, blackMistakes, whiteMistakes, blackBrilliant, whiteBrilliant);
    }

    private static int quickScore(int[][] b, int color, int n) {
        // 复用 Evaluate.evaluateGame 的本地副本计算
        // 简化：用 new Evaluate(b, color, 0, 0) 的内部 evaluateGame 不直接暴露，需要 expose 一个方法
        // 临时方案：用 getTheBestPosition 的副作用？不可。
        // 推荐改 Evaluate 暴露 public int snapshotScore(int[][] board, int color)
        // 这里假设已扩展（见 Task 6.1.1）
        return new Evaluate(b, color, 0, 0).snapshotScore();
    }

    private static int[][] copy(int[][] b) {
        int[][] c = new int[b.length][];
        for (int i = 0; i < b.length; i++) c[i] = b[i].clone();
        return c;
    }
}
```

#### Task 6.1.1: 在 Evaluate 暴露 snapshotScore 方法

修改 `src/main/java/ai/Evaluate.java`：把私有 `evaluateGame()` 改为 `public int snapshotScore() { return evaluateGame(); }`，或者直接将 `evaluateGame` 改为 public。注意：还需要让 `searchDepth=0` 时 `getTheBestPosition` 不爆炸（简单返回中心点）。

- [ ] **Step 1: 修 Evaluate（小心保 36 E2E）**

- [ ] **Step 2: 写 ReviewEngine + ReviewMove + ReviewReport**

  Write `src/main/java/feature/review/ReviewMove.java`:
  ```java
  package feature.review;

  public final class ReviewMove {
      public enum Tag { BRILLIANT, GOOD, OK, INACCURATE, MISTAKE, BLUNDER }
      public final int col, row;
      public final int evalBest, evalActual, delta;
      public final Tag tag;
      public final int[] suggestedMove;
      public ReviewMove(int col, int row, int evalBest, int evalActual, int delta, Tag tag, int[] suggestedMove) {
          this.col = col; this.row = row; this.evalBest = evalBest; this.evalActual = evalActual;
          this.delta = delta; this.tag = tag; this.suggestedMove = suggestedMove;
      }
  }
  ```

  Write `src/main/java/feature/review/ReviewReport.java`:
  ```java
  package feature.review;

  public final class ReviewReport {
      public final ReviewMove[] moves;
      public final int blackMistakes, whiteMistakes;
      public final int blackBrilliant, whiteBrilliant;
      public ReviewReport(ReviewMove[] moves, int bm, int wm, int bb, int wb) {
          this.moves = moves; this.blackMistakes = bm; this.whiteMistakes = wm;
          this.blackBrilliant = bb; this.whiteBrilliant = wb;
      }
  }
  ```

- [ ] **Step 3: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/review src/main/java/ai/Evaluate.java
  git commit -m "6.1: ReviewEngine 复盘算法 + ReviewMove/Report"
  ```

### Task 6.2: ReviewEngineTest 单元测试

**Files:** Create `src/test/java/feature/review/ReviewEngineTest.java`

测试用例：
- 构造一个已知棋谱（黑直接 5 连）→ analyze → 检查最后一手 tag = GOOD/BRILLIANT
- 构造一个明显失误棋谱（黑可活四不下，下了无关位）→ 检查该手 tag = MISTAKE/BLUNDER

- [ ] **Step 1-2: 写测试 + 跑**

  Expected: `结果: passed=2 failed=0`

- [ ] **Step 3: Commit**

  ```bash
  git add src/test/java/feature/review/ReviewEngineTest.java
  git commit -m "6.2: ReviewEngineTest 复盘单元测试"
  ```

### Task 6.3: SoundManager + 6 个音效

**Files:**
- Create: `src/main/java/feature/sound/SoundManager.java`
- Create: `src/main/java/feature/sound/SoundId.java`
- Create: `src/main/resources/sounds/{place,win,lose,invite,emote,click}.wav`（音效文件占位 — 实际从 freesound.org 找 CC0 的）

```java
package feature.sound;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.*;

public final class SoundManager {
    private static final Map<SoundId, Clip> CLIPS = new EnumMap<>(SoundId.class);
    private static boolean enabled = true;
    private static float volume = 0.7f;

    private SoundManager() {}

    public static void preload() {
        for (SoundId id : SoundId.values()) {
            try (InputStream in = SoundManager.class.getResourceAsStream("/sounds/" + id.fileName)) {
                if (in == null) continue;
                AudioInputStream ais = AudioSystem.getAudioInputStream(
                        new java.io.BufferedInputStream(in));
                Clip c = AudioSystem.getClip();
                c.open(ais);
                CLIPS.put(id, c);
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
                // 静默跳过
            }
        }
    }

    public static void play(SoundId id) {
        if (!enabled) return;
        Clip c = CLIPS.get(id);
        if (c == null) return;
        try {
            c.setFramePosition(0);
            // 音量
            try {
                FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(20f * (float) Math.log10(volume));
            } catch (Exception ignored) {}
            c.start();
        } catch (Exception ignored) {}
    }

    public static void setEnabled(boolean v) { enabled = v; }
    public static void setVolume(int percent) { volume = Math.max(0.01f, Math.min(1f, percent / 100f)); }
}
```

```java
package feature.sound;
public enum SoundId {
    PLACE("place.wav"), WIN("win.wav"), LOSE("lose.wav"),
    INVITE("invite.wav"), EMOTE("emote.wav"), CLICK("click.wav");
    public final String fileName;
    SoundId(String f) { this.fileName = f; }
}
```

- [ ] **Step 1: 写两个类 + 资源占位（空 wav 文件占位也行，preload 失败静默）**
- [ ] **Step 2: 在 Main 启动加 `SoundManager.preload()` 调用**
- [ ] **Step 3: 订阅 EventBus 各事件触发 play**

  Write `src/main/java/feature/sound/SoundSubscriber.java`:
  ```java
  package feature.sound;
  import ui.EventBus;
  import ui.events.*;

  public final class SoundSubscriber {
      private SoundSubscriber() {}
      public static void install() {
          EventBus.subscribe(MoveEvent.class, e -> SoundManager.play(SoundId.PLACE));
          EventBus.subscribe(GameEndEvent.class,
                  e -> SoundManager.play(e.youWon ? SoundId.WIN : SoundId.LOSE));
          EventBus.subscribe(RankUpEvent.class, e -> SoundManager.play(SoundId.WIN));
      }
  }
  ```

  在 Main 启动时调 `SoundSubscriber.install()`

- [ ] **Step 4: Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  mkdir -p src/main/resources/sounds
  # 占位空文件：touch src/main/resources/sounds/{place,win,lose,invite,emote,click}.wav
  git add src/main/java/feature/sound src/main/resources/sounds src/main/java/app/Main.java
  git commit -m "6.3: SoundManager + 6 SoundId + EventBus 订阅"
  ```

### Task 6.4: RankCalculator + Tier + GameType

**Files:**
- Create: `src/main/java/feature/rank/Tier.java`
- Create: `src/main/java/feature/rank/Rank.java`
- Create: `src/main/java/feature/rank/RankCalculator.java`

```java
package feature.rank;
public enum Tier {
    BRONZE("青铜",   0,    1000),
    SILVER("白银",   1000, 1300),
    GOLD("黄金",     1300, 1600),
    PLATINUM("铂金", 1600, 1900),
    DIAMOND("钻石",  1900, 2200),
    MASTER("大师",   2200, Integer.MAX_VALUE);
    public final String displayName;
    public final int min, max;
    Tier(String n, int min, int max) { this.displayName = n; this.min = min; this.max = max; }
    public static Tier of(int elo) {
        for (Tier t : values()) if (elo >= t.min && elo < t.max) return t;
        return BRONZE;
    }
}
```

```java
package feature.rank;
public final class Rank {
    public final int elo;
    public final Tier tier;
    public final int divisionPoints;  // 0-100
    public Rank(int elo) {
        this.elo = elo;
        this.tier = Tier.of(elo);
        if (tier.max == Integer.MAX_VALUE) divisionPoints = 100;
        else divisionPoints = (elo - tier.min) * 100 / (tier.max - tier.min);
    }
}
```

```java
package feature.rank;
public final class RankCalculator {
    public enum GameType { ONLINE, AI_EASY, AI_NORMAL, AI_HARD }

    public static int calcDelta(int myElo, GameType type, boolean won) {
        int oppElo;
        switch (type) {
            case AI_EASY:   oppElo = 800;  break;
            case AI_NORMAL: oppElo = 1200; break;
            case AI_HARD:   oppElo = 1600; break;
            default:        oppElo = 1200; break;
        }
        int K = (type == GameType.ONLINE) ? 32 : 16;  // AI 模式打折
        double expected = 1.0 / (1.0 + Math.pow(10, (oppElo - myElo) / 400.0));
        double actual = won ? 1.0 : 0.0;
        return (int) Math.round(K * (actual - expected));
    }
}
```

- [ ] **Step 1-3: 写 3 个文件 + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/rank
  git commit -m "6.4: RankCalculator + Tier + Rank"
  ```

### Task 6.5: RankCalculatorTest 单元测试

**Files:** Create `src/test/java/feature/rank/RankCalculatorTest.java`

测试用例：
- ELO=1200 联机胜 1200 对手 → delta = 16 (50% 期望)
- ELO=1200 联机负 1200 对手 → delta = -16
- ELO=1200 AI_EASY 胜 → K=16 + 大概率 → delta 约 +1
- ELO=1200 AI_HARD 胜 → K=16 + 小概率 → delta 约 +14
- Tier.of(0)==BRONZE / Tier.of(1300)==GOLD / Tier.of(2500)==MASTER

- [ ] **Step 1-3: 写 + 跑 + Commit**

  ```bash
  bash scripts/run-unit-tests.sh 2>&1 | tail -10
  git add src/test/java/feature/rank/RankCalculatorTest.java
  git commit -m "6.5: RankCalculatorTest 单元测试"
  ```

### Task 6.6: RankSubscriber 订阅 GameEndEvent 写战绩

**Files:** Create `src/main/java/feature/rank/RankSubscriber.java`

```java
package feature.rank;

import profile.Profile;
import ui.EventBus;
import ui.events.GameEndEvent;
import ui.events.GameStartEvent;
import ui.events.RankUpEvent;

public final class RankSubscriber {
    private RankSubscriber() {}
    public static void install() {
        EventBus.subscribe(GameEndEvent.class, e -> {
            Profile p = Profile.get();
            Tier oldTier = Tier.of(p.elo);
            RankCalculator.GameType type;
            if (e.mode == GameStartEvent.Mode.ONLINE) type = RankCalculator.GameType.ONLINE;
            else if (e.aiDifficultyElo <= 800) type = RankCalculator.GameType.AI_EASY;
            else if (e.aiDifficultyElo >= 1600) type = RankCalculator.GameType.AI_HARD;
            else type = RankCalculator.GameType.AI_NORMAL;
            int delta = RankCalculator.calcDelta(p.elo, type, e.youWon);
            p.elo += delta;
            if (p.elo > p.peakElo) p.peakElo = p.elo;
            if (e.youWon) {
                if (e.mode == GameStartEvent.Mode.ONLINE) p.onlineWins++;
                else p.aiWins++;
                p.weeklyWins++;
            } else {
                if (e.mode == GameStartEvent.Mode.ONLINE) p.onlineLosses++;
                else p.aiLosses++;
                p.weeklyLosses++;
            }
            p.save();
            Tier newTier = Tier.of(p.elo);
            if (newTier != oldTier && p.elo > oldTier.max) {
                EventBus.publish(new RankUpEvent(oldTier.displayName, newTier.displayName));
            }
        });
    }
}
```

在 Main 启动加 `RankSubscriber.install()`。

- [ ] **Step 1-3: 写 + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/rank/RankSubscriber.java src/main/java/app/Main.java
  git commit -m "6.6: RankSubscriber 订阅对局结束写战绩"
  ```

### Task 6.7: ReviewScene 复盘视图

**Files:** Create `src/main/java/ui/scene/ReviewScene.java`

布局：
- CENTER: GBoard（不可交互）+ GSlider 控制当前手
- EAST GCard:
  - 顶部摘要："共 N 手 | 黑失误 X | 白失误 Y | 黑妙手 A | 白妙手 B"
  - 评分曲线图（自定义 JComponent 画折线，X=手数 Y=分数，黑红双色）
  - 手列表 JList（每行显示手数 + tag GChip + 坐标）
- 底部 [导出 GIF] [返回大厅] GButton
- 构造接受 `Manual manual`
- onEnter 启 SwingWorker 跑 ReviewEngine.analyze，进度条显示在曲线图位置

- [ ] **Step 1-2: 写 ReviewScene + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/scene/ReviewScene.java
  git commit -m "6.7: ReviewScene 复盘视图"
  ```

### Task 6.8: ScoreChart 评分曲线组件

**Files:** Create `src/main/java/ui/component/ScoreChart.java`

JComponent 子类：
- `setData(int[] blackScores, int[] whiteScores)` 设数据
- paintComponent 画 X/Y 轴 + 黑/红双折线 + 标题"评分曲线"
- 当数据为空时显示"复盘进行中..."

- [ ] **Step 1-2: 写 + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/ScoreChart.java
  git commit -m "6.8: ScoreChart 评分曲线组件"
  ```

---

> **Phase 6 检查点**：AI 对局结束 → 进 ReviewScene → 看到评分曲线 + 手列表 + 段位变化 GToast

---

## Phase 7: ShopScene + 金币/签到 + Emote + GIF

### Task 7.1: CoinManager 金币 + 签到

**Files:** Create `src/main/java/feature/shop/CoinManager.java`

```java
package feature.shop;

import java.text.SimpleDateFormat;
import java.util.Date;
import profile.Profile;
import ui.EventBus;
import ui.events.CoinsChangedEvent;

public final class CoinManager {
    private CoinManager() {}

    public static int balance() { return Profile.get().coins; }

    public static void add(int delta, String reason) {
        Profile p = Profile.get();
        p.coins += delta;
        p.save();
        EventBus.publish(new CoinsChangedEvent(p.coins, delta, reason));
    }

    public static boolean spend(int amount, String reason) {
        Profile p = Profile.get();
        if (p.coins < amount) return false;
        p.coins -= amount;
        p.save();
        EventBus.publish(new CoinsChangedEvent(p.coins, -amount, reason));
        return true;
    }

    /** 启动时检查每日签到，自动签到 */
    public static CheckinResult checkinIfDue() {
        Profile p = Profile.get();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        String today = fmt.format(new Date());
        String last = p.lastCheckinDate == null ? "" : p.lastCheckinDate;
        if (today.equals(last)) {
            return new CheckinResult(false, p.consecutiveCheckinDays, 0);
        }
        // 检查是不是连续
        long now = System.currentTimeMillis();
        long oneDay = 24 * 60 * 60 * 1000L;
        boolean consecutive;
        try {
            long lastMs = last.isEmpty() ? 0 : fmt.parse(last).getTime();
            consecutive = (now - lastMs) <= oneDay * 2;  // 容忍 24h 误差
        } catch (Exception e) { consecutive = false; }
        if (consecutive && !last.isEmpty()) p.consecutiveCheckinDays++;
        else p.consecutiveCheckinDays = 1;
        p.lastCheckinDate = today;
        int reward = 10;
        if (p.consecutiveCheckinDays % 7 == 0) reward = 50;
        if (p.consecutiveCheckinDays % 14 == 0) reward = 100;
        p.save();
        add(reward, "每日签到（连续 " + p.consecutiveCheckinDays + " 天）");
        return new CheckinResult(true, p.consecutiveCheckinDays, reward);
    }

    public static final class CheckinResult {
        public final boolean newToday;
        public final int consecutiveDays;
        public final int rewardCoins;
        public CheckinResult(boolean n, int d, int r) {
            newToday = n; consecutiveDays = d; rewardCoins = r;
        }
    }
}
```

- [ ] **Step 1-2: 写 + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/shop/CoinManager.java
  git commit -m "7.1: CoinManager 金币 + 签到"
  ```

### Task 7.2: CoinEarnSubscriber 胜利赚金币

**Files:** Create `src/main/java/feature/shop/CoinEarnSubscriber.java`

```java
package feature.shop;
import ui.EventBus;
import ui.events.GameEndEvent;
import ui.events.GameStartEvent;

public final class CoinEarnSubscriber {
    private CoinEarnSubscriber() {}
    public static void install() {
        EventBus.subscribe(GameEndEvent.class, e -> {
            if (!e.youWon) return;
            int reward = (e.mode == GameStartEvent.Mode.ONLINE) ? 10 : 5;
            CoinManager.add(reward, e.mode == GameStartEvent.Mode.ONLINE ? "联机胜利" : "AI 胜利");
        });
    }
}
```

在 Main 启动加 `CoinEarnSubscriber.install()` + `CoinManager.checkinIfDue()`（含 GToast 提示签到结果）。

- [ ] **Step 1-2: 写 + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/shop/CoinEarnSubscriber.java src/main/java/app/Main.java
  git commit -m "7.2: CoinEarnSubscriber 胜利赚金币 + 启动签到"
  ```

### Task 7.3: ShopScene 视图

**Files:** Create `src/main/java/ui/scene/ShopScene.java`

布局：
- NORTH GCard 金币条：💰 当前 X 金币 | 连续签到 N 天 | （刷新按钮）
- CENTER GTab 三选一：[皮肤] [道具] [会员]
  - 皮肤 tab：4 张 GCard 卡（每张：mini 棋盘预览 + 名字 + 价格 GChip + 状态按钮[使用中/已拥有/购买]）
  - 道具 tab：占位 4 张"敬请期待"
  - 会员 tab：占位"敬请期待"
- 订阅 CoinsChangedEvent 实时刷新金币条
- 点击购买 → 弹 PurchaseDialog

- [ ] **Step 1-2: 写 + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/scene/ShopScene.java
  git commit -m "7.3: ShopScene 商店视图"
  ```

### Task 7.4: PurchaseDialog 购买对话框

**Files:** Create `src/main/java/ui/scene/dialog/PurchaseDialog.java`

GDialog 子类：
- 内容：皮肤大图（mini 棋盘预览）+ 名字 + 描述 + 价格
- 三个按钮：
  - [使用 X 金币购买] 主色，金币不够时禁用
  - [跳过支付直接试用] 次色（占位变现钩子）
  - [取消] ghost
- 任何"购买成功"路径：CoinManager.spend → SkinManager.unlock → SkinManager.setCurrent → GToast.success → dispose

- [ ] **Step 1-2: 写 + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/scene/dialog/PurchaseDialog.java
  git commit -m "7.4: PurchaseDialog 购买对话框"
  ```

### Task 7.5: Emote + EmoteParser

**Files:**
- Create: `src/main/java/feature/emote/Emote.java`
- Create: `src/main/java/feature/emote/EmotePack.java`
- Create: `src/main/java/feature/emote/EmoteParser.java`

```java
package feature.emote;
public final class Emote {
    public final String code, text;
    public Emote(String code, String text) { this.code = code; this.text = text; }
}
```

```java
package feature.emote;
import java.util.Arrays;
import java.util.List;

public final class EmotePack {
    public static final List<Emote> DEFAULT = Arrays.asList(
        new Emote("good",    "妙手！"),
        new Emote("nice",    "好棋"),
        new Emote("oops",    "失误了"),
        new Emote("again",   "再来一局？"),
        new Emote("gg",      "认输，承让"),
        new Emote("hi",      "你好"),
        new Emote("waiting", "等等..."),
        new Emote("thanks",  "感谢")
    );
    private EmotePack() {}
}
```

```java
package feature.emote;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmoteParser {
    private static final Pattern P = Pattern.compile("^\\[emote:([a-z]+)\\](.*)$");
    private EmoteParser() {}
    public static Optional<Emote> parse(String text) {
        if (text == null) return Optional.empty();
        Matcher m = P.matcher(text);
        if (!m.matches()) return Optional.empty();
        String code = m.group(1);
        for (Emote e : EmotePack.DEFAULT) if (e.code.equals(code)) return Optional.of(e);
        return Optional.empty();
    }
    public static String wrap(Emote e) { return "[emote:" + e.code + "]" + e.text; }
}
```

- [ ] **Step 1-3: 写 + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/emote
  git commit -m "7.5: Emote + EmotePack + EmoteParser"
  ```

### Task 7.6: EmoteParserTest 单元测试

**Files:** Create `src/test/java/feature/emote/EmoteParserTest.java`

测试用例：
- `parse("[emote:good]妙手！")` → present, code=="good"
- `parse("普通文字")` → empty
- `parse("[emote:unknown]xxx")` → empty
- `wrap(EmotePack.DEFAULT.get(0))` → "[emote:good]妙手！"

- [ ] **Step 1-3: 写 + 跑 + Commit**

  ```bash
  bash scripts/run-unit-tests.sh 2>&1 | tail -10
  git add src/test/java/feature/emote/EmoteParserTest.java
  git commit -m "7.6: EmoteParserTest 单元测试"
  ```

### Task 7.7: EmoteBar + 集成 GameScene/ChatPanel

**Files:**
- Create: `src/main/java/ui/component/EmoteBar.java`：8 个 GButton 网格，点击触发 `Consumer<Emote>`
- Modify: `src/main/java/ui/scene/GameScene.java`：右下角 [表情] GButton → 点击 popup EmoteBar → 选中调 `session.sendChat(EmoteParser.wrap(emote))`
- Modify: `src/main/java/ui/component/ChatPanel.java`：appendChat 时先 `EmoteParser.parse(content)`，是 emote 则用 GChip 渲染

注意：`session.sendChat` 需要在 GameSession 接口加方法（OnlineGameSession 走 Communication.talk，AiGameSession no-op）

- [ ] **Step 1: 在 GameSession 加 `void sendChat(String content)`**
- [ ] **Step 2-3: 写 EmoteBar + 改 GameScene/ChatPanel + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/ui/component/EmoteBar.java \
          src/main/java/ui/scene/GameScene.java \
          src/main/java/ui/component/ChatPanel.java \
          src/main/java/net/GameSession.java \
          src/main/java/net/OnlineGameSession.java \
          src/main/java/net/AiGameSession.java
  git commit -m "7.7: EmoteBar 表情快捷面板 + 集成 GameScene/ChatPanel"
  ```

### Task 7.8: GifExporter 棋谱 GIF 导出

**Files:**
- Create: `src/main/java/feature/share/ExportOpts.java`
- Create: `src/main/java/feature/share/GifExporter.java`

```java
package feature.share;
public final class ExportOpts {
    public String skinId = "classic";
    public int frameDelayMs = 800;
    public int scale = 600;
    public boolean includeMoveLabels = false;
    public int maxFrames = 100;  // 风险缓解：限制
}
```

```java
package feature.share;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import config.AppConfig;
import feature.skin.SkinPack;
import game.Manual;
import ui.Theme;
import ui.component.GBoard;
import ui.design.Skin;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class GifExporter {
    public interface ProgressCallback { void onProgress(int current, int total); }

    public static File export(Manual m, ExportOpts opts, ProgressCallback cb) throws Exception {
        // 切换 Theme 到 opts.skinId（不持久化，只渲染用）
        Skin saved = Theme.currentSkin();
        Theme.set(SkinPack.byId(opts.skinId));
        try {
            int total = Math.min(m.getTotalStep(), opts.maxFrames);
            File dir = new File(AppConfig.get().userDataDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            SimpleDateFormat ts = new SimpleDateFormat("yyyyMMdd-HHmmss");
            File out = new File(dir, m.getbUser() + "_vs_" + m.getwUer() + "_" + ts.format(new Date()) + ".gif");
            AnimatedGifEncoder enc = new AnimatedGifEncoder();
            enc.setRepeat(0);
            enc.setDelay(opts.frameDelayMs);
            enc.start(out.getAbsolutePath());

            // 离屏 GBoard
            GBoard board = new GBoard();
            board.setInteractive(false);
            board.setSize(opts.scale, opts.scale);
            board.doLayout();
            for (int i = 0; i < total; i++) {
                board.clear();
                for (int j = 0; j <= i; j++) {
                    int col = m.getChessList()[j][0];
                    int row = m.getChessList()[j][1];
                    boolean black = (j % 2 == 0);
                    if (col >= 0 && col <= board.cols() && row >= 0 && row <= board.rows()) {
                        board.placeChess(col, row, black);
                    }
                }
                BufferedImage img = new BufferedImage(opts.scale, opts.scale, BufferedImage.TYPE_INT_RGB);
                board.paint(img.getGraphics());
                enc.addFrame(img);
                if (cb != null) cb.onProgress(i + 1, total);
            }
            enc.finish();
            return out;
        } finally {
            Theme.set(saved);
        }
    }
}
```

- [ ] **Step 1-2: 写 + Compile + Commit**

  ```bash
  bash scripts/build.sh | tail -3
  git add src/main/java/feature/share
  git commit -m "7.8: GifExporter 棋谱 GIF 导出"
  ```

### Task 7.9: GifExporterTest + ReviewScene 集成"导出 GIF"

**Files:**
- Create: `src/test/java/feature/share/GifExporterTest.java`
- Modify: `src/main/java/ui/scene/ReviewScene.java`

测试：
- 构造 5 手 Manual → export → 验证文件存在 + 字节数 > 100 + 前 6 字节是 "GIF89a"

ReviewScene 集成：[导出 GIF] GButton → 弹小对话框选 skin/速度 → SwingWorker 跑 → 完成 GToast.success "已导出 → 打开文件夹"

- [ ] **Step 1-3: 写测试 + 改 ReviewScene + Commit**

  ```bash
  bash scripts/run-unit-tests.sh 2>&1 | tail -10
  git add src/test/java/feature/share/GifExporterTest.java src/main/java/ui/scene/ReviewScene.java
  git commit -m "7.9: GifExporterTest + ReviewScene 集成导出 GIF"
  ```

---

> **Phase 7 检查点**：商店能用，签到生效，胜利涨金币，对局中能发表情，复盘能导 GIF

---

## Phase 8: 错误收口 + 文案 + 音效素材

### Task 8.1: 全工程替换 JOptionPane 系统消息为 GToast

**Files:** Modify all `*.java` 中调用 `JOptionPane.showMessageDialog` 用于"非阻塞提示"的位置

**保留 JOptionPane（强确认）：** 认输确认 / 清空棋谱 / 购买确认 / 登录失败（用 ERROR_MESSAGE）
**改为 GToast（一次性提示）：** 头像更新成功 / 签到成功 / 文件导出成功 / 主题切换成功 / 错误提示（非关键）

逐文件 grep 找 + 改：

```bash
grep -rn "JOptionPane.showMessageDialog" src/main/java/
```

按列表逐处判断后改为：
- 信息：`GToast.info(parent, "...")`
- 成功：`GToast.success(parent, "...")`
- 错误（非关键）：`GToast.error(parent, "...")`

- [ ] **Step 1: grep 列出所有点 + Step 2: 逐处改 + Step 3: Compile + Commit**

  ```bash
  grep -rn "JOptionPane.showMessageDialog" src/main/java/ | tee /tmp/joptionpane.txt
  # 编辑各文件
  bash scripts/build.sh | tail -3
  git add -u
  git commit -m "8.1: 非阻塞提示统一为 GToast"
  ```

### Task 8.2: SettingsScene 设置面板

**Files:** Create `src/main/java/ui/scene/SettingsScene.java`

内容：
- 主题：[当前皮肤名] 按钮 → 跳 ShopScene
- 音效：开关 GButton + GSlider 音量 0-100
- 字体：下拉选择（系统默认 / 微软雅黑 / 思源黑体 / 楷体）
- AI 默认难度：[简单] [标准] [困难] 单选
- 邀请用时默认值：GTextField (秒)
- 数据：[导出档案] [清空本地数据] GButton
- [关于] [新手引导] [游戏规则]

- [ ] **Step 1-2: 写 + Commit**

  ```bash
  git add src/main/java/ui/scene/SettingsScene.java
  git commit -m "8.2: SettingsScene 设置面板"
  ```

### Task 8.3: 音效素材落地

**Files:** Replace `src/main/resources/sounds/{place,win,lose,invite,emote,click}.wav` 为真实音效

来源：freesound.org / opengameart.org，CC0 license。
- place.wav：木质落子声，0.2s
- win.wav：胜利提示音，0.8s
- lose.wav：失败提示音，0.5s
- invite.wav：通知"叮"，0.4s
- emote.wav：轻盈"哒"，0.15s
- click.wav：按钮点击，0.05s

每个文件 < 50KB。

- [ ] **Step 1: 下载并替换 + Step 2: 验证音效播放 + Step 3: Commit**

  ```bash
  ls -la src/main/resources/sounds/
  git add src/main/resources/sounds
  git commit -m "8.3: 音效素材落地（CC0）"
  ```

### Task 8.4: 文案 / 引导刷新

**Files:**
- Modify: `src/main/java/ui/scene/dialog/AboutDialog.java`：版本号 "1.0.0" + GitHub 链接 + 依赖致谢（FlatLaf, MySQL Connector/J, animated-gif-lib）
- Modify: `src/main/java/client/OnboardingDialog.java`：3 步引导（迁移到 ui.scene.dialog.OnboardingDialog 并刷新文案）
- 全工程 grep "TODO" / "FIXME" / 中英文混杂残留

- [ ] **Step 1-3: 文案润色 + Compile + Commit**

  ```bash
  grep -rn "TODO\|FIXME" src/main/java/ | tee /tmp/leftovers.txt
  # 逐处处理
  git add -u
  git commit -m "8.4: 文案润色 + AboutDialog 致谢 + OnboardingDialog 重写"
  ```

---

## Phase 9: 测试 + 重打包 + 文档

### Task 9.1: 跑全套测试 + 修补回归

```bash
taskkill //F //IM java.exe 2>&1 | head -1
bash scripts/build.sh | tail -3
java -cp "target/classes;lib/*" server.FiveServer > /tmp/srv.log 2>&1 &
sleep 3
bash scripts/run-tests.sh 2>&1 | grep -E "结果:|All tests|FAILED"
bash scripts/run-unit-tests.sh 2>&1 | grep -E "结果:|FAIL"
taskkill //F //IM java.exe 2>&1 | head -1
```

期望：
- E2E：`passed=28 failed=0` + `passed=8 failed=0` + `All tests passed.`
- 单元测试：每个测试类 `结果: passed=N failed=0`

如有 fail：定位到 task 修补 → 重跑 → commit `9.1: 修补 <X> 回归`

- [ ] **Step 1-3: 跑测试 + 修补 + Commit**

### Task 9.2: 手工冒烟检查表

**Files:** Create `docs/testing-checklist.md`

```markdown
# 手工冒烟测试清单（阶段一）

## 启动
- [ ] 第一次启动弹新手引导（3 步可跳过）
- [ ] 第二次启动不弹引导
- [ ] 启动时若新一天则自动签到 + GToast

## HomeScene
- [ ] LOGO 大字标题正常显示
- [ ] "联机对战"卡片渐变绿背景，可点击
- [ ] "人机对战"卡片渐变蓝背景，可点击
- [ ] 玩家档案行显示 nickname / 等级 / 总对局数
- [ ] 底部 [本地棋谱] [设置] [关于] 三个 ghost 按钮可点

## 联机流程
- [ ] 点 [联机对战] → 弹 LoginDialog
- [ ] 输错密码 → ERROR JOptionPane（不可关）
- [ ] 输对密码 → 进 LobbyScene
- [ ] 大厅看到自己 + 别的玩家
- [ ] 双击玩家 → 弹用时输入 → 邀请发出
- [ ] 对方接受 → 双方进 GameScene
- [ ] 双方落子 → 棋盘同步，落子声响
- [ ] 一方五连 → 双方收到结果 → 段位变化（GToast）→ 进 ReviewScene
- [ ] 复盘评分曲线显示 + 手列表 tag 颜色正确
- [ ] [导出 GIF] → 完成 GToast → 文件夹有 .gif

## 人机流程
- [ ] HomeScene 点 [人机对战] → 进 GameScene（AI 难度选择对话框）
- [ ] 各种难度都能开局
- [ ] 落子 → 1-3 秒后 AI 应招（带思考延时）
- [ ] AI 胜 / 我胜 → 战绩卡（替换为 ResultCard 或直接进 ReviewScene 都行）
- [ ] 段位升级 → GToast 动画 + 音效

## 皮肤
- [ ] 设置 → 主题 → 跳 ShopScene
- [ ] ShopScene 4 张皮肤卡片显示
- [ ] [使用中] 状态正确显示在当前皮肤
- [ ] 点付费皮肤 → PurchaseDialog
- [ ] [跳过支付直接试用] → 解锁 + 即时切换 + 全窗口刷新
- [ ] 主题色 / 棋盘 / 棋子 全部跟随皮肤变化

## 表情/聊天
- [ ] GameScene 右下角 [表情] → 弹 8 个快捷短语
- [ ] 选 → 对方收到 → 在 ChatPanel 显示为 GChip 而非纯文本
- [ ] 自由文字消息正常显示

## 签到 / 金币
- [ ] 启动 GToast 显示签到奖励
- [ ] ShopScene 顶部金币条正确
- [ ] 联机胜利 → +10 金币 + GToast
- [ ] AI 胜利 → +5 金币 + GToast
- [ ] 购买皮肤 → 扣金币 → CoinsChangedEvent 触发金币条刷新

## 错误处理
- [ ] 服务器关掉 → 红色 StatusBar + 自动回 HomeScene
- [ ] 重新启动服务器 → 重登 → 正常

## 性能
- [ ] 棋盘动画流畅（呼吸光环 30FPS 不卡）
- [ ] 复盘进度条平稳
- [ ] GIF 导出 30 手 < 10 秒
```

- [ ] **Step 1: 写清单 + Step 2: 提交**

  ```bash
  git add docs/testing-checklist.md
  git commit -m "9.2: 手工冒烟测试清单"
  ```

### Task 9.3: 重打包 + 验证 .exe

```bash
chmod -R +w target/installer 2>/dev/null
rm -rf target/installer 2>/dev/null
bash scripts/package-installer.sh 2>&1 | tail -10
ls -la target/installer/

# 验证两个 .exe 启动
"target/installer/Gomoku-Server/Gomoku-Server.exe" > /tmp/srv-pkg.log 2>&1 &
sleep 3
"target/installer/Gomoku-Client/Gomoku-Client.exe" > /tmp/cli-pkg.log 2>&1 &
sleep 4
tasklist | grep Gomoku
# 手工跑一遍 README 中的核心场景
taskkill //F //IM Gomoku-Server.exe
taskkill //F //IM Gomoku-Client.exe
```

- [ ] **Step 1-3: 重打 + 启动验证 + Commit**

  ```bash
  git add -u
  git commit -m "9.3: 重打包 + 启动验证（jar/exe/zip）"
  ```

### Task 9.4: 更新 README + ARCHITECTURE 文档

**Files:**
- Modify: `README.md`：新增"产品功能"章节，列出 7 大模块
- Modify: `docs/ARCHITECTURE.md`：新章节"重塑后架构（阶段一）"，描述 ui/feature/net 包结构 + EventBus + Theme/Skin
- Create: `docs/superpowers/plans/2026-04-30-gomoku-product-redesign-stage1-summary.md`：总结实施过程、未完成项、阶段二预告

- [ ] **Step 1-3: 写文档 + Commit + push 分支**

  ```bash
  git add README.md docs/ARCHITECTURE.md docs/superpowers/plans/
  git commit -m "9.4: 文档更新（README + ARCHITECTURE + summary）"
  git push -u origin redesign-stage1   # 推到远程作为里程碑
  ```

---

> **Phase 9 检查点**：分支 `redesign-stage1` 推送成功；安装包可分发；冒烟全过；文档同步。
> **阶段一结束。** 下一步：用户体验后决定是否进阶段二（联机房间/观战/真天梯）。

---

## Self-Review

按 writing-plans 要求做本计划的自检：

### 1. Spec 覆盖度

逐项核对 spec 节 3 的 7 个 feature：

| Spec 节 | 对应 Plan Task | 状态 |
|---|---|---|
| 3.1 皮肤系统 | Phase 3 全部 (3.1-3.5) + Phase 7 (7.3 ShopScene, 7.4 PurchaseDialog) | ✓ |
| 3.2 AI 复盘 | Phase 6 (6.1 ReviewEngine, 6.7 ReviewScene, 6.8 ScoreChart) | ✓ |
| 3.3 音效系统 | Phase 6 (6.3 SoundManager + SoundSubscriber) + Phase 8 (8.3 素材) | ✓ |
| 3.4 段位系统 | Phase 6 (6.4 RankCalculator, 6.6 RankSubscriber) | ✓ |
| 3.5 聊天表情 | Phase 7 (7.5-7.7 Emote/Pack/Parser/Bar/集成) | ✓ |
| 3.6 GIF 分享 | Phase 7 (7.8-7.9 GifExporter) | ✓ |
| 3.7 商店 + 金币 + 签到 | Phase 7 (7.1-7.4 CoinManager + 订阅 + ShopScene + PurchaseDialog) | ✓ |

| Spec 节 4 数据流 | Plan 中实现位置 | 状态 |
|---|---|---|
| A. 启动流 | Phase 4 Task 4.1 Main + Phase 6 SoundSubscriber + Phase 7 CoinEarn/Checkin | ✓ |
| B. 切换皮肤 | Phase 3 SkinManager.setCurrent | ✓ |
| C. 一手棋（联机）| Phase 5 OnlineGameSession + GameScene | ✓ |
| D. 对局结束 → 复盘 | Phase 5 GameScene RESULT + Phase 6 ReviewSubscriber + ReviewScene | ✓ |
| E. 表情发送 | Phase 7 (7.7 EmoteBar 集成) | ✓ |
| F. GIF 导出 | Phase 7 (7.9) | ✓ |
| G. 金币与签到 | Phase 7 (7.1, 7.2) | ✓ |

### 2. Placeholder 扫描

✓ 没有 "TBD" / "TODO" / "implement later"
✓ 关键算法（ReviewEngine/RankCalculator/CoinManager）有完整代码
⚠ 部分组件（GButton/GCard/GAvatar/GChip/GToast/GDialog/GTextField/GSlider/GTab/GBadge）在 Task 2.5-2.11 给出"实现要点"指南而非完整代码——理由是这些是相对模板化的 paint 代码，10 个组件每个都贴 100 行会让 plan 失控（plan 已 2700+ 行）。每个组件的关键约束（用什么 token / 圆角 / 状态）已写明。

### 3. 类型一致性

- `Skin` 数据类签名 `(id, displayName, premium, priceCoins, tokens, board, pieces)` —— Phase 1.4 定义、Phase 3.x 使用、Phase 7.4 PurchaseDialog 读取，一致。
- `GameSession.GameSessionEvent` 包含 `EventType + col + row + chatSender + chatText` —— Phase 5.1 定义、5.3/5.4 实现、5.5 GameScene 订阅，一致。
- `RankCalculator.GameType` 枚举 ONLINE/AI_EASY/AI_NORMAL/AI_HARD —— Phase 6.4 定义、6.6 RankSubscriber 使用，一致。

### 4. 范围检查

✓ 7 个 feature 全部对应单一 Phase 7（Emote+GIF+Coin/Shop）但 Sound/Rank/Review 在 Phase 6（按 spec 时间预算合并），合理。
✓ 阶段二（房间制/观战/真天梯）明确不在本 plan 范围。

---

## Execution Handoff

**计划完成并已保存到 `docs/superpowers/plans/2026-04-30-gomoku-product-redesign-stage1.md`。**

执行选项：

**1. Subagent-Driven（推荐）** — 我每个 Task 派一个 fresh subagent，两阶段评审。优点：context 不污染、失败影响小。

**2. Inline Execution** — 在本会话直接执行，按 batch checkpoint。优点：你能随时打断 / 调整。

考虑到本计划包含 60+ Task，**强烈推荐 Subagent-Driven**（避免 context 爆炸）。

哪个？
