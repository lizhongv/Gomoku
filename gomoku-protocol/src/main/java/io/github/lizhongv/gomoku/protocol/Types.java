package io.github.lizhongv.gomoku.protocol;

/** 协议消息类型常量。 */
public final class Types {

  public static final String HELLO = "hello";
  public static final String WELCOME = "welcome";
  public static final String REGISTER = "register";
  public static final String LOGIN = "login";
  public static final String LOGGED_IN = "logged_in";
  public static final String RECONNECT = "reconnect";
  public static final String ERROR = "error";
  public static final String LIST_ROOMS = "list_rooms";
  public static final String ROOMS = "rooms";
  public static final String CREATE_ROOM = "create_room";
  public static final String QUICK_MATCH = "quick_match";
  public static final String JOIN_ROOM = "join_room";
  public static final String ROOM_JOINED = "room_joined";
  public static final String GAME_START = "game_start";
  public static final String MOVE = "move";
  public static final String STATE = "state";
  public static final String CHAT = "chat";
  public static final String RESIGN = "resign";
  public static final String LEAVE = "leave";
  public static final String GAME_OVER = "game_over";
  public static final String PING = "ping";
  public static final String PONG = "pong";
  public static final String OPPONENT_LEFT = "opponent_left";

  private Types() {}
}
