package ltdjms.discord.gametoken.integration;

import ltdjms.discord.currency.integration.PostgresIntegrationTestBase;
import ltdjms.discord.gametoken.domain.DiceGame1Config;
import ltdjms.discord.gametoken.domain.GameTokenAccount;
import ltdjms.discord.gametoken.persistence.DiceGame1ConfigRepository;
import ltdjms.discord.gametoken.persistence.GameTokenAccountRepository;
import ltdjms.discord.gametoken.persistence.InsufficientTokensException;
import ltdjms.discord.gametoken.persistence.JdbcDiceGame1ConfigRepository;
import ltdjms.discord.gametoken.persistence.JdbcGameTokenAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for game token repositories.
 * Tests repository operations against a real PostgreSQL database.
 */
class GameTokenRepositoryIntegrationTest extends PostgresIntegrationTestBase {

    private GameTokenAccountRepository tokenRepository;
    private DiceGame1ConfigRepository configRepository;

    private static final long TEST_GUILD_ID = 123456789012345678L;
    private static final long TEST_USER_ID = 987654321098765432L;

    @BeforeEach
    void setUp() {
        tokenRepository = new JdbcGameTokenAccountRepository(dataSource);
        configRepository = new JdbcDiceGame1ConfigRepository(dataSource);
    }

    @Nested
    @DisplayName("GameTokenAccountRepository Tests")
    class GameTokenAccountRepositoryTests {

        @Test
        @DisplayName("should save and find game token account")
        void shouldSaveAndFindAccount() {
            // Given
            GameTokenAccount account = GameTokenAccount.createNew(TEST_GUILD_ID, TEST_USER_ID);

            // When
            tokenRepository.save(account);
            Optional<GameTokenAccount> found = tokenRepository.findByGuildIdAndUserId(TEST_GUILD_ID, TEST_USER_ID);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().guildId()).isEqualTo(TEST_GUILD_ID);
            assertThat(found.get().userId()).isEqualTo(TEST_USER_ID);
            assertThat(found.get().tokens()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should return empty when account not found")
        void shouldReturnEmptyWhenNotFound() {
            // When
            Optional<GameTokenAccount> found = tokenRepository.findByGuildIdAndUserId(999L, 999L);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should find or create account")
        void shouldFindOrCreate() {
            // When - first call creates
            GameTokenAccount created = tokenRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID);

            // Then
            assertThat(created.guildId()).isEqualTo(TEST_GUILD_ID);
            assertThat(created.userId()).isEqualTo(TEST_USER_ID);
            assertThat(created.tokens()).isEqualTo(0L);

            // When - second call finds existing
            GameTokenAccount found = tokenRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID);

            // Then
            assertThat(found.guildId()).isEqualTo(TEST_GUILD_ID);
            assertThat(found.userId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("should adjust tokens positively")
        void shouldAdjustTokensPositively() {
            // Given
            tokenRepository.save(GameTokenAccount.createNew(TEST_GUILD_ID, TEST_USER_ID));

            // When
            GameTokenAccount updated = tokenRepository.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, 100L);

            // Then
            assertThat(updated.tokens()).isEqualTo(100L);

            // Verify persisted
            Optional<GameTokenAccount> found = tokenRepository.findByGuildIdAndUserId(TEST_GUILD_ID, TEST_USER_ID);
            assertThat(found).isPresent();
            assertThat(found.get().tokens()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should adjust tokens negatively within limits")
        void shouldAdjustTokensNegatively() {
            // Given - start with 100
            tokenRepository.save(GameTokenAccount.createNew(TEST_GUILD_ID, TEST_USER_ID));
            tokenRepository.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, 100L);

            // When
            GameTokenAccount updated = tokenRepository.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, -50L);

            // Then
            assertThat(updated.tokens()).isEqualTo(50L);
        }

        @Test
        @DisplayName("should prevent negative tokens")
        void shouldPreventNegativeTokens() {
            // Given - start with 50
            tokenRepository.save(GameTokenAccount.createNew(TEST_GUILD_ID, TEST_USER_ID));
            tokenRepository.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, 50L);

            // When/Then
            assertThatThrownBy(() -> tokenRepository.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, -100L))
                    .isInstanceOf(InsufficientTokensException.class)
                    .hasMessageContaining("Insufficient tokens");
        }

