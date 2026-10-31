package io.github.lizhongv.gomoku.ai.bench;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.Stone;

/**
 * 战术题库：一组有确定答案的局面，用于回归 AI 的战术能力。
 *
 * <p>三类：
 *
 * <ul>
 *   <li><b>一手胜</b>：己方已有四连，只有一个成五点；
 *   <li><b>一手防</b>：对手四连，只有一个成五点必须堵；
 *   <li><b>VCF</b>：需要连续冲四才能取胜的局面，普通浅层搜索容易漏掉。
 * </ul>
 *
 * <p>题库由固定种子生成，每次运行题目一致；答案以「是否成立」判定，不要求唯一着法。
 */
public final class TacticalSuite {

  private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

  private TacticalSuite() {}

  /** 一道题。 */
  public record Case(String name, Board board, Stone side, Move expected, Kind kind) {}

  /** 题目类型。 */
  public enum Kind {
    WIN,
    BLOCK,
    VCF
  }

  /** 全部题目。 */
  public static java.util.List<Case> all() {
    java.util.List<Case> cases = new java.util.ArrayList<>();
    cases.addAll(winInOne());
    cases.addAll(blockInOne());
    cases.addAll(vcf());
    return java.util.List.copyOf(cases);
  }

  private static java.util.List<Case> winInOne() {
    java.util.List<Case> cases = new java.util.ArrayList<>();
    int number = 0;
    for (int[] direction : DIRECTIONS) {
      for (int offset = -3; offset <= 3; offset += 2) {
        Board board = new Board(15);
        Move start = new Move(7 + direction[0] * offset, 7 + direction[1] * offset);
        for (int i = 0; i < 4; i++) {
          board.place(
              new Move(start.row() + direction[0] * i, start.col() + direction[1] * i),
              Stone.BLACK);
        }
        Move low = new Move(start.row() - direction[0], start.col() - direction[1]);
        Move high = new Move(start.row() + direction[0] * 4, start.col() + direction[1] * 4);
        if (!board.inBounds(low.row(), low.col()) || !board.inBounds(high.row(), high.col())) {
          continue;
        }
        // 封住一端，只留一个成五点
        board.place(low, Stone.WHITE);
        board.place(new Move(0, 0), Stone.WHITE);
        board.place(new Move(0, 1), Stone.WHITE);
        cases.add(new Case("win-" + (++number), board, Stone.BLACK, high, Kind.WIN));
      }
    }
    return cases;
  }

  private static java.util.List<Case> blockInOne() {
    java.util.List<Case> cases = new java.util.ArrayList<>();
    int number = 0;
    for (int[] direction : DIRECTIONS) {
      for (int offset = -3; offset <= 3; offset += 2) {
        Board board = new Board(15);
        Move start = new Move(7 + direction[0] * offset, 7 + direction[1] * offset);
        for (int i = 0; i < 4; i++) {
          board.place(
              new Move(start.row() + direction[0] * i, start.col() + direction[1] * i),
              Stone.BLACK);
        }
        Move low = new Move(start.row() - direction[0], start.col() - direction[1]);
        Move high = new Move(start.row() + direction[0] * 4, start.col() + direction[1] * 4);
        if (!board.inBounds(low.row(), low.col()) || !board.inBounds(high.row(), high.col())) {
          continue;
        }
        board.place(low, Stone.WHITE);
        board.place(new Move(0, 0), Stone.WHITE);
        board.place(new Move(0, 1), Stone.WHITE);
        cases.add(new Case("block-" + (++number), board, Stone.WHITE, high, Kind.BLOCK));
      }
    }
    return cases;
  }

  private static java.util.List<Case> vcf() {
    java.util.List<Case> cases = new java.util.ArrayList<>();
    // 以同一杀棋骨架做平移与镜像，得到 10 道等价但位置不同的题
    int[][] shifts = {
      {0, 0}, {1, 0}, {0, 1}, {2, 2}, {-1, 0}, {0, -1}, {1, 1}, {-1, -1}, {2, 0}, {0, 2}, {1, -1},
      {-1, 1}, {2, -1}, {-2, 1}, {1, 2}, {-1, -2}, {2, 1}, {-2, -1}
    };
    for (int i = 0; i < shifts.length; i++) {
      int dr = shifts[i][0];
      int dc = shifts[i][1];
      Board board = new Board(15);
      int[][] blacks = {{7, 3}, {7, 4}, {7, 5}, {5, 7}, {6, 7}};
      for (int[] point : blacks) {
        board.place(new Move(point[0] + dr, point[1] + dc), Stone.BLACK);
      }
      board.place(new Move(0, 11), Stone.WHITE);
      board.place(new Move(1, 11), Stone.WHITE);
      cases.add(new Case("vcf-" + (i + 1), board, Stone.BLACK, new Move(7 + dr, 7 + dc), Kind.VCF));
    }
    return cases;
  }
}
