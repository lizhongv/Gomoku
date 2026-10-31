package io.github.lizhongv.gomoku.core;

/** 棋局结果。 */
public enum GameResult {
  /** 进行中。 */
  ONGOING,
  /** 黑胜。 */
  BLACK_WIN,
  /** 白胜。 */
  WHITE_WIN,
  /** 和棋（棋盘下满）。 */
  DRAW;

  /** 是否已结束。 */
  public boolean isFinished() {
    return this != ONGOING;
  }

  /** 获胜方；进行中或和棋返回 {@link Stone#EMPTY}。 */
  public Stone winner() {
    return switch (this) {
      case BLACK_WIN -> Stone.BLACK;
      case WHITE_WIN -> Stone.WHITE;
      default -> Stone.EMPTY;
    };
  }
}
