package io.github.lizhongv.gomoku.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lizhongv.gomoku.ai.GomokuEngine;
import io.github.lizhongv.gomoku.ai.SearchLimits;
import io.github.lizhongv.gomoku.ai.SearchResult;
import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.WinDetector;
import org.junit.jupiter.api.Test;

class PuzzleLibraryTest {

  @Test
  void everyPuzzleIsLegalAndSolvable() {
    for (PuzzleLibrary.Puzzle puzzle : PuzzleLibrary.all()) {
      assertThat(puzzle.solution()).isNotEmpty();
      Board board = puzzle.board().copy();
      for (Move move : puzzle.solution()) {
        assertThat(board.isEmpty(move.row(), move.col())).isTrue();
        board.place(move, Stone.BLACK);
      }
      // 引擎在同一局面必须能找到取胜路线（一手胜或 VCF）
      SearchResult result =
          new GomokuEngine(15, RuleSet.FREESTYLE)
              .search(puzzle.board().copy(), Stone.BLACK, SearchLimits.fast());
      assertThat(result.move()).isNotNull();
      assertThat(result.score()).isGreaterThanOrEqualTo(GomokuEngine.MATE - 100);
    }
  }

  @Test
  void oneMovePuzzlesWinImmediately() {
    for (PuzzleLibrary.Puzzle puzzle : PuzzleLibrary.all()) {
      if (puzzle.maxMoves() != 1) {
        continue;
      }
      Board board = puzzle.board().copy();
      Move move = puzzle.solution().get(0);
      assertThat(board.place(move, Stone.BLACK)).isTrue();
      assertThat(WinDetector.isWin(board, move, Stone.BLACK, RuleSet.FREESTYLE)).isTrue();
    }
  }
}
