package io.github.lizhongv.gomoku.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuleSetTest {

  private static Board boardWith(Stone stone, int[][] points) {
    Board board = new Board(15);
    for (int[] point : points) {
      board.place(new Move(point[0], point[1]), stone);
    }
    return board;
  }

  @Test
  void freestyleAllowsEverythingExceptOccupiedCells() {
    Board board = boardWith(Stone.BLACK, new int[][] {{7, 3}, {7, 4}, {7, 5}, {7, 6}, {7, 7}});
    assertThat(RuleSet.FREESTYLE.isForbidden(board, new Move(7, 8), Stone.BLACK)).isFalse();
    assertThat(RuleSet.FREESTYLE.isLegal(board, new Move(7, 8), Stone.BLACK)).isTrue();
    assertThat(RuleSet.FREESTYLE.isLegal(board, new Move(7, 7), Stone.BLACK)).isFalse();
  }

  @Test
  void renjuForbidsOverline() {
    Board board = boardWith(Stone.BLACK, new int[][] {{7, 2}, {7, 3}, {7, 4}, {7, 5}, {7, 6}});
    assertThat(RuleSet.RENJU.isForbidden(board, new Move(7, 7), Stone.BLACK)).isTrue();
    assertThat(RuleSet.RENJU.isLegal(board, new Move(7, 7), Stone.BLACK)).isFalse();
  }

  @Test
  void renjuExactFiveTakesPrecedenceOverForbiddenShapes() {
    Board board =
        boardWith(
            Stone.BLACK, new int[][] {{7, 3}, {7, 4}, {7, 5}, {7, 6}, {3, 7}, {4, 7}, {5, 7}});
    assertThat(RuleSet.RENJU.isForbidden(board, new Move(7, 7), Stone.BLACK)).isFalse();
    assertThat(RuleSet.RENJU.isLegal(board, new Move(7, 7), Stone.BLACK)).isTrue();
  }

  @Test
  void renjuForbidsDoubleFour() {
    Board board =
        boardWith(Stone.BLACK, new int[][] {{7, 3}, {7, 4}, {7, 5}, {3, 7}, {4, 7}, {5, 7}});
    assertThat(RuleSet.RENJU.isForbidden(board, new Move(7, 7), Stone.BLACK)).isTrue();
  }

  @Test
  void renjuForbidsDoubleThree() {
    Board board = boardWith(Stone.BLACK, new int[][] {{7, 4}, {7, 5}, {4, 7}, {5, 7}});
    assertThat(RuleSet.RENJU.isForbidden(board, new Move(7, 7), Stone.BLACK)).isTrue();
  }

  @Test
  void renjuDoesNotRestrictWhite() {
    Board board =
        boardWith(Stone.WHITE, new int[][] {{7, 3}, {7, 4}, {7, 5}, {3, 7}, {4, 7}, {5, 7}});
    assertThat(RuleSet.RENJU.isForbidden(board, new Move(7, 7), Stone.WHITE)).isFalse();
  }

  @Test
  void forbiddenCheckLeavesBoardUnchanged() {
    Board board = boardWith(Stone.BLACK, new int[][] {{7, 4}, {7, 5}, {4, 7}, {5, 7}});
    long hash = board.hash();
    int count = board.moveCount();
    RuleSet.RENJU.isForbidden(board, new Move(7, 7), Stone.BLACK);
    assertThat(board.hash()).isEqualTo(hash);
    assertThat(board.moveCount()).isEqualTo(count);
    assertThat(board.isEmpty(7, 7)).isTrue();
  }

  @Test
  void winDetectionHonoursOverlineRules() {
    Board board = new Board(15);
    for (int col = 2; col <= 7; col++) {
      board.place(new Move(7, col), Stone.BLACK);
    }
    Move last = new Move(7, 7);
    assertThat(WinDetector.isWin(board, last, Stone.BLACK, RuleSet.FREESTYLE)).isTrue();
    assertThat(WinDetector.isWin(board, last, Stone.BLACK, RuleSet.RENJU)).isFalse();
    assertThat(WinDetector.longestLine(board, last, Stone.BLACK)).isEqualTo(6);
  }

  @Test
  void detectsFiveInAllDirections() {
    int[][] cases = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
    for (int[] direction : cases) {
      Board board = new Board(15);
      Move last = null;
      for (int step = 0; step < 5; step++) {
        Move move = new Move(7 + direction[0] * step, 7 + direction[1] * step);
        board.place(move, Stone.BLACK);
        last = move;
      }
      assertThat(WinDetector.isWin(board, last, Stone.BLACK, RuleSet.FREESTYLE)).isTrue();
      assertThat(WinDetector.lineLength(board, last, Stone.BLACK, direction[0], direction[1]))
          .isEqualTo(5);
    }
  }

  @Test
  void gameStateAlternatesTurnsAndRejectsOccupied() {
    GameState state = new GameState(new Board(15), RuleSet.FREESTYLE, Stone.BLACK);
    assertThat(state.turn()).isEqualTo(Stone.BLACK);
    assertThat(state.play(new Move(7, 7))).isTrue();
    assertThat(state.turn()).isEqualTo(Stone.WHITE);
    assertThat(state.play(new Move(7, 7))).isFalse();
    assertThat(state.turn()).isEqualTo(Stone.WHITE);
    assertThat(state.moves()).containsExactly(new Move(7, 7));
  }

  @Test
  void gameStateDetectsWinAndUndoReopensGame() {
    GameState state = new GameState(new Board(15), RuleSet.FREESTYLE, Stone.BLACK);
    int[][] blacks = {{7, 3}, {7, 4}, {7, 5}, {7, 6}, {7, 7}};
    int[][] whites = {{0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 4}};
    for (int i = 0; i < 5; i++) {
      assertThat(state.play(new Move(blacks[i][0], blacks[i][1]))).isTrue();
      if (i < 4) {
        assertThat(state.play(new Move(whites[i][0], whites[i][1]))).isTrue();
      }
    }
    assertThat(state.result()).isEqualTo(GameResult.BLACK_WIN);
    assertThat(state.result().isFinished()).isTrue();
    assertThat(state.result().winner()).isEqualTo(Stone.BLACK);

    assertThat(state.undo()).isEqualTo(new Move(7, 7));
    assertThat(state.result()).isEqualTo(GameResult.ONGOING);
    assertThat(state.turn()).isEqualTo(Stone.BLACK);
  }

  @Test
  void replayRebuildsGameOrFails() {
    List<Move> moves = List.of(new Move(7, 7), new Move(8, 8), new Move(7, 8));
    GameState state = GameState.replay(new Board(15), RuleSet.FREESTYLE, Stone.BLACK, moves);
    assertThat(state.moves()).isEqualTo(moves);
    assertThat(state.turn()).isEqualTo(Stone.WHITE);
  }

  @Test
  void gameResultHelpers() {
    assertThat(GameResult.ONGOING.isFinished()).isFalse();
    assertThat(GameResult.DRAW.isFinished()).isTrue();
    assertThat(GameResult.DRAW.winner()).isEqualTo(Stone.EMPTY);
    assertThat(GameResult.WHITE_WIN.winner()).isEqualTo(Stone.WHITE);
  }

  @Test
  void undoAfterWinKeepsSamePlayer() {
    GameState state = new GameState(new Board(15), RuleSet.FREESTYLE, Stone.BLACK);
    int[][] moves = {{7, 3}, {0, 0}, {7, 4}, {0, 1}, {7, 5}, {0, 2}, {7, 6}, {0, 3}, {7, 7}};
    for (int[] move : moves) {
      state.play(new Move(move[0], move[1]));
    }
    assertThat(state.result()).isEqualTo(GameResult.BLACK_WIN);
    state.undo();
    assertThat(state.turn()).isEqualTo(Stone.BLACK);
  }
}
