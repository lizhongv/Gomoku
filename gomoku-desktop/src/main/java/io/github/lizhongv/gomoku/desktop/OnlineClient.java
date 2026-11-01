package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.protocol.Codec;
import io.github.lizhongv.gomoku.protocol.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 联机客户端：后台线程读取协议帧，回到 JavaFX 线程回调。
 *
 * <p>只负责传输，不做规则判断；棋盘状态以服务端下发的 STATE 为准。
 */
final class OnlineClient implements AutoCloseable {

  private final Socket socket;

  private final InputStream input;

  private final OutputStream output;

  private final Thread reader;

  private volatile boolean running = true;

  OnlineClient(
      String host, int port, boolean tls, Consumer<Message> onMessage, Consumer<String> onClosed)
      throws IOException {
    socket = tls ? javax.net.ssl.SSLSocketFactory.getDefault().createSocket() : new Socket();
    socket.connect(new InetSocketAddress(host, port), 3_000);
    socket.setSoTimeout(0);
    input = socket.getInputStream();
    output = socket.getOutputStream();
    reader = new Thread(() -> readLoop(onMessage, onClosed), "gomoku-online");
    reader.setDaemon(true);
    reader.start();
  }

  /** 发送一条消息。 */
  synchronized void send(Message message) throws IOException {
    output.write((Codec.encode(message) + "\n").getBytes(StandardCharsets.UTF_8));
    output.flush();
  }

  /** 是否已关闭。 */
  boolean running() {
    return running;
  }

  private void readLoop(Consumer<Message> onMessage, Consumer<String> onClosed) {
    try {
      String line;
      while (running && (line = Codec.readLine(input)) != null) {
        if (line.isBlank()) {
          continue;
        }
        Message message = Codec.decode(line);
        javafx.application.Platform.runLater(() -> onMessage.accept(message));
      }
    } catch (IOException | RuntimeException error) {
      if (running) {
        javafx.application.Platform.runLater(() -> onClosed.accept(error.getMessage()));
      }
    } finally {
      running = false;
    }
  }

  @Override
  public void close() {
    running = false;
    try {
      socket.close();
    } catch (IOException ignored) {
      // 已关闭
    }
  }
}
