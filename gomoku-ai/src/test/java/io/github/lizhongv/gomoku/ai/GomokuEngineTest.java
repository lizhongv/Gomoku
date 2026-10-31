package io.github.lizhongv.gomoku.ai;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.WinDetector;
import java.util.List;
import org.junit.jupiter.api.Test;

class GomokuEngineTest {

  private static Board board(Stone stone, int[][] points) {
    Board board = new Board(15);
    for (int[] point : points) {
      board.place(new Move(point[0], point[1]), stone);
    }
    return board;
  }

  private static SearchLimits fast() {
    return new SearchLimits(300, 6, 12, 0, true);
  }

  @Test
  void emptyBoardPlaysCenter() {
    GomokuEngine engine = new GomokuEngine();
    SearchResult result = engine.search(new Board(15), Stone.BLACK, fast());
    assertThat(result.move()).isEqualTo(new Move(7, 7));
  }

  @Test
  void takesImmediateWin() {
    Board board = board(Stone.BLACK, new int[][] {{7, 3}, {7, 4}, {7, 5}, {7, 6}});
    board.place(new Move(0, 0), Stone.WHITE);
    GomokuEngine engine = new GomokuEngine();
    SearchResult result = engine.search(board, Stone.BLACK, fast());
    assertThat(result.move()).isNotNull();
    board.place(result.move(), Stone.BLACK);
    assertThat(WinDetector.isWin(board, result.move(), Stone.BLACK, RuleSet.FREESTYLE)).isTrue();
  }

  @Test
  void blocksOpponentWin() {
    // 黑棋四连，左端被白子封死，唯一成五点是 (7, 7)
    Board board = board(Stone.BLACK, new int[][] {{7, 3}, {7, 4}, {7, 5}, {7, 6}});
    board.place(new Move(7, 2), Stone.WHITE);
    board.place(new Move(6, 6), Stone.WHITE);
    GomokuEngine engine = new GomokuEngine();
    SearchResult result = engine.search(board, Stone.WHITE, fast());
    assertThat(result.move()).isEqualTo(new Move(7, 7));
  }

  @Test
  void findsVcfWin() {
    // 黑在 (7,7) 形成冲四，白只能堵 (7,6)，黑再走 (4,7) 成活四
    Board board = board(Stone.BLACK, new int[][] {{7, 3}, {7, 4}, {7, 5}, {5, 7}, {6, 7}});
    board.place(new Move(0, 0), Stone.WHITE);
    board.place(new Move(1, 1), Stone.WHITE);
    GomokuEngine engine = new GomokuEngine();
    SearchResult result = engine.search(board, Stone.BLACK, fast());
    assertThat(result.move()).isNotNull();
    assertThat(result.score()).isGreaterThanOrEqualTo(GomokuEngine.MATE - 20);
    assertThat(result.pv()).isNotEmpty();
  }

  @Test
  void respectsTimeBudget() {
    Board board = board(Stone.BLACK, new int[][] {{7, 7}, {7, 8}, {8, 8}, {6, 6}, {8, 6}});
    board.place(new Move(7, 9), Stone.WHITE);
    GomokuEngine engine = new GomokuEngine();
    SearchLimits limits = new SearchLimits(150, 16, 20, 0, true);
    long start = System.nanoTime();
    SearchResult result = engine.search(board, Stone.WHITE, limits);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
    assertThat(result.move()).isNotNull();
    assertThat(elapsedMs).isLessThan(2_000L);
  }

  @Test
  void analyzeRanksCandidates() {
    Board board = board(Stone.BLACK, new int[][] {{7, 7}, {7, 8}});
    board.place(new Move(8, 8), Stone.WHITE);
    GomokuEngine engine = new GomokuEngine();
    List<Candidate> candidates =
        engine.analyze(board, Stone.BLACK, 5, new SearchLimits(300, 4, 12, 0, false));
    assertThat(candidates).hasSize(5);
    assertThat(candidates).isSortedAccordingTo((a, b) -> Integer.compare(b.score(), a.score()));
  }

  @Test
  void renjuAvoidsForbiddenMove() {
    // 黑方若走 (7,7) 会形成双三（禁手），引擎不应选择它
    Board board = board(Stone.BLACK, new int[][] {{7, 4}, {7, 5}, {4, 7}, {5, 7}, {0, 0}});
    board.place(new Move(0, 1), Stone.WHITE);
    board.place(new Move(1, 0), Stone.WHITE);
    GomokuEngine engine = new GomokuEngine(15, RuleSet.RENJU);
    SearchResult result = engine.search(board, Stone.BLACK, fast());
    assertThat(result.move()).isNotEqualTo(new Move(7, 7));
  }

  @Test
  void randomLevelStillReturnsLegalMove() {
    Board board = board(Stone.BLACK, new int[][] {{7, 7}, {6, 7}});
    board.place(new Move(6, 6), Stone.WHITE);
    GomokuEngine engine = new GomokuEngine(15, RuleSet.FREESTYLE, 7L);
    SearchLimits limits = SearchLimits.forLevel(1);
    SearchResult result = engine.search(board, Stone.WHITE, limits);
    assertThat(result.move()).isNotNull();
    assertThat(board.isEmpty(result.move().row(), result.move().col())).isTrue();
  }

  @Test
  void timeoutLeavesBoardUntouched() {
    // 回归：超时异常必须撤销已落下的棋子，否则真实棋盘会被留下残子
    Board board = board(Stone.BLACK, new int[][] {{7, 7}, {7, 8}, {8, 8}, {6, 6}, {8, 6}, {6, 8}});
    board.place(new Move(7, 9), Stone.WHITE);
    board.place(new Move(5, 7), Stone.WHITE);
    long hash = board.hash();
    int moves = board.moveCount();
    GomokuEngine engine = new GomokuEngine();
    engine.search(board, Stone.BLACK, new SearchLimits(1, 16, 20, 0, true));
    assertThat(board.hash()).isEqualTo(hash);
    assertThat(board.moveCount()).isEqualTo(moves);
  }

  @Test
  void limitsValidateArguments() {
    assertThat(SearchLimits.forLevel(1).timeMillis()).isPositive();
    assertThat(SearchLimits.forLevel(12).maxDepth()).isEqualTo(16);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> SearchLimits.forLevel(0))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> new SearchLimits(0, 4, 8, 0, true))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
