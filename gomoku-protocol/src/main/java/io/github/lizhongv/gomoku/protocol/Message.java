package io.github.lizhongv.gomoku.protocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 协议消息：类型加一组键值字段。
 *
 * <p>帧格式为一行 UTF-8 JSON：`{"type":"move","data":{"row":7,"col":7}}`。 不用 Java 序列化：跨语言、可读、无 gadget
 * 反序列化风险。
 */
public record Message(String type, Map<String, Object> data) {

  /** 创建消息；参数按 key, value 成对传入。 */
  public static Message of(String type, Object... pairs) {
    if (pairs.length % 2 != 0) {
      throw new IllegalArgumentException("字段须成对传入");
    }
    Map<String, Object> data = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      data.put(String.valueOf(pairs[i]), pairs[i + 1]);
    }
    return new Message(type, Map.copyOf(data));
  }

  /** 取字符串字段。 */
  public String text(String key) {
    Object value = data.get(key);
    return value == null ? "" : String.valueOf(value);
  }

  /** 取整数字段；缺失或格式错误返回默认值。 */
  public int integer(String key, int fallback) {
    Object value = data.get(key);
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  /** 取布尔字段。 */
  public boolean flag(String key) {
    Object value = data.get(key);
    return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
  }

  /** 取列表字段。 */
  @SuppressWarnings("unchecked")
  public List<Object> list(String key) {
    Object value = data.get(key);
    return value instanceof List<?> items ? (List<Object>) items : List.of();
  }
}
