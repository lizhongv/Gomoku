package io.github.lizhongv.gomoku.server;

import java.nio.file.Path;

/** 服务端入口：配置来自环境变量，便于容器与脚本部署。 */
public final class ServerMain {

  private ServerMain() {}

  /** 启动并阻塞。 */
  public static void main(String[] args) throws Exception {
    int port = intEnv("GOMOKU_PORT", 4802);
    int grace = intEnv("GOMOKU_RECONNECT_SECONDS", 60);
    Path data =
        Path.of(
            System.getenv()
                .getOrDefault(
                    "GOMOKU_DATA",
                    Path.of(System.getProperty("user.home"), ".gomoku-server").toString()));
    Server server = new Server(port, data, grace);
    Runtime.getRuntime().addShutdownHook(new Thread(server::close, "gomoku-shutdown"));
    System.out.printf(
        "Gomoku server listening on %d, data %s%n", server.port(), data.toAbsolutePath());
    Thread.currentThread().join();
  }

  private static int intEnv(String name, int fallback) {
    try {
      return Integer.parseInt(System.getenv().getOrDefault(name, String.valueOf(fallback)));
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }
}
