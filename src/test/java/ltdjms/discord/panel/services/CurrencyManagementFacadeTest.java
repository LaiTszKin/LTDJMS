package ltdjms.discord.panel.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.currency.domain.BalanceView;
import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.BalanceService;
import ltdjms.discord.currency.services.CurrencyConfigService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** Unit tests for CurrencyManagementFacade. */
@ExtendWith(MockitoExtension.class)
class CurrencyManagementFacadeTest {

  private static final long TEST_GUILD_ID = 123456789012345678L;
  private static final long TEST_USER_ID = 987654321098765432L;

  @Mock private BalanceService balanceService;
  @Mock private BalanceAdjustmentService balanceAdjustmentService;
  @Mock private CurrencyConfigService currencyConfigService;

  private CurrencyManagementFacade facade;

  @BeforeEach
  void setUp() {
    facade =
        new CurrencyManagementFacade(
            balanceService, balanceAdjustmentService, currencyConfigService);
  }

  @Nested
  @DisplayName("getCurrencyConfig")
  class GetCurrencyConfig {

    @Test
    @DisplayName("should return currency config from service")
    void shouldReturnCurrencyConfig() {
      // Given
      GuildCurrencyConfig config =
          new GuildCurrencyConfig(TEST_GUILD_ID, "Gold", "💰", Instant.now(), Instant.now());
      when(currencyConfigService.tryGetConfig(TEST_GUILD_ID)).thenReturn(Result.ok(config));

      // When
      Result<GuildCurrencyConfig, DomainError> result = facade.getCurrencyConfig(TEST_GUILD_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().currencyName()).isEqualTo("Gold");
      assertThat(result.getValue().currencyIcon()).isEqualTo("💰");
      verify(currencyConfigService).tryGetConfig(TEST_GUILD_ID);
    }