        @Test
        @DisplayName("should set tokens to specific value")
        void shouldSetTokens() {
            // Given
            tokenRepository.save(GameTokenAccount.createNew(TEST_GUILD_ID, TEST_USER_ID));

            // When
            GameTokenAccount updated = tokenRepository.setTokens(TEST_GUILD_ID, TEST_USER_ID, 500L);

            // Then
            assertThat(updated.tokens()).isEqualTo(500L);
        }

        @Test
        @DisplayName("should reject negative tokens in setTokens")
        void shouldRejectNegativeTokensInSet() {
            // Given
            tokenRepository.save(GameTokenAccount.createNew(TEST_GUILD_ID, TEST_USER_ID));

            // When/Then
            assertThatThrownBy(() -> tokenRepository.setTokens(TEST_GUILD_ID, TEST_USER_ID, -100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("should delete account")
        void shouldDeleteAccount() {
            // Given
            tokenRepository.save(GameTokenAccount.createNew(TEST_GUILD_ID, TEST_USER_ID));

            // When
            boolean deleted = tokenRepository.deleteByGuildIdAndUserId(TEST_GUILD_ID, TEST_USER_ID);

            // Then
            assertThat(deleted).isTrue();
            assertThat(tokenRepository.findByGuildIdAndUserId(TEST_GUILD_ID, TEST_USER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("DiceGame1ConfigRepository Tests")
    class DiceGame1ConfigRepositoryTests {

        @Test
        @DisplayName("should save and find dice game config")
        void shouldSaveAndFindConfig() {
            // Given
            DiceGame1Config config = DiceGame1Config.createDefault(TEST_GUILD_ID);

            // When
            configRepository.save(config);
            Optional<DiceGame1Config> found = configRepository.findByGuildId(TEST_GUILD_ID);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().guildId()).isEqualTo(TEST_GUILD_ID);
            assertThat(found.get().tokensPerPlay()).isEqualTo(DiceGame1Config.DEFAULT_TOKENS_PER_PLAY);
        }

        @Test
        @DisplayName("should return empty when config not found")
        void shouldReturnEmptyWhenNotFound() {
            // When
            Optional<DiceGame1Config> found = configRepository.findByGuildId(999L);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should find or create default config")
        void shouldFindOrCreateDefault() {
            // When - first call creates
            DiceGame1Config created = configRepository.findOrCreateDefault(TEST_GUILD_ID);

            // Then
            assertThat(created.guildId()).isEqualTo(TEST_GUILD_ID);
            assertThat(created.tokensPerPlay()).isEqualTo(DiceGame1Config.DEFAULT_TOKENS_PER_PLAY);

            // When - second call finds existing
            DiceGame1Config found = configRepository.findOrCreateDefault(TEST_GUILD_ID);

            // Then
            assertThat(found.guildId()).isEqualTo(TEST_GUILD_ID);
        }

        @Test
        @DisplayName("should update tokens per play")
        void shouldUpdateTokensPerPlay() {
            // Given
            configRepository.save(DiceGame1Config.createDefault(TEST_GUILD_ID));

            // When
            DiceGame1Config updated = configRepository.updateTokensPerPlay(TEST_GUILD_ID, 5L);

            // Then
            assertThat(updated.tokensPerPlay()).isEqualTo(5L);

            // Verify persisted
            Optional<DiceGame1Config> found = configRepository.findByGuildId(TEST_GUILD_ID);
            assertThat(found).isPresent();
            assertThat(found.get().tokensPerPlay()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should reject negative tokens per play")
        void shouldRejectNegativeTokensPerPlay() {
            // Given
            configRepository.save(DiceGame1Config.createDefault(TEST_GUILD_ID));

            // When/Then
            assertThatThrownBy(() -> configRepository.updateTokensPerPlay(TEST_GUILD_ID, -1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("should delete config")
        void shouldDeleteConfig() {
            // Given
            configRepository.save(DiceGame1Config.createDefault(TEST_GUILD_ID));

            // When
            boolean deleted = configRepository.deleteByGuildId(TEST_GUILD_ID);

            // Then
            assertThat(deleted).isTrue();
            assertThat(configRepository.findByGuildId(TEST_GUILD_ID)).isEmpty();
        }
    }
}
