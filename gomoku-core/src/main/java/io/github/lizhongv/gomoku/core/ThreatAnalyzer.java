package io.github.lizhongv.gomoku.core;

/**
 * 单点威胁分析：把某点周围四个方向的棋型合并成一个可比较的威胁值。
 *
 * <p>搜索中的走法排序与叶子评估都基于这里的分析。除了四个方向各自的分值，还对组合威胁额外加分： 双四、四三、双活三在三层搜索内通常已经决定胜负，不额外加分会被对手的单一活四盖过。
 *
 * <p>结果只反映「在该点落子后」的局部形态，不判断全局胜负；胜负由 {@link WinDetector} 单独判定。
 */
public final class ThreatAnalyzer {

  private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

  private static final long DOUBLE_FOUR_BONUS = 500_000L;

  private static final long FOUR_THREE_BONUS = 300_000L;

  private static final long DOUBLE_THREE_BONUS = 150_000L;

  private ThreatAnalyzer() {}

  /**
   * 分析某点的威胁。
   *
   * <p>该点可以是空格（预判落子后的形态），也可以已经是同色棋子（分析已落下的这手棋）， 后者用于禁手判定。
   *
   * @return 四方向中最好的棋型、四的数量、活三的数量与合计分值；点位不合法时返回 {@link Threat#NONE}
   */
  public static Threat analyze(Board board, Move move, Stone stone) {
    if (stone == null || stone == Stone.EMPTY || !board.inBounds(move.row(), move.col())) {
      return Threat.NONE;
    }
    Stone occupant = board.get(move);
    if (occupant != Stone.EMPTY && occupant != stone) {
      return Threat.NONE;
    }
    int size = board.size();
    Pattern best = Pattern.NONE;
    int fours = 0;
    int openThrees = 0;
    long score = 0;
    byte[] window = new byte[PatternTable.WINDOW];
    for (int[] direction : DIRECTIONS) {
      fillWindow(board, move, stone, direction[0], direction[1], size, window);
      Pattern pattern = PatternTable.classify(window);
      if (pattern.ordinal() < best.ordinal()) {
        best = pattern;
      }
      if (pattern.isFour()) {
        fours++;
      }
      if (pattern == Pattern.OPEN_THREE) {
        openThrees++;
      }
      score += pattern.score();
    }
    if (fours >= 2) {
      score += DOUBLE_FOUR_BONUS;
    } else if (fours >= 1 && openThrees >= 1) {
      score += FOUR_THREE_BONUS;
    } else if (openThrees >= 2) {
      score += DOUBLE_THREE_BONUS;
    }
    return new Threat(best, fours, openThrees, score);
  }

  /** 某点在指定颜色下的分值，便于评估函数直接比较双方。 */
  public static long score(Board board, Move move, Stone stone) {
    return analyze(board, move, stone).score();
  }

  private static void fillWindow(
      Board board, Move move, Stone stone, int dr, int dc, int size, byte[] window) {
    for (int offset = -PatternTable.CENTER; offset <= PatternTable.CENTER; offset++) {
      int index = offset + PatternTable.CENTER;
      if (offset == 0) {
        window[index] = 1;
        continue;
      }
      int row = move.row() + dr * offset;
      int col = move.col() + dc * offset;
      if (row < 0 || row >= size || col < 0 || col >= size) {
        // 边界按阻挡处理：贴边的三、四无法向界外延伸
        window[index] = 2;
        continue;
      }
      Stone other = board.get(row, col);
      if (other == Stone.EMPTY) {
        window[index] = 0;
      } else if (other == stone) {
        window[index] = 1;
      } else {
        window[index] = 2;
      }
    }
  }

  /** 一个点的威胁快照。 */
  public record Threat(Pattern best, int fours, int openThrees, long score) {

    /** 无威胁。 */
    public static final Threat NONE = new Threat(Pattern.NONE, 0, 0, 0);

    /** 是否包含至少一个四。 */
    public boolean hasFour() {
      return fours > 0;
    }

    /** 是否为双四或四三这类必胜组合。 */
    public boolean isWinningCombination() {
      return fours >= 2 || (fours >= 1 && openThrees >= 1);
    }
  }
}
