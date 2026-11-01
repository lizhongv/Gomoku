package io.github.lizhongv.gomoku.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lizhongv.gomoku.protocol.Codec;
import io.github.lizhongv.gomoku.protocol.Message;
import io.github.lizhongv.gomoku.protocol.Types;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerTest {

  @TempDir Path dataDir;

  private Server server;

  @BeforeEach
  void start() throws IOException {
    server = new Server(0, dataDir, 2);
  }

  @AfterEach
  void stop() {
    server.close();
  }

  @Test
  void rejectsOutOfTurnAndUnknownWinMessage() throws Exception {
    try (Client alice = new Client(server.port());
        Client bob = new Client(server.port())) {
      login(alice, "alice");
      login(bob, "bob");
      String room = createRoom(alice);
      joinRoom(alice, bob, room);

      alice.call(Message.of(Types.MOVE, "row", 7, "col", 7));
      bob.expect(Types.STATE);
      // 黑方刚下完，再让黑方落子应被拒绝
      Message error = alice.call(Message.of(Types.MOVE, "row", 7, "col", 8));
      assertThat(error.type()).isEqualTo(Types.ERROR);
      assertThat(error.text("code")).isEqualTo("NOT_YOUR_TURN");

      Message unknown = bob.call(Message.of("win"));
      assertThat(unknown.type()).isEqualTo(Types.ERROR);
      assertThat(unknown.text("code")).isEqualTo("UNKNOWN_TYPE");
    }
  }

  @Test
  void serverDecidesWinnerFromMoves() throws Exception {
    try (Client alice = new Client(server.port());
        Client bob = new Client(server.port())) {
      login(alice, "alice2");
      login(bob, "bob2");
      String room = createRoom(alice);
      joinRoom(alice, bob, room);

      int[][] blacks = {{7, 3}, {7, 4}, {7, 5}, {7, 6}, {7, 7}};
      int[][] whites = {{0, 0}, {0, 2}, {0, 4}, {0, 6}};
      Message over = null;
      for (int i = 0; i < blacks.length; i++) {
        over = alice.call(Message.of(Types.MOVE, "row", blacks[i][0], "col", blacks[i][1]));
        if (i < whites.length) {
          bob.expect(Types.STATE);
          bob.call(Message.of(Types.MOVE, "row", whites[i][0], "col", whites[i][1]));
          alice.expect(Types.STATE);
        }
      }
      assertThat(over).isNotNull();
      assertThat(over.type()).isEqualTo(Types.GAME_OVER);
      assertThat(over.text("result")).isEqualTo("BLACK_WIN");
      assertThat(over.text("reason")).isEqualTo("five");
      assertThat(over.integer("rating", 0)).isGreaterThan(1200);
    }
  }

  @Test
  void quickMatchPairsWaitingPlayers() throws Exception {
    try (Client alice = new Client(server.port());
        Client bob = new Client(server.port())) {
      login(alice, "quick1");
      login(bob, "quick2");
      Message first = alice.call(Message.of(Types.QUICK_MATCH));
      assertThat(first.type()).isEqualTo(Types.ROOM_JOINED);
      Message second = bob.call(Message.of(Types.QUICK_MATCH));
      assertThat(second.type()).isEqualTo(Types.ROOM_JOINED);
      assertThat(second.text("room")).isEqualTo(first.text("room"));
      alice.expect(Types.GAME_START);
      bob.expect(Types.GAME_START);
    }
  }

  @Test
  void rejectsDuplicatedMove() throws Exception {
    try (Client alice = new Client(server.port());
        Client bob = new Client(server.port())) {
      login(alice, "alice3");
      login(bob, "bob3");
      String room = createRoom(alice);
      joinRoom(alice, bob, room);
      alice.call(Message.of(Types.MOVE, "row", 7, "col", 7));
      bob.expect(Types.STATE);
      bob.call(Message.of(Types.MOVE, "row", 8, "col", 8));
      alice.expect(Types.STATE);
      Message error = alice.call(Message.of(Types.MOVE, "row", 8, "col", 8));
      assertThat(error.type()).isEqualTo(Types.ERROR);
      assertThat(error.text("code")).isEqualTo("ILLEGAL_MOVE");
    }
  }

  @Test
  void reconnectRestoresRoomAndMoves() throws Exception {
    Client alice = new Client(server.port());
    Client bob = new Client(server.port());
    login(alice, "alice4");
    String bobToken = login(bob, "bob4");
    String room = createRoom(alice);
    bob.call(Message.of(Types.JOIN_ROOM, "room", room));
    alice.call(Message.of(Types.MOVE, "row", 7, "col", 7));
    bob.expect(Types.STATE);
    bob.call(Message.of(Types.MOVE, "row", 8, "col", 8));
    alice.expect(Types.STATE);
    bob.close();

    try (Client again = new Client(server.port())) {
      again.call(Message.of(Types.HELLO, "version", Codec.VERSION));
      again.call(Message.of(Types.RECONNECT, "token", bobToken));
      Message state = again.expect(Types.STATE);
      assertThat(state.list("moves")).hasSize(2);
      assertThat(state.text("turn")).isEqualTo("BLACK");
    }
    alice.close();
  }

  private String login(Client client, String name) throws IOException {
    client.call(Message.of(Types.HELLO, "version", Codec.VERSION));
    Message result =
        client.call(Message.of(Types.REGISTER, "username", name, "password", "password123"));
    assertThat(result.type()).isEqualTo(Types.LOGGED_IN);
    return result.text("token");
  }

  private void joinRoom(Client alice, Client bob, String room) throws IOException {
    bob.call(Message.of(Types.JOIN_ROOM, "room", room));
    // 加入后双方都会收到开局与初始局面，先同步掉，避免后续断言读到旧消息
    alice.expect(Types.ROOM_JOINED);
    alice.expect(Types.GAME_START);
    alice.expect(Types.STATE);
    bob.expect(Types.GAME_START);
    bob.expect(Types.STATE);
  }

  private String createRoom(Client client) throws IOException {
    Message joined = client.call(Message.of(Types.CREATE_ROOM));
    assertThat(joined.type()).isEqualTo(Types.ROOM_JOINED);
    return joined.text("room");
  }

  private static final class Client implements AutoCloseable {

    private final Socket socket;

    private final InputStream input;

    private final OutputStream output;

    Client(int port) throws IOException {
      socket = new Socket("127.0.0.1", port);
      socket.setSoTimeout(5_000);
      input = socket.getInputStream();
      output = socket.getOutputStream();
    }

    Message call(Message message) throws IOException {
      output.write((Codec.encode(message) + "\n").getBytes(StandardCharsets.UTF_8));
      output.flush();
      return read();
    }

    Message expect(String type) throws IOException {
      for (int i = 0; i < 50; i++) {
        Message message = read();
        if (message.type().equals(type)) {
          return message;
        }
      }
      throw new AssertionError("没有收到消息类型 " + type);
    }

    private Message read() throws IOException {
      String line = Codec.readLine(input);
      if (line == null) {
        throw new IOException("连接已关闭");
      }
      return Codec.decode(line);
    }

    @Override
    public void close() throws IOException {
      socket.close();
    }
  }
}
