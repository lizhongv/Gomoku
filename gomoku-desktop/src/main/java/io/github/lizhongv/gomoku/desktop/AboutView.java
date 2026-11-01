package io.github.lizhongv.gomoku.desktop;

import java.awt.Desktop;
import java.net.URI;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** 关于页：版本、引擎能力、许可证与仓库地址。 */
public final class AboutView extends VBox {

  /** 创建关于页。 */
  public AboutView(AppShell shell) {
    I18n i18n = shell.context().i18n();
    setPadding(new Insets(28, 40, 28, 40));
    setSpacing(12);
    Label title = new Label(i18n.t("about.title"));
    title.getStyleClass().add("page-title");

    Label version = new Label(i18n.t("about.version", "2.0.0"));
    Label engine = new Label(i18n.t("about.engine"));
    Label benchmark = new Label(i18n.t("about.benchmark"));
    Label license = new Label(i18n.t("about.license"));
    for (Label label : new Label[] {version, engine, benchmark, license}) {
      label.setWrapText(true);
    }

    Hyperlink link = new Hyperlink("github.com/lizhongv/Gomoku");
    link.setOnAction(
        event -> {
          try {
            if (Desktop.isDesktopSupported()) {
              Desktop.getDesktop().browse(URI.create("https://github.com/lizhongv/Gomoku"));
            }
          } catch (Exception ignored) {
            // 无法打开浏览器时忽略
          }
        });

    Button back = new Button(i18n.t("nav.home"));
    back.getStyleClass().add("secondary-button");
    back.setOnAction(event -> shell.showHome());

    getChildren().addAll(title, version, engine, benchmark, license, link, new HBox(back));
  }
}
