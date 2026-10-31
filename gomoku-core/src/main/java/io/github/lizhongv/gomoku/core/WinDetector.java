package io.github.lizhongv.gomoku.core;

/**
 * 胜负判定。
 *
 * <p>无禁手规则下五连及以上均胜；连珠规则下黑棋恰好五连才胜，长连属于禁手，白棋五连及以上胜。
 */
public final class WinDetector {

  private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

  private WinDetector() {}

  /** 该点落子后是否已获胜（按规则区分长连）。 */
  public static boolean isWin(Board board, Move move, Stone stone, RuleSet ruleSet) {
    if (stone == Stone.EMPTY) {
      return false;
    }
    int longest = longestLine(board, move, stone);
    if (ruleSet == RuleSet.RENJU && stone == Stone.BLACK) {
      // 黑棋长连是禁手，不判胜；恰好五连才是胜
      return longest == 5;
    }
    return longest >= 5;
  }

  /** 四方向中最长的连子数。 */
  public static int longestLine(Board board, Move move, Stone stone) {
    int longest = 0;
    for (int[] direction : DIRECTIONS) {
      longest = Math.max(longest, lineLength(board, move, stone, direction[0], direction[1]));
    }
    return longest;
  }

  /** 某方向经过该点的连子数（包含该点本身）。 */
  public static int lineLength(Board board, Move move, Stone stone, int dr, int dc) {
    int count = 1;
    count += walk(board, move, stone, dr, dc);
    count += walk(board, move, stone, -dr, -dc);
    return count;
  }

  /** 沿某方向数连续同色棋子，遇到边界或异色停止。 */
  public static int walk(Board board, Move move, Stone stone, int dr, int dc) {
    int count = 0;
    int row = move.row() + dr;
    int col = move.col() + dc;
    while (board.inBounds(row, col) && board.get(row, col) == stone) {
      count++;
      row += dr;
      col += dc;
    }
    return count;
  }
}
