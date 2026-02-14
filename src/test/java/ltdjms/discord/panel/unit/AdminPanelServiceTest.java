package ltdjms.discord.panel.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ltdjms.discord.aichat.domain.AllowedCategory;
import ltdjms.discord.aichat.domain.AllowedChannel;
import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import ltdjms.discord.dispatch.services.DispatchAfterSalesStaffService;
import ltdjms.discord.gametoken.domain.DiceGame1Config;
import ltdjms.discord.gametoken.domain.DiceGame2Config;
import ltdjms.discord.panel.commands.AdminPanelButtonHandler;
import ltdjms.discord.panel.services.AIConfigManagementFacade;
import ltdjms.discord.panel.services.AdminPanelService;
import ltdjms.discord.panel.services.CurrencyManagementFacade;
import ltdjms.discord.panel.services.GameConfigManagementFacade;
import ltdjms.discord.panel.services.GameTokenManagementFacade;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shared.Unit;

/** Unit tests for AdminPanelService. */
class AdminPanelServiceTest {

  private static final long TEST_GUILD_ID = 123456789012345678L;
  private static final long TEST_USER_ID = 987654321098765432L;

  private CurrencyManagementFacade currencyFacade;
  private GameTokenManagementFacade gameTokenFacade;
  private GameConfigManagementFacade gameConfigFacade;
  private AIConfigManagementFacade aiConfigFacade;
  private DispatchAfterSalesStaffService dispatchAfterSalesStaffService;
  private AdminPanelService adminPanelService;

  @BeforeEach
  void setUp() {
    currencyFacade = mock(CurrencyManagementFacade.class);
    gameTokenFacade = mock(GameTokenManagementFacade.class);
    gameConfigFacade = mock(GameConfigManagementFacade.class);
    aiConfigFacade = mock(AIConfigManagementFacade.class);
    dispatchAfterSalesStaffService = mock(DispatchAfterSalesStaffService.class);

    adminPanelService =
        new AdminPanelService(
            currencyFacade,
            gameTokenFacade,
            gameConfigFacade,
            aiConfigFacade,
            dispatchAfterSalesStaffService);
  }

  @Nested
  @DisplayName("getCurrencyConfig")
  class GetCurrencyConfig {

    @Test
    @DisplayName("should return custom currency config when available")
    void shouldReturnCustomCurrencyConfig() {
      // Given - guild has custom currency "星幣" with icon "✨"
      GuildCurrencyConfig customConfig =
          GuildCurrencyConfig.createDefault(TEST_GUILD_ID).withUpdates("星幣", "✨");
      when(currencyFacade.getCurrencyConfig(TEST_GUILD_ID)).thenReturn(Result.ok(customConfig));

      // When
      Result<GuildCurrencyConfig, DomainError> result =
          adminPanelService.getCurrencyConfig(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().currencyName()).isEqualTo("星幣");
      assertThat(result.getValue().currencyIcon()).isEqualTo("✨");
    }

    @Test
    @DisplayName("should return default currency config when no custom config exists")
    void shouldReturnDefaultCurrencyConfig() {
      // Given - no custom config, returns default
      GuildCurrencyConfig defaultConfig = GuildCurrencyConfig.createDefault(TEST_GUILD_ID);
      when(currencyFacade.getCurrencyConfig(TEST_GUILD_ID)).thenReturn(Result.ok(defaultConfig));

      // When
      Result<GuildCurrencyConfig, DomainError> result =
          adminPanelService.getCurrencyConfig(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().currencyName()).isEqualTo(GuildCurrencyConfig.DEFAULT_NAME);
      assertThat(result.getValue().currencyIcon()).isEqualTo(GuildCurrencyConfig.DEFAULT_ICON);
    }
  }

  @Nested
  @DisplayName("getMemberBalance")
  class GetMemberBalance {

