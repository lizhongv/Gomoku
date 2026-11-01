package io.github.lizhongv.gomoku.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.Move;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 对局记录：一行一局 JSON，追加写入。 */
final class GameStore {

  private static final Logger LOG = LoggerFactory.getLogger(GameStore.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Path file;

  GameStore(Path dataDir) {
    this.file = dataDir.resolve("games.jsonl");
  }

  /** 记录一局结果。 */
  synchronized void append(
      String roomId, String black, String white, List<Move> moves, GameResult result) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("room", roomId);
    row.put("black", black);
    row.put("white", white);
    row.put("result", result.name());
    row.put("moves", moves.stream().map(move -> new int[] {move.row(), move.col()}).toList());
    row.put("time", Instant.now().toString());
    try {
      Files.createDirectories(file.getParent());
      Files.writeString(
          file,
          MAPPER.writeValueAsString(row) + "\n",
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException error) {
      LOG.error("对局记录写入失败", error);
    }
  }
}
