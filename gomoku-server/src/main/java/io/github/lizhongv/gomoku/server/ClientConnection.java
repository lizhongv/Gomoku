package io.github.lizhongv.gomoku.server;

import io.github.lizhongv.gomoku.protocol.Codec;
import io.github.lizhongv.gomoku.protocol.Message;
import io.github.lizhongv.gomoku.protocol.Types;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 一个客户端连接：读取帧、校验登录状态、分发请求。
 *
 * <p>单连接单线程读取；发送加锁，避免两个房间广播同时写同一个流造成帧交错。 每连接限制每秒消息数，防止恶意刷屏。
 */
final class ClientConnection implements Runnable, AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(ClientConnection.class);

  private static final int MAX_MESSAGES_PER_SECOND = 40;

  private final Server server;

  private final Socket socket;

  private final InputStream input;

  private final OutputStream output;

  private final Deque<Long> recent = new ArrayDeque<>();

  private UserStore.User user;

  private String token = "";

  private Room room;

  ClientConnection(Server server, Socket socket) throws IOException {
    this.server = server;
    this.socket = socket;
    this.input = socket.getInputStream();
    this.output = socket.getOutputStream();
    socket.setSoTimeout(0);
  }

  UserStore.User user() {
    return user;
  }

  String token() {
    return token;
  }

  Room room() {
    return room;
  }

  void attach(Room room) {
    this.room = room;
  }

  /** 异步发送；连接已关闭时静默丢弃。 */
  synchronized void send(Message message) {
    try {
      output.write((Codec.encode(message) + "\n").getBytes(StandardCharsets.UTF_8));
      output.flush();
    } catch (IOException error) {
      LOG.debug("发送失败：{}", error.getMessage());
    }
  }

  /** 发送错误。 */
  void error(String code, String message) {
    send(Message.of(Types.ERROR, "code", code, "message", message));
  }

  @Override
  public void run() {
    try {
      String line;
      while ((line = Codec.readLine(input)) != null) {
        if (!allowMessage()) {
          error("RATE_LIMIT", "消息过于频繁");
          break;
        }
        if (line.isBlank()) {
          continue;
        }
        try {
          handle(Codec.decode(line));
        } catch (IllegalArgumentException error) {
          error("BAD_MESSAGE", "消息格式错误");
        } catch (RuntimeException error) {
          LOG.warn("处理消息异常", error);
          error("SERVER_ERROR", "服务器处理失败");
        }
      }
    } catch (IOException error) {
      LOG.debug("连接断开：{}", error.getMessage());
    } finally {
      close();
    }
  }

  private boolean allowMessage() {
    long now = System.currentTimeMillis();
    recent.addLast(now);
    while (!recent.isEmpty() && now - recent.peekFirst() > 1000) {
      recent.removeFirst();
    }
    return recent.size() <= MAX_MESSAGES_PER_SECOND;
  }

  private void handle(Message message) {
    switch (message.type()) {
      case Types.HELLO -> hello(message);
      case Types.CAPTCHA -> captcha();
      case Types.REGISTER -> register(message);
      case Types.LOGIN -> login(message);
      case Types.RECONNECT -> reconnect(message);
      case Types.LIST_ROOMS -> send(server.roomList());
      case Types.CREATE_ROOM -> createRoom();
      case Types.QUICK_MATCH -> quickMatch();
      case Types.JOIN_ROOM -> joinRoom(message);
      case Types.MOVE -> move(message);
      case Types.CHAT -> chat(message);
      case Types.RESIGN -> resign();
      case Types.LEAVE -> leave();
      case Types.PING -> send(Message.of(Types.PONG));
      default -> error("UNKNOWN_TYPE", "未知消息类型：" + message.type());
    }
  }

  private void hello(Message message) {
    int version = message.integer("version", 0);
    if (version != Codec.VERSION) {
      error("VERSION_MISMATCH", "客户端协议版本不兼容，请升级");
      return;
    }
    send(Message.of(Types.WELCOME, "version", Codec.VERSION, "server", "gomoku-server"));
  }

  private void captcha() {
    Server.Captcha challenge = server.issueCaptcha();
    send(Message.of(Types.CAPTCHA, "id", challenge.id(), "question", challenge.question()));
  }

  private void register(Message message) {
    if (user != null) {
      error("ALREADY_LOGGED_IN", "已经登录");
      return;
    }
    try {
      UserStore.User created =
          server.register(
              message.text("username"),
              message.text("password"),
              message.text("display"),
              message.text("captcha_id"),
              message.integer("captcha_answer", -1));
      loggedIn(created, server.issueToken(created));
    } catch (IllegalArgumentException error) {
      error("REGISTER_FAILED", error.getMessage());
    }
  }

  private void login(Message message) {
    if (user != null) {
      error("ALREADY_LOGGED_IN", "已经登录");
      return;
    }
    UserStore.User authenticated;
    try {
      authenticated = server.authenticate(message.text("username"), message.text("password"));
    } catch (IllegalStateException locked) {
      error("LOGIN_LOCKED", locked.getMessage());
      return;
    }
    if (authenticated == null) {
      error("LOGIN_FAILED", "账号或密码错误");
      return;
    }
    String issued = server.issueToken(authenticated);
    if (issued == null) {
      error("ALREADY_ONLINE", "该账号已在别处登录");
      return;
    }
    loggedIn(authenticated, issued);
  }

  private void loggedIn(UserStore.User authenticated, String issued) {
    user = authenticated;
    token = issued;
    server.bind(this);
    send(
        Message.of(
            Types.LOGGED_IN,
            "token",
            issued,
            "name",
            user.name(),
            "display",
            user.display(),
            "wins",
            user.wins(),
            "losses",
            user.losses(),
            "rating",
            user.rating()));
  }

  private void reconnect(Message message) {
    if (user != null) {
      error("ALREADY_LOGGED_IN", "已经登录");
      return;
    }
    UserStore.User restored = server.userForToken(message.text("token"));
    if (restored == null) {
      error("RECONNECT_FAILED", "会话已失效，请重新登录");
      return;
    }
    user = restored;
    token = message.text("token");
    server.bind(this);
    send(
        Message.of(
            Types.LOGGED_IN,
            "token",
            token,
            "name",
            user.name(),
            "display",
            user.display(),
            "wins",
            user.wins(),
            "losses",
            user.losses(),
            "rating",
            user.rating()));
    server.restoreRoom(this);
  }

  private void quickMatch() {
    if (!requireLogin()) {
      return;
    }
    server.quickMatch(this);
  }

  private void createRoom() {
    if (!requireLogin()) {
      return;
    }
    server.createRoom(this);
  }

  private void joinRoom(Message message) {
    if (!requireLogin()) {
      return;
    }
    server.joinRoom(this, message.text("room"));
  }

  private void move(Message message) {
    if (!requireLogin() || room == null) {
      error("NO_ROOM", "不在对局中");
      return;
    }
    room.move(this, message.integer("row", -1), message.integer("col", -1));
  }

  private void chat(Message message) {
    if (room != null) {
      room.chat(this, message.text("text"));
    }
  }

  private void resign() {
    if (room != null) {
      room.resign(this);
    }
  }

  private void leave() {
    if (room != null) {
      Room current = room;
      room = null;
      current.leave(this);
    }
  }

  private boolean requireLogin() {
    if (user == null) {
      error("NOT_LOGGED_IN", "请先登录");
      return false;
    }
    return true;
  }

  @Override
  public void close() {
    try {
      socket.close();
    } catch (IOException ignored) {
      // 已关闭
    }
    server.disconnected(this);
  }
}
