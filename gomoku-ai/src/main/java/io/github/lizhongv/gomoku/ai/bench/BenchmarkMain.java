package io.github.lizhongv.gomoku.ai.bench;

import io.github.lizhongv.gomoku.ai.GomokuEngine;
import io.github.lizhongv.gomoku.ai.SearchLimits;
import io.github.lizhongv.gomoku.ai.SearchResult;
import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.WinDetector;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI 基准测试：战术题库、搜索性能、自对弈胜率，输出 Markdown 报告。
 *
 * <p>运行：
 *
 * <pre>
 *   mvn -q -pl gomoku-ai exec:java -Dexec.mainClass=io.github.lizhongv.gomoku.ai.bench.BenchmarkMain
 *   mvn -q -pl gomoku-ai exec:java -Dexec.mainClass=...BenchmarkMain -Dexec.args="200"
 * </pre>
 *
 * <p>参数为自对弈局数，默认 20。自对弈强方为 8 档、弱方为 4 档，双方各执黑一半，抵消先手优势。
 */
public final class BenchmarkMain {

  private BenchmarkMain() {}

  /** 运行全部基准并打印报告。 */
  public static void main(String[] args) {
    int games = args.length > 0 ? Integer.parseInt(args[0]) : 20;
    StringBuilder out = new StringBuilder();
    out.append("# AI 基准报告\n\n");

    TacticalReport tactical = runTactics();
    out.append("## 战术题库\n\n");
    out.append(
        String.format(
            Locale.ROOT,
            "- 共 %d 题：一手胜 %d、一手防 %d、VCF %d%n",
            tactical.total(),
            tactical.wins(),
            tactical.blocks(),
            tactical.vcf()));
    out.append(
        String.format(
            Locale.ROOT,
            "- 通过 %d 题，通过率 %.1f%%%n",
            tactical.passed(),
            100.0 * tactical.passed() / tactical.total()));
    if (!tactical.failures().isEmpty()) {
      out.append("- 未通过：").append(String.join("、", tactical.failures())).append('\n');
    }
    out.append('\n');

    PerformanceReport performance = runPerformance();
    out.append("## 搜索性能\n\n");
    out.append(
        String.format(
            Locale.ROOT,
            "- 固定局面 500 ms：%d 节点，%.0f 节点/秒，完成深度 %d%n",
            performance.nodes(),
            performance.nodesPerSecond(),
            performance.depth()));
    out.append(
        String.format(
            Locale.ROOT, "- 每步耗时：P50 %d ms，P95 %d ms%n", performance.p50(), performance.p95()));
    out.append('\n');

    MatchReport match = runSelfPlay(games);
    out.append("## 自对弈（8 档 vs 4 档）\n\n");
    out.append(
        String.format(
            Locale.ROOT,
            "- 共 %d 局：8 档胜 %d、4 档胜 %d、和棋 %d%n",
            match.games(),
            match.strongWins(),
            match.weakWins(),
            match.draws()));
    out.append(
        String.format(
            Locale.ROOT,
            "- 8 档胜率 %.1f%%；平均完成深度：8 档 %.1f，4 档 %.1f%n",
            match.winRate(),
            match.strongDepth(),
            match.weakDepth()));
    out.append('\n');

    System.out.println(out);
  }

  private static TacticalReport runTactics() {
    List<TacticalSuite.Case> cases = TacticalSuite.all();
    List<String> failures = new ArrayList<>();
    int wins = 0;
    int blocks = 0;
    int vcf = 0;
    int passed = 0;
    for (TacticalSuite.Case testCase : cases) {
      switch (testCase.kind()) {
        case WIN -> wins++;
        case BLOCK -> blocks++;
        case VCF -> vcf++;
        default -> throw new IllegalStateException();
      }
      Board board = testCase.board().copy();
      GomokuEngine engine = new GomokuEngine(board.size(), RuleSet.FREESTYLE);
      SearchResult result = engine.search(board, testCase.side(), SearchLimits.fast());
      if (check(testCase, board, result)) {
        passed++;
      } else {
        failures.add(testCase.name());
      }
    }
    return new TacticalReport(cases.size(), wins, blocks, vcf, passed, failures);
  }

