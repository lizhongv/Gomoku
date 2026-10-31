package io.github.lizhongv.gomoku.ai;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.ThreatAnalyzer;

/**
 * 局面评估：把候选点的威胁折算成一个数，供搜索叶子节点比较。
 *
 * <p>评估从行棋方视角出发，己方威胁减去对方威胁；防守权重略大于进攻，避免引擎只顾进攻被对手先成五。 单个点的威胁由 {@link ThreatAnalyzer}
 * 计算，双四、四三等组合威胁已在其中加分。
 *
 * <p>该评估不是「棋盘绝对分」，而是「下一手机会的净收益」，与搜索的零和假设一致。
 */
public final class Evaluator {

  private static final double DEFENSE_WEIGHT = 1.05;

  /** 评估值上限：低于胜负分，避免大量五连机会累加后与「已获胜」混淆。 */
  public static final long MAX_SCORE = 900_000_000L;

  private Evaluator() {}

  /** 评估局面；side 为当前行棋方。 */
  public static long evaluate(Board board, Stone side) {
    if (board.moveCount() == 0) {
      return 0;
    }
    long score = 0;
    for (Move move : board.candidates()) {
      score += ThreatAnalyzer.score(board, move, side);
      score -= (long) (ThreatAnalyzer.score(board, move, side.opposite()) * DEFENSE_WEIGHT);
      // 单个点就可能累积到很大，逐点截断，避免溢出并保证胜负分始终更大
      score = Math.max(-MAX_SCORE, Math.min(MAX_SCORE, score));
    }
    return score;
  }
}
