package io.github.lizhongv.gomoku.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 帧编解码：一行一条 JSON，长度上限 64 KB。
 *
 * <p>读取时逐字节检查长度，避免对端发送超长行导致内存耗尽；写入后换行并刷新。
 */
public final class Codec {

  /** 协议版本。不兼容的改动须加一。 */
  public static final int VERSION = 2;

  /** 单帧最大字节数。 */
  public static final int MAX_FRAME = 64 * 1024;

  private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
      new com.fasterxml.jackson.databind.ObjectMapper();

  private Codec() {}

  /** 编码为一行 JSON。 */
  public static String encode(Message message) {
    try {
      return MAPPER.writeValueAsString(message);
    } catch (IOException error) {
      throw new IllegalStateException("消息无法编码", error);
    }
  }

  /** 解析一行 JSON；格式错误抛出 IllegalArgumentException。 */
  public static Message decode(String line) {
    try {
      return MAPPER.readValue(line, Message.class);
    } catch (IOException error) {
      throw new IllegalArgumentException("消息格式错误", error);
    }
  }

  /**
   * 读取一行，超过上限时抛异常。
   *
   * @return 一行文本（不含换行）；输入结束返回 null
   */
  public static String readLine(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
    int current;
    boolean any = false;
    while ((current = input.read()) != -1) {
      any = true;
      if (current == '\n') {
        break;
      }
      if (current != '\r') {
        buffer.write(current);
      }
      if (buffer.size() > MAX_FRAME) {
        throw new IOException("消息超过 " + MAX_FRAME + " 字节");
      }
    }
    if (!any) {
      return null;
    }
    return buffer.toString(StandardCharsets.UTF_8);
  }
}
