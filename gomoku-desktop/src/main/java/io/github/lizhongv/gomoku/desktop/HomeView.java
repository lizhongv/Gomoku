package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.Stone;
import java.util.stream.IntStream;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** 首页：开始对局、继续上局、复盘与战绩。 */
public final class HomeView extends VBox {

  private final AppShell shell;

  /** 创建首页。 */
  public HomeView(AppShell shell) {
    this.shell = shell;
    AppContext context = shell.context();
    I18n i18n = context.i18n();
    getStyleClass().add("home-view");
    setPadding(new Insets(36, 48, 36, 48));
    setSpacing(18);

    Label title = new Label(i18n.t("app.title"));
    title.getStyleClass().add("hero-title");
    Label subtitle = new Label(i18n.t("home.subtitle"));
    subtitle.getStyleClass().add("hero-subtitle");
    subtitle.setWrapText(true);

    ComboBox<Integer> levels = new ComboBox<>();
    levels.getItems().addAll(IntStream.rangeClosed(1, 12).boxed().toList());
    levels.setValue(context.level());
    levels.getStyleClass().add("level-select");
    Label levelHint = new Label(i18n.t("home.level.hint", context.level()));
    levelHint.getStyleClass().add("hint");
    levels
        .valueProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              context.setLevel(newValue);
              levelHint.setText(i18n.t("home.level.hint", newValue));
            });

    Button start = new Button(i18n.t("home.start"));
    start.getStyleClass().add("primary-button");
    start.setOnAction(event -> shell.startGame(context.level(), context.humanColor()));

    Button resume = new Button(i18n.t("home.continue"));
    resume.getStyleClass().add("secondary-button");
    resume.setDisable(GameRecord.load().filter(GameRecord::unfinished).isEmpty());
    resume.setOnAction(
        event -> GameRecord.load().filter(GameRecord::unfinished).ifPresent(shell::resume));

    Button review = new Button(i18n.t("home.review"));
    review.getStyleClass().add("secondary-button");
    review.setDisable(GameRecord.load().isEmpty());
    review.setOnAction(
        event ->
            GameRecord.load()
                .ifPresent(record -> shell.showReview(record.moves(), record.result())));

    HBox actions = new HBox(12, start, resume, review);
    actions.setAlignment(Pos.CENTER_LEFT);

    Label badge = new Label(i18n.t("home.badge"));
    badge.getStyleClass().add("badge");
    HBox stats =
        new HBox(
            10,
            statCard(i18n.t("home.wins"), context.wins()),
            statCard(i18n.t("home.losses"), context.losses()),
            statCard(i18n.t("home.draws"), context.draws()));

    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    VBox levelRow = new VBox(6, new Label(i18n.t("home.level")), levels, levelHint);
    levelRow.getStyleClass().add("field-group");

    VBox hero = new VBox(12, badge, title, subtitle, levelRow, actions, stats);
    hero.getStyleClass().add("hero-card");
    hero.setPadding(new Insets(32));
    hero.setMaxWidth(640);

    BoardCanvas decoration = new BoardCanvas();
    decoration.setWidth(320);
    decoration.setHeight(320);
    decoration.setReadOnly(true);
    decoration.setShowCoordinates(false);
    io.github.lizhongv.gomoku.core.Board preview = new io.github.lizhongv.gomoku.core.Board(15);
    int[][] stones = {{7, 7, 1}, {8, 8, 2}, {6, 8, 1}, {8, 6, 2}, {7, 9, 1}, {6, 6, 2}, {9, 7, 1}};
    for (int[] stone : stones) {
      preview.place(
          new io.github.lizhongv.gomoku.core.Move(stone[0], stone[1]),
          stone[2] == 1 ? Stone.BLACK : Stone.WHITE);
    }
    decoration.setBoard(preview);
    decoration.getStyleClass().add("decor-board");

    HBox body = new HBox(40, hero, decoration);
    body.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(hero, Priority.ALWAYS);

    getChildren().addAll(body, spacer);
  }

  private VBox statCard(String label, int value) {
    Label number = new Label(String.valueOf(value));
    number.getStyleClass().add("stat-value");
    Label caption = new Label(label);
    caption.getStyleClass().add("stat-label");
    VBox card = new VBox(2, number, caption);
    card.getStyleClass().add("stat-card");
    card.setAlignment(Pos.CENTER_LEFT);
    return card;
  }
}
