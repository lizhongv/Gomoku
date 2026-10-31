package io.github.lizhongv.gomoku.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BoardTest {

  @Test
  void newBoardIsEmpty() {
    Board board = new Board(15);
    assertThat(board.size()).isEqualTo(15);
    assertThat(board.moveCount()).isZero();
    assertThat(board.hash()).isZero();
    assertThat(board.candidates()).isEmpty();
    assertThat(board.lastMove()).isNull();
    assertThat(board.get(7, 7)).isEqualTo(Stone.EMPTY);
  }

  @Test
  void rejectsIllegalSize() {
    assertThatThrownBy(() -> new Board(4)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Board(30)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void placeAndUndoRestoresHashAndCandidates() {
    Board board = new Board(15);
    long emptyHash = board.hash();
    assertThat(board.place(new Move(7, 7), Stone.BLACK)).isTrue();
    assertThat(board.get(7, 7)).isEqualTo(Stone.BLACK);
    assertThat(board.moveCount()).isEqualTo(1);
    assertThat(board.hash()).isNotEqualTo(emptyHash);
    assertThat(board.candidates()).isNotEmpty();

    assertThat(board.undo()).isEqualTo(new Move(7, 7));
    assertThat(board.moveCount()).isZero();
    assertThat(board.hash()).isEqualTo(emptyHash);
    assertThat(board.get(7, 7)).isEqualTo(Stone.EMPTY);
  }

  @Test
  void consecutiveUndoKeepsNeighborCountsCorrect() {
    Board board = new Board(15);
    board.place(new Move(7, 7), Stone.BLACK);
    board.place(new Move(7, 8), Stone.WHITE);
    board.place(new Move(8, 8), Stone.BLACK);
    board.undo();
    board.undo();
    assertThat(board.isCandidate(new Move(8, 8))).isTrue();
    assertThat(board.isCandidate(new Move(7, 8))).isTrue();
    board.undo();
    assertThat(board.candidates()).isEmpty();
  }

  @Test
  void rejectsOccupiedOutOfBoundsAndEmptyStone() {
    Board board = new Board(15);
    assertThat(board.place(new Move(7, 7), Stone.EMPTY)).isFalse();
    assertThat(board.place(new Move(-1, 7), Stone.BLACK)).isFalse();
    assertThat(board.place(new Move(15, 7), Stone.BLACK)).isFalse();
    board.place(new Move(7, 7), Stone.BLACK);
    assertThat(board.place(new Move(7, 7), Stone.WHITE)).isFalse();
  }

  @Test
  void copyIsIndependent() {
    Board board = new Board(15);
    board.place(new Move(7, 7), Stone.BLACK);
    Board copy = board.copy();
    assertThat(copy.hash()).isEqualTo(board.hash());
    assertThat(copy.moveCount()).isEqualTo(1);
    copy.place(new Move(7, 8), Stone.WHITE);
    assertThat(copy.moveCount()).isEqualTo(2);
    assertThat(board.moveCount()).isEqualTo(1);
  }

  @Test
  void hashDependsOnSideToMove() {
    Board board = new Board(15);
    assertThat(board.hash(Stone.BLACK)).isNotEqualTo(board.hash(Stone.WHITE));
  }

  @Test
  void candidateOnlyNearStones() {
    Board board = new Board(15);
    board.place(new Move(7, 7), Stone.BLACK);
    assertThat(board.isCandidate(new Move(9, 9))).isTrue();
    assertThat(board.isCandidate(new Move(10, 10))).isFalse();
    assertThat(board.hasNeighbor(8, 8, 2)).isTrue();
    assertThat(board.hasNeighbor(0, 0, 2)).isFalse();
    assertThat(board.hasNeighbor(10, 10, 2)).isFalse();
  }

  @Test
  void undoOnEmptyBoardThrows() {
    Board board = new Board(15);
    assertThatThrownBy(board::undo).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void stoneCodesAndOpposite() {
    assertThat(Stone.of(1)).isEqualTo(Stone.BLACK);
    assertThat(Stone.of(2)).isEqualTo(Stone.WHITE);
    assertThat(Stone.of(99)).isEqualTo(Stone.EMPTY);
    assertThat(Stone.BLACK.opposite()).isEqualTo(Stone.WHITE);
    assertThatThrownBy(() -> Stone.EMPTY.opposite()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void moveIndexRoundTrip() {
    Move move = new Move(3, 4);
    assertThat(Move.ofIndex(move.index(15), 15)).isEqualTo(move);
    assertThat(move.distanceTo(new Move(5, 6))).isEqualTo(2);
  }
}
