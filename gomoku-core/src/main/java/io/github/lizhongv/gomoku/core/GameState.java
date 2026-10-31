package io.github.lizhongv.gomoku.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 一局棋的状态机：行棋方、合法性、胜负与和棋。
 *
 * <p>界面、AI 自对弈与联机服务器共用这一份规则实现，避免各处各写一套胜负判定。 非线程安全：一个实例只由一个线程驱动。
 */
public final class GameState {

  private final Board board;

  private final RuleSet ruleSet;

  private final List<Move> moves = new ArrayList<>();

  private Stone turn;

  private GameResult result = GameResult.ONGOING;

  /** 创建新棋局。 */
  public GameState(Board board, RuleSet ruleSet, Stone first) {
    this.board = board;
    this.ruleSet = ruleSet;
    this.turn = first == Stone.WHITE ? Stone.WHITE : Stone.BLACK;
  }

  /** 棋盘。 */
  public Board board() {
    return board;
  }

  /** 规则。 */
  public RuleSet ruleSet() {
    return ruleSet;
  }

  /** 当前行棋方。 */
  public Stone turn() {
    return turn;
  }

  /** 当前结果。 */
  public GameResult result() {
    return result;
  }

  /** 最近一手；空盘为 null。 */
  public Move lastMove() {
    return board.lastMove();
  }

  /** 已进行的着法（按顺序，可修改的快照由调用方复制）。 */
  public List<Move> moves() {
    return List.copyOf(moves);
  }

  /** 该手是否合法：棋局进行中、轮到落子方且不违禁。 */
  public boolean isLegal(Move move) {
    return result == GameResult.ONGOING
        && board.inBounds(move.row(), move.col())
        && ruleSet.isLegal(board, move, turn);
  }

  /**
   * 落一手棋。
   *
   * @return 是否成功；不合法时棋局不变
   */
  public boolean play(Move move) {
    if (!isLegal(move)) {
      return false;
    }
    Stone moving = turn;
    board.place(move, moving);
    moves.add(move);
    if (WinDetector.isWin(board, move, moving, ruleSet)) {
      result = moving == Stone.BLACK ? GameResult.BLACK_WIN : GameResult.WHITE_WIN;
    } else if (board.isFull()) {
      result = GameResult.DRAW;
    } else {
      turn = moving.opposite();
    }
    return true;
  }

  /**
   * 撤销最近一手。
   *
   * @return 被撤销的着法；没有可撤销的着法时返回 null
   */
  public Move undo() {
    if (moves.isEmpty()) {
      return null;
    }
    Move move = moves.remove(moves.size() - 1);
    Stone moved = turn;
    // 若上一手后棋局已结束，行棋方没有切换，撤销后仍由同一方行棋
    if (result == GameResult.ONGOING) {
      moved = turn.opposite();
    }
    board.undo();
    result = GameResult.ONGOING;
    turn = moved;
    return move;
  }

  /** 从任意局面重放一组着法，供复盘与测试使用；遇到非法着法抛异常。 */
  public static GameState replay(Board board, RuleSet ruleSet, Stone first, List<Move> history) {
    GameState state = new GameState(board, ruleSet, first);
    for (Move move : history) {
      if (!state.play(move)) {
        throw new IllegalArgumentException("非法的着法序列，卡在 " + move);
      }
    }
    return state;
  }
}
