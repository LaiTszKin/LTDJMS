package ltdjms.discord.panel.services;

import ltdjms.discord.gametoken.domain.GameTokenTransaction;
import ltdjms.discord.gametoken.services.GameTokenService;
import ltdjms.discord.gametoken.services.GameTokenTransactionService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/**
 * Facade for game token management operations. Aggregates game token balance and transaction
 * services to provide a simplified interface for panel operations.
 */
public class GameTokenManagementFacade {

  private final GameTokenService gameTokenService;
  private final GameTokenTransactionService transactionService;

  public GameTokenManagementFacade(
      GameTokenService gameTokenService, GameTokenTransactionService transactionService) {
    this.gameTokenService = gameTokenService;
    this.transactionService = transactionService;
  }

  /**
   * Gets a member's current game token balance.
   *
   * @param guildId the guild ID
   * @param userId the user ID
   * @return the current token balance
   */
  public long getMemberTokens(long guildId, long userId) {
    return gameTokenService.getBalance(guildId, userId);
  }

  /**
   * Adjusts a member's game token balance using mode-based adjustment.
   *
   * @param guildId the guild ID
   * @param userId the user ID
   * @param mode the adjustment mode: "add", "deduct", or "adjust"
   * @param amount the amount to add/deduct or target balance for "adjust" mode
   * @return the result of the adjustment
   */
  public Result<TokenAdjustmentResult, DomainError> adjustTokens(
      long guildId, long userId, String mode, long amount) {
    long previousTokens = gameTokenService.getBalance(guildId, userId);

    long actualAdjustment =
        switch (mode) {
          case "add" -> amount;
          case "deduct" -> -amount;
          case "adjust" -> amount - previousTokens;
          default -> {
            yield 0L;
          }
        };

    if (mode.equals("adjust") && amount < 0) {
      return Result.err(DomainError.invalidInput("目標代幣餘額不可為負數"));
    }

    return gameTokenService
        .tryAdjustTokens(guildId, userId, actualAdjustment)
        .map(
            result -> {
              transactionService.recordTransaction(
                  guildId,
                  userId,
                  actualAdjustment,
                  result.newTokens(),
                  GameTokenTransaction.Source.ADMIN_ADJUSTMENT,
                  null);

              return new TokenAdjustmentResult(
                  previousTokens, result.newTokens(), actualAdjustment);
            });
  }

  /**
   * Gets a page of token transaction history for a user.
   *
   * @param guildId the Discord guild ID
   * @param userId the Discord user ID
   * @param page the page number (1-based)
   * @return the transaction page
   */
  public GameTokenTransactionService.TransactionPage getTokenTransactionPage(
      long guildId, long userId, int page) {
    return transactionService.getTransactionPage(
        guildId, userId, page, GameTokenTransactionService.DEFAULT_PAGE_SIZE);
  }

  /** Record type for token adjustment results. */
  public record TokenAdjustmentResult(long previousTokens, long newTokens, long adjustment) {
    public String formatMessage() {
      String action = adjustment >= 0 ? "增加" : "扣除";
      long displayAmount = Math.abs(adjustment);
      return String.format(
          "%s %,d 遊戲代幣\n調整前：🎮 %,d\n調整後：🎮 %,d", action, displayAmount, previousTokens, newTokens);
    }
  }
}
