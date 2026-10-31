package io.github.lizhongv.gomoku.ai.bench;

import io.github.lizhongv.gomoku.ai.GomokuEngine;
import io.github.lizhongv.gomoku.ai.SearchLimits;
import io.github.lizhongv.gomoku.ai.SearchResult;
import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.WinDetector;
import java.util.Locale;

/**
 * 自对弈参数实验：对比两组限制条件的胜率，用于调参与回归。
 *
 * <p>用法：{@code java ... SelfPlayExperiment <局数> <A时间> <A深度> <A随机> <B时间> <B深度> <B随机>}
 *
 * <p>例：{@code 20 300 4 0 300 2 0} 表示同样 300 ms，深度 4 对深度 2。
 */
public final class SelfPlayExperiment {

  private SelfPlayExperiment() {}

  /** 入口。 */
  public static void main(String[] args) {
    int games = args.length > 0 ? Integer.parseInt(args[0]) : 20;
    long timeA = args.length > 1 ? Long.parseLong(args[1]) : 300;
    int depthA = args.length > 2 ? Integer.parseInt(args[2]) : 4;
    double randomA = args.length > 3 ? Double.parseDouble(args[3]) : 0;
    long timeB = args.length > 4 ? Long.parseLong(args[4]) : timeA;
    int depthB = args.length > 5 ? Integer.parseInt(args[5]) : 2;
    double randomB = args.length > 6 ? Double.parseDouble(args[6]) : 0;

    SearchLimits a = new SearchLimits(timeA, depthA, 12, randomA, depthA >= 4);
    SearchLimits b = new SearchLimits(timeB, depthB, 12, randomB, depthB >= 4);
    int winsA = 0;
    int winsB = 0;
    int draws = 0;
    long depthSumA = 0;
    long depthSumB = 0;
    long movesA = 0;
    long movesB = 0;
    for (int game = 0; game < games; game++) {
      GomokuEngine engineA = new GomokuEngine(15, RuleSet.FREESTYLE, 11L);
      GomokuEngine engineB = new GomokuEngine(15, RuleSet.FREESTYLE, 22L);
      Board board = new Board(15);
      Stone turn = Stone.BLACK;
      Stone colorA = game % 2 == 0 ? Stone.BLACK : Stone.WHITE;
      Stone winner = Stone.EMPTY;
      for (int ply = 0; ply < 225; ply++) {
        boolean isA = turn == colorA;
        SearchResult result = isA ? engineA.search(board, turn, a) : engineB.search(board, turn, b);
        if (result.move() == null) {
          break;
        }
        if (isA) {
          depthSumA += result.depth();
          movesA++;
        } else {
          depthSumB += result.depth();
          movesB++;
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
      } else if (winner == colorA) {
        winsA++;
      } else {
        winsB++;
      }
    }
    System.out.printf(
        Locale.ROOT,
        "A(time=%d,depth=%d,r=%.2f) 胜 %d，B(time=%d,depth=%d,r=%.2f) 胜 %d，和 %d；"
            + "平均深度 A=%.1f B=%.1f%n",
        timeA,
        depthA,
        randomA,
        winsA,
        timeB,
        depthB,
        randomB,
        winsB,
        draws,
        movesA == 0 ? 0 : (double) depthSumA / movesA,
        movesB == 0 ? 0 : (double) depthSumB / movesB);
  }
}
