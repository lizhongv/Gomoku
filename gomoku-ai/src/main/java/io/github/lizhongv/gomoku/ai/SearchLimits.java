package io.github.lizhongv.gomoku.ai;

/**
 * 一次搜索的限制条件。
 *
 * @param timeMillis 时间预算，软上限：迭代加深在超时后丢弃未完成的这一层
 * @param maxDepth 最大搜索深度（层数，含根节点）
 * @param branching 每个节点展开的候选数
 * @param randomness 低难度用的随机度：0 表示总选最优，越大越容易选次优
 * @param vcf 是否在根节点先做 VCF（连续冲四）必胜搜索
 */
public record SearchLimits(
    long timeMillis, int maxDepth, int branching, double randomness, boolean vcf) {

  /** 校验参数，避免出现 0 时间或负深度导致搜索行为异常。 */
  public SearchLimits {
    if (timeMillis <= 0) {
      throw new IllegalArgumentException("时间预算须大于 0");
    }
    if (maxDepth < 1) {
      throw new IllegalArgumentException("搜索深度至少为 1");
    }
    if (branching < 1) {
      throw new IllegalArgumentException("候选数至少为 1");
    }
    randomness = Math.max(0, Math.min(1, randomness));
  }

  /** 12 档难度：1 档适合新手，12 档为最高强度。 */
  public static SearchLimits forLevel(int level) {
    if (level < 1 || level > 12) {
      throw new IllegalArgumentException("难度须在 1 到 12 之间：" + level);
    }
    return switch (level) {
      case 1 -> new SearchLimits(50, 2, 6, 0.80, false);
      case 2 -> new SearchLimits(80, 2, 6, 0.65, false);
      case 3 -> new SearchLimits(120, 2, 8, 0.50, false);
      case 4 -> new SearchLimits(200, 4, 8, 0.35, false);
      case 5 -> new SearchLimits(300, 4, 10, 0.22, false);
      case 6 -> new SearchLimits(500, 4, 12, 0.12, true);
      case 7 -> new SearchLimits(800, 6, 12, 0.06, true);
      case 8 -> new SearchLimits(1200, 6, 14, 0.00, true);
      case 9 -> new SearchLimits(1800, 8, 16, 0.00, true);
      case 10 -> new SearchLimits(2500, 10, 18, 0.00, true);
      case 11 -> new SearchLimits(3500, 12, 20, 0.00, true);
      default -> new SearchLimits(5000, 16, 24, 0.00, true);
    };
  }

  /** 基准测试等场景使用的快速配置。 */
  public static SearchLimits fast() {
    return new SearchLimits(200, 6, 12, 0, true);
  }
}
