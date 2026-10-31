package io.github.lizhongv.gomoku.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 可变棋盘：落子、撤销、Zobrist 哈希与邻域候选。
 *
 * <p>搜索采用「落子-递归-撤销」的原地修改方式，避免每层复制棋盘。为快速得到候选点， 每个空格维护一个「附近 2 格内的棋子数」，落子与撤销时增量更新；撤销时对恢复的空格重新计数，
 * 避免长时间对局后计数漂移。
 *
 * <p>非线程安全：一个实例只在一个搜索线程内使用。
 */
public final class Board {

  /** 支持的最小与最大边长；比赛用 15 与 19 均已覆盖。 */
  public static final int MIN_SIZE = 5;

  public static final int MAX_SIZE = 25;

  /** 候选点邻域半径：距任一棋子 2 格以内。 */
  public static final int NEIGHBOR_RADIUS = 2;

  private final int size;

  private final byte[] cells;

  private final byte[] neighborCount;

  private final long[][] zobrist;

  private final int[] history;

  private final byte[] historyStones;

  private int moveCount;

  private long hash;

  /** 创建空棋盘。 */
  public Board(int size) {
    if (size < MIN_SIZE || size > MAX_SIZE) {
      throw new IllegalArgumentException("棋盘边长须在 " + MIN_SIZE + " 到 " + MAX_SIZE + " 之间：" + size);
    }
    this.size = size;
    this.cells = new byte[size * size];
    this.neighborCount = new byte[size * size];
    this.zobrist = Zobrist.table(size);
    this.history = new int[size * size];
    this.historyStones = new byte[size * size];
  }

  /** 棋盘边长。 */
  public int size() {
    return size;
  }

  /** 已落子数。 */
  public int moveCount() {
    return moveCount;
  }

  /** 当前局面哈希（只含棋子，不含行棋方）。 */
  public long hash() {
    return hash;
  }

  /** 最近一次落子；空棋盘返回 null。 */
  public Move lastMove() {
    return moveCount == 0 ? null : Move.ofIndex(history[moveCount - 1], size);
  }

  /** 含行棋方的哈希，用于置换表。 */
  public long hash(Stone side) {
    return side == Stone.WHITE ? hash ^ Zobrist.sideKey() : hash;
  }

  /** 坐标是否在棋盘内。 */
  public boolean inBounds(int row, int col) {
    return row >= 0 && row < size && col >= 0 && col < size;
  }

  /** 读取某格棋子。 */
  public Stone get(int row, int col) {
    return inBounds(row, col) ? Stone.of(cells[row * size + col]) : Stone.EMPTY;
  }

  /** 读取某格棋子。 */
  public Stone get(Move move) {
    return get(move.row(), move.col());
  }

  /** 是否空格。 */
  public boolean isEmpty(int row, int col) {
    return inBounds(row, col) && cells[row * size + col] == 0;
  }

  /** 棋盘是否已满。 */
  public boolean isFull() {
    return moveCount == size * size;
  }

  /**
   * 落子。
   *
   * @return 是否成功；坐标越界、格子非空或颜色为空时返回 false，棋盘保持不变
   */
  public boolean place(Move move, Stone stone) {
    if (stone == null || stone == Stone.EMPTY || !inBounds(move.row(), move.col())) {
      return false;
    }
    int index = move.index(size);
    if (cells[index] != 0) {
      return false;
    }
    cells[index] = (byte) stone.code();
    hash ^= zobrist[stone.code() - 1][index];
    history[moveCount] = index;
    historyStones[moveCount] = (byte) stone.code();
    moveCount++;
    bumpNeighbors(move.row(), move.col(), 1);
    return true;
  }

  /** 撤销最近一次落子并返回该点；没有落子时抛异常。 */
  public Move undo() {
    if (moveCount == 0) {
      throw new IllegalStateException("棋盘上没有可撤销的落子");
    }
    moveCount--;
    int index = history[moveCount];
    byte code = historyStones[moveCount];
    cells[index] = 0;
    hash ^= zobrist[code - 1][index];
    Move move = Move.ofIndex(index, size);
    bumpNeighbors(move.row(), move.col(), -1);
    // 恢复的空格此前被占用，邻域计数可能漏掉占用期间落在附近的棋子，重新计算一次
    neighborCount[index] = (byte) countNeighbors(move.row(), move.col());
    return move;
  }

  /** 复制当前局面，供自对弈或界面预览使用。 */
  public Board copy() {
    Board copy = new Board(size);
    System.arraycopy(cells, 0, copy.cells, 0, cells.length);
    System.arraycopy(history, 0, copy.history, 0, history.length);
    System.arraycopy(historyStones, 0, copy.historyStones, 0, historyStones.length);
    System.arraycopy(neighborCount, 0, copy.neighborCount, 0, neighborCount.length);
    copy.moveCount = moveCount;
    copy.hash = hash;
    return copy;
  }

  /** 邻域候选点：空格且 2 格内已有棋子。顺序为行列升序，便于测试复现。 */
  public List<Move> candidates() {
    List<Move> result = new ArrayList<>(64);
    for (int index = 0; index < cells.length; index++) {
      if (cells[index] == 0 && neighborCount[index] > 0) {
        result.add(Move.ofIndex(index, size));
      }
    }
    return result;
  }

  /** 该空格是否在候选范围内。 */
  public boolean isCandidate(Move move) {
    return isEmpty(move.row(), move.col()) && neighborCount[move.index(size)] > 0;
  }

  /** 距某点 2 格内是否有棋子。 */
  public boolean hasNeighbor(int row, int col, int radius) {
    for (int dr = -radius; dr <= radius; dr++) {
      for (int dc = -radius; dc <= radius; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = row + dr;
        int c = col + dc;
        if (inBounds(r, c) && cells[r * size + c] != 0) {
          return true;
        }
      }
    }
    return false;
  }

  private void bumpNeighbors(int row, int col, int delta) {
    for (int dr = -NEIGHBOR_RADIUS; dr <= NEIGHBOR_RADIUS; dr++) {
      for (int dc = -NEIGHBOR_RADIUS; dc <= NEIGHBOR_RADIUS; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = row + dr;
        int c = col + dc;
        if (inBounds(r, c)) {
          int index = r * size + c;
          if (cells[index] == 0) {
            neighborCount[index] = (byte) (neighborCount[index] + delta);
          }
        }
      }
    }
  }

  private int countNeighbors(int row, int col) {
    int count = 0;
    for (int dr = -NEIGHBOR_RADIUS; dr <= NEIGHBOR_RADIUS; dr++) {
      for (int dc = -NEIGHBOR_RADIUS; dc <= NEIGHBOR_RADIUS; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int r = row + dr;
        int c = col + dc;
        if (inBounds(r, c) && cells[r * size + c] != 0) {
          count++;
        }
      }
    }
    return count;
  }
}
