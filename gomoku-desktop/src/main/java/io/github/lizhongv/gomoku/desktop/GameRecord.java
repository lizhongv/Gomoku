package io.github.lizhongv.gomoku.desktop;

import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.Move;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 最近一局的棋谱：用于「继续上局」与「复盘」。
 *
 * <p>格式为纯文本，一行一手：`B 7 7`；首行为结果。刻意不用 Java 序列化， 便于用户查看、跨版本读取，也避免反序列化漏洞。
 */
public final class GameRecord {

  private static final Path FILE = AppContext.dataDir().resolve("last-game.txt");

  private final List<Move> moves;

  private final GameResult result;

  private GameRecord(List<Move> moves, GameResult result) {
    this.moves = List.copyOf(moves);
    this.result = result;
  }

  /** 着法序列。 */
  public List<Move> moves() {
    return moves;
  }

  /** 结果；未下完为 ONGOING。 */
  public GameResult result() {
    return result;
  }

  /** 保存棋谱；写失败时静默，不影响对局。 */
  public static void save(List<Move> moves, GameResult result) {
    StringBuilder text = new StringBuilder("result=").append(result.name()).append('\n');
    for (int i = 0; i < moves.size(); i++) {
      Move move = moves.get(i);
      text.append(i % 2 == 0 ? 'B' : 'W')
          .append(' ')
          .append(move.row())
          .append(' ')
          .append(move.col())
          .append('\n');
    }
    try {
      Files.writeString(FILE, text);
    } catch (IOException ignored) {
      // 棋谱保存失败不打断对局
    }
  }

  /** 读取最近一局；文件不存在或格式错误时返回空。 */
  public static Optional<GameRecord> load() {
    if (!Files.exists(FILE)) {
      return Optional.empty();
    }
    try {
      List<String> lines = Files.readAllLines(FILE);
      GameResult result = GameResult.ONGOING;
      List<Move> moves = new ArrayList<>();
      for (String line : lines) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        if (trimmed.startsWith("result=")) {
          result = parseResult(trimmed.substring("result=".length()));
          continue;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 3) {
          moves.add(new Move(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
        }
      }
      return Optional.of(new GameRecord(moves, result));
    } catch (IOException | RuntimeException ignored) {
      return Optional.empty();
    }
  }

  /** 是否存在可续下的棋谱。 */
  public boolean unfinished() {
    return result == GameResult.ONGOING;
  }

  private static GameResult parseResult(String value) {
    try {
      return GameResult.valueOf(value);
    } catch (IllegalArgumentException ignored) {
      return GameResult.ONGOING;
    }
  }
}
