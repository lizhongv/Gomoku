package io.github.lizhongv.gomoku.server;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.GameState;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.RuleSet;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.protocol.Message;
import io.github.lizhongv.gomoku.protocol.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 一局联机对局：服务端持有唯一的棋盘与行棋方，客户端只能请求落子。
 *
 * <p>所有修改在房间对象上同步；广播时逐个发送，单个连接异常不影响另一方。
 */
final class Room {

  /** 房间状态。 */
  enum Status {
    WAITING,
    PLAYING,
    FINISHED
  }

  private final Server server;

  private final String id;

  private final String blackName;

  private final GameState game = new GameState(new Board(15), RuleSet.FREESTYLE, Stone.BLACK);

  private final List<Move> moves = new ArrayList<>();

  private ClientConnection black;

  private ClientConnection white;

  private String whiteName = "";

  private Status status = Status.WAITING;

  private long lastActivity = System.currentTimeMillis();

  Room(Server server, String id, ClientConnection black) {
    this.server = server;
    this.id = id;
    this.black = black;
    this.blackName = black.user().name();
  }

  String id() {
    return id;
  }

  String blackName() {
    return blackName;
  }

  String whiteName() {
    return whiteName;
  }

  Status status() {
    return status;
  }

  synchronized boolean join(ClientConnection connection) {
    if (status != Status.WAITING || !whiteName.isEmpty()) {
      connection.error("ROOM_FULL", "房间已满或已开始");
      return false;
    }
    if (connection.user().name().equals(blackName)) {
      connection.error("SAME_USER", "不能和自己对局");
      return false;
    }
    white = connection;
    whiteName = connection.user().name();
    status = Status.PLAYING;
    connection.attach(this);
    connection.send(
        Message.of(Types.ROOM_JOINED, "room", id, "color", "WHITE", "opponent", blackName));
    black.send(Message.of(Types.ROOM_JOINED, "room", id, "color", "BLACK", "opponent", whiteName));
    broadcast(startMessage());
    broadcast(stateMessage());
    server.roomStarted(this);
    return true;
  }

  synchronized void move(ClientConnection connection, int row, int col) {
    if (status != Status.PLAYING) {
      connection.error("NOT_PLAYING", "对局尚未开始或已结束");
      return;
    }
    Stone side = colorOf(connection);
    if (side == null) {
      connection.error("NOT_PLAYER", "你不在这个房间里");
      return;
    }
    if (game.turn() != side) {
      connection.error("NOT_YOUR_TURN", "还没轮到你");
      return;
    }
    Move move = new Move(row, col);
    if (!game.isLegal(move)) {
      connection.error("ILLEGAL_MOVE", "这个点不能落子");
      return;
    }
    game.play(move);
    moves.add(move);
    lastActivity = System.currentTimeMillis();
    if (game.result().isFinished()) {
      finish(game.result(), "five");
      return;
    }
    broadcast(stateMessage());
  }

  synchronized void chat(ClientConnection connection, String text) {
    if (status == Status.FINISHED || colorOf(connection) == null) {
      return;
    }
    String clean = text == null ? "" : text.strip();
    if (clean.isEmpty()) {
      return;
    }
    if (clean.length() > 200) {
      clean = clean.substring(0, 200);
    }
    broadcast(Message.of(Types.CHAT, "from", connection.user().name(), "text", clean));
  }

  synchronized void resign(ClientConnection connection) {
    Stone side = colorOf(connection);
    if (status != Status.PLAYING || side == null) {
      return;
    }
    finish(side == Stone.BLACK ? GameResult.WHITE_WIN : GameResult.BLACK_WIN, "resign");
  }

  synchronized void leave(ClientConnection connection) {
    Stone side = colorOf(connection);
    if (status == Status.WAITING) {
      server.dropRoom(this);
      return;
    }
    if (status == Status.PLAYING && side != null) {
      finish(side == Stone.BLACK ? GameResult.WHITE_WIN : GameResult.BLACK_WIN, "leave");
    }
  }

  /** 断线：保留座位一段时间，超时后判负。 */
  synchronized void disconnected(ClientConnection connection) {
    // 先记住对手，再把座位置空；置空后无法再从连接反查对手
    ClientConnection peer = connection == black ? white : black;
    if (connection == black) {
      black = null;
    } else if (connection == white) {
      white = null;
    }
    if (status == Status.PLAYING) {
      if (peer != null) {
        peer.send(Message.of(Types.OPPONENT_LEFT, "grace", server.reconnectSeconds()));
      }
      server.scheduleForfeit(this, connection.user().name(), server.reconnectSeconds());
    }
  }

  /** 重连：替换连接并补发当前局面。 */
  synchronized boolean reconnect(ClientConnection connection) {
    if (!connection.user().name().equals(blackName)
        && !connection.user().name().equals(whiteName)) {
      return false;
    }
    if (connection.user().name().equals(blackName)) {
      black = connection;
    } else {
      white = connection;
    }
    connection.attach(this);
    connection.send(
        Message.of(
            Types.ROOM_JOINED,
            "room",
            id,
            "color",
            connection.user().name().equals(blackName) ? "BLACK" : "WHITE",
            "opponent",
            connection.user().name().equals(blackName) ? whiteName : blackName));
    connection.send(stateMessage());
    return true;
  }

  /** 断线超时判负。 */
  synchronized void forfeit(String username) {
    if (status != Status.PLAYING) {
      return;
    }
    boolean blackOffline = black == null && username.equals(blackName);
    boolean whiteOffline = white == null && username.equals(whiteName);
    if (!blackOffline && !whiteOffline) {
      return;
    }
    finish(blackOffline ? GameResult.WHITE_WIN : GameResult.BLACK_WIN, "timeout");
  }

  synchronized boolean hasBothPlayers() {
    return black != null || white != null;
  }

  private void finish(GameResult result, String reason) {
    status = Status.FINISHED;
    Message over = Message.of(Types.GAME_OVER, "result", result.name(), "reason", reason);
    broadcast(over);
    server.recordGame(this, moves, result);
    server.dropRoom(this);
  }

  private Message startMessage() {
    return Message.of(Types.GAME_START, "room", id, "black", blackName, "white", whiteName);
  }

  private Message stateMessage() {
    List<Object> serialized = new ArrayList<>();
    for (Move move : moves) {
      serialized.add(new int[] {move.row(), move.col()});
    }
    return Message.of(
        Types.STATE,
        "room",
        id,
        "moves",
        serialized,
        "turn",
        game.turn().name(),
        "result",
        game.result().name(),
        "black",
        blackName,
        "white",
        whiteName);
  }

  private Stone colorOf(ClientConnection connection) {
    if (connection == null || connection.user() == null) {
      return null;
    }
    if (connection.user().name().equals(blackName)) {
      return Stone.BLACK;
    }
    return connection.user().name().equals(whiteName) ? Stone.WHITE : null;
  }

  private java.util.Optional<ClientConnection> opponent(ClientConnection connection) {
    ClientConnection peer = connection == black ? white : black;
    return java.util.Optional.ofNullable(peer);
  }

  private void broadcast(Message message) {
    if (black != null) {
      black.send(message);
    }
    if (white != null) {
      white.send(message);
    }
  }
}
