package ltdjms.discord.gametoken.services;

import ltdjms.discord.currency.domain.MemberCurrencyAccount;
import ltdjms.discord.currency.persistence.MemberCurrencyAccountRepository;
import ltdjms.discord.gametoken.services.DiceGame1Service.DiceGameResult;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DiceGame1Service.
 * Uses a predictable Random implementation to avoid Java 25 mocking restrictions.
 */
class DiceGame1ServiceTest {

    private static final long TEST_GUILD_ID = 123456789012345678L;
    private static final long TEST_USER_ID = 987654321098765432L;

    /**
     * A predictable Random that returns values from a predefined sequence.
     */
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

    /**
     * A stub repository for testing.
     */
    static class StubCurrencyRepository implements MemberCurrencyAccountRepository {
        private MemberCurrencyAccount account;
        private int adjustCallCount = 0;

        StubCurrencyRepository(long initialBalance) {
            Instant now = Instant.now();
            this.account = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, initialBalance, now, now);
        }

        @Override
        public Optional<MemberCurrencyAccount> findByGuildIdAndUserId(long guildId, long userId) {
            return Optional.of(account);
        }

        @Override
        public MemberCurrencyAccount save(MemberCurrencyAccount account) {
            this.account = account;
            return account;
        }

        @Override
        public MemberCurrencyAccount findOrCreate(long guildId, long userId) {
            return account;
        }

        @Override
        public MemberCurrencyAccount adjustBalance(long guildId, long userId, long amount) {
            adjustCallCount++;
            Instant now = Instant.now();
            this.account = new MemberCurrencyAccount(guildId, userId, account.balance() + amount, account.createdAt(), now);
            return account;
        }

        @Override
        public Result<MemberCurrencyAccount, DomainError> tryAdjustBalance(long guildId, long userId, long amount) {
            MemberCurrencyAccount updated = adjustBalance(guildId, userId, amount);
            return Result.ok(updated);
        }

        @Override
        public MemberCurrencyAccount setBalance(long guildId, long userId, long newBalance) {
            Instant now = Instant.now();
            this.account = new MemberCurrencyAccount(guildId, userId, newBalance, account.createdAt(), now);
            return account;
        }

        @Override
        public boolean deleteByGuildIdAndUserId(long guildId, long userId) {
            return true;
        }

        int getAdjustCallCount() {
            return adjustCallCount;
        }
    }

    @Test
    @DisplayName("should roll 5 dice")
    void shouldRollFiveDice() {
        // Given - predictable random that returns 0, 1, 2, 3, 4 (resulting in dice values 1, 2, 3, 4, 5)
        PredictableRandom random = new PredictableRandom(List.of(0, 1, 2, 3, 4));
        StubCurrencyRepository repository = new StubCurrencyRepository(0L);
        DiceGame1Service service = new DiceGame1Service(repository, random);

        // When
        List<Integer> rolls = service.rollDice();

        // Then
        assertThat(rolls).hasSize(5);
        assertThat(rolls).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("should calculate correct total reward")
    void shouldCalculateCorrectTotalReward() {
        // Given
        StubCurrencyRepository repository = new StubCurrencyRepository(0L);
        DiceGame1Service service = new DiceGame1Service(repository);
        List<Integer> rolls = List.of(1, 2, 3, 4, 5);
        // Expected: 1*250000 + 2*250000 + 3*250000 + 4*250000 + 5*250000 = 15*250000 = 3,750,000

        // When
        long totalReward = service.calculateTotalReward(rolls);

        // Then
        assertThat(totalReward).isEqualTo(3_750_000L);
    }

    @Test
    @DisplayName("should calculate max reward when all dice are 6")
    void shouldCalculateMaxReward() {
        // Given
        StubCurrencyRepository repository = new StubCurrencyRepository(0L);
        DiceGame1Service service = new DiceGame1Service(repository);
        List<Integer> rolls = List.of(6, 6, 6, 6, 6);
        // Expected: 6*250000 * 5 = 7,500,000

        // When
        long totalReward = service.calculateTotalReward(rolls);

        // Then
        assertThat(totalReward).isEqualTo(7_500_000L);
    }

    @Test
    @DisplayName("should calculate min reward when all dice are 1")
    void shouldCalculateMinReward() {
        // Given
        StubCurrencyRepository repository = new StubCurrencyRepository(0L);
        DiceGame1Service service = new DiceGame1Service(repository);
        List<Integer> rolls = List.of(1, 1, 1, 1, 1);
        // Expected: 1*250000 * 5 = 1,250,000

        // When
        long totalReward = service.calculateTotalReward(rolls);

        // Then
        assertThat(totalReward).isEqualTo(1_250_000L);
    }

    @Test
    @DisplayName("should play game and return result")
    void shouldPlayGameAndReturnResult() {
        // Given - predictable random returns 0,1,2,3,4 resulting in dice 1,2,3,4,5
        PredictableRandom random = new PredictableRandom(List.of(0, 1, 2, 3, 4));
        StubCurrencyRepository repository = new StubCurrencyRepository(1000L);
        DiceGame1Service service = new DiceGame1Service(repository, random);

        // When
        DiceGameResult result = service.play(TEST_GUILD_ID, TEST_USER_ID);

        // Then
        assertThat(result.diceRolls()).containsExactly(1, 2, 3, 4, 5);
        assertThat(result.totalReward()).isEqualTo(3_750_000L);
        assertThat(result.previousBalance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("should apply reward in single adjustment when MAX_ADJUSTMENT_AMOUNT is very large")
    void shouldApplyRewardInSingleAdjustment() {
        // Given - max reward is 7,500,000 which is less than Long.MAX_VALUE
        // Random returns all 5s, resulting in dice all showing 6
        PredictableRandom random = new PredictableRandom(List.of(5, 5, 5, 5, 5));
        StubCurrencyRepository repository = new StubCurrencyRepository(0L);
        DiceGame1Service service = new DiceGame1Service(repository, random);

        // When
        service.play(TEST_GUILD_ID, TEST_USER_ID);

        // Then - should call adjustBalance just once since MAX_ADJUSTMENT_AMOUNT is Long.MAX_VALUE
        assertThat(repository.getAdjustCallCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should format game result message correctly")
    void shouldFormatGameResultMessageCorrectly() {
        // Given
        DiceGameResult result = new DiceGameResult(
                TEST_GUILD_ID, TEST_USER_ID,
                List.of(1, 2, 3, 4, 5),
                3_750_000L, 0L, 3_750_000L
        );

        // When
        String message = result.formatMessage("💰", "Gold");

        // Then
        assertThat(message).contains("Dice Game Results");
        assertThat(message).contains(":one:");
        assertThat(message).contains(":two:");
        assertThat(message).contains(":three:");
        assertThat(message).contains(":four:");
        assertThat(message).contains(":five:");
        assertThat(message).contains("3,750,000");
        assertThat(message).contains("💰");
        assertThat(message).contains("Gold");
    }
}
