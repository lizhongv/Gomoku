package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.Stone;
import java.util.stream.IntStream;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** 设置页：难度、主题、语言、执子与显示选项。 */
public final class SettingsView extends VBox {

  private final AppShell shell;

  /** 创建设置页。 */
  public SettingsView(AppShell shell) {
    this.shell = shell;
    AppContext context = shell.context();
    I18n i18n = context.i18n();
    getStyleClass().add("settings-view");
    setPadding(new Insets(28, 40, 28, 40));
    setSpacing(18);

    Label title = new Label(i18n.t("settings.title"));
    title.getStyleClass().add("page-title");

    ComboBox<Integer> level = new ComboBox<>();
    level.getItems().addAll(IntStream.rangeClosed(1, 12).boxed().toList());
    level.setValue(context.level());
    level
        .valueProperty()
        .addListener((observable, oldValue, newValue) -> context.setLevel(newValue));

    ComboBox<String> theme = new ComboBox<>();
    theme.getItems().addAll("wood", "dark", "jade");
    theme.setValue(context.theme());
    theme.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(String value) {
            return value == null ? "" : i18n.t("theme." + value);
          }

          @Override
          public String fromString(String text) {
            return text;
          }
        });
    theme
        .valueProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              context.setTheme(newValue);
              shell.applyTheme();
            });

    ComboBox<String> language = new ComboBox<>();
    language.getItems().addAll("zh", "en");
    language.setValue("en".equals(context.locale().getLanguage()) ? "en" : "zh");
    language.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(String value) {
            return "en".equals(value) ? "English" : "中文";
          }

          @Override
          public String fromString(String text) {
            return text;
          }
        });
    language
        .valueProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              context.setLanguage(newValue);
              shell.applyLanguage();
            });

    ComboBox<Stone> color = new ComboBox<>();
    color.getItems().addAll(Stone.BLACK, Stone.WHITE);
    color.setValue(context.humanColor());
    color.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(Stone value) {
            return value == Stone.WHITE
                ? i18n.t("settings.color.white")
                : i18n.t("settings.color.black");
          }

          @Override
          public Stone fromString(String text) {
            return Stone.BLACK;
          }
        });
    color
        .valueProperty()
        .addListener((observable, oldValue, newValue) -> context.setHumanColor(newValue));

    CheckBox sound = new CheckBox(i18n.t("settings.sound"));
    sound.setSelected(context.sound());
    sound
        .selectedProperty()
        .addListener((observable, oldValue, newValue) -> context.setSound(newValue));

    CheckBox coordinates = new CheckBox(i18n.t("settings.coordinates"));
    coordinates.setSelected(context.showCoordinates());
    coordinates
        .selectedProperty()
        .addListener((observable, oldValue, newValue) -> context.setShowCoordinates(newValue));

    GridPane form = new GridPane();
    form.getStyleClass().add("settings-card");
    form.setHgap(24);
    form.setVgap(16);
    form.setPadding(new Insets(24));
    int row = 0;
    addRow(form, row++, i18n.t("settings.level"), level, i18n.t("settings.level.hint"));
    addRow(form, row++, i18n.t("settings.theme"), theme, i18n.t("settings.theme.hint"));
    addRow(form, row++, i18n.t("settings.language"), language, i18n.t("settings.language.hint"));
    addRow(form, row++, i18n.t("settings.color"), color, i18n.t("settings.color.hint"));
    addRow(form, row++, i18n.t("settings.display"), new VBox(8, sound, coordinates), "");

    Button back = new Button(i18n.t("nav.home"));
    back.getStyleClass().add("secondary-button");
    back.setOnAction(event -> shell.showHome());

    HBox actions = new HBox(back);
    actions.setAlignment(Pos.CENTER_LEFT);

    getChildren().addAll(title, form, actions);
  }

  private static void addRow(
      GridPane form, int row, String label, javafx.scene.Node control, String hint) {
    Label name = new Label(label);
    name.getStyleClass().add("field-label");
    VBox value = new VBox(4, control);
    if (hint != null && !hint.isBlank()) {
      Label note = new Label(hint);
      note.getStyleClass().add("hint");
      note.setWrapText(true);
      value.getChildren().add(note);
    }
    form.add(name, 0, row);
    form.add(value, 1, row);
  }
}
