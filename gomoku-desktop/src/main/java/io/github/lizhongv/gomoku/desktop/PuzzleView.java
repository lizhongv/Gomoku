package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.ai.Candidate;
import io.github.lizhongv.gomoku.ai.GomokuEngine;
import io.github.lizhongv.gomoku.ai.SearchLimits;
import io.github.lizhongv.gomoku.ai.SearchResult;
import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.core.WinDetector;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** 残局页：选择题目、人机验证、提示与重来。 */
public final class PuzzleView extends BorderPane {

  private final AppShell shell;

  private final I18n i18n;

  private final List<PuzzleLibrary.Puzzle> puzzles = PuzzleLibrary.all();

  private final ListView<String> list = new ListView<>();

  private final BoardCanvas canvas = new BoardCanvas();

  private final Label title = new Label();

  private final Label objective = new Label();

  private final Label status = new Label();

  private final GomokuEngine engine = new GomokuEngine(15, RuleSet.FREESTYLE);

  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "gomoku-puzzle");
            thread.setDaemon(true);
            return thread;
          });

  private Board board;

  private PuzzleLibrary.Puzzle current;

  private int humanMoves;

  private boolean busy;

  /** 创建残局页。 */
  public PuzzleView(AppShell shell) {
    this.shell = shell;
    this.i18n = shell.context().i18n();
    setPadding(new Insets(16, 20, 16, 20));

    list.getItems()
        .addAll(puzzles.stream().map(puzzle -> puzzle.id() + " · " + puzzle.name()).toList());
    list.getSelectionModel()
        .selectedIndexProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (newValue.intValue() >= 0) {
                load(newValue.intValue());
              }
            });
    list.setPrefWidth(190);

    canvas.setOnPlace(
        (move, valid) -> {
          if (valid && !busy) {
            playHuman(move);
          }
        });

    title.getStyleClass().add("page-title");
    objective.getStyleClass().add("hint");
    status.getStyleClass().add("hint");

    Button hint = new Button(i18n.t("puzzles.hint"));
    hint.getStyleClass().add("secondary-button");
    hint.setOnAction(event -> hint());
    Button reset = new Button(i18n.t("puzzles.reset"));
    reset.getStyleClass().add("secondary-button");
    reset.setOnAction(event -> load(list.getSelectionModel().getSelectedIndex()));
    Button next = new Button(i18n.t("puzzles.next"));
    next.getStyleClass().add("secondary-button");
    next.setOnAction(event -> list.getSelectionModel().selectNext());
    Button back = new Button(i18n.t("nav.home"));
    back.getStyleClass().add("secondary-button");
    back.setOnAction(event -> shell.showHome());

    VBox panel = new VBox(10, title, objective, status, new HBox(8, hint, reset, next), back);
    panel.setPrefWidth(250);
    panel.setPadding(new Insets(0, 0, 0, 12));

    BoardPane boardPane = new BoardPane(canvas);
    HBox body = new HBox(12, list, boardPane, panel);
    HBox.setHgrow(boardPane, Priority.ALWAYS);
    setCenter(body);
    if (!puzzles.isEmpty()) {
      list.getSelectionModel().select(0);
    }
  }

  private void load(int index) {
    if (index < 0 || index >= puzzles.size()) {
      return;
    }
    current = puzzles.get(index);
    board = current.board().copy();
    humanMoves = 0;
    busy = false;
    canvas.setBoard(board);
    canvas.setReadOnly(false);
    canvas.setLastMove(null);
    canvas.setHints(List.of());
    title.setText(current.name());
    objective.setText(i18n.t("puzzles.objective", current.maxMoves()));
    status.setText(i18n.t("puzzles.prompt"));
  }

  private void playHuman(Move move) {
    if (board.get(move) != Stone.EMPTY) {
      return;
    }
    board.place(move, Stone.BLACK);
    humanMoves++;
    canvas.setLastMove(move);
    if (WinDetector.isWin(board, move, Stone.BLACK, RuleSet.FREESTYLE)) {
      status.setText(i18n.t("puzzles.success"));
      canvas.setReadOnly(true);
      return;
    }
    if (humanMoves >= current.maxMoves()) {
      status.setText(i18n.t("puzzles.failed"));
      canvas.setReadOnly(true);
      return;
    }
    busy = true;
    status.setText(i18n.t("puzzles.thinking"));
    Board copy = board.copy();
    executor.submit(
        () -> {
          SearchResult result =
              engine.search(copy, Stone.WHITE, new SearchLimits(400, 4, 12, 0, true));
          Platform.runLater(
              () -> {
                busy = false;
                if (result.move() != null
                    && board.isEmpty(result.move().row(), result.move().col())) {
                  board.place(result.move(), Stone.WHITE);
                  canvas.setLastMove(result.move());
                  if (WinDetector.isWin(board, result.move(), Stone.WHITE, RuleSet.FREESTYLE)) {
                    status.setText(i18n.t("puzzles.failed"));
                    canvas.setReadOnly(true);
                    return;
                  }
                }
                canvas.setBoard(board);
                status.setText(i18n.t("puzzles.prompt"));
              });
        });
  }

  private void hint() {
    if (board == null || busy) {
      return;
    }
    Board copy = board.copy();
    executor.submit(
        () -> {
          List<Candidate> candidates =
              engine.analyze(copy, Stone.BLACK, 3, new SearchLimits(300, 4, 12, 0, false));
          Platform.runLater(
              () -> canvas.setHints(candidates.stream().map(Candidate::move).toList()));
        });
  }

  /** 离开页面时释放线程。 */
  public void dispose() {
    executor.shutdownNow();
  }
}