    @Test
    @DisplayName("should return balance when found")
    void shouldReturnBalanceWhenFound() {
      // Given
      when(currencyFacade.getMemberBalance(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Result.ok(5000L));

      // When
      Result<Long, DomainError> result =
          adminPanelService.getMemberBalance(TEST_GUILD_ID, TEST_USER_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("should propagate error when balance service fails")
    void shouldPropagateErrorWhenFails() {
      // Given
      DomainError error = DomainError.persistenceFailure("Connection failed", null);
      when(currencyFacade.getMemberBalance(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Result.err(error));

      // When
      Result<Long, DomainError> result =
          adminPanelService.getMemberBalance(TEST_GUILD_ID, TEST_USER_ID);

      // Then
      assertThat(result.isErr()).isTrue();
    }
  }

  @Nested
  @DisplayName("getMemberTokens")
  class GetMemberTokens {

    @Test
    @DisplayName("should return token balance")
    void shouldReturnTokenBalance() {
      // Given
      when(gameTokenFacade.getMemberTokens(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(100L);

      // When
      long tokens = adminPanelService.getMemberTokens(TEST_GUILD_ID, TEST_USER_ID);

      // Then
      assertThat(tokens).isEqualTo(100L);
    }
  }

  @Nested
  @DisplayName("adjustBalance")
  class AdjustBalance {

    @Test
    @DisplayName("should add balance when mode is add")
    void shouldAddBalance() {
      // Given
      CurrencyManagementFacade.BalanceAdjustmentResult adjustResult =
          new CurrencyManagementFacade.BalanceAdjustmentResult(1000L, 2000L, 1000L);
      when(currencyFacade.adjustBalance(eq(TEST_GUILD_ID), eq(TEST_USER_ID), eq("add"), anyLong()))
          .thenReturn(Result.ok(adjustResult));

      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          adminPanelService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "add", 1000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().previousBalance()).isEqualTo(1000L);
      assertThat(result.getValue().newBalance()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("should deduct balance when mode is deduct")
    void shouldDeductBalance() {
      // Given
      CurrencyManagementFacade.BalanceAdjustmentResult adjustResult =
          new CurrencyManagementFacade.BalanceAdjustmentResult(2000L, 1500L, -500L);
      when(currencyFacade.adjustBalance(
              eq(TEST_GUILD_ID), eq(TEST_USER_ID), eq("deduct"), anyLong()))
          .thenReturn(Result.ok(adjustResult));

      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          adminPanelService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "deduct", 500L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().newBalance()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("should set balance when mode is adjust")
    void shouldSetBalance() {
      // Given
      CurrencyManagementFacade.BalanceAdjustmentResult adjustResult =
          new CurrencyManagementFacade.BalanceAdjustmentResult(1000L, 5000L, 4000L);
      when(currencyFacade.adjustBalance(
              eq(TEST_GUILD_ID), eq(TEST_USER_ID), eq("adjust"), anyLong()))
          .thenReturn(Result.ok(adjustResult));

      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          adminPanelService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "adjust", 5000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().newBalance()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("should return error for invalid mode")
    void shouldReturnErrorForInvalidMode() {
      // Given
      when(currencyFacade.adjustBalance(
              eq(TEST_GUILD_ID), eq(TEST_USER_ID), eq("invalid"), anyLong()))
          .thenReturn(Result.err(DomainError.invalidInput("Unknown adjustment mode: invalid")));

      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          adminPanelService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "invalid", 1000L);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().message()).contains("Unknown adjustment mode");
    }
  }

  @Nested
  @DisplayName("adjustTokens")
  class AdjustTokens {

    @Test
    @DisplayName("should add tokens when mode is add")
    void shouldAddTokens() {
      // Given
      GameTokenManagementFacade.TokenAdjustmentResult adjustResult =
          new GameTokenManagementFacade.TokenAdjustmentResult(50L, 70L, 20L);
      when(gameTokenFacade.adjustTokens(eq(TEST_GUILD_ID), eq(TEST_USER_ID), eq("add"), anyLong()))
          .thenReturn(Result.ok(adjustResult));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          adminPanelService.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "add", 20L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().previousTokens()).isEqualTo(50L);
      assertThat(result.getValue().newTokens()).isEqualTo(70L);
    }

    @Test
    @DisplayName("should deduct tokens when mode is deduct")
    void shouldDeductTokens() {
      // Given
      GameTokenManagementFacade.TokenAdjustmentResult adjustResult =
          new GameTokenManagementFacade.TokenAdjustmentResult(50L, 30L, -20L);
      when(gameTokenFacade.adjustTokens(
              eq(TEST_GUILD_ID), eq(TEST_USER_ID), eq("deduct"), anyLong()))
          .thenReturn(Result.ok(adjustResult));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          adminPanelService.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "deduct", 20L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().newTokens()).isEqualTo(30L);
    }

    @Test
    @DisplayName("should set tokens when mode is adjust")
    void shouldSetTokens() {
      // Given
      GameTokenManagementFacade.TokenAdjustmentResult adjustResult =
          new GameTokenManagementFacade.TokenAdjustmentResult(50L, 100L, 50L);
      when(gameTokenFacade.adjustTokens(
              eq(TEST_GUILD_ID), eq(TEST_USER_ID), eq("adjust"), anyLong()))
          .thenReturn(Result.ok(adjustResult));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          adminPanelService.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "adjust", 100L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().newTokens()).isEqualTo(100L);
    }

    @Test
    @DisplayName("should reject negative target balance in adjust mode")
    void shouldRejectNegativeTargetBalance() {
      // Given
      when(gameTokenFacade.adjustTokens(
              eq(TEST_GUILD_ID), eq(TEST_USER_ID), eq("adjust"), anyLong()))
          .thenReturn(Result.err(DomainError.invalidInput("目標代幣餘額不可為負數")));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          adminPanelService.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "adjust", -10L);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().message()).contains("負數");
    }
  }

  @Nested
  @DisplayName("Game configuration")
  class GameConfiguration {

    @Test
    @DisplayName("should get dice-game-1 config")
    void shouldGetDiceGame1Config() {
      // Given
      DiceGame1Config config = DiceGame1Config.createDefault(TEST_GUILD_ID);
      when(gameConfigFacade.getDiceGame1Config(TEST_GUILD_ID)).thenReturn(config);

      // When
      DiceGame1Config result = adminPanelService.getDiceGame1Config(TEST_GUILD_ID);

      // Then
      assertThat(result.guildId()).isEqualTo(TEST_GUILD_ID);
      assertThat(result.minTokensPerPlay()).isEqualTo(DiceGame1Config.DEFAULT_MIN_TOKENS_PER_PLAY);
      assertThat(result.maxTokensPerPlay()).isEqualTo(DiceGame1Config.DEFAULT_MAX_TOKENS_PER_PLAY);
    }

    @Test
    @DisplayName("should get dice-game-2 config")
    void shouldGetDiceGame2Config() {
      // Given
      DiceGame2Config config = DiceGame2Config.createDefault(TEST_GUILD_ID);
      when(gameConfigFacade.getDiceGame2Config(TEST_GUILD_ID)).thenReturn(config);

      // When
      DiceGame2Config result = adminPanelService.getDiceGame2Config(TEST_GUILD_ID);

      // Then
      assertThat(result.guildId()).isEqualTo(TEST_GUILD_ID);
      assertThat(result.minTokensPerPlay()).isEqualTo(DiceGame2Config.DEFAULT_MIN_TOKENS_PER_PLAY);
      assertThat(result.maxTokensPerPlay()).isEqualTo(DiceGame2Config.DEFAULT_MAX_TOKENS_PER_PLAY);
    }

    @Test
    @DisplayName("should update dice-game-1 config token range")
    void shouldUpdateDiceGame1Config() {
      // Given
      DiceGame1Config oldConfig = DiceGame1Config.createDefault(TEST_GUILD_ID);
      DiceGame1Config updatedConfig = oldConfig.withTokensPerPlayRange(2L, 20L);
      when(gameConfigFacade.updateDiceGame1Config(eq(TEST_GUILD_ID), eq(2L), eq(20L), eq(null)))
          .thenReturn(Result.ok(updatedConfig));

      // When
      Result<DiceGame1Config, DomainError> result =
          adminPanelService.updateDiceGame1Config(TEST_GUILD_ID, 2L, 20L, null);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().minTokensPerPlay()).isEqualTo(2L);
      assertThat(result.getValue().maxTokensPerPlay()).isEqualTo(20L);
    }

    @Test
    @DisplayName("should update dice-game-1 reward")
    void shouldUpdateDiceGame1Reward() {
      // Given
      DiceGame1Config oldConfig = DiceGame1Config.createDefault(TEST_GUILD_ID);
      DiceGame1Config updatedConfig = oldConfig.withRewardPerDiceValue(500_000L);
      when(gameConfigFacade.updateDiceGame1Config(
              eq(TEST_GUILD_ID), eq(null), eq(null), eq(500_000L)))
          .thenReturn(Result.ok(updatedConfig));

      // When
      Result<DiceGame1Config, DomainError> result =
          adminPanelService.updateDiceGame1Config(TEST_GUILD_ID, null, null, 500_000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().rewardPerDiceValue()).isEqualTo(500_000L);
    }

    @Test
    @DisplayName("should update dice-game-2 token range")
    void shouldUpdateDiceGame2TokenRange() {
      // Given
      DiceGame2Config oldConfig = DiceGame2Config.createDefault(TEST_GUILD_ID);
      DiceGame2Config updatedConfig = oldConfig.withTokensPerPlayRange(10L, 40L);
      when(gameConfigFacade.updateDiceGame2Config(
              eq(TEST_GUILD_ID), eq(10L), eq(40L), eq(null), eq(null), eq(null), eq(null)))
          .thenReturn(Result.ok(updatedConfig));

      // When
      Result<DiceGame2Config, DomainError> result =
          adminPanelService.updateDiceGame2Config(TEST_GUILD_ID, 10L, 40L, null, null, null, null);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().minTokensPerPlay()).isEqualTo(10L);
      assertThat(result.getValue().maxTokensPerPlay()).isEqualTo(40L);
    }

