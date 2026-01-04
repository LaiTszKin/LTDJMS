package ltdjms.discord.gametoken.services;

import static org.assertj.core.api.Assertions.*;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ltdjms.discord.currency.domain.CurrencyTransaction;
import ltdjms.discord.currency.services.GameRewardService;
import ltdjms.discord.gametoken.domain.DiceGame2Config;
import ltdjms.discord.gametoken.services.DiceGame2Service.DiceGame2Result;

/**
 * Unit tests for DiceGame2Service. Uses a predictable Random implementation and stub
 * GameRewardService to test the game logic in isolation.
 */
class DiceGame2ServiceTest {

  private static final long TEST_GUILD_ID = 123456789012345678L;
  private static final long TEST_USER_ID = 987654321098765432L;
  private static final int DEFAULT_DICE_COUNT = 15; // 5 tokens × 3 dice per token

  /** A predictable Random that returns values from a predefined sequence. */
  static class PredictableRandom extends Random {
    private final Iterator<Integer> values;

    PredictableRandom(List<Integer> values) {
      this.values = values.iterator();
    }

    @Override
    public int nextInt(int bound) {
      return values.next();
    }
  }

  /** A stub GameRewardService for testing that tracks balance changes. */
  static class StubGameRewardService extends GameRewardService {
    private long currentBalance = 1000L;
    private int creditCallCount = 0;
    private long lastRewardAmount = 0;

    public StubGameRewardService() {
      super(null, null, null);
    }

    public void setCurrentBalance(long balance) {
      this.currentBalance = balance;
    }

    @Override
    public long creditReward(
        long guildId, long userId, long amount, CurrencyTransaction.Source source) {
      creditCallCount++;
      lastRewardAmount = amount;

      if (amount > 0) {
        currentBalance += amount;
      }
      return currentBalance;
    }

    public int getCreditCallCount() {
      return creditCallCount;
    }

    public long getLastRewardAmount() {
      return lastRewardAmount;
    }

    public long getCurrentBalance() {
      return currentBalance;
    }
  }

  @Nested
  @DisplayName("Basic dice rolling")
  class BasicDiceRolling {

    @Test
    @DisplayName("should roll specified number of dice")
    void shouldRollSpecifiedNumberOfDice() {
      // Given
      List<Integer> randomValues = List.of(0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);

      // When
      List<Integer> rolls = service.rollDice(15);

      // Then
      assertThat(rolls).hasSize(15);
    }

    @Test
    @DisplayName("should convert random values to dice values 1-6")
    void shouldConvertRandomValuesToDiceValues() {
      // Given
      List<Integer> randomValues = List.of(0, 1, 2, 3, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);

      // When
      List<Integer> rolls = service.rollDice(15);

      // Then
      assertThat(rolls.subList(0, 6)).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    @DisplayName("should roll different number of dice based on parameter")
    void shouldRollDifferentNumberBasedOnParameter() {
      // Given
      List<Integer> randomValues = List.of(0, 1, 2, 3, 4, 5, 0, 1, 2);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);

      // When - 3 tokens = 9 dice
      List<Integer> rolls = service.rollDice(9);

      // Then
      assertThat(rolls).hasSize(9);
      assertThat(rolls).containsExactly(1, 2, 3, 4, 5, 6, 1, 2, 3);
    }
  }

  @Nested
  @DisplayName("No straights and no triples - base reward only")
  class NoStraightsNoTriples {

    @Test
    @DisplayName("should calculate total sum × 20,000 when no straights or triples")
    void shouldCalculateBasicRewardWithNoStraightsOrTriples() {
      // Given
      List<Integer> randomValues = List.of(0, 2, 4, 0, 2, 4, 0, 2, 4, 0, 2, 4, 0, 2, 5);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);
      DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);

      // When
      DiceGame2Result result =
          service.play(TEST_GUILD_ID, TEST_USER_ID, config, DEFAULT_DICE_COUNT);

      // Then - total = 1+3+5+1+3+5+1+3+5+1+3+5+1+3+6 = 46, reward = 46 × 20,000 = 920,000
      assertThat(result.totalReward()).isEqualTo(920_000L);

