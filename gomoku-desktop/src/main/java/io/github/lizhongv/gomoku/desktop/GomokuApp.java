package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.Move;
import java.io.File;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;

/** 桌面客户端入口。 */
public final class GomokuApp extends Application {

  /** 应用版本，与 pom.xml 保持一致。 */
  public static final String VERSION = "2.0.0";

  @Override
  public void start(Stage stage) {
    AppContext context = new AppContext();
    Scene scene = new Scene(new javafx.scene.layout.StackPane(), 1180, 780);
    AppShell shell = new AppShell(context, scene);
    scene.setRoot(shell.root());
    shell.applyTheme();
    stage.setScene(scene);
    stage.setTitle(context.i18n().t("app.title"));
    stage.setMinWidth(960);
    stage.setMinHeight(680);
    stage.show();
    String screenshotDir = screenshotDirectory();
    if (screenshotDir != null && !screenshotDir.isBlank()) {
      capture(stage, shell, screenshotDir);
    }
  }

  /** 截图目录：优先命令行参数 --screenshot=DIR，其次环境变量 GOMOKU_SCREENSHOT。 */
  private String screenshotDirectory() {
    for (String argument : getParameters().getRaw()) {
      if (argument.startsWith("--screenshot=")) {
        return argument.substring("--screenshot=".length());
      }
    }
    return System.getenv("GOMOKU_SCREENSHOT");
  }

  /** 无交互截图模式：依次渲染首页、设置、对局与复盘，供文档与界面回归使用。 */
  private void capture(Stage stage, AppShell shell, String directory) {
    File folder = new File(directory);
    folder.mkdirs();
    after(
        stage,
        800,
        () -> {
          save(stage, folder, "home");
          shell.showSettings();
        });
    after(
        stage,
        1400,
        () -> {
          save(stage, folder, "settings");
          shell.startGame(6, io.github.lizhongv.gomoku.core.Stone.BLACK);
          if (shell.controller() != null) {
            shell.controller().humanMove(new Move(7, 7));
          }
        });
    after(stage, 2200, () -> save(stage, folder, "game"));
    after(
        stage,
        2600,
        () -> {
          List<Move> moves =
              List.of(
                  new Move(7, 7), new Move(8, 8), new Move(7, 8), new Move(8, 7), new Move(6, 8));
          shell.showReview(moves, GameResult.ONGOING);
        });
    after(
        stage,
        3200,
        () -> {
          save(stage, folder, "review");
          shell.showOnline();
        });
    after(
        stage,
        3800,
        () -> {
          save(stage, folder, "online");
          shell.showPuzzles();
        });
    after(
        stage,
        4400,
        () -> {
          save(stage, folder, "puzzles");
          Platform.exit();
        });
  }

  private void after(Stage stage, int millis, Runnable action) {
    PauseTransition pause = new PauseTransition(Duration.millis(millis));
    pause.setOnFinished(event -> action.run());
    pause.play();
  }

  private void save(Stage stage, File folder, String name) {
    try {
      stage.getScene().getRoot().applyCss();
      stage.getScene().getRoot().layout();
      WritableImage image = stage.getScene().snapshot(null);
      ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new File(folder, name + ".png"));
    } catch (Exception ignored) {
      // 截图失败不影响应用运行
    }
  }

  /** 入口。 */
  public static void main(String[] args) {
    launch(args);
  }
}
