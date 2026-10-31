package io.github.lizhongv.gomoku.core;

/**
 * 规则集。
 *
 * <p>无禁手：黑先白后，五连及以上即胜，适合大众对局与人机练习。 连珠：黑方三三、四四、长连为禁手，白方不受限制；用于专业场。
 *
 * <p>禁手判定基于局部棋型计数，未实现连珠规则中「假活三」等递归排除，属于工程近似； 对普通对局与 AI 训练足够，正式比赛执裁应使用专门裁判程序。
 */
public enum RuleSet {
  /** 无禁手。 */
  FREESTYLE("无禁手"),
  /** 连珠（黑方禁手）。 */
  RENJU("连珠");

  private final String displayName;

  RuleSet(String displayName) {
    this.displayName = displayName;
  }

  /** 用于界面展示的中文名称。 */
  public String displayName() {
    return displayName;
  }

  /** 该点落子是否合法：空点，且连珠规则下黑方不落入禁手。 */
  public boolean isLegal(Board board, Move move, Stone stone) {
    if (stone == Stone.EMPTY || !board.isEmpty(move.row(), move.col())) {
      return false;
    }
    return !isForbidden(board, move, stone);
  }

  /** 该点落黑子是否为禁手；非黑方恒为 false。 */
  public boolean isForbidden(Board board, Move move, Stone stone) {
    if (this != RENJU || stone != Stone.BLACK) {
      return false;
    }
    if (!board.place(move, Stone.BLACK)) {
      return false;
    }
    try {
      int longest = WinDetector.longestLine(board, move, Stone.BLACK);
      // 成五优先于禁手：恰好五连合法，长连才是禁手
      if (longest == 5) {
        return false;
      }
      if (longest >= 6) {
        return true;
      }
      ThreatAnalyzer.Threat threat = ThreatAnalyzer.analyze(board, move, Stone.BLACK);
      return threat.fours() >= 2 || threat.openThrees() >= 2;
    } finally {
      board.undo();
    }
  }
}
