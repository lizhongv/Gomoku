package io.github.lizhongv.gomoku.server;

import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.protocol.Message;
import io.github.lizhongv.gomoku.protocol.Types;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 联机服务端：账号、房间与权威棋局。
 *
 * <p>服务端持有唯一的 {@link Room} 与棋盘，客户端只能请求落子；胜负由服务端判定。 断线后保留座位 {@code reconnectSeconds} 秒，超时判负；期间可用
 * token 重连恢复。
 */
public final class Server implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private static final SecureRandom RANDOM = new SecureRandom();

  private final int reconnectSeconds;

  private final int turnSeconds;

  private final UserStore users;

  private final GameStore games;

  private final ServerSocket serverSocket;

  private final ExecutorService pool =
      Executors.newCachedThreadPool(
          runnable -> {
            Thread thread = new Thread(runnable, "gomoku-client");
            thread.setDaemon(true);
            return thread;
          });

  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "gomoku-timer");
            thread.setDaemon(true);
            return thread;
          });

  private final Map<String, Room> rooms = new ConcurrentHashMap<>();

  private final Map<String, String> tokens = new ConcurrentHashMap<>();

  private final Map<String, ClientConnection> online = new ConcurrentHashMap<>();

  private final java.util.Set<ClientConnection> connections = ConcurrentHashMap.newKeySet();

  private final Map<String, String> playerRoom = new ConcurrentHashMap<>();

  private final Thread acceptThread;

  private volatile boolean running = true;

  /** 启动服务端；端口传 0 时由系统分配。 */
  public Server(int port, Path dataDir, int reconnectSeconds) throws IOException {
    this(port, dataDir, reconnectSeconds, 60);
  }

  /** 指定断线宽限与每手超时秒数。 */
  public Server(int port, Path dataDir, int reconnectSeconds, int turnSeconds) throws IOException {
    this.reconnectSeconds = reconnectSeconds;
    this.turnSeconds = Math.max(5, turnSeconds);
    this.users = new UserStore(dataDir);
    this.games = new GameStore(dataDir);
    this.serverSocket = new ServerSocket(port);
    this.acceptThread = new Thread(this::acceptLoop, "gomoku-accept");
    this.acceptThread.setDaemon(true);
    this.acceptThread.start();
    LOG.info("服务端已启动，端口 {}", port());
  }

  /** 实际监听端口。 */
  public int port() {
    return serverSocket.getLocalPort();
  }

  /** 断线重连宽限秒数。 */
  int reconnectSeconds() {
    return reconnectSeconds;
  }

  /** 每手超时秒数。 */
  int turnSeconds() {
    return turnSeconds;
  }

  UserStore.User register(String username, String password, String display) {
    return users.register(username, password, display);
  }

  UserStore.User authenticate(String username, String password) {
    return users.authenticate(username, password);
  }

  /** 登录成功后签发 token；同一账号已在线时返回 null。 */
  String issueToken(UserStore.User user) {
    if (online.containsKey(user.name())) {
      return null;
    }
    byte[] bytes = new byte[24];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    tokens.put(token, user.name());
    return token;
  }

  UserStore.User userForToken(String token) {
    String name = tokens.get(token);
    return name == null ? null : users.find(name);
  }

  void bind(ClientConnection connection) {
    ClientConnection previous = online.put(connection.user().name(), connection);
    if (previous != null && previous != connection) {
      previous.close();
    }
  }

  void createRoom(ClientConnection connection) {
    String id;
    do {
      id = String.format("%04X", RANDOM.nextInt(0x10000));
    } while (rooms.containsKey(id));
    Room room = new Room(this, id, connection);
    rooms.put(id, room);
    playerRoom.put(connection.user().name(), id);
    connection.attach(room);
    connection.send(Message.of(Types.ROOM_JOINED, "room", id, "color", "BLACK", "opponent", ""));
  }

  void joinRoom(ClientConnection connection, String roomId) {
    Room room = rooms.get(roomId);
    if (room == null) {
      connection.error("NO_ROOM", "房间不存在");
      return;
    }
    if (room.join(connection)) {
      playerRoom.put(connection.user().name(), roomId);
    }
  }

  void restoreRoom(ClientConnection connection) {
    String roomId = playerRoom.get(connection.user().name());
    Room room = roomId == null ? null : rooms.get(roomId);
    if (room != null && room.reconnect(connection)) {
      return;
    }
    connection.send(Message.of(Types.ROOMS, "rooms", roomListData()));
  }

  Message roomList() {
    return Message.of(Types.ROOMS, "rooms", roomListData());
  }

  private List<Object> roomListData() {
    List<Object> list = new ArrayList<>();
    for (Room room : rooms.values()) {
      if (room.status() == Room.Status.WAITING) {
        list.add(Map.of("id", room.id(), "black", room.blackName(), "white", room.whiteName()));
      }
    }
    return list;
  }

  void roomStarted(Room room) {
    if (!room.whiteName().isEmpty()) {
      playerRoom.put(room.whiteName(), room.id());
    }
  }

  Map<String, Integer> recordGame(Room room, List<Move> moves, GameResult result) {
    games.append(room.id(), room.blackName(), room.whiteName(), moves, result);
    return users.recordGame(room.blackName(), room.whiteName(), result);
  }

  /** 快速匹配：加入最早的空房间，没有则新建。 */
  void quickMatch(ClientConnection connection) {
    for (Room room : rooms.values()) {
      if (room.status() == Room.Status.WAITING
          && !room.blackName().equals(connection.user().name())) {
        joinRoom(connection, room.id());
        return;
      }
    }
    createRoom(connection);
  }

  java.util.concurrent.ScheduledFuture<?> scheduleTurn(Room room, int seconds) {
    return scheduler.schedule(room::turnTimeout, seconds, TimeUnit.SECONDS);
  }

  void dropRoom(Room room) {
    rooms.remove(room.id());
    playerRoom.remove(room.blackName(), room.id());
    if (!room.whiteName().isEmpty()) {
      playerRoom.remove(room.whiteName(), room.id());
    }
  }

  void scheduleForfeit(Room room, String username, int seconds) {
    scheduler.schedule(() -> room.forfeit(username), seconds, TimeUnit.SECONDS);
  }

  void disconnected(ClientConnection connection) {
    if (connection.user() == null) {
      return;
    }
    connections.remove(connection);
    online.remove(connection.user().name(), connection);
    Room room = connection.room();
    if (room != null) {
      room.disconnected(connection);
    }
  }

  /** 关闭服务端与所有连接。 */
  @Override
  public void close() {
    running = false;
    try {
      serverSocket.close();
    } catch (IOException ignored) {
      // 已关闭
    }
    for (ClientConnection connection : java.util.List.copyOf(connections)) {
      connection.close();
    }
    pool.shutdownNow();
    scheduler.shutdownNow();
  }

  private void acceptLoop() {
    while (running) {
      try {
        Socket socket = serverSocket.accept();
        ClientConnection connection = new ClientConnection(this, socket);
        connections.add(connection);
        pool.submit(connection);
      } catch (IOException error) {
        if (running) {
          LOG.warn("接受连接失败：{}", error.getMessage());
        }
        return;
      }
    }
  }
}
