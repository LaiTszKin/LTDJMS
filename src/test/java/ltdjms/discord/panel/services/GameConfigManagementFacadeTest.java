package ltdjms.discord.panel.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.gametoken.domain.DiceGame1Config;
import ltdjms.discord.gametoken.domain.DiceGame2Config;
import ltdjms.discord.gametoken.persistence.DiceGame1ConfigRepository;
import ltdjms.discord.gametoken.persistence.DiceGame2ConfigRepository;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shared.events.DiceGameConfigChangedEvent;
import ltdjms.discord.shared.events.DomainEventPublisher;

/** Unit tests for GameConfigManagementFacade. */
@ExtendWith(MockitoExtension.class)
class GameConfigManagementFacadeTest {

  private static final long TEST_GUILD_ID = 123456789012345678L;

  @Mock private DiceGame1ConfigRepository diceGame1ConfigRepository;
  @Mock private DiceGame2ConfigRepository diceGame2ConfigRepository;
  @Mock private DomainEventPublisher eventPublisher;

  private GameConfigManagementFacade facade;

  @BeforeEach
  void setUp() {
    facade =
        new GameConfigManagementFacade(
            diceGame1ConfigRepository, diceGame2ConfigRepository, eventPublisher);
  }

  @Nested
  @DisplayName("getDiceGame1Config")
  class GetDiceGame1Config {

    @Test
    @DisplayName("should return config from repository")
    void shouldReturnConfigFromRepository() {
      // Given
      DiceGame1Config config =
          DiceGame1Config.createDefault(TEST_GUILD_ID)
              .withTokensPerPlayRange(5L, 20L)
              .withRewardPerDiceValue(250000L);
      when(diceGame1ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(config);

      // When
      DiceGame1Config result = facade.getDiceGame1Config(TEST_GUILD_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.minTokensPerPlay()).isEqualTo(5L);
      assertThat(result.maxTokensPerPlay()).isEqualTo(20L);
      assertThat(result.rewardPerDiceValue()).isEqualTo(250000L);
      verify(diceGame1ConfigRepository).findOrCreateDefault(TEST_GUILD_ID);
    }
  }

  @Nested
  @DisplayName("getDiceGame2Config")
  class GetDiceGame2Config {

    @Test
    @DisplayName("should return config from repository")
    void shouldReturnConfigFromRepository() {
      // Given
      DiceGame2Config config =
          DiceGame2Config.createDefault(TEST_GUILD_ID)
              .withTokensPerPlayRange(5L, 20L)
              .withMultipliers(200000L, 20000L)
              .withTripleBonuses(1500000L, 2500000L);
      when(diceGame2ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(config);

      // When
      DiceGame2Config result = facade.getDiceGame2Config(TEST_GUILD_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.minTokensPerPlay()).isEqualTo(5L);
      assertThat(result.maxTokensPerPlay()).isEqualTo(20L);
      assertThat(result.straightMultiplier()).isEqualTo(200000L);
      verify(diceGame2ConfigRepository).findOrCreateDefault(TEST_GUILD_ID);
    }
  }

  @Nested
  @DisplayName("updateDiceGame1Config")
  class UpdateDiceGame1Config {

    @Test
    @DisplayName("should update tokens range and publish event")
    void shouldUpdateTokensRangeAndPublishEvent() {
      // Given
      DiceGame1Config current = DiceGame1Config.createDefault(TEST_GUILD_ID);
      DiceGame1Config updated = current.withTokensPerPlayRange(5L, 20L);
      when(diceGame1ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(current);
      when(diceGame1ConfigRepository.updateTokensPerPlayRange(TEST_GUILD_ID, 5L, 20L))
          .thenReturn(updated);

      // When
      Result<DiceGame1Config, DomainError> result =
          facade.updateDiceGame1Config(TEST_GUILD_ID, 5L, 20L, null);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().minTokensPerPlay()).isEqualTo(5L);
      assertThat(result.getValue().maxTokensPerPlay()).isEqualTo(20L);
      verify(diceGame1ConfigRepository).updateTokensPerPlayRange(TEST_GUILD_ID, 5L, 20L);
      verify(eventPublisher).publish(any(DiceGameConfigChangedEvent.class));
    }

