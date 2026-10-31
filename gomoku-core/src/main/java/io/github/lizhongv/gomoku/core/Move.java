package io.github.lizhongv.gomoku.core;

/**
 * 棋盘坐标，行列均从 0 开始。
 *
 * <p>这是一个值对象：搜索中会大量创建，保持不可变，避免别名修改。
 */
public record Move(int row, int col) {

  /** 按棋盘边长把一个线性下标还原为坐标。 */
  public static Move ofIndex(int index, int size) {
    return new Move(index / size, index % size);
  }

  /** 转成线性下标，供数组访问。 */
  public int index(int size) {
    return row * size + col;
  }

  /** 与另一点的切比雪夫距离，用于邻域候选判断。 */
  public int distanceTo(Move other) {
    return Math.max(Math.abs(row - other.row), Math.abs(col - other.col));
  }
}
