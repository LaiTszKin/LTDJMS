package ltdjms.discord.gametoken.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.currency.integration.PostgresIntegrationTestBase;
import ltdjms.discord.gametoken.domain.DiceGame2Config;
import ltdjms.discord.gametoken.persistence.DiceGame2ConfigRepository;
import ltdjms.discord.gametoken.persistence.JdbcDiceGame2ConfigRepository;

/**
 * Integration tests for DiceGame2ConfigRepository. Tests repository operations against a real
 * PostgreSQL database.
 */
class DiceGame2ConfigRepositoryIntegrationTest extends PostgresIntegrationTestBase {

  private static final long TEST_GUILD_ID = 123456789012345678L;

  private DiceGame2ConfigRepository configRepository;

  @BeforeEach
  void setUp() {
    configRepository = new JdbcDiceGame2ConfigRepository(dataSource);
  }

  @Test
  @DisplayName("should save and find dice game 2 config")
  void shouldSaveAndFindConfig() {
    // Given
    DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);

    // When
    configRepository.save(config);
    Optional<DiceGame2Config> found = configRepository.findByGuildId(TEST_GUILD_ID);

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().guildId()).isEqualTo(TEST_GUILD_ID);
    assertThat(found.get().minTokensPerPlay())
        .isEqualTo(DiceGame2Config.DEFAULT_MIN_TOKENS_PER_PLAY);
    assertThat(found.get().maxTokensPerPlay())
        .isEqualTo(DiceGame2Config.DEFAULT_MAX_TOKENS_PER_PLAY);
    assertThat(found.get().straightMultiplier())
        .isEqualTo(DiceGame2Config.DEFAULT_STRAIGHT_MULTIPLIER);
    assertThat(found.get().baseMultiplier()).isEqualTo(DiceGame2Config.DEFAULT_BASE_MULTIPLIER);
  }

  @Test
  @DisplayName("should find or create default config")
  void shouldFindOrCreateDefault() {
    // When - first call creates
    DiceGame2Config created = configRepository.findOrCreateDefault(TEST_GUILD_ID);

    // Then
    assertThat(created.guildId()).isEqualTo(TEST_GUILD_ID);
    assertThat(created.minTokensPerPlay()).isEqualTo(DiceGame2Config.DEFAULT_MIN_TOKENS_PER_PLAY);
    assertThat(created.maxTokensPerPlay()).isEqualTo(DiceGame2Config.DEFAULT_MAX_TOKENS_PER_PLAY);

    // When - second call finds existing config
    DiceGame2Config found = configRepository.findOrCreateDefault(TEST_GUILD_ID);

    // Then
    assertThat(found.guildId()).isEqualTo(TEST_GUILD_ID);
    assertThat(found.createdAt()).isEqualTo(created.createdAt());
  }

  @Test
  @DisplayName("should update tokens per play range")
  void shouldUpdateTokensPerPlayRange() {
    // Given
    configRepository.save(DiceGame2Config.createDefault(TEST_GUILD_ID));

    // When
    DiceGame2Config updated = configRepository.updateTokensPerPlayRange(TEST_GUILD_ID, 7L, 21L);

    // Then
    assertThat(updated.minTokensPerPlay()).isEqualTo(7L);
    assertThat(updated.maxTokensPerPlay()).isEqualTo(21L);

    Optional<DiceGame2Config> persisted = configRepository.findByGuildId(TEST_GUILD_ID);
    assertThat(persisted).isPresent();
    assertThat(persisted.get().minTokensPerPlay()).isEqualTo(7L);
    assertThat(persisted.get().maxTokensPerPlay()).isEqualTo(21L);
  }

  @Test
  @DisplayName("should create default config before updating multipliers")
  void shouldCreateDefaultBeforeUpdatingMultipliers() {
    // When
    DiceGame2Config updated = configRepository.updateMultipliers(TEST_GUILD_ID, 150_000L, 25_000L);

    // Then
    assertThat(updated.straightMultiplier()).isEqualTo(150_000L);
    assertThat(updated.baseMultiplier()).isEqualTo(25_000L);
    assertThat(updated.minTokensPerPlay()).isEqualTo(DiceGame2Config.DEFAULT_MIN_TOKENS_PER_PLAY);
    assertThat(updated.maxTokensPerPlay()).isEqualTo(DiceGame2Config.DEFAULT_MAX_TOKENS_PER_PLAY);
  }

  @Test
  @DisplayName("should update triple bonuses")
  void shouldUpdateTripleBonuses() {
    // Given
    configRepository.save(DiceGame2Config.createDefault(TEST_GUILD_ID));

    // When
    DiceGame2Config updated =
        configRepository.updateTripleBonuses(TEST_GUILD_ID, 1_600_000L, 2_600_000L);

    // Then
    assertThat(updated.tripleLowBonus()).isEqualTo(1_600_000L);
    assertThat(updated.tripleHighBonus()).isEqualTo(2_600_000L);

    Optional<DiceGame2Config> persisted = configRepository.findByGuildId(TEST_GUILD_ID);
    assertThat(persisted).isPresent();
    assertThat(persisted.get().tripleLowBonus()).isEqualTo(1_600_000L);
    assertThat(persisted.get().tripleHighBonus()).isEqualTo(2_600_000L);
  }

  @Test
  @DisplayName("should delete config")
  void shouldDeleteConfig() {
    // Given
    configRepository.findOrCreateDefault(TEST_GUILD_ID);

    // When
    boolean deleted = configRepository.deleteByGuildId(TEST_GUILD_ID);

    // Then
    assertThat(deleted).isTrue();
    assertThat(configRepository.findByGuildId(TEST_GUILD_ID)).isEmpty();
  }
}
