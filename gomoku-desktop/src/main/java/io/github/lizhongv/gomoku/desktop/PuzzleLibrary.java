package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.Stone;
import java.util.ArrayList;
import java.util.List;

/**
 * 残局题库：预置局面，玩家执黑，目标是按要求取胜。
 *
 * <p>题目由固定坐标生成，每次运行一致；solution 只用于展示提示，不强制按它走， 只要在限定手数内取胜即算过关。
 */
public final class PuzzleLibrary {

  /** 一道残局。 */
  public record Puzzle(
      String id, String name, int level, int maxMoves, Board board, List<Move> solution) {}

  private PuzzleLibrary() {}

  /** 全部题目。 */
  public static List<Puzzle> all() {
    List<Puzzle> list = new ArrayList<>();
    list.add(
        new Puzzle(
            "p1",
            "四连冲杀",
            1,
            1,
            board(new int[][] {{7, 4}, {7, 5}, {7, 6}, {7, 7}}, new int[][] {{7, 3}, {0, 0}}),
            List.of(new Move(7, 8))));
    list.add(
        new Puzzle(
            "p2",
            "竖向收官",
            1,
            1,
            board(new int[][] {{4, 7}, {5, 7}, {6, 7}, {7, 7}}, new int[][] {{3, 7}, {0, 1}}),
            List.of(new Move(8, 7))));
    list.add(
        new Puzzle(
            "p3",
            "斜线一刀",
            1,
            1,
            board(new int[][] {{5, 5}, {6, 6}, {7, 7}, {8, 8}}, new int[][] {{4, 4}, {0, 2}}),
            List.of(new Move(9, 9))));
    list.add(
        new Puzzle(
            "p4",
            "反斜收束",
            2,
            1,
            board(new int[][] {{5, 9}, {6, 8}, {7, 7}, {8, 6}}, new int[][] {{4, 10}, {0, 3}}),
            List.of(new Move(9, 5))));
    list.add(
        new Puzzle(
            "p5",
            "连续冲四",
            3,
            3,
            board(
                new int[][] {{7, 3}, {7, 4}, {7, 5}, {5, 7}, {6, 7}},
                new int[][] {{0, 11}, {1, 11}}),
            List.of(new Move(7, 7), new Move(4, 7))));
    list.add(
        new Puzzle(
            "p6",
            "镜像连杀",
            3,
            3,
            board(
                new int[][] {{7, 11}, {7, 10}, {7, 9}, {5, 7}, {6, 7}},
                new int[][] {{0, 0}, {1, 0}}),
            List.of(new Move(7, 7), new Move(4, 7))));
    return List.copyOf(list);
  }

  private static Board board(int[][] black, int[][] white) {
    Board board = new Board(15);
    for (int[] point : black) {
      board.place(new Move(point[0], point[1]), Stone.BLACK);
    }
    for (int[] point : white) {
      board.place(new Move(point[0], point[1]), Stone.WHITE);
    }
    return board;
  }
}
