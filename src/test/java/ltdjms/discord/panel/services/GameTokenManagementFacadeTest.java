package ltdjms.discord.panel.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.gametoken.services.GameTokenService;
import ltdjms.discord.gametoken.services.GameTokenTransactionService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** Unit tests for GameTokenManagementFacade. */
@ExtendWith(MockitoExtension.class)
class GameTokenManagementFacadeTest {

  private static final long TEST_GUILD_ID = 123456789012345678L;
  private static final long TEST_USER_ID = 987654321098765432L;

  @Mock private GameTokenService gameTokenService;
  @Mock private GameTokenTransactionService transactionService;

  private GameTokenManagementFacade facade;

  @BeforeEach
  void setUp() {
    facade = new GameTokenManagementFacade(gameTokenService, transactionService);
  }

  @Nested
  @DisplayName("getMemberTokens")
  class GetMemberTokens {

    @Test
    @DisplayName("should return token balance from service")
    void shouldReturnTokenBalance() {
      // Given
      when(gameTokenService.getBalance(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(100L);

      // When
      long tokens = facade.getMemberTokens(TEST_GUILD_ID, TEST_USER_ID);

      // Then
      assertThat(tokens).isEqualTo(100L);
      verify(gameTokenService).getBalance(TEST_GUILD_ID, TEST_USER_ID);
    }
  }

  @Nested
  @DisplayName("adjustTokens")
  class AdjustTokens {

    @Test
    @DisplayName("should add tokens successfully")
    void shouldAddTokens() {
      // Given
      when(gameTokenService.getBalance(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(50L);
      when(gameTokenService.tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, 30L))
          .thenReturn(
              Result.ok(
                  new GameTokenService.TokenAdjustmentResult(
                      TEST_GUILD_ID, TEST_USER_ID, 50L, 80L, 30L)));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          facade.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "add", 30L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().previousTokens()).isEqualTo(50L);
      assertThat(result.getValue().newTokens()).isEqualTo(80L);
      assertThat(result.getValue().adjustment()).isEqualTo(30L);
      verify(gameTokenService).getBalance(TEST_GUILD_ID, TEST_USER_ID);
      verify(gameTokenService).tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, 30L);
      verify(transactionService)
          .recordTransaction(
              TEST_GUILD_ID,
              TEST_USER_ID,
              30L,
              80L,
              ltdjms.discord.gametoken.domain.GameTokenTransaction.Source.ADMIN_ADJUSTMENT,
              null);
    }

    @Test
    @DisplayName("should deduct tokens successfully")
    void shouldDeductTokens() {
      // Given
      when(gameTokenService.getBalance(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(80L);
      when(gameTokenService.tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, -20L))
          .thenReturn(
              Result.ok(
                  new GameTokenService.TokenAdjustmentResult(
                      TEST_GUILD_ID, TEST_USER_ID, 80L, 60L, -20L)));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          facade.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "deduct", 20L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().previousTokens()).isEqualTo(80L);
      assertThat(result.getValue().newTokens()).isEqualTo(60L);
      assertThat(result.getValue().adjustment()).isEqualTo(-20L);
      verify(gameTokenService).tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, -20L);
    }

    @Test
    @DisplayName("should adjust tokens to target balance")
    void shouldAdjustTokensToTarget() {
      // Given
      when(gameTokenService.getBalance(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(50L);
      when(gameTokenService.tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, 30L))
          .thenReturn(
              Result.ok(
                  new GameTokenService.TokenAdjustmentResult(
                      TEST_GUILD_ID, TEST_USER_ID, 50L, 80L, 30L)));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          facade.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "adjust", 80L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().previousTokens()).isEqualTo(50L);
      assertThat(result.getValue().newTokens()).isEqualTo(80L);
      assertThat(result.getValue().adjustment()).isEqualTo(30L);
      verify(gameTokenService).tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, 30L);
    }

