package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.Board;
import io.github.lizhongv.gomoku.core.Move;
import io.github.lizhongv.gomoku.core.Stone;
import io.github.lizhongv.gomoku.protocol.Codec;
import io.github.lizhongv.gomoku.protocol.Message;
import io.github.lizhongv.gomoku.protocol.Types;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 联机页：连接、登录/注册、房间列表与对局。
 *
 * <p>棋局以服务端下发的 STATE 为准，本地只负责显示与发送请求；服务端拒绝的落子会以 ERROR 返回。
 */
public final class OnlineView extends BorderPane {

  private final AppShell shell;

  private final I18n i18n;

  private final VBox lobby = new VBox(12);

  private final VBox gamePane = new VBox(12);

  private final StackPane center = new StackPane();

  private final Label status = new Label();

  private final TextField host = new TextField("127.0.0.1");

  private final TextField port = new TextField("4802");

  private final TextField username = new TextField();

  private final PasswordField password = new PasswordField();

  private final ListView<String> rooms = new ListView<>();

  private final List<String> roomIds = new ArrayList<>();

  private final BoardCanvas board = new BoardCanvas();

  private final Label turn = new Label();

  private final TextArea chat = new TextArea();

  private final TextField chatInput = new TextField();

  private final Button resign = new Button();

  private OnlineClient client;

  private Stone myColor = Stone.EMPTY;

  private String roomId = "";

  private String opponent = "";

  private boolean finished;

  private int rating = 1200;

  /** 创建联机页。 */
  public OnlineView(AppShell shell) {
    this.shell = shell;
    this.i18n = shell.context().i18n();
    setPadding(new Insets(20, 28, 20, 28));
    buildLobby();
    buildGame();
    center.getChildren().add(lobby);
    setCenter(center);
    setBottom(status);
    status.getStyleClass().add("hint");
    board.setOnPlace(
        (move, valid) -> {
          if (valid) {
            placeMove(move);
          }
        });
  }

  private void buildLobby() {
    Label title = new Label(i18n.t("online.title"));
    title.getStyleClass().add("page-title");

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(8);
    form.add(new Label(i18n.t("online.host")), 0, 0);
    form.add(host, 1, 0);
    form.add(new Label(i18n.t("online.port")), 2, 0);
    form.add(port, 3, 0);
    form.add(new Label(i18n.t("online.user")), 0, 1);
    form.add(username, 1, 1);
    form.add(new Label(i18n.t("online.password")), 2, 1);
    form.add(password, 3, 1);

    Button login = new Button(i18n.t("online.login"));
    login.getStyleClass().add("primary-button");
    login.setOnAction(event -> authenticate(false));
    Button register = new Button(i18n.t("online.register"));
    register.getStyleClass().add("secondary-button");
    register.setOnAction(event -> authenticate(true));
    HBox authRow = new HBox(8, login, register);

    rooms.setPrefHeight(180);
    Button refresh = new Button(i18n.t("online.refresh"));
    refresh.getStyleClass().add("secondary-button");
    refresh.setOnAction(event -> send(Message.of(Types.LIST_ROOMS)));
    Button create = new Button(i18n.t("online.create"));
    create.getStyleClass().add("secondary-button");
    create.setOnAction(event -> send(Message.of(Types.CREATE_ROOM)));
    Button join = new Button(i18n.t("online.join"));
    join.getStyleClass().add("primary-button");
    join.setOnAction(
        event -> {
          int index = rooms.getSelectionModel().getSelectedIndex();
          if (index >= 0 && index < roomIds.size()) {
            send(Message.of(Types.JOIN_ROOM, "room", roomIds.get(index)));
          }
        });

    Button quick = new Button(i18n.t("online.quick"));
    quick.getStyleClass().add("secondary-button");
    quick.setOnAction(event -> send(Message.of(Types.QUICK_MATCH)));

    lobby
        .getChildren()
        .addAll(
            title,
            form,
            authRow,
            new Label(i18n.t("online.rooms")),
            rooms,
            new HBox(8, refresh, quick, create, join));
    lobby.setPadding(new Insets(8, 0, 0, 0));
  }

  private void buildGame() {
    board.setShowCoordinates(true);
    BoardPane boardPane = new BoardPane(board);

    turn.getStyleClass().add("turn-label");
    chat.setEditable(false);
    chat.setPrefHeight(220);
    Button sendChat = new Button(i18n.t("online.send"));
    sendChat.getStyleClass().add("secondary-button");
    sendChat.setOnAction(event -> sendChat());
    chatInput.setOnAction(event -> sendChat());
    resign.setText(i18n.t("online.resign"));
    resign.getStyleClass().add("secondary-button");
    resign.setOnAction(event -> send(Message.of(Types.RESIGN)));
    Button leave = new Button(i18n.t("online.leave"));
    leave.getStyleClass().add("secondary-button");
    leave.setOnAction(
        event -> {
          send(Message.of(Types.LEAVE));
          showLobby();
        });
    Button back = new Button(i18n.t("nav.home"));
    back.getStyleClass().add("secondary-button");
    back.setOnAction(
        event -> {
          closeClient();
          shell.showHome();
        });

    VBox panel =
        new VBox(
            10,
            turn,
            new Label(i18n.t("online.chat")),
            chat,
            chatInput,
            new HBox(8, sendChat, resign, leave),
            back);
    panel.setPrefWidth(280);
    panel.setPadding(new Insets(0, 0, 0, 12));

    HBox body = new HBox(12, boardPane, panel);
    HBox.setHgrow(boardPane, Priority.ALWAYS);
    gamePane.getChildren().add(body);
  }

