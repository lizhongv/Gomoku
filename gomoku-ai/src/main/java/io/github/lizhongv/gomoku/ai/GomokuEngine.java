package io.github.lizhongv.gomoku.ai;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.Pattern;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.ThreatAnalyzer;
import io.github.lizhongv.gomoku.core.WinDetector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 五子棋搜索引擎。
 *
 * <p>组成：
 *
 * <ol>
 *   <li><b>立即胜负</b>：先看自己能否一手成五，再看对手是否有成五点必须堵；
 *   <li><b>VCF</b>：根节点先做连续冲四搜索，能杀就直接给出杀棋序列；
 *   <li><b>迭代加深 PVS</b>：每层加深，置换表、Killer、历史启发排序，超时丢弃半层结果；
 *   <li><b>难度</b>：时间、深度、候选宽度与随机度共同决定棋力，低档会故意不选最优着法。
 * </ol>
 *
 * <p>搜索在调用线程中同步执行；界面须放到后台线程，时间预算到点会主动中止。 一个实例只由一个线程使用，不要并发调用 {@link #search}。
 */
public final class GomokuEngine {

  /** 必胜分。 */
  public static final int MATE = 1_000_000_000;

  private static final int INF = MATE + 1;

  private static final int MAX_PLY = 64;

  private static final int KILLER_BONUS = 5_000_000;

  private static final int HISTORY_CAP = 1_000_000;

  private final RuleSet ruleSet;

  private final int size;

  private final TranspositionTable table = new TranspositionTable(18);

  private final int[][] killers = new int[MAX_PLY][2];

  private final int[] history;

  private final Random random;

  private long nodes;

  private long deadlineNanos;

  private int branching;

  /** 创建 15 路棋盘的无禁手引擎。 */
  public GomokuEngine() {
    this(15, RuleSet.FREESTYLE);
  }

  /** 创建指定棋盘与规则的引擎。 */
  public GomokuEngine(int size, RuleSet ruleSet) {
    this(size, ruleSet, 20260101L);
  }

  /** 指定随机种子，便于测试复现。 */
  public GomokuEngine(int size, RuleSet ruleSet, long seed) {
    this.size = size;
    this.ruleSet = ruleSet;
    this.history = new int[size * size];
    this.random = new Random(seed);
    reset();
  }

  /** 当前规则集。 */
  public RuleSet ruleSet() {
    return ruleSet;
  }

  /** 清空置换表与启发信息，开始新的一局。 */
  public void reset() {
    table.clear();
    for (int[] killer : killers) {
      killer[0] = -1;
      killer[1] = -1;
    }
    java.util.Arrays.fill(history, 0);
  }

  /**
   * 搜索当前局面的最佳着法。
   *
   * @param board 局面，搜索过程中会临时落子并撤销，返回时棋盘与调用前一致
   * @param side 行棋方
   */
  public SearchResult search(Board board, Stone side, SearchLimits limits) {
    long start = System.nanoTime();
    if (side == null || side == Stone.EMPTY || board.isFull()) {
      return SearchResult.none(0);
    }
    nodes = 0;
    branching = limits.branching();
    deadlineNanos = start + limits.timeMillis() * 1_000_000L;

    if (board.moveCount() == 0) {
      Move center = new Move(size / 2, size / 2);
      return new SearchResult(center, 0, 1, 0, elapsed(start), List.of(center));
    }

    List<Move> candidates = board.candidates();
    if (candidates.isEmpty()) {
      return SearchResult.none(elapsed(start));
    }

    Move myWin = findWinningMove(board, side, candidates);
    if (myWin != null) {
      return new SearchResult(myWin, MATE, 1, nodes, elapsed(start), List.of(myWin));
    }
    List<Move> opponentWins = winningMoves(board, side.opposite(), candidates);
    if (!opponentWins.isEmpty()) {
      Move block = bestByThreat(board, side, opponentWins);
      return new SearchResult(block, -MATE + 2, 1, nodes, elapsed(start), List.of(block));
    }

    if (limits.vcf()) {
      List<Move> line = VcfSolver.findWin(board, side, ruleSet, 12, deadlineNanos);
      if (line != null && !line.isEmpty()) {
        int score = MATE - line.size();
        return new SearchResult(line.get(0), score, 2, nodes, elapsed(start), List.copyOf(line));
      }
    }

    Move bestMove = null;
    int bestScore = -INF;
    int completedDepth = 0;
    List<Move> bestPv = List.of();
    List<Move> bestOrdered = List.of();
    int[] bestScores = new int[0];

    int maxDepth = Math.min(limits.maxDepth(), MAX_PLY - 4);
    for (int depth = 2; depth <= maxDepth; depth += 2) {
      try {
        RootOutcome outcome = searchRoot(board, side, depth);
        if (outcome.move == null) {
          break;
        }
        bestMove = outcome.move;
        bestScore = outcome.score;
        completedDepth = depth;
        bestPv = List.of(outcome.move);
        bestOrdered = outcome.moves;
        bestScores = outcome.scores;
        if (bestScore >= MATE - 1000) {
          break;
        }
      } catch (SearchAborted aborted) {
        break;
      }
    }

    if (bestMove == null) {
      bestMove = bestByThreat(board, side, candidates);
      bestScore = 0;
      bestPv = List.of(bestMove);
    }

    if (limits.randomness() > 0 && bestOrdered.size() > 1) {
      bestMove = applyRandomness(limits.randomness(), bestOrdered, bestScores, bestMove);
    }

    return new SearchResult(bestMove, bestScore, completedDepth, nodes, elapsed(start), bestPv);
  }

  /**
   * 分析若干候选着法，供界面提示与复盘使用。
   *
   * @param topN 返回前多少个候选
   */
  public List<Candidate> analyze(Board board, Stone side, int topN, SearchLimits limits) {
    long start = System.nanoTime();
    nodes = 0;
    branching = Math.max(limits.branching(), topN);
    deadlineNanos = start + limits.timeMillis() * 1_000_000L;
    List<Move> ordered = orderMoves(board, side, board.candidates(), 0, -1);
    List<Candidate> result = new ArrayList<>();
    int depth = Math.max(2, Math.min(limits.maxDepth(), 4));
    try {
      for (Move move : ordered) {
        board.place(move, side);
        int score;
        if (WinDetector.isWin(board, move, side, ruleSet)) {
          score = MATE;
        } else {
          score = -negamax(board, side.opposite(), depth - 1, -INF, INF, 1, limits.branching());
        }
        board.undo();
        result.add(new Candidate(move, score, depth));
      }
    } catch (SearchAborted aborted) {
      // 时间到就用已有结果
    }
    result.sort(Comparator.comparingInt(Candidate::score).reversed());
    return result.size() > topN ? List.copyOf(result.subList(0, topN)) : List.copyOf(result);
  }

  private RootOutcome searchRoot(Board board, Stone side, int depth) {
    List<Move> ordered = orderMoves(board, side, board.candidates(), 0, -1);
    int limit = Math.min(branching, ordered.size());
    List<Move> moves = new ArrayList<>(ordered.subList(0, limit));
    int[] scores = new int[moves.size()];
    Move best = null;
    int bestScore = -INF;
    int alpha = -INF;
    for (int i = 0; i < moves.size(); i++) {
      checkTime();
      Move move = moves.get(i);
      board.place(move, side);
      int score;
      try {
        if (WinDetector.isWin(board, move, side, ruleSet)) {
          score = MATE;
        } else {
          score = -negamax(board, side.opposite(), depth - 1, -INF, -alpha, 1, branching);
        }
      } finally {
        // 超时异常会从递归中直接抛出，必须在 finally 中撤销，否则真实棋盘会被留下残子
        board.undo();
      }
      scores[i] = score;
      if (score > bestScore) {
        bestScore = score;
        best = move;
      }
      if (score > alpha) {
        alpha = score;
      }
    }
    if (best != null) {
      table.put(board.hash(side), depth, bestScore, TranspositionTable.EXACT, best.index(size));
    }
    return new RootOutcome(best, bestScore, moves, scores);
  }

  private int negamax(Board board, Stone side, int depth, int alpha, int beta, int ply, int width) {
    nodes++;
    if ((nodes & 255) == 0) {
      checkTime();
    }
    if (board.isFull()) {
      return 0;
    }
    if (depth <= 0) {
      long value = Evaluator.evaluate(board, side);
      return (int) Math.max(-Evaluator.MAX_SCORE, Math.min(Evaluator.MAX_SCORE, value));
    }

    long key = board.hash(side);
    TranspositionTable.Entry entry = table.get(key);
    int ttMove = -1;
    if (entry != null) {
      ttMove = entry.move();
      if (entry.depth() >= depth) {
        if (entry.flag() == TranspositionTable.EXACT) {
          return entry.score();
        }
        if (entry.flag() == TranspositionTable.LOWER && entry.score() > alpha) {
          alpha = entry.score();
        } else if (entry.flag() == TranspositionTable.UPPER && entry.score() < beta) {
          beta = entry.score();
        }
        if (alpha >= beta) {
          return entry.score();
        }
      }
    }

    List<Move> moves = orderMoves(board, side, board.candidates(), ply, ttMove);
    if (moves.isEmpty()) {
      return 0;
    }
    if (moves.size() > width) {
      moves = moves.subList(0, width);
    }
    int originalAlpha = alpha;
    int originalBeta = beta;
    int bestScore = -INF;
    int bestMove = -1;
    for (Move move : moves) {
      board.place(move, side);
      int score;
      try {
        if (WinDetector.isWin(board, move, side, ruleSet)) {
          score = MATE - ply;
        } else {
          score = -negamax(board, side.opposite(), depth - 1, -beta, -alpha, ply + 1, width);
        }
      } finally {
        board.undo();
      }
      if (score > bestScore) {
        bestScore = score;
        bestMove = move.index(size);
      }
      if (score > alpha) {
        alpha = score;
      }
      if (alpha >= beta) {
        recordKiller(ply, move.index(size));
        bumpHistory(move.index(size), depth);
        break;
      }
    }

    int flag =
        bestScore <= originalAlpha
            ? TranspositionTable.UPPER
            : (bestScore >= originalBeta ? TranspositionTable.LOWER : TranspositionTable.EXACT);
    table.put(key, depth, bestScore, flag, bestMove);
    return bestScore;
  }

  /** 按「能形成什么威胁」给候选排序，四、三优先，其次置换表着法与 Killer。 */
  private List<Move> orderMoves(
      Board board, Stone side, List<Move> candidates, int ply, int ttMove) {
    List<Move> legal = new ArrayList<>(candidates.size());
    for (Move move : candidates) {
      // 连珠规则下黑方的禁手点不能走，直接排除
      if (ruleSet.isLegal(board, move, side)) {
        legal.add(move);
      }
    }
    if (legal.isEmpty()) {
      return List.of();
    }
    int n = legal.size();
    long[] scores = new long[n];
    for (int i = 0; i < n; i++) {
      Move move = legal.get(i);
      int index = move.index(size);
      ThreatAnalyzer.Threat own = ThreatAnalyzer.analyze(board, move, side);
      ThreatAnalyzer.Threat opponent = ThreatAnalyzer.analyze(board, move, side.opposite());
      long score;
      if (own.best() == Pattern.FIVE) {
        score = 1_500_000_000L;
      } else if (opponent.best() == Pattern.FIVE) {
        score = 1_200_000_000L;
      } else {
        score = Math.max(own.score(), opponent.score()) + history[index];
        if (index == ttMove) {
          score += 800_000_000L;
        } else if (ply < MAX_PLY && index == killers[ply][0]) {
          score += KILLER_BONUS;
        } else if (ply < MAX_PLY && index == killers[ply][1]) {
          score += KILLER_BONUS / 2;
        }
      }
      scores[i] = score;
    }
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) {
      order[i] = i;
    }
    java.util.Arrays.sort(order, (a, b) -> Long.compare(scores[b], scores[a]));
    List<Move> result = new ArrayList<>(n);
    for (int i : order) {
      result.add(legal.get(i));
    }
    return result;
  }

  private Move findWinningMove(Board board, Stone side, List<Move> candidates) {
    for (Move move : candidates) {
      if (!ruleSet.isLegal(board, move, side)) {
        continue;
      }
      board.place(move, side);
      boolean win = WinDetector.isWin(board, move, side, ruleSet);
      board.undo();
      if (win) {
        return move;
      }
    }
    return null;
  }

  private List<Move> winningMoves(Board board, Stone side, List<Move> candidates) {
    List<Move> result = new ArrayList<>();
    for (Move move : candidates) {
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

  private Move bestByThreat(Board board, Stone side, List<Move> moves) {
    Move best = moves.get(0);
    long bestScore = Long.MIN_VALUE;
    for (Move move : moves) {
      long score = ThreatAnalyzer.score(board, move, side);
      if (score > bestScore) {
        bestScore = score;
        best = move;
      }
    }
    return best;
  }

  private Move applyRandomness(double probability, List<Move> moves, int[] scores, Move fallback) {
    if (moves.size() < 2 || random.nextDouble() >= probability) {
      return fallback;
    }
    int maxIndex = Math.min(2, moves.size() - 1);
    int index = 1 + random.nextInt(maxIndex);
    return moves.get(index);
  }

  private void recordKiller(int ply, int move) {
    if (ply >= MAX_PLY || killers[ply][0] == move) {
      return;
    }
    killers[ply][1] = killers[ply][0];
    killers[ply][0] = move;
  }

  private void bumpHistory(int move, int depth) {
    history[move] = Math.min(HISTORY_CAP, history[move] + depth * depth);
  }

  private void checkTime() {
    if (System.nanoTime() > deadlineNanos) {
      throw SearchAborted.INSTANCE;
    }
  }

  private long elapsed(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000L;
  }

  private record RootOutcome(Move move, int score, List<Move> moves, int[] scores) {}

  /** 搜索超时，沿调用栈向上中止。单例避免反复创建异常。 */
  private static final class SearchAborted extends RuntimeException {
    static final SearchAborted INSTANCE = new SearchAborted();

    private SearchAborted() {
      super(null, null, false, false);
    }
  }
}
