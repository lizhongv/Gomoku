# 开发指南

> 适用版本：v2.0.0（开发中）

## 一、环境

| 组件 | 版本 |
| --- | --- |
| JDK | 17 及以上（CI 覆盖 17、21） |
| Maven | 由 `./mvnw` 自动下载（3.9.9） |
| 操作系统 | Windows / Linux / macOS |

```bash
git clone https://github.com/lizhongv/Gomoku.git
cd Gomoku
./mvnw verify
```

Windows 下使用 `mvnw.cmd` 或 Git Bash 中的 `./mvnw`。

## 二、常用命令

| 命令 | 作用 |
| --- | --- |
| `./mvnw verify` | 编译、Spotless、Checkstyle、SpotBugs、测试、覆盖率 |
| `./mvnw -pl gomoku-core test` | 只跑规则内核测试 |
| `./mvnw -pl gomoku-ai test` | 只跑 AI 测试 |
| `./mvnw spotless:apply` | 按 google-java-format 自动格式化 |
| `./mvnw -pl gomoku-legacy -am package` | 打包迁移期 Swing 客户端 |
| `./scripts/run-desktop.sh` | 启动 JavaFX 客户端 |
| `./scripts/screenshots.sh` | 生成客户端截图到 `docs/images/desktop/` |
| `java -jar gomoku-legacy/target/gomoku-legacy.jar` | 启动旧 Swing 客户端 |
| `java -cp "gomoku-core/target/classes;gomoku-ai/target/classes" io.github.lizhongv.gomoku.ai.bench.BenchmarkMain 20` | 运行 AI 基准（Windows 分隔符 `;`） |

## 三、模块约定

- `gomoku-core`：零第三方依赖；只放规则、棋盘、棋型与胜负，不放 UI、网络、配置。
- `gomoku-ai`：只依赖 core 与 SLF4J；搜索不阻塞调用线程之外的资源；一个引擎实例只在一个线程使用。
- `gomoku-legacy`：迁移期保留，只做必要的编译修复，不再加新功能；阶段三删除。
- 新增模块必须继承父 POM，并声明依赖方向；禁止反向依赖（core 不得依赖 ai）。

## 四、代码规范

- 使用 google-java-format（Spotless 自动执行），行宽 100。
- 注释与 Javadoc 用中文，写「为什么」：取值理由、边界条件、踩过的坑。
- 公共类与公共方法必须有 Javadoc；内部实现用行内注释解释非直观逻辑。
- 常量集中定义，不在逻辑中散落魔法数字；评测相关阈值放入 `SearchLimits` 或 `Pattern`。
- 异常只捕获可处理的类型；不得吞异常；搜索超时使用内部单例异常，避免重复创建。
- 线程安全：core 与 ai 默认非线程安全；共享实例必须在文档中说明同步方式。

## 五、测试要求

- 新功能必须附 JUnit 5 测试；修 bug 先写复现用例。
- 搜索相关改动必须跑战术题库与自对弈实验，确认没有回归。
- 超时、并发、边界（空盘、满盘、单点、禁手）必须有测试。
- `gomoku-core` 行覆盖率低于 80% 时构建失败。

## 六、提交与发布

- 约定式提交：`feat:`、`fix:`、`docs:`、`test:`、`refactor:`、`perf:`、`ci:`、`chore:`。
- 一个 PR 只做一件事；提交信息写清「为什么」。
- 版本语义化：破坏性改动升主版本；新增功能升次版本；修复升修订号。
- 发版步骤：
  1. 更新 `pom.xml` 的 `<version>` 与 `CHANGELOG.md`；
  2. `./mvnw verify` 全绿；
  3. 打 tag `vX.Y.Z` 并推送；
  4. GitHub Actions 构建并上传安装包（阶段三启用）。
- 公开仓库使用匿名邮箱提交：`49021250+lizhongv@users.noreply.github.com`。

## 七、阶段计划

| 阶段 | 内容 | 验收 |
| --- | --- | --- |
| 一（已完成） | 多模块工程、规则内核、搜索引擎、基准、CI、文档 | `./mvnw verify` 全绿；战术题库 50/50 |
| 二（基础版完成） | JavaFX 客户端：首页、对局、复盘、设置、主题、i18n | 可完整对局与复盘；截图已生成；残局与联机待后续 |
| 三 | 服务端权威棋局、协议 v2、房间匹配、jpackage 安装包 | 联机对局与断线重连通过；安装包可安装运行 |
