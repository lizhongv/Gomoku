package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.ai.Candidate;
import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.Move;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** 对局页：棋盘、状态、操作与着法列表。 */
public final class GameView extends BorderPane {

  private final AppShell shell;

  private final GameController controller;

  private final I18n i18n;

  private final BoardCanvas canvas = new BoardCanvas();

  private final BoardPane boardArea = new BoardPane(canvas);

  private final StackPane boardLayer = new StackPane();

  private final VBox overlay = new VBox(14);

  private final Label turnLabel = new Label();

  private final Label levelLabel = new Label();

  private final Label timeLabel = new Label();

  private final Label movesLabel = new Label();

  private final ListView<String> moveList = new ListView<>();

  private final Button undoButton;

  private final Button hintButton;

  private final Button resignButton;

  private final Label thinking = new Label();

  private final Timeline timer;

  private final PauseTransition hintClear = new PauseTransition(Duration.seconds(5));

  private final SoundPlayer sound = new SoundPlayer();

  private int lastHintCount = -1;

  /** 创建对局页。 */
  public GameView(AppShell shell, GameController controller) {
    this.shell = shell;
    this.controller = controller;
    this.i18n = shell.context().i18n();
    getStyleClass().add("game-view");

    canvas.setShowCoordinates(shell.context().showCoordinates());
    boardLayer.getChildren().addAll(boardArea, overlay);
    boardLayer.setPadding(new Insets(18));
    boardLayer.getStyleClass().add("board-area");
    overlay.setAlignment(Pos.CENTER);
    overlay.getStyleClass().add("result-overlay");
    overlay.setVisible(false);

    turnLabel.getStyleClass().add("turn-label");
    levelLabel.getStyleClass().add("hint");
    timeLabel.getStyleClass().add("hint");
    movesLabel.getStyleClass().add("hint");
    thinking.getStyleClass().add("hint");

    undoButton = action("game.undo", this::undo);
    hintButton = action("game.hint", this::hint);
    resignButton = action("game.resign", this::resign);
    Button restart = action("game.new", this::restart);
    Button review =
        action("game.review", () -> shell.showReview(controller.moves(), controller.result()));
    Button home = action("nav.home", this::leave);
    restart.getStyleClass().add("primary-button");

    GridPane actions = new GridPane();
    actions.setHgap(8);
    actions.setVgap(8);
    actions.add(restart, 0, 0);
    actions.add(review, 1, 0);
    actions.add(undoButton, 0, 1);
    actions.add(hintButton, 1, 1);
    actions.add(resignButton, 0, 2);
    actions.add(home, 1, 2);

    VBox panel =
        new VBox(12, turnLabel, thinking, levelLabel, timeLabel, movesLabel, actions, moveList);
    panel.setPrefWidth(300);
    panel.setMinWidth(260);
    panel.setPadding(new Insets(20));
    panel.getStyleClass().add("side-panel");
    VBox.setVgrow(moveList, Priority.ALWAYS);

    setCenter(boardLayer);
    setRight(panel);

    canvas.setOnPlace(
        (move, valid) -> {
          if (valid && controller.humanMove(move)) {
            sound.play("place");
          }
        });
    hintClear.setOnFinished(event -> canvas.setHints(List.of()));

    controller.setOnChange(this::refresh);
    controller.setOnFinish(this::onFinish);
    refresh();

    timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateClock()));
    timer.setCycleCount(Timeline.INDEFINITE);
    timer.play();
  }

  private Button action(String key, Runnable runnable) {
    Button button = new Button(i18n.t(key));
    button.getStyleClass().add("secondary-button");
    button.setMaxWidth(Double.MAX_VALUE);
    button.setOnAction(event -> runnable.run());
    return button;
  }

  private void refresh() {
    canvas.setBoard(controller.board());
    List<Move> moves = controller.moves();
    canvas.setLastMove(moves.isEmpty() ? null : moves.get(moves.size() - 1));
    if (moves.size() != lastHintCount) {
      canvas.setHints(List.of());
      lastHintCount = moves.size();
    }
    GameResult result = controller.result();
    if (result.isFinished()) {
      turnLabel.setText(resultText(result));
    } else if (controller.thinking()) {
      turnLabel.setText(i18n.t("game.aiThinking"));
    } else if (controller.turn() == controller.humanColor()) {
      turnLabel.setText(i18n.t("game.yourTurn"));
    } else {
      turnLabel.setText(i18n.t("game.aiTurn"));
    }
    thinking.setText(controller.thinking() ? i18n.t("game.thinkingHint") : "");
    levelLabel.setText(i18n.t("game.level", controller.level()));
    movesLabel.setText(i18n.t("game.moves", moves.size()));
    undoButton.setDisable(controller.thinking() || result.isFinished() || moves.isEmpty());
    hintButton.setDisable(!controller.humanTurn());
    resignButton.setDisable(result.isFinished());
    moveList
        .getItems()
        .setAll(
            java.util.stream.IntStream.range(0, moves.size())
                .mapToObj(index -> String.format("%d. %s", index + 1, coordinate(moves.get(index))))
                .toList());
    if (!moves.isEmpty()) {
      moveList.scrollTo(moves.size() - 1);
    }
    showResult(result);
  }

  private void showResult(GameResult result) {
    overlay.getChildren().clear();
    if (!result.isFinished()) {
      overlay.setVisible(false);
      return;
    }
    Label title = new Label(resultText(result));
    title.getStyleClass().add("result-title");
    Button again = new Button(i18n.t("game.again"));
    again.getStyleClass().add("primary-button");
    again.setOnAction(event -> shell.startGame(controller.level(), controller.humanColor()));
    Button review = new Button(i18n.t("game.review"));
    review.getStyleClass().add("secondary-button");
    review.setOnAction(event -> shell.showReview(controller.moves(), controller.result()));
    Button home = new Button(i18n.t("nav.home"));
    home.getStyleClass().add("secondary-button");
    home.setOnAction(event -> shell.showHome());
    overlay.getChildren().addAll(title, new VBox(8, again, review, home));
    overlay.setVisible(true);
  }

  private void onFinish(GameResult result) {
    timer.stop();
    if (!result.isFinished()) {
      return;
    }
    if (result.winner() == controller.humanColor()) {
      sound.play("win");
    } else if (result != GameResult.DRAW) {
      sound.play("lose");
    }
  }

  private void undo() {
    controller.undo();
  }

  private void hint() {
    controller.hint(
        candidates -> {
          canvas.setHints(candidates.stream().map(Candidate::move).toList());
          hintClear.playFromStart();
        });
  }

  private void resign() {
    if (confirm(i18n.t("game.resign"), i18n.t("game.resignConfirm"))) {
      controller.resign();
    }
  }

  private void restart() {
    if (controller.result().isFinished()
        || confirm(i18n.t("game.new"), i18n.t("game.restartConfirm"))) {
      shell.startGame(controller.level(), controller.humanColor());
    }
  }

  private void leave() {
    if (controller.result().isFinished()
        || confirm(i18n.t("nav.home"), i18n.t("game.leaveConfirm"))) {
      shell.showHome();
    }
  }

  private boolean confirm(String title, String message) {
    Alert alert =
        new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
    alert.setHeaderText(title);
    alert.setTitle(title);
    return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
  }

  private String resultText(GameResult result) {
    if (result == GameResult.DRAW) {
      return i18n.t("game.draw");
    }
    return result.winner() == controller.humanColor() ? i18n.t("game.win") : i18n.t("game.lose");
  }

  private String coordinate(Move move) {
    char column = (char) ('A' + move.col());
    return column + String.valueOf(controller.board().size() - move.row());
  }

  private void updateClock() {
    long seconds = (System.nanoTime() - startedAt) / 1_000_000_000L;
    timeLabel.setText(i18n.t("game.time", String.format("%02d:%02d", seconds / 60, seconds % 60)));
  }

  private final long startedAt = System.nanoTime();

  /** 页面销毁时停止计时与音效。 */
  public void dispose() {
    timer.stop();
    sound.close();
  }
}