    @Test
    @DisplayName("should propagate error from service")
    void shouldPropagateError() {
      // Given
      DomainError error = DomainError.persistenceFailure("Database error", null);
      when(currencyConfigService.tryGetConfig(TEST_GUILD_ID)).thenReturn(Result.err(error));

      // When
      Result<GuildCurrencyConfig, DomainError> result = facade.getCurrencyConfig(TEST_GUILD_ID);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.PERSISTENCE_FAILURE);
    }
  }

  @Nested
  @DisplayName("getMemberBalance")
  class GetMemberBalance {

    @Test
    @DisplayName("should return member balance from service")
    void shouldReturnMemberBalance() {
      // Given
      when(balanceService.tryGetBalance(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Result.ok(new BalanceView(TEST_GUILD_ID, TEST_USER_ID, 1000L, "Gold", "💰")));

      // When
      Result<Long, DomainError> result = facade.getMemberBalance(TEST_GUILD_ID, TEST_USER_ID);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).isEqualTo(1000L);
      verify(balanceService).tryGetBalance(TEST_GUILD_ID, TEST_USER_ID);
    }
  }

  @Nested
  @DisplayName("adjustBalance")
  class AdjustBalance {

    @Test
    @DisplayName("should add balance successfully")
    void shouldAddBalance() {
      // Given
      when(balanceAdjustmentService.tryAdjustBalance(TEST_GUILD_ID, TEST_USER_ID, 500L))
          .thenReturn(
              Result.ok(
                  new BalanceAdjustmentService.BalanceAdjustmentResult(
                      TEST_GUILD_ID, TEST_USER_ID, 1000L, 1500L, 500L, "Gold", "💰")));

      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          facade.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "add", 500L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().previousBalance()).isEqualTo(1000L);
      assertThat(result.getValue().newBalance()).isEqualTo(1500L);
      assertThat(result.getValue().adjustment()).isEqualTo(500L);
      verify(balanceAdjustmentService).tryAdjustBalance(TEST_GUILD_ID, TEST_USER_ID, 500L);
    }

    @Test
    @DisplayName("should deduct balance successfully")
    void shouldDeductBalance() {
      // Given
      when(balanceAdjustmentService.tryAdjustBalance(TEST_GUILD_ID, TEST_USER_ID, -200L))
          .thenReturn(
              Result.ok(
                  new BalanceAdjustmentService.BalanceAdjustmentResult(
                      TEST_GUILD_ID, TEST_USER_ID, 1000L, 800L, -200L, "Gold", "💰")));

      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          facade.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "deduct", 200L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().previousBalance()).isEqualTo(1000L);
      assertThat(result.getValue().newBalance()).isEqualTo(800L);
      assertThat(result.getValue().adjustment()).isEqualTo(-200L);
      verify(balanceAdjustmentService).tryAdjustBalance(TEST_GUILD_ID, TEST_USER_ID, -200L);
    }

    @Test
    @DisplayName("should adjust balance to target")
    void shouldAdjustBalanceToTarget() {
      // Given
      when(balanceAdjustmentService.tryAdjustBalanceTo(TEST_GUILD_ID, TEST_USER_ID, 2000L))
          .thenReturn(
              Result.ok(
                  new BalanceAdjustmentService.BalanceAdjustmentResult(
                      TEST_GUILD_ID, TEST_USER_ID, 1000L, 2000L, 1000L, "Gold", "💰")));

      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          facade.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "adjust", 2000L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().previousBalance()).isEqualTo(1000L);
      assertThat(result.getValue().newBalance()).isEqualTo(2000L);
      assertThat(result.getValue().adjustment()).isEqualTo(1000L);
      verify(balanceAdjustmentService).tryAdjustBalanceTo(TEST_GUILD_ID, TEST_USER_ID, 2000L);
    }

    @Test
    @DisplayName("should return error for invalid mode")
    void shouldReturnErrorForInvalidMode() {
      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          facade.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "invalid", 500L);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
      assertThat(result.getError().message()).contains("Unknown adjustment mode: invalid");
      verify(balanceAdjustmentService, never()).tryAdjustBalance(anyLong(), anyLong(), anyLong());
      verify(balanceAdjustmentService, never()).tryAdjustBalanceTo(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("should propagate error from service")
    void shouldPropagateError() {
      // Given
      DomainError error = DomainError.insufficientBalance("Not enough balance");
      when(balanceAdjustmentService.tryAdjustBalance(TEST_GUILD_ID, TEST_USER_ID, -5000L))
          .thenReturn(Result.err(error));

      // When
      Result<CurrencyManagementFacade.BalanceAdjustmentResult, DomainError> result =
          facade.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, "deduct", 5000L);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.INSUFFICIENT_BALANCE);
    }
  }

  @Nested
  @DisplayName("BalanceAdjustmentResult")
  class BalanceAdjustmentResult {

    @Test
    @DisplayName("should format message for addition")
    void shouldFormatMessageForAddition() {
      // Given
      CurrencyManagementFacade.BalanceAdjustmentResult result =
          new CurrencyManagementFacade.BalanceAdjustmentResult(1000L, 1500L, 500L);

      // When
      String message = result.formatMessage("Gold", "💰");

      // Then
      assertThat(message).contains("增加 500 💰 Gold");
      assertThat(message).contains("調整前：💰 1,000");
      assertThat(message).contains("調整後：💰 1,500");
    }

    @Test
    @DisplayName("should format message for deduction")
    void shouldFormatMessageForDeduction() {
      // Given
      CurrencyManagementFacade.BalanceAdjustmentResult result =
          new CurrencyManagementFacade.BalanceAdjustmentResult(1000L, 800L, -200L);

      // When
      String message = result.formatMessage("Gold", "💰");

      // Then
      assertThat(message).contains("扣除 200 💰 Gold");
      assertThat(message).contains("調整前：💰 1,000");
      assertThat(message).contains("調整後：💰 800");
    }

    @Test
    @DisplayName("should format large numbers correctly")
    void shouldFormatLargeNumbersCorrectly() {
      // Given
      CurrencyManagementFacade.BalanceAdjustmentResult result =
          new CurrencyManagementFacade.BalanceAdjustmentResult(10000L, 25000L, 15000L);

      // When
      String message = result.formatMessage("Points", "⭐");

      // Then
      assertThat(message).contains("15,000");
      assertThat(message).contains("10,000");
      assertThat(message).contains("25,000");
    }
  }
}