      // Verify reward was credited
      assertThat(rewardService.getLastRewardAmount()).isEqualTo(920_000L);
    }
  }

  @Nested
  @DisplayName("Single straight detection and reward")
  class SingleStraight {

    @Test
    @DisplayName("should detect single straight (1,2,3) and calculate correctly")
    void shouldDetectSingleStraight123() {
      // Given
      List<Integer> randomValues = List.of(0, 1, 2, 5, 5, 4, 3, 0, 5, 4, 3, 0, 5, 4, 3);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);
      DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);

      // When
      DiceGame2Result result =
          service.play(TEST_GUILD_ID, TEST_USER_ID, config, DEFAULT_DICE_COUNT);

      // Then
      assertThat(result.totalReward()).isEqualTo(1_660_000L);
      assertThat(result.straightSegments()).hasSize(1);
      assertThat(result.straightSegments().get(0)).containsExactly(1, 2, 3);
    }
  }

  @Nested
  @DisplayName("Triple detection")
  class TripleDetection {

    @Test
    @DisplayName("should detect single triple (1,1,1) with bonus 1,500,000")
    void shouldDetectSingleTriple111() {
      // Given
      List<Integer> randomValues = List.of(0, 0, 0, 5, 4, 3, 2, 5, 4, 3, 2, 5, 4, 3, 2);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);
      DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);

      // When
      DiceGame2Result result =
          service.play(TEST_GUILD_ID, TEST_USER_ID, config, DEFAULT_DICE_COUNT);

      // Then
      assertThat(result.totalReward()).isEqualTo(2_580_000L);
      assertThat(result.tripleSegments()).hasSize(1);
    }

    @Test
    @DisplayName("should detect single triple (5,5,5) with bonus 2,500,000")
    void shouldDetectSingleTriple555() {
      // Given
      List<Integer> randomValues = List.of(4, 4, 4, 0, 2, 5, 0, 2, 5, 0, 2, 5, 0, 2, 5);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);
      DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);

      // When
      DiceGame2Result result =
          service.play(TEST_GUILD_ID, TEST_USER_ID, config, DEFAULT_DICE_COUNT);

      // Then
      assertThat(result.totalReward()).isEqualTo(3_300_000L);
    }
  }

  @Nested
  @DisplayName("Four or more consecutive same values (NOT a triple)")
  class FourOrMoreConsecutive {

    @Test
    @DisplayName("four consecutive same values should NOT be a triple")
    void fourConsecutiveShouldNotBeTriple() {
      // Given
      List<Integer> randomValues = List.of(0, 0, 0, 0, 2, 4, 2, 4, 2, 4, 2, 4, 2, 4, 2);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);
      DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);

      // When
      DiceGame2Result result =
          service.play(TEST_GUILD_ID, TEST_USER_ID, config, DEFAULT_DICE_COUNT);

      // Then
      assertThat(result.tripleSegments()).isEmpty();
      assertThat(result.totalReward()).isEqualTo(940_000L);
    }
  }

  @Nested
  @DisplayName("Comprehensive scenarios")
  class ComprehensiveScenarios {

    @Test
    @DisplayName("comprehensive case with straights and triples")
    void comprehensiveCaseWithStraightsAndTriples() {
      // Given
      List<Integer> randomValues = List.of(0, 1, 2, 3, 4, 5, 0, 0, 0, 1, 1, 1, 0, 2, 5);
      PredictableRandom random = new PredictableRandom(randomValues);
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService, random);
      DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);

      // When
      DiceGame2Result result =
          service.play(TEST_GUILD_ID, TEST_USER_ID, config, DEFAULT_DICE_COUNT);

      // Then
      assertThat(result.totalReward()).isEqualTo(5_300_000L);
      assertThat(result.straightSegments()).hasSize(1);
      assertThat(result.tripleSegments()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Result formatting")
  class ResultFormatting {

    @Test
    @DisplayName("should format game result message correctly")
    void shouldFormatGameResultMessageCorrectly() {
      // Given
      var result =
          new DiceGame2Result(
              TEST_GUILD_ID,
              TEST_USER_ID,
              List.of(1, 2, 3, 4, 5, 6, 1, 1, 1, 2, 2, 2, 1, 3, 6),
              5_300_000L,
              0L,
              5_300_000L,
              List.of(List.of(1, 2, 3, 4, 5, 6)),
              List.of(List.of(1, 1, 1), List.of(2, 2, 2)),
              2_100_000L, // straight reward
              200_000L, // non-straight reward
              3_000_000L // triple reward
              );

      // When
      String message = result.formatMessage("💰", "Gold");

      // Then
      assertThat(message).contains("Dice Game 2 Results");
      assertThat(message).contains(":one:");
      assertThat(message).contains(":six:");
      assertThat(message).contains("5,300,000");
      assertThat(message).contains("💰");
      assertThat(message).contains("Gold");
    }
  }

  @Nested
  @DisplayName("Performance regression tests")
  class PerformanceRegression {

    @Test
    @DisplayName("should complete 500 games within 200ms")
    void shouldComplete500GamesWithin200ms() {
      // Given
      StubGameRewardService rewardService = new StubGameRewardService();
      DefaultDiceGame2Service service = new DefaultDiceGame2Service(rewardService);
      DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);

      // When
      long startTime = System.nanoTime();
      for (int i = 0; i < 500; i++) {
        service.play(TEST_GUILD_ID, TEST_USER_ID, config, DEFAULT_DICE_COUNT);
      }
      long endTime = System.nanoTime();

      // Then - should complete within 200ms (200,000,000 ns)
      long durationNs = endTime - startTime;
      assertThat(durationNs).as("500 games should complete within 200ms").isLessThan(200_000_000L);
    }
  }
}
