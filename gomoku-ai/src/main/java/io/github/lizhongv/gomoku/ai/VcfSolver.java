package io.github.lizhongv.gomoku.ai;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.WinDetector;
import java.util.ArrayList;
import java.util.List;

/**
 * VCF（Victory by Continuous Fours，连续冲四取胜）搜索。
 *
 * <p>只考虑能形成四的进攻着法；对手被四逼着只能堵，进攻方再找下一个四。若某一步形成活四或双四， 对手只堵得了一个成五点，直接判胜。VCF 能在很浅的搜索里发现普通 α-β
 * 需要很深才能看到的连杀。
 *
 * <p>搜索深度按进攻着法计数，默认 12 层；时间到即返回未找到，不影响主搜索。
 */
final class VcfSolver {

  private VcfSolver() {}

  /**
   * 寻找 VCF 必胜序列。
   *
   * @return 从当前局面开始的着法序列（进攻方、防守方交替），未找到返回 null
   */
  static List<Move> findWin(Board board, Stone side, RuleSet ruleSet, int maxDepth, long deadline) {
    if (maxDepth <= 0 || System.nanoTime() > deadline) {
      return null;
    }
    return search(board, side, ruleSet, maxDepth, deadline);
  }

  private static List<Move> search(
      Board board, Stone side, RuleSet ruleSet, int depth, long deadline) {
    if (depth <= 0 || System.nanoTime() > deadline) {
      return null;
    }
    for (Move move : fourMoves(board, side, ruleSet)) {
      board.place(move, side);
      try {
        if (WinDetector.isWin(board, move, side, ruleSet)) {
          return List.of(move);
        }
        List<Move> wins = fiveMoves(board, side, ruleSet);
        if (wins.isEmpty()) {
          continue;
        }
        // 对手若已有成五点，会先成五，这条线不成立（必须放在「活四即胜」判断之前）
        List<Move> opponentWins = fiveMoves(board, side.opposite(), ruleSet);
        if (!opponentWins.isEmpty()) {
          continue;
        }
        if (wins.size() >= 2) {
          // 活四或双四：对手只能堵一个点
          return List.of(move);
        }
        Move block = wins.get(0);
        board.place(block, side.opposite());
        List<Move> rest;
        try {
          rest = search(board, side, ruleSet, depth - 1, deadline);
        } finally {
          board.undo();
        }
        if (rest != null) {
          List<Move> line = new ArrayList<>(rest.size() + 2);
          line.add(move);
          line.add(block);
          line.addAll(rest);
          return line;
        }
      } finally {
        board.undo();
      }
    }
    return null;
  }

  /** 能形成四（活四或冲四）的合法着法。 */
  private static List<Move> fourMoves(Board board, Stone side, RuleSet ruleSet) {
    List<Move> result = new ArrayList<>();
    for (Move move : board.candidates()) {
      if (!ruleSet.isLegal(board, move, side)) {
        continue;
      }
      if (io.github.lizhongv.gomoku.core.ThreatAnalyzer.analyze(board, move, side).hasFour()) {
        result.add(move);
      }
    }
    return result;
  }

  /** 当前行棋方能直接成五的全部落点。 */
  private static List<Move> fiveMoves(Board board, Stone side, RuleSet ruleSet) {
    List<Move> result = new ArrayList<>();
    for (Move move : board.candidates()) {
      if (!ruleSet.isLegal(board, move, side)) {
        continue;
      }
      board.place(move, side);
      boolean win = WinDetector.isWin(board, move, side, ruleSet);
      board.undo();
      if (win) {
        result.add(move);
      }
    }
    return result;
  }
}
