package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.ai.Candidate;
import io.github.lizhongv.gomoku.ai.GomokuEngine;
import io.github.lizhongv.gomoku.ai.SearchLimits;
import io.github.lizhongv.gomoku.ai.SearchResult;
import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.GameState;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * 一局人机对局的控制：规则推进、AI 后台思考、悔棋、提示与结果记录。
 *
 * <p>AI 在副本棋盘上搜索，避免后台线程与界面读取同一棋盘造成数据竞争；结果回到 JavaFX 线程再落子。 每次新局、悔棋或重新开始时递增代号，过期的后台结果直接丢弃。
 *
 * <p>只在 JavaFX 应用线程调用公开方法；搜索由内部单线程执行器完成。
 */
public final class GameController implements AutoCloseable {

  private final AppContext context;

  private final int level;

  private final Stone human;

  private final Board board = new Board(15);

  private final GameState game = new GameState(board, RuleSet.FREESTYLE, Stone.BLACK);

  private final GomokuEngine engine;

  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "gomoku-ai");
            thread.setDaemon(true);
            return thread;
          });

  private int generation;

  private boolean thinking;

  private GameResult result = GameResult.ONGOING;

  private boolean recorded;

  private Runnable onChange = () -> {};

  private Consumer<GameResult> onFinish = result -> {};

  /** 创建一局。 */
  public GameController(AppContext context, int level, Stone human) {
    this.context = context;
    this.level = level;
    this.human = human;
    this.engine = new GomokuEngine(15, RuleSet.FREESTYLE);
  }

  /** 开始：若玩家执白，先由 AI 落子。 */
  public void start() {
    if (game.turn() != human) {
      think();
    }
  }

  /** 棋盘。 */
  public Board board() {
    return board;
  }

  /** 已下着法。 */
  public List<Move> moves() {
    return game.moves();
  }

  /** 结果。 */
  public GameResult result() {
    return result;
  }

  /** 玩家颜色。 */
  public Stone humanColor() {
    return human;
  }

  /** 等级。 */
  public int level() {
    return level;
  }

  /** 是否轮到玩家。 */
  public boolean humanTurn() {
    return !result.isFinished() && !thinking && game.turn() == human;
  }

  /** AI 是否在思考。 */
  public boolean thinking() {
    return thinking;
  }

  /** 现在轮到谁。 */
  public Stone turn() {
    return game.turn();
  }

  /** 变更回调（落子、思考状态变化）。 */
  public void setOnChange(Runnable onChange) {
    this.onChange = onChange == null ? () -> {} : onChange;
  }

  /** 结束回调。 */
  public void setOnFinish(Consumer<GameResult> onFinish) {
    this.onFinish = onFinish == null ? result -> {} : onFinish;
  }

  /** 玩家落子。 */
  public boolean humanMove(Move move) {
    if (!humanTurn() || !game.isLegal(move)) {
      return false;
    }
    game.play(move);
    afterMove();
    return true;
  }

  /** 悔棋：退回到玩家回合，至少撤掉玩家与 AI 各一手。 */
  public void undo() {
    if (thinking || result.isFinished() || game.moves().isEmpty()) {
      return;
    }
    generation++;
    do {
      game.undo();
    } while (!game.moves().isEmpty() && game.turn() != human);
    saveRecord();
    onChange.run();
    if (game.turn() != human) {
      think();
    }
  }

  /** 提示：后台分析前三候选。 */
  public void hint(Consumer<List<Candidate>> callback) {
    if (!humanTurn()) {
      return;
    }
    Board copy = board.copy();
    Stone side = human;
    executor.submit(
        () -> {
          List<Candidate> candidates =
              engine.analyze(copy, side, 3, SearchLimits.forLevel(Math.min(level, 6)));
          Platform.runLater(() -> callback.accept(candidates));
        });
  }

  /** 认输。 */
  public void resign() {
    if (result.isFinished()) {
      return;
    }
    generation++;
    thinking = false;
    result = human == Stone.BLACK ? GameResult.WHITE_WIN : GameResult.BLACK_WIN;
    finish();
  }

  /** 从一份棋谱继续或复盘。 */
  public static GameController replay(
      AppContext context, int level, Stone human, List<Move> moves) {
    GameController controller = new GameController(context, level, human);
    for (Move move : moves) {
      if (!controller.game.play(move)) {
        break;
      }
      controller.result = controller.game.result();
    }
    controller.recorded = controller.result.isFinished();
    return controller;
  }

  /** 释放后台线程。 */
  @Override
  public void close() {
    generation++;
    executor.shutdownNow();
  }

  private void think() {
    if (result.isFinished() || game.turn() == human) {
      return;
    }
    thinking = true;
    onChange.run();
    int current = ++generation;
    Board copy = board.copy();
    Stone side = game.turn();
    SearchLimits limits = SearchLimits.forLevel(level);
    executor.submit(
        () -> {
          SearchResult searchResult = engine.search(copy, side, limits);
          Platform.runLater(
              () -> {
                if (current != generation || result.isFinished()) {
                  return;
                }
                thinking = false;
                if (searchResult.move() != null && game.isLegal(searchResult.move())) {
                  game.play(searchResult.move());
                  afterMove();
                } else {
                  onChange.run();
                }
              });
        });
  }

  private void afterMove() {
    result = game.result();
    saveRecord();
    onChange.run();
    if (result.isFinished()) {
      finish();
    } else {
      think();
    }
  }

  private void finish() {
    if (!recorded) {
      recorded = true;
      context.recordResult(result, human);
    }
    onFinish.accept(result);
    onChange.run();
  }

  private void saveRecord() {
    GameRecord.save(game.moves(), result);
  }
}