  private void authenticate(boolean register) {
    try {
      if (client == null || !client.running()) {
        int serverPort;
        try {
          serverPort = Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException error) {
          status.setText(i18n.t("online.badPort"));
          return;
        }
        client =
            new OnlineClient(host.getText().trim(), serverPort, this::onMessage, this::onClosed);
        send(Message.of(Types.HELLO, "version", Codec.VERSION));
      }
      Message message =
          register
              ? Message.of(
                  Types.REGISTER,
                  "username",
                  username.getText().trim(),
                  "password",
                  password.getText(),
                  "display",
                  username.getText().trim())
              : Message.of(
                  Types.LOGIN,
                  "username",
                  username.getText().trim(),
                  "password",
                  password.getText());
      send(message);
    } catch (IOException error) {
      status.setText(i18n.t("online.connectFailed", error.getMessage()));
    }
  }

  private void onMessage(Message message) {
    switch (message.type()) {
      case Types.LOGGED_IN -> {
        rating = message.integer("rating", 1200);
        status.setText(i18n.t("online.connected", message.text("display"), rating));
        send(Message.of(Types.LIST_ROOMS));
        showLobby();
      }
      case Types.ROOMS -> updateRooms(message);
      case Types.ROOM_JOINED -> {
        roomId = message.text("room");
        opponent = message.text("opponent");
        myColor = "BLACK".equals(message.text("color")) ? Stone.BLACK : Stone.WHITE;
        finished = false;
        chat.clear();
        center.getChildren().setAll(gamePane);
        turn.setText(i18n.t("online.waiting"));
      }
      case Types.GAME_START -> {
        opponent = "BLACK".equals(roleOf(myColor)) ? message.text("white") : message.text("black");
        status.setText(i18n.t("online.started", message.text("black"), message.text("white")));
      }
      case Types.STATE -> applyState(message);
      case Types.CHAT -> chat.appendText(message.text("from") + "：" + message.text("text") + "\n");
      case Types.GAME_OVER -> {
        finished = true;
        rating = message.integer("rating", rating);
        status.setText(
            i18n.t("online.over", message.text("result"), message.text("reason"), rating));
        resign.setDisable(true);
      }
      case Types.OPPONENT_LEFT -> status.setText(i18n.t("online.opponentLeft"));
      case Types.ERROR ->
          status.setText(i18n.t("online.error", message.text("code"), message.text("message")));
      default -> {
        // WELCOME/PONG 等无需界面处理
      }
    }
  }

  private void updateRooms(Message message) {
    List<String> labels = new ArrayList<>();
    roomIds.clear();
    for (Object item : message.list("rooms")) {
      if (item instanceof Map<?, ?> room) {
        String id = String.valueOf(room.get("id"));
        roomIds.add(id);
        labels.add(id + " · " + room.get("black"));
      }
    }
    rooms.getItems().setAll(labels);
    if (labels.isEmpty()) {
      rooms.getItems().setAll(List.of(i18n.t("online.noRooms")));
    }
  }

  private void applyState(Message message) {
    Board boardState = new Board(15);
    List<Object> moves = message.list("moves");
    for (int index = 0; index < moves.size(); index++) {
      if (moves.get(index) instanceof List<?> point && point.size() == 2) {
        int row = ((Number) point.get(0)).intValue();
        int col = ((Number) point.get(1)).intValue();
        boardState.place(new Move(row, col), index % 2 == 0 ? Stone.BLACK : Stone.WHITE);
      }
    }
    board.setBoard(boardState);
    if (!moves.isEmpty()
        && moves.get(moves.size() - 1) instanceof List<?> last
        && last.size() == 2) {
      board.setLastMove(
          new Move(((Number) last.get(0)).intValue(), ((Number) last.get(1)).intValue()));
    }
    boolean myTurn = message.text("turn").equals(roleOf(myColor));
    turn.setText(myTurn ? i18n.t("game.yourTurn") : i18n.t("game.aiTurn"));
    boolean over = !"ONGOING".equals(message.text("result"));
    if (over) {
      finished = true;
    }
  }

  private String roleOf(Stone color) {
    return color == Stone.BLACK ? "BLACK" : "WHITE";
  }

  private void sendChat() {
    String text = chatInput.getText().strip();
    if (!text.isEmpty()) {
      send(Message.of(Types.CHAT, "text", text));
      chatInput.clear();
    }
  }

  private void send(Message message) {
    if (client == null) {
      return;
    }
    try {
      client.send(message);
    } catch (IOException error) {
      status.setText(i18n.t("online.disconnected"));
    }
  }

  private void onClosed(String reason) {
    status.setText(i18n.t("online.disconnected"));
  }

  private void showLobby() {
    center.getChildren().setAll(lobby);
  }

  private void closeClient() {
    if (client != null) {
      client.close();
      client = null;
    }
  }

  /** 放子入口：由棋盘回调调用。 */
  public void placeMove(Move move) {
    if (!finished && myColor != Stone.EMPTY) {
      send(Message.of(Types.MOVE, "row", move.row(), "col", move.col()));
    }
  }

  /** 离开页面时关闭连接。 */
  public void dispose() {
    closeClient();
  }
}
