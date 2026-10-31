package io.github.lizhongv.gomoku.ai;

/**
 * 置换表：按 Zobrist 哈希缓存搜索结果，减少重复局面的搜索。
 *
 * <p>固定大小、总是替换：下棋类搜索的节点数以指数增长，用深度优先替换在同等内存下表现更好。 表按线程独占使用，不做同步。
 */
final class TranspositionTable {

  static final int EXACT = 0;

  static final int LOWER = 1;

  static final int UPPER = 2;

  private final long[] keys;

  private final int[] scores;

  private final int[] moves;

  private final byte[] depths;

  private final byte[] flags;

  private final int mask;

  TranspositionTable(int bits) {
    int size = 1 << bits;
    this.keys = new long[size];
    this.scores = new int[size];
    this.moves = new int[size];
    this.depths = new byte[size];
    this.flags = new byte[size];
    this.mask = size - 1;
  }

  /** 查询；未命中返回 null。 */
  Entry get(long key) {
    int index = index(key);
    if (keys[index] != key) {
      return null;
    }
    return new Entry(depths[index], scores[index], flags[index], moves[index]);
  }

  /** 写入一条结果；move 用棋盘下标表示，-1 表示无。 */
  void put(long key, int depth, int score, int flag, int move) {
    int index = index(key);
    keys[index] = key;
    depths[index] = (byte) depth;
    scores[index] = score;
    flags[index] = (byte) flag;
    moves[index] = move;
  }

  /** 清空表，供新的一局开始前调用。 */
  void clear() {
    java.util.Arrays.fill(keys, 0L);
    java.util.Arrays.fill(depths, (byte) 0);
  }

  private int index(long key) {
    // 低位与高位混合，避免棋盘哈希的后半段变化少导致冲突集中
    return (int) ((key ^ (key >>> 32)) & mask);
  }

  /** 置换表条目。 */
  record Entry(int depth, int score, int flag, int move) {}
}
