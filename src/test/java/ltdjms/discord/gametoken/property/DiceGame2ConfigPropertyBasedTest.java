package ltdjms.discord.gametoken.property;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import ltdjms.discord.gametoken.domain.DiceGame2Config;

/** Property-based tests for DiceGame2Config core invariants. */
@DisplayName("DiceGame2Config Property-Based Tests")
class DiceGame2ConfigPropertyBasedTest {

  private final Random random = new Random(20260213L);

  @RepeatedTest(200)
  @DisplayName("isValidTokenAmount should match inclusive range definition")
  void isValidTokenAmountShouldMatchRangeDefinition() {
    // Given
    long min = random.nextInt(100);
    long max = min + random.nextInt(100);
    long amount = min - 20 + random.nextInt((int) (max - min + 41));

    DiceGame2Config config =
        new DiceGame2Config(
            123456789012345678L,
            min,
            max,
            DiceGame2Config.DEFAULT_STRAIGHT_MULTIPLIER,
            DiceGame2Config.DEFAULT_BASE_MULTIPLIER,
            DiceGame2Config.DEFAULT_TRIPLE_LOW_BONUS,
            DiceGame2Config.DEFAULT_TRIPLE_HIGH_BONUS,
            Instant.now(),
            Instant.now());

    // When
    boolean actual = config.isValidTokenAmount(amount);

    // Then
    boolean expected = amount >= min && amount <= max;
    assertThat(actual).isEqualTo(expected);
  }

  @RepeatedTest(200)
  @DisplayName("calculateDiceCount should be linear by DICE_PER_TOKEN")
  void calculateDiceCountShouldBeLinearByDicePerToken() {
    // Given
    int tokens = random.nextInt(10_000);
    DiceGame2Config config = DiceGame2Config.createDefault(123456789012345678L);

    // When
    int diceCount = config.calculateDiceCount(tokens);
    int nextDiceCount = config.calculateDiceCount(tokens + 1);

    // Then
    assertThat(diceCount).isEqualTo(tokens * DiceGame2Config.DICE_PER_TOKEN);
    assertThat(nextDiceCount - diceCount).isEqualTo(DiceGame2Config.DICE_PER_TOKEN);
  }
}
