package ltdjms.discord.panel.services;

import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.BalanceService;
import ltdjms.discord.currency.services.CurrencyConfigService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/**
 * Facade for currency management operations. Aggregates balance, adjustment, and configuration
 * services to provide a simplified interface for panel operations.
 */
public class CurrencyManagementFacade {

  private final BalanceService balanceService;
  private final BalanceAdjustmentService balanceAdjustmentService;
  private final CurrencyConfigService currencyConfigService;

  public CurrencyManagementFacade(
      BalanceService balanceService,
      BalanceAdjustmentService balanceAdjustmentService,
      CurrencyConfigService currencyConfigService) {
    this.balanceService = balanceService;
    this.balanceAdjustmentService = balanceAdjustmentService;
    this.currencyConfigService = currencyConfigService;
  }

  /**
   * Gets the currency configuration for a guild.
   *
   * @param guildId the guild ID
   * @return Result containing the currency configuration, or an error
   */
  public Result<GuildCurrencyConfig, DomainError> getCurrencyConfig(long guildId) {
    return currencyConfigService.tryGetConfig(guildId);
  }

  /**
   * Gets a member's current currency balance.
   *
   * @param guildId the guild ID
   * @param userId the user ID
   * @return Result containing the balance, or an error
   */
  public Result<Long, DomainError> getMemberBalance(long guildId, long userId) {
    return balanceService.tryGetBalance(guildId, userId).map(view -> view.balance());
  }

  /**
   * Adjusts a member's currency balance.
   *
   * @param guildId the guild ID
   * @param userId the user ID
   * @param mode the adjustment mode: "add", "deduct", or "adjust"
   * @param amount the amount to add/deduct or target balance for "adjust" mode
   * @return Result containing the adjustment result, or an error
   */
  public Result<BalanceAdjustmentResult, DomainError> adjustBalance(
      long guildId, long userId, String mode, long amount) {
    return switch (mode) {
      case "add" ->
          balanceAdjustmentService
              .tryAdjustBalance(guildId, userId, amount)
              .map(this::toBalanceAdjustmentResult);
      case "deduct" ->
          balanceAdjustmentService
              .tryAdjustBalance(guildId, userId, -amount)
              .map(this::toBalanceAdjustmentResult);
      case "adjust" ->
          balanceAdjustmentService
              .tryAdjustBalanceTo(guildId, userId, amount)
              .map(this::toBalanceAdjustmentResult);
      default -> Result.err(DomainError.invalidInput("Unknown adjustment mode: " + mode));
    };
  }

  private BalanceAdjustmentResult toBalanceAdjustmentResult(
      BalanceAdjustmentService.BalanceAdjustmentResult result) {
    return new BalanceAdjustmentResult(
        result.previousBalance(), result.newBalance(), result.adjustment());
  }

  /** Record type for balance adjustment results. */
  public record BalanceAdjustmentResult(long previousBalance, long newBalance, long adjustment) {
    public String formatMessage(String currencyName, String currencyIcon) {
      String action = adjustment >= 0 ? "增加" : "扣除";
      long displayAmount = Math.abs(adjustment);
      return String.format(
          "%s %,d %s %s\n調整前：%s %,d\n調整後：%s %,d",
          action,
          displayAmount,
          currencyIcon,
          currencyName,
          currencyIcon,
          previousBalance,
          currencyIcon,
          newBalance);
    }
  }
}