  private static boolean check(TacticalSuite.Case testCase, Board board, SearchResult result) {
    if (result.move() == null) {
      return false;
    }
    return switch (testCase.kind()) {
      case WIN -> {
        board.place(result.move(), testCase.side());
        yield WinDetector.isWin(board, result.move(), testCase.side(), RuleSet.FREESTYLE);
      }
      case BLOCK -> result.move().equals(testCase.expected());
      case VCF -> result.score() >= GomokuEngine.MATE - 100;
    };
  }

  private static PerformanceReport runPerformance() {
    Board board = new Board(15);
    int[][] stones = {{7, 7, 1}, {7, 8, 2}, {8, 8, 1}, {6, 6, 2}, {8, 6, 1}, {6, 8, 2}, {9, 7, 1}};
    for (int[] stone : stones) {
      board.place(new Move(stone[0], stone[1]), stone[2] == 1 ? Stone.BLACK : Stone.WHITE);
    }
    GomokuEngine engine = new GomokuEngine();
    SearchLimits limits = new SearchLimits(500, 16, 20, 0, true);
    SearchResult result = engine.search(board.copy(), Stone.WHITE, limits);

    List<Long> times = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      Board copy = board.copy();
      long start = System.nanoTime();
      engine.search(copy, Stone.WHITE, new SearchLimits(100, 10, 16, 0, true));
      times.add((System.nanoTime() - start) / 1_000_000L);
    }
    times.sort(Long::compare);
    long p50 = times.get(times.size() / 2);
    long p95 = times.get(Math.min(times.size() - 1, (int) Math.round(0.95 * (times.size() - 1))));
    double seconds = Math.max(0.001, result.elapsedMillis() / 1000.0);
    return new PerformanceReport(
        result.nodes(), (long) (result.nodes() / seconds), result.depth(), p50, p95);
  }

  private static MatchReport runSelfPlay(int games) {
    GomokuEngine strongEngine = new GomokuEngine(15, RuleSet.FREESTYLE, 1L);
    GomokuEngine weakEngine = new GomokuEngine(15, RuleSet.FREESTYLE, 2L);
    SearchLimits strong = new SearchLimits(300, 8, 16, 0, true);
    SearchLimits weak = new SearchLimits(80, 2, 8, 0.35, false);
    int strongWins = 0;
    int weakWins = 0;
    int draws = 0;
    long strongDepth = 0;
    long weakDepth = 0;
    long strongMoves = 0;
    long weakMoves = 0;
    for (int game = 0; game < games; game++) {
      strongEngine.reset();
      weakEngine.reset();
      Board board = new Board(15);
      Stone turn = Stone.BLACK;
      Stone strongColor = game % 2 == 0 ? Stone.BLACK : Stone.WHITE;
      Stone winner = Stone.EMPTY;
      for (int ply = 0; ply < 225; ply++) {
        SearchResult result =
            turn == strongColor
                ? strongEngine.search(board, turn, strong)
                : weakEngine.search(board, turn, weak);
        if (result.move() == null) {
          break;
        }
        if (turn == strongColor) {
          strongDepth += result.depth();
          strongMoves++;
        } else {
          weakDepth += result.depth();
          weakMoves++;
        }
        board.place(result.move(), turn);
        if (WinDetector.isWin(board, result.move(), turn, RuleSet.FREESTYLE)) {
          winner = turn;
          break;
        }
        turn = turn.opposite();
      }
      if (winner == Stone.EMPTY) {
        draws++;
      } else if (winner == strongColor) {
        strongWins++;
      } else {
        weakWins++;
      }
    }
    double winRate = games == 0 ? 0 : 100.0 * strongWins / games;
    double avgStrong = strongMoves == 0 ? 0 : (double) strongDepth / strongMoves;
    double avgWeak = weakMoves == 0 ? 0 : (double) weakDepth / weakMoves;
    return new MatchReport(games, strongWins, weakWins, draws, winRate, avgStrong, avgWeak);
  }

  private record TacticalReport(
      int total, int wins, int blocks, int vcf, int passed, List<String> failures) {}

  private record PerformanceReport(
      long nodes, double nodesPerSecond, int depth, long p50, long p95) {}

  private record MatchReport(
      int games,
      int strongWins,
      int weakWins,
      int draws,
      double winRate,
      double strongDepth,
      double weakDepth) {}
}
