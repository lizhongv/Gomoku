package io.github.lizhongv.gomoku.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lizhongv.gomoku.core.GameResult;
import io.github.lizhongv.gomoku.core.Move;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameRecordTest {

  @TempDir static Path home;

  @BeforeAll
  static void useTemporaryHome() {
    // 在首次加载 AppContext 之前改用户目录，避免测试写入真实用户数据
    System.setProperty("user.home", home.toString());
  }

  @Test
  void savesAndLoadsMoves() {
    List<Move> moves = List.of(new Move(7, 7), new Move(8, 8), new Move(7, 8));
    GameRecord.save(moves, GameResult.ONGOING);
    GameRecord loaded = GameRecord.load().orElseThrow();
    assertThat(loaded.moves()).isEqualTo(moves);
    assertThat(loaded.unfinished()).isTrue();
  }

  @Test
  void parsesFinishedResult() {
    GameRecord.save(List.of(new Move(7, 7)), GameResult.BLACK_WIN);
    GameRecord loaded = GameRecord.load().orElseThrow();
    assertThat(loaded.result()).isEqualTo(GameResult.BLACK_WIN);
    assertThat(loaded.unfinished()).isFalse();
  }

  @Test
  void malformedFileIsIgnored() throws Exception {
    Path file = home.resolve(".gomoku-v2").resolve("last-game.txt");
    Files.createDirectories(file.getParent());
    Files.writeString(file, "not a valid record\nB x y\n");
    assertThat(GameRecord.load()).isEmpty();
  }
}
