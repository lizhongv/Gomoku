package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.Stone;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** 应用外壳：顶部导航与内容区，负责页面切换与对局生命周期。 */
public final class AppShell {

  private final AppContext context;

  private final Scene scene;

  private final BorderPane root = new BorderPane();

  private final StackPane content = new StackPane();

  private final HBox nav = new HBox(8);

  private final Label brand = new Label();

  private GameController controller;

  /** 创建外壳。 */
  public AppShell(AppContext context, Scene scene) {
    this.context = context;
    this.scene = scene;
    root.getStyleClass().add("app-root");
    buildNav();
    root.setTop(nav);
    root.setCenter(content);
    showHome();
  }

  /** 根节点。 */
  public Parent root() {
    return root;
  }

  /** 设置。 */
  public AppContext context() {
    return context;
  }

  /** 首页。 */
  public void showHome() {
    closeController();
    content.getChildren().setAll(new HomeView(this));
  }

  /** 新开一局。 */
  public void startGame(int level, Stone human) {
    closeController();
    controller = new GameController(context, level, human);
    showGame();
    controller.start();
  }

  /** 继续一局未下完的棋。 */
  public void resume(GameRecord record) {
    closeController();
    controller =
        GameController.replay(context, context.level(), context.humanColor(), record.moves());
    if (!record.unfinished()) {
      showReview(record.moves(), record.result());
      return;
    }
    showGame();
    if (controller.turn() != controller.humanColor()) {
      controller.start();
    }
  }

  /** 当前对局控制器；无对局时为 null。 */
  public GameController controller() {
    return controller;
  }

  private void showGame() {
    if (controller == null) {
      showHome();
      return;
    }
    content.getChildren().setAll(new GameView(this, controller));
  }

  /** 复盘。 */
  public void showReview(List<Move> moves, GameResult result) {
    closeController();
    content.getChildren().setAll(new ReviewView(this, moves, result));
  }

  /** 残局。 */
  public void showPuzzles() {
    closeController();
    content.getChildren().setAll(new PuzzleView(this));
  }

  /** 联机。 */
  public void showOnline() {
    closeController();
    content.getChildren().setAll(new OnlineView(this));
  }

  /** 设置。 */
  public void showSettings() {
    closeController();
    content.getChildren().setAll(new SettingsView(this));
  }

  /** 关于。 */
  public void showAbout() {
    closeController();
    content.getChildren().setAll(new AboutView(this));
  }

  /** 主题切换后重新应用样式。 */
  public void applyTheme() {
    scene
        .getStylesheets()
        .setAll(resource("/styles/base.css"), resource("/styles/" + context.theme() + ".css"));
  }

  /** 语言切换后重建外壳与当前页面。 */
  public void applyLanguage() {
    buildNav();
    showSettings();
  }

  /** 关闭当前对局的后台线程，并停止对局页的计时与音效。 */
  public void closeController() {
    if (content.getChildren().size() == 1) {
      if (content.getChildren().get(0) instanceof GameView gameView) {
        gameView.dispose();
      } else if (content.getChildren().get(0) instanceof OnlineView onlineView) {
        onlineView.dispose();
      } else if (content.getChildren().get(0) instanceof PuzzleView puzzleView) {
        puzzleView.dispose();
      }
    }
    if (controller != null) {
      controller.close();
      controller = null;
    }
  }

  private void buildNav() {
    brand.setText(context.i18n().t("app.title"));
    brand.getStyleClass().add("brand");
    Button home = navButton("nav.home", this::showHome);
    Button puzzles = navButton("nav.puzzles", this::showPuzzles);
    Button online = navButton("nav.online", this::showOnline);
    Button settings = navButton("nav.settings", this::showSettings);
    Button about = navButton("nav.about", this::showAbout);
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    nav.getChildren().setAll(brand, spacer, home, puzzles, online, settings, about);
    nav.getStyleClass().add("top-nav");
    nav.setAlignment(Pos.CENTER_LEFT);
    nav.setPadding(new Insets(12, 18, 12, 18));
  }

  private Button navButton(String key, Runnable action) {
    Button button = new Button(context.i18n().t(key));
    button.getStyleClass().add("nav-button");
    button.setOnAction(event -> action.run());
    return button;
  }

  private String resource(String path) {
    return AppShell.class.getResource(path).toExternalForm();
  }

  /** 用同一套样式生成一个页面容器。 */
  static VBox page(String title, String subtitle) {
    Label heading = new Label(title);
    heading.getStyleClass().add("page-title");
    VBox box = new VBox(10);
    box.getChildren().add(heading);
    if (subtitle != null && !subtitle.isBlank()) {
      Label sub = new Label(subtitle);
      sub.getStyleClass().add("page-subtitle");
      sub.setWrapText(true);
      box.getChildren().add(sub);
    }
    box.setPadding(new Insets(24, 32, 24, 32));
    return box;
  }
}
