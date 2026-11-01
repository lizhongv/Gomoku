package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.Stone;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * 应用上下文：用户设置、语言与统计数据。
 *
 * <p>设置保存在用户目录下的 `.gomoku-v2/settings.properties`，不写注册表，卸载后不留系统残留。
 * 读取失败一律回退默认值，不阻止启动；只合并已知字段，忽略旧版本残留。
 */
public final class AppContext {

  private static final Path HOME = Path.of(System.getProperty("user.home"), ".gomoku-v2");

  private final Path file = HOME.resolve("settings.properties");

  private final Properties properties = new Properties();

  private I18n i18n;

  /** 创建上下文并加载设置。 */
  public AppContext() {
    load();
    i18n = new I18n(locale());
  }

  /** 当前等级 1～12。 */
  public int level() {
    return clamp(intValue("level", 6), 1, 12);
  }

  public void setLevel(int level) {
    properties.setProperty("level", String.valueOf(clamp(level, 1, 12)));
    save();
  }

  /** 主题名：wood、dark、jade。 */
  public String theme() {
    String theme = properties.getProperty("theme", "wood");
    return switch (theme) {
      case "dark", "jade" -> theme;
      default -> "wood";
    };
  }

  public void setTheme(String theme) {
    properties.setProperty("theme", theme);
    save();
  }

  /** 语言：zh 或 en。 */
  public Locale locale() {
    return "en".equalsIgnoreCase(properties.getProperty("language", "zh"))
        ? Locale.ENGLISH
        : Locale.SIMPLIFIED_CHINESE;
  }

  public void setLanguage(String language) {
    properties.setProperty("language", "en".equalsIgnoreCase(language) ? "en" : "zh");
    i18n = new I18n(locale());
    save();
  }

  /** 当前语言的文案。 */
  public I18n i18n() {
    return i18n;
  }

  /** 音效开关。 */
  public boolean sound() {
    return Boolean.parseBoolean(properties.getProperty("sound", "true"));
  }

  public void setSound(boolean sound) {
    properties.setProperty("sound", String.valueOf(sound));
    save();
  }

  /** 是否显示坐标。 */
  public boolean showCoordinates() {
    return Boolean.parseBoolean(properties.getProperty("coordinates", "true"));
  }

  public void setShowCoordinates(boolean show) {
    properties.setProperty("coordinates", String.valueOf(show));
    save();
  }

  /** 玩家执子颜色。 */
  public Stone humanColor() {
    return "white".equalsIgnoreCase(properties.getProperty("humanColor", "black"))
        ? Stone.WHITE
        : Stone.BLACK;
  }

  public void setHumanColor(Stone color) {
    properties.setProperty("humanColor", color == Stone.WHITE ? "white" : "black");
    save();
  }

  /** 胜 / 负 / 和。 */
  public int wins() {
    return intValue("wins", 0);
  }

  public int losses() {
    return intValue("losses", 0);
  }

  public int draws() {
    return intValue("draws", 0);
  }

  /** 记录一局结果。 */
  public void recordResult(io.github.lizhongv.gomoku.core.GameResult result, Stone humanColor) {
    if (result == io.github.lizhongv.gomoku.core.GameResult.DRAW) {
      properties.setProperty("draws", String.valueOf(draws() + 1));
    } else if (result.winner() == humanColor) {
      properties.setProperty("wins", String.valueOf(wins() + 1));
    } else if (result.isFinished()) {
      properties.setProperty("losses", String.valueOf(losses() + 1));
    }
    save();
  }

  /** 用户数据目录，用于棋谱与截图。 */
  public static Path dataDir() {
    try {
      Files.createDirectories(HOME);
    } catch (IOException ignored) {
      // 目录不可写时由调用方处理，读取设置仍可继续
    }
    return HOME;
  }

  private void load() {
    if (!Files.exists(file)) {
      return;
    }
    try (InputStream in = Files.newInputStream(file)) {
      properties.load(in);
    } catch (IOException ignored) {
      // 设置损坏时使用默认值
    }
  }

  private void save() {
    try {
      Files.createDirectories(file.getParent());
      try (OutputStream out = Files.newOutputStream(file)) {
        properties.store(out, "Gomoku settings");
      }
    } catch (IOException ignored) {
      // 保存失败不打断对局
    }
  }

  private int intValue(String key, int fallback) {
    try {
      return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback)));
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
