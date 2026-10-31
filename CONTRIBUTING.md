# 参与贡献

## 提交 Issue

- **缺陷**：写明版本、复现步骤、期望结果与实际结果；涉及 AI 的请附棋盘局面（坐标列表或 SGF）。
- **功能建议**：写明要解决的问题与使用场景，而不只是方案。
- **安全问题**不要公开提 Issue，见 [SECURITY.md](SECURITY.md)。

## 开发环境

```bash
git clone https://github.com/lizhongv/Gomoku.git
cd Gomoku
./mvnw verify          # 编译、格式检查、静态检查、测试、覆盖率
```

要求 JDK 17 及以上；首次运行会自动下载 Maven 与依赖。

## 提交 Pull Request

1. 从 `main` 新建分支：`feat/ai-vct`、`fix/server-reconnect`。
2. 一个 PR 只做一件事；新增行为附测试。
3. 提交前本地通过：

   ```bash
   ./mvnw spotless:apply
   ./mvnw verify
   ```

4. 在 `CHANGELOG.md` 的「开发中」下记录改动。
5. CI 全部通过后请求评审。

## 代码约定

- 使用约定式提交：`feat:`、`fix:`、`docs:`、`test:`、`refactor:`、`perf:`、`ci:`、`chore:`。
- 注释与文档用中文，写「为什么这么做」，不复述代码。
- `gomoku-core` 不依赖任何第三方库；`gomoku-ai` 只依赖 core 与日志接口。
- 规则内核是唯一真相源：界面、AI、服务端都通过 `GameState` 判定胜负，不得各写一套。
- 新增棋盘规则必须补对应单元测试，并在 `docs/RULES.md` 说明。
- 不提交任何密钥、数据库密码与用户数据。
