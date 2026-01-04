package ltdjms.discord.currency.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.currency.domain.CurrencyTransaction;
import ltdjms.discord.currency.domain.MemberCurrencyAccount;
import ltdjms.discord.currency.persistence.MemberCurrencyAccountRepository;
import ltdjms.discord.shared.events.BalanceChangedEvent;
import ltdjms.discord.shared.events.DomainEventPublisher;

/** Unit tests for GameRewardService. */
@ExtendWith(MockitoExtension.class)
class GameRewardServiceTest {

  private static final long TEST_GUILD_ID = 123456789012345678L;
  private static final long TEST_USER_ID = 987654321098765432L;

  @Mock private MemberCurrencyAccountRepository accountRepository;
  @Mock private CurrencyTransactionService transactionService;
  @Mock private DomainEventPublisher eventPublisher;

  private GameRewardService gameRewardService;

  @BeforeEach
  void setUp() {
    gameRewardService =
        new GameRewardService(accountRepository, transactionService, eventPublisher);
  }

  @Nested
  @DisplayName("creditReward")
  class CreditReward {

    @Test
    @DisplayName("should credit reward and return new balance")
    void shouldCreditRewardAndReturnNewBalance() {
      // Given
      MemberCurrencyAccount account = MemberCurrencyAccount.createNew(TEST_GUILD_ID, TEST_USER_ID);
      MemberCurrencyAccount creditedAccount = account.withAdjustedBalance(500L);
      when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(account);
      when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 500L))
          .thenReturn(creditedAccount);
      when(accountRepository.findByGuildIdAndUserId(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Optional.of(creditedAccount));

      // When
      long newBalance =
          gameRewardService.creditReward(
              TEST_GUILD_ID, TEST_USER_ID, 500L, CurrencyTransaction.Source.DICE_GAME_1_WIN);

      // Then
      assertThat(newBalance).isEqualTo(500L);
      verify(accountRepository).findOrCreate(TEST_GUILD_ID, TEST_USER_ID);
      verify(accountRepository).adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 500L);
      verify(transactionService)
          .recordTransaction(
              TEST_GUILD_ID,
              TEST_USER_ID,
              500L,
              500L,
              CurrencyTransaction.Source.DICE_GAME_1_WIN,
              null);
      verify(eventPublisher).publish(any(BalanceChangedEvent.class));
    }

    @Test
    @DisplayName("should credit large reward amount")
    void shouldCreditLargeReward() {
      // Given
      long largeReward = 3000L;
      MemberCurrencyAccount account = MemberCurrencyAccount.createNew(TEST_GUILD_ID, TEST_USER_ID);
      MemberCurrencyAccount creditedAccount =
          new MemberCurrencyAccount(
              TEST_GUILD_ID, TEST_USER_ID, 3000L, account.createdAt(), account.updatedAt());

      when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(account);
      when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 3000L))
          .thenReturn(creditedAccount);
      when(accountRepository.findByGuildIdAndUserId(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Optional.of(creditedAccount));

      // When
      long newBalance =
          gameRewardService.creditReward(
              TEST_GUILD_ID, TEST_USER_ID, largeReward, CurrencyTransaction.Source.DICE_GAME_2_WIN);

      // Then
      assertThat(newBalance).isEqualTo(3000L);
      verify(accountRepository).adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 3000L);
      verify(transactionService)
          .recordTransaction(
              TEST_GUILD_ID,
              TEST_USER_ID,
              largeReward,
              3000L,
              CurrencyTransaction.Source.DICE_GAME_2_WIN,
              null);
      verify(eventPublisher).publish(any(BalanceChangedEvent.class));
    }

    @Test
    @DisplayName("should handle zero reward by returning current balance")
    void shouldHandleZeroReward() {
      // Given
      MemberCurrencyAccount account =
          MemberCurrencyAccount.createNew(TEST_GUILD_ID, TEST_USER_ID).withAdjustedBalance(1000L);
      when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(account);

      // When
      long newBalance =
          gameRewardService.creditReward(
              TEST_GUILD_ID, TEST_USER_ID, 0L, CurrencyTransaction.Source.DICE_GAME_1_WIN);

      // Then
      assertThat(newBalance).isEqualTo(1000L);
      verify(accountRepository).findOrCreate(TEST_GUILD_ID, TEST_USER_ID);
      verify(accountRepository, never()).adjustBalance(anyLong(), anyLong(), anyLong());
      verify(transactionService, never())
          .recordTransaction(anyLong(), anyLong(), anyLong(), anyLong(), any(), any());
      verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("should throw exception for negative reward")
    void shouldThrowExceptionForNegativeReward() {
      // When/Then
      assertThatThrownBy(
              () ->
                  gameRewardService.creditReward(
                      TEST_GUILD_ID,
                      TEST_USER_ID,
                      -100L,
                      CurrencyTransaction.Source.DICE_GAME_1_WIN))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Reward amount cannot be negative");

      verify(accountRepository, never()).findOrCreate(anyLong(), anyLong());
      verify(transactionService, never())
          .recordTransaction(anyLong(), anyLong(), anyLong(), anyLong(), any(), any());
      verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("should use previous balance when account not found after credit")
    void shouldUsePreviousBalanceWhenAccountNotFound() {
      // Given
      MemberCurrencyAccount account =
          MemberCurrencyAccount.createNew(TEST_GUILD_ID, TEST_USER_ID).withAdjustedBalance(500L);
      MemberCurrencyAccount adjustedAccount =
          new MemberCurrencyAccount(
              TEST_GUILD_ID, TEST_USER_ID, 800L, account.createdAt(), account.updatedAt());
      when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(account);
      when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 300L))
          .thenReturn(adjustedAccount);
      when(accountRepository.findByGuildIdAndUserId(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Optional.empty());

      // When
      long newBalance =
          gameRewardService.creditReward(
              TEST_GUILD_ID, TEST_USER_ID, 300L, CurrencyTransaction.Source.DICE_GAME_1_WIN);

      // Then - should fall back to previousBalance + rewardAmount
      assertThat(newBalance).isEqualTo(800L); // 500 + 300
      verify(transactionService)
          .recordTransaction(
              TEST_GUILD_ID,
              TEST_USER_ID,
              300L,
              800L,
              CurrencyTransaction.Source.DICE_GAME_1_WIN,
              null);
    }
  }

  @Nested
  @DisplayName("applyRewardToAccount - private method behavior")
  class ApplyRewardToAccountBehavior {

    @Test
    @DisplayName("should make single adjustment for small reward")
    void shouldMakeSingleAdjustmentForSmallReward() {
      // Given
      MemberCurrencyAccount account = MemberCurrencyAccount.createNew(TEST_GUILD_ID, TEST_USER_ID);
      MemberCurrencyAccount creditedAccount = account.withAdjustedBalance(1000L);
      when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(account);
      when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 1000L))
          .thenReturn(creditedAccount);
      when(accountRepository.findByGuildIdAndUserId(TEST_GUILD_ID, TEST_USER_ID))
          .thenReturn(Optional.of(creditedAccount));

      // When
      gameRewardService.creditReward(
          TEST_GUILD_ID, TEST_USER_ID, 1000L, CurrencyTransaction.Source.DICE_GAME_1_WIN);

      // Then
      verify(accountRepository, times(1)).adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 1000L);
    }
  }
}
