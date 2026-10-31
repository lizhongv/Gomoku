package io.github.lizhongv.gomoku.core;

/**
 * 单方向棋型。枚举顺序即强弱顺序，可直接比较 {@link #ordinal()}。
 *
 * <p>分值是搜索与评估使用的相对权重，不是概率；五连远高于其他，保证搜索优先选择成五与防五。
 */
public enum Pattern {
  /** 五连及以上。 */
  FIVE(10_000_000L),
  /** 活四：两端都空，下一手成五有两个点，无法防守。 */
  OPEN_FOUR(1_000_000L),
  /** 冲四：只有一个成五点。 */
  FOUR(100_000L),
  /** 活三：再走一手可成活四。 */
  OPEN_THREE(50_000L),
  /** 眠三：再走一手只能成冲四。 */
  THREE(5_000L),
  /** 活二：再走一手可成活三。 */
  OPEN_TWO(500L),
  /** 眠二。 */
  TWO(50L),
  /** 无威胁。 */
  NONE(0L);

  private final long score;

  Pattern(long score) {
    this.score = score;
  }

  /** 该棋型的评估分值。 */
  public long score() {
    return score;
  }

  /** 是否为四类（活四或冲四）。 */
  public boolean isFour() {
    return this == OPEN_FOUR || this == FOUR;
  }

  /** 是否为三及以上的进攻棋型。 */
  public boolean isThreeOrBetter() {
    return this == FIVE || this == OPEN_FOUR || this == FOUR || this == OPEN_THREE || this == THREE;
  }
}
