# 新旧引擎对照

> 运行时间：2026-11-01。环境：Windows 11、JDK 17、单线程。
> 对手：v1 `gomoku-legacy` 的 `ai.Evaluate`，固定深度 3、每层 10 个候选（旧版本默认配置）。
> 规则：15×15 无禁手，双方各执黑一半，直到五连或棋盘下满。

## 结果

| v2 档位 | 时间预算 | 局数 | v2 胜 | v1 胜 | 和 | v2 胜率 | 总耗时 |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 5 | 300 ms | 20 | 10 | 10 | 0 | 50% | 23 s |
| 8 | 1200 ms | 20 | 19 | 1 | 0 | 95% | 415 s |

复现命令：

```bash
./mvnw -q -pl gomoku-benchmark -am compile
java -cp "gomoku-core/target/classes:gomoku-ai/target/classes:gomoku-legacy/target/classes:gomoku-benchmark/target/classes" \
  io.github.lizhongv.gomoku.bench.VersusMain 20 8
```

## 解读

- 5 档（300 ms）与旧引擎基本持平，说明旧版的深度 3 启发式并不是随手可胜的对手；
  仅把搜索换成迭代加深、置换表与 VCF，而不给足时间，收益有限。
- 8 档（1200 ms）胜率 95%，差距来自更深的搜索与 VCF 的连杀能力；
  20 局样本量有限，95% 是点估计，真实差距需要更多对局与不同开局。
- 结论只适用于本机与本规则；对外不宣称职业水平，见 [../COMPETITORS.md](../COMPETITORS.md) 的说明。
