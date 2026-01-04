package ltdjms.discord.currency.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.currency.domain.CurrencyTransaction;
import ltdjms.discord.currency.domain.MemberCurrencyAccount;
import ltdjms.discord.currency.persistence.MemberCurrencyAccountRepository;
import ltdjms.discord.shared.events.BalanceChangedEvent;
import ltdjms.discord.shared.events.DomainEventPublisher;

/**
 * Service for processing game rewards and adding them to member currency accounts. This service
 * encapsulates the logic for distributing game winnings while maintaining proper transaction
 * records and event publishing.
 *
 * <p>This service is designed to be used by game modules (dice games, etc.) to credit rewards to
 * players' currency accounts.
 */
public class GameRewardService {

  private static final Logger LOG = LoggerFactory.getLogger(GameRewardService.class);

  private final MemberCurrencyAccountRepository accountRepository;
  private final CurrencyTransactionService transactionService;
  private final DomainEventPublisher eventPublisher;

  public GameRewardService(
      MemberCurrencyAccountRepository accountRepository,
      CurrencyTransactionService transactionService,
      DomainEventPublisher eventPublisher) {
    this.accountRepository = accountRepository;
    this.transactionService = transactionService;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Credits a game reward to a member's currency account. This method handles the full reward
   * distribution process including balance adjustment, transaction recording, and event publishing.
   *
   * <p>If the reward amount exceeds the maximum adjustment amount, it will be split into multiple
   * adjustments automatically.
   *
   * @param guildId the Discord guild ID
   * @param userId the Discord user ID
   * @param rewardAmount the total reward amount to credit (must be positive)
   * @param transactionSource the source of this reward (e.g., DICE_GAME_1_WIN, DICE_GAME_2_WIN)
   * @return the final balance after the reward is applied
   * @throws IllegalArgumentException if rewardAmount is negative
   */
  public long creditReward(
      long guildId, long userId, long rewardAmount, CurrencyTransaction.Source transactionSource) {
    if (rewardAmount < 0) {
      throw new IllegalArgumentException("Reward amount cannot be negative: " + rewardAmount);
    }

    if (rewardAmount == 0) {
      // No reward to credit, return current balance
      return accountRepository.findOrCreate(guildId, userId).balance();
    }

    LOG.debug(
        "Crediting game reward: guildId={}, userId={}, amount={}, source={}",
        guildId,
        userId,
        rewardAmount,
        transactionSource);

    // Get previous balance
    long previousBalance = accountRepository.findOrCreate(guildId, userId).balance();

    // Apply reward (may need multiple adjustments due to MAX_ADJUSTMENT_AMOUNT)
    applyRewardToAccount(guildId, userId, rewardAmount);

    // Get new balance
    long newBalance =
        accountRepository
            .findByGuildIdAndUserId(guildId, userId)
            .map(MemberCurrencyAccount::balance)
            .orElse(previousBalance + rewardAmount);

    // Record transaction
    transactionService.recordTransaction(
        guildId, userId, rewardAmount, newBalance, transactionSource, null);

    // Publish event
    eventPublisher.publish(new BalanceChangedEvent(guildId, userId, newBalance));

    LOG.info(
        "Game reward credited: guildId={}, userId={}, amount={}, source={}, previousBalance={},"
            + " newBalance={}",
        guildId,
        userId,
        rewardAmount,
        transactionSource,
        previousBalance,
        newBalance);

    return newBalance;
  }

  /**
   * Applies the reward to the member's currency account. If the reward exceeds the max adjustment
   * amount, splits into multiple adjustments.
   *
   * @param guildId the Discord guild ID
   * @param userId the Discord user ID
   * @param totalReward the total reward to apply
   */
  private void applyRewardToAccount(long guildId, long userId, long totalReward) {
    long remaining = totalReward;
    long maxAdjustment = MemberCurrencyAccount.MAX_ADJUSTMENT_AMOUNT;

    while (remaining > 0) {
      long adjustment = Math.min(remaining, maxAdjustment);
      accountRepository.adjustBalance(guildId, userId, adjustment);
      remaining -= adjustment;
    }
  }
}
