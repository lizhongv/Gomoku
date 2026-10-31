package io.github.lizhongv.gomoku.bench;

import io.github.lizhongv.gomoku.ai.GomokuEngine;
import io.github.lizhongv.gomoku.ai.SearchLimits;
import io.github.lizhongv.gomoku.ai.SearchResult;
import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.WinDetector;
import java.util.Locale;

/**
 * 新旧引擎对照：v2 搜索引擎对 v1 的深度 3 启发式 AI。
 *
 * <p>用法：{@code java ... VersusMain <局数> <v2 档位>}，默认 20 局、5 档。
 * 双方各执黑一半，15 路无禁手，直到五连或棋盘下满。
 */
public final class VersusMain {

    private VersusMain() {}

    /** 入口。 */
    public static void main(String[] args) {
        int games = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int level = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        SearchLimits limits = SearchLimits.forLevel(level);
        int newWins = 0;
        int oldWins = 0;
        int draws = 0;
        long start = System.nanoTime();
        for (int game = 0; game < games; game++) {
            Board board = new Board(15);
            Stone turn = Stone.BLACK;
            Stone newColor = game % 2 == 0 ? Stone.BLACK : Stone.WHITE;
            Stone winner = Stone.EMPTY;
            GomokuEngine engine = new GomokuEngine(15, RuleSet.FREESTYLE, 7L);
            for (int ply = 0; ply < 225; ply++) {
                Move move =
                        turn == newColor ? chooseNew(engine, board, turn, limits) : chooseOld(board, turn);
                if (move == null || !board.isEmpty(move.row(), move.col())) {
                    break;
                }
                board.place(move, turn);
                if (WinDetector.isWin(board, move, turn, RuleSet.FREESTYLE)) {
                    winner = turn;
                    break;
                }
                turn = turn.opposite();
            }
            if (winner == Stone.EMPTY) {
                draws++;
            } else if (winner == newColor) {
                newWins++;
            } else {
                oldWins++;
            }
        }
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        System.out.printf(
                Locale.ROOT,
                "v2（%d 档）胜 %d，v1（深度 3）胜 %d，和 %d；v2 胜率 %.1f%%，耗时 %.1f s%n",
                level, newWins, oldWins, draws, games == 0 ? 0 : 100.0 * newWins / games, seconds);
    }

    private static Move chooseNew(GomokuEngine engine, Board board, Stone side, SearchLimits limits) {
        SearchResult result = engine.search(board, side, limits);
        return result.move();
    }

    private static Move chooseOld(Board board, Stone side) {
        int[][] snapshot = new int[15][15];
        for (int row = 0; row < 15; row++) {
            for (int col = 0; col < 15; col++) {
                snapshot[col][row] = board.get(row, col).code();
            }
        }
        int[] position = new ai.Evaluate(snapshot, side.code()).getTheBestPosition();
        return new Move(position[1], position[0]);
    }
}
