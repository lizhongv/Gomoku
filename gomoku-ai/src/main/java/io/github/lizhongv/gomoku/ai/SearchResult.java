package io.github.lizhongv.gomoku.ai;

import io.github.lizhongv.gomoku.core.Move;
import java.util.List;

/**
 * 一次搜索的结果。
 *
 * @param move 推荐着法；无合法着法时为 null
 * @param score 该着法的评分（当前行棋方视角，越大越好）
 * @param depth 实际完成的搜索深度
 * @param nodes 搜索节点数
 * @param elapsedMillis 实际耗时
 * @param pv 主变例（至少包含推荐着法）
 */
public record SearchResult(
    Move move, int score, int depth, long nodes, long elapsedMillis, List<Move> pv) {

  /** 无着法可走。 */
  public static SearchResult none(long elapsedMillis) {
    return new SearchResult(null, 0, 0, 0, elapsedMillis, List.of());
  }
}