    @Test
    @DisplayName("should update dice-game-2 multipliers")
    void shouldUpdateDiceGame2Multipliers() {
      // Given
      DiceGame2Config oldConfig = DiceGame2Config.createDefault(TEST_GUILD_ID);
      DiceGame2Config updatedConfig = oldConfig.withMultipliers(200_000L, 40_000L);
      when(gameConfigFacade.updateDiceGame2Config(
              eq(TEST_GUILD_ID), eq(null), eq(null), eq(200_000L), eq(40_000L), eq(null), eq(null)))
          .thenReturn(Result.ok(updatedConfig));

      // When
      Result<DiceGame2Config, DomainError> result =
          adminPanelService.updateDiceGame2Config(
              TEST_GUILD_ID, null, null, 200_000L, 40_000L, null, null);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().straightMultiplier()).isEqualTo(200_000L);
      assertThat(result.getValue().baseMultiplier()).isEqualTo(40_000L);
    }

    @Test
    @DisplayName("should update dice-game-2 bonuses")
    void shouldUpdateDiceGame2Bonuses() {
      // Given
      DiceGame2Config oldConfig = DiceGame2Config.createDefault(TEST_GUILD_ID);
      DiceGame2Config updatedConfig = oldConfig.withTripleBonuses(3_000_000L, 5_000_000L);
      when(gameConfigFacade.updateDiceGame2Config(
              eq(TEST_GUILD_ID),
              eq(null),
              eq(null),
              eq(null),
              eq(null),
              eq(3_000_000L),
              eq(5_000_000L)))
          .thenReturn(Result.ok(updatedConfig));

      // When
      Result<DiceGame2Config, DomainError> result =
          adminPanelService.updateDiceGame2Config(
              TEST_GUILD_ID, null, null, null, null, 3_000_000L, 5_000_000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().tripleLowBonus()).isEqualTo(3_000_000L);
      assertThat(result.getValue().tripleHighBonus()).isEqualTo(5_000_000L);
    }
  }

  @Nested
  @DisplayName("AI configuration")
  class AIConfiguration {

    @Test
    @DisplayName("should get allowed channels")
    void shouldGetAllowedChannels() {
      // Given
      Set<AllowedChannel> channels = Set.of(new AllowedChannel(123L, "general"));
      when(aiConfigFacade.getAllowedChannels(TEST_GUILD_ID)).thenReturn(Result.ok(channels));

      // When
      Result<Set<AllowedChannel>, DomainError> result =
          adminPanelService.getAllowedChannels(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("should get allowed categories")
    void shouldGetAllowedCategories() {
      // Given
      Set<AllowedCategory> categories = Set.of(new AllowedCategory(456L, "Text Channels"));
      when(aiConfigFacade.getAllowedCategories(TEST_GUILD_ID)).thenReturn(Result.ok(categories));

      // When
      Result<Set<AllowedCategory>, DomainError> result =
          adminPanelService.getAllowedCategories(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("should add allowed channel")
    void shouldAddAllowedChannel() {
      // Given
      AllowedChannel channel = new AllowedChannel(123L, "general");
      when(aiConfigFacade.addAllowedChannel(eq(TEST_GUILD_ID), eq(123L), eq("general")))
          .thenReturn(Result.ok(channel));

      // When
      Result<AllowedChannel, DomainError> result =
          adminPanelService.addAllowedChannel(TEST_GUILD_ID, 123L, "general");

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().channelId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("should remove allowed channel")
    void shouldRemoveAllowedChannel() {
      // Given
      when(aiConfigFacade.removeAllowedChannel(eq(TEST_GUILD_ID), eq(123L)))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result =
          adminPanelService.removeAllowedChannel(TEST_GUILD_ID, 123L);

      // Then
      assertThat(result.isOk()).isTrue();
    }

    @Test
    @DisplayName("should get enabled agent channels")
    void shouldGetEnabledAgentChannels() {
      // Given
      List<Long> channels = List.of(123L, 456L);
      when(aiConfigFacade.getEnabledAgentChannels(TEST_GUILD_ID)).thenReturn(Result.ok(channels));

      // When
      Result<List<Long>, DomainError> result =
          adminPanelService.getEnabledAgentChannels(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).containsExactly(123L, 456L);
    }

    @Test
    @DisplayName("should check if agent is enabled")
    void shouldCheckIfAgentEnabled() {
      // Given
      when(aiConfigFacade.isAgentEnabled(TEST_GUILD_ID, 123L)).thenReturn(true);

      // When
      boolean result = adminPanelService.isAgentEnabled(TEST_GUILD_ID, 123L);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should enable agent channel")
    void shouldEnableAgentChannel() {
      // Given
      when(aiConfigFacade.enableAgentChannel(eq(TEST_GUILD_ID), eq(123L)))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result = adminPanelService.enableAgentChannel(TEST_GUILD_ID, 123L);

      // Then
      assertThat(result.isOk()).isTrue();
    }

    @Test
    @DisplayName("should disable agent channel")
    void shouldDisableAgentChannel() {
      // Given
      when(aiConfigFacade.disableAgentChannel(eq(TEST_GUILD_ID), eq(123L)))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result = adminPanelService.disableAgentChannel(TEST_GUILD_ID, 123L);

      // Then
      assertThat(result.isOk()).isTrue();
    }

    @Test
    @DisplayName("should remove agent channel")
    void shouldRemoveAgentChannel() {
      // Given
      when(aiConfigFacade.removeAgentChannel(eq(TEST_GUILD_ID), eq(123L)))
          .thenReturn(Result.ok(Unit.INSTANCE));

      // When
      Result<Unit, DomainError> result = adminPanelService.removeAgentChannel(TEST_GUILD_ID, 123L);

      // Then
      assertThat(result.isOk()).isTrue();
    }
  }

  @Nested
  @DisplayName("Result formatting")
  class ResultFormatting {

    @Test
    @DisplayName("should format balance adjustment result")
    void shouldFormatBalanceAdjustmentResult() {
      // Given
      CurrencyManagementFacade.BalanceAdjustmentResult result =
          new CurrencyManagementFacade.BalanceAdjustmentResult(1000L, 2000L, 1000L);

      // When
      String message = result.formatMessage("Gold", "💰");

      // Then
      assertThat(message).contains("增加");
      assertThat(message).contains("1,000");
      assertThat(message).contains("2,000");
      assertThat(message).contains("💰");
    }

    @Test
    @DisplayName("should format token adjustment result")
    void shouldFormatTokenAdjustmentResult() {
      // Given
      GameTokenManagementFacade.TokenAdjustmentResult result =
          new GameTokenManagementFacade.TokenAdjustmentResult(50L, 70L, 20L);

      // When
      String message = result.formatMessage();

      // Then
      assertThat(message).contains("增加");
      assertThat(message).contains("20");
      assertThat(message).contains("50");
      assertThat(message).contains("70");
    }
  }

  @Nested
  @DisplayName("Dispatch after-sales staff")
  class DispatchAfterSalesStaffTests {

    @Test
    @DisplayName("should get dispatch after-sales staff list")
    void shouldGetDispatchAfterSalesStaffList() {
      when(dispatchAfterSalesStaffService.getStaffUserIds(TEST_GUILD_ID))
          .thenReturn(Result.ok(Set.of(TEST_USER_ID, 123L)));

      Result<Set<Long>, DomainError> result =
          adminPanelService.getDispatchAfterSalesStaff(TEST_GUILD_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).contains(TEST_USER_ID, 123L);
    }

    @Test
    @DisplayName("should add dispatch after-sales staff")
    void shouldAddDispatchAfterSalesStaff() {
      when(dispatchAfterSalesStaffService.addStaff(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Result.ok(Unit.INSTANCE));

      Result<Unit, DomainError> result =
          adminPanelService.addDispatchAfterSalesStaff(TEST_GUILD_ID, TEST_USER_ID);

      assertThat(result.isOk()).isTrue();
    }

    @Test
    @DisplayName("should remove dispatch after-sales staff")
    void shouldRemoveDispatchAfterSalesStaff() {
      when(dispatchAfterSalesStaffService.removeStaff(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Result.ok(Unit.INSTANCE));

      Result<Unit, DomainError> result =
          adminPanelService.removeDispatchAfterSalesStaff(TEST_GUILD_ID, TEST_USER_ID);

      assertThat(result.isOk()).isTrue();
    }
  }

  @Nested
  @DisplayName("AdminPanelButtonHandler constants")
  class AdminPanelButtonHandlerConstants {

    @Test
    @DisplayName("should define all button IDs")
    void shouldDefineAllButtonIds() {
      assertThat(AdminPanelButtonHandler.BUTTON_BALANCE).isEqualTo("admin_panel_balance");
      assertThat(AdminPanelButtonHandler.BUTTON_TOKENS).isEqualTo("admin_panel_tokens");
      assertThat(AdminPanelButtonHandler.BUTTON_GAMES).isEqualTo("admin_panel_games");
      assertThat(AdminPanelButtonHandler.BUTTON_BACK).isEqualTo("admin_panel_back");
      assertThat(AdminPanelButtonHandler.BUTTON_OPEN_BALANCE_MODAL)
          .isEqualTo("admin_open_balance_modal");
      assertThat(AdminPanelButtonHandler.BUTTON_OPEN_TOKEN_MODAL)
          .isEqualTo("admin_open_token_modal");
    }

    @Test
    @DisplayName("should define all modal IDs")
    void shouldDefineAllModalIds() {
      assertThat(AdminPanelButtonHandler.MODAL_BALANCE_ADJUST)
          .isEqualTo("admin_modal_balance_adjust");
      assertThat(AdminPanelButtonHandler.MODAL_TOKEN_ADJUST).isEqualTo("admin_modal_token_adjust");
      assertThat(AdminPanelButtonHandler.MODAL_GAME_1_TOKENS).isEqualTo("admin_modal_game1_tokens");
      assertThat(AdminPanelButtonHandler.MODAL_GAME_1_REWARD).isEqualTo("admin_modal_game1_reward");
      assertThat(AdminPanelButtonHandler.MODAL_GAME_2_TOKENS).isEqualTo("admin_modal_game2_tokens");
      assertThat(AdminPanelButtonHandler.MODAL_GAME_2_MULTIPLIERS)
          .isEqualTo("admin_modal_game2_multipliers");
      assertThat(AdminPanelButtonHandler.MODAL_GAME_2_BONUSES)
          .isEqualTo("admin_modal_game2_bonuses");
    }

    @Test
    @DisplayName("should define all select menu IDs")
    void shouldDefineAllSelectMenuIds() {
      assertThat(AdminPanelButtonHandler.SELECT_GAME).isEqualTo("admin_select_game");
      assertThat(AdminPanelButtonHandler.SELECT_BALANCE_USER)
          .isEqualTo("admin_select_balance_user");
      assertThat(AdminPanelButtonHandler.SELECT_BALANCE_MODE)
          .isEqualTo("admin_select_balance_mode");
      assertThat(AdminPanelButtonHandler.SELECT_TOKEN_USER).isEqualTo("admin_select_token_user");
      assertThat(AdminPanelButtonHandler.SELECT_TOKEN_MODE).isEqualTo("admin_select_token_mode");
      assertThat(AdminPanelButtonHandler.SELECT_GAME_SETTING)
          .isEqualTo("admin_select_game_setting");
    }
  }
}