    @Test
    @DisplayName("should update reward multiplier and publish event")
    void shouldUpdateRewardMultiplierAndPublishEvent() {
      // Given
      DiceGame1Config current = DiceGame1Config.createDefault(TEST_GUILD_ID);
      DiceGame1Config updated = current.withRewardPerDiceValue(500000L);
      when(diceGame1ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(current);
      when(diceGame1ConfigRepository.updateRewardPerDiceValue(TEST_GUILD_ID, 500000L))
          .thenReturn(updated);

      // When
      Result<DiceGame1Config, DomainError> result =
          facade.updateDiceGame1Config(TEST_GUILD_ID, null, null, 500000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().rewardPerDiceValue()).isEqualTo(500000L);
      verify(diceGame1ConfigRepository).updateRewardPerDiceValue(TEST_GUILD_ID, 500000L);
      verify(eventPublisher).publish(any(DiceGameConfigChangedEvent.class));
    }

    @Test
    @DisplayName("should update all parameters and publish event once")
    void shouldUpdateAllParametersAndPublishEventOnce() {
      // Given
      DiceGame1Config current = DiceGame1Config.createDefault(TEST_GUILD_ID);
      DiceGame1Config withTokens = current.withTokensPerPlayRange(10L, 50L);
      DiceGame1Config withReward = withTokens.withRewardPerDiceValue(300000L);
      when(diceGame1ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(current);
      when(diceGame1ConfigRepository.updateTokensPerPlayRange(TEST_GUILD_ID, 10L, 50L))
          .thenReturn(withTokens);
      when(diceGame1ConfigRepository.updateRewardPerDiceValue(TEST_GUILD_ID, 300000L))
          .thenReturn(withReward);

      // When
      Result<DiceGame1Config, DomainError> result =
          facade.updateDiceGame1Config(TEST_GUILD_ID, 10L, 50L, 300000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().minTokensPerPlay()).isEqualTo(10L);
      assertThat(result.getValue().maxTokensPerPlay()).isEqualTo(50L);
      assertThat(result.getValue().rewardPerDiceValue()).isEqualTo(300000L);
      verify(eventPublisher).publish(any(DiceGameConfigChangedEvent.class));
    }

    @Test
    @DisplayName("should return error when repository throws IllegalArgumentException")
    void shouldReturnErrorOnInvalidArgument() {
      // Given
      when(diceGame1ConfigRepository.findOrCreateDefault(TEST_GUILD_ID))
          .thenReturn(DiceGame1Config.createDefault(TEST_GUILD_ID));
      when(diceGame1ConfigRepository.updateTokensPerPlayRange(TEST_GUILD_ID, 20L, 5L))
          .thenThrow(new IllegalArgumentException("min cannot be greater than max"));

      // When
      Result<DiceGame1Config, DomainError> result =
          facade.updateDiceGame1Config(TEST_GUILD_ID, 20L, 5L, null);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
      assertThat(result.getError().message()).contains("min cannot be greater than max");
      verify(eventPublisher, never()).publish(any());
    }
  }

  @Nested
  @DisplayName("updateDiceGame2Config")
  class UpdateDiceGame2Config {

    @Test
    @DisplayName("should update tokens range and publish event")
    void shouldUpdateTokensRangeAndPublishEvent() {
      // Given
      DiceGame2Config current = DiceGame2Config.createDefault(TEST_GUILD_ID);
      DiceGame2Config updated = current.withTokensPerPlayRange(10L, 30L);
      when(diceGame2ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(current);
      when(diceGame2ConfigRepository.updateTokensPerPlayRange(TEST_GUILD_ID, 10L, 30L))
          .thenReturn(updated);

      // When
      Result<DiceGame2Config, DomainError> result =
          facade.updateDiceGame2Config(TEST_GUILD_ID, 10L, 30L, null, null, null, null);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().minTokensPerPlay()).isEqualTo(10L);
      assertThat(result.getValue().maxTokensPerPlay()).isEqualTo(30L);
      verify(diceGame2ConfigRepository).updateTokensPerPlayRange(TEST_GUILD_ID, 10L, 30L);
      verify(eventPublisher).publish(any(DiceGameConfigChangedEvent.class));
    }

    @Test
    @DisplayName("should update multipliers and publish event")
    void shouldUpdateMultipliersAndPublishEvent() {
      // Given
      DiceGame2Config current = DiceGame2Config.createDefault(TEST_GUILD_ID);
      DiceGame2Config updated = current.withMultipliers(300000L, 30000L);
      when(diceGame2ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(current);
      when(diceGame2ConfigRepository.updateMultipliers(TEST_GUILD_ID, 300000L, 30000L))
          .thenReturn(updated);

      // When
      Result<DiceGame2Config, DomainError> result =
          facade.updateDiceGame2Config(TEST_GUILD_ID, null, null, 300000L, 30000L, null, null);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().straightMultiplier()).isEqualTo(300000L);
      assertThat(result.getValue().baseMultiplier()).isEqualTo(30000L);
      verify(diceGame2ConfigRepository).updateMultipliers(TEST_GUILD_ID, 300000L, 30000L);
      verify(eventPublisher).publish(any(DiceGameConfigChangedEvent.class));
    }

    @Test
    @DisplayName("should update triple bonuses and publish event")
    void shouldUpdateTripleBonusesAndPublishEvent() {
      // Given
      DiceGame2Config current = DiceGame2Config.createDefault(TEST_GUILD_ID);
      DiceGame2Config updated = current.withTripleBonuses(2000000L, 3000000L);
      when(diceGame2ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(current);
      when(diceGame2ConfigRepository.updateTripleBonuses(TEST_GUILD_ID, 2000000L, 3000000L))
          .thenReturn(updated);

      // When
      Result<DiceGame2Config, DomainError> result =
          facade.updateDiceGame2Config(TEST_GUILD_ID, null, null, null, null, 2000000L, 3000000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().tripleLowBonus()).isEqualTo(2000000L);
      assertThat(result.getValue().tripleHighBonus()).isEqualTo(3000000L);
      verify(diceGame2ConfigRepository).updateTripleBonuses(TEST_GUILD_ID, 2000000L, 3000000L);
      verify(eventPublisher).publish(any(DiceGameConfigChangedEvent.class));
    }

    @Test
    @DisplayName("should update all parameters and publish event once")
    void shouldUpdateAllParametersAndPublishEventOnce() {
      // Given
      DiceGame2Config current = DiceGame2Config.createDefault(TEST_GUILD_ID);
      DiceGame2Config withTokens = current.withTokensPerPlayRange(15L, 45L);
      DiceGame2Config withMultipliers = withTokens.withMultipliers(250000L, 25000L);
      DiceGame2Config withBonuses = withMultipliers.withTripleBonuses(1800000L, 2800000L);
      when(diceGame2ConfigRepository.findOrCreateDefault(TEST_GUILD_ID)).thenReturn(current);
      when(diceGame2ConfigRepository.updateTokensPerPlayRange(TEST_GUILD_ID, 15L, 45L))
          .thenReturn(withTokens);
      when(diceGame2ConfigRepository.updateMultipliers(TEST_GUILD_ID, 250000L, 25000L))
          .thenReturn(withMultipliers);
      when(diceGame2ConfigRepository.updateTripleBonuses(TEST_GUILD_ID, 1800000L, 2800000L))
          .thenReturn(withBonuses);

      // When
      Result<DiceGame2Config, DomainError> result =
          facade.updateDiceGame2Config(
              TEST_GUILD_ID, 15L, 45L, 250000L, 25000L, 1800000L, 2800000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().minTokensPerPlay()).isEqualTo(15L);
      assertThat(result.getValue().maxTokensPerPlay()).isEqualTo(45L);
      assertThat(result.getValue().straightMultiplier()).isEqualTo(250000L);
      assertThat(result.getValue().baseMultiplier()).isEqualTo(25000L);
      assertThat(result.getValue().tripleLowBonus()).isEqualTo(1800000L);
      assertThat(result.getValue().tripleHighBonus()).isEqualTo(2800000L);
      verify(eventPublisher).publish(any(DiceGameConfigChangedEvent.class));
    }

    @Test
    @DisplayName("should return error when repository throws IllegalArgumentException")
    void shouldReturnErrorOnInvalidArgument() {
      // Given
      when(diceGame2ConfigRepository.findOrCreateDefault(TEST_GUILD_ID))
          .thenReturn(DiceGame2Config.createDefault(TEST_GUILD_ID));
      when(diceGame2ConfigRepository.updateTokensPerPlayRange(TEST_GUILD_ID, 50L, 10L))
          .thenThrow(new IllegalArgumentException("min cannot be greater than max"));

      // When
      Result<DiceGame2Config, DomainError> result =
          facade.updateDiceGame2Config(TEST_GUILD_ID, 50L, 10L, null, null, null, null);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
      assertThat(result.getError().message()).contains("min cannot be greater than max");
      verify(eventPublisher, never()).publish(any());
    }
  }
}
