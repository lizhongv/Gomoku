package io.github.lizhongv.gomoku.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PatternTableTest {

  @Test
  void detectsFive() {
    assertThat(PatternTable.classify("..XXXXX..")).isEqualTo(Pattern.FIVE);
    assertThat(PatternTable.classify("OXXXXX...")).isEqualTo(Pattern.FIVE);
  }

  @Test
  void detectsOpenFour() {
    assertThat(PatternTable.classify("...XXXX..")).isEqualTo(Pattern.OPEN_FOUR);
    assertThat(PatternTable.classify("..XXXX...")).isEqualTo(Pattern.OPEN_FOUR);
  }

  @Test
  void detectsBlockedFour() {
    assertThat(PatternTable.classify("OXXXX....")).isEqualTo(Pattern.FOUR);
    assertThat(PatternTable.classify("O..XXXXO.")).isEqualTo(Pattern.FOUR);
  }

  @Test
  void detectsBrokenFour() {
    // 中间隔一个空点的冲四：XXX.X
    assertThat(PatternTable.classify("OOXXX.X..")).isEqualTo(Pattern.FOUR);
  }

  @Test
  void detectsOpenThree() {
    assertThat(PatternTable.classify("..XXX....")).isEqualTo(Pattern.OPEN_THREE);
    assertThat(PatternTable.classify("...XXX...")).isEqualTo(Pattern.OPEN_THREE);
  }

  @Test
  void detectsSleepingThree() {
    assertThat(PatternTable.classify("OOXXX....")).isEqualTo(Pattern.THREE);
  }

  @Test
  void detectsOpenTwo() {
    assertThat(PatternTable.classify("....XX...")).isEqualTo(Pattern.OPEN_TWO);
  }

  @Test
  void loneStoneHasNoThreat() {
    assertThat(PatternTable.classify("....X....")).isEqualTo(Pattern.NONE);
  }

  @Test
  void rejectsMalformedWindow() {
    assertThatThrownBy(() -> PatternTable.classify("........."))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PatternTable.classify("..X.."))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PatternTable.classify(new byte[] {0, 0, 0}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void byteWindowMatchesStringWindow() {
    byte[] window = {0, 0, 1, 1, 1, 0, 0, 0, 0};
    assertThat(PatternTable.classify(window)).isEqualTo(PatternTable.classify("..XXX...."));
  }

  @Test
  void patternScoresAreOrdered() {
    assertThat(Pattern.FIVE.score()).isGreaterThan(Pattern.OPEN_FOUR.score());
    assertThat(Pattern.OPEN_FOUR.score()).isGreaterThan(Pattern.FOUR.score());
    assertThat(Pattern.FOUR.score()).isGreaterThan(Pattern.OPEN_THREE.score());
    assertThat(Pattern.OPEN_THREE.score()).isGreaterThan(Pattern.THREE.score());
    assertThat(Pattern.THREE.score()).isGreaterThan(Pattern.OPEN_TWO.score());
    assertThat(Pattern.OPEN_TWO.score()).isGreaterThan(Pattern.TWO.score());
    assertThat(Pattern.TWO.score()).isGreaterThan(Pattern.NONE.score());
    assertThat(Pattern.OPEN_FOUR.isFour()).isTrue();
    assertThat(Pattern.OPEN_THREE.isFour()).isFalse();
    assertThat(Pattern.THREE.isThreeOrBetter()).isTrue();
    assertThat(Pattern.TWO.isThreeOrBetter()).isFalse();
  }
}
