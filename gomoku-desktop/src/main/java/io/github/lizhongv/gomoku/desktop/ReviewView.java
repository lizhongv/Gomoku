package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.Stone;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** 复盘页：按手数回放棋谱，可逐手前进后退。 */
public final class ReviewView extends BorderPane {

  private final AppShell shell;

  private final List<Move> moves;

  private final GameResult result;

  private final I18n i18n;

  private final BoardCanvas canvas = new BoardCanvas();

  private final Slider slider = new Slider(0, 1, 1);

  private final Label counter = new Label();

  private final Label outcome = new Label();

  /** 创建复盘页。 */
  public ReviewView(AppShell shell, List<Move> moves, GameResult result) {
    this.shell = shell;
    this.moves = List.copyOf(moves);
    this.result = result;
    this.i18n = shell.context().i18n();
    getStyleClass().add("review-view");
    setPadding(new Insets(18, 24, 18, 24));

    canvas.setReadOnly(true);
    canvas.setShowCoordinates(shell.context().showCoordinates());
    BoardPane boardArea = new BoardPane(canvas);

    slider.setMax(Math.max(1, moves.size()));
    slider.setValue(moves.size());
    slider.setBlockIncrement(1);
    slider
        .valueProperty()
        .addListener((observable, oldValue, newValue) -> show(newValue.intValue()));

    Button first = navButton("review.first", 0);
    Button previous = navButton("review.previous", -1);
    Button next = navButton("review.next", 1);
    Button last = navButton("review.last", moves.size());
    Button back = new Button(i18n.t("nav.home"));
    back.getStyleClass().add("secondary-button");
    back.setOnAction(event -> shell.showHome());

    HBox controls = new HBox(8, first, previous, next, last, slider, counter, back);
    controls.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(slider, Priority.ALWAYS);
    controls.getStyleClass().add("review-bar");
    controls.setPadding(new Insets(10, 0, 0, 0));

    outcome.getStyleClass().add("page-subtitle");
    VBox bottom = new VBox(6, outcome, controls);

    setCenter(boardArea);
    setBottom(bottom);

    show(moves.size());
  }

  private Button navButton(String key, int delta) {
    Button button = new Button(i18n.t(key));
    button.getStyleClass().add("secondary-button");
    button.setOnAction(
        event -> {
          int target = delta == Integer.MIN_VALUE ? 0 : (int) Math.round(slider.getValue()) + delta;
          target = Math.max(0, Math.min(moves.size(), target));
          slider.setValue(target);
        });
    return button;
  }

  private void show(int index) {
    int count = Math.max(0, Math.min(moves.size(), index));
    Board board = new Board(15);
    Move last = null;
    for (int i = 0; i < count; i++) {
      Move move = moves.get(i);
      board.place(move, i % 2 == 0 ? Stone.BLACK : Stone.WHITE);
      last = move;
    }
    canvas.setBoard(board);
    canvas.setLastMove(last);
    counter.setText(i18n.t("review.counter", count, moves.size()));
    if (count == moves.size()) {
      outcome.setText(resultText());
    } else {
      outcome.setText(i18n.t("review.replaying"));
    }
  }

  private String resultText() {
    if (!result.isFinished()) {
      return i18n.t("review.unfinished");
    }
    if (result == GameResult.DRAW) {
      return i18n.t("game.draw");
    }
    return result.winner() == shell.context().humanColor()
        ? i18n.t("game.win")
        : i18n.t("game.lose");
  }
}
