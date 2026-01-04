package ltdjms.discord.panel.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ltdjms.discord.gametoken.domain.DiceGame1Config;
import ltdjms.discord.gametoken.domain.DiceGame2Config;
import ltdjms.discord.shared.Result;

class AdminPanelServiceEventTest {

  private CurrencyManagementFacade currencyFacade;
  private GameTokenManagementFacade gameTokenFacade;
  private GameConfigManagementFacade gameConfigFacade;
  private AIConfigManagementFacade aiConfigFacade;
  private AdminPanelService service;

  @BeforeEach
  void setUp() {
    currencyFacade = mock(CurrencyManagementFacade.class);
    gameTokenFacade = mock(GameTokenManagementFacade.class);
    gameConfigFacade = mock(GameConfigManagementFacade.class);
    aiConfigFacade = mock(AIConfigManagementFacade.class);

    service =
        new AdminPanelService(currencyFacade, gameTokenFacade, gameConfigFacade, aiConfigFacade);
  }

  @Test
  void updateDiceGame1Config_shouldPublishEventOnSuccess() {
    // Given
    long guildId = 123L;
    Instant now = Instant.now();
    DiceGame1Config updatedConfig = new DiceGame1Config(guildId, 5L, 20L, 150L, now, now);

    when(gameConfigFacade.updateDiceGame1Config(eq(guildId), eq(5L), eq(20L), eq(150L)))
        .thenReturn(Result.ok(updatedConfig));

    // When
    Result<DiceGame1Config, ?> result = service.updateDiceGame1Config(guildId, 5L, 20L, 150L);

    // Then
    assertThat(result.isOk()).isTrue();
    verify(gameConfigFacade).updateDiceGame1Config(guildId, 5L, 20L, 150L);
  }

  @Test
  void updateDiceGame2Config_shouldPublishEventOnSuccess() {
    // Given
    long guildId = 123L;
    Instant now = Instant.now();
    DiceGame2Config updatedConfig =
        new DiceGame2Config(guildId, 5L, 20L, 10L, 3L, 75L, 150L, now, now);

    when(gameConfigFacade.updateDiceGame2Config(
            eq(guildId), eq(5L), eq(20L), eq(10L), eq(3L), eq(75L), eq(150L)))
        .thenReturn(Result.ok(updatedConfig));

    // When
    Result<DiceGame2Config, ?> result =
        service.updateDiceGame2Config(guildId, 5L, 20L, 10L, 3L, 75L, 150L);

    // Then
    assertThat(result.isOk()).isTrue();
    verify(gameConfigFacade).updateDiceGame2Config(guildId, 5L, 20L, 10L, 3L, 75L, 150L);
  }

  @Test
  void updateDiceGame1Config_shouldNotPublishEventOnFailure() {
    // Given
    long guildId = 123L;
    when(gameConfigFacade.updateDiceGame1Config(eq(guildId), eq(20L), eq(5L), eq(null)))
        .thenReturn(Result.err(ltdjms.discord.shared.DomainError.invalidInput("Invalid range")));

    // When
    Result<DiceGame1Config, ?> result = service.updateDiceGame1Config(guildId, 20L, 5L, null);

    // Then
    assertThat(result.isErr()).isTrue();
    verify(gameConfigFacade).updateDiceGame1Config(guildId, 20L, 5L, null);
  }
}
