package io.github.lizhongv.gomoku.core;

/**
 * 棋型识别表：以落子点为中心的 9 格窗口，查表得到该方向的棋型。
 *
 * <p>窗口编码为三个状态：空、己方、阻挡（对方棋子或棋盘边界）。中心固定为刚落的己方棋子， 其余 8 格共 3^8 = 6561 种组合，启动时一次性算好，搜索中只做一次数组查表。
 *
 * <p>识别采用「再走一手会得到什么」的定义逐级判断，因此活三、跳三、跳四等复合形态不需要单独枚举：
 *
 * <ul>
 *   <li>四：存在一个空点，填入后成五；
 *   <li>活三：存在一个空点，填入后成活四；
 *   <li>眠三：存在一个空点，填入后成四，但不构成活三；
 *   <li>活二、眠二同理，再向下看一层。
 * </ul>
 *
 * <p>窗口取 ±4 格：活四形态 {@code .XXXX.} 最多延伸到中心外 4 格，足够判断。
 */
public final class PatternTable {

  /** 窗口长度。 */
  public static final int WINDOW = 9;

  /** 中心下标。 */
  public static final int CENTER = 4;

  private static final int SIZE = 6561;

  private static final byte[] TABLE = new byte[SIZE];

  static {
    for (int key = 0; key < SIZE; key++) {
      TABLE[key] = (byte) classify(decode(key), CENTER).ordinal();
    }
  }

  private PatternTable() {}

  /**
   * 查询以中心为中心的棋型。
   *
   * @param window 9 格窗口，取值 0 空、1 己方、2 阻挡；中心必须为 1
   */
  public static Pattern classify(byte[] window) {
    if (window == null || window.length != WINDOW || window[CENTER] != 1) {
      throw new IllegalArgumentException("窗口须为 9 格且中心为己方棋子");
    }
    int key = 0;
    int power = 1;
    for (int i = 0; i < WINDOW; i++) {
      if (i == CENTER) {
        continue;
      }
      key += window[i] * power;
      power *= 3;
    }
    return Pattern.values()[TABLE[key]];
  }

  /** 由字符串窗口判断棋型，供测试与文档示例使用；字符含义 X 己方、O 阻挡、. 空，中心须为 X。 */
  public static Pattern classify(String window) {
    if (window.length() != WINDOW || window.charAt(CENTER) != 'X') {
      throw new IllegalArgumentException("窗口须为 9 个字符且中心为 X：" + window);
    }
    byte[] cells = new byte[WINDOW];
    for (int i = 0; i < WINDOW; i++) {
      cells[i] =
          switch (window.charAt(i)) {
            case 'X' -> 1;
            case '.' -> 0;
            default -> 2;
          };
    }
    return classify(cells);
  }

  /** 静态分类入口：允许中心以外的位置为空，模拟落子时使用。 */
  static Pattern classify(char[] line, int center) {
    if (hasFiveAt(line, center)) {
      return Pattern.FIVE;
    }
    if (hasOpenFourAt(line, center)) {
      return Pattern.OPEN_FOUR;
    }
    if (hasFourAt(line, center)) {
      return Pattern.FOUR;
    }
    if (hasShapeAfterMove(line, center, PatternTable::hasOpenFourAt)) {
      return Pattern.OPEN_THREE;
    }
    if (hasShapeAfterMove(line, center, PatternTable::hasFourAt)) {
      return Pattern.THREE;
    }
    if (hasShapeAfterMove(line, center, PatternTable::hasOpenThreeAt)) {
      return Pattern.OPEN_TWO;
    }
    if (hasShapeAfterMove(line, center, PatternTable::hasThreeAt)) {
      return Pattern.TWO;
    }
    return Pattern.NONE;
  }

  /** 经过中心的最长连子数是否达到 5（无禁手规则下长连同样算五）。 */
  static boolean hasFiveAt(char[] line, int center) {
    return runThrough(line, center) >= 5;
  }

  /** 是否存在包含中心的活四形态 {@code .XXXX.}。 */
  static boolean hasOpenFourAt(char[] line, int center) {
    for (int start = 0; start + 5 < line.length; start++) {
      if (line[start] != '.' || line[start + 5] != '.') {
        continue;
      }
      boolean four = true;
      boolean containsCenter = false;
      for (int i = start + 1; i <= start + 4; i++) {
        if (line[i] != 'X') {
          four = false;
          break;
        }
        if (i == center) {
          containsCenter = true;
        }
      }
      if (four && containsCenter) {
        return true;
      }
    }
    return false;
  }

  /** 是否存在一个空点，填入后成五且五连经过中心。 */
  static boolean hasFourAt(char[] line, int center) {
    return hasShapeAfterMove(line, center, PatternTable::hasFiveAt);
  }

  /** 是否存在一个空点，填入后成活四且活四经过中心。 */
  static boolean hasOpenThreeAt(char[] line, int center) {
    return hasShapeAfterMove(line, center, PatternTable::hasOpenFourAt);
  }

  /** 是否存在一个空点，填入后成冲四且冲四经过中心。 */
  static boolean hasThreeAt(char[] line, int center) {
    return hasShapeAfterMove(line, center, PatternTable::hasFourAt);
  }

  /** 枚举窗口内的空点，填入己方棋子后测试是否满足目标棋型。 */
  private static boolean hasShapeAfterMove(char[] line, int center, ShapeTest test) {
    for (int i = 0; i < line.length; i++) {
      if (line[i] != '.') {
        continue;
      }
      line[i] = 'X';
      boolean matched = test.test(line, center);
      line[i] = '.';
      if (matched) {
        return true;
      }
    }
    return false;
  }

  private static int runThrough(char[] line, int center) {
    int left = center;
    while (left > 0 && line[left - 1] == 'X') {
      left--;
    }
    int right = center;
    while (right + 1 < line.length && line[right + 1] == 'X') {
      right++;
    }
    return right - left + 1;
  }

  private static char[] decode(int key) {
    char[] line = new char[WINDOW];
    for (int i = 0; i < WINDOW; i++) {
      if (i == CENTER) {
        line[i] = 'X';
        continue;
      }
      int digit = key % 3;
      key /= 3;
      line[i] =
          switch (digit) {
            case 1 -> 'X';
            case 2 -> 'O';
            default -> '.';
          };
    }
    return line;
  }

  @FunctionalInterface
  private interface ShapeTest {
    boolean test(char[] line, int center);
  }
}
