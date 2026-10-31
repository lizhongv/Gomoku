package io.github.lizhongv.gomoku.core;

/**
 * 棋子颜色。
 *
 * <p>棋盘格用 byte 存储，取值与 {@link #code()} 一致，避免搜索时频繁装箱。
 */
public enum Stone {
  EMPTY(0),
  BLACK(1),
  WHITE(2);

  private final int code;

  Stone(int code) {
    this.code = code;
  }

  /** 存储与协议使用的编码：0 空、1 黑、2 白。 */
  public int code() {
    return code;
  }

  /** 对方颜色；对 {@link #EMPTY} 调用会抛异常，调用方须先判断。 */
  public Stone opposite() {
    return switch (this) {
      case BLACK -> WHITE;
      case WHITE -> BLACK;
      case EMPTY -> throw new IllegalStateException("空位没有对方颜色");
    };
  }

  /** 由编码还原；非法编码按空位处理，避免损坏数据导致搜索崩溃。 */
  public static Stone of(int code) {
    return switch (code) {
      case 1 -> BLACK;
      case 2 -> WHITE;
      default -> EMPTY;
    };
  }
}