    @Test
    @DisplayName("should return error when target balance is negative")
    void shouldReturnErrorWhenTargetIsNegative() {
      // Given
      when(gameTokenService.getBalance(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(50L);

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          facade.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "adjust", -10L);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
      assertThat(result.getError().message()).contains("目標代幣餘額不可為負數");
      verify(gameTokenService, never()).tryAdjustTokens(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("should return error for invalid mode")
    void shouldReturnErrorForInvalidMode() {
      // Given
      when(gameTokenService.getBalance(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(50L);
      when(gameTokenService.tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, 0L))
          .thenReturn(
              Result.ok(
                  new GameTokenService.TokenAdjustmentResult(
                      TEST_GUILD_ID, TEST_USER_ID, 50L, 50L, 0L)));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          facade.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "invalid", 30L);

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().adjustment()).isEqualTo(0L);
      verify(gameTokenService).tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, 0L);
    }

    @Test
    @DisplayName("should propagate error from service")
    void shouldPropagateError() {
      // Given
      when(gameTokenService.getBalance(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(50L);
      DomainError error = DomainError.insufficientTokens("Not enough tokens");
      when(gameTokenService.tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, -100L))
          .thenReturn(Result.err(error));

      // When
      Result<GameTokenManagementFacade.TokenAdjustmentResult, DomainError> result =
          facade.adjustTokens(TEST_GUILD_ID, TEST_USER_ID, "deduct", 100L);

      // Then
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.INSUFFICIENT_TOKENS);
      verify(transactionService, never())
          .recordTransaction(anyLong(), anyLong(), anyLong(), anyLong(), any(), any());
    }
  }

  @Nested
  @DisplayName("getTokenTransactionPage")
  class GetTokenTransactionPage {

    @Test
    @DisplayName("should return transaction page from service")
    void shouldReturnTransactionPage() {
      // Given
      GameTokenTransactionService.TransactionPage expectedPage =
          new GameTokenTransactionService.TransactionPage(Collections.emptyList(), 1, 1, 0, 10);
      when(transactionService.getTransactionPage(
              TEST_GUILD_ID, TEST_USER_ID, 2, GameTokenTransactionService.DEFAULT_PAGE_SIZE))
          .thenReturn(expectedPage);

      // When
      GameTokenTransactionService.TransactionPage result =
          facade.getTokenTransactionPage(TEST_GUILD_ID, TEST_USER_ID, 2);

      // Then
      assertThat(result).isEqualTo(expectedPage);
      verify(transactionService)
          .getTransactionPage(
              TEST_GUILD_ID, TEST_USER_ID, 2, GameTokenTransactionService.DEFAULT_PAGE_SIZE);
    }
  }

  @Nested
  @DisplayName("TokenAdjustmentResult")
  class TokenAdjustmentResult {

    @Test
    @DisplayName("should format message for addition")
    void shouldFormatMessageForAddition() {
      // Given
      GameTokenManagementFacade.TokenAdjustmentResult result =
          new GameTokenManagementFacade.TokenAdjustmentResult(50L, 80L, 30L);

      // When
      String message = result.formatMessage();

      // Then
      assertThat(message).contains("增加 30 遊戲代幣");
      assertThat(message).contains("調整前：🎮 50");
      assertThat(message).contains("調整後：🎮 80");
    }

    @Test
    @DisplayName("should format message for deduction")
    void shouldFormatMessageForDeduction() {
      // Given
      GameTokenManagementFacade.TokenAdjustmentResult result =
          new GameTokenManagementFacade.TokenAdjustmentResult(80L, 60L, -20L);

      // When
      String message = result.formatMessage();

      // Then
      assertThat(message).contains("扣除 20 遊戲代幣");
      assertThat(message).contains("調整前：🎮 80");
      assertThat(message).contains("調整後：🎮 60");
    }

    @Test
    @DisplayName("should format large numbers correctly")
    void shouldFormatLargeNumbersCorrectly() {
      // Given
      GameTokenManagementFacade.TokenAdjustmentResult result =
          new GameTokenManagementFacade.TokenAdjustmentResult(10000L, 25000L, 15000L);

      // When
      String message = result.formatMessage();

      // Then
      assertThat(message).contains("15,000");
      assertThat(message).contains("10,000");
      assertThat(message).contains("25,000");
    }
  }
}
