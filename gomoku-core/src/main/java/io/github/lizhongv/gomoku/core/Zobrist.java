package io.github.lizhongv.gomoku.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zobrist 哈希表。
 *
 * <p>按棋盘边长建表，使用固定种子，保证同一局面在不同运行中得到相同哈希，便于测试复现与置换表工作。 表一旦生成即只读，多线程共享安全。
 */
final class Zobrist {

  /** 固定种子；改动会使历史基准结果失效，不要随意修改。 */
  private static final long SEED = 0x9E3779B97F4A7C15L;

  private static final long SIDE_KEY = 0x1234567890ABCDEFL;

  private static final Map<Integer, long[][]> TABLES = new ConcurrentHashMap<>();

  private Zobrist() {}

  /** 某边长的哈希表：[颜色下标 0=黑、1=白][格子下标]。 */
  static long[][] table(int size) {
    return TABLES.computeIfAbsent(size, Zobrist::build);
  }

  /** 轮到白方时的附加键，区分同一局面下不同的行棋方。 */
  static long sideKey() {
    return SIDE_KEY;
  }

  private static long[][] build(int size) {
    long[][] table = new long[2][size * size];
    long state = SEED;
    for (int color = 0; color < 2; color++) {
      for (int i = 0; i < size * size; i++) {
        // xorshift64*：简单、无依赖、跨平台结果一致
        state ^= state >>> 12;
        state ^= state << 25;
        state ^= state >>> 27;
        table[color][i] = state * 0x2545F4914F6CDD1DL;
      }
    }
    return table;
  }
}
