package io.github.lizhongv.gomoku.desktop;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** 国际化文案。 */
public final class I18n {

  private final ResourceBundle bundle;

  /** 按语言加载文案；缺失的语言回退到中文。 */
  public I18n(Locale locale) {
    this.bundle = ResourceBundle.getBundle("i18n.messages", locale);
  }

  /** 取文案并格式化参数。 */
  public String t(String key, Object... args) {
    String pattern = bundle.containsKey(key) ? bundle.getString(key) : key;
    return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
  }
}
